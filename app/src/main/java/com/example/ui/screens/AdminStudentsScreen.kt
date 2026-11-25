package com.example.ui.screens

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
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.OpenInBrowser
import androidx.compose.material.icons.filled.PersonSearch
import androidx.compose.material.icons.filled.QrCode2
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material3.AlertDialog
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
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.firebase.VerifierHostStatus
import com.example.data.model.AdminConfig
import com.example.data.model.Certificate
import com.example.data.model.RutUtils
import com.example.data.model.StudentProfile
import com.example.ui.state.UiState
import com.example.ui.theme.BlueCorp
import com.example.ui.theme.InvalidRedBg
import com.example.ui.theme.InvalidRedText
import com.example.ui.theme.PendingAmberBg
import com.example.ui.theme.PendingAmberText
import com.example.ui.theme.ValidGreenBg
import com.example.ui.theme.ValidGreenText

@Composable
fun AdminStudentsScreen(
    profilesState: UiState<List<StudentProfile>>,
    allCertificates: List<Certificate>,
    verifierHostStatus: VerifierHostStatus,
    onSaveVerifierUrl: (String) -> Unit,
    onRecheckVerifierHost: () -> Unit = {},
    onOpenFirebaseHostingConsole: () -> Unit = {},
    onApproveProfile: (StudentProfile) -> Unit,
    onRejectProfile: (StudentProfile, String) -> Unit,
    onIssueCertificateForStudent: (StudentProfile) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val activeUrl = verifierHostStatus.effectiveUrl
    var urlInput by remember {
        mutableStateOf(
            verifierHostStatus.rawUrl.ifBlank { AdminConfig.FIREBASE_HOSTING_DEFAULT_URL }
        )
    }
    var showConfigPanel by remember(verifierHostStatus.isLive) {
        mutableStateOf(!verifierHostStatus.isLive)
    }
    var rejectingTarget by remember { mutableStateOf<StudentProfile?>(null) }
    var rejectReason by remember { mutableStateOf("") }

    LaunchedEffect(verifierHostStatus.rawUrl, verifierHostStatus.isLive) {
        urlInput = verifierHostStatus.rawUrl.ifBlank { AdminConfig.FIREBASE_HOSTING_DEFAULT_URL }
        if (verifierHostStatus.isLive) {
            showConfigPanel = false
        }
    }

    rejectingTarget?.let { target ->
        AlertDialog(
            onDismissRequest = { rejectingTarget = null },
            title = { Text("Rechazar comprobación") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Indica el motivo para ${target.fullName} (RUT ${target.rut}):")
                    OutlinedTextField(
                        value = rejectReason,
                        onValueChange = { rejectReason = it.take(280) },
                        label = { Text("Motivo opcional") },
                        singleLine = false,
                        maxLines = 3,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        onRejectProfile(target, rejectReason)
                        rejectReason = ""
                        rejectingTarget = null
                    }
                ) {
                    Text("Rechazar", color = InvalidRedText)
                }
            },
            dismissButton = {
                TextButton(onClick = { rejectingTarget = null }) {
                    Text("Cancelar")
                }
            }
        )
    }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Column {
                Text(
                    text = "CONTROL OFICIAL · ${AdminConfig.OFFICIAL_ADMIN_EMAIL}",
                    style = MaterialTheme.typography.labelSmall,
                    color = BlueCorp
                )
                Text(
                    text = "Alumnos y Verificador Web",
                    style = MaterialTheme.typography.headlineMedium
                )
                Text(
                    text = "Comprueba la identidad por Nombre Completo y RUT de los alumnos y configura la URL pública del verificador QR.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        // Card 1: One-time Global Web Verifier URL Config
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.Language, contentDescription = null, tint = BlueCorp)
                            Column {
                                Text(
                                    text = "Verificador web · Firebase Hosting",
                                    style = MaterialTheme.typography.titleMedium
                                )
                                Text(
                                    text = "Hosting único oficial de Google Firebase para todos los QR y enlaces",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        if (verifierHostStatus.isLive) {
                            TextButton(onClick = { showConfigPanel = !showConfigPanel }) {
                                Text(if (showConfigPanel) "Ocultar" else "Cambiar")
                                Icon(
                                    if (showConfigPanel) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }

                    if (verifierHostStatus.isLive && activeUrl.isNotBlank()) {
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = ValidGreenBg,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    Icons.Default.CheckCircle,
                                    contentDescription = null,
                                    tint = ValidGreenText,
                                    modifier = Modifier.size(16.dp)
                                )
                                Text(
                                    text = "FIREBASE HOSTING ACTIVO EN VIVO: $activeUrl",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = ValidGreenText
                                )
                            }
                        }
                    } else if (verifierHostStatus.rawUrl.isNotBlank()) {
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = PendingAmberBg,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier.padding(12.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.Top,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Icon(
                                        Icons.Default.WarningAmber,
                                        contentDescription = null,
                                        tint = PendingAmberText,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Text(
                                        text = "Protección anti-404 activa: ${verifierHostStatus.rawUrl} aún no está publicado en Firebase Hosting. Tus QR, PDF y botones muestran la ficha verificable dentro de la app sin error 404.",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = PendingAmberText
                                    )
                                }
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    OutlinedButton(
                                        onClick = onRecheckVerifierHost,
                                        modifier = Modifier
                                            .weight(1f)
                                            .height(48.dp)
                                            .testTag("recheck_verifier_host_button")
                                    ) {
                                        Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("Reprobar", maxLines = 1)
                                    }
                                    OutlinedButton(
                                        onClick = { onSaveVerifierUrl("") },
                                        modifier = Modifier
                                            .weight(1f)
                                            .height(48.dp)
                                            .testTag("clear_pending_verifier_url_button")
                                    ) {
                                        Text("Limpiar URL", color = InvalidRedText, maxLines = 1)
                                    }
                                }
                            }
                        }
                    }

                    if (showConfigPanel) {
                        Text(
                            text = "Para publicar en https://gen-lang-client-0631567384.web.app sin error 404: entra una sola vez a Firebase Console → Usuarios y permisos, agrega a ${AdminConfig.OFFICIAL_ADMIN_EMAIL} como Propietario, luego en tu PC ejecuta «firebase logout», «firebase login» (con ${AdminConfig.OFFICIAL_ADMIN_EMAIL}) y «firebase deploy --only hosting», y pulsa «Verificar y activar Firebase Hosting».",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        OutlinedTextField(
                            value = urlInput,
                            onValueChange = { urlInput = it.take(300) },
                            label = { Text("URL de Firebase Hosting (https://...)") },
                            placeholder = { Text(AdminConfig.FIREBASE_HOSTING_DEFAULT_URL) },
                            singleLine = true,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("input_verifier_base_url")
                        )
                        Button(
                            onClick = { onSaveVerifierUrl(urlInput) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                                .testTag("activate_firebase_hosting_button"),
                            colors = ButtonDefaults.buttonColors(containerColor = BlueCorp)
                        ) {
                            Icon(Icons.Default.CloudUpload, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Verificar y activar Firebase Hosting")
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedButton(
                                onClick = onOpenFirebaseHostingConsole,
                                modifier = Modifier
                                    .weight(1f)
                                    .height(48.dp)
                            ) {
                                Icon(Icons.Default.OpenInBrowser, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Consola Firebase", maxLines = 1)
                            }
                            if (verifierHostStatus.rawUrl.isNotBlank()) {
                                OutlinedButton(
                                    onClick = {
                                        urlInput = AdminConfig.FIREBASE_HOSTING_DEFAULT_URL
                                        onSaveVerifierUrl("")
                                    },
                                    modifier = Modifier.height(48.dp)
                                ) {
                                    Text("Desactivar", color = InvalidRedText)
                                }
                            }
                        }
                    }
                }
            }
        }

        item {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.padding(top = 4.dp)
            ) {
                Icon(Icons.Default.PersonSearch, contentDescription = null, tint = BlueCorp)
                Text(
                    text = "Solicitudes de alumnos",
                    style = MaterialTheme.typography.titleLarge
                )
            }
        }

        when (profilesState) {
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
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = InvalidRedBg
                    ) {
                        Text(
                            text = profilesState.message,
                            color = InvalidRedText,
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.padding(14.dp)
                        )
                    }
                }
            }

            is UiState.Success -> {
                val profiles = profilesState.data
                if (profiles.isEmpty()) {
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                        ) {
                            Column(
                                modifier = Modifier.padding(20.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    text = "Aún no hay alumnos registrados para comprobación.",
                                    style = MaterialTheme.typography.titleSmall
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "Cuando un alumno ingrese con Google en el Portal Alumno e ingrese su Nombre Completo y RUT, aparecerá aquí para tu aprobación.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                } else {
                    items(profiles, key = { it.uid }) { profile ->
                        val profileRutClean = RutUtils.clean(profile.rut)
                        val profileEmailNorm = profile.email.trim().lowercase()
                        val matchingCount = allCertificates.count { cert ->
                            (profileRutClean.isNotBlank() && RutUtils.clean(cert.studentRut) == profileRutClean) ||
                                cert.studentEmail.trim().lowercase() == profileEmailNorm
                        }

                        StudentApprovalCard(
                            profile = profile,
                            matchingCount = matchingCount,
                            onApprove = { onApproveProfile(profile) },
                            onReject = {
                                rejectReason = ""
                                rejectingTarget = profile
                            },
                            onIssueCertificate = { onIssueCertificateForStudent(profile) }
                        )
                    }
                }
            }
        }

        item { Spacer(modifier = Modifier.height(16.dp)) }
    }
}

