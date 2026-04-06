package com.example.obkcheckout

internal data class ParsedQrCode(
    val companyName: String,
    val batchId: String,
    val mealsInBatch: Int,
    val toteId: String,
    val mealsInTote: Int,
    val checkedInDate: String
)

internal fun parseQrCode(raw: String): ParsedQrCode? {
    val trimmed = raw.trim()
    for (delimiter in listOf("\r\n", "\n", ",", "|", ";", "\t")) {
        val parts = trimmed.split(delimiter)
        if (parts.size == 6) {
            val mealsInBatch = parts[2].trim().toIntOrNull() ?: continue
            val toteId = parts[3].trim()
            val mealsInTote = parts[4].trim().toIntOrNull() ?: continue
            if (toteId.isBlank()) continue
            return ParsedQrCode(
                companyName = parts[0].trim(),
                batchId = parts[1].trim(),
                mealsInBatch = mealsInBatch,
                toteId = toteId,
                mealsInTote = mealsInTote,
                checkedInDate = parts[5].trim()
            )
        }
    }
    return null
}

internal fun normalizeToToteId(rawValue: String): String {
    parseQrCode(rawValue)?.let { return it.toteId }
    val trimmed = rawValue.trim()
    // If QR payload is JSON, extract ContainerId for dedup so scanner avoids duplicate API calls
    if (trimmed.startsWith("{")) {
        Regex(""""ContainerId"\s*:\s*(\d+)""")
            .find(trimmed)
            ?.groupValues?.getOrNull(1)
            ?.takeIf { it.isNotBlank() }
            ?.let { return it }
    }
    val cleaned = trimmed.removePrefix("#").trim()
    val digits = cleaned.takeWhile { it.isDigit() }
    return if (digits.isNotBlank()) digits else cleaned
}
//