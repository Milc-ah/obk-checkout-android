package com.example.obkcheckout

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.QrCode2
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.copy
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.obkcheckout.ui.theme.OBKCheckoutTheme
import kotlinx.coroutines.delay

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            OBKCheckoutTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    OBKAppRoot()
                }
            }
        }
    }
}

private fun fs(base: Int) = (base * 1.2f).sp

@Composable
private fun OBKAppRoot() {
    var splashVisible by rememberSaveable { mutableStateOf(true) }

    LaunchedEffect(Unit) {
        delay(1400)
        splashVisible = false
    }

    if (splashVisible) {
        OBKSplashScreen()
    } else {
        OBKApp()
    }
}

@Composable
private fun OBKApp(vm: CheckoutViewModel = viewModel()) {
    val navController = rememberNavController()

    NavHost(navController = navController, startDestination = CheckoutRoutes.LOGIN) {
        composable(CheckoutRoutes.LOGIN) {
            LoginScreen(
                onLoginSuccess = {
                    vm.loadCharities()
                    navController.navigate(CheckoutRoutes.START) {
                        popUpTo(CheckoutRoutes.LOGIN) { inclusive = true }
                        launchSingleTop = true
                    }
                }
            )
        }

        composable(CheckoutRoutes.START) {
            ToteCheckoutStartScreen(
                scannedByCompany = vm.scannedByCompany,
                totalToteCount = vm.totes.size,
                toteErrorMessage = vm.toteErrorMessage,
                onDismissToteError = vm::clearToteError,
                onScanClicked = { navController.navigate(CheckoutRoutes.SCANNER) },
                onManualAdd = { toteId, companyName -> vm.addManualToteEntry(toteId, companyName) },
                onRemove = { company, id -> vm.removeScannedId(company, id) },
                onProceed = {
                    navController.navigate(CheckoutRoutes.CONFIRM) {
                        launchSingleTop = true
                    }
                },
                onLogout = {
                    TokenStore.clear()
                    vm.reset()
                    navController.navigate(CheckoutRoutes.LOGIN) {
                        popUpTo(CheckoutRoutes.START) { inclusive = true }
                        launchSingleTop = true
                    }
                }
            )
        }

        composable(CheckoutRoutes.SCANNER) {
            ContinuousScannerScreen(
                scannedByCompany = vm.scannedByCompany,
                toteErrorMessage = vm.toteErrorMessage,
                onDismissToteError = vm::clearToteError,
                onScannedTote = { toteId -> vm.addScannedId(toteId) },
                onManualToteId = { toteId -> vm.addManualToteId(toteId) },
                onRemoveTote = { company, toteId -> vm.removeScannedId(company, toteId) },
                onDone = {
                    navController.navigate(CheckoutRoutes.CONFIRM) {
                        popUpTo(CheckoutRoutes.START)
                        launchSingleTop = true
                    }
                },
                onBack = { navController.popBackStack() }
            )
        }

        composable(CheckoutRoutes.CONFIRM) {
            ConfirmScreen(
                scannedByCompany = vm.scannedByCompany,
                onRemove = { company, id -> vm.removeScannedId(company, id) },
                onAddMore = {
                    navController.popBackStack(CheckoutRoutes.START, inclusive = false)
                },
                onBack = { navController.popBackStack(CheckoutRoutes.START, inclusive = false) },
                onProceed = { navController.navigate("${CheckoutRoutes.CHARITY_DESTINATION}?returnToReview=false") }
            )
        }

        composable(route = "${CheckoutRoutes.CHARITY_DESTINATION}?returnToReview={returnToReview}") { backStackEntry ->
            val returnToReview =
                backStackEntry.arguments?.getString("returnToReview")?.toBoolean() ?: false

            CharityDestinationScreen(
                selectedCharity = vm.selectedCharity,
                charities = vm.charityNames,
                onCharitySelected = { vm.setSelectedCharity(it) },
                onBack = { navController.popBackStack() },
                onSplitByCharity = {
                    vm.setSplitEnabled(true)
                    navController.navigate("${CheckoutRoutes.SPLIT_BY_CHARITY}?returnToReview=$returnToReview")
                },
                onContinue = { charity ->
                    vm.setSelectedCharity(charity)
                    vm.setSplitEnabled(false)
                    vm.setAssignedByToteId(emptyMap())
                    if (returnToReview) {
                        vm.rebuildReviewSummary()
                        navController.popBackStack(CheckoutRoutes.REVIEW, inclusive = false)
                    } else {
                        navController.navigate(CheckoutRoutes.CONTACT_DETAILS)
                    }
                }
            )
        }

        composable(route = "${CheckoutRoutes.SPLIT_BY_CHARITY}?returnToReview={returnToReview}") { backStackEntry ->
            val returnToReview =
                backStackEntry.arguments?.getString("returnToReview")?.toBoolean() ?: false
            val groupedTotes = groupedTotesByCompany(vm.scannedByCompany)

            SplitByCharityScreen(
                groupedTotes = groupedTotes,
                charities = vm.charityNames,
                initialAssignments = vm.assignedByToteId,
                onAssignmentsChanged = { vm.setAssignedByToteId(it) },
                onBack = { navController.popBackStack() },
                onContinue = { assignedMap ->
                    vm.setSplitEnabled(true)
                    vm.setAssignedByToteId(assignedMap)
                    if (returnToReview) {
                        vm.rebuildReviewSummary()
                        navController.popBackStack(CheckoutRoutes.REVIEW, inclusive = false)
                    } else {
                        navController.navigate(CheckoutRoutes.CONTACT_DETAILS)
                    }
                }
            )
        }

        composable(CheckoutRoutes.CONTACT_DETAILS) {
            ContactDetailsScreen(
                initialContact = vm.contactDetails,
                lookedUpContact = vm.lookedUpContact,
                onBack = { navController.popBackStack() },
                onRequestLookup = { email -> vm.lookupContactByEmail(email) },
                onContinue = { savedContact ->
                    vm.buildAndSetReviewSummary(savedContact)
                    navController.navigate(CheckoutRoutes.REVIEW)
                }
            )
        }

        composable(CheckoutRoutes.REVIEW) {
            val summary: ReviewSummary? = vm.reviewSummary
            if (summary != null) {
                ReviewScreen(
                    summary = summary,
                    submissionErrorMessage = vm.submissionErrorMessage,
                    isSubmitting = vm.isSubmitting,
                    onDismissSubmissionError = vm::clearSubmissionError,
                    onBack = { navController.popBackStack() },
                    onEditCharities = {
                        if (vm.splitEnabled) {
                            navController.navigate("${CheckoutRoutes.SPLIT_BY_CHARITY}?returnToReview=true")
                        } else {
                            navController.navigate("${CheckoutRoutes.CHARITY_DESTINATION}?returnToReview=true")
                        }
                    },
                    onEditContact = { navController.navigate(CheckoutRoutes.CONTACT_DETAILS) },
                    onCheckoutMoreMeals = {
                        navController.popBackStack(CheckoutRoutes.START, inclusive = false)
                    },
                    onEditTote = { company, oldToteId, newToteId, newCompany ->
                        vm.updateTote(company, oldToteId, newToteId, newCompany)
                    },
                    onRemoveTote = { company, toteId ->
                        vm.removeScannedId(company, toteId)
                    },
                    onContinue = {
                        vm.submitCheckout {
                            navController.navigate(CheckoutRoutes.THANK_YOU)
                        }
                    }
                )
            } else {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text("Unable to load review summary. Returning to Contact Details.")
                }
                LaunchedEffect(Unit) {
                    navController.popBackStack(CheckoutRoutes.CONTACT_DETAILS, inclusive = false)
                }
            }
        }

        composable(CheckoutRoutes.THANK_YOU) {
            val summary: ReviewSummary? = vm.reviewSummary
            if (summary != null) {
                ThankYouScreen(
                    confirmationId = vm.confirmationId,
                    mealsGrandTotal = summary.mealsGrandTotal,
                    companies = summary.companies,
                    contact = summary.contact,
                    onFinish = {
                        vm.reset()
                        navController.navigate(CheckoutRoutes.START) {
                            popUpTo(CheckoutRoutes.START) { inclusive = true }
                            launchSingleTop = true
                        }
                    }
                )
            } else {
                LaunchedEffect(Unit) {
                    navController.navigate(CheckoutRoutes.START) {
                        popUpTo(CheckoutRoutes.START) { inclusive = true }
                        launchSingleTop = true
                    }
                }
            }
        }
    }
}

