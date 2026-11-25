package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.OpenInBrowser
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Smartphone
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.data.model.AdminConfig
import com.example.data.model.Certificate
import com.example.ui.components.CredentialImage
import com.example.ui.components.QrThumbnail
import com.example.ui.components.StatusBadge
import com.example.ui.theme.BlueCorp
import com.example.ui.theme.DiplomaBorder
import com.example.ui.theme.DiplomaCream
import com.example.ui.theme.GoldMuted
import com.example.ui.theme.InvalidRedText
import com.example.ui.theme.NavyDeep
import com.example.ui.theme.ValidGreenBg
import com.example.ui.theme.ValidGreenText
import com.example.util.DateFormatter

@Composable
fun DetailScreen(
    certificate: Certificate?,
    verifierBaseUrl: String = "",
    readOnly: Boolean = false,
    onBackClick: () -> Unit,
    onOpenPublicView: (Certificate) -> Unit,
    onOpenWebPreview: (Certificate) -> Unit,
    onEditClick: (Certificate) -> Unit,
    onDuplicateForNewStudent: (Certificate) -> Unit = {},
    onDownloadQr: (Certificate) -> Unit,
    onExportPdf: (Certificate) -> Unit,
    onShareClick: (Certificate) -> Unit,
    onTogglePublish: (Certificate) -> Unit,
    onDeleteClick: (Certificate) -> Unit,
    modifier: Modifier = Modifier
) {
    if (certificate == null) {
        Box(
            modifier = modifier
                .fillMaxSize()
                .padding(24.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("Certificación no encontrada", style = MaterialTheme.typography.titleMedium)
                Spacer(modifier = Modifier.height(12.dp))
                OutlinedButton(onClick = onBackClick) {
                    Text("Volver al listado")
                }
            }
        }
        return
    }

    var showDeleteConfirm by remember { mutableStateOf(false) }

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text("Eliminar certificación") },
            text = {
                Text("¿Confirmas que deseas eliminar la certificación ${certificate.certificateCode}?")
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDeleteConfirm = false
                        onDeleteClick(certificate)
                    }
                ) {
                    Text("Eliminar", color = InvalidRedText)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) {
                    Text("Cancelar")
                }
            }
        )
    }

    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        val isWide = maxWidth >= 820.dp

        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "CREDENCIAL INDIVIDUAL",
                        style = MaterialTheme.typography.labelSmall,
                        color = BlueCorp
                    )
                    Text(
                        text = "Detalle del certificado",
                        style = MaterialTheme.typography.headlineMedium
                    )
                }
                OutlinedButton(onClick = onBackClick) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Listado")
                }
            }

            if (isWide) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(18.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    DiplomaCard(
                        certificate = certificate,
                        modifier = Modifier.weight(1.3f)
                    )
                    QrSideCard(
                        certificate = certificate,
                        verifierBaseUrl = verifierBaseUrl,
                        readOnly = readOnly,
                        onOpenPublicView = { onOpenPublicView(certificate) },
                        onOpenWebPreview = { onOpenWebPreview(certificate) },
                        onEditClick = { onEditClick(certificate) },
                        onDuplicateForNewStudent = { onDuplicateForNewStudent(certificate) },
                        onDownloadQr = { onDownloadQr(certificate) },
                        onExportPdf = { onExportPdf(certificate) },
                        onShareClick = { onShareClick(certificate) },
                        onTogglePublish = { onTogglePublish(certificate) },
                        onDeleteRequest = { showDeleteConfirm = true },
                        modifier = Modifier.weight(0.9f)
                    )
                }
            } else {
                DiplomaCard(certificate = certificate)
                QrSideCard(
                    certificate = certificate,
                    verifierBaseUrl = verifierBaseUrl,
                    readOnly = readOnly,
                    onOpenPublicView = { onOpenPublicView(certificate) },
                    onOpenWebPreview = { onOpenWebPreview(certificate) },
                    onEditClick = { onEditClick(certificate) },
                    onDuplicateForNewStudent = { onDuplicateForNewStudent(certificate) },
                    onDownloadQr = { onDownloadQr(certificate) },
                    onExportPdf = { onExportPdf(certificate) },
                    onShareClick = { onShareClick(certificate) },
                    onTogglePublish = { onTogglePublish(certificate) },
                    onDeleteRequest = { showDeleteConfirm = true }
                )
            }
        }
    }
}

