package com.example.obkcheckout

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Review screen renders a ReviewSummary which can be:
 * - computed locally from current CheckoutSession state, OR
 * - returned directly by backend (recommended for consistency).
 */
@Composable
fun ReviewScreen(
    summary: ReviewSummary,
    onBack: () -> Unit,
    onEditCharities: () -> Unit,
    onEditContact: () -> Unit,
    onContinue: () -> Unit
) {
    Column(modifier = Modifier.fillMaxSize().background(Color.White)) {

        OBKHeader(onBack = onBack)

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp, vertical = 16.dp)
        ) {
            Text(
                text = "Review",
                fontSize = 26.sp,
                fontWeight = FontWeight.ExtraBold,
                color = Color.Black
            )

            Spacer(Modifier.height(14.dp))

            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                verticalArrangement = Arrangement.spacedBy(14.dp),
                contentPadding = PaddingValues(bottom = 12.dp)
            ) {
                items(summary.companies, key = { it.company }) { company ->
                    CompanyReviewCard(company = company, onEditCharities = onEditCharities)
                }

                item { ContactReviewBlock(contact = summary.contact, onEditContact = onEditContact) }
                item { GrandTotalBlock(mealsGrandTotal = summary.mealsGrandTotal) }
                item { Spacer(Modifier.height(80.dp)) }
            }

            Button(
                onClick = onContinue,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary
                ),
                shape = RoundedCornerShape(10.dp)
            ) {
                Text(
                    "Continue",
                    color = MaterialTheme.colorScheme.onPrimary,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
            }

            Spacer(Modifier.height(10.dp))
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun CompanyReviewCard(
    company: CompanySummary,
    onEditCharities: () -> Unit
) {
    val charityText = company.charities.filter { it.isNotBlank() }.joinToString(" and ")

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = company.company.ifBlank { "Company" }.uppercase(),
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 16.sp,
                    color = Color.Black,
                    modifier = Modifier.weight(1f)
                )
                Text(
                    text = "${company.mealsTotal} meals",
                    fontWeight = FontWeight.Medium,
                    fontSize = 16.sp,
                    color = Color.Black
                )
            }

            Spacer(Modifier.height(10.dp))

            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                company.toteIds.forEach { tote ->
                    Box(
                        modifier = Modifier
                            .background(MaterialTheme.colorScheme.secondary, RoundedCornerShape(6.dp))
                            .padding(horizontal = 10.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = tote.ifBlank { "Tote" },
                            color = MaterialTheme.colorScheme.onSecondary,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            Spacer(Modifier.height(14.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Charity Destination",
                        color = Color.Black,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        text = if (charityText.isBlank()) "Select" else charityText,
                        color = Color.Black,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.ExtraBold
                    )
                }
                IconButton(onClick = onEditCharities) {
                    Icon(Icons.Default.Edit, contentDescription = "Edit charity")
                }
            }
        }
    }
}

@Composable
private fun ContactReviewBlock(
    contact: SavedContact,
    onEditContact: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.Top
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text("Contact Name",   fontWeight = FontWeight.Medium, fontSize = 13.sp, color = Color.Black)
                Text(contact.fullName.ifBlank { "-" }, fontWeight = FontWeight.ExtraBold, fontSize = 18.sp, color = Color.Black)

                Spacer(Modifier.height(10.dp))

                Text("Contact Number", fontWeight = FontWeight.Medium, fontSize = 13.sp, color = Color.Black)
                Text(contact.phone.ifBlank { "-" }, fontWeight = FontWeight.ExtraBold, fontSize = 18.sp, color = Color.Black)

                Spacer(Modifier.height(10.dp))

                Text("Email address",  fontWeight = FontWeight.Medium, fontSize = 13.sp, color = Color.Black)
                Text(contact.email.ifBlank { "-" }, fontWeight = FontWeight.ExtraBold, fontSize = 18.sp, color = Color.Black)
            }

            IconButton(onClick = onEditContact) {
                Icon(Icons.Default.Edit, contentDescription = "Edit contact")
            }
        }
    }
}

@Composable
private fun GrandTotalBlock(mealsGrandTotal: Int) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Grand total",
                fontWeight = FontWeight.ExtraBold,
                fontSize = 16.sp,
                color = Color.Black,
                modifier = Modifier.weight(1f)
            )
            Text(
                text = "$mealsGrandTotal meals",
                fontWeight = FontWeight.ExtraBold,
                fontSize = 16.sp,
                color = Color.Black
            )
        }
    }
}
