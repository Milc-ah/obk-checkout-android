package com.example.obkcheckout

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.QrCode2
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.obkcheckout.ui.theme.OBKCheckoutTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            OBKCheckoutTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    OBKApp()
                }
            }
        }
    }
}

/** Font scale helper (~20% bigger everywhere) */
private fun fs(base: Int) = (base * 1.2f).sp

@Composable
private fun OBKApp(vm: CheckoutViewModel = viewModel()) {
    val navController = rememberNavController()

    NavHost(navController = navController, startDestination = "login") {

        composable("login") {
            LoginScreen(
                onLoginSuccess = {
                    vm.loadCharities()
                    navController.navigate("start") {
                        popUpTo("login") { inclusive = true }
                        launchSingleTop = true
                    }
                }
            )
        }

        composable("start") {
            ToteCheckoutStartScreen(
                onScanClicked = { navController.navigate("scanner") },
                onManualSubmit = { toteId ->
                    vm.addManualToteId(toteId)
                    navController.navigate("confirm")
                },
                onLogout = {
                    TokenStore.clear()
                    vm.reset()
                    navController.navigate("login") {
                        popUpTo("start") { inclusive = true }
                        launchSingleTop = true
                    }
                }
            )
        }

        composable("scanner") {
            // Invert scannedByCompany so each toteId maps to its company name.
            // This is derived locally from the same state the VM already holds.
            val companyByToteId = vm.scannedByCompany
                .flatMap { (company, totes) -> totes.map { it to company } }
                .toMap()

            ContinuousScannerScreen(
                scannedByCompany = vm.scannedByCompany,
                companyByToteId  = companyByToteId,
                onScannedTote    = { toteId -> vm.addScannedId(toteId) },
                onManualToteId   = { toteId -> vm.addManualToteId(toteId) },
                onRemoveTote     = { company, toteId -> vm.removeScannedId(company, toteId) },
                onDone           = { navController.navigate("confirm") },
                onBack           = { navController.popBackStack() }
            )
        }

        composable("confirm") {
            ConfirmScreen(
                scannedByCompany = vm.scannedByCompany,
                onRemove         = { company, id -> vm.removeScannedId(company, id) },
                onAddMore        = { navController.navigate("scanner") },
                onBack           = { navController.popBackStack() },
                onProceed        = { navController.navigate("charityDestination?returnToReview=false") }
            )
        }

        composable(route = "charityDestination?returnToReview={returnToReview}") { backStackEntry ->
            val returnToReview =
                backStackEntry.arguments?.getString("returnToReview")?.toBoolean() ?: false

            CharityDestinationScreen(
                selectedCharity  = vm.selectedCharity,
                charities        = vm.charityNames,
                onCharitySelected = { vm.setSelectedCharity(it) },
                onBack           = { navController.popBackStack() },
                onSplitByCharity = {
                    vm.setSplitEnabled(true)
                    navController.navigate("splitByCharity?returnToReview=$returnToReview")
                },
                onContinue = {
                    vm.setSplitEnabled(false)
                    vm.setAssignedByToteId(emptyMap())
                    if (returnToReview) {
                        vm.rebuildReviewSummary()
                        navController.popBackStack("review", inclusive = false)
                    } else {
                        navController.navigate("contactDetails")
                    }
                }
            )
        }

        composable(route = "splitByCharity?returnToReview={returnToReview}") { backStackEntry ->
            val returnToReview =
                backStackEntry.arguments?.getString("returnToReview")?.toBoolean() ?: false
            val allTotes = vm.scannedByCompany.values.flatten().distinct().sorted()
            SplitByCharityScreen(
                toteIds   = allTotes,
                charities = vm.charityNames,
                onBack    = { navController.popBackStack() },
                onContinue = { assignedMap ->
                    vm.setSplitEnabled(true)
                    vm.setAssignedByToteId(assignedMap)
                    if (returnToReview) {
                        vm.rebuildReviewSummary()
                        navController.popBackStack("review", inclusive = false)
                    } else {
                        navController.navigate("contactDetails")
                    }
                }
            )
        }

        composable("contactDetails") {
            ContactDetailsScreen(
                initialContact = vm.contactDetails,
                onBack         = { navController.popBackStack() },
                onContinue     = { savedContact ->
                    vm.buildAndSetReviewSummary(savedContact)
                    navController.navigate("review")
                }
            )
        }

        composable("review") {
            val summary: ReviewSummary? = vm.reviewSummary
            if (summary != null) {
                ReviewScreen(
                    summary         = summary,
                    onBack          = { navController.popBackStack() },
                    onEditCharities = {
                        if (vm.splitEnabled) {
                            navController.navigate("splitByCharity?returnToReview=true")
                        } else {
                            navController.navigate("charityDestination?returnToReview=true")
                        }
                    },
                    onEditContact = { navController.navigate("contactDetails") },
                    onContinue    = {
                        vm.submitCheckout {
                            navController.navigate("thankYou")
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
                    navController.popBackStack("contactDetails", inclusive = false)
                }
            }
        }

        composable("thankYou") {
            val summary: ReviewSummary? = vm.reviewSummary
            if (summary != null) {
                ThankYouScreen(
                    confirmationId  = vm.confirmationId,
                    mealsGrandTotal = summary.mealsGrandTotal,
                    companies       = summary.companies,
                    contactName     = summary.contact.fullName,
                    onFinish        = {
                        vm.reset()
                        navController.navigate("start") {
                            popUpTo("start") { inclusive = true }
                            launchSingleTop = true
                        }
                    }
                )
            } else {
                LaunchedEffect(Unit) {
                    navController.navigate("start") {
                        popUpTo("start") { inclusive = true }
                        launchSingleTop = true
                    }
                }
            }
        }
    }
}

// ---------------------------------------------------------------------------
// Screen composables that live in MainActivity
// ---------------------------------------------------------------------------

@Composable
private fun ToteCheckoutStartScreen(
    onScanClicked: () -> Unit,
    onManualSubmit: (String) -> Unit,
    onLogout: () -> Unit
) {
    var toteId by remember { mutableStateOf("") }
    var error  by remember { mutableStateOf<String?>(null) }

    val green  = MaterialTheme.colorScheme.primary
    val lightBg = MaterialTheme.colorScheme.primaryContainer

    Box(modifier = Modifier.fillMaxSize().background(Color.White)) {

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.TopCenter)
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

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
                .height(520.dp)
                .background(
                    color = lightBg,
                    shape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp)
                )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 26.dp, vertical = 26.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Spacer(Modifier.height(10.dp))

                Text(
                    text = "Enter Tote ID to checkout",
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
                        if (!error.isNullOrEmpty()) error = null
                    },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("Enter Tote ID", fontSize = fs(13)) },
                    singleLine = true
                )

                if (!error.isNullOrEmpty()) {
                    Spacer(Modifier.height(8.dp))
                    Text(text = error!!, color = MaterialTheme.colorScheme.error, fontSize = fs(12))
                }

                Spacer(Modifier.height(18.dp))

                Button(
                    onClick = {
                        val trimmed = toteId.trim()
                        if (trimmed.isEmpty()) {
                            error = "Please enter a Tote ID."
                        } else {
                            onManualSubmit(trimmed)
                            toteId = ""
                        }
                    },
                    modifier = Modifier.fillMaxWidth().height(56.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = green),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text(
                        "Continue",
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
                    Divider(modifier = Modifier.weight(1f))
                    Text(text = "  or  ", color = Color.Gray, fontSize = fs(12))
                    Divider(modifier = Modifier.weight(1f))
                }

                Spacer(Modifier.height(22.dp))

                OutlinedButton(
                    onClick = onScanClicked,
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
            }
        }
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
                scannedByCompany.forEach { (company, ids) ->
                    Text(
                        text = company,
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
                "← Back",
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
                text = "×",
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
