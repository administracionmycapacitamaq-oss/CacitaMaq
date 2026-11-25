package com.example.auth

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.QrCode2
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.credentials.CredentialManager
import com.example.R
import com.example.data.model.AdminConfig
import com.example.ui.theme.BlueCorp
import com.example.ui.theme.GoldPrimary
import com.example.ui.theme.InvalidRedBg
import com.example.ui.theme.InvalidRedText
import com.example.ui.theme.NavyDeep

enum class AuthPortalRole {
    STUDENT,
    ADMIN
}

@Composable
fun AuthScreen(
    selectedRole: AuthPortalRole,
    onSelectRole: (AuthPortalRole) -> Unit,
    onAuthSuccess: (AuthPortalRole) -> Unit = {}
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val credentialManager = remember { CredentialManager.create(context) }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        NavyDeep,
                        BlueCorp,
                        MaterialTheme.colorScheme.background
                    )
                )
            )
            .safeDrawingPadding()
            .padding(20.dp),
        contentAlignment = Alignment.Center
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 460.dp)
                .verticalScroll(rememberScrollState()),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 10.dp)
        ) {
            Column(
                modifier = Modifier.padding(22.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier.fillMaxWidth(),
                    contentAlignment = Alignment.TopCenter
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(172.dp)
                            .clip(RoundedCornerShape(18.dp))
                            .border(
                                width = 1.5.dp,
                                color = GoldPrimary.copy(alpha = 0.7f),
                                shape = RoundedCornerShape(18.dp)
                            )
                    ) {
                        Image(
                            painter = painterResource(id = R.drawable.img_capacitamaq_hero_mining),
                            contentDescription = stringResource(R.string.cd_company_banner),
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(
                                    Brush.verticalGradient(
                                        0.0f to Color.Transparent,
                                        0.55f to Color.Transparent,
                                        1.0f to NavyDeep.copy(alpha = 0.68f)
                                    )
                                )
                        )
                    }

                    Surface(
                        modifier = Modifier
                            .padding(top = 124.dp)
                            .size(92.dp)
                            .clip(CircleShape)
                            .border(3.dp, GoldPrimary, CircleShape),
                        color = Color.White,
                        shadowElevation = 8.dp
                    ) {
                        Image(
                            painter = painterResource(id = R.drawable.img_capacitamaq_seal_1795460461997),
                            contentDescription = stringResource(R.string.cd_company_seal),
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(3.dp)
                                .clip(CircleShape),
                            contentScale = ContentScale.Fit
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = stringResource(R.string.auth_title),
                    style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.ExtraBold),
                    color = MaterialTheme.colorScheme.onSurface,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = stringResource(R.string.auth_brand_subtitle),
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                    color = MaterialTheme.colorScheme.onSurface,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(14.dp))

                // Role switcher: Alumno vs Administrador oficial
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    RoleSelectorCard(
                        title = "Soy Alumno",
                        subtitle = "Ver mis certificados",
                        icon = Icons.Default.Person,
                        selected = selectedRole == AuthPortalRole.STUDENT,
                        onClick = { onSelectRole(AuthPortalRole.STUDENT) },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("role_student_tab")
                    )
                    RoleSelectorCard(
                        title = "Administrador",
                        subtitle = "Solo cuenta oficial",
                        icon = Icons.Default.AdminPanelSettings,
                        selected = selectedRole == AuthPortalRole.ADMIN,
                        onClick = { onSelectRole(AuthPortalRole.ADMIN) },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("role_admin_tab")
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    color = if (selectedRole == AuthPortalRole.ADMIN) {
                        GoldPrimary.copy(alpha = 0.16f)
                    } else {
                        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.55f)
                    },
                    border = BorderStroke(
                        1.dp,
                        if (selectedRole == AuthPortalRole.ADMIN) GoldPrimary else MaterialTheme.colorScheme.outline
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = if (selectedRole == AuthPortalRole.ADMIN) Icons.Default.Lock else Icons.Default.VerifiedUser,
                                contentDescription = null,
                                tint = BlueCorp,
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = if (selectedRole == AuthPortalRole.ADMIN) {
                                    "Acceso exclusivo de Administrador"
                                } else {
                                    "Portal del Alumno · Comprobación por RUT"
                                },
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                        Text(
                            text = if (selectedRole == AuthPortalRole.ADMIN) {
                                "Solo ${AdminConfig.OFFICIAL_ADMIN_EMAIL} puede ingresar como administrador, emitir o editar certificados y aprobar alumnos."
                            } else {
                                "Inicia sesión con Google, registra tu Nombre Completo y RUT. Tras la comprobación de ${AdminConfig.OFFICIAL_ADMIN_EMAIL}, verás únicamente tus certificados."
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    FeatureChip(
                        icon = Icons.Default.QrCode2,
                        label = "Certificaciones",
                        modifier = Modifier.weight(1f)
                    )
                    FeatureChip(
                        icon = Icons.Default.VerifiedUser,
                        label = "Verificación",
                        modifier = Modifier.weight(1f)
                    )
                }

                if (errorMessage != null) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp),
                        color = InvalidRedBg
                    ) {
                        Text(
                            text = errorMessage.orEmpty(),
                            style = MaterialTheme.typography.bodySmall,
                            color = InvalidRedText,
                            modifier = Modifier.padding(12.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))

                Button(
                    onClick = {
                        errorMessage = null
                        isLoading = true
                        onGoogleSignInClicked(
                            context = context,
                            credentialManager = credentialManager,
                            onAuthSuccess = {
                                isLoading = false
                                onAuthSuccess(selectedRole)
                            },
                            onAuthError = { msg ->
                                isLoading = false
                                errorMessage = msg
                            },
                            scope = scope,
                            onAuthCancelled = {
                                isLoading = false
                            }
                        )
                    },
                    enabled = !isLoading,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                        .testTag("google_sign_in_button"),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (selectedRole == AuthPortalRole.ADMIN) NavyDeep else BlueCorp,
                        contentColor = Color.White
                    )
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(22.dp),
                            color = Color.White,
                            strokeWidth = 2.5.dp
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Default.AccountCircle,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = if (selectedRole == AuthPortalRole.ADMIN) {
                                "Ingresar como Administrador"
                            } else {
                                "Ingresar como Alumno con Google"
                            },
                            style = MaterialTheme.typography.labelLarge
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = stringResource(R.string.brand_tagline),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

@Composable
private fun RoleSelectorCard(
    title: String,
    subtitle: String,
    icon: ImageVector,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(14.dp),
        color = if (selected) BlueCorp.copy(alpha = 0.14f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
        border = BorderStroke(
            width = if (selected) 2.dp else 1.dp,
            color = if (selected) BlueCorp else MaterialTheme.colorScheme.outline
        )
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 10.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (selected) BlueCorp else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(22.dp)
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
private fun FeatureChip(
    icon: ImageVector,
    label: String,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(10.dp),
        color = MaterialTheme.colorScheme.primaryContainer
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 9.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}
