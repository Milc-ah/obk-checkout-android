package com.example.obkcheckout

import android.annotation.SuppressLint
import android.util.Log
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.common.InputImage
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Real camera scanner UI:
 * - Camera continuously scans and calls onScanned(rawValue)
 * - UI provides a frame overlay + back / close
 *
 * Integration notes:
 * - onScanned forwards the raw QR string to the checkout flow (caller parses toteId).
 * - Caller decides whether to navigate immediately or keep scanning.
 */
@Composable
fun RealScannerRoute(
    onScanned: (String) -> Unit,
    onBack: () -> Unit
) {
    Box(modifier = Modifier.fillMaxSize().background(Color.Black)) {

        CameraPreviewWithAnalyzer(onBarcode = onScanned)

        // Scan frame overlay
        Box(modifier = Modifier.fillMaxSize()) {
            Box(
                modifier = Modifier
                    .size(width = 280.dp, height = 200.dp)
                    .align(Alignment.Center)
                    .offset(y = (-20).dp)
                    .border(
                        width = 2.dp,
                        color = Color.White,
                        shape = RoundedCornerShape(14.dp)
                    )
            )

            Text(
                text = "Place QR code within frame",
                color = Color.White,
                fontSize = 13.sp,
                modifier = Modifier
                    .align(Alignment.Center)
                    .offset(y = 90.dp)
                    .background(Color(0x66000000), RoundedCornerShape(10.dp))
                    .padding(horizontal = 12.dp, vertical = 8.dp)
            )
        }

        // Top bar (back + close)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 10.dp, start = 8.dp, end = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "← Back",
                color = Color.White,
                modifier = Modifier
                    .background(Color(0x33000000), RoundedCornerShape(999.dp))
                    .clickable { onBack() }
                    .padding(horizontal = 12.dp, vertical = 6.dp)
            )

            IconButton(
                onClick = onBack,
                modifier = Modifier.background(Color(0x33000000), CircleShape)
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Close",
                    tint = Color.White
                )
            }
        }
    }
}

@SuppressLint("UnsafeOptInUsageError")
@Composable
private fun CameraPreviewWithAnalyzer(onBarcode: (String) -> Unit) {
    val context        = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    val isProcessing   = remember { AtomicBoolean(false) }
    var lastValue      by remember { mutableStateOf<String?>(null) }
    var lastEmitAtMs   by remember { mutableLongStateOf(0L) }
    val minIntervalMs  = 900L

    AndroidView(
        modifier = Modifier.fillMaxSize(),
        factory = { ctx ->
            val previewView = PreviewView(ctx)

            val cameraProviderFuture = ProcessCameraProvider.getInstance(ctx)
            cameraProviderFuture.addListener({
                val cameraProvider = cameraProviderFuture.get()

                val preview = Preview.Builder().build().also {
                    it.setSurfaceProvider(previewView.surfaceProvider)
                }

                val analysis = ImageAnalysis.Builder()
                    .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                    .build()

                val scanner = BarcodeScanning.getClient()

                analysis.setAnalyzer(ContextCompat.getMainExecutor(ctx)) { imageProxy ->
                    val mediaImage = imageProxy.image
                    if (mediaImage == null) { imageProxy.close(); return@setAnalyzer }

                    if (isProcessing.getAndSet(true)) { imageProxy.close(); return@setAnalyzer }

                    val inputImage = InputImage.fromMediaImage(
                        mediaImage,
                        imageProxy.imageInfo.rotationDegrees
                    )

                    scanner.process(inputImage)
                        .addOnSuccessListener { barcodes ->
                            val value = barcodes.firstOrNull()?.rawValue?.trim()
                            if (!value.isNullOrEmpty()) {
                                val now          = System.currentTimeMillis()
                                val isDifferent  = value != lastValue
                                val isPastCooldown = (now - lastEmitAtMs) >= minIntervalMs
                                if (isDifferent || isPastCooldown) {
                                    lastValue    = value
                                    lastEmitAtMs = now
                                    onBarcode(value)
                                }
                            }
                        }
                        .addOnFailureListener { e -> Log.e("OBKScanner", "Scan failed", e) }
                        .addOnCompleteListener {
                            isProcessing.set(false)
                            imageProxy.close()
                        }
                }

                cameraProvider.unbindAll()
                cameraProvider.bindToLifecycle(
                    lifecycleOwner,
                    CameraSelector.DEFAULT_BACK_CAMERA,
                    preview,
                    analysis
                )
            }, ContextCompat.getMainExecutor(ctx))

            previewView
        }
    )
}
