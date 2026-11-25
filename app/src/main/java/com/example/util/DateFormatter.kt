package com.example.util

import java.security.SecureRandom
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

object DateFormatter {
    private val inputFormat = SimpleDateFormat("yyyy-MM-dd", Locale.US)
    private val spanishDisplayFormat = SimpleDateFormat("dd 'de' MMMM 'de' yyyy", Locale("es", "ES"))
    private val random = SecureRandom()

    fun formatSpanish(isoDate: String?): String {
        val raw = isoDate?.trim().orEmpty()
        if (raw.isEmpty()) return "Sin fecha"
        return try {
            val date = synchronized(inputFormat) { inputFormat.parse(raw.take(10)) }
            if (date != null) {
                synchronized(spanishDisplayFormat) { spanishDisplayFormat.format(date) }
            } else {
                raw
            }
        } catch (_: Exception) {
            raw
        }
    }

    fun addOneYearIso(isoDate: String): String {
        return try {
            val parsed = synchronized(inputFormat) { inputFormat.parse(isoDate.trim().take(10)) }
                ?: return ""
            val cal = Calendar.getInstance(Locale.US).apply {
                time = parsed
                add(Calendar.YEAR, 1)
            }
            synchronized(inputFormat) { inputFormat.format(cal.time) }
        } catch (_: Exception) {
            ""
        }
    }

    fun generateCertificateCode(): String {
        val bytes = ByteArray(5)
        random.nextBytes(bytes)
        val hex = bytes.joinToString("") { "%02X".format(it) }
        return "CERTIFICACION-CM-$hex"
    }

    fun sanitizeCertificateCode(raw: String): String {
        val normalized = raw.trim()
            .replace(Regex("[^a-zA-Z0-9_\\-]"), "-")
            .replace(Regex("-{2,}"), "-")
            .trim('-')
            .take(100)
        return normalized.ifBlank { generateCertificateCode() }
    }
}
