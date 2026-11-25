package com.example.data.model

import android.net.Uri
import android.util.Base64
import androidx.annotation.Keep
import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp
import org.json.JSONObject
import java.nio.charset.StandardCharsets
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object AdminConfig {
    const val OFFICIAL_ADMIN_EMAIL = "administracionmycapacitamaq@gmail.com"
    const val FIREBASE_HOSTING_DEFAULT_URL = "https://gen-lang-client-0631567384.web.app"

    fun isOfficialAdmin(email: String?): Boolean {
        return email?.trim()?.lowercase(Locale.US) == OFFICIAL_ADMIN_EMAIL
    }
}

object RutUtils {
    fun clean(raw: String): String {
        return raw.uppercase(Locale.US).replace(Regex("[^0-9K]"), "")
    }

    fun format(raw: String): String {
        val cleaned = clean(raw)
        if (cleaned.length < 2) return raw.trim()
        val body = cleaned.dropLast(1)
        val dv = cleaned.last()
        val reversed = body.reversed().chunked(3).joinToString(".").reversed()
        return "$reversed-$dv"
    }

    fun isPlausibleRut(raw: String): Boolean {
        val cleaned = clean(raw)
        if (cleaned.length !in 7..10) return false
        val body = cleaned.dropLast(1)
        return body.all { it.isDigit() }
    }
}

enum class ComputedStatus(val label: String) {
    VALID("CERTIFICADA"),
    PENDING("PENDIENTE"),
    EXPIRED("VENCIDA"),
    INVALID("NO VÁLIDA")
}

enum class StatusFilter(val key: String, val label: String) {
    ALL("all", "Todos los estados"),
    VALID("valid", "Certificado"),
    PENDING("pending", "Pendiente"),
    EXPIRED("expired", "Vencido"),
    INVALID("invalid", "No válido")
}

enum class SortOrder(val key: String, val label: String) {
    NEWEST("new", "Más recientes"),
    OLDEST("old", "Más antiguos"),
    NAME_AZ("name", "Alumno A–Z")
}

object CertificateDefaults {
    const val DEFAULT_COURSE_NAME = "CERTIFICACION DE CARGADOR FRONTAL CAT992-K"
    const val DEFAULT_DESCRIPTION = "CERTIFICACION VALIDADA POR UN AÑO CAPACITAMAQ"
    const val DEFAULT_CERTIFICATE_TYPE = "Certificación de competencia"
    const val DEFAULT_EQUIPMENT = "Cargador frontal CAT992-K"
    const val DEFAULT_ISSUER = "CAPACITAMAQ"
    const val DEFAULT_ISSUER_URL = ""
    const val DEFAULT_WHATSAPP = "+56983126221"
    const val DEFAULT_EMAIL = AdminConfig.OFFICIAL_ADMIN_EMAIL
    const val DEFAULT_THEME_COLOR = "#f3c970"
    const val DEFAULT_BACKGROUND_COLOR = "#ffffff"
    const val DEFAULT_CARD_COLOR = "#fafafa"
    const val DEFAULT_QR_COLOR = "#102b53"
    const val DEFAULT_FONT = "Manrope"
    const val DEFAULT_TEMPLATE = "classic"
    const val DEFAULT_QR_PATTERN = "square"
    const val DEFAULT_QR_CAPTION = "Escanea para verificar credencial"

    val CERTIFICATE_TYPES = listOf(
        "Certificación de competencia",
        "Constancia de formación",
        "Diploma",
        "Certificado de participación"
    )

    val FONT_CHOICES = listOf("Manrope", "DM Sans", "Arial", "Georgia")

    val TEMPLATES = listOf(
        "classic" to "Clásica · referencia móvil",
        "modern" to "Moderna",
        "minimal" to "Minimalista"
    )

    val QR_PATTERNS = listOf(
        "square" to "Cuadrado",
        "soft" to "Bordes suaves",
        "frame" to "Marco de color"
    )

