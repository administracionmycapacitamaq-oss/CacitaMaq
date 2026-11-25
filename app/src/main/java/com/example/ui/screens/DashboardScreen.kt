package com.example.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material.icons.filled.WorkspacePremium
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.QrCode2
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Smartphone
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.model.Certificate
import com.example.data.model.SortOrder
import com.example.data.model.StatusFilter
import com.example.ui.components.QrThumbnail
import com.example.ui.components.StatusBadge
import com.example.ui.state.CertificateStats
import com.example.ui.state.DashboardState
import com.example.ui.state.UiState
import com.example.ui.theme.BlueCorp
import com.example.ui.theme.BlueSoft
import com.example.ui.theme.GoldMuted
import com.example.ui.theme.InvalidRedText
import com.example.util.DateFormatter

@Composable
fun DashboardScreen(
    uiState: UiState<DashboardState>,
    verifierBaseUrl: String = "",
    onCreateClick: () -> Unit,
    onOpenStudentsClick: () -> Unit,
    onOpenAnalyticsClick: () -> Unit,
    pendingStudentsCount: Int = 0,
    onSearchChange: (String) -> Unit,
    onStatusFilterChange: (StatusFilter) -> Unit,
    onSortChange: (SortOrder) -> Unit,
    onPageLimitChange: (Int) -> Unit,
    onOpenDetail: (Certificate) -> Unit,
    onOpenPublic: (Certificate) -> Unit,
    onEditCertificate: (Certificate) -> Unit,
    onDuplicateCertificate: (Certificate) -> Unit = {},
    onOpenWebPreview: (Certificate) -> Unit,
    onDownloadQr: (Certificate) -> Unit,
    onExportPdf: (Certificate) -> Unit,
    onShareCertificate: (Certificate) -> Unit,
    onTogglePublish: (Certificate) -> Unit,
    onDeleteCertificate: (Certificate) -> Unit,
    modifier: Modifier = Modifier
) {
    var pendingDelete by remember { mutableStateOf<Certificate?>(null) }

    pendingDelete?.let { target ->
        AlertDialog(
            onDismissRequest = { pendingDelete = null },
            title = { Text("Eliminar certificación") },
            text = {
                Text("¿Deseas eliminar permanentemente la certificación de ${target.studentName.ifBlank { target.certificateCode }}?")
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        onDeleteCertificate(target)
                        pendingDelete = null
                    }
                ) {
                    Text("Eliminar", color = InvalidRedText)
                }
            },
            dismissButton = {
                TextButton(onClick = { pendingDelete = null }) {
                    Text("Cancelar")
                }
            }
        )
    }

    when (uiState) {
        is UiState.Loading -> {
            Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    CircularProgressIndicator(color = BlueCorp)
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "Cargando certificaciones…",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        is UiState.Error -> {
            Box(
                modifier = modifier
                    .fillMaxSize()
                    .padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "No se pudo cargar la colección",
                            style = MaterialTheme.typography.titleMedium,
                            color = InvalidRedText
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = uiState.message,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }

        is UiState.Success -> {
            val data = uiState.data
            LazyColumn(
                modifier = modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "PANEL DE ADMINISTRACIÓN",
                                style = MaterialTheme.typography.labelSmall,
                                color = BlueCorp
                            )
                            Text(
                                text = "Mis códigos QR",
                                style = MaterialTheme.typography.headlineMedium,
                                color = MaterialTheme.colorScheme.onBackground
                            )
                            Text(
                                text = "Gestiona certificados, enlaces públicos y verificaciones desde un solo lugar.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Button(
                            onClick = onCreateClick,
                            modifier = Modifier.testTag("create_certification_button"),
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = BlueCorp)
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Crear")
                        }
                    }
                }

                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedButton(
                            onClick = onOpenStudentsClick,
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp)
                                .testTag("dashboard_students_button"),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Icon(
                                Icons.Default.VerifiedUser,
                                contentDescription = null,
                                tint = BlueCorp,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = if (pendingStudentsCount > 0) {
                                    "Comprobar alumnos ($pendingStudentsCount)"
                                } else {
                                    "Alumnos y Web QR"
                                },
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        OutlinedButton(
                            onClick = onOpenAnalyticsClick,
                            modifier = Modifier
                                .height(48.dp)
                                .testTag("dashboard_analytics_button"),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Icon(
                                Icons.Default.Analytics,
                                contentDescription = null,
                                tint = BlueCorp,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Analítica")
                        }
                    }
                }

                item {
                    HeroBanner()
                }

                item {
                    StatsStrip(stats = data.stats)
                }

                item {
                    FilterToolbar(
                        searchQuery = data.searchQuery,
                        statusFilter = data.statusFilter,
                        sortOrder = data.sortOrder,
                        pageLimit = data.pageLimit,
                        onSearchChange = onSearchChange,
                        onStatusFilterChange = onStatusFilterChange,
                        onSortChange = onSortChange,
                        onPageLimitChange = onPageLimitChange
                    )
                }

                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Lista de certificaciones",
                            style = MaterialTheme.typography.titleMedium
                        )
                        Text(
                            text = "${data.totalMatching} resultados",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                if (data.filteredRecords.isEmpty()) {
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(32.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Icon(
                                    imageVector = Icons.Default.QrCode2,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(44.dp)
                                )
                                Spacer(modifier = Modifier.height(10.dp))
                                Text(
                                    text = if (data.allRecords.isEmpty()) {
                                        "Todavía no hay certificaciones. Crea la primera para comenzar."
                                    } else {
                                        "No hay certificaciones que coincidan con los filtros."
                                    },
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                if (data.allRecords.isEmpty()) {
                                    Spacer(modifier = Modifier.height(14.dp))
                                    Button(onClick = onCreateClick) {
                                        Icon(Icons.Default.Add, contentDescription = null)
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("Crear certificación")
                                    }
                                }
                            }
                        }
                    }
                } else {
                    items(
                        items = data.filteredRecords,
                        key = { it.id.ifBlank { it.certificateCode } }
                    ) { cert ->
                        CertificateRecordCard(
                            certificate = cert,
                            verifierBaseUrl = verifierBaseUrl,
                            onOpenDetail = { onOpenDetail(cert) },
                            onOpenPublic = { onOpenPublic(cert) },
                            onEdit = { onEditCertificate(cert) },
                            onDuplicate = { onDuplicateCertificate(cert) },
                            onOpenWebPreview = { onOpenWebPreview(cert) },
                            onDownloadQr = { onDownloadQr(cert) },
                            onExportPdf = { onExportPdf(cert) },
                            onShare = { onShareCertificate(cert) },
                            onTogglePublish = { onTogglePublish(cert) },
                            onRequestDelete = { pendingDelete = cert }
                        )
                    }

                    item {
                        Text(
                            text = "Mostrando ${data.filteredRecords.size} de ${data.totalMatching}" +
                                if (data.allRecords.size >= 900) " · Cerca del límite de 999 registros" else "",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(vertical = 4.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun HeroBanner() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(
                Brush.horizontalGradient(
                    colors = listOf(Color(0xFF0E2C57), Color(0xFF1556A1))
                )
            )
            .padding(18.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Certificaciones que inspiran confianza.",
                    style = MaterialTheme.typography.titleLarge,
                    color = Color.White
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Identidad Capacitamaq, verificaciones individuales y códigos QR listos para compartir.",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFFD8E7FB)
                )
            }
            Surface(
                modifier = Modifier.size(88.dp),
                shape = RoundedCornerShape(14.dp),
                color = Color.White
            ) {
                Image(
                    painter = painterResource(id = R.drawable.img_capacitamaq_banner_1795460462074),
                    contentDescription = stringResource(R.string.cd_company_banner),
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(4.dp)
                        .clip(RoundedCornerShape(10.dp)),
                    contentScale = ContentScale.Fit
                )
            }
        }
    }
}

