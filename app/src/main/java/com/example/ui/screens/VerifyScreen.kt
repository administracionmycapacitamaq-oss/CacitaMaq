package com.example.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import android.os.Handler
import android.os.Looper
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.border
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ImageSearch
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Smartphone
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.example.data.model.AdminConfig
import com.example.data.model.Certificate
import com.example.ui.components.StatusBadge
import com.example.ui.state.VerifyOutcome
import com.example.ui.theme.BlueCorp
import com.example.ui.theme.GoldPrimary
import com.example.ui.theme.InvalidRedBg
import com.example.ui.theme.InvalidRedText
import com.example.ui.theme.PendingAmberBg
import com.example.ui.theme.PendingAmberText
import com.example.ui.theme.ValidGreenBg
import com.example.ui.theme.ValidGreenText
import com.example.util.DateFormatter
import com.example.util.LocalImageStore
import com.example.util.QrCodecService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.concurrent.Executors

@Composable
fun VerifyScreen(
    verifyInput: String,
    outcome: VerifyOutcome,
    onInputChange: (String) -> Unit,
    onVerifySubmit: (String) -> Unit,
    onOpenPublicView: (Certificate) -> Unit,
    onShowMessage: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var showCameraScanner by remember { mutableStateOf(false) }
    var permissionDeniedMsg by remember { mutableStateOf<String?>(null) }

    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            permissionDeniedMsg = null
            showCameraScanner = true
        } else {
            permissionDeniedMsg = "Se requiere permiso de cámara para escanear códigos QR en vivo. Puedes usar «Escanear desde imagen» o escribir el código."
        }
    }

    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            scope.launch {
                val decoded = withContext(Dispatchers.IO) {
                    val bmp = LocalImageStore.decodeImageFromUri(context, uri)
                    val res = bmp?.let { QrCodecService.decodeQrFromBitmap(it) }
                    bmp?.recycle()
                    res
                }
                if (decoded != null) {
                    onVerifySubmit(decoded)
                } else {
                    onShowMessage("No se detectó un QR legible. Prueba con otra imagen.")
                }
            }
        }
    }

    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        val isWide = maxWidth >= 820.dp

        Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Column {
                Text(
                    text = "CONSULTA PÚBLICA",
                    style = MaterialTheme.typography.labelSmall,
                    color = BlueCorp
                )
                Text(
                    text = "Verificar certificación",
                    style = MaterialTheme.typography.headlineMedium
                )
                Text(
                    text = "Escanea con la cámara en vivo, elige una imagen QR o escribe el código público.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            if (isWide) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    VerifyControlsCard(
                        verifyInput = verifyInput,
                        showCameraScanner = showCameraScanner,
                        permissionDeniedMsg = permissionDeniedMsg,
                        onInputChange = onInputChange,
                        onVerifyClick = { onVerifySubmit(verifyInput) },
                        onToggleCamera = {
                            if (showCameraScanner) {
                                showCameraScanner = false
                            } else {
                                val granted = ContextCompat.checkSelfPermission(
                                    context,
                                    Manifest.permission.CAMERA
                                ) == PackageManager.PERMISSION_GRANTED
                                if (granted) {
                                    permissionDeniedMsg = null
                                    showCameraScanner = true
                                } else {
                                    cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
                                }
                            }
                        },
                        onPickImage = {
                            imagePickerLauncher.launch(
                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                            )
                        },
                        onCameraQrDetected = { raw ->
                            showCameraScanner = false
                            onVerifySubmit(raw)
                        },
                        modifier = Modifier.weight(1f)
                    )

                    VerifyResultCard(
                        outcome = outcome,
                        onOpenPublicView = onOpenPublicView,
                        modifier = Modifier.weight(1.1f)
                    )
                }
            } else {
                VerifyControlsCard(
                    verifyInput = verifyInput,
                    showCameraScanner = showCameraScanner,
                    permissionDeniedMsg = permissionDeniedMsg,
                    onInputChange = onInputChange,
                    onVerifyClick = { onVerifySubmit(verifyInput) },
                    onToggleCamera = {
                        if (showCameraScanner) {
                            showCameraScanner = false
                        } else {
                            val granted = ContextCompat.checkSelfPermission(
                                context,
                                Manifest.permission.CAMERA
                            ) == PackageManager.PERMISSION_GRANTED
                            if (granted) {
                                permissionDeniedMsg = null
                                showCameraScanner = true
                            } else {
                                cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
                            }
                        }
                    },
                    onPickImage = {
                        imagePickerLauncher.launch(
                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                        )
                    },
                    onCameraQrDetected = { raw ->
                        showCameraScanner = false
                        onVerifySubmit(raw)
                    }
                )

                VerifyResultCard(
                    outcome = outcome,
                    onOpenPublicView = onOpenPublicView
                )
            }
        }
    }
}

