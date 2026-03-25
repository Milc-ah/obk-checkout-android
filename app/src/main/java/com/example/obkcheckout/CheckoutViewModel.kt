package com.example.obkcheckout

import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.obkcheckout.Entities.Container
import com.example.obkcheckout.Entities.Organization
import kotlinx.coroutines.launch
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.decodeFromJsonElement

/**
 * Parsed fields from a tote QR code.
 * Format (one field per line):
 *   Company Name
 *   Batch ID
 *   Meals in batch
 *   Tote ID
 *   Meals in tote
 *   Checked in date (UTC)
 */
internal data class ParsedQrCode(
    val companyName: String,
    val batchId: String,
    val mealsInBatch: Int,
    val toteId: String,
    val mealsInTote: Int,
    val checkedInDate: String
)

/**
 * Tries to parse a raw QR string as the 6-field tote QR format.
 * Tries newline delimiters first (multi-line QR), then comma/pipe/semicolon/tab.
 * Returns null if the string doesn't match the expected format.
 */
internal fun parseQrCode(raw: String): ParsedQrCode? {
    val trimmed = raw.trim()
    for (delimiter in listOf("\r\n", "\n", ",", "|", ";", "\t")) {
        val parts = trimmed.split(delimiter)
        if (parts.size == 6) {
            val mealsInBatch = parts[2].trim().toIntOrNull() ?: continue
            val toteId = parts[3].trim()
            val mealsInTote = parts[4].trim().toIntOrNull() ?: continue
            if (toteId.isBlank()) continue
            return ParsedQrCode(
                companyName = parts[0].trim(),
                batchId = parts[1].trim(),
                mealsInBatch = mealsInBatch,
                toteId = toteId,
                mealsInTote = mealsInTote,
                checkedInDate = parts[5].trim()
            )
        }
    }
    return null
}

/**
 * Normalises a raw QR / barcode string to a plain tote ID.
 */
internal fun normalizeToToteId(rawValue: String): String {
    parseQrCode(rawValue)?.let { return it.toteId }
    val trimmed = rawValue.trim()
    val cleaned = trimmed.removePrefix("#").trim()
    val digits = cleaned.takeWhile { it.isDigit() }
    return if (digits.isNotBlank()) digits else cleaned
}

class CheckoutViewModel : ViewModel() {

    private val api = QairosRetrofit.api
    private val json = Json { ignoreUnknownKeys = true }

    private val _selectedCharity = mutableStateOf("Select")
    private val _splitEnabled = mutableStateOf(false)
    private val _assignedByToteId = mutableStateOf<Map<String, String>>(emptyMap())
    private val _contactDetails = mutableStateOf(SavedContact())
    private val _reviewSummary = mutableStateOf<ReviewSummary?>(null)
    private val _finalCheckoutPayload = mutableStateOf<FinalCheckoutPayload?>(null)
    private val _confirmationId = mutableStateOf<String?>(null)
    private val _charityNames = mutableStateOf<List<String>>(emptyList())
    private val _scannedByCompany = mutableStateOf<Map<String, List<String>>>(emptyMap())

    val selectedCharity: String get() = _selectedCharity.value
    val splitEnabled: Boolean get() = _splitEnabled.value
    val assignedByToteId: Map<String, String> get() = _assignedByToteId.value
    val contactDetails: SavedContact get() = _contactDetails.value
    val reviewSummary: ReviewSummary? get() = _reviewSummary.value
    val finalCheckoutPayload: FinalCheckoutPayload? get() = _finalCheckoutPayload.value
    val confirmationId: String? get() = _confirmationId.value
    val charityNames: List<String> get() = _charityNames.value
    val scannedByCompany: Map<String, List<String>> get() = _scannedByCompany.value

    private val toteIds = mutableListOf<String>()
    private val mealsByToteId = mutableMapOf<String, Int>()

    fun setSelectedCharity(charity: String) {
        _selectedCharity.value = charity
        if (!_splitEnabled.value) rebuildDerivedCheckoutState()
    }

    fun setSplitEnabled(enabled: Boolean) {
        _splitEnabled.value = enabled
        rebuildDerivedCheckoutState()
    }

    fun setAssignedByToteId(map: Map<String, String>) {
        _assignedByToteId.value = map
            .filterKeys { toteIds.contains(it) }
            .filterValues { it.isNotBlank() && it != "Select" }
        rebuildDerivedCheckoutState()
    }

    fun setConfirmationId(id: String?) {
        _confirmationId.value = id
    }

    fun addManualToteId(rawValue: String) = addTote(rawValue)

    fun addScannedId(rawValue: String) = addTote(rawValue)

    fun addTote(rawValue: String) {
        viewModelScope.launch {
            val resolved = resolveTote(rawValue) ?: return@launch
            if (toteIds.contains(resolved.toteId)) return@launch

            toteIds.add(resolved.toteId)
            mealsByToteId[resolved.toteId] = resolved.mealsInTote
            addToteToCompany(resolved.companyName, resolved.toteId)
            rebuildDerivedCheckoutState()
        }
    }

    /** Fetches the charity list from the server. */
    fun loadCharities() {
        viewModelScope.launch {
            try {
                val parameters = mapOf("where" to "OrganizationTypeId=4")
                val response = api.getRecords(
                    TokenStore.bearerToken,
                    Organization::class.simpleName ?: "Organization",
                    parameters
                )
                if (response.isSuccessful) {
                    val data = response.body()?.data
                    if (data != null) {
                        val organizations = json.decodeFromJsonElement<Array<Organization>>(data)
                        _charityNames.value = organizations
                            .map { it.Name }
                            .filter { it.isNotBlank() }
                    }
                }
            } catch (_: Exception) {
                // Leave charityNames empty; UI shows loading state.
            }
        }
    }

