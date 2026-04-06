package com.example.obkcheckout

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun ThankYouScreen(
    confirmationId: String?,
    mealsGrandTotal: Int,
    companies: List<CompanySummary>,
    contact: SavedContact,
    onFinish: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF5F7F1))
            .padding(horizontal = 20.dp, vertical = 18.dp)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(modifier = Modifier.fillMaxWidth()) {
            Image(
                painter = painterResource(id = R.drawable.obklogo2),
                contentDescription = "OBK Logo",
                modifier = Modifier.height(40.dp),
                contentScale = ContentScale.Fit
            )
        }

        Spacer(Modifier.height(22.dp))

        Text(
            text = "Checkout Complete",
            fontSize = 30.sp,
            color = Color(0xFF172114),
            fontWeight = FontWeight.ExtraBold
        )

        Spacer(Modifier.height(8.dp))

        Text(
            text = "Thank you ${contact.fullName.ifBlank { "" }}",
            fontSize = 20.sp,
            fontWeight = FontWeight.Medium,
            color = Color(0xFF556150)
        )

        Spacer(Modifier.height(16.dp))

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
        ) {
            Column(modifier = Modifier.padding(horizontal = 18.dp, vertical = 18.dp)) {
                Text(
                    text = "Total Checked Out",
                    style = MaterialTheme.typography.labelLarge,
                    color = Color(0xFF556150)
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    text = "$mealsGrandTotal meals",
                    fontSize = 28.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color(0xFF172114)
                )
                confirmationId?.let {
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = "Confirmation ID: $it",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color(0xFF556150)
                    )
                }
            }
        }

        Spacer(Modifier.height(18.dp))

        companies.forEach { company ->
            OrangeAccentCard(
                modifier = Modifier.fillMaxWidth(),
            ) {
                Column(modifier = Modifier.padding(horizontal = 18.dp, vertical = 18.dp)) {
                    Text(
                        text = company.company.ifBlank { "UNKNOWN" },
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 18.sp,
                        color = Color(0xFF172114)
                    )

                    Spacer(Modifier.height(6.dp))

                    Text(
                        text = "${company.toteIds.size} totes • ${company.toteIds.size * MEALS_PER_TOTE} meals",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color(0xFF556150)
                    )

                    Spacer(Modifier.height(14.dp))

                    Text(
                        text = "Totes",
                        style = MaterialTheme.typography.labelLarge,
                        color = Color(0xFF556150)
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = company.toteIds.joinToString(", "),
                        fontWeight = FontWeight.Bold,
                        fontSize = 17.sp,
                        color = Color(0xFF172114)
                    )

                    Spacer(Modifier.height(14.dp))

                    Text(
                        text = "Charity Destination",
                        style = MaterialTheme.typography.labelLarge,
                        color = Color(0xFF556150)
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = formatCharityList(company.charities),
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 18.sp,
                        color = Color(0xFF172114)
                    )

                    Spacer(Modifier.height(12.dp))

                    company.toteAssignments.forEach { assignment ->
                        Text(
                            text = "Tote ${assignment.toteId} -> ${normalizeCharityName(assignment.charity).ifBlank { "Not assigned" }}",
                            fontSize = 14.sp,
                            color = Color(0xFF556150)
                        )
                        Spacer(Modifier.height(4.dp))
                    }
                }
            }
            Spacer(Modifier.height(16.dp))
        }

        OrangeAccentCard(
            modifier = Modifier.fillMaxWidth(),
        ) {
            Column(modifier = Modifier.padding(horizontal = 18.dp, vertical = 18.dp)) {
                Text("Contact Details", fontWeight = FontWeight.ExtraBold, fontSize = 18.sp, color = Color(0xFF172114))
                Spacer(Modifier.height(12.dp))
                ThankYouContactLine("Name", contact.fullName)
                Spacer(Modifier.height(10.dp))
                ThankYouContactLine("Phone", contact.phone)
                Spacer(Modifier.height(10.dp))
                ThankYouContactLine("Email", contact.email)
                Spacer(Modifier.height(10.dp))
                ThankYouContactLine("Role", contact.role)
            }
        }

        Spacer(Modifier.height(22.dp))

        Button(
            onClick = onFinish,
            modifier = Modifier
                .fillMaxWidth()
                .height(58.dp),
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
            shape = RoundedCornerShape(16.dp)
        ) {
            Text(
                text = "Check Out More Meals",
                color = MaterialTheme.colorScheme.onPrimary,
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp
            )
        }
    }
}

@Composable
private fun ThankYouContactLine(label: String, value: String) {
    Column {
        Text(text = label, style = MaterialTheme.typography.labelLarge, color = Color(0xFF556150))
        Spacer(Modifier.height(4.dp))
        Text(text = value.ifBlank { "-" }, fontWeight = FontWeight.Bold, fontSize = 17.sp, color = Color(0xFF172114))
    }
}
//