@Composable
private fun VerifyControlsCard(
    verifyInput: String,
    showCameraScanner: Boolean,
    permissionDeniedMsg: String?,
    onInputChange: (String) -> Unit,
    onVerifyClick: () -> Unit,
    onToggleCamera: () -> Unit,
    onPickImage: () -> Unit,
    onCameraQrDetected: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text("Buscar una certificación", style = MaterialTheme.typography.titleMedium)

            OutlinedTextField(
                value = verifyInput,
                onValueChange = onInputChange,
                label = { Text("Código o enlace público") },
                placeholder = { Text("CERTIFICACION-CM-…") },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("verify_code_input")
            )

            Button(
                onClick = onVerifyClick,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .testTag("verify_submit_button"),
                colors = ButtonDefaults.buttonColors(containerColor = BlueCorp)
            ) {
                Icon(Icons.Default.Search, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Verificar código")
            }

            HorizontalDivider()

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedButton(
                    onClick = onToggleCamera,
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                        .testTag("verify_camera_button")
                ) {
                    Icon(
                        if (showCameraScanner) Icons.Default.Close else Icons.Default.CameraAlt,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(if (showCameraScanner) "Cerrar cámara" else "Cámara en vivo")
                }

                OutlinedButton(
                    onClick = onPickImage,
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                        .testTag("verify_gallery_button")
                ) {
                    Icon(Icons.Default.ImageSearch, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Desde imagen")
                }
            }

            if (permissionDeniedMsg != null) {
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = InvalidRedBg,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = permissionDeniedMsg,
                        style = MaterialTheme.typography.bodySmall,
                        color = InvalidRedText,
                        modifier = Modifier.padding(12.dp)
                    )
                }
            }

            if (showCameraScanner) {
                LiveQrCameraBox(onQrDetected = onCameraQrDetected)
            }
        }
    }
}

