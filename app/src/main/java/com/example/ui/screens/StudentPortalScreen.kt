package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Badge
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.OpenInBrowser
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.model.AdminConfig
import com.example.data.model.Certificate
import com.example.data.model.StudentProfile
import com.example.ui.components.QrThumbnail
import com.example.ui.components.StatusBadge
import com.example.ui.state.UiState
import com.example.ui.theme.BlueCorp
import com.example.ui.theme.GoldMuted
import com.example.ui.theme.GoldPrimary
import com.example.ui.theme.InvalidRedBg
import com.example.ui.theme.InvalidRedText
import com.example.ui.theme.NavyDeep
import com.example.ui.theme.PendingAmberBg
import com.example.ui.theme.PendingAmberText
import com.example.ui.theme.ValidGreenBg
import com.example.ui.theme.ValidGreenText
import com.example.util.DateFormatter

@Composable
fun StudentPortalScreen(
    studentEmail: String,
    initialDisplayName: String,
    profileState: UiState<StudentProfile?>,
    certificatesState: UiState<List<Certificate>>,
    verifierBaseUrl: String,
    isSubmitting: Boolean,
    onSubmitVerification: (fullName: String, rut: String) -> Unit,
    onOpenDetail: (Certificate) -> Unit,
    onOpenWebView: (Certificate) -> Unit,
    onDownloadQr: (Certificate) -> Unit,
    onExportPdf: (Certificate) -> Unit,
    modifier: Modifier = Modifier
) {
    when (profileState) {
        is UiState.Loading -> {
            Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = BlueCorp)
            }
        }

        is UiState.Error -> {
            Box(
                modifier = modifier
                    .fillMaxSize()
                    .padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                Surface(shape = RoundedCornerShape(14.dp), color = InvalidRedBg) {
                    Text(
                        text = profileState.message,
                        color = InvalidRedText,
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(16.dp)
                    )
                }
            }
        }

        is UiState.Success -> {
            val profile = profileState.data
            if (profile == null || profile.verificationStatus != "approved") {
                StudentVerificationGate(
                    studentEmail = studentEmail,
                    initialDisplayName = initialDisplayName,
                    existingProfile = profile,
                    isSubmitting = isSubmitting,
                    onSubmit = onSubmitVerification,
                    modifier = modifier
                )
            } else {
                ApprovedStudentCertificatesView(
                    profile = profile,
                    certificatesState = certificatesState,
                    verifierBaseUrl = verifierBaseUrl,
                    onOpenDetail = onOpenDetail,
                    onOpenWebView = onOpenWebView,
                    onDownloadQr = onDownloadQr,
                    onExportPdf = onExportPdf,
                    modifier = modifier
                )
            }
        }
    }
}