@Composable
private fun StudentApprovalCard(
    profile: StudentProfile,
    matchingCount: Int,
    onApprove: () -> Unit,
    onReject: () -> Unit,
    onIssueCertificate: () -> Unit
) {
    val (bg, fg, label, icon) = when (profile.verificationStatus) {
        "approved" -> Quad(ValidGreenBg, ValidGreenText, "COMPROBADO", Icons.Default.CheckCircle)
        "rejected" -> Quad(InvalidRedBg, InvalidRedText, "RECHAZADO", Icons.Default.Close)
        else -> Quad(PendingAmberBg, PendingAmberText, "PENDIENTE DE COMPROBACIÓN", Icons.Default.Schedule)
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(shape = RoundedCornerShape(999.dp), color = bg) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(5.dp)
                    ) {
                        Icon(icon, contentDescription = null, tint = fg, modifier = Modifier.size(14.dp))
                        Text(label, style = MaterialTheme.typography.labelSmall, color = fg)
                    }
                }
                Text(
                    text = "$matchingCount cert. por RUT/correo",
                    style = MaterialTheme.typography.labelSmall,
                    color = BlueCorp
                )
            }

            Text(
                text = profile.fullName,
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
            )
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Icon(Icons.Default.Badge, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
                Text(text = "RUT: ${profile.rut}", style = MaterialTheme.typography.bodyMedium)
            }
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Icon(Icons.Default.Email, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(16.dp))
                Text(
                    text = profile.email,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            if (profile.adminNote.isNotBlank()) {
                Text(
                    text = "Nota: ${profile.adminNote}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            HorizontalDivider()

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = onApprove,
                    modifier = Modifier
                        .weight(1f)
                        .height(44.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = BlueCorp)
                ) {
                    Icon(Icons.Default.VerifiedUser, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(if (profile.verificationStatus == "approved") "Re-vincular RUT" else "Comprobar y aprobar")
                }
                if (profile.verificationStatus != "rejected") {
                    OutlinedButton(
                        onClick = onReject,
                        modifier = Modifier.height(44.dp)
                    ) {
                        Text("Rechazar", color = InvalidRedText)
                    }
                }
            }

            OutlinedButton(
                onClick = onIssueCertificate,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(44.dp)
                    .testTag("issue_cert_for_${profile.uid}")
            ) {
                Icon(Icons.Default.QrCode2, contentDescription = null, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.primary)
                Spacer(modifier = Modifier.width(6.dp))
                Text("Emitir certificado y QR", color = MaterialTheme.colorScheme.onSurface)
            }
        }
    }
}

private data class Quad<A, B, C, D>(val first: A, val second: B, val third: C, val fourth: D)
