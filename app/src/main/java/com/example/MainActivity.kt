package com.example

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddCircleOutline
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.credentials.CredentialManager
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.auth.AuthPortalRole
import com.example.auth.AuthScreen
import com.example.auth.attemptAutoSignIn
import com.example.auth.authStateFlow
import com.example.auth.signOut
import com.example.data.firebase.CertificateRepository
import com.example.data.model.AdminConfig
import com.example.data.model.Certificate
import com.example.ui.components.CapacitamaqTopBar
import com.example.ui.components.WebVerificationDialog
import com.example.ui.screens.AdminStudentsScreen
import com.example.ui.screens.AnalyticsScreen
import com.example.ui.screens.DashboardScreen
import com.example.ui.screens.DetailScreen
import com.example.ui.screens.EditorScreen
import com.example.ui.screens.PublicViewScreen
import com.example.ui.screens.StudentPortalScreen
import com.example.ui.screens.VerifyScreen
import com.example.ui.state.CertificatesViewModel
import com.example.ui.state.StudentPortalViewModel
import com.example.ui.state.UiState
import com.example.ui.state.VerifyOutcome
import com.example.ui.theme.BlueCorp
import com.example.ui.theme.CapacitamaqTheme
import com.example.ui.theme.InvalidRedBg
import com.example.ui.theme.InvalidRedText
import com.example.ui.theme.NavyDeep
import com.example.util.ExportManager
import com.example.util.QrCodecService
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch

object Routes {
    const val DASHBOARD = "dashboard"
    const val CREATE = "create"
    const val STUDENTS = "students"
    const val ANALYTICS = "analytics"
    const val VERIFY = "verify"
    const val DETAIL = "detail/{code}"
    const val PUBLIC = "public/{code}"

    fun detail(code: String) = "detail/${Uri.encode(code)}"
    fun public(code: String) = "public/${Uri.encode(code)}"
}

private object StudentRoutes {
    const val HOME = "student_home"
    const val DETAIL = "student_detail/{code}"
    const val PUBLIC = "student_public/{code}"

    fun detail(code: String) = "student_detail/${Uri.encode(code)}"
    fun public(code: String) = "student_public/${Uri.encode(code)}"
}

private data class NavDestinationItem(
    val route: String,
    val labelRes: Int,
    val icon: ImageVector,
    val badgeCount: Int = 0
)

class MainActivity : ComponentActivity() {
    private val deepLinkFlow = MutableStateFlow<String?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        deepLinkFlow.value = intent?.dataString
        enableEdgeToEdge()
        setContent {
            CapacitamaqTheme {
                val incomingDeepLink by deepLinkFlow.collectAsStateWithLifecycle()
                AppNavigation(
                    incomingDeepLink = incomingDeepLink,
                    onConsumeDeepLink = { deepLinkFlow.value = null }
                )
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        intent.dataString?.let { data ->
            deepLinkFlow.value = data
        }
    }
}

@Composable
fun AppNavigation(
    incomingDeepLink: String? = null,
    onConsumeDeepLink: () -> Unit = {}
) {
    val auth = Firebase.auth
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val credentialManager = remember { CredentialManager.create(context) }
    val currentUser by auth.authStateFlow().collectAsStateWithLifecycle(initialValue = auth.currentUser)
    var selectedPortalRole by rememberSaveable { mutableStateOf(AuthPortalRole.STUDENT) }

    LaunchedEffect(Unit) {
        if (auth.currentUser == null) {
            attemptAutoSignIn(
                context = context,
                credentialManager = credentialManager,
                onAuthSuccess = {},
                onUnauthenticated = {},
                scope = scope
            )
        }
    }

    val user = currentUser
    if (user == null) {
        AuthScreen(
            selectedRole = selectedPortalRole,
            onSelectRole = { selectedPortalRole = it },
            onAuthSuccess = { role -> selectedPortalRole = role }
        )
    } else if (AdminConfig.isOfficialAdmin(user.email)) {
        key(user.uid) {
            AdminAppShell(
                currentUser = user,
                incomingDeepLink = incomingDeepLink,
                onConsumeDeepLink = onConsumeDeepLink,
                onSignOutClick = {
                    signOut(credentialManager = credentialManager, scope = scope)
                }
            )
        }
    } else if (selectedPortalRole == AuthPortalRole.ADMIN) {
        AdminUnauthorizedScreen(
            userEmail = user.email.orEmpty(),
            onSwitchToStudent = { selectedPortalRole = AuthPortalRole.STUDENT },
            onSignOutClick = {
                selectedPortalRole = AuthPortalRole.STUDENT
                signOut(credentialManager = credentialManager, scope = scope)
            }
        )
    } else {
        key(user.uid) {
            StudentAppShell(
                currentUser = user,
                incomingDeepLink = incomingDeepLink,
                onConsumeDeepLink = onConsumeDeepLink,
                onSignOutClick = {
                    signOut(credentialManager = credentialManager, scope = scope)
                }
            )
        }
    }
}

@Composable
private fun AdminUnauthorizedScreen(
    userEmail: String,
    onSwitchToStudent: () -> Unit,
    onSignOutClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(NavyDeep, BlueCorp, MaterialTheme.colorScheme.background)
                )
            )
            .safeDrawingPadding()
            .padding(20.dp),
        contentAlignment = Alignment.Center
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 460.dp),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Surface(
                    shape = RoundedCornerShape(99.dp),
                    color = InvalidRedBg
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.Lock,
                            contentDescription = null,
                            tint = InvalidRedText,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "ACCESO RESTRINGIDO",
                            style = MaterialTheme.typography.labelSmall,
                            color = InvalidRedText
                        )
                    }
                }

                Text(
                    text = "Acceso exclusivo de Administrador",
                    style = MaterialTheme.typography.headlineSmall,
                    textAlign = TextAlign.Center
                )
                Text(
                    text = "La cuenta $userEmail no tiene permisos de edición ni creación de certificados.\n\nÚnicamente el correo oficial autorizador ${AdminConfig.OFFICIAL_ADMIN_EMAIL} puede ingresar como administrador.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(6.dp))

                Button(
                    onClick = onSwitchToStudent,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                        .testTag("switch_to_student_portal_button"),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = BlueCorp)
                ) {
                    Icon(Icons.Default.Person, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Entrar como Alumno con esta cuenta")
                }

                OutlinedButton(
                    onClick = onSignOutClick,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("unauthorized_sign_out_button"),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Cerrar sesión y usar correo admin")
                }
            }
        }
    }
}

