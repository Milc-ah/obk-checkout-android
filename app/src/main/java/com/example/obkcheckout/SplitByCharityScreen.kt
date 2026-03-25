package com.example.obkcheckout

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun SplitByCharityScreen(
    toteIds: List<String>,
    charities: List<String>, // Backend should supply this list (e.g., from API/repository)
    initialAssignments: Map<String, String>,
    onBack: () -> Unit,
    onContinue: (assignedByToteId: Map<String, String>) -> Unit
) {
    val listState = rememberLazyListState()
    val assigned = remember(toteIds, initialAssignments) {
        mutableStateMapOf<String, String>().apply {
            putAll(initialAssignments.filterKeys { toteIds.contains(it) })
        }
    }

    val displayCharities = remember(charities) {
        val cleaned = charities
            .map { it.trim() }
            .filter { it.isNotBlank() }
            .distinct()
        listOf("Select") + cleaned
    }

    Column(modifier = Modifier.fillMaxSize()) {
        OBKHeader(onBack = onBack)

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp, vertical = 16.dp)
        ) {
            Text(
                text = "Charity Destination",
                fontSize = 22.sp,
                fontWeight = FontWeight.ExtraBold,
                color = Color.Black
            )

            Spacer(Modifier.height(10.dp))

            Text(
                text = "Assign each tote to a charity",
                fontSize = 15.sp,
                fontWeight = FontWeight.Medium,
                color = Color.Black
            )

            Spacer(Modifier.height(12.dp))
            Divider()
            Spacer(Modifier.height(14.dp))

            LazyColumn(
                state = listState,
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                contentPadding = PaddingValues(bottom = 12.dp)
            ) {
                items(toteIds, key = { it }) { toteId ->
                    ToteCharityRow(
                        toteId   = toteId,
                        charities = displayCharities,
                        selected  = assigned[toteId] ?: "Select",
                        onSelected = { chosen -> assigned[toteId] = chosen }
                    )
                }

                item { Spacer(Modifier.height(80.dp)) }
            }

            Button(
                onClick = {
                    // Only return totes with a real selection
                    val cleaned = assigned
                        .filterValues { it.isNotBlank() && it != "Select" }
                        .toMap()
                    onContinue(cleaned)
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                enabled = toteIds.isNotEmpty() && displayCharities.size > 1,
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ToteCharityRow(
    toteId: String,
    charities: List<String>,
    selected: String,
    onSelected: (String) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(color = Color.White, shape = RoundedCornerShape(12.dp))
            .padding(14.dp)
    ) {
        Text(
            text = "Tote $toteId",
            fontWeight = FontWeight.Bold,
            fontSize = 16.sp,
            color = Color.Black
        )

        Spacer(Modifier.height(10.dp))

        ExposedDropdownMenuBox(
            expanded = expanded,
            onExpandedChange = { expanded = !expanded }
        ) {
            OutlinedTextField(
                value = selected.ifBlank { "Select" },
                onValueChange = { },
                readOnly = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .menuAnchor(),
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) }
            )

            DropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false },
                modifier = Modifier.exposedDropdownSize()
            ) {
                charities.forEach { c ->
                    DropdownMenuItem(
                        text = { Text(c) },
                        onClick = {
                            onSelected(if (c == "Select") "" else c)
                            expanded = false
                        }
                    )
                }
            }
        }
    }
}
