package com.example.obkcheckout

import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import java.time.Instant
import kotlinx.coroutines.launch

class CheckoutViewModel(
    private val repository: CheckoutRepository = NetworkCheckoutRepository(QairosRetrofit.api)
) : ViewModel() {

    private val _selectedCharity = mutableStateOf("")
    private val _splitEnabled = mutableStateOf(false)
    private val _assignedByToteId = mutableStateOf<Map<String, String>>(emptyMap())
    private val _contactDetails = mutableStateOf(SavedContact())
    private val _lookedUpContact = mutableStateOf<SavedContact?>(null)
    private val _reviewSummary = mutableStateOf<ReviewSummary?>(null)
    private val _finalCheckoutPayload = mutableStateOf<FinalCheckoutPayload?>(null)
    private val _confirmationId = mutableStateOf<String?>(null)
    private val _charityNames = mutableStateOf<List<String>>(emptyList())
    private val _totes = mutableStateOf<List<ToteRecord>>(emptyList())
    private val _toteErrorMessage = mutableStateOf<String?>(null)
    private val _submissionErrorMessage = mutableStateOf<String?>(null)
    private val _isSubmitting = mutableStateOf(false)
    private val _isLoadingCharities = mutableStateOf(false)
    private val _charityLoadError = mutableStateOf<String?>(null)
    private val _isLookingUpContact = mutableStateOf(false)

    val selectedCharity: String get() = _selectedCharity.value
    val splitEnabled: Boolean get() = _splitEnabled.value
    val assignedByToteId: Map<String, String> get() = _assignedByToteId.value
    val contactDetails: SavedContact get() = _contactDetails.value
    val lookedUpContact: SavedContact? get() = _lookedUpContact.value
    val reviewSummary: ReviewSummary? get() = _reviewSummary.value
    val finalCheckoutPayload: FinalCheckoutPayload? get() = _finalCheckoutPayload.value
    val confirmationId: String? get() = _confirmationId.value
    val charityNames: List<String> get() = _charityNames.value
    val scannedByCompany: Map<String, List<String>>
        get() = _totes.value
            .groupBy { it.company.ifBlank { "UNKNOWN" } }
            .mapValues { (_, value) -> value.map { it.toteId } }
    val totes: List<ToteRecord> get() = _totes.value
    val toteErrorMessage: String? get() = _toteErrorMessage.value
    val submissionErrorMessage: String? get() = _submissionErrorMessage.value
    val isSubmitting: Boolean get() = _isSubmitting.value
    val isLoadingCharities: Boolean get() = _isLoadingCharities.value
    val charityLoadError: String? get() = _charityLoadError.value
    val isLookingUpContact: Boolean get() = _isLookingUpContact.value

    fun setSelectedCharity(charity: String) {
        _selectedCharity.value = charity.trim()
        if (!_splitEnabled.value) rebuildDerivedCheckoutState()
    }

    fun setSplitEnabled(enabled: Boolean) {
        _splitEnabled.value = enabled
        rebuildDerivedCheckoutState()
    }

    fun setAssignedByToteId(map: Map<String, String>) {
        val toteIds = _totes.value.map { it.toteId }.toSet()
        _assignedByToteId.value = map
            .mapValues { it.value.trim() }
            .filterKeys { toteIds.contains(it) }
            .filterValues { it.isNotBlank() }
        rebuildDerivedCheckoutState()
    }

    fun clearToteError() {
        _toteErrorMessage.value = null
    }

    fun clearSubmissionError() {
        _submissionErrorMessage.value = null
    }

    fun addManualToteEntry(toteId: String, companyName: String) {
        val trimmedToteId = toteId.trim()
        val trimmedCompany = companyName.trim()

        when {
            trimmedToteId.isBlank() -> {
                _toteErrorMessage.value = "Please enter a Tote ID."
                return
            }
            trimmedCompany.isBlank() -> {
                _toteErrorMessage.value = "Please enter a Company Name."
                return
            }
            _totes.value.any { it.toteId == trimmedToteId } -> {
                _toteErrorMessage.value = "Tote $trimmedToteId has already been added."
                return
            }
        }

        _toteErrorMessage.value = null
        _totes.value = (
            _totes.value + ToteRecord(
                toteId = trimmedToteId,
                company = trimmedCompany.uppercase(),
                mealsInTote = MEALS_PER_TOTE,
                source = ToteSource.MANUAL,
                isResolved = true
            )
        ).sortedWith(compareBy<ToteRecord> { it.company }.thenBy { it.toteId })
        rebuildDerivedCheckoutState()
    }

    fun addManualToteId(rawValue: String) = addTote(rawValue, ToteSource.MANUAL)

    fun addScannedId(rawValue: String) = addTote(rawValue, ToteSource.SCANNED)

    fun loadCharities() {
        _isLoadingCharities.value = true
        _charityLoadError.value = null
        viewModelScope.launch {
            repository.loadCharityNames()
                .onSuccess {
                    _charityNames.value = it
                    _isLoadingCharities.value = false
                }
                .onFailure {
                    _isLoadingCharities.value = false
                    _charityLoadError.value = "Unable to load charities. Tap Retry to try again."
                }
        }
    }

    fun retryLoadCharities() = loadCharities()

    fun lookupContactByEmail(email: String) {
        _isLookingUpContact.value = true
        viewModelScope.launch {
            repository.lookupContactByEmail(email)
                .onSuccess { contact ->
                    _lookedUpContact.value = contact
                    _isLookingUpContact.value = false
                }
                .onFailure {
                    _lookedUpContact.value = null
                    _isLookingUpContact.value = false
                }
        }
    }

    fun removeScannedId(company: String, toteId: String) {
        _totes.value = _totes.value.filterNot { it.toteId == toteId && it.company == company }
        _assignedByToteId.value = _assignedByToteId.value - toteId
        rebuildDerivedCheckoutState()
    }

    fun updateTote(
        currentCompany: String,
        currentToteId: String,
        newToteId: String,
        newCompany: String
    ): String? {
        val trimmedToteId = newToteId.trim()
        val trimmedCompany = newCompany.trim()

        when {
            trimmedToteId.isBlank() -> return "Please enter a Tote ID."
            trimmedCompany.isBlank() -> return "Please enter a Company Name."
            _totes.value.any { it.toteId == trimmedToteId && it.toteId != currentToteId } ->
                return "Tote $trimmedToteId has already been added."
        }

        val exists = _totes.value.any {
            it.toteId == currentToteId && it.company == currentCompany
        }
        if (!exists) return "Unable to update that tote."

        _totes.value = _totes.value.map { tote ->
            if (tote.toteId == currentToteId && tote.company == currentCompany) {
                tote.copy(
                    toteId = trimmedToteId,
                    company = trimmedCompany.uppercase(),
                    mealsInTote = MEALS_PER_TOTE
                )
            } else {
                tote
            }
        }.sortedWith(compareBy<ToteRecord> { it.company }.thenBy { it.toteId })

        val existingAssignment = _assignedByToteId.value[currentToteId]
        _assignedByToteId.value = (_assignedByToteId.value - currentToteId).let { current ->
            if (existingAssignment.isNullOrBlank()) current else current + (trimmedToteId to existingAssignment)
        }

        _toteErrorMessage.value = null
        rebuildDerivedCheckoutState()
        return null
    }

    fun buildAndSetReviewSummary(contact: SavedContact) {
        _contactDetails.value = contact
        rebuildDerivedCheckoutState()
    }

    fun rebuildReviewSummary() {
        rebuildDerivedCheckoutState()
    }

    fun submitCheckout(onSuccess: () -> Unit) {
        val payload = _finalCheckoutPayload.value ?: run {
            _submissionErrorMessage.value = "Checkout payload is incomplete."
            return
        }
        _isSubmitting.value = true
        _submissionErrorMessage.value = null
        viewModelScope.launch {
            repository.submitCheckout(
                CheckoutSubmission(
                    totes = _totes.value.map { tote ->
                        CheckoutSubmissionTote(
                            toteId = tote.toteId,
                            companyName = tote.company,
                            charityName = when {
                                _splitEnabled.value -> _assignedByToteId.value[tote.toteId].orEmpty()
                                _selectedCharity.value.isNotBlank() -> _selectedCharity.value
                                else -> ""
                            },
                            meals = MEALS_PER_TOTE
                        )
                    },
                    companies = payload.companies,
                    contact = payload.contact,
                    mealsGrandTotal = payload.mealsGrandTotal,
                    submittedAtUtc = Instant.now().toString(),
                    operatorId = TokenStore.token.takeIf { it.isNotBlank() }
                )
            ).onSuccess { response ->
                _confirmationId.value = response.confirmationId
                _reviewSummary.value = buildReviewSummary(_contactDetails.value)
                _finalCheckoutPayload.value = FinalCheckoutPayload(
                    companies = _reviewSummary.value?.companies.orEmpty(),
                    contact = _reviewSummary.value?.contact ?: _contactDetails.value,
                    mealsGrandTotal = _reviewSummary.value?.mealsGrandTotal ?: payload.mealsGrandTotal
                )
                onSuccess()
            }.onFailure { error ->
                _submissionErrorMessage.value = error.message
                    ?: "Something went wrong while submitting checkout. Please try again."
            }
            _isSubmitting.value = false
        }
    }

    fun reset() {
        _totes.value = emptyList()
        _assignedByToteId.value = emptyMap()
        _splitEnabled.value = false
        _selectedCharity.value = ""
        _reviewSummary.value = null
        _finalCheckoutPayload.value = null
        _confirmationId.value = null
        _contactDetails.value = SavedContact()
        _lookedUpContact.value = null
        _toteErrorMessage.value = null
        _submissionErrorMessage.value = null
        _isSubmitting.value = false
        _isLookingUpContact.value = false
    }

    private fun addTote(rawValue: String, source: ToteSource) {
        val trimmed = rawValue.trim()
        if (trimmed.isBlank()) {
            _toteErrorMessage.value = "Please enter a Tote ID."
            return
        }

        viewModelScope.launch {
            repository.resolveTote(trimmed, source)
                .onSuccess { tote ->
                    if (_totes.value.any { it.toteId == tote.toteId }) {
                        _toteErrorMessage.value = "Tote ${tote.toteId} has already been added."
                        return@onSuccess
                    }
                    _toteErrorMessage.value = null
                    _totes.value = (_totes.value + tote).sortedWith(
                        compareBy<ToteRecord> { it.company }.thenBy { it.toteId }
                    )
                    rebuildDerivedCheckoutState()
                }
                .onFailure { error ->
                    _toteErrorMessage.value = error.message ?: "Unable to add tote."
                }
        }
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
        val companies = _totes.value
            .groupBy { it.company.ifBlank { "UNKNOWN" } }
            .toSortedMap()
            .map { (company, totes) ->
                val toteAssignments = totes.map { tote ->
                    ToteCharitySummary(
                        toteId = tote.toteId,
                        charity = when {
                            _splitEnabled.value -> _assignedByToteId.value[tote.toteId].orEmpty()
                            _selectedCharity.value.isNotBlank() -> _selectedCharity.value
                            else -> ""
                        }
                    )
                }

                CompanySummary(
                    company = company,
                    toteIds = totes.map { it.toteId },
                    toteAssignments = toteAssignments,
                    charities = toteAssignments.map { it.charity }.filter { it.isNotBlank() }.distinct(),
                    mealsTotal = totes.sumOf { it.mealsInTote }
                )
            }

        return ReviewSummary(
            companies = companies,
            contact = contact,
            mealsGrandTotal = companies.sumOf { it.mealsTotal }
        )
    }
}
//