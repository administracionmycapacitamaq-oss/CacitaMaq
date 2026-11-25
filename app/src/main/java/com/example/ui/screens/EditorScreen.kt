package com.example.ui.screens

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.QrCode2
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Smartphone
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.model.AdminConfig
import com.example.data.model.CertificateDefaults
import com.example.data.model.CertificateDraft
import com.example.data.model.RutUtils
import com.example.data.model.StudentProfile
import com.example.ui.components.CredentialImage
import com.example.ui.components.MobilePublicCard
import com.example.ui.components.QrThumbnail
import com.example.ui.components.parseHexOrDefault
import com.example.ui.theme.BlueCorp
import com.example.ui.theme.BlueSoft
import com.example.ui.theme.ValidGreenBg
import com.example.ui.theme.ValidGreenText
import com.example.util.DateFormatter
import com.example.util.LocalImageStore
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditorScreen(
    draft: CertificateDraft,
    isSaving: Boolean,
    verifierBaseUrl: String = "",
    registeredStudents: List<StudentProfile> = emptyList(),
    hasSavedPreset: Boolean = false,
    onDraftChange: ((CertificateDraft) -> CertificateDraft) -> Unit,
    onSaveClick: () -> Unit,
    onCancelClick: () -> Unit,
    onShowMessage: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val isEditing = draft.editingId != null

    var activeTab by remember { mutableIntStateOf(0) }
    var dateTarget by remember { mutableStateOf<String?>(null) }

    val logoPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            scope.launch {
                LocalImageStore.copyPickedImageToInternalStorage(context, uri, "logo").fold(
                    onSuccess = { path -> onDraftChange { it.copy(logoPath = path) } },
                    onFailure = { onShowMessage("No se pudo guardar el logo seleccionado.") }
                )
            }
        }
    }

    val companyPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            scope.launch {
                LocalImageStore.copyPickedImageToInternalStorage(context, uri, "company").fold(
                    onSuccess = { path -> onDraftChange { it.copy(companyImagePath = path) } },
                    onFailure = { onShowMessage("No se pudo guardar la imagen de empresa.") }
                )
            }
        }
    }

    val operatorPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            scope.launch {
                LocalImageStore.copyPickedImageToInternalStorage(context, uri, "operator").fold(
                    onSuccess = { path -> onDraftChange { it.copy(operatorPhotoPath = path) } },
                    onFailure = { onShowMessage("No se pudo guardar la foto del operador.") }
                )
            }
        }
    }

    if (dateTarget != null) {
        val datePickerState = rememberDatePickerState()
        DatePickerDialog(
            onDismissRequest = { dateTarget = null },
            confirmButton = {
                TextButton(
                    onClick = {
                        val millis = datePickerState.selectedDateMillis
                        if (millis != null) {
                            val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.US).apply {
                                timeZone = TimeZone.getTimeZone("UTC")
                            }
                            val iso = sdf.format(Date(millis))
                            if (dateTarget == "issued") {
                                onDraftChange {
                                    it.copy(
                                        issuedAt = iso,
                                        expiresAt = DateFormatter.addOneYearIso(iso)
                                    )
                                }
                            } else {
                                onDraftChange { it.copy(expiresAt = iso) }
                            }
                        }
                        dateTarget = null
                    }
                ) {
                    Text("Aceptar")
                }
            },
            dismissButton = {
                TextButton(onClick = { dateTarget = null }) {
                    Text("Cancelar")
                }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }

    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        val isWide = maxWidth >= 860.dp

        Column(modifier = Modifier.fillMaxSize()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "EDITOR AUTOMÁTICO",
                        style = MaterialTheme.typography.labelSmall,
                        color = BlueCorp
                    )
                    Text(
                        text = if (isEditing) "Editar certificación" else "Nueva certificación y QR",
                        style = MaterialTheme.typography.headlineMedium
                    )
                }
                OutlinedButton(onClick = onCancelClick) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Volver")
                }
            }

            if (!isWide) {
                SingleChoiceSegmentedButtonRow(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                        .padding(bottom = 10.dp)
                ) {
                    SegmentedButton(
                        selected = activeTab == 0,
                        onClick = { activeTab = 0 },
                        shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2)
                    ) {
                        Text("Formulario")
                    }
                    SegmentedButton(
                        selected = activeTab == 1,
                        onClick = { activeTab = 1 },
                        shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2),
                        icon = {
                            Icon(Icons.Default.Smartphone, contentDescription = null, modifier = Modifier.size(16.dp))
                        }
                    ) {
                        Text("Vista móvil")
                    }
                }
            }

            if (isWide) {
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(20.dp)
                ) {
                    EditorFormColumn(
                        draft = draft,
                        isEditing = isEditing,
                        isSaving = isSaving,
                        verifierBaseUrl = verifierBaseUrl,
                        registeredStudents = registeredStudents,
                        hasSavedPreset = hasSavedPreset,
                        onDraftChange = onDraftChange,
                        onPickDate = { dateTarget = it },
                        onPickLogo = {
                            logoPicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                        },
                        onPickCompany = {
                            companyPicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                        },
                        onPickOperator = {
                            operatorPicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                        },
                        onSaveClick = onSaveClick,
                        onCancelClick = onCancelClick,
                        modifier = Modifier.weight(1.35f)
                    )

                    PreviewPhoneColumn(
                        draft = draft,
                        verifierBaseUrl = verifierBaseUrl,
                        modifier = Modifier
                            .weight(0.95f)
                            .verticalScroll(rememberScrollState())
                            .padding(bottom = 24.dp)
                    )
                }
            } else if (activeTab == 0) {
                EditorFormColumn(
                    draft = draft,
                    isEditing = isEditing,
                    isSaving = isSaving,
                    verifierBaseUrl = verifierBaseUrl,
                    registeredStudents = registeredStudents,
                    hasSavedPreset = hasSavedPreset,
                    onDraftChange = onDraftChange,
                    onPickDate = { dateTarget = it },
                    onPickLogo = {
                        logoPicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                    },
                    onPickCompany = {
                        companyPicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                    },
                    onPickOperator = {
                        operatorPicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                    },
                    onSaveClick = onSaveClick,
                    onCancelClick = onCancelClick,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp)
                )
            } else {
                PreviewPhoneColumn(
                    draft = draft,
                    verifierBaseUrl = verifierBaseUrl,
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(16.dp)
                )
            }
        }
    }
}

