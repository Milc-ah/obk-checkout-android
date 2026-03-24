package com.example.obkcheckout

import android.util.Log
import java.security.MessageDigest

@OptIn(ExperimentalStdlibApi::class)
class Authentication(private val api: ApiQairosService) {

    fun solveUserChallenge(serverChallenge64: String, password: String): String {
        val userChallengeAnswer = android.util.Base64.decode(serverChallenge64.trim(), android.util.Base64.URL_SAFE)
        val md5 = MessageDigest.getInstance("MD5")
        val userSecret = RC6.decrypt(
            userChallengeAnswer.copyOfRange(16, userChallengeAnswer.size),
            md5.digest(password.toByteArray()).toHexString().uppercase().encodeToByteArray()
        )
        return android.util.Base64.encodeToString(
            userChallengeAnswer.copyOfRange(0, 16) +
                RC6.encrypt(userChallengeAnswer.copyOfRange(0, 16), userSecret),
            android.util.Base64.URL_SAFE or android.util.Base64.NO_WRAP
        )
    }

    suspend fun login(userEmail: String, password: String?): String {
        return try {
            val responseChallenge = api.login(userEmail, null)
            Log.d("Authentication", "STEP 1 code: ${responseChallenge.code()}")

            val challenge = when (responseChallenge.code()) {
                401 -> responseChallenge.errorBody()?.string()
                200 -> responseChallenge.body()
                else -> null
            }
            Log.d("Authentication", "STEP 1 challenge: $challenge")

            if (challenge.isNullOrBlank()) {
                return "Login failed (no challenge received)"
            }

            // Validate the challenge is a proper base64 token (must decode to ≥16 bytes).
            // If it's shorter the server returned an error message, not a real challenge.
            val decodedChallenge = android.util.Base64.decode(challenge.trim(), android.util.Base64.URL_SAFE)
            if (decodedChallenge.size < 16) {
                return "Login failed: ${challenge.trim()}"
            }

            val solved = solveUserChallenge(challenge, password ?: "")
            Log.d("Authentication", "STEP 1 solved: $solved")

            val responseToken = api.login(userEmail, solved)
            Log.d("Authentication", "STEP 2 code: ${responseToken.code()}")
            Log.d("Authentication", "STEP 2 body: ${responseToken.body()}")
            Log.d("Authentication", "STEP 2 error: ${responseToken.errorBody()?.string()}")

            when {
                responseToken.isSuccessful && !responseToken.body().isNullOrBlank() -> {
                    TokenStore.store(responseToken.body()!!)
                    responseToken.body()!!
                }
                responseToken.code() == 401 -> "Invalid user/password"
                else -> "Login failed (code ${responseToken.code()})"
            }
        } catch (e: Exception) {
            Log.e("Authentication", "Login exception", e)
            "Network error: ${e.message}"
        }
    }
}