@Composable
private fun DiplomaCard(
    certificate: Certificate,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .border(1.5.dp, DiplomaBorder, RoundedCornerShape(16.dp)),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = DiplomaCream),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 28.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            CredentialImage(
                customPath = certificate.logoPath,
                fallbackRes = R.drawable.img_capacitamaq_seal_1795460461997,
                contentDescription = stringResource(R.string.cd_company_seal),
                modifier = Modifier
                    .size(78.dp)
                    .clip(CircleShape)
                    .background(Color.White),
                contentScale = ContentScale.Crop
            )

            Spacer(modifier = Modifier.height(14.dp))

            Text(
                text = "CERTIFICACIÓN VERIFICABLE",
                style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 2.sp),
                color = GoldMuted
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Certificado de reconocimiento",
                style = MaterialTheme.typography.headlineMedium,
                color = NavyDeep,
                textAlign = TextAlign.Center
            )

            HorizontalDivider(
                modifier = Modifier
                    .width(100.dp)
                    .padding(vertical = 16.dp),
                color = DiplomaBorder
            )

            Text(
                text = "Se certifica que",
                style = MaterialTheme.typography.bodySmall,
                color = Color(0xFF837D70)
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = certificate.studentName.ifBlank { "Alumno" },
                style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.ExtraBold),
                color = NavyDeep,
                textAlign = TextAlign.Center
            )
            if (certificate.studentRut.isNotBlank()) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "RUT: ${certificate.studentRut}",
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                    color = BlueCorp,
                    textAlign = TextAlign.Center
                )
            }
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "ha completado satisfactoriamente",
                style = MaterialTheme.typography.bodySmall,
                color = Color(0xFF837D70)
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = certificate.courseName,
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = GoldMuted,
                textAlign = TextAlign.Center
            )

            if (certificate.description.isNotBlank()) {
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = certificate.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFF6B6558),
                    textAlign = TextAlign.Center
                )
            }

            HorizontalDivider(
                modifier = Modifier
                    .width(100.dp)
                    .padding(vertical = 16.dp),
                color = DiplomaBorder
            )

            val meta = buildString {
                append("Emitido por ${certificate.issuer} · ${DateFormatter.formatSpanish(certificate.issuedAt)}")
                if (certificate.expiresAt.isNotBlank()) {
                    append("\nVálido hasta ${DateFormatter.formatSpanish(certificate.expiresAt)}")
                }
                if (certificate.operatorName.isNotBlank()) {
                    append("\nOperador certificador: ${certificate.operatorName}")
                }
                if (certificate.equipment.isNotBlank()) {
                    append(" · ${certificate.equipment}")
                }
                append("\nAutorizado por: ${certificate.verifiedByAdminEmail.ifBlank { AdminConfig.OFFICIAL_ADMIN_EMAIL }}")
            }
            Text(
                text = meta,
                style = MaterialTheme.typography.bodySmall,
                color = Color(0xFF837D70),
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
private fun QrSideCard(
    certificate: Certificate,
    verifierBaseUrl: String,
    readOnly: Boolean,
    onOpenPublicView: () -> Unit,
    onOpenWebPreview: () -> Unit,
    onEditClick: () -> Unit,
    onDuplicateForNewStudent: () -> Unit,
    onDownloadQr: () -> Unit,
    onExportPdf: () -> Unit,
    onShareClick: () -> Unit,
    onTogglePublish: () -> Unit,
    onDeleteRequest: () -> Unit,
    modifier: Modifier = Modifier
) {
    val webUrl = certificate.webUrlOrNull(verifierBaseUrl)
    val qrContent = certificate.publicLink(verifierBaseUrl)
    val shortWebUrl = webUrl?.substringBefore("?v=").orEmpty()
    val isLivePublished = certificate.status == "active"

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = if (isLivePublished) "QR oficial publicado" else "QR de certificación",
                style = MaterialTheme.typography.titleMedium
            )
            Text(
                text = certificate.qrCaption.ifBlank {
                    "Escanea o verifica el código ${certificate.certificateCode}"
                },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(14.dp))

            QrThumbnail(
                content = qrContent,
                qrColorHex = certificate.qrColor,
                qrPattern = certificate.qrPattern,
                size = 176.dp,
                pixelSize = 512
            )

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = certificate.certificateCode,
                style = MaterialTheme.typography.titleSmall,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(6.dp))
            StatusBadge(status = certificate.computedStatus())

            if (isLivePublished) {
                Spacer(modifier = Modifier.height(8.dp))
                Surface(
                    shape = RoundedCornerShape(99.dp),
                    color = ValidGreenBg
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(5.dp)
                    ) {
                        Icon(
                            Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = ValidGreenText,
                            modifier = Modifier.size(14.dp)
                        )
                        Text(
                            text = "Publicado en Firebase · ${certificate.verifiedByAdminEmail.ifBlank { AdminConfig.OFFICIAL_ADMIN_EMAIL }}",
                            style = MaterialTheme.typography.labelSmall,
                            color = ValidGreenText,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            if (webUrl != null) {
                Text(
                    text = shortWebUrl,
                    style = MaterialTheme.typography.bodySmall,
                    color = BlueCorp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.clickable { onOpenWebPreview() }
                )
                Text(
                    text = "Enlace web HTTPS automático activo para toda la empresa",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
            } else {
                Text(
                    text = "QR activo · Código ${certificate.certificateCode} · Toca «Ver certificado en web» para abrir la credencial",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            Button(
                onClick = onOpenWebPreview,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .testTag("detail_web_verify_button"),
                colors = ButtonDefaults.buttonColors(containerColor = BlueCorp)
            ) {
                Icon(
                    if (webUrl != null) Icons.Default.OpenInBrowser else Icons.Default.Language,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("Ver certificado en web")
            }

            Spacer(modifier = Modifier.height(8.dp))

            OutlinedButton(
                onClick = onOpenPublicView,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .testTag("detail_public_preview_button")
            ) {
                Icon(Icons.Default.Smartphone, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Vista previa móvil")
            }

            if (!readOnly) {
                Spacer(modifier = Modifier.height(8.dp))

                OutlinedButton(
                    onClick = onEditClick,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                ) {
                    Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Editar certificación")
                }

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedButton(
                    onClick = onDuplicateForNewStudent,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("detail_duplicate_button")
                ) {
                    Icon(Icons.Default.PersonAdd, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Duplicar para otro alumno")
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = onDownloadQr,
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                ) {
                    Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(17.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("QR PNG")
                }
                OutlinedButton(
                    onClick = onExportPdf,
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                ) {
                    Icon(Icons.Default.PictureAsPdf, contentDescription = null, modifier = Modifier.size(17.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("PDF")
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            OutlinedButton(
                onClick = onShareClick,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
            ) {
                Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(17.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Compartir")
            }

            if (!readOnly) {
                Spacer(modifier = Modifier.height(8.dp))

                OutlinedButton(
                    onClick = onTogglePublish,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                ) {
                    Icon(Icons.Default.SwapHoriz, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        if (certificate.status == "pending") {
                            "Publicar certificación"
                        } else {
                            "Cambiar a pendiente"
                        }
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                TextButton(
                    onClick = onDeleteRequest,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(
                        Icons.Default.DeleteOutline,
                        contentDescription = null,
                        tint = InvalidRedText,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Eliminar certificación", color = InvalidRedText)
                }
            }
        }
    }
}
