package com.example.obkcheckout

import com.google.gson.annotations.SerializedName
import java.util.Locale
import kotlinx.serialization.Serializable

const val MEALS_PER_TOTE = 36

// ----------------------------
// Core UI/domain models
// ----------------------------

@Serializable
data class SavedContact(
    val phone: String = "",
    val fullName: String = "",
    val email: String = "",
    val role: String = ""
)

enum class ToteSource {
    MANUAL,
    SCANNED
}

@Serializable
data class ToteRecord(
    val toteId: String,
    val company: String,
    val mealsInTote: Int = MEALS_PER_TOTE,
    val source: ToteSource,
    val isResolved: Boolean = true
)

@Serializable
data class CompanySummary(
    val company: String,
    val toteIds: List<String>,
    val toteAssignments: List<ToteCharitySummary>,
    val charities: List<String>,
    val mealsTotal: Int
)

@Serializable
data class ToteCharitySummary(
    val toteId: String,
    val charity: String
)

@Serializable
data class ReviewSummary(
    val companies: List<CompanySummary>,
    val contact: SavedContact,
    val mealsGrandTotal: Int
)

fun groupedTotesByCompany(scannedByCompany: Map<String, List<String>>): List<CompanyTotesGroup> =
    scannedByCompany.entries
        .sortedBy { it.key }
        .map { (company, toteIds) ->
            CompanyTotesGroup(
                company = company.ifBlank { "UNKNOWN" },
                toteIds = toteIds.distinct()
            )
        }

data class CompanyTotesGroup(
    val company: String,
    val toteIds: List<String>
)

@Serializable
data class FinalCheckoutPayload(
    val companies: List<CompanySummary>,
    val contact: SavedContact,
    val mealsGrandTotal: Int
)

@Serializable
data class CheckoutSubmission(
    val totes: List<CheckoutSubmissionTote>,
    val companies: List<CompanySummary>,
    val contact: SavedContact,
    val mealsGrandTotal: Int,
    val submittedAtUtc: String,
    val operatorId: String? = null
)

@Serializable
data class CheckoutSubmissionTote(
    val toteId: String,
    val companyName: String,
    val charityName: String,
    val meals: Int = MEALS_PER_TOTE
)

@Serializable
data class ConfirmCheckoutRequest(
    val sessionId: String = "",
    val totes: List<CheckoutSubmissionTote> = emptyList(),
    val companies: List<CompanySummary> = emptyList(),
    val contact: SavedContact = SavedContact(),
    val mealsGrandTotal: Int = 0,
    val submittedAtUtc: String = "",
    val operatorId: String? = null
)

@Serializable
data class ConfirmCheckoutResponse(
    @SerializedName(value = "confirmationId", alternate = ["confirmation_id", "ConfirmationId"])
    val confirmationId: String = "",
    @SerializedName(value = "mealsGrandTotal", alternate = ["meals_grand_total", "MealsGrandTotal"])
    val mealsGrandTotal: Int = 0,
    val reviewSummary: ReviewSummary? = null
)

fun normalizeCharityName(raw: String): String {
    val spaced = raw
        .trim()
        .replace(Regex("([a-z])([A-Z])"), "$1 $2")
        .replace(Regex("\\s+"), " ")

    return spaced.split(" ")
        .filter { it.isNotBlank() }
        .joinToString(" ") { token ->
            token.lowercase(Locale.getDefault()).replaceFirstChar { first ->
                if (first.isLowerCase()) first.titlecase(Locale.getDefault()) else first.toString()
            }
        }
}

fun formatCharityList(charities: List<String>): String {
    val cleaned = charities
        .map { normalizeCharityName(it) }
        .filter { it.isNotBlank() }
        .distinct()
    return if (cleaned.isEmpty()) "Not assigned" else cleaned.joinToString(" and ")
}

//