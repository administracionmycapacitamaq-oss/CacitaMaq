package com.example.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Badge
import androidx.compose.material.icons.filled.ChatBubbleOutline
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.R
import com.example.data.model.AdminConfig
import com.example.data.model.Certificate
import com.example.data.model.CertificateDefaults
import com.example.ui.theme.BlueCorp
import com.example.ui.theme.DmSansFontFamily
import com.example.ui.theme.ManropeFontFamily
import com.example.ui.theme.WhatsAppGreen
import com.example.util.DateFormatter
import java.io.File

@Composable
fun MobilePublicCard(
    certificate: Certificate,
    verifierBaseUrl: String = "",
    isPreview: Boolean = false,
    onOpenUrl: (String) -> Unit = {},
    onOpenWhatsApp: (String) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val bannerColor = parseHexOrDefault(certificate.themeColor, Color(0xFFF3C970))
    val pageBgColor = parseHexOrDefault(certificate.backgroundColor, Color.White)
    val cardBgColor = parseHexOrDefault(certificate.cardColor, Color(0xFFFAFAFA))

    val chosenFontFamily = when (certificate.fontChoice) {
        "DM Sans" -> DmSansFontFamily
        "Arial" -> FontFamily.SansSerif
        "Georgia" -> FontFamily.Serif
        else -> ManropeFontFamily
    }

    val isModern = certificate.template == "modern"
    val isMinimal = certificate.template == "minimal"
    val bannerHeight = if (isMinimal) 52.dp else if (isModern) 100.dp else 82.dp

    val webUrl = certificate.webUrlOrNull(verifierBaseUrl)
    val displayVerifyUrl = when {
        isPreview -> "Enlace público automático · ${certificate.certificateCode}"
        webUrl != null -> webUrl.substringBefore("?v=")
        else -> "Publicado en vivo · ${certificate.certificateCode}"
    }
    val operatorTitle = certificate.operatorName.ifBlank { "Nombre del operador · editable" }
    val operatorSubtitle = buildString {
        append(certificate.operatorRole.ifBlank { "Cargo editable" })
        if (certificate.operatorIdentifier.isNotBlank()) {
            append(" · ${certificate.operatorIdentifier}")
        }
    }

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = pageBgColor),
        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(bannerHeight)
                    .background(
                        if (isModern) {
                            Brush.horizontalGradient(listOf(bannerColor, bannerColor.copy(alpha = 0.72f)))
                        } else {
                            Brush.verticalGradient(listOf(bannerColor, bannerColor))
                        }
                    )
            )

            Surface(
                modifier = Modifier
                    .offset(y = (-46).dp)
                    .size(96.dp)
                    .border(4.dp, Color.White, CircleShape),
                shape = CircleShape,
                color = Color.White,
                shadowElevation = 5.dp
            ) {
                CredentialImage(
                    customPath = certificate.logoPath,
                    fallbackRes = R.drawable.img_capacitamaq_seal_1795460461997,
                    contentDescription = stringResource(R.string.cd_company_seal),
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(4.dp)
                        .clip(CircleShape),
                    contentScale = ContentScale.Crop
                )
            }

            Column(
                modifier = Modifier
                    .offset(y = (-34).dp)
                    .fillMaxWidth()
                    .padding(horizontal = 18.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = certificate.courseName.ifBlank { CertificateDefaults.DEFAULT_COURSE_NAME }.uppercase(),
                    fontFamily = chosenFontFamily,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 18.sp,
                    lineHeight = 24.sp,
                    textAlign = TextAlign.Center,
                    color = Color(0xFF101E32)
                )

                if (certificate.studentName.isNotBlank()) {
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = certificate.studentName.uppercase(),
                        fontFamily = chosenFontFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        textAlign = TextAlign.Center,
                        color = Color(0xFF0E2C57)
                    )
                }

                if (certificate.studentRut.isNotBlank()) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = Color(0xFFEEF4FC)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Badge,
                                contentDescription = null,
                                tint = BlueCorp,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(5.dp))
                            Text(
                                text = "RUT: ${certificate.studentRut}",
                                fontFamily = chosenFontFamily,
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp,
                                color = Color(0xFF0E2C57)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    text = certificate.description.ifBlank { CertificateDefaults.DEFAULT_DESCRIPTION }.uppercase(),
                    fontFamily = chosenFontFamily,
                    fontSize = 12.sp,
                    lineHeight = 17.sp,
                    textAlign = TextAlign.Center,
                    color = Color(0xFF697683)
                )

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = certificate.certificateCode,
                    fontFamily = chosenFontFamily,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    textAlign = TextAlign.Center,
                    color = Color(0xFF697683)
                )

                Spacer(modifier = Modifier.height(10.dp))

                StatusBadge(status = certificate.computedStatus())

                Spacer(modifier = Modifier.height(10.dp))

                // Comprobación oficial del administrador
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp),
                    color = Color(0xFFEFF6FF)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.VerifiedUser,
                            contentDescription = null,
                            tint = BlueCorp,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Comprobado por: ${certificate.verifiedByAdminEmail.ifBlank { AdminConfig.OFFICIAL_ADMIN_EMAIL }}",
                            fontFamily = chosenFontFamily,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color(0xFF0E2C57),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))
                HorizontalDivider(color = Color(0xFFEDF0F2))
                Spacer(modifier = Modifier.height(12.dp))

                // Operator row
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    color = cardBgColor
                ) {
                    Row(
                        modifier = Modifier.padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        CredentialImage(
                            customPath = certificate.operatorPhotoPath,
                            fallbackRes = R.drawable.img_capacitamaq_seal_1795460461997,
                            contentDescription = stringResource(R.string.cd_operator_photo),
                            modifier = Modifier
                                .size(width = 44.dp, height = 50.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color.White),
                            contentScale = ContentScale.Crop
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = operatorTitle,
                                fontFamily = chosenFontFamily,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 13.sp,
                                color = Color(0xFF0E1E32)
                            )
                            Text(
                                text = operatorSubtitle,
                                fontFamily = chosenFontFamily,
                                fontSize = 11.sp,
                                color = Color(0xFF78828C)
                            )
                        }
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                            contentDescription = null,
                            tint = Color(0xFF475569),
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Verification Web URL row (no longer redirects to Canva)
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .clickable(enabled = !isPreview && webUrl != null) {
                            webUrl?.let(onOpenUrl)
                        },
                    shape = RoundedCornerShape(14.dp),
                    color = cardBgColor
                ) {
                    Row(
                        modifier = Modifier.padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        CredentialImage(
                            customPath = certificate.companyImagePath,
                            fallbackRes = R.drawable.img_capacitamaq_banner_1795460462074,
                            contentDescription = stringResource(R.string.cd_company_banner),
                            modifier = Modifier
                                .size(48.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color.White),
                            contentScale = if (certificate.companyImagePath.isBlank()) ContentScale.Fit else ContentScale.Crop
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = displayVerifyUrl,
                            fontFamily = chosenFontFamily,
                            fontSize = 12.sp,
                            color = Color(0xFF0E1E32),
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f)
                        )
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                            contentDescription = null,
                            tint = Color(0xFF475569),
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "Encuéntreme en",
                    fontFamily = chosenFontFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    color = Color(0xFF0E1E32),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(8.dp))

                val hasWhatsApp = certificate.whatsapp.any { it.isDigit() }
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .clickable(enabled = hasWhatsApp && !isPreview) {
                            onOpenWhatsApp(certificate.whatsapp)
                        },
                    shape = RoundedCornerShape(14.dp),
                    color = Color.White,
                    tonalElevation = 1.dp
                ) {
                    Row(
                        modifier = Modifier.padding(vertical = 8.dp, horizontal = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(42.dp)
                                .clip(CircleShape)
                                .background(WhatsAppGreen),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.ChatBubbleOutline,
                                contentDescription = "WhatsApp",
                                tint = Color.White,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "WhatsApp",
                                fontFamily = chosenFontFamily,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 13.sp,
                                color = Color(0xFF0E1E32)
                            )
                            Text(
                                text = if (hasWhatsApp) {
                                    certificate.issuer.ifBlank { CertificateDefaults.DEFAULT_ISSUER }
                                } else {
                                    "Agrega un número de contacto"
                                },
                                fontFamily = chosenFontFamily,
                                fontSize = 11.sp,
                                color = Color(0xFF78828C)
                            )
                        }
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                            contentDescription = null,
                            tint = Color(0xFF475569),
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                val footerSummary = buildString {
                    if (certificate.studentName.isNotBlank()) {
                        append("Alumno: ${certificate.studentName}")
                        if (certificate.studentRut.isNotBlank()) append(" (${certificate.studentRut})")
                        append(" · ")
                    }
                    if (certificate.equipment.isNotBlank()) {
                        append("${certificate.equipment} · ")
                    }
                    append("Emitida: ${DateFormatter.formatSpanish(certificate.issuedAt)}")
                    if (certificate.expiresAt.isNotBlank()) {
                        append(" · Vence: ${DateFormatter.formatSpanish(certificate.expiresAt)}")
                    }
                }
                Text(
                    text = footerSummary,
                    fontFamily = chosenFontFamily,
                    fontSize = 11.sp,
                    lineHeight = 16.sp,
                    color = Color(0xFF71829A),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

@Composable
fun CredentialImage(
    customPath: String,
    fallbackRes: Int,
    contentDescription: String,
    modifier: Modifier = Modifier,
    contentScale: ContentScale = ContentScale.Crop
) {
    val file = customPath.takeIf { it.isNotBlank() }?.let { File(it) }?.takeIf { it.exists() }
    if (file != null) {
        AsyncImage(
            model = file,
            contentDescription = contentDescription,
            modifier = modifier,
            contentScale = contentScale
        )
    } else {
        Image(
            painter = painterResource(id = fallbackRes),
            contentDescription = contentDescription,
            modifier = modifier,
            contentScale = contentScale
        )
    }
}

fun parseHexOrDefault(hex: String, default: Color): Color {
    return try {
        Color(android.graphics.Color.parseColor(hex))
    } catch (_: Exception) {
        default
    }
}
