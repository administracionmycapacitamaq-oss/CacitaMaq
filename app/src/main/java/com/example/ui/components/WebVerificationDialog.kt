package com.example.ui.components

import android.annotation.SuppressLint
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.OpenInBrowser
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.model.AdminConfig
import com.example.data.model.Certificate
import com.example.ui.theme.BlueCorp
import com.example.ui.theme.PendingAmberBg
import com.example.ui.theme.PendingAmberText
import com.example.util.WebVerifierHtmlBuilder

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun WebVerificationDialog(
    certificate: Certificate,
    verifierBaseUrl: String,
    onDismiss: () -> Unit,
    onExportHtml: (Certificate) -> Unit,
    onOpenHostedUrl: (String) -> Unit
) {
    val context = LocalContext.current
    val htmlContent = remember(certificate.certificateCode, certificate.updatedAt, certificate.studentRut, certificate.studentName) {
        WebVerifierHtmlBuilder.buildSingleCertificateHtml(context, certificate)
    }
    val hasHostedWebUrl = verifierBaseUrl.startsWith("https://") || verifierBaseUrl.startsWith("http://")
    val resolvedLink = certificate.webUrlOrNull(verifierBaseUrl)

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .padding(14.dp),
            shape = RoundedCornerShape(20.dp),
            color = MaterialTheme.colorScheme.surface
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = if (hasHostedWebUrl) "VERIFICACIÓN WEB OFICIAL" else "FICHA WEB VERIFICABLE CAPACITAMAQ",
                            style = MaterialTheme.typography.labelSmall,
                            color = BlueCorp
                        )
                        Text(
                            text = resolvedLink ?: "${certificate.certificateCode} · Verificado por ${AdminConfig.OFFICIAL_ADMIN_EMAIL}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Cerrar vista web")
                    }
                }
                HorizontalDivider()

                if (!hasHostedWebUrl) {
                    Surface(
                        color = PendingAmberBg,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "Firebase Hosting (${AdminConfig.FIREBASE_HOSTING_DEFAULT_URL}) aún no está publicado en la web. Mostrando la ficha web verificada dentro de la app para evitar el error 404 en el navegador.",
                            style = MaterialTheme.typography.bodySmall,
                            color = PendingAmberText,
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)
                        )
                    }
                    HorizontalDivider()
                }

                AndroidView(
                    factory = { ctx ->
                        WebView(ctx).apply {
                            settings.javaScriptEnabled = true
                            settings.domStorageEnabled = true
                            webViewClient = WebViewClient()
                            loadDataWithBaseURL(
                                "https://capacitamaq.local/",
                                htmlContent,
                                "text/html",
                                "UTF-8",
                                null
                            )
                        }
                    },
                    update = { webView ->
                        webView.loadDataWithBaseURL(
                            "https://capacitamaq.local/",
                            htmlContent,
                            "text/html",
                            "UTF-8",
                            null
                        )
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                )

                HorizontalDivider()
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = { onExportHtml(certificate) },
                        modifier = Modifier
                            .weight(1f)
                            .height(46.dp)
                    ) {
                        Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Compartir HTML")
                    }
                    if (resolvedLink != null) {
                        Button(
                            onClick = { onOpenHostedUrl(resolvedLink) },
                            modifier = Modifier
                                .weight(1f)
                                .height(46.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = BlueCorp)
                        ) {
                            Icon(Icons.Default.OpenInBrowser, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Abrir URL web")
                        }
                    }
                }
            }
        }
    }
}
