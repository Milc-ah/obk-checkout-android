package com.example.obkcheckout

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun CharityDestinationScreen(
    selectedCharity: String,
    onCharitySelected: (String) -> Unit,
    onBack: () -> Unit,
    onSplitByCharity: () -> Unit,
    onContinue: () -> Unit,
    charities: List<String> = emptyList() // Backend should provide this (e.g., from API/repository)
) {
    val hasCharities = charities.isNotEmpty()
    var expanded by remember { mutableStateOf(false) }

    val displayValue = when {
        selectedCharity.isNotBlank() -> selectedCharity
        hasCharities -> "Select"
        else -> "Loading charities…"
    }

    val canContinue = hasCharities && displayValue != "Select" && displayValue != "Loading charities…"

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
    ) {
        OBKHeader(onBack = onBack)

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 22.dp, vertical = 22.dp)
        ) {
            Text(
                text = "Charity Destination",
                fontSize = 26.sp,
                fontWeight = FontWeight.ExtraBold,
                color = Color.Black
            )

            Spacer(Modifier.height(14.dp))

            Text(
                text = "Assign all totes to",
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium,
                color = Color.Black
            )

            Spacer(Modifier.height(10.dp))

            Box {
                OutlinedTextField(
                    value = displayValue,
                    onValueChange = { },
                    readOnly = true,
                    enabled = hasCharities,
                    modifier = Modifier.fillMaxWidth(),
                    trailingIcon = {
                        Icon(
                            imageVector = Icons.Default.KeyboardArrowDown,
                            contentDescription = "Dropdown",
                            modifier = Modifier.clickable(enabled = hasCharities) { expanded = true }
                        )
                    }
                )

                DropdownMenu(
                    expanded = expanded && hasCharities,
                    onDismissRequest = { expanded = false },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    charities.forEach { item ->
                        DropdownMenuItem(
                            text = { Text(item) },
                            onClick = {
                                onCharitySelected(item)
                                expanded = false
                            }
                        )
                    }
                }
            }

            Spacer(Modifier.height(18.dp))

            Button(
                onClick = onSplitByCharity,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFFE0E0E0),
                    contentColor = Color.Black
                ),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text(
                    text = "Split by Charity",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Medium
                )
            }

            Spacer(Modifier.weight(1f))

            Button(
                onClick = onContinue,
                enabled = canContinue,
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
        }
    }
}
