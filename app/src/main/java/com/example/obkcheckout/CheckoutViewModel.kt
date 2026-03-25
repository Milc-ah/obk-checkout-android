package com.example.obkcheckout

import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.obkcheckout.Entities.Container
import com.example.obkcheckout.Entities.Organization
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.decodeFromJsonElement

/**
 * Parsed fields from a tote QR code.
 * Format: Company Name,Batch ID,Meals in batch,Tote ID,Meals in tote,Checked in date (UTC)
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
 * Attempts comma, pipe, semicolon, and tab as delimiters.
 * Returns null if the string doesn't match the expected format.
 */
internal fun parseQrCode(raw: String): ParsedQrCode? {
    val trimmed = raw.trim()
    for (delimiter in listOf(",", "|", ";", "\t")) {
        val parts = trimmed.split(delimiter)
        if (parts.size == 6) {
            val mealsInBatch = parts[2].trim().toIntOrNull() ?: continue
            val toteId       = parts[3].trim()
            val mealsInTote  = parts[4].trim().toIntOrNull() ?: continue
            if (toteId.isBlank()) continue
            return ParsedQrCode(
                companyName   = parts[0].trim(),
                batchId       = parts[1].trim(),
                mealsInBatch  = mealsInBatch,
                toteId        = toteId,
                mealsInTote   = mealsInTote,
                checkedInDate = parts[5].trim()
            )
        }
    }
    return null
}

/**
 * Normalises a raw QR / barcode string to a plain tote ID.
 * Exposed as internal so unit tests can verify it directly.
 */
internal fun normalizeToToteId(rawValue: String): String {
    // Try the full 6-field QR format first
    parseQrCode(rawValue)?.let { return it.toteId }
    // Fall back to stripping leading "#" and taking leading digits
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
    /** Per-tote meal counts parsed from QR data; falls back to MEALS_PER_TOTE if absent. */
    private val mealsByToteId : MutableMap<String, Int> = mutableMapOf()

    // --- Mutators -----------------------------------------------------------

    fun setSelectedCharity(charity: String)          { _selectedCharity.value  = charity }
    fun setSplitEnabled(enabled: Boolean)             { _splitEnabled.value     = enabled }
    fun setAssignedByToteId(map: Map<String, String>) { _assignedByToteId.value = map }
    fun setConfirmationId(id: String?)                { _confirmationId.value   = id }

    fun addManualToteId(numericId: String) {
        val trimmed = numericId.trim().removePrefix("#").trim()
        val id = trimmed.toIntOrNull() ?: return
        val normalizedId = id.toString()
        viewModelScope.launch {
            if (toteIds.contains(normalizedId)) return@launch
            toteIds.add(normalizedId)
            val parameters = mapOf("include" to "ItemMovements(Warehouse,Organization)")
            val response = api.getRecord(
                TokenStore.bearerToken,
                Container::class.simpleName ?: "Container",
                id,
                parameters
            )
            if (response.isSuccessful) {
                val container = json.decodeFromString<Container>(response.body() ?: "")
                val itemMovement =
                    container.ItemMovements?.firstOrNull { it.Warehouse?.WarehouseTypeId == 3 }
                if (itemMovement != null) {
                    val key = itemMovement.Organization?.Name?.uppercase() ?: "Unknown"
                    val list = scannedByCompany.getOrPut(key) { mutableListOf() }
                    val toteId = (container.ContainerId?.toInt() ?: id).toString()
                    if (!list.contains(toteId)) list.add(toteId)
                }
            }
        }
    }

    fun addScannedId(rawQr: String) {
        viewModelScope.launch {
            val parsed = parseQrCode(rawQr)
            if (parsed != null) {
                // Full QR format — extract company and meals directly, no API call needed
                val toteId = parsed.toteId
                if (toteIds.contains(toteId)) return@launch
                toteIds.add(toteId)
                mealsByToteId[toteId] = parsed.mealsInTote
                val company = parsed.companyName.uppercase()
                val list = scannedByCompany.getOrPut(company) { mutableListOf() }
                if (!list.contains(toteId)) list.add(toteId)
            } else {
                // Fall back: treat as plain numeric barcode and look up via API
                val toteId = normalizeToToteId(rawQr)
                if (toteId.isBlank() || toteIds.contains(toteId)) return@launch
                toteIds.add(toteId)
                val id = toteId.toIntOrNull() ?: return@launch
                val parameters = mapOf("include" to "ItemMovements(Warehouse,Organization)")
                val response = api.getRecord(
                    TokenStore.bearerToken,
                    Container::class.simpleName ?: "Container",
                    id,
                    parameters
                )
                if (response.isSuccessful) {
                    val container = json.decodeFromString<Container>(response.body() ?: "")
                    val itemMovement =
                        container.ItemMovements?.firstOrNull { it.Warehouse?.WarehouseTypeId == 3 }
                    if (itemMovement != null) {
                        val key = itemMovement.Organization?.Name?.uppercase() ?: "Unknown"
                        val list = scannedByCompany.getOrPut(key) { mutableListOf() }
                        val actualToteId = (container.ContainerId?.toInt() ?: id).toString()
                        if (!list.contains(actualToteId)) list.add(actualToteId)
                    }
                }
            }
        }
    }

    fun deleteScannedId(company: String, toteId: String) {
        toteIds.remove(toteId)
        mealsByToteId.remove(toteId)
        scannedByCompany[company]?.remove(toteId)
        if (scannedByCompany[company]?.isEmpty() == true) scannedByCompany.remove(company)
    }

    /** Fetches the charity list from the server and stores it as display names. */
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
                        _charityNames.value =
                            organizations.map { it.Name }
                                .filter { it.isNotBlank() }
                    }
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
        toteIds.remove(toteId)
        mealsByToteId.remove(toteId)
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
        toteIds.clear()
        mealsByToteId.clear()
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
                mealsTotal = totes.sumOf { mealsByToteId[it] ?: MEALS_PER_TOTE }
            )
        }
        return ReviewSummary(
            companies       = companies,
            contact         = contact,
            mealsGrandTotal = companies.sumOf { it.mealsTotal }
        )
    }
}
