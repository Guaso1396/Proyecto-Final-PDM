package com.example.faceid.kevin.home

import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.Face
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Pin
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.surfaceColorAtElevation
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.faceid.kevin.components.AppHeader
import com.example.faceid.kevin.components.AuroraBackground
import com.example.faceid.kevin.components.GradientDivider
import com.example.faceid.kevin.components.SectionHeader
import com.example.faceid.kevin.components.StatusPill
import com.example.faceid.kevin.navigation.Routes
import com.example.faceid.ui.theme.FACEIDTheme
import com.example.faceid.ui.theme.GradientHeroDark
import com.example.faceid.ui.theme.GradientIrisCyan

@Composable
fun HomeScreen(
    onNavigate: (String) -> Unit,
    viewModel: HomeViewModel = viewModel()
) {
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                viewModel.refresh()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    HomeContent(
        state = viewModel.uiState,
        onNavigate = onNavigate,
        onToggleBiometrics = viewModel::onToggleBiometrics,
        onToggleLockActive = viewModel::onToggleLockActive
    )
}

@Composable
private fun HomeContent(
    state: HomeUiState,
    onNavigate: (String) -> Unit,
    onToggleBiometrics: (Boolean) -> Unit,
    onToggleLockActive: (Boolean) -> Unit
) {
    AuroraBackground {
        Scaffold(
            topBar = { AppHeader(title = "Vault") },
            containerColor = Color.Transparent
        ) { innerPadding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp)
                    .padding(bottom = 24.dp),
                verticalArrangement = Arrangement.spacedBy(18.dp)
            ) {
                // Saludo
                Column {
                    Text(
                        text = "Hola de nuevo 👋",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "Tu bóveda está lista",
                        style = MaterialTheme.typography.displaySmall,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                }

                // HERO — estado del blindaje
                HeroCard(
                    lockActive = state.lockActive,
                    protectedCount = state.protectedAppsCount,
                    biometricsOn = state.biometricsEnabled,
                    onSeeApps = { onNavigate(Routes.APPS) }
                )

                // Accesos rápidos
                SectionHeader(
                    title = "Accesos rápidos",
                    subtitle = "Todo lo importante a un toque"
                )
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        QuickAction(
                            title = "Apps",
                            subtitle = "${state.protectedAppsCount} protegidas",
                            icon = Icons.Default.Apps,
                            modifier = Modifier.weight(1f),
                            onClick = { onNavigate(Routes.APPS) }
                        )
                        QuickAction(
                            title = if (state.pinConfigured) "Cambiar PIN" else "Crear PIN",
                            subtitle = if (state.pinConfigured) "Actualiza tu código" else "PIN de respaldo",
                            icon = Icons.Default.Pin,
                            modifier = Modifier.weight(1f),
                            onClick = {
                                onNavigate(
                                    if (state.pinConfigured) Routes.CHANGE_PIN
                                    else Routes.CREATE_PIN
                                )
                            }
                        )
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        QuickAction(
                            title = "Probar PIN",
                            subtitle = "Simula un desbloqueo",
                            icon = Icons.Default.VerifiedUser,
                            modifier = Modifier.weight(1f),
                            onClick = { onNavigate(Routes.ENTER_PIN) }
                        )
                        QuickAction(
                            title = "Bloqueo",
                            subtitle = if (state.lockActive) "Activo" else "Inactivo",
                            icon = Icons.Default.Lock,
                            modifier = Modifier.weight(1f),
                            onClick = { onNavigate(Routes.APPS) }
                        )
                    }
                }

                // Seguridad
                SectionHeader(
                    title = "Seguridad",
                    subtitle = "Capas de protección"
                )
                SecurityRow(
                    icon = Icons.Default.Fingerprint,
                    title = "Rostro / huella",
                    subtitle = if (state.biometricsEnabled) "Desbloqueo biométrico activado" else "Toca para activar la biometría",
                    checked = state.biometricsEnabled,
                    onChecked = onToggleBiometrics
                )
                SecurityRow(
                    icon = Icons.Default.Shield,
                    title = "PIN de respaldo",
                    subtitle = if (state.pinConfigured) "Configurado · 4 dígitos" else "Sin configurar",
                    checked = state.pinConfigured,
                    onChecked = null
                )
                SecurityRow(
                    icon = Icons.Default.Face,
                    title = "Registro facial",
                    subtitle = if (state.faceEnrolled) {
                        "Toca para actualizar o eliminar el rostro"
                    } else {
                        "Registra tu rostro para desbloqueo automático"
                    },
                    checked = state.faceEnrolled,
                    onChecked = null,
                    onClick = { onNavigate(Routes.FACE_ENROLL) }
                )
                SecurityRow(
                    icon = Icons.Default.Lock,
                    title = "Bloqueo de apps",
                    subtitle = when {
                        state.lockActive -> "Vigilancia activa"
                        !state.hasUsagePermission ->
                            "Otorga acceso de uso para activar el candado"
                        !state.hasOverlayPermission ->
                            "Otorga permiso de superposición para activar el candado"
                        else -> "Activa para bloquear apps protegidas"
                    },
                    checked = state.lockActive,
                    onChecked = onToggleLockActive
                )

                GradientDivider()
                Text(
                    text = "Hecho con Material 3 · Aurora Vault",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.align(Alignment.CenterHorizontally)
                )
            }
        }
    }
}

