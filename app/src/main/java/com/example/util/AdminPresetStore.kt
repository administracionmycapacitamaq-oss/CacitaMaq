package com.example.util

import android.content.Context
import android.content.SharedPreferences
import com.example.data.model.Certificate
import com.example.data.model.CertificateDefaults
import com.example.data.model.CertificateDraft

object AdminPresetStore {
    private const val PREFS_NAME = "capacitamaq_admin_presets"
    private const val KEY_HAS_PRESET = "has_preset"
    private const val KEY_COURSE_NAME = "course_name"
    private const val KEY_DESCRIPTION = "description"
    private const val KEY_CERTIFICATE_TYPE = "certificate_type"
    private const val KEY_OPERATOR_NAME = "operator_name"
    private const val KEY_OPERATOR_ROLE = "operator_role"
    private const val KEY_OPERATOR_ID = "operator_identifier"
    private const val KEY_EQUIPMENT = "equipment"
    private const val KEY_ISSUER = "issuer"
    private const val KEY_ISSUER_URL = "issuer_url"
    private const val KEY_WHATSAPP = "whatsapp"
    private const val KEY_EMAIL = "email"
    private const val KEY_THEME_COLOR = "theme_color"
    private const val KEY_BG_COLOR = "bg_color"
    private const val KEY_CARD_COLOR = "card_color"
    private const val KEY_FONT = "font_choice"
    private const val KEY_TEMPLATE = "template"
    private const val KEY_QR_COLOR = "qr_color"
    private const val KEY_QR_PATTERN = "qr_pattern"
    private const val KEY_QR_CAPTION = "qr_caption"
    private const val KEY_LOGO_PATH = "logo_path"
    private const val KEY_COMPANY_IMAGE_PATH = "company_image_path"
    private const val KEY_OPERATOR_PHOTO_PATH = "operator_photo_path"

    private fun prefs(context: Context): SharedPreferences =
        context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun hasSavedPreset(context: Context): Boolean =
        prefs(context).getBoolean(KEY_HAS_PRESET, false)

    fun saveFromDraft(context: Context, draft: CertificateDraft) {
        prefs(context).edit()
            .putBoolean(KEY_HAS_PRESET, true)
            .putString(KEY_COURSE_NAME, draft.courseName.trim().ifBlank { CertificateDefaults.DEFAULT_COURSE_NAME })
            .putString(KEY_DESCRIPTION, draft.description.trim().ifBlank { CertificateDefaults.DEFAULT_DESCRIPTION })
            .putString(KEY_CERTIFICATE_TYPE, draft.certificateType.ifBlank { CertificateDefaults.DEFAULT_CERTIFICATE_TYPE })
            .putString(KEY_OPERATOR_NAME, draft.operatorName.trim())
            .putString(KEY_OPERATOR_ROLE, draft.operatorRole.trim())
            .putString(KEY_OPERATOR_ID, draft.operatorIdentifier.trim())
            .putString(KEY_EQUIPMENT, draft.equipment.trim())
            .putString(KEY_ISSUER, draft.issuer.trim().ifBlank { CertificateDefaults.DEFAULT_ISSUER })
            .putString(KEY_ISSUER_URL, draft.issuerUrl.trim())
            .putString(KEY_WHATSAPP, draft.whatsapp.trim())
            .putString(KEY_EMAIL, draft.email.trim())
            .putString(KEY_THEME_COLOR, draft.themeColor)
            .putString(KEY_BG_COLOR, draft.backgroundColor)
            .putString(KEY_CARD_COLOR, draft.cardColor)
            .putString(KEY_FONT, draft.fontChoice)
            .putString(KEY_TEMPLATE, draft.template)
            .putString(KEY_QR_COLOR, draft.qrColor)
            .putString(KEY_QR_PATTERN, draft.qrPattern)
            .putString(KEY_QR_CAPTION, draft.qrCaption.trim().ifBlank { CertificateDefaults.DEFAULT_QR_CAPTION })
            .putString(KEY_LOGO_PATH, draft.logoPath.trim())
            .putString(KEY_COMPANY_IMAGE_PATH, draft.companyImagePath.trim())
            .putString(KEY_OPERATOR_PHOTO_PATH, draft.operatorPhotoPath.trim())
            .apply()
    }