    /** POSTs the completed checkout to the server. */
    fun submitCheckout(onSuccess: () -> Unit) {
        viewModelScope.launch {
            try {
                val response = api.confirmCheckout(
                    TokenStore.bearerToken,
                    ConfirmCheckoutRequest(sessionId = "")
                )
                if (response.isSuccessful) {
                    _confirmationId.value = response.body()?.confirmationId
                    onSuccess()
                }
            } catch (_: Exception) {
                // Network failure; stay on review screen so the user can retry.
            }
        }
    }

    fun removeScannedId(company: String, toteId: String) {
        toteIds.remove(toteId)
        mealsByToteId.remove(toteId)
        _assignedByToteId.value = _assignedByToteId.value - toteId
        removeToteFromState(company, toteId)
        rebuildDerivedCheckoutState()
    }

    /** Saves contact details and rebuilds the review summary in one step. */
    fun buildAndSetReviewSummary(contact: SavedContact) {
        _contactDetails.value = contact
        rebuildDerivedCheckoutState()
    }

    fun rebuildReviewSummary() {
        rebuildDerivedCheckoutState()
    }

    /** Resets all session state. */
    fun reset() {
        _scannedByCompany.value = emptyMap()
        toteIds.clear()
        mealsByToteId.clear()
        _assignedByToteId.value = emptyMap()
        _splitEnabled.value = false
        _selectedCharity.value = "Select"
        _reviewSummary.value = null
        _finalCheckoutPayload.value = null
        _confirmationId.value = null
        _contactDetails.value = SavedContact()
    }

    private fun addToteToCompany(company: String, toteId: String) {
        val next = _scannedByCompany.value.toMutableMap()
        val list = next[company]?.toMutableList() ?: mutableListOf()
        if (!list.contains(toteId)) {
            list.add(toteId)
            next[company] = list
            _scannedByCompany.value = next
        }
    }

    private fun removeToteFromState(company: String, toteId: String) {
        val next = _scannedByCompany.value.toMutableMap()
        val list = next[company]?.toMutableList() ?: return
        list.remove(toteId)
        if (list.isEmpty()) next.remove(company) else next[company] = list
        _scannedByCompany.value = next
    }

    private suspend fun resolveTote(rawValue: String): ResolvedTote? {
        parseQrCode(rawValue)?.let { parsed ->
            return ResolvedTote(
                toteId = parsed.toteId,
                companyName = parsed.companyName.ifBlank { "Unknown" }.uppercase(),
                mealsInTote = parsed.mealsInTote
            )
        }

        val parsedContainerId = rawValue
            .takeIf { it.trim().startsWith("{") }
            ?.let { rawJson ->
                runCatching { json.decodeFromString<Container>(rawJson) }
                    .getOrNull()
                    ?.ContainerId
            }

        val toteId = (parsedContainerId?.toString() ?: normalizeToToteId(rawValue)).trim()
        val numericId = toteId.toIntOrNull() ?: return null

        val parameters = mapOf("include" to "ItemMovements(Warehouse,Organization)")
        val response = api.getRecord(
            TokenStore.bearerToken,
            Container::class.simpleName ?: "Container",
            numericId,
            parameters
        )
        if (!response.isSuccessful) return null

        val container = json.decodeFromString<Container>(response.body() ?: return null)
        val resolvedToteId = (container.ContainerId ?: numericId).toString()
        val companyName = container.ItemMovements
            ?.firstOrNull { it.Warehouse?.WarehouseTypeId == 3 }
            ?.Organization
            ?.Name
            ?.trim()
            ?.takeIf { it.isNotBlank() }
            ?: "Unknown"

        return ResolvedTote(
            toteId = resolvedToteId,
            companyName = companyName.uppercase(),
            mealsInTote = mealsByToteId[resolvedToteId] ?: MEALS_PER_TOTE
        )
    }

    private fun rebuildDerivedCheckoutState() {
        val summary = buildReviewSummary(contact = _contactDetails.value)
        _reviewSummary.value = summary
        _finalCheckoutPayload.value = FinalCheckoutPayload(
            companies = summary.companies,
            contact = summary.contact,
            mealsGrandTotal = summary.mealsGrandTotal
        )
    }

    private fun buildReviewSummary(contact: SavedContact): ReviewSummary {
        val companies = scannedByCompany.entries.map { (company, totes) ->
            val toteAssignments = totes.map { toteId ->
                ToteCharitySummary(
                    toteId = toteId,
                    charity = when {
                        _splitEnabled.value -> _assignedByToteId.value[toteId].orEmpty()
                        _selectedCharity.value != "Select" -> _selectedCharity.value
                        else -> ""
                    }
                )
            }

            CompanySummary(
                company = company,
                toteIds = totes.toList(),
                toteAssignments = toteAssignments,
                charities = toteAssignments.map { it.charity }.filter { it.isNotBlank() }.distinct(),
                mealsTotal = totes.sumOf { mealsByToteId[it] ?: MEALS_PER_TOTE }
            )
        }

        return ReviewSummary(
            companies = companies,
            contact = contact,
            mealsGrandTotal = companies.sumOf { it.mealsTotal }
        )
    }
}

private data class ResolvedTote(
    val toteId: String,
    val companyName: String,
    val mealsInTote: Int
)
