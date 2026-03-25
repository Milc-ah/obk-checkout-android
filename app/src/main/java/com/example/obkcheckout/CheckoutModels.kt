package com.example.obkcheckout

const val MEALS_PER_TOTE = 36

// ----------------------------
// Core UI/domain models
// ----------------------------

data class SavedContact(
    val phone: String = "",
    val fullName: String = "",
    val email: String = "",
    val role: String = ""
)

data class CompanySummary(
    val company: String,
    val toteIds: List<String>,
    val toteAssignments: List<ToteCharitySummary>,
    val charities: List<String>,
    val mealsTotal: Int
)

data class ToteCharitySummary(
    val toteId: String,
    val charity: String
)

data class ReviewSummary(
    val companies: List<CompanySummary>,
    val contact: SavedContact,
    val mealsGrandTotal: Int
)

data class FinalCheckoutPayload(
    val companies: List<CompanySummary>,
    val contact: SavedContact,
    val mealsGrandTotal: Int
)

// ----------------------------
// Checkout session state
// ----------------------------

data class CharityOption(
    val id: String,
    val name: String
)

data class ToteAssignment(
    val toteId: String,
    val charityId: String
)

data class CheckoutSession(
    val toteIds: List<String> = emptyList(),
    val charityOptions: List<CharityOption> = emptyList(),
    val assignments: List<ToteAssignment> = emptyList(),
    val contact: SavedContact = SavedContact(),
    val isLoading: Boolean = false,
    val errorMessage: String? = null
)

// ----------------------------
// Backend integration DTOs
// (Backend dev can map these to endpoints)
// ----------------------------

data class StartCheckoutRequest(
    val toteIds: List<String>
)

data class StartCheckoutResponse(
    val sessionId: String,
    val companyByToteId: Map<String, String> = emptyMap(),
    val availableCharities: List<CharityOption> = emptyList()
)

data class SubmitContactRequest(
    val sessionId: String,
    val contact: SavedContact
)

data class SubmitAssignmentsRequest(
    val sessionId: String,
    val assignments: List<ToteAssignment>
)

data class ConfirmCheckoutRequest(
    val sessionId: String
)

data class ConfirmCheckoutResponse(
    val confirmationId: String,
    val mealsGrandTotal: Int,
    val reviewSummary: ReviewSummary? = null
)

data class ContainerLookupResponse(
    val id: Int,
    val name: String,
    val company: String
)

// ----------------------------
// Generic networking helpers
// ----------------------------

sealed class ApiResult<out T> {
    data class Success<T>(val data: T) : ApiResult<T>()
    data class Failure(val error: ApiError) : ApiResult<Nothing>()
}

data class ApiError(
    val code: String = "UNKNOWN",
    val message: String = "Something went wrong"
)
