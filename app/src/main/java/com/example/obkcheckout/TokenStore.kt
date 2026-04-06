package com.example.obkcheckout

/**
 * Process-scoped singleton that holds the session token received after login.
 * All API calls read [bearerToken] for the Authorization header.
 * Call [clear] on logout so the token cannot be reused.
 */
object TokenStore {
    var token: String = ""
        private set

    /** Returns "Bearer <token>", or an empty string if not logged in. */
    val bearerToken: String
        get() = if (token.isNotEmpty()) "Bearer $token" else ""

    fun store(rawToken: String) {
        token = rawToken.trim()
    }

    fun clear() {
        token = ""
    }
}
//