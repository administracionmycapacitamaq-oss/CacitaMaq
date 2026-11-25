package com.example.util

import android.content.ClipData
import android.content.ClipboardManager
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.RectF
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import androidx.core.content.FileProvider
import com.example.R
import com.example.data.model.AdminConfig
import com.example.data.model.Certificate
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

object ExportManager {

    private fun safeFileName(raw: String): String =
        raw.replace(Regex("[^a-zA-Z0-9_\\-]"), "_").take(48).ifBlank { "certificado" }

    fun copyToClipboard(context: Context, label: String, text: String) {
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        clipboard.setPrimaryClip(ClipData.newPlainText(label, text))
    }

    suspend fun saveQrPng(
        context: Context,
        qrBitmap: Bitmap,
        certificateCode: String
    ): Result<String> = withContext(Dispatchers.IO) {
        try {
            val fileName = "QR_${safeFileName(certificateCode)}.png"
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val values = ContentValues().apply {
                    put(MediaStore.Images.Media.DISPLAY_NAME, fileName)
                    put(MediaStore.Images.Media.MIME_TYPE, "image/png")
                    put(MediaStore.Images.Media.RELATIVE_PATH, "${Environment.DIRECTORY_PICTURES}/Capacitamaq")
                    put(MediaStore.Images.Media.IS_PENDING, 1)
                }
                val resolver = context.contentResolver
                val uri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)
                    ?: return@withContext Result.failure(IllegalStateException("No se pudo crear el archivo PNG."))
                resolver.openOutputStream(uri)?.use { out ->
                    qrBitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
                }
                values.clear()
                values.put(MediaStore.Images.Media.IS_PENDING, 0)
                resolver.update(uri, values, null, null)
                Result.success("QR guardado en Imágenes/Capacitamaq/$fileName")
            } else {
                val uri = writeQrToCacheUri(context, qrBitmap, certificateCode)
                withContext(Dispatchers.Main) {
                    shareUri(context, uri, "image/png", "Guardar o compartir QR", certificateCode)
                }
                Result.success("QR listo para guardar o compartir.")
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun createQrShareUri(
        context: Context,
        qrBitmap: Bitmap,
        certificateCode: String
    ): Result<Uri> = withContext(Dispatchers.IO) {
        try {
            Result.success(writeQrToCacheUri(context, qrBitmap, certificateCode))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun writeQrToCacheUri(context: Context, qrBitmap: Bitmap, code: String): Uri {
        val dir = File(context.cacheDir, "exports").apply { mkdirs() }
        val file = File(dir, "QR_${safeFileName(code)}.png")
        FileOutputStream(file).use { out ->
            qrBitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
        }
        return FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
    }

    suspend fun exportUniversalWebVerifierHtml(context: Context): Result<Uri> = withContext(Dispatchers.IO) {
        try {
            val html = WebVerifierHtmlBuilder.buildUniversalVerifierHtml(context)
            writeHtmlToExportsAndDownloads(context, "index.html", html)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun exportSingleCertificateHtml(
        context: Context,
        cert: Certificate
    ): Result<Uri> = withContext(Dispatchers.IO) {
        try {
            val html = WebVerifierHtmlBuilder.buildSingleCertificateHtml(context, cert)
            val fileName = "Credencial_${safeFileName(cert.certificateCode)}.html"
            writeHtmlToExportsAndDownloads(context, fileName, html)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun writeHtmlToExportsAndDownloads(
        context: Context,
        fileName: String,
        html: String
    ): Result<Uri> {
        val dir = File(context.cacheDir, "exports").apply { mkdirs() }
        val cacheFile = File(dir, fileName)
        cacheFile.writeText(html, Charsets.UTF_8)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val values = ContentValues().apply {
                put(MediaStore.Downloads.DISPLAY_NAME, fileName)
                put(MediaStore.Downloads.MIME_TYPE, "text/html")
                put(MediaStore.Downloads.RELATIVE_PATH, "${Environment.DIRECTORY_DOWNLOADS}/Capacitamaq")
                put(MediaStore.Downloads.IS_PENDING, 1)
            }
            val resolver = context.contentResolver
            resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values)?.let { mediaUri ->
                resolver.openOutputStream(mediaUri)?.use { out ->
                    out.write(html.toByteArray(Charsets.UTF_8))
                }
                values.clear()
                values.put(MediaStore.Downloads.IS_PENDING, 0)
                resolver.update(mediaUri, values, null, null)
            }
        }

        val shareUri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            cacheFile
        )
        return Result.success(shareUri)
    }

    suspend fun exportCertificatePdf(
        context: Context,
        cert: Certificate,
        qrBitmap: Bitmap,
        verifierBaseUrl: String = ""
    ): Result<Uri> = withContext(Dispatchers.IO) {
        try {
            val pdf = PdfDocument()
            val pageInfo = PdfDocument.PageInfo.Builder(595, 842, 1).create()
            val page = pdf.startPage(pageInfo)
            val canvas: Canvas = page.canvas

            drawCertificatePdfPage(context, canvas, cert, qrBitmap, verifierBaseUrl)

            pdf.finishPage(page)

            val fileName = "Certificado_${safeFileName(cert.certificateCode)}.pdf"
            val dir = File(context.cacheDir, "exports").apply { mkdirs() }
            val cacheFile = File(dir, fileName)
            FileOutputStream(cacheFile).use { out ->
                pdf.writeTo(out)
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val values = ContentValues().apply {
                    put(MediaStore.Downloads.DISPLAY_NAME, fileName)
                    put(MediaStore.Downloads.MIME_TYPE, "application/pdf")
                    put(MediaStore.Downloads.RELATIVE_PATH, "${Environment.DIRECTORY_DOWNLOADS}/Capacitamaq")
                    put(MediaStore.Downloads.IS_PENDING, 1)
                }
                val resolver = context.contentResolver
                resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values)?.let { mediaUri ->
                    resolver.openOutputStream(mediaUri)?.use { out ->
                        cacheFile.inputStream().use { input -> input.copyTo(out) }
                    }
                    values.clear()
                    values.put(MediaStore.Downloads.IS_PENDING, 0)
                    resolver.update(mediaUri, values, null, null)
                }
            }

            pdf.close()
            val shareUri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                cacheFile
            )
            Result.success(shareUri)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun drawCertificatePdfPage(
        context: Context,
        canvas: Canvas,
        cert: Certificate,
        qrBitmap: Bitmap,
        verifierBaseUrl: String
    ) {
        val pageWidth = 595f

        canvas.drawColor(Color.parseColor("#FFFCF4"))

        val navyBorder = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#102B53")
            style = Paint.Style.STROKE
            strokeWidth = 6f
        }
        canvas.drawRect(24f, 24f, pageWidth - 24f, 842f - 24f, navyBorder)

        val goldBorder = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#D9C48F")
            style = Paint.Style.STROKE
            strokeWidth = 2f
        }
        canvas.drawRect(34f, 34f, pageWidth - 34f, 842f - 34f, goldBorder)

        val headerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = try {
                Color.parseColor(cert.themeColor)
            } catch (_: Exception) {
                Color.parseColor("#F3C970")
            }
        }
        canvas.drawRect(35f, 35f, pageWidth - 35f, 118f, headerPaint)

        val sealBitmap = loadSealBitmap(context, cert.logoPath)
        if (sealBitmap != null) {
            val sealRect = RectF(pageWidth / 2f - 46f, 70f, pageWidth / 2f + 46f, 162f)
            canvas.drawBitmap(sealBitmap, null, sealRect, null)
        }

        val kickerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#A18140")
            textSize = 11f
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
            textAlign = Paint.Align.CENTER
            letterSpacing = 0.16f
        }
        canvas.drawText("CERTIFICACIÓN VERIFICABLE", pageWidth / 2f, 192f, kickerPaint)

        val headingPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#102B53")
            textSize = 25f
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
            textAlign = Paint.Align.CENTER
        }
        canvas.drawText("Certificado de reconocimiento", pageWidth / 2f, 226f, headingPaint)