    fun buildNewDraft(
        context: Context,
        latestCertificateFallback: Certificate? = null
    ): CertificateDraft {
        val today = Certificate.todayIsoDate()
        val defaultExpiry = DateFormatter.addOneYearIso(today)
        val generatedCode = DateFormatter.generateCertificateCode()
        val p = prefs(context)

        if (p.getBoolean(KEY_HAS_PRESET, false)) {
            return CertificateDraft(
                courseName = p.getString(KEY_COURSE_NAME, CertificateDefaults.DEFAULT_COURSE_NAME).orEmpty()
                    .ifBlank { CertificateDefaults.DEFAULT_COURSE_NAME },
                description = p.getString(KEY_DESCRIPTION, CertificateDefaults.DEFAULT_DESCRIPTION).orEmpty()
                    .ifBlank { CertificateDefaults.DEFAULT_DESCRIPTION },
                certificateType = p.getString(KEY_CERTIFICATE_TYPE, CertificateDefaults.DEFAULT_CERTIFICATE_TYPE).orEmpty()
                    .ifBlank { CertificateDefaults.DEFAULT_CERTIFICATE_TYPE },
                operatorName = p.getString(KEY_OPERATOR_NAME, "").orEmpty(),
                operatorRole = p.getString(KEY_OPERATOR_ROLE, "").orEmpty(),
                operatorIdentifier = p.getString(KEY_OPERATOR_ID, "").orEmpty(),
                equipment = p.getString(KEY_EQUIPMENT, CertificateDefaults.DEFAULT_EQUIPMENT).orEmpty(),
                issuer = p.getString(KEY_ISSUER, CertificateDefaults.DEFAULT_ISSUER).orEmpty()
                    .ifBlank { CertificateDefaults.DEFAULT_ISSUER },
                issuedAt = today,
                expiresAt = defaultExpiry,
                certificateCode = generatedCode,
                issuerUrl = p.getString(KEY_ISSUER_URL, CertificateDefaults.DEFAULT_ISSUER_URL).orEmpty(),
                whatsapp = p.getString(KEY_WHATSAPP, CertificateDefaults.DEFAULT_WHATSAPP).orEmpty(),
                email = p.getString(KEY_EMAIL, CertificateDefaults.DEFAULT_EMAIL).orEmpty(),
                themeColor = p.getString(KEY_THEME_COLOR, CertificateDefaults.DEFAULT_THEME_COLOR).orEmpty(),
                backgroundColor = p.getString(KEY_BG_COLOR, CertificateDefaults.DEFAULT_BACKGROUND_COLOR).orEmpty(),
                cardColor = p.getString(KEY_CARD_COLOR, CertificateDefaults.DEFAULT_CARD_COLOR).orEmpty(),
                fontChoice = p.getString(KEY_FONT, CertificateDefaults.DEFAULT_FONT).orEmpty(),
                template = p.getString(KEY_TEMPLATE, CertificateDefaults.DEFAULT_TEMPLATE).orEmpty(),
                qrColor = p.getString(KEY_QR_COLOR, CertificateDefaults.DEFAULT_QR_COLOR).orEmpty(),
                qrPattern = p.getString(KEY_QR_PATTERN, CertificateDefaults.DEFAULT_QR_PATTERN).orEmpty(),
                qrCaption = p.getString(KEY_QR_CAPTION, CertificateDefaults.DEFAULT_QR_CAPTION).orEmpty(),
                status = "active",
                logoPath = p.getString(KEY_LOGO_PATH, "").orEmpty(),
                companyImagePath = p.getString(KEY_COMPANY_IMAGE_PATH, "").orEmpty(),
                operatorPhotoPath = p.getString(KEY_OPERATOR_PHOTO_PATH, "").orEmpty()
            )
        }

        if (latestCertificateFallback != null) {
            return latestCertificateFallback.toDraft().copy(
                editingId = null,
                existingCertificateCode = "",
                studentName = "",
                studentRut = "",
                studentEmail = "",
                issuedAt = today,
                expiresAt = defaultExpiry,
                certificateCode = generatedCode,
                status = "active"
            )
        }

        return CertificateDraft(
            issuedAt = today,
            expiresAt = defaultExpiry,
            certificateCode = generatedCode,
            status = "active"
        )
    }
}
