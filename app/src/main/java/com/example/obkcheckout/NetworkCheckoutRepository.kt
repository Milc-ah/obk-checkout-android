package com.example.obkcheckout

import android.util.Log
import com.example.obkcheckout.Entities.Container
import com.example.obkcheckout.Entities.NaturalPerson
import com.example.obkcheckout.Entities.Organization
import com.google.gson.Gson
import java.io.IOException
import kotlinx.coroutines.CancellationException
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.decodeFromJsonElement
import kotlinx.serialization.json.jsonObject

class NetworkCheckoutRepository(
    private val api: ApiQairosService,
    private val authHeaderProvider: () -> String = { TokenStore.bearerToken },
    private val json: Json = Json { ignoreUnknownKeys = true }
) : CheckoutRepository {
    private val gson = Gson()

    override suspend fun resolveTote(rawValue: String, source: ToteSource): Result<ToteRecord> {
        return runCatching {
            val parsed = parseQrCode(rawValue)
            if (parsed != null) {
                ToteRecord(
                    toteId = parsed.toteId,
                    company = parsed.companyName.ifBlank { "UNKNOWN" }.uppercase(),
                    mealsInTote = MEALS_PER_TOTE,
                    source = source,
                    isResolved = true
                )
            } else {
                val normalized = normalizeToToteId(rawValue)
                if (normalized.isBlank()) {
                    error("Invalid tote value.")
                }

                val parsedContainerId = rawValue
                    .takeIf { it.trim().startsWith("{") }
                    ?.let { rawJson ->
                        runCatching { json.decodeFromString<Container>(rawJson) }
                            .getOrNull()
                            ?.ContainerId
                    }

                val toteId = (parsedContainerId?.toString() ?: normalized).trim()
                val numericId = toteId.toIntOrNull()
                    ?: error("Manual tote entry must be a valid numeric tote ID or valid tote QR payload.")

                val response = api.getRecord(
                    authHeaderProvider(),
                    Container::class.simpleName ?: "Container",
                    numericId,
                    mapOf("include" to "ItemMovements(Warehouse,Organization)")
                )
                if (!response.isSuccessful) {
                    error("Unable to resolve tote $toteId from the backend.")
                }

                val container = json.decodeFromString<Container>(response.body() ?: error("Empty tote response."))
                val resolvedToteId = (container.ContainerId ?: numericId).toString()
                val companyName = container.ItemMovements
                    ?.firstOrNull { it.Warehouse?.WarehouseTypeId == 3 }
                    ?.Organization
                    ?.Name
                    ?.trim()
                    ?.takeIf { it.isNotBlank() }
                    ?: "UNKNOWN"

                ToteRecord(
                    toteId = resolvedToteId,
                    company = companyName.uppercase(),
                    mealsInTote = MEALS_PER_TOTE,
                    source = source,
                    isResolved = companyName != "UNKNOWN"
                )
            }
        }.onFailure { if (it is CancellationException) throw it }
    }

    override suspend fun loadCharityNames(): Result<List<String>> {
        return runCatching {
            val response = api.getRecords(
                authHeaderProvider(),
                Organization::class.simpleName ?: "Organization",
                mapOf("where" to "OrganizationTypeId=4")
            )
            if (!response.isSuccessful) {
                error("Unable to load charities.")
            }

            val rawBody = response.body() ?: return@runCatching emptyList()
            val data = json.parseToJsonElement(rawBody)
                .jsonObject["data"]
                ?: return@runCatching emptyList()
            json.decodeFromJsonElement<Array<Organization>>(data)
                .map { it.Name.trim() }
                .filter { it.isNotBlank() }
                .distinct()
                .sorted()
        }.onFailure { if (it is CancellationException) throw it }
    }

    override suspend fun lookupContactByEmail(email: String): Result<SavedContact?> {
        return runCatching {
            val response = api.getRecords(
                authHeaderProvider(),
                NaturalPerson::class.simpleName ?: "NaturalPerson",
                mapOf(
                    "where" to "Email='${email.trim()}'",
                    "include" to "PersonContactMappings"
                )
            )
            if (!response.isSuccessful) return@runCatching null

            val rawBody = response.body() ?: return@runCatching null
            val data = json.parseToJsonElement(rawBody)
                .jsonObject["data"]
                ?: return@runCatching null
            val persons = json.decodeFromJsonElement<Array<NaturalPerson>>(data)
            val person = persons.firstOrNull {
                it.Email.trim().equals(email.trim(), ignoreCase = true)
            } ?: persons.firstOrNull()
            ?: return@runCatching null

            val phone = person.PersonContactMappings
                ?.firstOrNull { it.PhoneNumber.isNotBlank() }
                ?.PhoneNumber
                .orEmpty()
                .ifBlank {
                    person.PersonContactMappings
                        ?.firstOrNull { it.CellNumber.isNotBlank() }
                        ?.CellNumber
                        .orEmpty()
                }

            SavedContact(
                email = person.Email.trim(),
                fullName = "${person.FirstName} ${person.LastName}".trim(),
                phone = phone,
                role = ""
            )
        }.onFailure { if (it is CancellationException) throw it }
    }

    override suspend fun submitCheckout(submission: CheckoutSubmission): Result<ConfirmCheckoutResponse> {
        return runCatching {
            val request = ConfirmCheckoutRequest(
                sessionId = "",
                totes = submission.totes,
                companies = submission.companies,
                contact = submission.contact,
                mealsGrandTotal = submission.mealsGrandTotal,
                submittedAtUtc = submission.submittedAtUtc,
                operatorId = submission.operatorId
            )

            Log.d("CheckoutSubmit", "POST checkout/confirm")
            Log.d("CheckoutSubmit", "Payload: ${gson.toJson(request)}")

            val response = api.confirmCheckout(
                authHeaderProvider(),
                request
            )
            Log.d("CheckoutSubmit", "Request URL: ${response.raw().request.url}")
            if (!response.isSuccessful) {
                val errorBody = response.errorBody()?.string()
                Log.e(
                    "CheckoutSubmit",
                    "API response error: code=${response.code()} url=${response.raw().request.url} body=$errorBody"
                )
                val userMessage = errorBody
                    ?.let { runCatching { json.parseToJsonElement(it).jsonObject["message"]?.toString()?.trim('"') }.getOrNull() }
                    ?.takeIf { it.isNotBlank() }
                    ?: "Checkout failed (code ${response.code()}). Please try again."
                error(userMessage)
            }
            val body = response.body()
            Log.d("CheckoutSubmit", "Response body: ${gson.toJson(body)}")
            body ?: error("API response error: empty response body.")
        }.onFailure { error ->
            if (error is CancellationException) throw error
            when (error) {
                is IOException -> Log.e("CheckoutSubmit", "Network error during checkout submission", error)
                is SerializationException -> Log.e("CheckoutSubmit", "Serialization error during checkout submission", error)
                else -> Log.e("CheckoutSubmit", "Unexpected checkout submission error", error)
            }
        }
    }
}