@Composable
private fun HeroCard(
    lockActive: Boolean,
    protectedCount: Int,
    biometricsOn: Boolean,
    onSeeApps: () -> Unit
) {
    val scheme = MaterialTheme.colorScheme
    val isDark = scheme.background.red < 0.3f
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(28.dp))
            .background(
                if (isDark) Brush.linearGradient(GradientHeroDark)
                else Brush.linearGradient(
                    listOf(Color(0xFF1B1440), Color(0xFF243B8B), Color(0xFF0E7490))
                )
            )
            .clickable(onClick = onSeeApps)
            .padding(22.dp)
    ) {
        // brillo decorativo
        Box(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .size(150.dp)
                .background(
                    Brush.radialGradient(
                        listOf(
                            Color(0xFF22D3EE).copy(alpha = 0.35f),
                            Color.Transparent
                        )
                    )
                )
        )
        Column {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Box(
                    modifier = Modifier
                        .size(58.dp)
                        .background(Brush.linearGradient(GradientIrisCyan), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Shield,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(30.dp)
                    )
                }
                StatusPill(
                    text = if (lockActive) "●  Blindaje activo" else "En pausa",
                    active = lockActive
                )
            }
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "$protectedCount apps protegidas",
                style = MaterialTheme.typography.headlineMedium,
                color = Color.White
            )
            Text(
                text = if (biometricsOn) "Biometría + PIN listos para defenderte"
                else "Activa la biometría para máxima protección",
                style = MaterialTheme.typography.bodyMedium,
                color = Color.White.copy(alpha = 0.72f)
            )
            Spacer(modifier = Modifier.height(16.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .background(Color.White, RoundedCornerShape(100.dp))
                        .padding(horizontal = 18.dp, vertical = 10.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Ver mis apps  →",
                        style = MaterialTheme.typography.labelLarge,
                        color = Color(0xFF1B1440)
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = "Seguro · Local · Privado",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.White.copy(alpha = 0.6f)
                )
            }
        }
    }
}

@Composable
private fun QuickAction(
    title: String,
    subtitle: String,
    icon: ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val scheme = MaterialTheme.colorScheme
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(22.dp))
            .background(scheme.surfaceColorAtElevation(2.dp))
            .clickable(onClick = onClick)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Box(
            modifier = Modifier
                .size(44.dp)
                .background(
                    Brush.linearGradient(GradientIrisCyan),
                    RoundedCornerShape(14.dp)
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(imageVector = icon, contentDescription = null, tint = Color.White)
        }
        Column {
            Text(text = title, style = MaterialTheme.typography.titleMedium)
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = scheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun SecurityRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    checked: Boolean,
    onChecked: ((Boolean) -> Unit)?,
    onClick: (() -> Unit)? = null
) {
    val scheme = MaterialTheme.colorScheme
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(scheme.surfaceColorAtElevation(2.dp))
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(46.dp)
                .background(scheme.primary.copy(alpha = 0.13f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(imageVector = icon, contentDescription = null, tint = scheme.primary)
        }
        Spacer(modifier = Modifier.width(14.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(text = title, style = MaterialTheme.typography.titleMedium)
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = scheme.onSurfaceVariant
            )
        }
        if (onChecked != null) {
            Switch(
                checked = checked,
                onCheckedChange = onChecked,
                colors = SwitchDefaults.colors(
                    checkedThumbColor = Color.White,
                    checkedTrackColor = scheme.primary,
                    checkedBorderColor = scheme.primary
                )
            )
        } else {
            Text(
                text = if (checked) "✓" else "!",
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                color = if (checked) Color(0xFF10B981) else Color(0xFFF59E0B),
                modifier = Modifier
                    .background(
                        if (checked) Color(0xFF10B981).copy(alpha = 0.14f)
                        else Color(0xFFF59E0B).copy(alpha = 0.14f),
                        CircleShape
                    )
                    .padding(horizontal = 12.dp, vertical = 6.dp)
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun HomeScreenPreview() {
    FACEIDTheme {
        HomeContent(
            state = HomeUiState(
                pinConfigured = true,
                biometricsEnabled = true,
                protectedAppsCount = 6,
                lockActive = true
            ),
            onNavigate = {},
            onToggleBiometrics = {},
            onToggleLockActive = {}
        )
    }
}
