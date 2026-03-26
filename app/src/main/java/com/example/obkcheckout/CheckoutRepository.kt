package com.example.obkcheckout

interface CheckoutRepository {
    suspend fun resolveTote(rawValue: String, source: ToteSource): Result<ToteRecord>
    suspend fun loadCharityNames(): Result<List<String>>
    suspend fun submitCheckout(submission: CheckoutSubmission): Result<ConfirmCheckoutResponse>
}