@Composable
private fun StudentVerificationGate(
    studentEmail: String,
    initialDisplayName: String,
    existingProfile: StudentProfile?,
    isSubmitting: Boolean,
    onSubmit: (String, String) -> Unit,
    modifier: Modifier = Modifier
) {
    var fullName by remember { mutableStateOf(existingProfile?.fullName ?: initialDisplayName) }
    var rut by remember { mutableStateOf(existingProfile?.rut ?: "") }

    LaunchedEffect(existingProfile) {
        if (existingProfile != null) {
            fullName = existingProfile.fullName
            rut = existingProfile.rut
        }
    }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Column {
                Text(
                    text = "PORTAL DEL ALUMNO",
                    style = MaterialTheme.typography.labelSmall,
                    color = BlueCorp
                )
                Text(
                    text = "Comprobación de identidad",
                    style = MaterialTheme.typography.headlineMedium
                )
                Text(
                    text = "Para proteger tus certificaciones, solo puedes consultar tus registros después de ser comprobado por el administrador oficial.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        if (existingProfile?.verificationStatus == "pending") {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = PendingAmberBg),
                    border = BorderStroke(1.dp, GoldPrimary)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(Icons.Default.Schedule, contentDescription = null, tint = PendingAmberText)
                            Text(
                                text = "En espera de comprobación del administrador",
                                style = MaterialTheme.typography.titleMedium,
                                color = PendingAmberText
                            )
                        }
                        Text(
                            text = "Nombre completo: ${existingProfile.fullName}\nRUT: ${existingProfile.rut}\nCorreo Google: ${existingProfile.email}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = NavyDeep
                        )
                        Text(
                            text = "El administrador oficial (${AdminConfig.OFFICIAL_ADMIN_EMAIL}) comprobará tu Nombre Completo y RUT para habilitar tus certificaciones.",
                            style = MaterialTheme.typography.bodySmall,
                            color = PendingAmberText
                        )
                    }
                }
            }
        }

        if (existingProfile?.verificationStatus == "rejected") {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = InvalidRedBg)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(Icons.Default.WarningAmber, contentDescription = null, tint = InvalidRedText)
                            Text(
                                text = "Solicitud rechazada por administración",
                                style = MaterialTheme.typography.titleMedium,
                                color = InvalidRedText
                            )
                        }
                        if (existingProfile.adminNote.isNotBlank()) {
                            Text(
                                text = existingProfile.adminNote,
                                style = MaterialTheme.typography.bodySmall,
                                color = InvalidRedText
                            )
                        }
                        Text(
                            text = "Corrige tu Nombre Completo o RUT abajo y vuelve a enviar tu solicitud.",
                            style = MaterialTheme.typography.bodySmall,
                            color = InvalidRedText
                        )
                    }
                }
            }
        }

        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(
                    modifier = Modifier.padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = if (existingProfile == null) "Registrar mis datos de alumno" else "Actualizar datos enviados",
                        style = MaterialTheme.typography.titleMedium
                    )
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text("Cuenta Google verificada", style = MaterialTheme.typography.labelSmall, color = BlueCorp)
                            Text(studentEmail, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                        }
                    }

                    OutlinedTextField(
                        value = fullName,
                        onValueChange = { fullName = it.take(100) },
                        label = { Text("Nombre completo *") },
                        placeholder = { Text("Nombres y dos apellidos") },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("student_input_full_name")
                    )

                    OutlinedTextField(
                        value = rut,
                        onValueChange = { rut = it.take(24) },
                        label = { Text("RUT *") },
                        placeholder = { Text("12.345.678-9") },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("student_input_rut")
                    )

                    Button(
                        onClick = { onSubmit(fullName, rut) },
                        enabled = !isSubmitting,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                            .testTag("student_submit_verification_button"),
                        colors = ButtonDefaults.buttonColors(containerColor = BlueCorp)
                    ) {
                        if (isSubmitting) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                color = Color.White,
                                strokeWidth = 2.dp
                            )
                        } else {
                            Icon(Icons.Default.VerifiedUser, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                if (existingProfile == null) "Enviar para comprobación" else "Reenviar solicitud"
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ApprovedStudentCertificatesView(
    profile: StudentProfile,
    certificatesState: UiState<List<Certificate>>,
    verifierBaseUrl: String,
    onOpenDetail: (Certificate) -> Unit,
    onOpenWebView: (Certificate) -> Unit,
    onDownloadQr: (Certificate) -> Unit,
    onExportPdf: (Certificate) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, GoldPrimary.copy(alpha = 0.65f)),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Surface(shape = RoundedCornerShape(999.dp), color = ValidGreenBg) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(Icons.Default.VerifiedUser, contentDescription = null, tint = ValidGreenText, modifier = Modifier.size(14.dp))
                            Text(
                                text = "ALUMNO COMPROBADO · ${AdminConfig.OFFICIAL_ADMIN_EMAIL}",
                                style = MaterialTheme.typography.labelSmall,
                                color = ValidGreenText
                            )
                        }
                    }
                    Text(
                        text = profile.fullName,
                        style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.ExtraBold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Icon(Icons.Default.Badge, contentDescription = null, tint = BlueCorp, modifier = Modifier.size(16.dp))
                        Text(
                            text = "RUT: ${profile.rut} · ${profile.email}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }

        item {
            Text(
                text = "Mis certificaciones personales",
                style = MaterialTheme.typography.titleLarge
            )
        }

        when (certificatesState) {
            is UiState.Loading -> {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(color = BlueCorp)
                    }
                }
            }

            is UiState.Error -> {
                item {
                    Surface(shape = RoundedCornerShape(12.dp), color = InvalidRedBg) {
                        Text(
                            text = certificatesState.message,
                            color = InvalidRedText,
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.padding(14.dp)
                        )
                    }
                }
            }

            is UiState.Success -> {
                val certs = certificatesState.data
                if (certs.isEmpty()) {
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                        ) {
                            Column(modifier = Modifier.padding(20.dp)) {
                                Text(
                                    text = "Aún no tienes certificaciones asignadas",
                                    style = MaterialTheme.typography.titleMedium
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "Tu cuenta ya está verificada. En cuanto ${AdminConfig.OFFICIAL_ADMIN_EMAIL} emita o vincule un certificado con tu RUT o correo, aparecerá aquí.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                } else {
                    items(certs, key = { it.id.ifBlank { it.certificateCode } }) { cert ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(14.dp))
                                .clickable { onOpenDetail(cert) },
                            shape = RoundedCornerShape(14.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                                    verticalAlignment = Alignment.Top
                                ) {
                                    QrThumbnail(
                                        content = cert.publicLink(verifierBaseUrl),
                                        qrColorHex = cert.qrColor,
                                        qrPattern = cert.qrPattern,
                                        size = 70.dp,
                                        pixelSize = 220
                                    )
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = cert.certificateType,
                                            style = MaterialTheme.typography.labelSmall,
                                            color = GoldMuted
                                        )
                                        Text(
                                            text = cert.courseName,
                                            style = MaterialTheme.typography.titleMedium,
                                            maxLines = 2,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Text(
                                            text = "Código: ${cert.certificateCode}",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        Spacer(modifier = Modifier.height(6.dp))
                                        StatusBadge(status = cert.computedStatus())
                                    }
                                }

                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = "Emisión: ${DateFormatter.formatSpanish(cert.issuedAt)} · Vence: ${DateFormatter.formatSpanish(cert.expiresAt)}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )

                                Spacer(modifier = Modifier.height(10.dp))
                                HorizontalDivider()
                                Spacer(modifier = Modifier.height(8.dp))

                                Column(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        OutlinedButton(
                                            onClick = { onOpenDetail(cert) },
                                            modifier = Modifier
                                                .weight(1f)
                                                .height(48.dp),
                                            shape = RoundedCornerShape(10.dp),
                                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 8.dp)
                                        ) {
                                            Icon(
                                                Icons.Default.Visibility,
                                                contentDescription = null,
                                                modifier = Modifier.size(16.dp)
                                            )
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text(
                                                text = "Ver ficha",
                                                maxLines = 1,
                                                softWrap = false,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }
                                        Button(
                                            onClick = { onOpenWebView(cert) },
                                            modifier = Modifier
                                                .weight(1f)
                                                .height(48.dp),
                                            shape = RoundedCornerShape(10.dp),
                                            colors = ButtonDefaults.buttonColors(containerColor = BlueCorp),
                                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 8.dp)
                                        ) {
                                            Icon(
                                                Icons.Default.OpenInBrowser,
                                                contentDescription = null,
                                                modifier = Modifier.size(16.dp)
                                            )
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text(
                                                text = "Ver en web",
                                                maxLines = 1,
                                                softWrap = false,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }
                                    }

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        OutlinedButton(
                                            onClick = { onExportPdf(cert) },
                                            modifier = Modifier
                                                .weight(1f)
                                                .height(48.dp),
                                            shape = RoundedCornerShape(10.dp),
                                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 8.dp)
                                        ) {
                                            Icon(
                                                Icons.Default.PictureAsPdf,
                                                contentDescription = null,
                                                modifier = Modifier.size(16.dp)
                                            )
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text(
                                                text = "Exportar PDF",
                                                maxLines = 1,
                                                softWrap = false,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }
                                        OutlinedButton(
                                            onClick = { onDownloadQr(cert) },
                                            modifier = Modifier
                                                .weight(1f)
                                                .height(48.dp),
                                            shape = RoundedCornerShape(10.dp),
                                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 8.dp)
                                        ) {
                                            Icon(
                                                Icons.Default.Download,
                                                contentDescription = null,
                                                modifier = Modifier.size(16.dp)
                                            )
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text(
                                                text = "Guardar QR",
                                                maxLines = 1,
                                                softWrap = false,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
