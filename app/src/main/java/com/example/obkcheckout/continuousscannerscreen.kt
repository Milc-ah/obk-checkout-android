package com.example.obkcheckout

import android.os.SystemClock
import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch

@Composable
fun ContinuousScannerScreen(
    scannedByCompany: Map<String, List<String>>,
    toteErrorMessage: String?,
    onDismissToteError: () -> Unit,
    onScannedTote: (String) -> Unit,
    onManualToteId: (String) -> Unit,
    onRemoveTote: (company: String, toteId: String) -> Unit,
    onDone: () -> Unit,
    onBack: () -> Unit
) {
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()
    val green = MaterialTheme.colorScheme.primary

    val allTotes = scannedByCompany.entries
        .flatMap { (company, toteIds) -> toteIds.map { toteId -> company to toteId } }
        .distinctBy { it.second }
        .sortedBy { it.second }

    var pendingDelete by remember { mutableStateOf<Pair<String, String>?>(null) }
    var lastScanAt by remember { mutableStateOf(0L) }
    var lastScanValue by remember { mutableStateOf("") }
    var manualInput by remember { mutableStateOf("") }
    var manualError by remember { mutableStateOf<String?>(null) }
    var lastAddedId by remember { mutableStateOf<String?>(null) }

    val showGoTop by remember {
        derivedStateOf {
            val total = listState.layoutInfo.totalItemsCount
            if (total <= 0) return@derivedStateOf false
            val lastVisible = listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
            lastVisible >= total - 1 && total > 3
        }
    }

    LaunchedEffect(allTotes.size) {
        if (toteErrorMessage == null) {
            manualInput = ""
            manualError = null
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
        ) {
            RealScannerRoute(
                onScanned = { raw ->
                    val now = SystemClock.elapsedRealtime()
                    val toteId = normalizeToToteId(raw)
                    val isRepeat = toteId == lastScanValue && (now - lastScanAt) < 900
                    if (isRepeat) return@RealScannerRoute

                    lastScanValue = toteId
                    lastScanAt = now

                    if (toteId.isNotBlank() && allTotes.none { it.second == toteId }) {
                        onScannedTote(raw)
                        lastAddedId = toteId
                    }
                },
                onBack = onBack
            )
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .background(Color.White)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp, vertical = 12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = manualInput,
                        onValueChange = {
                            manualInput = it
                            if (manualError != null) manualError = null
                            if (toteErrorMessage != null) onDismissToteError()
                        },
                        modifier = Modifier.weight(1f),
                        placeholder = { Text("Enter Tote ID manually") },
                        singleLine = true,
                        isError = manualError != null
                    )
                    Button(
                        onClick = {
                            val trimmed = manualInput.trim()
                            if (trimmed.isBlank()) {
                                manualError = "Please enter a Tote ID."
                            } else {
                                onManualToteId(trimmed)
                                lastAddedId = normalizeToToteId(trimmed)
                                manualInput = ""
                                manualError = null
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = green),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text("Add", color = MaterialTheme.colorScheme.onPrimary)
                    }
                }

                manualError?.let {
                    Spacer(Modifier.height(6.dp))
                    Text(
                        text = it,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall
                    )
                }

                toteErrorMessage?.let {
                    Spacer(Modifier.height(6.dp))
                    Text(
                        text = it,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall
                    )
                }

                lastAddedId?.let {
                    Spacer(Modifier.height(6.dp))
                    Text(
                        text = "Last added: #$it",
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.SemiBold,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }

                Spacer(Modifier.height(10.dp))

                Text(
                    text = "Totes (${allTotes.size})",
                    fontWeight = FontWeight.Bold
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    text = "Scroll to review. Tap x to remove.",
                    color = Color.Gray,
                    style = MaterialTheme.typography.bodySmall
                )

                Spacer(Modifier.height(10.dp))

                LazyColumn(
                    state = listState,
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    contentPadding = PaddingValues(bottom = 18.dp)
                ) {
                    if (allTotes.isEmpty()) {
                        item { Text("Scan a tote to start...", color = Color.Gray) }
                    } else {
                        items(allTotes, key = { it.second }) { (company, toteId) ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(
                                        color = Color(0xFFF1F1F1),
                                        shape = RoundedCornerShape(10.dp)
                                    )
                                    .padding(horizontal = 14.dp, vertical = 12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(text = "#$toteId", fontWeight = FontWeight.Bold)
                                    Text(
                                        text = company.ifBlank { "UNKNOWN" },
                                        color = Color.Gray,
                                        style = MaterialTheme.typography.bodySmall
                                    )
                                }

                                IconButton(onClick = { pendingDelete = company to toteId }) {
                                    Icon(
                                        imageVector = Icons.Default.Close,
                                        contentDescription = "Remove"
                                    )
                                }
                            }
                        }
                    }
                }

                Button(
                    onClick = onDone,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(54.dp),
                    enabled = allTotes.isNotEmpty(),
                    colors = ButtonDefaults.buttonColors(containerColor = green),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text(
                        "Finished Scanning",
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                }

                Spacer(Modifier.height(10.dp))

                OutlinedButton(
                    onClick = onBack,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text("Cancel")
                }
            }

            if (showGoTop) {
                FloatingActionButton(
                    onClick = { scope.launch { listState.animateScrollToItem(0) } },
                    containerColor = green,
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(end = 16.dp, bottom = 150.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.KeyboardArrowUp,
                        contentDescription = "Go to top",
                        tint = MaterialTheme.colorScheme.onPrimary
                    )
                }
            }
        }

        pendingDelete?.let { (company, toteId) ->
            AlertDialog(
                onDismissRequest = { pendingDelete = null },
                title = { Text("Remove tote?") },
                text = { Text("Do you want to remove #$toteId?") },
                confirmButton = {
                    TextButton(
                        onClick = {
                            onRemoveTote(company, toteId)
                            pendingDelete = null
                        }
                    ) {
                        Text("Yes, remove")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { pendingDelete = null }) {
                        Text("Cancel")
                    }
                }
            )
        }
    }
}

//