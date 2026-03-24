package com.example.obkcheckout

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch

/*
Backend integration points:
- Replace BackendAuth.login(...) and BackendAuth.requestPasswordReset(...)
  with real API calls (Retrofit / Ktor).
- Persist session token if required (e.g., DataStore) and handle logout / expiry.
*/

private fun normalizeEmail(raw: String): String = raw.trim()

/**
 * Backend auth facade (placeholder).
 * Backend team replaces these with Retrofit/Ktor client calls.
 */
private object BackendAuth {
    /** Validate admin credentials and return a session token. */
    suspend fun login(email: String, password: String): Result<Unit> =
        Result.failure(NotImplementedError("Backend login not implemented"))

    /** Send password-reset email if account exists (always return generic success). */
    suspend fun requestPasswordReset(email: String): Result<Unit> =
        Result.failure(NotImplementedError("Backend password reset not implemented"))
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoginScreen(
    onLoginSuccess: () -> Unit,
    modifier: Modifier = Modifier
) {
    @Suppress("UNUSED_VARIABLE")
    val context = LocalContext.current
    val scope             = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    var email          by remember { mutableStateOf("") }
    var password       by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    var errorText      by remember { mutableStateOf<String?>(null) }
    var isLoading      by remember { mutableStateOf(false) }

    var showForgotDialog by remember { mutableStateOf(false) }
    var forgotEmail      by remember { mutableStateOf("") }

    val textFieldColors = OutlinedTextFieldDefaults.colors(
        focusedBorderColor = MaterialTheme.colorScheme.primary,
        focusedLabelColor  = MaterialTheme.colorScheme.primary,
        cursorColor        = MaterialTheme.colorScheme.primary
    )

    Scaffold(
        modifier = modifier,
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Login") },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor   = Color.White,
                    titleContentColor = Color.Black
                )
            )
        },
        containerColor = Color.White
    ) { padding ->

        Column(
            modifier = Modifier
                .padding(padding)
                .padding(20.dp)
                .fillMaxSize(),
            verticalArrangement = Arrangement.Top
        ) {
            Spacer(Modifier.height(18.dp))

            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Image(
                    painter = painterResource(id = R.drawable.obklogo2),
                    contentDescription = "OBK Logo",
                    modifier = Modifier
                        .size(96.dp)
                        .clip(CircleShape)
                )
                Spacer(Modifier.height(10.dp))
                Text(
                    text = "OBK Checkout",
                    style = MaterialTheme.typography.headlineSmall,
                    color = Color.Black
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = "Admin login",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color(0xFF666666)
                )
            }

            Spacer(Modifier.height(26.dp))

            OutlinedTextField(
                value = email,
                onValueChange = { email = it; errorText = null },
                label = { Text("Email") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                colors = textFieldColors,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(Modifier.height(12.dp))

            OutlinedTextField(
                value = password,
                onValueChange = { password = it; errorText = null },
                label = { Text("Password") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                visualTransformation = if (passwordVisible) VisualTransformation.None
                                       else PasswordVisualTransformation(),
                trailingIcon = {
                    IconButton(onClick = { passwordVisible = !passwordVisible }) {
                        Icon(
                            imageVector = if (passwordVisible) Icons.Filled.VisibilityOff
                                          else Icons.Filled.Visibility,
                            contentDescription = if (passwordVisible) "Hide password" else "Show password",
                            tint = Color(0xFF444444)
                        )
                    }
                },
                colors = textFieldColors,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(Modifier.height(6.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                Text(
                    text = "Forgot password?",
                    color = MaterialTheme.colorScheme.primary,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.clickable {
                        forgotEmail = email
                        showForgotDialog = true
                    }
                )
            }

            if (errorText != null) {
                Spacer(Modifier.height(10.dp))
                Text(
                    text = errorText!!,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall
                )
            }

            Spacer(Modifier.height(14.dp))

            Button(
                onClick = {
                    val e = normalizeEmail(email)
                    when {
                        e.isBlank() || password.isBlank() ->
                            errorText = "Please enter email and password."
                        !e.contains("@") ->
                            errorText = "Please enter a valid email."
                        else -> {
                            isLoading = true
                            errorText = null
                            scope.launch {
                                val errorMsg = runCatching {
                                    Authentication(QairosRetrofit.api).login(e, password)
                                }.getOrNull()
                                isLoading = false
                                if (TokenStore.token.isNotEmpty()) {
                                    onLoginSuccess()
                                } else {
                                    errorText = errorMsg
                                        ?: "Login failed. Please check your details or contact your supervisor."
                                }
                            }
                        }
                    }
                },
                enabled = !isLoading,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor   = MaterialTheme.colorScheme.onPrimary
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(Modifier.padding(start = 10.dp))
                }
                Text("LOGIN")
            }
        }

        if (showForgotDialog) {
            AlertDialog(
                onDismissRequest = { showForgotDialog = false },
                title = { Text("Reset password") },
                text = {
                    Column {
                        Text("Enter your email and we'll send you a reset link.")
                        Spacer(Modifier.height(10.dp))
                        OutlinedTextField(
                            value = forgotEmail,
                            onValueChange = { forgotEmail = it },
                            label = { Text("Email") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                },
                confirmButton = {
                    TextButton(
                        onClick = {
                            val e = normalizeEmail(forgotEmail)
                            showForgotDialog = false
                            scope.launch {
                                if (e.isBlank() || !e.contains("@")) {
                                    snackbarHostState.showSnackbar("Please enter a valid email.")
                                    return@launch
                                }
                                BackendAuth.requestPasswordReset(e)
                                snackbarHostState.showSnackbar(
                                    "If an account exists for that email, a reset link will be sent."
                                )
                            }
                        }
                    ) {
                        Text("Send", color = MaterialTheme.colorScheme.primary)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showForgotDialog = false }) { Text("Cancel") }
                }
            )
        }
    }
}
