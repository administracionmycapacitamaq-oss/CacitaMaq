package com.example.util

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.net.Uri
import android.util.Base64
import com.example.data.model.AdminConfig
import com.example.data.model.Certificate
import com.google.zxing.BarcodeFormat
import com.google.zxing.BinaryBitmap
import com.google.zxing.DecodeHintType
import com.google.zxing.EncodeHintType
import com.google.zxing.PlanarYUVLuminanceSource
import com.google.zxing.RGBLuminanceSource
import com.google.zxing.common.GlobalHistogramBinarizer
import com.google.zxing.common.HybridBinarizer
import com.google.zxing.qrcode.QRCodeReader
import com.google.zxing.qrcode.QRCodeWriter
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel
import org.json.JSONObject
import java.nio.charset.StandardCharsets

object QrCodecService {

    fun generateQrBitmap(
        content: String,
        colorHex: String = "#102b53",
        sizePx: Int = 512,
        pattern: String = "square"
    ): Bitmap {
        val safeText = content.ifBlank { "CERTIFICACION-CM" }
        val darkColor = try {
            Color.parseColor(colorHex)
        } catch (_: Exception) {
            Color.parseColor("#102b53")
        }

        val hints = mapOf(
            EncodeHintType.ERROR_CORRECTION to ErrorCorrectionLevel.M,
            EncodeHintType.MARGIN to 2,
            EncodeHintType.CHARACTER_SET to "UTF-8"
        )

        val matrix = QRCodeWriter().encode(safeText, BarcodeFormat.QR_CODE, 0, 0, hints)
        val matrixSize = matrix.width.coerceAtLeast(1)

        val bitmap = Bitmap.createBitmap(sizePx, sizePx, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        canvas.drawColor(Color.WHITE)

        val padding = if (pattern == "frame") sizePx * 0.08f else sizePx * 0.04f
        val drawArea = sizePx - padding * 2f
        val cellSize = drawArea / matrixSize.toFloat()

        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = darkColor
            style = Paint.Style.FILL
        }

        for (y in 0 until matrixSize) {
            for (x in 0 until matrixSize) {
                if (matrix.get(x, y)) {
                    val left = padding + x * cellSize
                    val top = padding + y * cellSize
                    val right = left + cellSize
                    val bottom = top + cellSize
                    if (pattern == "soft") {
                        val radius = cellSize * 0.34f
                        canvas.drawRoundRect(
                            RectF(left + 0.3f, top + 0.3f, right - 0.3f, bottom - 0.3f),
                            radius,
                            radius,
                            paint
                        )
                    } else {
                        canvas.drawRect(left, top, right + 0.4f, bottom + 0.4f, paint)
                    }
                }
            }
        }

        if (pattern == "frame") {
            val strokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = darkColor
                style = Paint.Style.STROKE
                strokeWidth = (sizePx * 0.025f).coerceAtLeast(4f)
            }
            val inset = strokePaint.strokeWidth / 2f
            canvas.drawRoundRect(
                RectF(inset, inset, sizePx - inset, sizePx - inset),
                sizePx * 0.06f,
                sizePx * 0.06f,
                strokePaint
            )
        }

