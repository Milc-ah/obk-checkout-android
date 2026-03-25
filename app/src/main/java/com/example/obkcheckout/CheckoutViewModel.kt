package com.example.obkcheckout

import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.obkcheckout.Entities.Container
import com.example.obkcheckout.Entities.Organization
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import kotlinx.serialization.decodeFromString
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
 */
internal fun normalizeToToteId(rawValue: String): String {
    parseQrCode(rawValue)?.let { return it.toteId }
    val trimmed = rawValue.trim()
    val cleaned = trimmed.removePrefix("#").trim()
    val digits  = cleaned.takeWhile { it.isDigit() }
    return if (digits.isNotBlank()) digits else cleaned
}

/**
 * Holds all checkout session state so it survives recomposition and
 * configuration changes.
 *
 * scannedByCompany is exposed as mutableStateOf<Map> so that every
 * mutation replaces the value, guaranteeing Compose detects the change
 * and recomposes all screens that read it.
 */
class CheckoutViewModel : ViewModel() {

    private val api  = QairosRetrofit.api
    private val json = Json { ignoreUnknownKeys = true }

    // --- Observable state ---
    private val _selectedCharity  = mutableStateOf("Select")
    private val _splitEnabled     = mutableStateOf(false)
    private val _assignedByToteId = mutableStateOf<Map<String, String>>(emptyMap())
    private val _contactDetails   = mutableStateOf(SavedContact())
    private val _reviewSummary    = mutableStateOf<ReviewSummary?>(null)
    private val _confirmationId   = mutableStateOf<String?>(null)
    private val _charityNames     = mutableStateOf<List<String>>(emptyList())

    /**
     * The tote list grouped by company.
     * Stored as mutableStateOf so that replacing the value is always visible
     * to Compose regardless of how composables receive the parameter.
     */
    private val _scannedByCompany = mutableStateOf<Map<String, List<String>>>(emptyMap())

    // --- Public read surface ---
    val selectedCharity:  String                    get() = _selectedCharity.value
    val splitEnabled:     Boolean                   get() = _splitEnabled.value
    val assignedByToteId: Map<String, String>       get() = _assignedByToteId.value
    val contactDetails:   SavedContact              get() = _contactDetails.value
    val reviewSummary:    ReviewSummary?            get() = _reviewSummary.value
    val confirmationId:   String?                   get() = _confirmationId.value
    val charityNames:     List<String>              get() = _charityNames.value
    val scannedByCompany: Map<String, List<String>> get() = _scannedByCompany.value

    // Internal dedup + meal-count tracking (not observed by UI directly)
    private val toteIds       : MutableList<String> = mutableListOf()
    private val mealsByToteId : MutableMap<String, Int> = mutableMapOf()

    // --- Private state helpers ---

    /** Add a tote under [company], replacing the whole map value so Compose sees the change. */
    private fun addToteToCompany(company: String, toteId: String) {
        val next = _scannedByCompany.value.toMutableMap()
        val list = next[company]?.toMutableList() ?: mutableListOf()
        if (!list.contains(toteId)) {
            list.add(toteId)
            next[company] = list
            _scannedByCompany.value = next
        }
    }

    /** Move a tote from [fromCompany] to [toCompany], replacing the map value. */
    private fun moveTote(fromCompany: String, toCompany: String, toteId: String) {
        val next = _scannedByCompany.value.toMutableMap()
        val from = next[fromCompany]?.toMutableList()
        if (from != null) {
            from.remove(toteId)
            if (from.isEmpty()) next.remove(fromCompany) else next[fromCompany] = from
        }
        val to = next[toCompany]?.toMutableList() ?: mutableListOf()
        if (!to.contains(toteId)) to.add(toteId)
        next[toCompany] = to
        _scannedByCompany.value = next
    }

    /** Remove a tote entry, replacing the map value. */
    private fun removeToteFromState(company: String, toteId: String) {
        val next = _scannedByCompany.value.toMutableMap()
        val list = next[company]?.toMutableList() ?: return
        list.remove(toteId)
        if (list.isEmpty()) next.remove(company) else next[company] = list
        _scannedByCompany.value = next
    }

    // --- Mutators ---

    fun setSelectedCharity(charity: String)          { _selectedCharity.value  = charity }
    fun setSplitEnabled(enabled: Boolean)             { _splitEnabled.value     = enabled }
    fun setAssignedByToteId(map: Map<String, String>) { _assignedByToteId.value = map }
    fun setConfirmationId(id: String?)                { _confirmationId.value   = id }

    fun addManualToteId(numericId: String) {
        val normalizedId = numericId.trim().removePrefix("#").trim()
        if (normalizedId.isBlank()) return
        if (toteIds.contains(normalizedId)) return
        toteIds.add(normalizedId)
        addToteToCompany("Unknown", normalizedId)
        // API lookup requires a numeric ID; skip silently if not numeric
        val id = normalizedId.toIntOrNull() ?: return
        viewModelScope.launch {
            runCatching {
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
                    val company = itemMovement?.Organization?.Name?.uppercase() ?: "Unknown"
                    val toteId  = (container.ContainerId?.toInt() ?: id).toString()
                    if (company != "Unknown") moveTote("Unknown", company, toteId)
                }
            }
        }
    }

    fun addScannedId(rawQr: String) {
        val parsed = parseQrCode(rawQr)
        if (parsed != null) {
            val toteId = parsed.toteId
            if (toteIds.contains(toteId)) return
            toteIds.add(toteId)
            mealsByToteId[toteId] = parsed.mealsInTote
            addToteToCompany(parsed.companyName.uppercase(), toteId)
        } else {
            val toteId = normalizeToToteId(rawQr)
            if (toteId.isBlank() || toteIds.contains(toteId)) return
            toteIds.add(toteId)
            addToteToCompany("Unknown", toteId)
            val id = toteId.toIntOrNull() ?: return
            viewModelScope.launch {
                runCatching {
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
                        val company      = itemMovement?.Organization?.Name?.uppercase() ?: "Unknown"
                        val actualToteId = (container.ContainerId?.toInt() ?: id).toString()
                        if (company != "Unknown") moveTote("Unknown", company, actualToteId)
                    }
                }
            }
        }
    }

    fun deleteScannedId(company: String, toteId: String) {
        toteIds.remove(toteId)
        mealsByToteId.remove(toteId)
        removeToteFromState(company, toteId)
    }

    fun removeScannedId(company: String, toteId: String) {
        toteIds.remove(toteId)
        mealsByToteId.remove(toteId)
        removeToteFromState(company, toteId)
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
                        _charityNames.value =
                            organizations.map { it.Name }.filter { it.isNotBlank() }
                    }
                }
            } catch (_: Exception) { }
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
            } catch (_: Exception) { }
        }
    }

    fun buildAndSetReviewSummary(contact: SavedContact) {
        _contactDetails.value = contact
        _reviewSummary.value  = buildReviewSummary(contact = contact)
    }

    fun rebuildReviewSummary() {
        _reviewSummary.value = buildReviewSummary(contact = _contactDetails.value)
    }

    /** Resets all session state. */
    fun reset() {
        _scannedByCompany.value = emptyMap()
        toteIds.clear()
        mealsByToteId.clear()
        _assignedByToteId.value = emptyMap()
        _splitEnabled.value     = false
        _selectedCharity.value  = "Select"
        _reviewSummary.value    = null
        _confirmationId.value   = null
        _contactDetails.value   = SavedContact()
    }

    // --- Private helpers ---

    private fun buildReviewSummary(contact: SavedContact): ReviewSummary {
        val companies = _scannedByCompany.value.entries.map { (company, totes) ->
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
