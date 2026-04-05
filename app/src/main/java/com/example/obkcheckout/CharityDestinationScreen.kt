package com.example.obkcheckout

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
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
    onContinue: (String) -> Unit,
    charities: List<String> = emptyList(),
    isLoadingCharities: Boolean = false,
    charityLoadError: String? = null,
    onRetryLoadCharities: (() -> Unit)? = null
) {
    val options = remember(charities) {
        charities.map { it.trim() }.filter { it.isNotBlank() }.distinct().sorted()
    }
    var expanded by remember { mutableStateOf(false) }
    var selectedOption by rememberSaveable { mutableStateOf("") }
    var customCharity by rememberSaveable { mutableStateOf("") }
    var error by rememberSaveable { mutableStateOf<String?>(null) }

    LaunchedEffect(selectedCharity, options) {
        val trimmed = selectedCharity.trim()
        if (trimmed in options) {
            selectedOption = trimmed
            customCharity = ""
        } else {
            selectedOption = ""
            customCharity = trimmed
        }
    }

    val effectiveCharity = customCharity.trim().ifBlank { selectedOption.trim() }

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
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "Charity Destination",
                fontSize = 26.sp,
                fontWeight = FontWeight.ExtraBold,
                color = Color.Black
            )
            OrangeAccentCard(
                modifier = Modifier.fillMaxWidth(),
            ) {
                Column(
                    modifier = Modifier.padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "Assign all totes to one charity. Select an existing charity or type a custom one.",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color.Black
                    )

                    Text(
                        text = "Select existing charity",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color.Black
                    )

                    Box {
                        OutlinedTextField(
                            value = selectedOption.ifBlank { "Select charity" },
                            onValueChange = { },
                            readOnly = true,
                            modifier = Modifier.fillMaxWidth(),
                            trailingIcon = {
                                Icon(
                                    imageVector = if (expanded) Icons.Default.KeyboardArrowUp
                                                  else Icons.Default.KeyboardArrowDown,
                                    contentDescription = if (expanded) "Close list" else "Open list"
                                )
                            }
                        )

                        DropdownMenu(
                            expanded = expanded,
                            onDismissRequest = { expanded = false }
                        ) {
                            options.forEach { item ->
                                DropdownMenuItem(
                                    text = { Text(item) },
                                    onClick = {
                                        selectedOption = item
                                        customCharity = ""
                                        error = null
                                        onCharitySelected(normalizeCharityName(item))
                                        expanded = false
                                    }
                                )
                            }
                        }

                        Box(
                            modifier = Modifier
                                .matchParentSize()
                                .clickable { expanded = true }
                        )
                    }

                    when {
                        isLoadingCharities -> {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                                Text(
                                    text = "Loading charities...",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color.Gray
                                )
                            }
                        }
                        charityLoadError != null && options.isEmpty() -> {
                            Text(
                                text = charityLoadError,
                                color = MaterialTheme.colorScheme.error,
                                style = MaterialTheme.typography.bodySmall
                            )
                            TextButton(onClick = { onRetryLoadCharities?.invoke() }) {
                                Text("Retry", color = MaterialTheme.colorScheme.primary)
                            }
                        }
                    }

                    Text(
                        text = "Or enter custom charity",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color.Black
                    )

                    OutlinedTextField(
                        value = customCharity,
                        onValueChange = {
                            customCharity = it
                            if (it.isNotBlank()) {
                                selectedOption = ""
                                onCharitySelected(normalizeCharityName(it))
                            } else if (selectedOption.isNotBlank()) {
                                onCharitySelected(normalizeCharityName(selectedOption))
                            } else {
                                onCharitySelected("")
                            }
                            error = null
                        },
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = { Text("Type charity name") },
                        isError = error != null,
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
            }

            Spacer(Modifier.height(24.dp))

            Button(
                onClick = {
                    val charity = effectiveCharity
                    if (charity.isBlank()) {
                        error = "Please select or enter a charity."
                    } else {
                        val normalized = normalizeCharityName(charity)
                        onCharitySelected(normalized)
                        onContinue(normalized)
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary
                ),
                shape = RoundedCornerShape(10.dp)
            ) {
                Text(
                    "Proceed",
                    color = MaterialTheme.colorScheme.onPrimary,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
            }
        }
    }
}
