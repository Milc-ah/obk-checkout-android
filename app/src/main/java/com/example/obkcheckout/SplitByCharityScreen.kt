package com.example.obkcheckout

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
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
fun SplitByCharityScreen(
    groupedTotes: List<CompanyTotesGroup>,
    charities: List<String>,
    initialAssignments: Map<String, String>,
    onAssignmentsChanged: (Map<String, String>) -> Unit,
    onBack: () -> Unit,
    onContinue: (assignedByToteId: Map<String, String>) -> Unit,
    isLoadingCharities: Boolean = false
) {
    val options = remember(charities) {
        charities.map { it.trim() }.filter { it.isNotBlank() }.distinct().sorted()
    }
    val toteIds = remember(groupedTotes) { groupedTotes.flatMap { it.toteIds }.distinct() }
    val assigned = remember(toteIds, initialAssignments) {
        mutableStateMapOf<String, String>().apply {
            putAll(initialAssignments.filterKeys { toteIds.contains(it) })
        }
    }
    var error by rememberSaveable { mutableStateOf<String?>(null) }

    LaunchedEffect(assigned.size, assigned.values.toList()) {
        onAssignmentsChanged(assigned.toMap())
    }

    Column(modifier = Modifier.fillMaxSize().background(Color.White)) {
        OBKHeader(onBack = onBack)

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp, vertical = 16.dp)
        ) {
            Text(
                text = "Split By Charity",
                fontSize = 22.sp,
                fontWeight = FontWeight.ExtraBold,
                color = Color.Black
            )

            Spacer(Modifier.height(10.dp))

            Text(
                text = "Assign each tote to an existing or custom charity.",
                fontSize = 15.sp,
                fontWeight = FontWeight.Medium,
                color = Color.Black
            )

            Spacer(Modifier.height(14.dp))

            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                contentPadding = PaddingValues(bottom = 12.dp)
            ) {
                if (isLoadingCharities && options.isEmpty()) {
                    item {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                            Text(
                                text = "Loading charities...",
                                color = Color.Gray,
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }
                }

                items(groupedTotes, key = { it.company }) { group ->
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                color = Color(0xFFF7F7F7),
                                shape = RoundedCornerShape(12.dp)
                            )
                            .padding(14.dp)
                    ) {
                        Text(
                            text = group.company.ifBlank { "UNKNOWN" },
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 16.sp,
                            color = Color.Black
                        )

                        Spacer(Modifier.height(10.dp))

                        group.toteIds.forEach { toteId ->
                            ToteCharityAssignmentRow(
                                toteId = toteId,
                                charities = options,
                                selectedValue = assigned[toteId].orEmpty(),
                                onValueChange = {
                                    assigned[toteId] = normalizeCharityName(it)
                                    error = null
                                }
                            )
                            Spacer(Modifier.height(10.dp))
                        }
                    }
                }
            }

            error?.let {
                Spacer(Modifier.height(8.dp))
                Text(
                    text = it,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall
                )
            }

            Spacer(Modifier.height(8.dp))

            Button(
                onClick = {
                    val cleaned = assigned
                        .mapValues { it.value.trim() }
                        .filterValues { it.isNotBlank() }
                    val missing = toteIds.any { cleaned[it].isNullOrBlank() }
                    if (missing) {
                        error = "Please assign a charity to every tote."
                    } else {
                        onAssignmentsChanged(cleaned)
                        onContinue(cleaned)
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                enabled = toteIds.isNotEmpty(),
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

@Composable
private fun ToteCharityAssignmentRow(
    toteId: String,
    charities: List<String>,
    selectedValue: String,
    onValueChange: (String) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    var selectedOption by rememberSaveable(toteId) { mutableStateOf("") }
    var customValue by rememberSaveable(toteId) { mutableStateOf("") }

    LaunchedEffect(selectedValue, charities) {
        val trimmed = selectedValue.trim()
        if (trimmed in charities) {
            selectedOption = trimmed
            customValue = ""
        } else {
            selectedOption = ""
            customValue = trimmed
        }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.White, RoundedCornerShape(10.dp))
            .padding(12.dp)
    ) {
        Text(
            text = "Tote $toteId",
            fontWeight = FontWeight.Bold,
            fontSize = 15.sp,
            color = Color.Black
        )

        Spacer(Modifier.height(8.dp))

        Box(modifier = Modifier.fillMaxWidth()) {
            OutlinedTextField(
                value = selectedOption.ifBlank { "Select charity" },
                onValueChange = { },
                readOnly = true,
                modifier = Modifier.fillMaxWidth(),
                trailingIcon = {
                    androidx.compose.material3.Icon(
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
                charities.forEach { charity ->
                    DropdownMenuItem(
                        text = { Text(charity) },
                        onClick = {
                            selectedOption = charity
                            customValue = ""
                            onValueChange(normalizeCharityName(charity))
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

        Spacer(Modifier.height(8.dp))

        OutlinedTextField(
            value = customValue,
            onValueChange = {
                customValue = it
                if (it.isNotBlank()) {
                    selectedOption = ""
                    onValueChange(normalizeCharityName(it))
                } else {
                    onValueChange(selectedOption)
                }
            },
            modifier = Modifier.fillMaxWidth(),
            placeholder = { Text("Or type custom charity") },
            singleLine = true
        )
    }
}
//