@Composable
private fun EditorFormColumn(
    draft: CertificateDraft,
    isEditing: Boolean,
    isSaving: Boolean,
    verifierBaseUrl: String,
    registeredStudents: List<StudentProfile>,
    hasSavedPreset: Boolean,
    onDraftChange: ((CertificateDraft) -> CertificateDraft) -> Unit,
    onPickDate: (String) -> Unit,
    onPickLogo: () -> Unit,
    onPickCompany: () -> Unit,
    onPickOperator: () -> Unit,
    onSaveClick: () -> Unit,
    onCancelClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    var showVisualCustomization by remember(draft.editingId) { mutableStateOf(false) }
    var showAdvancedMeta by remember(draft.editingId) { mutableStateOf(false) }
    var studentMenuExpanded by remember { mutableStateOf(false) }

    val previewCert = draft.toPreviewCertificate()
    val previewLink = previewCert.publicLink(verifierBaseUrl)

    Column(
        modifier = modifier.verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Automatic QR & Publication Card (no setup from 0!)
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(
                modifier = Modifier.padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    QrThumbnail(
                        content = previewLink,
                        qrColorHex = draft.qrColor,
                        qrPattern = draft.qrPattern,
                        size = 70.dp,
                        pixelSize = 256
                    )
                    Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Surface(shape = RoundedCornerShape(999.dp), color = ValidGreenBg) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(Icons.Default.VerifiedUser, contentDescription = null, tint = ValidGreenText, modifier = Modifier.size(13.dp))
                                Text(
                                    text = "QR Y ENLACE AUTOMÁTICOS",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = ValidGreenText
                                )
                            }
                        }
                        Text(
                            text = previewCert.certificateCode,
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = if (hasSavedPreset && !isEditing) {
                                "Plantilla de empresa, logos y QR heredados automáticamente. Solo ingresa al alumno y guarda."
                            } else {
                                "Al guardar se publica en la nube bajo ${AdminConfig.OFFICIAL_ADMIN_EMAIL} y queda verificable al instante."
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }

        // Quick registered student picker when creating
        if (!isEditing && registeredStudents.isNotEmpty()) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = BlueSoft.copy(alpha = 0.45f))
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(Icons.Default.PersonAdd, contentDescription = null, tint = BlueCorp, modifier = Modifier.size(18.dp))
                        Text(
                            text = "Autocompletar desde alumno registrado",
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                            color = BlueCorp
                        )
                    }
                    Box(modifier = Modifier.fillMaxWidth()) {
                        OutlinedButton(
                            onClick = { studentMenuExpanded = true },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text(
                                text = if (draft.studentName.isNotBlank()) {
                                    "Seleccionado: ${draft.studentName} (${draft.studentRut})"
                                } else {
                                    "Elegir alumno registrado (${registeredStudents.size})"
                                },
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        DropdownMenu(
                            expanded = studentMenuExpanded,
                            onDismissRequest = { studentMenuExpanded = false }
                        ) {
                            registeredStudents.forEach { profile ->
                                DropdownMenuItem(
                                    text = {
                                        Column {
                                            Text(profile.fullName, fontWeight = FontWeight.SemiBold)
                                            Text(
                                                "RUT: ${profile.rut} · ${profile.email}",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    },
                                    onClick = {
                                        onDraftChange {
                                            it.copy(
                                                studentName = profile.fullName,
                                                studentRut = RutUtils.format(profile.rut),
                                                studentEmail = profile.email.lowercase()
                                            )
                                        }
                                        studentMenuExpanded = false
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }

        // Panel 1: Alumno y Certificación (streamlined)
        EditorPanel(
            icon = Icons.Default.Description,
            title = "Datos del alumno y certificación",
            description = "Código, vigencia (+1 año) y formato corporativo se rellenan automáticamente."
        ) {
            OutlinedTextField(
                value = draft.studentName,
                onValueChange = { v -> onDraftChange { it.copy(studentName = v.take(100)) } },
                label = { Text("Nombre completo del alumno *") },
                placeholder = { Text("Nombre y apellidos") },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("input_student_name")
            )
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = draft.studentRut,
                    onValueChange = { v -> onDraftChange { it.copy(studentRut = v.take(24)) } },
                    label = { Text("RUT del alumno") },
                    placeholder = { Text("12.345.678-9") },
                    singleLine = true,
                    modifier = Modifier
                        .weight(1f)
                        .testTag("input_student_rut")
                )
                OutlinedTextField(
                    value = draft.studentEmail,
                    onValueChange = { v -> onDraftChange { it.copy(studentEmail = v.take(120)) } },
                    label = { Text("Correo Google alumno") },
                    placeholder = { Text("alumno@gmail.com") },
                    singleLine = true,
                    modifier = Modifier
                        .weight(1.3f)
                        .testTag("input_student_email")
                )
            }
            OutlinedTextField(
                value = draft.courseName,
                onValueChange = { v -> onDraftChange { it.copy(courseName = v.take(120)) } },
                label = { Text("Título de la certificación *") },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("input_course_name")
            )
            OutlinedTextField(
                value = draft.equipment,
                onValueChange = { v -> onDraftChange { it.copy(equipment = v.take(100)) } },
                label = { Text("Equipo o maquinaria") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = draft.issuedAt,
                    onValueChange = { v -> onDraftChange { it.copy(issuedAt = v.take(10)) } },
                    label = { Text("Emisión *") },
                    trailingIcon = {
                        IconButton(onClick = { onPickDate("issued") }) {
                            Icon(Icons.Default.CalendarMonth, contentDescription = "Elegir fecha de emisión")
                        }
                    },
                    singleLine = true,
                    modifier = Modifier.weight(1f)
                )
                OutlinedTextField(
                    value = draft.expiresAt,
                    onValueChange = { v -> onDraftChange { it.copy(expiresAt = v.take(10)) } },
                    label = { Text("Vencimiento") },
                    trailingIcon = {
                        Row {
                            if (draft.expiresAt.isNotBlank()) {
                                IconButton(onClick = { onDraftChange { it.copy(expiresAt = "") } }) {
                                    Icon(Icons.Default.Clear, contentDescription = "Limpiar vencimiento")
                                }
                            }
                            IconButton(onClick = { onPickDate("expiry") }) {
                                Icon(Icons.Default.CalendarMonth, contentDescription = "Elegir vencimiento")
                            }
                        }
                    },
                    singleLine = true,
                    modifier = Modifier.weight(1f)
                )
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .clickable { showAdvancedMeta = !showAdvancedMeta }
                    .padding(vertical = 6.dp, horizontal = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Detalles del operador, entidad y código (${draft.operatorName.ifBlank { draft.issuer }})",
                    style = MaterialTheme.typography.labelLarge,
                    color = BlueCorp,
                    modifier = Modifier.weight(1f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Icon(
                    if (showAdvancedMeta) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                    contentDescription = "Mostrar u ocultar detalles adicionales",
                    tint = BlueCorp
                )
            }

            AnimatedVisibility(visible = showAdvancedMeta) {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = draft.description,
                        onValueChange = { v -> onDraftChange { it.copy(description = v.take(400)) } },
                        label = { Text("Texto de validación") },
                        minLines = 2,
                        maxLines = 4,
                        modifier = Modifier.fillMaxWidth()
                    )
                    DropdownSelectorField(
                        label = "Tipo de certificación *",
                        selected = draft.certificateType,
                        options = CertificateDefaults.CERTIFICATE_TYPES,
                        onSelect = { v -> onDraftChange { it.copy(certificateType = v) } }
                    )
                    OutlinedTextField(
                        value = draft.operatorName,
                        onValueChange = { v -> onDraftChange { it.copy(operatorName = v.take(100)) } },
                        label = { Text("Nombre completo del operador certificador") },
                        placeholder = { Text("Se recordará para los próximos alumnos") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        OutlinedTextField(
                            value = draft.operatorRole,
                            onValueChange = { v -> onDraftChange { it.copy(operatorRole = v.take(100)) } },
                            label = { Text("Cargo operador") },
                            placeholder = { Text("Instructor certificador") },
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = draft.operatorIdentifier,
                            onValueChange = { v -> onDraftChange { it.copy(operatorIdentifier = v.take(80)) } },
                            label = { Text("ID operador") },
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                    }
                    OutlinedTextField(
                        value = draft.issuer,
                        onValueChange = { v -> onDraftChange { it.copy(issuer = v.take(100)) } },
                        label = { Text("Entidad certificadora *") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = draft.certificateCode,
                        onValueChange = { v -> onDraftChange { it.copy(certificateCode = v.take(100)) } },
                        label = { Text("Identificador público único") },
                        trailingIcon = {
                            IconButton(
                                onClick = {
                                    onDraftChange { it.copy(certificateCode = DateFormatter.generateCertificateCode()) }
                                }
                            ) {
                                Icon(Icons.Default.Refresh, contentDescription = "Generar otro código")
                            }
                        },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        OutlinedTextField(
                            value = draft.whatsapp,
                            onValueChange = { v -> onDraftChange { it.copy(whatsapp = v.take(40)) } },
                            label = { Text("WhatsApp empresa") },
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = draft.email,
                            onValueChange = { v -> onDraftChange { it.copy(email = v.take(120)) } },
                            label = { Text("Correo empresa") },
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }

        // Collapsible Branding / Appearance / QR Style section (already inherited!)
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { showVisualCustomization = !showVisualCustomization },
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = BlueCorp, modifier = Modifier.size(18.dp))
                        Column {
                            Text(
                                text = "Logos, colores y diseño del QR",
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                            )
                            Text(
                                text = "Ya configurados y heredados automáticamente · Toca para ajustar",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    Icon(
                        if (showVisualCustomization) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                        contentDescription = "Expandir o contraer diseño",
                        tint = BlueCorp
                    )
                }

                AnimatedVisibility(visible = showVisualCustomization) {
                    Column(
                        modifier = Modifier.padding(top = 14.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Icon(Icons.Default.PhotoLibrary, contentDescription = null, tint = BlueCorp, modifier = Modifier.size(16.dp))
                            Text("Imágenes de la credencial", style = MaterialTheme.typography.titleSmall)
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            ImageSlotPicker(
                                label = "Logo circular",
                                customPath = draft.logoPath,
                                fallbackRes = R.drawable.img_capacitamaq_seal_1795460461997,
                                onPick = onPickLogo,
                                onReset = { onDraftChange { it.copy(logoPath = "") } },
                                modifier = Modifier.weight(1f)
                            )
                            ImageSlotPicker(
                                label = "Imagen empresa",
                                customPath = draft.companyImagePath,
                                fallbackRes = R.drawable.img_capacitamaq_banner_1795460462074,
                                onPick = onPickCompany,
                                onReset = { onDraftChange { it.copy(companyImagePath = "") } },
                                modifier = Modifier.weight(1f)
                            )
                            ImageSlotPicker(
                                label = "Foto operador",
                                customPath = draft.operatorPhotoPath,
                                fallbackRes = R.drawable.img_capacitamaq_seal_1795460461997,
                                onPick = onPickOperator,
                                onReset = { onDraftChange { it.copy(operatorPhotoPath = "") } },
                                modifier = Modifier.weight(1f)
                            )
                        }

                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Icon(Icons.Default.Palette, contentDescription = null, tint = BlueCorp, modifier = Modifier.size(16.dp))
                            Text("Apariencia", style = MaterialTheme.typography.titleSmall)
                        }
                        ColorSelectorRow(
                            label = "Color cabecera principal",
                            selectedHex = draft.themeColor,
                            presets = listOf("#f3c970", "#f6ce65", "#1554a0", "#102b53", "#18ba68"),
                            onSelect = { hex -> onDraftChange { it.copy(themeColor = hex) } }
                        )
                        ColorSelectorRow(
                            label = "Fondo de página pública",
                            selectedHex = draft.backgroundColor,
                            presets = listOf("#ffffff", "#f5f7fb", "#fffcf4", "#f0f4fa"),
                            onSelect = { hex -> onDraftChange { it.copy(backgroundColor = hex) } }
                        )
                        ColorSelectorRow(
                            label = "Fondo de tarjetas",
                            selectedHex = draft.cardColor,
                            presets = listOf("#fafafa", "#f1f5f9", "#ffffff", "#fff8e7"),
                            onSelect = { hex -> onDraftChange { it.copy(cardColor = hex) } }
                        )
                        DropdownSelectorField(
                            label = "Tipografía",
                            selected = draft.fontChoice,
                            options = CertificateDefaults.FONT_CHOICES,
                            onSelect = { font -> onDraftChange { it.copy(fontChoice = font) } }
                        )
                        Text("Distribución", style = MaterialTheme.typography.labelLarge)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            CertificateDefaults.TEMPLATES.forEach { (key, label) ->
                                FilterChip(
                                    selected = draft.template == key,
                                    onClick = { onDraftChange { it.copy(template = key) } },
                                    label = { Text(label.substringBefore(" ·"), style = MaterialTheme.typography.labelMedium) }
                                )
                            }
                        }

                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Icon(Icons.Default.QrCode2, contentDescription = null, tint = BlueCorp, modifier = Modifier.size(16.dp))
                            Text("Estilo del código QR", style = MaterialTheme.typography.titleSmall)
                        }
                        ColorSelectorRow(
                            label = "Color del QR",
                            selectedHex = draft.qrColor,
                            presets = listOf("#102b53", "#1554a0", "#000000", "#178655"),
                            onSelect = { hex -> onDraftChange { it.copy(qrColor = hex) } }
                        )
                        Text("Marco del QR", style = MaterialTheme.typography.labelLarge)
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            CertificateDefaults.QR_PATTERNS.forEach { (key, label) ->
                                FilterChip(
                                    selected = draft.qrPattern == key,
                                    onClick = { onDraftChange { it.copy(qrPattern = key) } },
                                    label = { Text(label, style = MaterialTheme.typography.labelMedium) }
                                )
                            }
                        }
                        OutlinedTextField(
                            value = draft.qrCaption,
                            onValueChange = { v -> onDraftChange { it.copy(qrCaption = v.take(50)) } },
                            label = { Text("Texto bajo el QR") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }
        }

        // Panel: Publicación y Guardado
        EditorPanel(
            icon = Icons.Default.Send,
            title = "Publicación automática",
            description = "Al publicar se habilita de inmediato el QR y la verificación en vivo bajo ${AdminConfig.OFFICIAL_ADMIN_EMAIL}."
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                CertificateDefaults.PUBLICATION_STATUSES.forEach { (key, label) ->
                    FilterChip(
                        selected = draft.status == key,
                        onClick = { onDraftChange { it.copy(status = key) } },
                        label = { Text(label) },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            Spacer(modifier = Modifier.height(6.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedButton(
                    onClick = onCancelClick,
                    enabled = !isSaving,
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                ) {
                    Text("Cancelar")
                }
                Button(
                    onClick = onSaveClick,
                    enabled = !isSaving,
                    modifier = Modifier
                        .weight(1.5f)
                        .height(48.dp)
                        .testTag("save_certificate_button"),
                    colors = ButtonDefaults.buttonColors(containerColor = BlueCorp)
                ) {
                    if (isSaving) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(18.dp),
                            color = Color.White,
                            strokeWidth = 2.dp
                        )
                    } else {
                        Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(if (isEditing) "Guardar cambios" else "Publicar y generar QR")
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))
    }
}

@Composable
private fun PreviewPhoneColumn(
    draft: CertificateDraft,
    verifierBaseUrl: String,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Vista previa móvil · página pública",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(10.dp))
        Box(
            modifier = Modifier
                .widthIn(max = 330.dp)
                .fillMaxWidth()
                .clip(RoundedCornerShape(32.dp))
                .border(6.dp, Color(0xFF202A37), RoundedCornerShape(32.dp))
                .background(Color.White)
        ) {
            MobilePublicCard(
                certificate = draft.toPreviewCertificate(),
                verifierBaseUrl = verifierBaseUrl,
                isPreview = true
            )
        }
    }
}

@Composable
private fun EditorPanel(
    icon: ImageVector,
    title: String,
    description: String,
    content: @Composable () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(icon, contentDescription = null, tint = BlueCorp, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text(title, style = MaterialTheme.typography.titleMedium)
            }
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            content()
        }
    }
}

@Composable
private fun DropdownSelectorField(
    label: String,
    selected: String,
    options: List<String>,
    onSelect: (String) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    Box(modifier = Modifier.fillMaxWidth()) {
        OutlinedTextField(
            value = selected,
            onValueChange = {},
            readOnly = true,
            label = { Text(label) },
            modifier = Modifier
                .fillMaxWidth()
                .clickable { expanded = true }
        )
        Box(
            modifier = Modifier
                .matchParentSize()
                .clickable { expanded = true }
        )
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            options.forEach { opt ->
                DropdownMenuItem(
                    text = { Text(opt) },
                    onClick = {
                        onSelect(opt)
                        expanded = false
                    }
                )
            }
        }
    }
}

@Composable
private fun ImageSlotPicker(
    label: String,
    customPath: String,
    fallbackRes: Int,
    onPick: () -> Unit,
    onReset: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        CredentialImage(
            customPath = customPath,
            fallbackRes = fallbackRes,
            contentDescription = label,
            modifier = Modifier
                .fillMaxWidth()
                .height(74.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(Color.White)
                .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(10.dp))
                .clickable { onPick() },
            contentScale = if (customPath.isBlank()) ContentScale.Fit else ContentScale.Crop
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium
        )
        OutlinedButton(
            onClick = onPick,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(8.dp)
        ) {
            Text("Elegir", style = MaterialTheme.typography.labelSmall)
        }
        if (customPath.isNotBlank()) {
            TextButton(onClick = onReset) {
                Text("Restaurar", style = MaterialTheme.typography.labelSmall)
            }
        }
    }
}

@Composable
private fun ColorSelectorRow(
    label: String,
    selectedHex: String,
    presets: List<String>,
    onSelect: (String) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(label, style = MaterialTheme.typography.labelLarge)
        Row(
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            presets.forEach { hex ->
                val isSelected = selectedHex.equals(hex, ignoreCase = true)
                val color = parseHexOrDefault(hex, Color.Gray)
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(color)
                        .border(
                            width = if (isSelected) 3.dp else 1.dp,
                            color = if (isSelected) BlueCorp else MaterialTheme.colorScheme.outline,
                            shape = CircleShape
                        )
                        .clickable { onSelect(hex) }
                )
            }
        }
    }
}
