package com.example.obkcheckout

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Badge
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val ReviewBg = Color(0xFFF5F7F1)
private val ReviewText = Color(0xFF172114)
private val ReviewMuted = Color(0xFF556150)
private val ReviewCard = Color.White
private val ToteChip = Color(0xFFF59E0B)
private val ToteChipText = Color(0xFF4A2D00)
private val MessageBg = Color(0xFFFFF7E8)
private val MessageText = Color(0xFF6C5314)
@Composable
fun ReviewScreen(
    summary: ReviewSummary,
    submissionErrorMessage: String?,
    isSubmitting: Boolean,
    onDismissSubmissionError: () -> Unit,
    onBack: () -> Unit,
    onEditCharities: () -> Unit,
    onEditContact: () -> Unit,
    onEditTote: (company: String, oldToteId: String, newToteId: String, newCompany: String) -> String?,
    onRemoveTote: (company: String, toteId: String) -> Unit,
    onContinue: () -> Unit,
    onCheckoutMoreMeals: (() -> Unit)? = null
) {
    var editingTote by remember { mutableStateOf<Pair<String, String>?>(null) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(ReviewBg)
    ) {
        OBKHeader(onBack = onBack)

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp, vertical = 14.dp)
        ) {
            Text(
                text = "Review",
                fontSize = 28.sp,
                fontWeight = FontWeight.ExtraBold,
                color = ReviewText
            )

            Spacer(Modifier.height(6.dp))

            Text(
                text = "Check the details below before submitting checkout.",
                style = MaterialTheme.typography.bodyMedium,
                color = ReviewMuted
            )

            Spacer(Modifier.height(12.dp))

            ReviewHeroCard(
                totalMeals = summary.mealsGrandTotal,
                toteCount = summary.companies.sumOf { it.toteIds.size }
            )

            Spacer(Modifier.height(12.dp))

            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                contentPadding = PaddingValues(bottom = 12.dp)
            ) {
                items(summary.companies, key = { it.company }) { company ->
                    CompanyReviewCard(
                        company = company,
                        onEditCharities = onEditCharities,
                        onEditTote = { companyName, toteId -> editingTote = companyName to toteId },
                        onRemoveTote = onRemoveTote
                    )
                }

                item {
                    ContactReviewBlock(
                        contact = summary.contact,
                        onEditContact = onEditContact
                    )
                }
            }

            if (!submissionErrorMessage.isNullOrBlank()) {
                SubmissionMessageCard(message = submissionErrorMessage)
                Spacer(Modifier.height(10.dp))
            }

            onCheckoutMoreMeals?.let {
                OutlinedButton(
                    onClick = it,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Text("Add More Meals")
                }

                Spacer(Modifier.height(10.dp))
            }

            Button(
                onClick = {
                    onDismissSubmissionError()
                    onContinue()
                },
                enabled = !isSubmitting,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                shape = RoundedCornerShape(16.dp)
            ) {
                Text(
                    text = if (isSubmitting) "Submitting..." else "Submit Checkout",
                    color = MaterialTheme.colorScheme.onPrimary,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
            }
        }
    }

    editingTote?.let { (company, toteId) ->
        EditToteDialog(
            company = company,
            toteId = toteId,
            onDismiss = { editingTote = null },
            onSave = { newToteId, newCompany ->
                onEditTote(company, toteId, newToteId, newCompany)
            }
        )
    }
}

@Composable
private fun ReviewHeroCard(totalMeals: Int, toteCount: Int) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
    ) {
        Column(modifier = Modifier.padding(horizontal = 18.dp, vertical = 14.dp)) {
            Text(
                text = "Checkout Summary",
                style = MaterialTheme.typography.labelLarge,
                color = ReviewMuted
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = "$toteCount totes • $totalMeals meals",
                fontSize = 24.sp,
                fontWeight = FontWeight.ExtraBold,
                color = ReviewText
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun CompanyReviewCard(
    company: CompanySummary,
    onEditCharities: () -> Unit,
    onEditTote: (company: String, toteId: String) -> Unit,
    onRemoveTote: (company: String, toteId: String) -> Unit
) {
    OrangeAccentCard(
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(modifier = Modifier.padding(horizontal = 18.dp, vertical = 16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = company.company.ifBlank { "UNKNOWN" },
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 18.sp,
                    color = ReviewText,
                    modifier = Modifier.weight(1f)
                )
                Text(
                    text = "${company.toteIds.size * MEALS_PER_TOTE} meals",
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    color = ReviewMuted
                )
            }

            Spacer(Modifier.height(10.dp))

            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                company.toteIds.forEach { toteId ->
                    ToteIdChip(
                        toteId = toteId,
                        onEdit = { onEditTote(company.company, toteId) },
                        onRemove = { onRemoveTote(company.company, toteId) }
                    )
                }
            }

            Spacer(Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Top
            ) {
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.Top
                ) {
                    Icon(
                        imageVector = Icons.Default.LocationOn,
                        contentDescription = "Charity Destination",
                        tint = ReviewMuted,
                        modifier = Modifier
                            .size(18.dp)
                            .padding(top = 1.dp)
                    )
                    Spacer(Modifier.size(8.dp))
                    Column {
                        Text(
                            text = "Charity Destination",
                            style = MaterialTheme.typography.labelLarge,
                            color = ReviewMuted
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = formatCharityList(company.charities),
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 18.sp,
                            color = ReviewText
                        )
                    }
                }

                IconButton(onClick = onEditCharities) {
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = "Edit charity",
                        tint = ReviewMuted
                    )
                }
            }

            if (company.toteAssignments.isNotEmpty()) {
                Spacer(Modifier.height(8.dp))
                company.toteAssignments.forEach { assignment ->
                    Text(
                        text = "Tote ${assignment.toteId} -> ${normalizeCharityName(assignment.charity).ifBlank { "Not assigned" }}",
                        fontSize = 13.sp,
                        color = ReviewMuted
                    )
                    Spacer(Modifier.height(2.dp))
                }
            }
        }
    }
}

