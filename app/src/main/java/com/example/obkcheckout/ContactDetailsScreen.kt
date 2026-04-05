package com.example.obkcheckout

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.unit.dp

/*
Role options are currently defined on the app side for UI completeness.
If roles should come from backend, replace ROLE_OPTIONS with a parameter:
- ContactDetailsScreen(..., roleOptions: List<String>, ...)
and pass them from MainActivity once backend wiring is ready.
*/
private val ROLE_OPTIONS = listOf(
    "Select role",
    "Driver",
    "Warehouse",
    "Volunteer",
    "Team Leader",
    "Manager"
)

@Composable
fun ContactDetailsScreen(
    initialContact: SavedContact,
    lookedUpContact: SavedContact? = null,
    onBack: () -> Unit,
    onContinue: (SavedContact) -> Unit,
    onRequestLookup: ((email: String) -> Unit)? = null
) {
    var fullName by remember { mutableStateOf(initialContact.fullName) }
    var phone    by remember { mutableStateOf(initialContact.phone) }
    var email    by remember { mutableStateOf(initialContact.email) }
    var role     by remember { mutableStateOf(initialContact.role) }

    // Re-fill fields when navigating back to edit saved state
    LaunchedEffect(initialContact) {
        fullName = initialContact.fullName
        phone    = initialContact.phone
        email    = initialContact.email
        role     = initialContact.role
    }

    // Auto-fill from backend lookup result; only overwrite blank/unchanged fields
    LaunchedEffect(lookedUpContact) {
        lookedUpContact?.let { found ->
            if (found.fullName.isNotBlank()) fullName = found.fullName
            if (found.phone.isNotBlank()) phone = found.phone
            if (found.role.isNotBlank()) role = found.role
        }
    }

    var fullNameError by remember { mutableStateOf<String?>(null) }
    var phoneError    by remember { mutableStateOf<String?>(null) }
    var emailError    by remember { mutableStateOf<String?>(null) }
    var roleError     by remember { mutableStateOf<String?>(null) }

    fun normalizeEmail(raw: String) = raw.trim().lowercase()

    fun validate(): Boolean {
        var ok = true
        val n = fullName.trim()
        val p = phone.trim()
        val e = normalizeEmail(email)
        val r = role.trim()

        fullNameError = null
        phoneError    = null
        emailError    = null
        roleError     = null

        if (n.isBlank()) { fullNameError = "Full name is required."; ok = false }

        val digits = p.filter { it.isDigit() }
        when {
            p.isBlank()              -> { phoneError = "Phone number is required."; ok = false }
            digits.length !in 8..15  -> { phoneError = "Enter a valid phone number (8-15 digits)."; ok = false }
        }

        if (e.isBlank()) {
            emailError = "Email is required."; ok = false
        } else {
            val emailRegex = Regex("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$")
            if (!emailRegex.matches(e)) { emailError = "Enter a valid email address."; ok = false }
        }

        if (r.isBlank() || r == "Select role") { roleError = "Please select a role."; ok = false }

        return ok
    }

    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
    ) {
        OBKHeader(onBack = onBack)

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp, vertical = 16.dp)
                .verticalScroll(scrollState)
        ) {
            Text(
                text = "Contact Details",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.ExtraBold,
                color = Color.Black
            )

            Spacer(Modifier.height(14.dp))
            OrangeAccentCard(
                modifier = Modifier.fillMaxWidth(),
            ) {
                Column(
                    modifier = Modifier.padding(18.dp)
                ) {
                    // Email first — triggers backend lookup on focus lost
                    OutlinedTextField(
                        value = email,
                        onValueChange = { email = it; emailError = null },
                        label = { Text("Email address") },
                        singleLine = true,
                        isError = emailError != null,
                        modifier = Modifier
                            .fillMaxWidth()
                            .onFocusChanged { focus ->
                                if (!focus.isFocused) {
                                    val e = normalizeEmail(email)
                                    val emailRegex = Regex("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$")
                                    if (emailRegex.matches(e)) onRequestLookup?.invoke(e)
                                }
                            }
                    )
                    if (emailError != null) {
                        Spacer(Modifier.height(4.dp))
                        Text(emailError!!, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                    }

                    Spacer(Modifier.height(12.dp))

                    OutlinedTextField(
                        value = fullName,
                        onValueChange = { fullName = it; fullNameError = null },
                        label = { Text("Full name") },
                        singleLine = true,
                        isError = fullNameError != null,
                        modifier = Modifier.fillMaxWidth()
                    )
                    if (fullNameError != null) {
                        Spacer(Modifier.height(4.dp))
                        Text(fullNameError!!, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                    }

                    Spacer(Modifier.height(12.dp))

                    OutlinedTextField(
                        value = phone,
                        onValueChange = { phone = it; phoneError = null },
                        label = { Text("Phone number") },
                        singleLine = true,
                        isError = phoneError != null,
                        modifier = Modifier.fillMaxWidth()
                    )
                    if (phoneError != null) {
                        Spacer(Modifier.height(4.dp))
                        Text(phoneError!!, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                    }

                    Spacer(Modifier.height(12.dp))

                    RoleDropdown(
                        role = role,
                        onRoleChange = { role = it; roleError = null },
                        isError = roleError != null,
                        errorText = roleError
                    )
                }
            }

            Spacer(Modifier.height(18.dp))

            Button(
                onClick = {
                    if (!validate()) return@Button
                    onContinue(
                        SavedContact(
                            fullName = fullName.trim(),
                            phone    = phone.trim(),
                            email    = normalizeEmail(email),
                            role     = role.trim()
                        )
                    )
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
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(Modifier.height(10.dp))
        }
    }
}

@Composable
private fun RoleDropdown(
    role: String,
    onRoleChange: (String) -> Unit,
    isError: Boolean,
    errorText: String?
) {
    var expanded by remember { mutableStateOf(false) }
    var selectedRole by remember(role) { mutableStateOf(role.takeIf { it in ROLE_OPTIONS } ?: "") }
    var customRole by remember(role) { mutableStateOf(role.takeIf { it !in ROLE_OPTIONS } ?: "") }

    Column(modifier = Modifier.fillMaxWidth()) {
        Box(modifier = Modifier.fillMaxWidth()) {
            OutlinedTextField(
                value = selectedRole.ifBlank { "Select role" },
                onValueChange = {},
                readOnly = true,
                label = { Text("Role") },
                isError = isError,
                trailingIcon = {
                    Icon(
                        imageVector = Icons.Filled.ArrowDropDown,
                        contentDescription = "Select role"
                    )
                },
                modifier = Modifier.fillMaxWidth()
            )

            DropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false }
            ) {
                ROLE_OPTIONS.forEach { option ->
                    DropdownMenuItem(
                        text = { Text(option) },
                        onClick = {
                            expanded = false
                            selectedRole = option
                            customRole = ""
                            onRoleChange(option)
                        }
                    )
                }
            }

            Box(modifier = Modifier.matchParentSize().clickable { expanded = true })
        }

        Spacer(Modifier.height(8.dp))

        OutlinedTextField(
            value = customRole,
            onValueChange = {
                customRole = it
                if (it.isNotBlank()) {
                    selectedRole = ""
                    onRoleChange(it)
                } else {
                    onRoleChange(selectedRole)
                }
            },
            label = { Text("Or enter role manually") },
            singleLine = true,
            isError = isError,
            modifier = Modifier.fillMaxWidth()
        )

        if (errorText != null) {
            Spacer(Modifier.height(4.dp))
            Text(
                text = errorText,
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall
            )
        }
    }
}
