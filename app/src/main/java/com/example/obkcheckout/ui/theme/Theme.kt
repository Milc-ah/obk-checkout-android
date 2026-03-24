package com.example.obkcheckout.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

/**
 * OBK colour scheme — always light, no dynamic colour.
 *
 * Role mapping:
 *   primary          → OBK Green  (#13A84A)  — buttons, headers, FABs
 *   secondary        → OBK Orange (#E4572E)  — tote chips, accent chips
 *   primaryContainer → Light green (#E9F6EC) — start-screen panel
 *   surface          → Card grey  (#F2F2F2)  — review / thank-you cards
 *   background       → White                 — page backgrounds
 *   error            → Material red          — validation messages
 */
private val OBKColorScheme = lightColorScheme(
    primary             = OBKGreen,
    onPrimary           = Color.White,
    primaryContainer    = OBKLightBg,
    onPrimaryContainer  = Color(0xFF00210B),

    secondary           = OBKOrange,
    onSecondary         = Color.White,
    secondaryContainer  = Color(0xFFFFDBCF),
    onSecondaryContainer = Color(0xFF3A0A00),

    background          = Color.White,
    onBackground        = Color(0xFF111111),

    surface             = OBKCardBg,
    onSurface           = Color(0xFF111111),
    surfaceVariant      = OBKLightBg,
    onSurfaceVariant    = Color(0xFF111111),

    error               = Color(0xFFB00020),
    onError             = Color.White,
)

@Composable
fun OBKCheckoutTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = OBKColorScheme,
        typography  = Typography,
        content     = content
    )
}