@Composable
private fun ToteIdChip(
    toteId: String,
    onEdit: () -> Unit,
    onRemove: () -> Unit
) {
    Row(
        modifier = Modifier
            .background(ToteChip, RoundedCornerShape(10.dp))
            .padding(horizontal = 10.dp, vertical = 7.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = toteId,
            color = ToteChipText,
            fontWeight = FontWeight.ExtraBold,
            fontSize = 14.sp
        )
        Spacer(Modifier.size(6.dp))
        IconButton(onClick = onEdit, modifier = Modifier.size(18.dp)) {
            Icon(
                imageVector = Icons.Default.Edit,
                contentDescription = "Edit tote",
                tint = ToteChipText,
                modifier = Modifier.size(14.dp)
            )
        }
        Spacer(Modifier.size(2.dp))
        IconButton(onClick = onRemove, modifier = Modifier.size(18.dp)) {
            Icon(
                imageVector = Icons.Default.Close,
                contentDescription = "Remove tote",
                tint = ToteChipText,
                modifier = Modifier.size(14.dp)
            )
        }
    }
}

@Composable
private fun ContactReviewBlock(
    contact: SavedContact,
    onEditContact: () -> Unit
) {
    OrangeAccentCard(
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(modifier = Modifier.padding(horizontal = 18.dp, vertical = 16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Contact Details",
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 18.sp,
                    color = ReviewText,
                    modifier = Modifier.weight(1f)
                )
                IconButton(onClick = onEditContact) {
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = "Edit contact",
                        tint = ReviewMuted
                    )
                }
            }

            Spacer(Modifier.height(6.dp))
            ContactRow(icon = Icons.Default.Person, label = "Contact Name", value = contact.fullName)
            Spacer(Modifier.height(10.dp))
            ContactRow(icon = Icons.Default.Phone, label = "Contact Number", value = contact.phone)
            Spacer(Modifier.height(10.dp))
            ContactRow(icon = Icons.Default.Email, label = "Email Address", value = contact.email)
            Spacer(Modifier.height(10.dp))
            ContactRow(icon = Icons.Default.Badge, label = "Role", value = contact.role)
        }
    }
}

@Composable
private fun EditToteDialog(
    company: String,
    toteId: String,
    onDismiss: () -> Unit,
    onSave: (newToteId: String, newCompany: String) -> String?
) {
    var editedToteId by remember(toteId) { mutableStateOf(toteId) }
    var editedCompany by remember(company) { mutableStateOf(company) }
    var error by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Edit Tote") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = editedToteId,
                    onValueChange = {
                        editedToteId = it
                        error = null
                    },
                    label = { Text("Tote ID") },
                    singleLine = true
                )
                OutlinedTextField(
                    value = editedCompany,
                    onValueChange = {
                        editedCompany = it
                        error = null
                    },
                    label = { Text("Company Name") },
                    singleLine = true
                )
                error?.let {
                    Text(
                        text = it,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val result = onSave(editedToteId, editedCompany)
                    if (result == null) onDismiss() else error = result
                }
            ) {
                Text("Save")
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
private fun ContactRow(
    icon: ImageVector,
    label: String,
    value: String
) {
    Row(verticalAlignment = Alignment.Top) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = ReviewMuted,
            modifier = Modifier
                .size(18.dp)
                .padding(top = 2.dp)
        )
        Spacer(Modifier.size(8.dp))
        Column {
            Text(
                text = label,
                style = MaterialTheme.typography.labelLarge,
                color = ReviewMuted
            )
            Spacer(Modifier.height(3.dp))
            Text(
                text = value.ifBlank { "-" },
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp,
                color = ReviewText
            )
        }
    }
}

@Composable
private fun SubmissionMessageCard(message: String) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MessageBg)
    ) {
        Text(
            text = message,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            color = MessageText,
            style = MaterialTheme.typography.bodyMedium
        )
    }
}
//