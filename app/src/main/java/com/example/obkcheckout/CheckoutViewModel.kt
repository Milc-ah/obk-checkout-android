package com.example.obkcheckout

import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.obkcheckout.Entities.Container
import com.example.obkcheckout.Entities.Organization
import com.google.gson.JsonObject
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.decodeFromJsonElement

/**
 * Normalises a raw QR / barcode string to a plain tote ID.
 * Exposed as internal so unit tests can verify it directly.
 */
internal fun normalizeToToteId(rawValue: String): String {
    val trimmed = rawValue.trim()
    val cleaned = trimmed.removePrefix("#").trim()
    val digits  = cleaned.takeWhile { it.isDigit() }
    return if (digits.isNotBlank()) digits else cleaned
}

/**
 * Holds all checkout session state so it survives recomposition and
 * configuration changes (e.g. screen rotation).
 *
 * State is exposed via read-only properties backed by [mutableStateOf] so
 * Compose can observe changes. Writes go through the explicit mutator functions.
 *
 * BACKEND: Replace [PlaceholderCompanyResolver] with a real API lookup
 * once the endpoint is available.
 */
class CheckoutViewModel : ViewModel() {

    private val api = QairosRetrofit.api

    // Backing state — private write, public read via property below
    private val _selectedCharity  = mutableStateOf("Select")
    private val _splitEnabled     = mutableStateOf(false)
    private val _assignedByToteId = mutableStateOf<Map<String, String>>(emptyMap())
    private val _contactDetails   = mutableStateOf(SavedContact())
    private val _reviewSummary    = mutableStateOf<ReviewSummary?>(null)
    private val _confirmationId   = mutableStateOf<String?>(null)
    private val _charityNames     = mutableStateOf<List<String>>(emptyList())

    private val json = Json{ ignoreUnknownKeys = true }

    // Read-only public surface — Compose observes reads through mutableStateOf
    val selectedCharity:  String              get() = _selectedCharity.value
    val splitEnabled:     Boolean             get() = _splitEnabled.value
    val assignedByToteId: Map<String, String> get() = _assignedByToteId.value
    val contactDetails:   SavedContact        get() = _contactDetails.value
    val reviewSummary:    ReviewSummary?      get() = _reviewSummary.value
    val confirmationId:   String?             get() = _confirmationId.value
    val charityNames:     List<String>        get() = _charityNames.value

    val scannedByCompany = mutableStateMapOf<String, MutableList<String>>()
    private val toteIds : MutableList<String> = mutableListOf()

    // --- Mutators -----------------------------------------------------------

    fun setSelectedCharity(charity: String)          { _selectedCharity.value  = charity }
    fun setSplitEnabled(enabled: Boolean)             { _splitEnabled.value     = enabled }
    fun setAssignedByToteId(map: Map<String, String>) { _assignedByToteId.value = map }
    fun setConfirmationId(id: String?)                { _confirmationId.value   = id }

    fun addScannedId(toteQRCode: String) {
        viewModelScope.launch {
            // Tote IDs on this system are numeric strings — convert for the API path.

            if(!toteIds.contains(toteQRCode)) {
                toteIds.add(toteQRCode);
                var container: Container

                if (toteQRCode.isNotEmpty()) {
                    container = json.decodeFromString<Container>(toteQRCode)

                    val parameters = mapOf("include" to "ItemMovements(Warehouse,Organization)")

                    val response = api.getRecord(
                        TokenStore.bearerToken,
                        Container.Companion::class.java.name.split('.', '$')
                            .takeWhile { it != "Companion" }.last(),
                        (container.ContainerId ?: 0),
                        parameters
                    )

                    if (response.isSuccessful) {
                        container = json.decodeFromString<Container>(response.body() ?: "")

                        val itemMovement =
                            container.ItemMovements?.firstOrNull { it.Warehouse?.WarehouseTypeId == 3 }

                        if (itemMovement != null) {
                            val key = itemMovement.Organization?.Name?.uppercase() ?: "Unknow"
                            val list = scannedByCompany.getOrPut(key) { mutableListOf() }
                            val toteId = (container.ContainerId?.toInt() ?: 0).toString();
                            if (!list.contains(toteId)) list.add(toteId)
                        } else {
                        }
                    } else {
                    }
                } else {
                }
            }
            else { }
        }
    }