    val PUBLICATION_STATUSES = listOf(
        "active" to "Publicar como certificado",
        "pending" to "Guardar como pendiente",
        "invalid" to "Marcar como no válido"
    )
}

@Keep
@IgnoreExtraProperties
data class Certificate(
    @DocumentId val id: String = "",
    val userId: String = "",
    val certificateCode: String = "",
    val courseName: String = CertificateDefaults.DEFAULT_COURSE_NAME,
    val description: String = CertificateDefaults.DEFAULT_DESCRIPTION,
    val studentName: String = "",
    val studentRut: String = "",
    val studentEmail: String = "",
    val verifiedByAdminEmail: String = AdminConfig.OFFICIAL_ADMIN_EMAIL,
    val certificateType: String = CertificateDefaults.DEFAULT_CERTIFICATE_TYPE,
    val operatorName: String = "",
    val operatorRole: String = "",
    val operatorIdentifier: String = "",
    val equipment: String = CertificateDefaults.DEFAULT_EQUIPMENT,
    val issuer: String = CertificateDefaults.DEFAULT_ISSUER,
    val issuedAt: String = "",
    val expiresAt: String = "",
    val status: String = "active",
    val issuerUrl: String = CertificateDefaults.DEFAULT_ISSUER_URL,
    val whatsapp: String = CertificateDefaults.DEFAULT_WHATSAPP,
    val email: String = CertificateDefaults.DEFAULT_EMAIL,
    val themeColor: String = CertificateDefaults.DEFAULT_THEME_COLOR,
    val backgroundColor: String = CertificateDefaults.DEFAULT_BACKGROUND_COLOR,
    val cardColor: String = CertificateDefaults.DEFAULT_CARD_COLOR,
    val fontChoice: String = CertificateDefaults.DEFAULT_FONT,
    val template: String = CertificateDefaults.DEFAULT_TEMPLATE,
    val qrColor: String = CertificateDefaults.DEFAULT_QR_COLOR,
    val qrPattern: String = CertificateDefaults.DEFAULT_QR_PATTERN,
    val qrCaption: String = CertificateDefaults.DEFAULT_QR_CAPTION,
    val logoPath: String = "",
    val companyImagePath: String = "",
    val operatorPhotoPath: String = "",
    val scanCount: Long = 0L,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
) {
    fun computedStatus(todayIso: String = todayIsoDate()): ComputedStatus {
        return when {
            status == "invalid" || status == "revoked" -> ComputedStatus.INVALID
            status == "pending" -> ComputedStatus.PENDING
            expiresAt.isNotBlank() && expiresAt < todayIso -> ComputedStatus.EXPIRED
            else -> ComputedStatus.VALID
        }
    }

    fun toVerificationPayloadBase64(): String {
        val obj = JSONObject().apply {
            put("c", certificateCode)
            put("t", courseName)
            put("d", description)
            put("s", studentName)
            put("r", studentRut)
            put("ct", certificateType)
            put("e", equipment)
            put("i", issuer)
            put("ia", issuedAt)
            put("ea", expiresAt)
            put("st", computedStatus().name)
            put("on", operatorName)
            put("or", operatorRole)
            put("w", whatsapp)
            put("em", email)
            put("vb", verifiedByAdminEmail.ifBlank { AdminConfig.OFFICIAL_ADMIN_EMAIL })
            put("tc", themeColor)
            put("bg", backgroundColor)
            put("cc", cardColor)
        }
        val bytes = obj.toString().toByteArray(StandardCharsets.UTF_8)
        return Base64.encodeToString(bytes, Base64.URL_SAFE or Base64.NO_WRAP or Base64.NO_PADDING)
    }

    fun hasWebVerifier(verifierBaseUrl: String): Boolean {
        val cleanBase = verifierBaseUrl.trim().trimEnd('/')
        return cleanBase.startsWith("https://") || cleanBase.startsWith("http://")
    }

    fun webUrlOrNull(verifierBaseUrl: String = ""): String? {
        val cleanBase = verifierBaseUrl.trim().trimEnd('/')
        if (!cleanBase.startsWith("https://") && !cleanBase.startsWith("http://")) {
            return null
        }
        val payload = toVerificationPayloadBase64()
        val encodedCode = Uri.encode(certificateCode)
        val separator = if (
            cleanBase.endsWith(".html", ignoreCase = true) ||
            cleanBase.endsWith(".htm", ignoreCase = true)
        ) {
            ""
        } else {
            "/"
        }
        return "$cleanBase$separator#verificar/$encodedCode?v=$payload"
    }

    fun publicLink(verifierBaseUrl: String = ""): String {
        return webUrlOrNull(verifierBaseUrl) ?: certificateCode
    }

    fun publicSummaryText(verifierBaseUrl: String = ""): String {
        val hosted = webUrlOrNull(verifierBaseUrl)
        if (hosted != null) return hosted
        return buildString {
            append("Certificación CapacitaMaq · ${computedStatus().label}\n")
            append("Código: $certificateCode\n")
            if (studentName.isNotBlank()) append("Alumno: $studentName\n")
            if (studentRut.isNotBlank()) append("RUT: $studentRut\n")
            append("Curso: $courseName\n")
            append("Publicado por: ${verifiedByAdminEmail.ifBlank { AdminConfig.OFFICIAL_ADMIN_EMAIL }}")
        }
    }

    fun toPublicVerificationMap(): Map<String, Any> {
        return mapOf(
            "certificateCode" to certificateCode.take(100),
            "courseName" to courseName.trim().take(120).ifBlank { CertificateDefaults.DEFAULT_COURSE_NAME },
            "description" to description.trim().take(400),
            "studentName" to studentName.trim().take(100).ifBlank { "Alumno" },
            "studentRut" to studentRut.trim().take(24),
            "certificateType" to certificateType.trim().take(80).ifBlank { CertificateDefaults.DEFAULT_CERTIFICATE_TYPE },
            "operatorName" to operatorName.trim().take(100),
            "operatorRole" to operatorRole.trim().take(100),
            "equipment" to equipment.trim().take(100),
            "issuer" to issuer.trim().take(100).ifBlank { CertificateDefaults.DEFAULT_ISSUER },
            "issuedAt" to issuedAt.trim().take(20).ifBlank { todayIsoDate() },
            "expiresAt" to expiresAt.trim().take(20),
            "status" to "active",
            "whatsapp" to whatsapp.trim().take(40),
            "email" to email.trim().take(120),
            "verifiedByAdminEmail" to AdminConfig.OFFICIAL_ADMIN_EMAIL,
            "themeColor" to safeHex(themeColor, CertificateDefaults.DEFAULT_THEME_COLOR),
            "backgroundColor" to safeHex(backgroundColor, CertificateDefaults.DEFAULT_BACKGROUND_COLOR),
            "cardColor" to safeHex(cardColor, CertificateDefaults.DEFAULT_CARD_COLOR),
            "updatedAt" to FieldValue.serverTimestamp()
        )
    }

    fun toDraft(): CertificateDraft = CertificateDraft(
        editingId = id.ifBlank { null },
        existingCertificateCode = certificateCode,
        courseName = courseName,
        description = description,
        studentName = studentName,
        studentRut = studentRut,
        studentEmail = studentEmail,
        certificateType = certificateType.ifBlank { CertificateDefaults.DEFAULT_CERTIFICATE_TYPE },
        operatorName = operatorName,
        operatorRole = operatorRole,
        operatorIdentifier = operatorIdentifier,
        equipment = equipment,
        issuer = issuer.ifBlank { CertificateDefaults.DEFAULT_ISSUER },
        issuedAt = issuedAt.ifBlank { todayIsoDate() },
        expiresAt = expiresAt,
        certificateCode = certificateCode,
        issuerUrl = issuerUrl,
        whatsapp = whatsapp,
        email = email,
        themeColor = safeHex(themeColor, CertificateDefaults.DEFAULT_THEME_COLOR),
        backgroundColor = safeHex(backgroundColor, CertificateDefaults.DEFAULT_BACKGROUND_COLOR),
        cardColor = safeHex(cardColor, CertificateDefaults.DEFAULT_CARD_COLOR),
        fontChoice = if (fontChoice in CertificateDefaults.FONT_CHOICES) fontChoice else CertificateDefaults.DEFAULT_FONT,
        template = if (template in listOf("classic", "modern", "minimal")) template else CertificateDefaults.DEFAULT_TEMPLATE,
        qrColor = safeHex(qrColor, CertificateDefaults.DEFAULT_QR_COLOR),
        qrPattern = if (qrPattern in listOf("square", "soft", "frame")) qrPattern else CertificateDefaults.DEFAULT_QR_PATTERN,
        qrCaption = qrCaption.ifBlank { CertificateDefaults.DEFAULT_QR_CAPTION },
        status = when {
            status == "pending" -> "pending"
            status == "invalid" || status == "revoked" -> "invalid"
            else -> "active"
        },
        logoPath = logoPath,
        companyImagePath = companyImagePath,
        operatorPhotoPath = operatorPhotoPath
    )

    companion object {
        fun todayIsoDate(): String = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())

        fun safeHex(value: String?, fallback: String): String {
            val trimmed = value?.trim().orEmpty()
            return if (Regex("^#[0-9a-fA-F]{6}$").matches(trimmed)) trimmed.lowercase(Locale.US) else fallback
        }
    }
}

