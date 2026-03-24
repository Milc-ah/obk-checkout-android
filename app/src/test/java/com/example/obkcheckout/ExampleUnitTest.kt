package com.example.obkcheckout

import org.junit.Test
import org.junit.Assert.*

/**
 * Unit tests for pure checkout logic — no Android framework required.
 *
 * Run with: ./gradlew test
 */
class CheckoutUnitTests {

    // -----------------------------------------------------------------------
    // MEALS_PER_TOTE constant
    // -----------------------------------------------------------------------

    @Test
    fun mealsPerTote_isThirtySix() {
        assertEquals(36, MEALS_PER_TOTE)
    }

    // -----------------------------------------------------------------------
    // normalizeToToteId
    // -----------------------------------------------------------------------

    @Test
    fun normalize_stripsLeadingHash() {
        assertEquals("123", normalizeToToteId("#123"))
    }

    @Test
    fun normalize_trimsWhitespace() {
        assertEquals("456", normalizeToToteId("  456  "))
    }

    @Test
    fun normalize_stripsHashAndWhitespace() {
        assertEquals("789", normalizeToToteId(" #789 "))
    }

    @Test
    fun normalize_takesLeadingDigitsOnly() {
        assertEquals("123", normalizeToToteId("123abc"))
    }

    @Test
    fun normalize_nonDigitValuePassesThroughCleaned() {
        assertEquals("ABCDEF", normalizeToToteId("ABCDEF"))
    }

    @Test
    fun normalize_emptyStringReturnsEmpty() {
        assertEquals("", normalizeToToteId(""))
    }

    @Test
    fun normalize_hashOnlyReturnsEmpty() {
        assertEquals("", normalizeToToteId("#"))
    }

    // -----------------------------------------------------------------------
    // Phone number digit-count validation (mirrors ContactDetailsScreen logic)
    // -----------------------------------------------------------------------

    @Test
    fun phoneDigits_eightIsValid() {
        val count = "12345678".filter { it.isDigit() }.length
        assertTrue(count in 8..15)
    }

    @Test
    fun phoneDigits_fifteenIsValid() {
        val count = "123456789012345".filter { it.isDigit() }.length
        assertTrue(count in 8..15)
    }

    @Test
    fun phoneDigits_sevenIsTooShort() {
        val count = "1234567".filter { it.isDigit() }.length
        assertFalse(count in 8..15)
    }

    @Test
    fun phoneDigits_sixteenIsTooLong() {
        val count = "1234567890123456".filter { it.isDigit() }.length
        assertFalse(count in 8..15)
    }

    // -----------------------------------------------------------------------
    // Email regex (mirrors ContactDetailsScreen logic)
    // -----------------------------------------------------------------------

    private val emailRegex = Regex("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$")

    @Test
    fun email_simpleAddressIsValid() {
        assertTrue(emailRegex.matches("user@example.com"))
    }

    @Test
    fun email_plusTagAddressIsValid() {
        assertTrue(emailRegex.matches("user+tag@sub.example.co.uk"))
    }

    @Test
    fun email_missingAtSignIsInvalid() {
        assertFalse(emailRegex.matches("notanemail"))
    }

    @Test
    fun email_missingTldIsInvalid() {
        assertFalse(emailRegex.matches("user@nodomain"))
    }

    @Test
    fun email_leadingAtSignIsInvalid() {
        assertFalse(emailRegex.matches("@nodomain.com"))
    }

    // -----------------------------------------------------------------------
    // SavedContact defaults
    // -----------------------------------------------------------------------

    @Test
    fun savedContact_defaultsAreAllEmpty() {
        val contact = SavedContact()
        assertEquals("", contact.fullName)
        assertEquals("", contact.phone)
        assertEquals("", contact.email)
        assertEquals("", contact.role)
    }
}