    fun deleteScannedId(company: String, toteId: String){
        if(toteIds.contains(toteId)) {
            toteIds.remove(toteId)
            scannedByCompany[company]?.remove(toteId)
            if (scannedByCompany[company]?.isEmpty() == true) scannedByCompany.remove(company)
        }
        else { }
    }

    /** Fetches the charity list from the server and stores it as display names. */
    fun loadCharities() {
        viewModelScope.launch {
            try {
                val parameters = mapOf("where" to "OrganizationTypeId=4")

                val response = api.getRecords(
                    TokenStore.bearerToken,
                    Organization.Companion::class.java.name.split('.', '$').takeWhile{it != "Companion"}.last(),
                    parameters
                )

                if (response.isSuccessful) {
                    val data = response.body()?.data;

                    if(data != null){
                        var organization = json.decodeFromJsonElement<Array<Organization>>(data)
                        _charityNames.value =
                            organization?.map { it.Name }
                            ?.filter { it.isNotBlank() }
                            ?: emptyList()
                    }
                    else { }
                }
            } catch (_: Exception) {
                // Leave charityNames empty; UI shows "Loading charities…"
            }
        }
    }

    /** POSTs the completed checkout to the server. Navigates via [onSuccess] on 2xx. */
    fun submitCheckout(onSuccess: () -> Unit) {
        viewModelScope.launch {
            try {
                val response = api.confirmCheckout(
                    TokenStore.bearerToken,
                    ConfirmCheckoutRequest(sessionId = "")   // TODO: track server session ID
                )
                if (response.isSuccessful) {
                    _confirmationId.value = response.body()?.confirmationId
                    onSuccess()
                }
            } catch (_: Exception) {
                // Network failure — stay on review screen so the user can retry
            }
        }
    }

    fun removeScannedId(company: String, toteId: String) {
        scannedByCompany[company]?.remove(toteId)
        if (scannedByCompany[company]?.isEmpty() == true) scannedByCompany.remove(company)
    }

    /** Saves contact details and rebuilds the review summary in one step. */
    fun buildAndSetReviewSummary(contact: SavedContact) {
        _contactDetails.value = contact
        _reviewSummary.value  = buildReviewSummary(contact = contact)
    }

    /**
     * Rebuilds the review summary from current state (e.g. after editing
     * charities from the review screen).
     */
    fun rebuildReviewSummary() {
        _reviewSummary.value = buildReviewSummary(contact = _contactDetails.value)
    }

    /** Resets all session state so the app is ready for the next checkout. */
    fun reset() {
        scannedByCompany.clear()
        _assignedByToteId.value = emptyMap()
        _splitEnabled.value     = false
        _selectedCharity.value  = "Select"
        _reviewSummary.value    = null
        _confirmationId.value   = null
        _contactDetails.value   = SavedContact()
    }

    // --- Private helpers ----------------------------------------------------

    private fun buildReviewSummary(contact: SavedContact): ReviewSummary {
        val companies = scannedByCompany.entries.map { (company, totes) ->
            val charitiesForCompany =
                if (!_splitEnabled.value) {
                    listOf(_selectedCharity.value).filter { it != "Select" }
                } else {
                    totes.mapNotNull { _assignedByToteId.value[it] }
                        .filter { it != "Select" }
                        .distinct()
                }
            CompanySummary(
                company    = company,
                toteIds    = totes.toList(),
                charities  = charitiesForCompany,
                mealsTotal = totes.size * MEALS_PER_TOTE
            )
        }
        return ReviewSummary(
            companies       = companies,
            contact         = contact,
            mealsGrandTotal = companies.sumOf { it.mealsTotal }
        )
    }
}