        val linePaint = Paint().apply {
            color = Color.parseColor("#D9C48F")
            strokeWidth = 2f
        }
        canvas.drawLine(pageWidth / 2f - 70f, 244f, pageWidth / 2f + 70f, 244f, linePaint)

        val mutedPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#697683")
            textSize = 12f
            textAlign = Paint.Align.CENTER
        }
        canvas.drawText("Se certifica que", pageWidth / 2f, 274f, mutedPaint)

        val namePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#172B46")
            textSize = 22f
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
            textAlign = Paint.Align.CENTER
        }
        canvas.drawText(cert.studentName.ifBlank { "Alumno" }, pageWidth / 2f, 308f, namePaint)

        if (cert.studentRut.isNotBlank()) {
            val rutPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.parseColor("#1554A0")
                textSize = 13f
                typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
                textAlign = Paint.Align.CENTER
            }
            canvas.drawText("RUT: ${cert.studentRut}", pageWidth / 2f, 330f, rutPaint)
        }

        canvas.drawText("ha completado satisfactoriamente", pageWidth / 2f, 356f, mutedPaint)

        val coursePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#A27C32")
            textSize = 16f
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
            textAlign = Paint.Align.CENTER
        }
        drawCenteredWrappedText(canvas, cert.courseName, pageWidth / 2f, 386f, pageWidth - 120f, coursePaint)

        if (cert.description.isNotBlank()) {
            drawCenteredWrappedText(canvas, cert.description, pageWidth / 2f, 430f, pageWidth - 130f, mutedPaint)
        }

        val metaBoxPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            style = Paint.Style.FILL
        }
        val metaStroke = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#E4EAF2")
            style = Paint.Style.STROKE
            strokeWidth = 1.5f
        }
        val boxRect = RectF(64f, 472f, pageWidth - 64f, 616f)
        canvas.drawRoundRect(boxRect, 12f, 12f, metaBoxPaint)
        canvas.drawRoundRect(boxRect, 12f, 12f, metaStroke)

        val labelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#71829A")
            textSize = 11f
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
        }
        val valuePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#172B46")
            textSize = 11.5f
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.NORMAL)
        }

        val rows = listOf(
            "Entidad certificadora:" to cert.issuer,
            "Tipo / Maquinaria:" to "${cert.certificateType} · ${cert.equipment.ifBlank { "General" }}",
            "Operador certificador:" to cert.operatorName.ifBlank { "No asignado" },
            "Emisión / Vigencia:" to "${DateFormatter.formatSpanish(cert.issuedAt)}${if (cert.expiresAt.isNotBlank()) " · Vence: ${DateFormatter.formatSpanish(cert.expiresAt)}" else ""}",
            "Autorizado por:" to cert.verifiedByAdminEmail.ifBlank { AdminConfig.OFFICIAL_ADMIN_EMAIL }
        )
        var yPos = 498f
        rows.forEach { (k, v) ->
            canvas.drawText(k, 82f, yPos, labelPaint)
            canvas.drawText(v.take(54), 220f, yPos, valuePaint)
            yPos += 23f
        }

        val qrSize = 122f
        val qrLeft = (pageWidth - qrSize) / 2f
        val qrRect = RectF(qrLeft, 636f, qrLeft + qrSize, 636f + qrSize)
        canvas.drawBitmap(qrBitmap, null, qrRect, null)

        val codePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#102B53")
            textSize = 12f
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
            textAlign = Paint.Align.CENTER
        }
        canvas.drawText(cert.certificateCode, pageWidth / 2f, 778f, codePaint)

        val captionPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#71829A")
            textSize = 9.5f
            textAlign = Paint.Align.CENTER
        }
        val linkPreview = cert.publicLink(verifierBaseUrl).substringBefore("?v=")
        canvas.drawText(
            "${cert.qrCaption.ifBlank { "Escanea para verificar" }} · $linkPreview",
            pageWidth / 2f,
            796f,
            captionPaint
        )
    }

    private fun drawCenteredWrappedText(
        canvas: Canvas,
        text: String,
        centerX: Float,
        startY: Float,
        maxWidth: Float,
        paint: Paint
    ) {
        val words = text.split(" ")
        var line = ""
        var y = startY
        for (word in words) {
            val test = if (line.isEmpty()) word else "$line $word"
            val bounds = Rect()
            paint.getTextBounds(test, 0, test.length, bounds)
            if (bounds.width() > maxWidth && line.isNotEmpty()) {
                canvas.drawText(line, centerX, y, paint)
                line = word
                y += paint.textSize * 1.3f
            } else {
                line = test
            }
        }
        if (line.isNotEmpty()) {
            canvas.drawText(line, centerX, y, paint)
        }
    }

    private fun loadSealBitmap(context: Context, customPath: String): Bitmap? {
        if (customPath.isNotBlank()) {
            val file = File(customPath)
            if (file.exists()) {
                BitmapFactory.decodeFile(file.absolutePath)?.let { return it }
            }
        }
        return BitmapFactory.decodeResource(context.resources, R.drawable.img_capacitamaq_seal_1795460461997)
    }

    fun shareUri(
        context: Context,
        uri: Uri,
        mimeType: String,
        chooserTitle: String,
        subject: String
    ) {
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = mimeType
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_SUBJECT, subject)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(intent, chooserTitle))
    }

    fun shareLink(context: Context, title: String, url: String) {
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_SUBJECT, title)
            putExtra(Intent.EXTRA_TEXT, "$title\nVerifica esta certificación:\n$url")
        }
        context.startActivity(Intent.createChooser(intent, "Compartir certificación"))
    }

    fun openWhatsApp(context: Context, rawPhone: String) {
        val digits = rawPhone.filter { it.isDigit() }
        if (digits.isBlank()) return
        openExternalUrl(context, "https://wa.me/$digits")
    }

    fun openExternalUrl(context: Context, rawUrl: String) {
        val trimmed = rawUrl.trim()
        if (trimmed.isBlank()) return
        val normalized = if (trimmed.startsWith("http://") || trimmed.startsWith("https://")) {
            trimmed
        } else {
            "https://$trimmed"
        }
        runCatching {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(normalized)).apply {
                addCategory(Intent.CATEGORY_BROWSABLE)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        }
    }
}