data class CertificateDraft(
    val editingId: String? = null,
    val existingCertificateCode: String = "",
    val courseName: String = CertificateDefaults.DEFAULT_COURSE_NAME,
    val description: String = CertificateDefaults.DEFAULT_DESCRIPTION,
    val studentName: String = "",
    val studentRut: String = "",
    val studentEmail: String = "",
    val certificateType: String = CertificateDefaults.DEFAULT_CERTIFICATE_TYPE,
    val operatorName: String = "",
    val operatorRole: String = "",
    val operatorIdentifier: String = "",
    val equipment: String = CertificateDefaults.DEFAULT_EQUIPMENT,
    val issuer: String = CertificateDefaults.DEFAULT_ISSUER,
    val issuedAt: String = Certificate.todayIsoDate(),
    val expiresAt: String = "",
    val certificateCode: String = "",
    val issuerUrl: String = CertificateDefaults.DEFAULT_ISSUER_URL,
    val whatsapp: String = CertificateDefaults.DEFAULT_WHATSAPP,
    val email: String = CertificateDefaults.DEFAULT_EMAIL,
    val themeColor: String = CertificateDefaults.DEFAULT_THEME_COLOR,
    val backgroundColor: String = CertificateDefaults.DEFAULT_BACKGROUND_COLOR,
    val cardColor: String = CertificateDefaults.DEFAULT_CARD_COLOR,
    val fontChoice: String = CertificateDefaults.DEFAULT_FONT,
    val template: String = CertificateDefaults.DEFAULT_TEMPLATE,
    val qrColor: String = CertificateDefaults.DEFAULT_QR_COLOR,
    val qrPattern: String = CertificateDefaults.DEFAULT_QR_PATTERN,
    val qrCaption: String = CertificateDefaults.DEFAULT_QR_CAPTION,
    val status: String = "active",
    val logoPath: String = "",
    val companyImagePath: String = "",
    val operatorPhotoPath: String = ""
) {
    fun toPreviewCertificate(userId: String = "preview"): Certificate {
        val code = certificateCode.trim().ifBlank { "CERTIFICACION-CM-202611-CAT992K" }
        return Certificate(
            id = editingId ?: "",
            userId = userId,
            certificateCode = code,
            courseName = courseName.trim().ifBlank { CertificateDefaults.DEFAULT_COURSE_NAME },
            description = description.trim().ifBlank { CertificateDefaults.DEFAULT_DESCRIPTION },
            studentName = studentName.trim(),
            studentRut = if (studentRut.isNotBlank()) RutUtils.format(studentRut) else "",
            studentEmail = studentEmail.trim().lowercase(Locale.US),
            verifiedByAdminEmail = AdminConfig.OFFICIAL_ADMIN_EMAIL,
            certificateType = certificateType.trim().ifBlank { CertificateDefaults.DEFAULT_CERTIFICATE_TYPE },
            operatorName = operatorName.trim(),
            operatorRole = operatorRole.trim(),
            operatorIdentifier = operatorIdentifier.trim(),
            equipment = equipment.trim(),
            issuer = issuer.trim().ifBlank { CertificateDefaults.DEFAULT_ISSUER },
            issuedAt = issuedAt.trim().ifBlank { Certificate.todayIsoDate() },
            expiresAt = expiresAt.trim(),
            status = if (status in listOf("active", "pending", "invalid")) status else "active",
            issuerUrl = issuerUrl.trim(),
            whatsapp = whatsapp.trim(),
            email = email.trim(),
            themeColor = Certificate.safeHex(themeColor, CertificateDefaults.DEFAULT_THEME_COLOR),
            backgroundColor = Certificate.safeHex(backgroundColor, CertificateDefaults.DEFAULT_BACKGROUND_COLOR),
            cardColor = Certificate.safeHex(cardColor, CertificateDefaults.DEFAULT_CARD_COLOR),
            fontChoice = if (fontChoice in CertificateDefaults.FONT_CHOICES) fontChoice else CertificateDefaults.DEFAULT_FONT,
            template = if (template in listOf("classic", "modern", "minimal")) template else CertificateDefaults.DEFAULT_TEMPLATE,
            qrColor = Certificate.safeHex(qrColor, CertificateDefaults.DEFAULT_QR_COLOR),
            qrPattern = if (qrPattern in listOf("square", "soft", "frame")) qrPattern else CertificateDefaults.DEFAULT_QR_PATTERN,
            qrCaption = qrCaption.trim().take(50).ifBlank { CertificateDefaults.DEFAULT_QR_CAPTION },
            logoPath = logoPath.trim(),
            companyImagePath = companyImagePath.trim(),
            operatorPhotoPath = operatorPhotoPath.trim()
        )
    }

    fun toCreateMap(userId: String, resolvedCode: String): Map<String, Any> {
        val clean = buildSharedFieldMap(resolvedCode)
        return (clean + mapOf(
            "userId" to userId,
            "scanCount" to 0L,
            "createdAt" to FieldValue.serverTimestamp(),
            "updatedAt" to FieldValue.serverTimestamp()
        ))
    }

    fun toUpdateMap(resolvedCode: String): Map<String, Any> {
        val clean = buildSharedFieldMap(resolvedCode)
        return clean + mapOf(
            "updatedAt" to FieldValue.serverTimestamp()
        )
    }

    fun toPublicVerificationMap(resolvedCode: String): Map<String, Any> {
        val formattedRut = if (studentRut.isNotBlank()) RutUtils.format(studentRut) else ""
        return mapOf(
            "certificateCode" to resolvedCode.take(100),
            "courseName" to courseName.trim().take(120),
            "description" to description.trim().take(400),
            "studentName" to studentName.trim().take(100),
            "studentRut" to formattedRut.take(24),
            "certificateType" to certificateType.trim().take(80),
            "operatorName" to operatorName.trim().take(100),
            "operatorRole" to operatorRole.trim().take(100),
            "equipment" to equipment.trim().take(100),
            "issuer" to issuer.trim().take(100),
            "issuedAt" to issuedAt.trim().take(20),
            "expiresAt" to expiresAt.trim().take(20),
            "status" to "active",
            "whatsapp" to whatsapp.trim().take(40),
            "email" to email.trim().take(120),
            "verifiedByAdminEmail" to AdminConfig.OFFICIAL_ADMIN_EMAIL,
            "themeColor" to Certificate.safeHex(themeColor, CertificateDefaults.DEFAULT_THEME_COLOR),
            "backgroundColor" to Certificate.safeHex(backgroundColor, CertificateDefaults.DEFAULT_BACKGROUND_COLOR),
            "cardColor" to Certificate.safeHex(cardColor, CertificateDefaults.DEFAULT_CARD_COLOR),
            "updatedAt" to FieldValue.serverTimestamp()
        )
    }

    private fun buildSharedFieldMap(resolvedCode: String): Map<String, Any> {
        val formattedRut = if (studentRut.isNotBlank()) RutUtils.format(studentRut) else ""
        val map = mutableMapOf<String, Any?>(
            "certificateCode" to resolvedCode.take(100),
            "courseName" to courseName.trim().take(120),
            "certificateType" to certificateType.trim().take(80),
            "studentName" to studentName.trim().take(100),
            "studentRut" to formattedRut.take(24),
            "studentEmail" to studentEmail.trim().lowercase(Locale.US).take(120),
            "verifiedByAdminEmail" to AdminConfig.OFFICIAL_ADMIN_EMAIL,
            "issuer" to issuer.trim().take(100),
            "issuedAt" to issuedAt.trim().take(20),
            "status" to (if (status in listOf("active", "pending", "invalid")) status else "active"),
            "description" to description.trim().take(400),
            "operatorName" to operatorName.trim().take(100),
            "operatorRole" to operatorRole.trim().take(100),
            "operatorIdentifier" to operatorIdentifier.trim().take(80),
            "equipment" to equipment.trim().take(100),
            "expiresAt" to expiresAt.trim().take(20),
            "issuerUrl" to issuerUrl.trim().take(300),
            "whatsapp" to whatsapp.trim().take(40),
            "email" to email.trim().take(120),
            "themeColor" to Certificate.safeHex(themeColor, CertificateDefaults.DEFAULT_THEME_COLOR),
            "backgroundColor" to Certificate.safeHex(backgroundColor, CertificateDefaults.DEFAULT_BACKGROUND_COLOR),
            "cardColor" to Certificate.safeHex(cardColor, CertificateDefaults.DEFAULT_CARD_COLOR),
            "fontChoice" to (if (fontChoice in CertificateDefaults.FONT_CHOICES) fontChoice else CertificateDefaults.DEFAULT_FONT),
            "template" to (if (template in listOf("classic", "modern", "minimal")) template else CertificateDefaults.DEFAULT_TEMPLATE),
            "qrColor" to Certificate.safeHex(qrColor, CertificateDefaults.DEFAULT_QR_COLOR),
            "qrPattern" to (if (qrPattern in listOf("square", "soft", "frame")) qrPattern else CertificateDefaults.DEFAULT_QR_PATTERN),
            "qrCaption" to qrCaption.trim().take(50),
            "logoPath" to logoPath.trim().take(300),
            "companyImagePath" to companyImagePath.trim().take(300),
            "operatorPhotoPath" to operatorPhotoPath.trim().take(300)
        )
        @Suppress("UNCHECKED_CAST")
        return map.filterValues { it != null } as Map<String, Any>
    }
}

@Keep
@IgnoreExtraProperties
data class StudentProfile(
    @DocumentId val id: String = "",
    val uid: String = "",
    val email: String = "",
    val fullName: String = "",
    val rut: String = "",
    val verificationStatus: String = "pending",
    val approvedByEmail: String = "",
    val adminNote: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class VerifierConfig(
    @DocumentId val id: String = "",
    val verifierBaseUrl: String = "",
    val updatedByEmail: String = "",
    @ServerTimestamp val updatedAt: Timestamp? = null
)
