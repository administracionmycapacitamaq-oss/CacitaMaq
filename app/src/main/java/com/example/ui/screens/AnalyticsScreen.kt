package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.ui.state.DashboardState
import com.example.ui.state.UiState
import com.example.ui.theme.BlueCorp
import com.example.ui.theme.ExpiredOrangeText
import com.example.ui.theme.InvalidRedText
import com.example.ui.theme.PendingAmberText
import com.example.ui.theme.ValidGreenText

@Composable
fun AnalyticsScreen(
    uiState: UiState<DashboardState>,
    modifier: Modifier = Modifier
) {
    when (uiState) {
        is UiState.Loading -> {
            Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = BlueCorp)
            }
        }

        is UiState.Error -> {
            Box(modifier = modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
                Text(uiState.message, color = InvalidRedText)
            }
        }

        is UiState.Success -> {
            val stats = uiState.data.stats
            val records = uiState.data.allRecords
            val total = stats.total.coerceAtLeast(1)

            val metrics = listOf(
                "Certificaciones" to stats.total.toString(),
                "Certificadas" to stats.valid.toString(),
                "Pendientes" to stats.pending.toString(),
                "Vencidas" to stats.expired.toString(),
                "No válidas" to stats.invalid.toString(),
                "Escaneos totales" to stats.totalScans.toString()
            )

            val byType = records
                .groupBy { it.certificateType.ifBlank { "Sin tipo" } }
                .mapValues { it.value.size }
                .entries
                .sortedByDescending { it.value }

            LazyColumn(
                modifier = modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                item {
                    Column {
                        Text(
                            text = "RESUMEN DE ACTIVIDAD",
                            style = MaterialTheme.typography.labelSmall,
                            color = BlueCorp
                        )
                        Text(
                            text = "Datos analíticos",
                            style = MaterialTheme.typography.headlineMedium
                        )
                        Text(
                            text = "Estado de las certificaciones y consultas registradas en Capacitamaq.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                item {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        metrics.chunked(2).forEach { pair ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                pair.forEach { (label, value) ->
                                    Card(
                                        modifier = Modifier.weight(1f),
                                        shape = RoundedCornerShape(14.dp),
                                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                                    ) {
                                        Column(modifier = Modifier.padding(16.dp)) {
                                            Text(
                                                text = label,
                                                style = MaterialTheme.typography.labelMedium,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                            Spacer(modifier = Modifier.height(6.dp))
                                            Text(
                                                text = value,
                                                style = MaterialTheme.typography.displaySmall,
                                                color = MaterialTheme.colorScheme.onSurface
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(18.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Text("Distribución por estado", style = MaterialTheme.typography.titleMedium)
                            AnalyticsProgressBar("Certificadas", stats.valid, total, ValidGreenText)
                            AnalyticsProgressBar("Pendientes", stats.pending, total, PendingAmberText)
                            AnalyticsProgressBar("Vencidas", stats.expired, total, ExpiredOrangeText)
                            AnalyticsProgressBar("No válidas", stats.invalid, total, InvalidRedText)
                        }
                    }
                }

                if (byType.isNotEmpty()) {
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(14.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                        ) {
                            Column(
                                modifier = Modifier.padding(18.dp),
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Text("Por tipo de certificación", style = MaterialTheme.typography.titleMedium)
                                byType.forEach { (typeName, count) ->
                                    AnalyticsProgressBar(typeName, count, total, BlueCorp)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun AnalyticsProgressBar(
    label: String,
    count: Int,
    total: Int,
    barColor: Color
) {
    val ratio = (count.toFloat() / total.toFloat()).coerceIn(0f, 1f)
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(label, style = MaterialTheme.typography.bodySmall)
            Text(
                text = "$count (${(ratio * 100).toInt()}%)",
                style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold)
            )
        }
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(10.dp)
                .clip(RoundedCornerShape(99.dp))
                .background(MaterialTheme.colorScheme.outline.copy(alpha = 0.5f))
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(ratio)
                    .fillMaxHeight()
                    .clip(RoundedCornerShape(99.dp))
                    .background(barColor)
            )
        }
    }
}
