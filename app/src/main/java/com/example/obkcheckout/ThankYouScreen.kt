package com.example.obkcheckout

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Thank-you screen shown after backend confirms checkout.
 * confirmationId + totals should come from ConfirmCheckoutResponse.
 */
@Composable
fun ThankYouScreen(
    confirmationId: String?,
    mealsGrandTotal: Int,
    companies: List<CompanySummary>,
    contactName: String,
    onFinish: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
            .padding(horizontal = 20.dp, vertical = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(modifier = Modifier.fillMaxWidth()) {
            Image(
                painter = painterResource(id = R.drawable.obklogo2),
                contentDescription = "OBK Logo",
                modifier = Modifier.height(38.dp),
                contentScale = ContentScale.Fit
            )
        }

        Spacer(Modifier.height(18.dp))

        Text(
            text = "✓",
            fontSize = 54.sp,
            color = MaterialTheme.colorScheme.primary,
            fontWeight = FontWeight.ExtraBold
        )

        Spacer(Modifier.height(6.dp))

        Text(
            text = "Thank you ${contactName.ifBlank { "" }}",
            fontSize = 28.sp,
            fontWeight = FontWeight.ExtraBold,
            color = Color.Black
        )

        Spacer(Modifier.height(10.dp))

        Text(
            text = "That's $mealsGrandTotal meals checked out.",
            fontSize = 18.sp,
            fontWeight = FontWeight.Medium,
            color = Color.Black
        )

        confirmationId?.let {
            Spacer(Modifier.height(6.dp))
            Text(
                text = "Confirmation ID: $it",
                fontSize = 14.sp,
                color = Color.Gray
            )
        }

        Spacer(Modifier.height(18.dp))

        companies.forEach { company ->
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(modifier = Modifier.fillMaxWidth()) {
                        Text(
                            text = company.company.ifBlank { "Company" }.uppercase(),
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 16.sp,
                            color = Color.Black,
                            modifier = Modifier.weight(1f)
                        )
                        Text(
                            text = "${company.mealsTotal} meals",
                            fontSize = 16.sp,
                            color = Color.Black
                        )
                    }

                    Spacer(Modifier.height(10.dp))

                    Text(
                        text = "Charity Destination",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color.Black
                    )
                    Text(
                        text = company.charities.joinToString(" and "),
                        fontSize = 18.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color.Black
                    )
                }
            }
            Spacer(Modifier.height(14.dp))
        }

        Spacer(Modifier.weight(1f))

        Button(
            onClick = onFinish,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary
            ),
            shape = RoundedCornerShape(10.dp)
        ) {
            Text(
                text = "Finish",
                color = MaterialTheme.colorScheme.onPrimary,
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp
            )
        }
    }
}