@Composable
private fun AdminAppShell(
    currentUser: FirebaseUser,
    incomingDeepLink: String?,
    onConsumeDeepLink: () -> Unit,
    onSignOutClick: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    val navController = rememberNavController()

    val viewModel: CertificatesViewModel = viewModel(
        key = "admin_${currentUser.uid}",
        factory = viewModelFactory {
            initializer {
                val app = checkNotNull(this[APPLICATION_KEY])
                val databaseId = app.getString(R.string.firestore_database_id)
                val db = FirebaseFirestore.getInstance(databaseId)
                CertificatesViewModel(app.applicationContext, CertificateRepository(db), currentUser.uid)
            }
        }
    )

    val dashboardState by viewModel.dashboardState.collectAsStateWithLifecycle()
    val editorDraft by viewModel.editorDraft.collectAsStateWithLifecycle()
    val isSaving by viewModel.isSaving.collectAsStateWithLifecycle()
    val verifyInput by viewModel.verifyInput.collectAsStateWithLifecycle()
    val verifyOutcome by viewModel.verifyOutcome.collectAsStateWithLifecycle()
    val verifierHostStatus by viewModel.verifierHostStatus.collectAsStateWithLifecycle()
    val verifierBaseUrl by viewModel.verifierBaseUrl.collectAsStateWithLifecycle()
    val studentProfilesState by viewModel.studentProfilesState.collectAsStateWithLifecycle()
    val pendingStudentsCount by viewModel.pendingStudentsCount.collectAsStateWithLifecycle()
    val allRecordsState by viewModel.allRecordsState.collectAsStateWithLifecycle()

    var webPreviewCode by remember { mutableStateOf<String?>(null) }
    val webPreviewCert = remember(webPreviewCode, allRecordsState) {
        val targetCode = webPreviewCode ?: return@remember null
        (allRecordsState as? UiState.Success)?.data?.firstOrNull {
            it.certificateCode.equals(targetCode, ignoreCase = true)
        }
    }

    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route ?: Routes.DASHBOARD

    val breadcrumb = when {
        currentRoute == Routes.DASHBOARD -> "Admin · Códigos QR"
        currentRoute == Routes.CREATE -> if (editorDraft.editingId != null) "Editar" else "Crear"
        currentRoute == Routes.STUDENTS -> "Comprobar alumnos"
        currentRoute == Routes.ANALYTICS -> "Datos analíticos"
        currentRoute == Routes.VERIFY -> "Verificar QR"
        currentRoute.startsWith("detail") -> "Detalle"
        currentRoute.startsWith("public") -> "Credencial web"
        else -> "Capacitamaq"
    }

    val topDestinations = listOf(
        NavDestinationItem(Routes.DASHBOARD, R.string.nav_dashboard, Icons.Default.Dashboard),
        NavDestinationItem(Routes.CREATE, R.string.nav_create, Icons.Default.AddCircleOutline),
        NavDestinationItem(Routes.STUDENTS, R.string.nav_students, Icons.Default.VerifiedUser, pendingStudentsCount),
        NavDestinationItem(Routes.VERIFY, R.string.nav_verify, Icons.Default.QrCodeScanner)
    )

    fun showMessage(msg: String) {
        scope.launch {
            snackbarHostState.showSnackbar(msg)
        }
    }

    fun navigateTop(route: String) {
        if (route == Routes.CREATE) {
            viewModel.startNewCertificate()
        }
        navController.navigate(route) {
            popUpTo(navController.graph.findStartDestination().id) {
                saveState = false
            }
            launchSingleTop = true
        }
    }

    LaunchedEffect(incomingDeepLink, allRecordsState) {
        val raw = incomingDeepLink ?: return@LaunchedEffect
        val code = QrCodecService.extractCertificateCode(raw)
        if (code.isBlank()) {
            onConsumeDeepLink()
            return@LaunchedEffect
        }
        when (val state = allRecordsState) {
            is UiState.Loading -> return@LaunchedEffect
            is UiState.Success -> {
                viewModel.verifyCodeOrUrl(raw, incrementScan = true)
                val found = state.data.firstOrNull {
                    it.certificateCode.equals(code, ignoreCase = true)
                }
                if (found != null) {
                    webPreviewCode = found.certificateCode
                } else {
                    navController.navigate(Routes.VERIFY) {
                        launchSingleTop = true
                    }
                }
                onConsumeDeepLink()
            }
            is UiState.Error -> {
                viewModel.verifyCodeOrUrl(raw, incrementScan = true)
                navController.navigate(Routes.VERIFY) {
                    launchSingleTop = true
                }
                onConsumeDeepLink()
            }
        }
    }

    webPreviewCert?.let { targetCert ->
        WebVerificationDialog(
            certificate = targetCert,
            verifierBaseUrl = verifierBaseUrl,
            onDismiss = { webPreviewCode = null },
            onExportHtml = { cert ->
                scope.launch {
                    ExportManager.exportSingleCertificateHtml(context, cert).fold(
                        onSuccess = { uri ->
                            ExportManager.shareUri(
                                context = context,
                                uri = uri,
                                mimeType = "text/html",
                                chooserTitle = "Compartir ficha web verificable",
                                subject = "${cert.studentName} · ${cert.courseName}"
                            )
                            showMessage("Ficha HTML guardada en Descargas/Capacitamaq.")
                        },
                        onFailure = { showMessage("No se pudo exportar la vista HTML.") }
                    )
                }
            },
            onOpenHostedUrl = { url -> ExportManager.openExternalUrl(context, url) }
        )
    }

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val useRail = maxWidth >= 600.dp

        Scaffold(
            contentWindowInsets = WindowInsets(0, 0, 0, 0),
            containerColor = MaterialTheme.colorScheme.background,
            snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
            topBar = {
                CapacitamaqTopBar(
                    userEmail = currentUser.email,
                    breadcrumb = breadcrumb,
                    onSignOutClick = onSignOutClick
                )
            },
            bottomBar = {
                if (!useRail) {
                    NavigationBar(
                        modifier = Modifier.navigationBarsPadding(),
                        containerColor = MaterialTheme.colorScheme.surface
                    ) {
                        topDestinations.forEach { item ->
                            val selected = currentRoute == item.route
                            NavigationBarItem(
                                selected = selected,
                                onClick = { navigateTop(item.route) },
                                icon = {
                                    DestinationIconWithBadge(item = item)
                                },
                                label = { Text(stringResource(item.labelRes)) }
                            )
                        }
                    }
                }
            }
        ) { innerPadding ->
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                if (useRail) {
                    NavigationRail(
                        containerColor = MaterialTheme.colorScheme.surface
                    ) {
                        topDestinations.forEach { item ->
                            val selected = currentRoute == item.route
                            NavigationRailItem(
                                selected = selected,
                                onClick = { navigateTop(item.route) },
                                icon = {
                                    DestinationIconWithBadge(item = item)
                                },
                                label = { Text(stringResource(item.labelRes)) }
                            )
                        }
                    }
                }

                NavHost(
                    navController = navController,
                    startDestination = Routes.DASHBOARD,
                    modifier = Modifier.weight(1f)
                ) {
                    composable(Routes.DASHBOARD) {
                        DashboardScreen(
                            uiState = dashboardState,
                            verifierBaseUrl = verifierBaseUrl,
                            onCreateClick = {
                                viewModel.startNewCertificate()
                                navController.navigate(Routes.CREATE)
                            },
                            onOpenStudentsClick = { navigateTop(Routes.STUDENTS) },
                            onOpenAnalyticsClick = { navController.navigate(Routes.ANALYTICS) },
                            pendingStudentsCount = pendingStudentsCount,
                            onSearchChange = viewModel::setSearchQuery,
                            onStatusFilterChange = viewModel::setStatusFilter,
                            onSortChange = viewModel::setSortOrder,
                            onPageLimitChange = viewModel::setPageLimit,
                            onOpenDetail = { cert ->
                                navController.navigate(Routes.detail(cert.certificateCode))
                            },
                            onOpenPublic = { cert ->
                                navController.navigate(Routes.public(cert.certificateCode))
                            },
                            onEditCertificate = { cert ->
                                viewModel.startEditing(cert)
                                navController.navigate(Routes.CREATE)
                            },
                            onDuplicateCertificate = { cert ->
                                viewModel.duplicateCertificateForNewStudent(cert)
                                navController.navigate(Routes.CREATE)
                            },
                            onOpenWebPreview = { cert ->
                                val webUrl = cert.webUrlOrNull(verifierBaseUrl)
                                if (webUrl != null) {
                                    ExportManager.openExternalUrl(context, webUrl)
                                } else {
                                    webPreviewCode = cert.certificateCode
                                }
                            },
                            onDownloadQr = { cert ->
                                scope.launch {
                                    val qr = QrCodecService.generateQrBitmap(
                                        content = cert.publicLink(verifierBaseUrl),
                                        colorHex = cert.qrColor,
                                        sizePx = 1024,
                                        pattern = cert.qrPattern
                                    )
                                    ExportManager.saveQrPng(context, qr, cert.certificateCode).fold(
                                        onSuccess = { showMessage(it) },
                                        onFailure = { showMessage("No se pudo guardar el QR.") }
                                    )
                                }
                            },
                            onExportPdf = { cert ->
                                scope.launch {
                                    val qr = QrCodecService.generateQrBitmap(
                                        content = cert.publicLink(verifierBaseUrl),
                                        colorHex = cert.qrColor,
                                        sizePx = 512,
                                        pattern = cert.qrPattern
                                    )
                                    ExportManager.exportCertificatePdf(context, cert, qr, verifierBaseUrl).fold(
                                        onSuccess = { uri ->
                                            ExportManager.shareUri(
                                                context = context,
                                                uri = uri,
                                                mimeType = "application/pdf",
                                                chooserTitle = "Compartir o imprimir certificado PDF",
                                                subject = cert.courseName
                                            )
                                            showMessage("Certificado PDF exportado en Descargas/Capacitamaq.")
                                        },
                                        onFailure = { showMessage("No se pudo exportar el PDF.") }
                                    )
                                }
                            },
                            onShareCertificate = { cert ->
                                val webUrl = cert.webUrlOrNull(verifierBaseUrl)
                                if (webUrl != null) {
                                    ExportManager.shareLink(context, cert.courseName, webUrl)
                                } else {
                                    scope.launch {
                                        val qr = QrCodecService.generateQrBitmap(
                                            content = cert.publicLink(verifierBaseUrl),
                                            colorHex = cert.qrColor,
                                            sizePx = 1024,
                                            pattern = cert.qrPattern
                                        )
                                        ExportManager.createQrShareUri(context, qr, cert.certificateCode).fold(
                                            onSuccess = { uri ->
                                                ExportManager.shareUri(
                                                    context = context,
                                                    uri = uri,
                                                    mimeType = "image/png",
                                                    chooserTitle = "Compartir código QR",
                                                    subject = cert.courseName
                                                )
                                            },
                                            onFailure = {
                                                ExportManager.shareLink(
                                                    context,
                                                    cert.courseName,
                                                    cert.publicSummaryText(verifierBaseUrl)
                                                )
                                            }
                                        )
                                    }
                                }
                            },
                            onTogglePublish = { cert ->
                                viewModel.togglePublishStatus(cert) { msg -> showMessage(msg) }
                            },
                            onDeleteCertificate = { cert ->
                                viewModel.deleteCertificate(
                                    certificate = cert,
                                    onDeleted = { showMessage("Certificación eliminada.") },
                                    onError = { showMessage(it) }
                                )
                            }
                        )
                    }

                    composable(Routes.CREATE) {
                        val registeredStudents = (studentProfilesState as? UiState.Success)?.data.orEmpty()
                        EditorScreen(
                            draft = editorDraft,
                            isSaving = isSaving,
                            verifierBaseUrl = verifierBaseUrl,
                            registeredStudents = registeredStudents,
                            hasSavedPreset = viewModel.hasSavedPreset,
                            onDraftChange = viewModel::updateDraft,
                            onSaveClick = {
                                viewModel.saveDraft(
                                    onSuccess = { saved, wasEdit ->
                                        showMessage(
                                            if (wasEdit) "Cambios guardados y actualizados en Firebase."
                                            else "Certificación publicada en Firebase con QR listo."
                                        )
                                        navController.navigate(Routes.detail(saved.certificateCode)) {
                                            popUpTo(Routes.DASHBOARD)
                                        }
                                    },
                                    onError = { err -> showMessage(err) }
                                )
                            },
                            onCancelClick = { navController.popBackStack() },
                            onShowMessage = { showMessage(it) }
                        )
                    }

                    composable(Routes.STUDENTS) {
                        val allCerts = (dashboardState as? UiState.Success)?.data?.allRecords.orEmpty()
                        AdminStudentsScreen(
                            profilesState = studentProfilesState,
                            allCertificates = allCerts,
                            verifierHostStatus = verifierHostStatus,
                            onSaveVerifierUrl = { url ->
                                viewModel.saveVerifierBaseUrl(url) { msg -> showMessage(msg) }
                            },
                            onRecheckVerifierHost = {
                                viewModel.recheckVerifierHost()
                                showMessage("Comprobando si Firebase Hosting ya está en línea...")
                            },
                            onOpenFirebaseHostingConsole = {
                                ExportManager.openExternalUrl(
                                    context,
                                    "https://console.firebase.google.com/project/gen-lang-client-0631567384/hosting"
                                )
                            },
                            onApproveProfile = { profile ->
                                viewModel.approveStudentProfile(profile) { msg -> showMessage(msg) }
                            },
                            onRejectProfile = { profile, reason ->
                                viewModel.rejectStudentProfile(profile, reason) { msg -> showMessage(msg) }
                            },
                            onIssueCertificateForStudent = { profile ->
                                viewModel.startNewCertificateForStudent(profile)
                                navController.navigate(Routes.CREATE)
                            }
                        )
                    }

                    composable(Routes.ANALYTICS) {
                        AnalyticsScreen(uiState = dashboardState)
                    }

                    composable(Routes.VERIFY) {
                        VerifyScreen(
                            verifyInput = verifyInput,
                            outcome = verifyOutcome,
                            onInputChange = viewModel::setVerifyInput,
                            onVerifySubmit = { raw -> viewModel.verifyCodeOrUrl(raw) },
                            onOpenPublicView = { cert ->
                                navController.navigate(Routes.public(cert.certificateCode))
                            },
                            onShowMessage = { showMessage(it) }
                        )
                    }

                    composable(
                        route = Routes.DETAIL,
                        arguments = listOf(navArgument("code") { type = NavType.StringType })
                    ) { backStackEntry ->
                        val code = Uri.decode(backStackEntry.arguments?.getString("code").orEmpty())
                        val liveRecordsState by viewModel.allRecordsState.collectAsStateWithLifecycle()
                        val cert = (liveRecordsState as? UiState.Success)?.data?.firstOrNull {
                            it.certificateCode.equals(code, ignoreCase = true)
                        }
                        DetailScreen(
                            certificate = cert,
                            verifierBaseUrl = verifierBaseUrl,
                            readOnly = false,
                            onBackClick = { navController.popBackStack() },
                            onOpenPublicView = { item ->
                                navController.navigate(Routes.public(item.certificateCode))
                            },
                            onOpenWebPreview = { item ->
                                val webUrl = item.webUrlOrNull(verifierBaseUrl)
                                if (webUrl != null) {
                                    ExportManager.openExternalUrl(context, webUrl)
                                } else {
                                    webPreviewCode = item.certificateCode
                                }
                            },
                            onEditClick = { item ->
                                viewModel.startEditing(item)
                                navController.navigate(Routes.CREATE)
                            },
                            onDuplicateForNewStudent = { item ->
                                viewModel.duplicateCertificateForNewStudent(item)
                                navController.navigate(Routes.CREATE)
                            },
                            onDownloadQr = { item ->
                                scope.launch {
                                    val qr = QrCodecService.generateQrBitmap(
                                        content = item.publicLink(verifierBaseUrl),
                                        colorHex = item.qrColor,
                                        sizePx = 1024,
                                        pattern = item.qrPattern
                                    )
                                    ExportManager.saveQrPng(context, qr, item.certificateCode).fold(
                                        onSuccess = { showMessage(it) },
                                        onFailure = { showMessage("No se pudo guardar el QR.") }
                                    )
                                }
                            },
                            onExportPdf = { item ->
                                scope.launch {
                                    val qr = QrCodecService.generateQrBitmap(
                                        content = item.publicLink(verifierBaseUrl),
                                        colorHex = item.qrColor,
                                        sizePx = 512,
                                        pattern = item.qrPattern
                                    )
                                    ExportManager.exportCertificatePdf(context, item, qr, verifierBaseUrl).fold(
                                        onSuccess = { uri ->
                                            ExportManager.shareUri(
                                                context = context,
                                                uri = uri,
                                                mimeType = "application/pdf",
                                                chooserTitle = "Compartir o imprimir certificado PDF",
                                                subject = item.courseName
                                            )
                                            showMessage("Certificado PDF exportado en Descargas/Capacitamaq.")
                                        },
                                        onFailure = { showMessage("No se pudo exportar el PDF.") }
                                    )
                                }
                            },
                            onShareClick = { item ->
                                scope.launch {
                                    val qr = QrCodecService.generateQrBitmap(
                                        content = item.publicLink(verifierBaseUrl),
                                        colorHex = item.qrColor,
                                        sizePx = 1024,
                                        pattern = item.qrPattern
                                    )
                                    ExportManager.createQrShareUri(context, qr, item.certificateCode).fold(
                                        onSuccess = { uri ->
                                            ExportManager.shareUri(
                                                context = context,
                                                uri = uri,
                                                mimeType = "image/png",
                                                chooserTitle = "Compartir código QR",
                                                subject = item.courseName
                                            )
                                        },
                                        onFailure = {
                                            ExportManager.shareLink(context, item.courseName, item.publicLink(verifierBaseUrl))
                                        }
                                    )
                                }
                            },
                            onTogglePublish = { item ->
                                viewModel.togglePublishStatus(item) { msg -> showMessage(msg) }
                            },
                            onDeleteClick = { item ->
                                viewModel.deleteCertificate(
                                    certificate = item,
                                    onDeleted = {
                                        showMessage("Certificación eliminada.")
                                        navController.popBackStack(Routes.DASHBOARD, inclusive = false)
                                    },
                                    onError = { showMessage(it) }
                                )
                            }
                        )
                    }

                    composable(
                        route = Routes.PUBLIC,
                        arguments = listOf(navArgument("code") { type = NavType.StringType })
                    ) { backStackEntry ->
                        val code = Uri.decode(backStackEntry.arguments?.getString("code").orEmpty())
                        val liveRecordsState by viewModel.allRecordsState.collectAsStateWithLifecycle()
                        val cert = (liveRecordsState as? UiState.Success)?.data?.firstOrNull {
                            it.certificateCode.equals(code, ignoreCase = true)
                        }
                        PublicViewScreen(
                            certificate = cert,
                            verifierBaseUrl = verifierBaseUrl,
                            onBackClick = { navController.popBackStack() },
                            onOpenUrl = { url -> ExportManager.openExternalUrl(context, url) },
                            onOpenWhatsApp = { phone -> ExportManager.openWhatsApp(context, phone) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun DestinationIconWithBadge(item: NavDestinationItem) {
    if (item.badgeCount > 0) {
        BadgedBox(
            badge = {
                Badge {
                    Text(item.badgeCount.coerceAtMost(99).toString())
                }
            }
        ) {
            Icon(
                imageVector = item.icon,
                contentDescription = stringResource(item.labelRes)
            )
        }
    } else {
        Icon(
            imageVector = item.icon,
            contentDescription = stringResource(item.labelRes)
        )
    }
}

@Composable
private fun StudentAppShell(
    currentUser: FirebaseUser,
    incomingDeepLink: String?,
    onConsumeDeepLink: () -> Unit,
    onSignOutClick: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    val navController = rememberNavController()

    val viewModel: StudentPortalViewModel = viewModel(
        key = "student_${currentUser.uid}",
        factory = viewModelFactory {
            initializer {
                val app = checkNotNull(this[APPLICATION_KEY])
                val databaseId = app.getString(R.string.firestore_database_id)
                val db = FirebaseFirestore.getInstance(databaseId)
                StudentPortalViewModel(
                    repository = CertificateRepository(db),
                    currentUserId = currentUser.uid,
                    studentEmail = currentUser.email.orEmpty()
                )
            }
        }
    )

    val profileState by viewModel.profileState.collectAsStateWithLifecycle()
    val certificatesState by viewModel.myCertificatesState.collectAsStateWithLifecycle()
    val verifierBaseUrl by viewModel.verifierBaseUrl.collectAsStateWithLifecycle()
    val isSubmitting by viewModel.isSubmitting.collectAsStateWithLifecycle()

    var webPreviewCode by remember { mutableStateOf<String?>(null) }
    val webPreviewCert = remember(webPreviewCode, certificatesState) {
        val targetCode = webPreviewCode ?: return@remember null
        (certificatesState as? UiState.Success)?.data?.firstOrNull {
            it.certificateCode.equals(targetCode, ignoreCase = true)
        }
    }

    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route ?: StudentRoutes.HOME

    val breadcrumb = when {
        currentRoute == StudentRoutes.HOME -> "Portal Alumno"
        currentRoute.startsWith("student_detail") -> "Mi credencial"
        currentRoute.startsWith("student_public") -> "Vista web"
        else -> "Portal Alumno"
    }

    fun showMessage(msg: String) {
        scope.launch {
            snackbarHostState.showSnackbar(msg)
        }
    }

    LaunchedEffect(incomingDeepLink, certificatesState, profileState) {
        val raw = incomingDeepLink ?: return@LaunchedEffect
        val code = QrCodecService.extractCertificateCode(raw)
        if (code.isBlank()) {
            onConsumeDeepLink()
            return@LaunchedEffect
        }
        if (certificatesState is UiState.Loading || profileState is UiState.Loading) {
            return@LaunchedEffect
        }
        val approved = (profileState as? UiState.Success)?.data?.verificationStatus == "approved"
        val myCert = if (approved) {
            (certificatesState as? UiState.Success)?.data?.firstOrNull {
                it.certificateCode.equals(code, ignoreCase = true)
            }
        } else null

        if (myCert != null) {
            webPreviewCode = myCert.certificateCode
        } else {
            showMessage("Solo puedes abrir tus propias certificaciones aprobadas por CapacitaMaq.")
        }
        onConsumeDeepLink()
    }

    webPreviewCert?.let { targetCert ->
        WebVerificationDialog(
            certificate = targetCert,
            verifierBaseUrl = verifierBaseUrl,
            onDismiss = { webPreviewCode = null },
            onExportHtml = { cert ->
                scope.launch {
                    ExportManager.exportSingleCertificateHtml(context, cert).fold(
                        onSuccess = { uri ->
                            ExportManager.shareUri(
                                context = context,
                                uri = uri,
                                mimeType = "text/html",
                                chooserTitle = "Compartir ficha web verificable",
                                subject = "${cert.studentName} · ${cert.courseName}"
                            )
                            showMessage("Ficha HTML guardada en Descargas/Capacitamaq.")
                        },
                        onFailure = { showMessage("No se pudo exportar la vista HTML.") }
                    )
                }
            },
            onOpenHostedUrl = { url -> ExportManager.openExternalUrl(context, url) }
        )
    }

    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        containerColor = MaterialTheme.colorScheme.background,
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        topBar = {
            CapacitamaqTopBar(
                userEmail = currentUser.email,
                breadcrumb = breadcrumb,
                onSignOutClick = onSignOutClick
            )
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = StudentRoutes.HOME,
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            composable(StudentRoutes.HOME) {
                StudentPortalScreen(
                    studentEmail = currentUser.email.orEmpty(),
                    initialDisplayName = currentUser.displayName.orEmpty(),
                    profileState = profileState,
                    certificatesState = certificatesState,
                    verifierBaseUrl = verifierBaseUrl,
                    isSubmitting = isSubmitting,
                    onSubmitVerification = { fullName, rut ->
                        viewModel.submitVerificationRequest(
                            fullName = fullName,
                            rut = rut,
                            onSuccess = {
                                showMessage("Solicitud enviada a administracionmycapacitamaq@gmail.com.")
                            },
                            onError = { err -> showMessage(err) }
                        )
                    },
                    onOpenDetail = { cert ->
                        navController.navigate(StudentRoutes.detail(cert.certificateCode))
                    },
                    onOpenWebView = { cert ->
                        val webUrl = cert.webUrlOrNull(verifierBaseUrl)
                        if (webUrl != null) {
                            ExportManager.openExternalUrl(context, webUrl)
                        } else {
                            webPreviewCode = cert.certificateCode
                        }
                    },
                    onDownloadQr = { cert ->
                        scope.launch {
                            val qr = QrCodecService.generateQrBitmap(
                                content = cert.publicLink(verifierBaseUrl),
                                colorHex = cert.qrColor,
                                sizePx = 1024,
                                pattern = cert.qrPattern
                            )
                            ExportManager.saveQrPng(context, qr, cert.certificateCode).fold(
                                onSuccess = { showMessage(it) },
                                onFailure = { showMessage("No se pudo guardar el QR.") }
                            )
                        }
                    },
                    onExportPdf = { cert ->
                        scope.launch {
                            val qr = QrCodecService.generateQrBitmap(
                                content = cert.publicLink(verifierBaseUrl),
                                colorHex = cert.qrColor,
                                sizePx = 512,
                                pattern = cert.qrPattern
                            )
                            ExportManager.exportCertificatePdf(context, cert, qr, verifierBaseUrl).fold(
                                onSuccess = { uri ->
                                    ExportManager.shareUri(
                                        context = context,
                                        uri = uri,
                                        mimeType = "application/pdf",
                                        chooserTitle = "Compartir o imprimir certificado PDF",
                                        subject = cert.courseName
                                    )
                                    showMessage("Certificado PDF exportado en Descargas/Capacitamaq.")
                                },
                                onFailure = { showMessage("No se pudo exportar el PDF.") }
                            )
                        }
                    }
                )
            }

            composable(
                route = StudentRoutes.DETAIL,
                arguments = listOf(navArgument("code") { type = NavType.StringType })
            ) { backStackEntry ->
                val code = Uri.decode(backStackEntry.arguments?.getString("code").orEmpty())
                val liveCertsState by viewModel.myCertificatesState.collectAsStateWithLifecycle()
                val myCert = (liveCertsState as? UiState.Success)?.data?.firstOrNull {
                    it.certificateCode.equals(code, ignoreCase = true)
                }
                DetailScreen(
                    certificate = myCert,
                    verifierBaseUrl = verifierBaseUrl,
                    readOnly = true,
                    onBackClick = { navController.popBackStack() },
                    onOpenPublicView = { item ->
                        navController.navigate(StudentRoutes.public(item.certificateCode))
                    },
                    onOpenWebPreview = { item ->
                        val webUrl = item.webUrlOrNull(verifierBaseUrl)
                        if (webUrl != null) {
                            ExportManager.openExternalUrl(context, webUrl)
                        } else {
                            webPreviewCode = item.certificateCode
                        }
                    },
                    onEditClick = {},
                    onDownloadQr = { item ->
                        scope.launch {
                            val qr = QrCodecService.generateQrBitmap(
                                content = item.publicLink(verifierBaseUrl),
                                colorHex = item.qrColor,
                                sizePx = 1024,
                                pattern = item.qrPattern
                            )
                            ExportManager.saveQrPng(context, qr, item.certificateCode).fold(
                                onSuccess = { showMessage(it) },
                                onFailure = { showMessage("No se pudo guardar el QR.") }
                            )
                        }
                    },
                    onExportPdf = { item ->
                        scope.launch {
                            val qr = QrCodecService.generateQrBitmap(
                                content = item.publicLink(verifierBaseUrl),
                                colorHex = item.qrColor,
                                sizePx = 512,
                                pattern = item.qrPattern
                            )
                            ExportManager.exportCertificatePdf(context, item, qr, verifierBaseUrl).fold(
                                onSuccess = { uri ->
                                    ExportManager.shareUri(
                                        context = context,
                                        uri = uri,
                                        mimeType = "application/pdf",
                                        chooserTitle = "Compartir o imprimir certificado PDF",
                                        subject = item.courseName
                                    )
                                    showMessage("Certificado PDF exportado en Descargas/Capacitamaq.")
                                },
                                onFailure = { showMessage("No se pudo exportar el PDF.") }
                            )
                        }
                    },
                    onShareClick = { item ->
                        val webUrl = item.webUrlOrNull(verifierBaseUrl)
                        if (webUrl != null) {
                            ExportManager.shareLink(context, item.courseName, webUrl)
                        } else {
                            scope.launch {
                                val qr = QrCodecService.generateQrBitmap(
                                    content = item.publicLink(verifierBaseUrl),
                                    colorHex = item.qrColor,
                                    sizePx = 1024,
                                    pattern = item.qrPattern
                                )
                                ExportManager.createQrShareUri(context, qr, item.certificateCode).fold(
                                    onSuccess = { uri ->
                                        ExportManager.shareUri(
                                            context = context,
                                            uri = uri,
                                            mimeType = "image/png",
                                            chooserTitle = "Compartir código QR",
                                            subject = item.courseName
                                        )
                                    },
                                    onFailure = {
                                        ExportManager.shareLink(
                                            context,
                                            item.courseName,
                                            item.publicSummaryText(verifierBaseUrl)
                                        )
                                    }
                                )
                            }
                        }
                    },
                    onTogglePublish = {},
                    onDeleteClick = {}
                )
            }

            composable(
                route = StudentRoutes.PUBLIC,
                arguments = listOf(navArgument("code") { type = NavType.StringType })
            ) { backStackEntry ->
                val code = Uri.decode(backStackEntry.arguments?.getString("code").orEmpty())
                val liveCertsState by viewModel.myCertificatesState.collectAsStateWithLifecycle()
                val myCert = (liveCertsState as? UiState.Success)?.data?.firstOrNull {
                    it.certificateCode.equals(code, ignoreCase = true)
                }
                PublicViewScreen(
                    certificate = myCert,
                    verifierBaseUrl = verifierBaseUrl,
                    onBackClick = { navController.popBackStack() },
                    onOpenUrl = { url -> ExportManager.openExternalUrl(context, url) },
                    onOpenWhatsApp = { phone -> ExportManager.openWhatsApp(context, phone) }
                )
            }
        }
    }
}