        return bitmap
    }

    fun decodeQrFromBitmap(source: Bitmap): String? {
        val maxDim = 1600
        val bitmap = if (source.width > maxDim || source.height > maxDim) {
            val scale = maxDim.toFloat() / maxOf(source.width, source.height).toFloat()
            Bitmap.createScaledBitmap(
                source,
                (source.width * scale).toInt().coerceAtLeast(1),
                (source.height * scale).toInt().coerceAtLeast(1),
                true
            )
        } else {
            source
        }

        val width = bitmap.width
        val height = bitmap.height
        val pixels = IntArray(width * height)
        bitmap.getPixels(pixels, 0, width, 0, 0, width, height)

        val rgbSource = RGBLuminanceSource(width, height, pixels)
        val hints = mapOf(
            DecodeHintType.TRY_HARDER to true,
            DecodeHintType.CHARACTER_SET to "UTF-8"
        )
        val reader = QRCodeReader()

        return try {
            reader.decode(BinaryBitmap(HybridBinarizer(rgbSource)), hints).text
        } catch (_: Exception) {
            try {
                reader.decode(BinaryBitmap(GlobalHistogramBinarizer(rgbSource)), hints).text
            } catch (_: Exception) {
                null
            }
        }
    }

    fun decodeQrFromYuv(yuvData: ByteArray, width: Int, height: Int): String? {
        return try {
            val source = PlanarYUVLuminanceSource(
                yuvData,
                width,
                height,
                0,
                0,
                width,
                height,
                false
            )
            val hints = mapOf(DecodeHintType.TRY_HARDER to true)
            QRCodeReader().decode(BinaryBitmap(HybridBinarizer(source)), hints).text
        } catch (_: Exception) {
            null
        }
    }

    fun extractCertificateCode(rawInput: String): String {
        val trimmed = rawInput.trim()
        return when {
            trimmed.contains("capacitamaq://verificar/") -> {
                val after = trimmed.substringAfter("capacitamaq://verificar/")
                Uri.decode(after.substringBefore("?").substringBefore("&").substringBefore("#")).trim()
            }
            trimmed.contains("#verificar/") -> {
                val after = trimmed.substringAfter("#verificar/")
                Uri.decode(after.substringBefore("?").substringBefore("&")).trim()
            }
            trimmed.contains("/verificar/") -> {
                val after = trimmed.substringAfter("/verificar/")
                Uri.decode(after.substringBefore("?").substringBefore("&").substringBefore("#")).trim()
            }
            trimmed.contains("?c=") -> {
                val after = trimmed.substringAfter("?c=")
                Uri.decode(after.substringBefore("&").substringBefore("#")).trim()
            }
            trimmed.contains("&c=") -> {
                val after = trimmed.substringAfter("&c=")
                Uri.decode(after.substringBefore("&").substringBefore("#")).trim()
            }
            else -> trimmed
        }
    }

    fun decodeVerificationPayload(rawInput: String): Certificate? {
        val marker = "?v="
        val hashMarker = "#v="
        val encoded = when {
            rawInput.contains(marker) -> rawInput.substringAfter(marker).substringBefore("&").substringBefore("#")
            rawInput.contains(hashMarker) -> rawInput.substringAfter(hashMarker).substringBefore("&")
            else -> return null
        }.trim()

        if (encoded.isBlank()) return null

        return try {
            val decodedBytes = Base64.decode(encoded, Base64.URL_SAFE or Base64.NO_WRAP or Base64.NO_PADDING)
            val json = JSONObject(String(decodedBytes, StandardCharsets.UTF_8))
            val statusStr = when (json.optString("st")) {
                "PENDING" -> "pending"
                "INVALID" -> "invalid"
                else -> "active"
            }
            Certificate(
                certificateCode = json.optString("c"),
                courseName = json.optString("t"),
                description = json.optString("d"),
                studentName = json.optString("s"),
                studentRut = json.optString("r"),
                certificateType = json.optString("ct").ifBlank { "Certificación de competencia" },
                equipment = json.optString("e"),
                issuer = json.optString("i").ifBlank { "CAPACITAMAQ" },
                issuedAt = json.optString("ia"),
                expiresAt = json.optString("ea"),
                status = statusStr,
                operatorName = json.optString("on"),
                operatorRole = json.optString("or"),
                whatsapp = json.optString("w"),
                email = json.optString("em"),
                verifiedByAdminEmail = json.optString("vb").ifBlank { AdminConfig.OFFICIAL_ADMIN_EMAIL },
                themeColor = Certificate.safeHex(json.optString("tc"), "#f3c970"),
                backgroundColor = Certificate.safeHex(json.optString("bg"), "#ffffff"),
                cardColor = Certificate.safeHex(json.optString("cc"), "#fafafa")
            )
        } catch (_: Exception) {
            null
        }
    }
}