@Composable
private fun StatsStrip(stats: CertificateStats) {
    val items = listOf(
        Triple("Certificaciones", stats.total.toString(), Icons.Default.WorkspacePremium),
        Triple("Activos", stats.valid.toString(), Icons.Default.Verified),
        Triple("Pendientes", stats.pending.toString(), Icons.Default.Schedule),
        Triple("Vencidos", stats.expired.toString(), Icons.Default.CalendarToday),
        Triple("No válidos", stats.invalid.toString(), Icons.Default.Shield)
    )

    BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
        if (maxWidth >= 700.dp) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items.forEach { (label, value, icon) ->
                    StatCard(
                        label = label,
                        value = value,
                        icon = icon,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        } else {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items.forEach { (label, value, icon) ->
                    StatCard(
                        label = label,
                        value = value,
                        icon = icon,
                        modifier = Modifier.width(126.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun StatCard(
    label: String,
    value: String,
    icon: ImageVector,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = BlueCorp,
                    modifier = Modifier.size(16.dp)
                )
            }
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

@Composable
private fun FilterToolbar(
    searchQuery: String,
    statusFilter: StatusFilter,
    sortOrder: SortOrder,
    pageLimit: Int,
    onSearchChange: (String) -> Unit,
    onStatusFilterChange: (StatusFilter) -> Unit,
    onSortChange: (SortOrder) -> Unit,
    onPageLimitChange: (Int) -> Unit
) {
    var sortExpanded by remember { mutableStateOf(false) }
    var limitExpanded by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Buscar y filtrar certificaciones",
                style = MaterialTheme.typography.titleMedium
            )
            Spacer(modifier = Modifier.height(10.dp))
            OutlinedTextField(
                value = searchQuery,
                onValueChange = onSearchChange,
                placeholder = { Text("Buscar alumno, operador, curso o código…") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("dashboard_search_input"),
                shape = RoundedCornerShape(10.dp)
            )
            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                StatusFilter.entries.forEach { filter ->
                    FilterChip(
                        selected = statusFilter == filter,
                        onClick = { onStatusFilterChange(filter) },
                        label = { Text(filter.label, style = MaterialTheme.typography.labelMedium) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = BlueSoft,
                            selectedLabelColor = BlueCorp
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Box(modifier = Modifier.weight(1f)) {
                    OutlinedButton(
                        onClick = { sortExpanded = true },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            text = "Orden: ${sortOrder.label}",
                            style = MaterialTheme.typography.labelMedium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    DropdownMenu(
                        expanded = sortExpanded,
                        onDismissRequest = { sortExpanded = false }
                    ) {
                        SortOrder.entries.forEach { option ->
                            DropdownMenuItem(
                                text = { Text(option.label) },
                                onClick = {
                                    onSortChange(option)
                                    sortExpanded = false
                                }
                            )
                        }
                    }
                }

                Box(modifier = Modifier.weight(1f)) {
                    OutlinedButton(
                        onClick = { limitExpanded = true },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            text = "$pageLimit por página",
                            style = MaterialTheme.typography.labelMedium
                        )
                    }
                    DropdownMenu(
                        expanded = limitExpanded,
                        onDismissRequest = { limitExpanded = false }
                    ) {
                        listOf(10, 25, 50).forEach { limit ->
                            DropdownMenuItem(
                                text = { Text("$limit por página") },
                                onClick = {
                                    onPageLimitChange(limit)
                                    limitExpanded = false
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CertificateRecordCard(
    certificate: Certificate,
    verifierBaseUrl: String,
    onOpenDetail: () -> Unit,
    onOpenPublic: () -> Unit,
    onEdit: () -> Unit,
    onDuplicate: () -> Unit,
    onOpenWebPreview: () -> Unit,
    onDownloadQr: () -> Unit,
    onExportPdf: () -> Unit,
    onShare: () -> Unit,
    onTogglePublish: () -> Unit,
    onRequestDelete: () -> Unit
) {
    var menuExpanded by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .clickable { onOpenDetail() }
            .testTag("certificate_card_${certificate.certificateCode}"),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Top,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                QrThumbnail(
                    content = certificate.publicLink(verifierBaseUrl),
                    qrColorHex = certificate.qrColor,
                    qrPattern = certificate.qrPattern,
                    size = 64.dp,
                    pixelSize = 192
                )

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = certificate.certificateType.ifBlank { "Certificación" },
                        style = MaterialTheme.typography.labelSmall,
                        color = GoldMuted
                    )
                    Text(
                        text = buildString {
                            append(certificate.studentName.ifBlank { "Alumno sin nombre" })
                            if (certificate.studentRut.isNotBlank()) {
                                append(" · RUT ${certificate.studentRut}")
                            }
                        },
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "${certificate.courseName} · Operador: ${certificate.operatorName.ifBlank { "Sin asignar" }}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        StatusBadge(status = certificate.computedStatus())
                        Text(
                            text = "${certificate.scanCount} escaneos",
                            style = MaterialTheme.typography.labelSmall,
                            color = BlueCorp
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Emisión: ${DateFormatter.formatSpanish(certificate.issuedAt)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "Vence: ${DateFormatter.formatSpanish(certificate.expiresAt)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Text(
                text = certificate.certificateCode,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(top = 4.dp)
            )

            Spacer(modifier = Modifier.height(10.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.outline)
            Spacer(modifier = Modifier.height(6.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    modifier = Modifier
                        .weight(1f)
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    SmallActionButton(
                        icon = Icons.Default.Visibility,
                        label = "Detalle",
                        onClick = onOpenDetail
                    )
                    SmallActionButton(
                        icon = Icons.Default.Language,
                        label = "Web",
                        onClick = onOpenWebPreview
                    )
                    SmallActionButton(
                        icon = Icons.Default.Download,
                        label = "QR",
                        onClick = onDownloadQr
                    )
                    SmallActionButton(
                        icon = Icons.Default.Share,
                        label = "Compartir",
                        onClick = onShare
                    )
                }

                Box {
                    IconButton(
                        onClick = { menuExpanded = true },
                        modifier = Modifier.size(48.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.MoreVert,
                            contentDescription = "Más opciones",
                            tint = BlueCorp
                        )
                    }
                    DropdownMenu(
                        expanded = menuExpanded,
                        onDismissRequest = { menuExpanded = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("Vista pública móvil") },
                            leadingIcon = { Icon(Icons.Default.Smartphone, contentDescription = null) },
                            onClick = {
                                menuExpanded = false
                                onOpenPublic()
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Editar certificación") },
                            leadingIcon = { Icon(Icons.Default.Edit, contentDescription = null) },
                            onClick = {
                                menuExpanded = false
                                onEdit()
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Duplicar para otro alumno") },
                            leadingIcon = { Icon(Icons.Default.PersonAdd, contentDescription = null) },
                            onClick = {
                                menuExpanded = false
                                onDuplicate()
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Exportar certificado PDF") },
                            leadingIcon = { Icon(Icons.Default.PictureAsPdf, contentDescription = null) },
                            onClick = {
                                menuExpanded = false
                                onExportPdf()
                            }
                        )
                        DropdownMenuItem(
                            text = {
                                Text(
                                    if (certificate.status == "pending") {
                                        "Publicar certificación"
                                    } else {
                                        "Cambiar a pendiente"
                                    }
                                )
                            },
                            leadingIcon = { Icon(Icons.Default.SwapHoriz, contentDescription = null) },
                            onClick = {
                                menuExpanded = false
                                onTogglePublish()
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Eliminar", color = InvalidRedText) },
                            leadingIcon = {
                                Icon(
                                    Icons.Default.DeleteOutline,
                                    contentDescription = null,
                                    tint = InvalidRedText
                                )
                            },
                            onClick = {
                                menuExpanded = false
                                onRequestDelete()
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SmallActionButton(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit
) {
    OutlinedButton(
        onClick = onClick,
        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
        shape = RoundedCornerShape(8.dp),
        modifier = Modifier.height(40.dp)
    ) {
        Icon(icon, contentDescription = null, modifier = Modifier.size(15.dp))
        Spacer(modifier = Modifier.width(4.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            maxLines = 1,
            softWrap = false,
            overflow = TextOverflow.Ellipsis
        )
    }
}
