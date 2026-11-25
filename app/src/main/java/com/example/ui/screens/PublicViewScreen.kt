package com.example.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.data.model.Certificate
import com.example.ui.components.MobilePublicCard
import com.example.ui.theme.BlueCorp

@Composable
fun PublicViewScreen(
    certificate: Certificate?,
    verifierBaseUrl: String = "",
    onBackClick: () -> Unit,
    onOpenUrl: (String) -> Unit,
    onOpenWhatsApp: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 520.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "VERIFICACIÓN EN LÍNEA",
                    style = MaterialTheme.typography.labelSmall,
                    color = BlueCorp
                )
                Text(
                    text = certificate?.let { "Certificación ${it.computedStatus().label.lowercase()}" }
                        ?: "Certificación",
                    style = MaterialTheme.typography.headlineSmall
                )
            }
            OutlinedButton(onClick = onBackClick) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Volver")
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        if (certificate == null) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 48.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Certificado no disponible.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            MobilePublicCard(
                certificate = certificate,
                verifierBaseUrl = verifierBaseUrl,
                isPreview = false,
                onOpenUrl = onOpenUrl,
                onOpenWhatsApp = onOpenWhatsApp,
                modifier = Modifier.widthIn(max = 420.dp)
            )
        }
    }
}