@Composable
private fun LiveQrCameraBox(
    onQrDetected: (String) -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val mainHandler = remember { Handler(Looper.getMainLooper()) }
    val analysisExecutor = remember { Executors.newSingleThreadExecutor() }
    var detectedOnce by remember { mutableStateOf(false) }

    DisposableEffect(Unit) {
        onDispose {
            analysisExecutor.shutdown()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(260.dp)
            .clip(RoundedCornerShape(14.dp))
            .border(2.dp, GoldPrimary, RoundedCornerShape(14.dp))
    ) {
        AndroidView(
            factory = { ctx ->
                val previewView = PreviewView(ctx)
                val cameraProviderFuture = ProcessCameraProvider.getInstance(ctx)
                cameraProviderFuture.addListener({
                    val cameraProvider = cameraProviderFuture.get()
                    val preview = Preview.Builder().build().also {
                        it.surfaceProvider = previewView.surfaceProvider
                    }
                    val imageAnalysis = ImageAnalysis.Builder()
                        .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                        .build()
                        .also { analysis ->
                            analysis.setAnalyzer(analysisExecutor) { imageProxy ->
                                try {
                                    if (!detectedOnce) {
                                        val plane = imageProxy.planes.firstOrNull()
                                        if (plane != null) {
                                            val buffer = plane.buffer
                                            val bytes = ByteArray(buffer.remaining())
                                            buffer.get(bytes)
                                            val decoded = QrCodecService.decodeQrFromYuv(
                                                bytes,
                                                imageProxy.width,
                                                imageProxy.height
                                            )
                                            if (!decoded.isNullOrBlank()) {
                                                detectedOnce = true
                                                mainHandler.post { onQrDetected(decoded) }
                                            }
                                        }
                                    }
                                } finally {
                                    imageProxy.close()
                                }
                            }
                        }

                    runCatching {
                        cameraProvider.unbindAll()
                        cameraProvider.bindToLifecycle(
                            lifecycleOwner,
                            CameraSelector.DEFAULT_BACK_CAMERA,
                            preview,
                            imageAnalysis
                        )
                    }
                }, ContextCompat.getMainExecutor(ctx))
                previewView
            },
            modifier = Modifier.fillMaxSize()
        )

        Box(
            modifier = Modifier
                .align(Alignment.Center)
                .size(165.dp)
                .border(2.dp, GoldPrimary, RoundedCornerShape(16.dp))
        )
        Surface(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(10.dp),
            shape = RoundedCornerShape(99.dp),
            color = Color.Black.copy(alpha = 0.65f)
        ) {
            Text(
                text = "Apunta al código QR del certificado",
                color = Color.White,
                style = MaterialTheme.typography.labelSmall,
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
            )
        }
    }
}

@Composable
private fun VerifyResultCard(
    outcome: VerifyOutcome,
    onOpenPublicView: (Certificate) -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            when (outcome) {
                is VerifyOutcome.Idle -> {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Default.QrCodeScanner,
                            contentDescription = null,
                            tint = BlueCorp,
                            modifier = Modifier.size(22.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Resultado de verificación", style = MaterialTheme.typography.titleMedium)
                    }
                    Text(
                        text = "El resultado aparecerá aquí al introducir, pegar o escanear un código QR.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                is VerifyOutcome.NotFound -> {
                    Surface(
                        shape = RoundedCornerShape(99.dp),
                        color = InvalidRedBg
                    ) {
                        Text(
                            text = "NO VÁLIDA",
                            color = InvalidRedText,
                            style = MaterialTheme.typography.labelSmall,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                        )
                    }
                    Text("Código no registrado", style = MaterialTheme.typography.titleLarge)
                    Text(
                        text = "No se encontró una certificación con el código ${outcome.searchedCode}.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                is VerifyOutcome.Found -> {
                    val cert = outcome.certificate
                    val computed = cert.computedStatus()
                    StatusBadge(status = computed)
                    Text(
                        text = "Certificación ${computed.label}",
                        style = MaterialTheme.typography.titleLarge
                    )

                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = if (outcome.isLiveCloudVerified) ValidGreenBg else PendingAmberBg,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                Icons.Default.CheckCircle,
                                contentDescription = null,
                                tint = if (outcome.isLiveCloudVerified) ValidGreenText else PendingAmberText,
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = if (outcome.isLiveCloudVerified) {
                                    "VERIFICADO EN VIVO EN FIREBASE · Publicado por ${cert.verifiedByAdminEmail.ifBlank { AdminConfig.OFFICIAL_ADMIN_EMAIL }}"
                                } else {
                                    "Registro en borrador / no publicado en verificador público"
                                },
                                style = MaterialTheme.typography.labelSmall,
                                color = if (outcome.isLiveCloudVerified) ValidGreenText else PendingAmberText
                            )
                        }
                    }

                    val rows = buildList {
                        add("Alumno" to cert.studentName.ifBlank { "No indicado" })
                        if (cert.studentRut.isNotBlank()) {
                            add("RUT" to cert.studentRut)
                        }
                        add("Operador certificador" to cert.operatorName.ifBlank { "No indicado" })
                        add("Curso" to cert.courseName)
                        add("Equipo" to cert.equipment.ifBlank { "No indicado" })
                        add("Emisor" to cert.issuer)
                        add("Autorizado por" to cert.verifiedByAdminEmail.ifBlank { AdminConfig.OFFICIAL_ADMIN_EMAIL })
                        add("Emisión" to DateFormatter.formatSpanish(cert.issuedAt))
                        add("Vencimiento" to DateFormatter.formatSpanish(cert.expiresAt))
                        add("Código" to cert.certificateCode)
                    }

                    rows.forEach { (label, value) ->
                        HorizontalDivider(color = MaterialTheme.colorScheme.outline)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = label,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                text = value,
                                style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                                modifier = Modifier.weight(1f, fill = false)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))
                    Button(
                        onClick = { onOpenPublicView(cert) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = BlueCorp)
                    ) {
                        Icon(Icons.Default.Smartphone, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Abrir vista móvil")
                    }
                }
            }
        }
    }
}