@Composable
private fun OBKSplashScreen() {
    val chefOffset by rememberInfiniteTransition(label = "chefSplash").animateFloat(
        initialValue = -42f,
        targetValue = 42f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 900),
            repeatMode = RepeatMode.Reverse
        ),
        label = "chefOffset"
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF5F7F1))
            .padding(horizontal = 28.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Image(
            painter = painterResource(id = R.drawable.obklogo2),
            contentDescription = "Big Kitchen Logo",
            modifier = Modifier.size(220.dp)
        )
        Spacer(Modifier.height(18.dp))
        Text(
            text = "Preparing checkout...",
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF172114)
        )
        Spacer(Modifier.height(20.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
                .border(BorderStroke(1.dp, Color(0xFFE48A14)), RoundedCornerShape(16.dp))
                .padding(horizontal = 18.dp),
            contentAlignment = Alignment.CenterStart
        ) {
            Text(
                text = "\uD83E\uDDD1\u200D\uD83C\uDF73",
                fontSize = 28.sp,
                modifier = Modifier.offset(x = chefOffset.dp)
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ToteCheckoutStartScreen(
    scannedByCompany: Map<String, List<String>>,
    totalToteCount: Int,
    toteErrorMessage: String?,
    onDismissToteError: () -> Unit,
    onScanClicked: () -> Unit,
    onManualAdd: (String, String) -> Unit,
    onRemove: (company: String, toteId: String) -> Unit,
    onProceed: () -> Unit,
    onLogout: () -> Unit
) {
    val context = LocalContext.current
    val activity = context.findActivity()
    var toteId by rememberSaveable { mutableStateOf("") }
    var companyName by rememberSaveable { mutableStateOf("") }
    var toteIdError by rememberSaveable { mutableStateOf<String?>(null) }
    var companyNameError by rememberSaveable { mutableStateOf<String?>(null) }
    var cameraMessage by rememberSaveable { mutableStateOf<String?>(null) }
    var showPermissionRationale by rememberSaveable { mutableStateOf(false) }
    var showPermissionSettings by rememberSaveable { mutableStateOf(false) }

    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            cameraMessage = null
            showPermissionRationale = false
            showPermissionSettings = false
            onScanClicked()
        } else {
            cameraMessage = "Camera permission is required to scan QR codes."
            showPermissionSettings = activity?.let {
                !ActivityCompat.shouldShowRequestPermissionRationale(
                    it,
                    Manifest.permission.CAMERA
                )
            } == true
        }
    }

    fun launchScanner() {
        val granted = ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) ==
            PackageManager.PERMISSION_GRANTED
        when {
            granted -> {
                cameraMessage = null
                onScanClicked()
            }
            activity != null && ActivityCompat.shouldShowRequestPermissionRationale(
                activity,
                Manifest.permission.CAMERA
            ) -> {
                showPermissionRationale = true
            }
            else -> {
                cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
            }
        }
    }

    fun openAppSettings() {
        val intent = Intent(
            Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
            Uri.fromParts("package", context.packageName, null)
        )
        context.startActivity(intent)
    }

    val green = MaterialTheme.colorScheme.primary
    val lightBg = MaterialTheme.colorScheme.primaryContainer
    val hasTotes = scannedByCompany.values.any { it.isNotEmpty() }

    LaunchedEffect(totalToteCount) {
        if (totalToteCount > 0 && toteErrorMessage == null) {
            toteId = ""
            companyName = ""
            toteIdError = null
            companyNameError = null
        }
    }

    Column(modifier = Modifier.fillMaxSize().background(Color.White)) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 56.dp, start = 24.dp, end = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Image(
                painter = painterResource(id = R.drawable.obklogo2),
                contentDescription = "OBK Logo",
                modifier = Modifier.size(190.dp)
            )
            Spacer(Modifier.height(10.dp))
            Text(
                text = "Welcome to Tote Checkout!",
                fontSize = fs(24),
                fontWeight = FontWeight.ExtraBold,
                color = Color.Black,
                textAlign = TextAlign.Center
            )
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .background(
                    color = lightBg,
                    shape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp)
                )
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 26.dp, vertical = 26.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(Modifier.height(10.dp))

            Text(
                text = "Enter Tote details to checkout",
                modifier = Modifier.fillMaxWidth(),
                color = Color.Black,
                fontSize = fs(13),
                fontWeight = FontWeight.Medium
            )

            Spacer(Modifier.height(10.dp))

            OutlinedTextField(
                value = toteId,
                onValueChange = {
                    toteId = it
                    if (!toteIdError.isNullOrEmpty()) toteIdError = null
                    if (toteErrorMessage != null) onDismissToteError()
                },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Tote ID", fontSize = fs(12)) },
                placeholder = {
                    Text(
                        "Enter Tote ID",
                        fontSize = fs(13),
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.45f)
                    )
                },
                singleLine = true
            )

            if (!toteIdError.isNullOrEmpty()) {
                Spacer(Modifier.height(8.dp))
                Text(text = toteIdError!!, color = MaterialTheme.colorScheme.error, fontSize = fs(12))
            }

            Spacer(Modifier.height(12.dp))

            OutlinedTextField(
                value = companyName,
                onValueChange = {
                    companyName = it
                    if (!companyNameError.isNullOrEmpty()) companyNameError = null
                    if (toteErrorMessage != null) onDismissToteError()
                },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Company Name", fontSize = fs(12)) },
                placeholder = {
                    Text(
                        "Enter Company Name",
                        fontSize = fs(13),
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.45f)
                    )
                },
                singleLine = true
            )

            if (!companyNameError.isNullOrEmpty()) {
                Spacer(Modifier.height(8.dp))
                Text(text = companyNameError!!, color = MaterialTheme.colorScheme.error, fontSize = fs(12))
            }

            if (!toteErrorMessage.isNullOrBlank()) {
                Spacer(Modifier.height(8.dp))
                Text(
                    text = toteErrorMessage,
                    color = MaterialTheme.colorScheme.error,
                    fontSize = fs(12),
                    modifier = Modifier.fillMaxWidth()
                )
            }

            Spacer(Modifier.height(18.dp))

            Button(
                onClick = {
                    val trimmedToteId = toteId.trim()
                    val trimmedCompanyName = companyName.trim()

                    toteIdError = if (trimmedToteId.isBlank()) "Please enter a Tote ID." else null
                    companyNameError = if (trimmedCompanyName.isBlank()) "Please enter a Company Name." else null

                    if (toteIdError == null && companyNameError == null) {
                        onManualAdd(trimmedToteId, trimmedCompanyName)
                    }
                },
                modifier = Modifier.fillMaxWidth().height(56.dp),
                colors = ButtonDefaults.buttonColors(containerColor = green),
                shape = RoundedCornerShape(10.dp)
            ) {
                Text(
                    "Add",
                    color = MaterialTheme.colorScheme.onPrimary,
                    fontWeight = FontWeight.Bold,
                    fontSize = fs(15)
                )
            }

            Spacer(Modifier.height(22.dp))

            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                HorizontalDivider(modifier = Modifier.weight(1f))
                Text(text = "  or  ", color = Color.Gray, fontSize = fs(12))
                HorizontalDivider(modifier = Modifier.weight(1f))
            }

            Spacer(Modifier.height(22.dp))

            OutlinedButton(
                onClick = { launchScanner() },
                modifier = Modifier.fillMaxWidth().height(56.dp),
                border = BorderStroke(2.dp, green),
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.outlinedButtonColors(containerColor = Color.White)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "Scan QR Code",
                        color = green,
                        fontWeight = FontWeight.Bold,
                        fontSize = fs(14)
                    )
                    Spacer(Modifier.size(10.dp))
                    androidx.compose.material3.Icon(
                        imageVector = Icons.Filled.QrCode2,
                        contentDescription = "QR",
                        tint = green,
                        modifier = Modifier.size(22.dp)
                    )
                }
            }

            if (!cameraMessage.isNullOrBlank()) {
                Spacer(Modifier.height(8.dp))
                Text(
                    text = cameraMessage!!,
                    color = MaterialTheme.colorScheme.error,
                    fontSize = fs(12),
                    modifier = Modifier.fillMaxWidth()
                )
            }

            Spacer(Modifier.height(16.dp))

            OutlinedButton(
                onClick = onLogout,
                modifier = Modifier.fillMaxWidth().height(48.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.error),
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.outlinedButtonColors(containerColor = Color.White)
            ) {
                Text(
                    "Logout",
                    color = MaterialTheme.colorScheme.error,
                    fontWeight = FontWeight.Medium,
                    fontSize = fs(14)
                )
            }

            Spacer(Modifier.height(24.dp))
            HorizontalDivider()
            Spacer(Modifier.height(14.dp))
            Text(
                text = "Totes added",
                fontSize = fs(14),
                fontWeight = FontWeight.Bold,
                color = Color.Black,
                modifier = Modifier.fillMaxWidth()
            )
            if (hasTotes) {
                scannedByCompany.toSortedMap().forEach { (company, ids) ->
                    Text(
                        text = company.ifBlank { "UNKNOWN" },
                        fontSize = fs(14),
                        fontWeight = FontWeight.Bold,
                        color = Color.Black,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 14.dp, bottom = 8.dp)
                    )
                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        ids.forEach { id ->
                            OBKIdChip(id = id, onRemove = { onRemove(company, id) })
                        }
                    }
                }
            } else {
                Spacer(Modifier.height(8.dp))
                Text(
                    text = "No totes added yet.",
                    color = Color.Gray,
                    fontSize = fs(12),
                    modifier = Modifier.fillMaxWidth()
                )
            }

            Spacer(Modifier.height(20.dp))
            Button(
                onClick = onProceed,
                enabled = hasTotes,
                modifier = Modifier.fillMaxWidth().height(56.dp),
                colors = ButtonDefaults.buttonColors(containerColor = green),
                shape = RoundedCornerShape(10.dp)
            ) {
                Text(
                    "Continue",
                    color = MaterialTheme.colorScheme.onPrimary,
                    fontWeight = FontWeight.Bold,
                    fontSize = fs(14)
                )
            }
            Spacer(Modifier.height(10.dp))
        }
    }

    if (showPermissionRationale) {
        AlertDialog(
            onDismissRequest = { showPermissionRationale = false },
            title = { Text("Allow Camera Access") },
            text = {
                Text("Camera access is needed to scan QR codes. Please allow camera access to continue scanning.")
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showPermissionRationale = false
                        cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
                    }
                ) {
                    Text("Continue")
                }
            },
            dismissButton = {
                TextButton(onClick = { showPermissionRationale = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    if (showPermissionSettings) {
        AlertDialog(
            onDismissRequest = { showPermissionSettings = false },
            title = { Text("Turn On Camera Access") },
            text = {
                Text("Camera access is needed to scan QR codes. Please allow camera access in Android Settings to continue scanning.")
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showPermissionSettings = false
                        openAppSettings()
                    }
                ) {
                    Text("Open Settings")
                }
            },
            dismissButton = {
                TextButton(onClick = { showPermissionSettings = false }) {
                    Text("Not now")
                }
            }
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ConfirmScreen(
    scannedByCompany: Map<String, List<String>>,
    onRemove: (company: String, toteId: String) -> Unit,
    onAddMore: () -> Unit,
    onBack: () -> Unit,
    onProceed: () -> Unit
) {
    val green = MaterialTheme.colorScheme.primary

    Column(modifier = Modifier.fillMaxSize()) {
        OBKHeaderBarWithBack(onBack = onBack)

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp, vertical = 14.dp)
        ) {
            Text(
                text = "Confirm",
                fontSize = fs(22),
                fontWeight = FontWeight.Bold,
                color = Color.Black
            )

            Text(
                text = "Please confirm all details are correct or go back.",
                fontSize = fs(13),
                fontWeight = FontWeight.Medium,
                color = Color.DarkGray,
                modifier = Modifier.padding(top = 8.dp, bottom = 18.dp)
            )

            if (scannedByCompany.isEmpty()) {
                Text(text = "No totes scanned yet.", color = Color.Gray, fontSize = fs(13))
            } else {
                scannedByCompany.toSortedMap().forEach { (company, ids) ->
                    Text(
                        text = company.ifBlank { "UNKNOWN" },
                        fontSize = fs(14),
                        fontWeight = FontWeight.Bold,
                        color = Color.Black,
                        modifier = Modifier.padding(top = 14.dp, bottom = 8.dp)
                    )
                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        ids.forEach { id ->
                            OBKIdChip(id = id, onRemove = { onRemove(company, id) })
                        }
                    }
                }
            }

            Spacer(Modifier.weight(1f))

            OutlinedButton(
                onClick = onAddMore,
                modifier = Modifier.fillMaxWidth().height(56.dp),
                border = BorderStroke(2.dp, green),
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.outlinedButtonColors(containerColor = Color.White)
            ) {
                Text("Add More", color = green, fontWeight = FontWeight.Bold, fontSize = fs(14))
            }

            Spacer(Modifier.height(12.dp))

            Button(
                onClick = onProceed,
                modifier = Modifier.fillMaxWidth().height(56.dp),
                colors = ButtonDefaults.buttonColors(containerColor = green),
                shape = RoundedCornerShape(10.dp)
            ) {
                Text(
                    "Proceed",
                    color = MaterialTheme.colorScheme.onPrimary,
                    fontWeight = FontWeight.Bold,
                    fontSize = fs(14)
                )
            }

            Spacer(Modifier.height(10.dp))
        }
    }
}

private fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}

@Composable
private fun OBKHeaderBarWithBack(onBack: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.primary)
            .padding(horizontal = 16.dp, vertical = 10.dp)
    ) {
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Image(
                painter = painterResource(id = R.drawable.obklogo2),
                contentDescription = "OBK Logo Small",
                modifier = Modifier.height(38.dp)
            )
        }

        TextButton(
            onClick = onBack,
            modifier = Modifier.padding(top = 6.dp)
        ) {
            Text(
                "Back",
                color = MaterialTheme.colorScheme.onPrimary,
                fontWeight = FontWeight.Bold,
                fontSize = fs(13)
            )
        }
    }
}

@Composable
private fun OBKIdChip(id: String, onRemove: () -> Unit) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondary),
        shape = RoundedCornerShape(8.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 9.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = id,
                color = MaterialTheme.colorScheme.onSecondary,
                fontWeight = FontWeight.Bold,
                fontSize = fs(12)
            )
            Spacer(Modifier.size(10.dp))
            Text(
                text = "x",
                color = MaterialTheme.colorScheme.onSecondary,
                fontWeight = FontWeight.Bold,
                fontSize = fs(14),
                modifier = Modifier
                    .clickable { onRemove() }
                    .padding(horizontal = 4.dp)
            )
        }
    }
}
