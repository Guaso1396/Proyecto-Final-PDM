package com.example.faceid.ganan.lock

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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.faceid.diana.authentication.AuthHero
import com.example.faceid.diana.biometric.BiometricAuthenticator
import com.example.faceid.diana.biometric.BiometricResult
import com.example.faceid.diana.face.FaceCameraPreview
import com.example.faceid.ganan.manager.LockManager
import com.example.faceid.kevin.components.AppButton
import com.example.faceid.kevin.components.AppHeader
import com.example.faceid.kevin.components.AuroraBackground
import com.example.faceid.kevin.components.ErrorBanner
import com.example.faceid.kevin.components.InfoBanner
import com.example.faceid.kevin.components.LoadingIndicator
import com.example.faceid.kevin.components.PinDots
import com.example.faceid.kevin.components.PinKeypad
import com.example.faceid.ui.theme.FACEIDTheme

@Composable
fun LockScreen(
    packageName: String,
    onUnlocked: () -> Unit = {},
    onBack: () -> Unit = {},
    viewModel: LockViewModel = viewModel()
) {
    val state = viewModel.uiState
    val context = LocalContext.current

    LaunchedEffect(packageName) {
        viewModel.setTargetApp(packageName)
    }

    LaunchedEffect(state.result) {
        if (state.result is LockResult.Success) {
            if (state.packageName.isNotBlank()) {
                LockManager(context).markUnlocked(state.packageName)
            }
            onUnlocked()
        }
    }

    LaunchedEffect(state.pin) {
        if (state.pin.length == 4 && state.result !is LockResult.Loading && state.pinConfigured) {
            viewModel.verifyPin()
        }
    }

    LockContent(
        state = state,
        onDigit = { d -> if (state.pin.length < 4) viewModel.onPinChange(state.pin + d) },
        onDelete = { viewModel.onPinChange(state.pin.dropLast(1)) },
        onVerify = viewModel::verifyPin,
        onFaceDetected = viewModel::processFaceScan,
        onBiometric = {
            val activity = context as? FragmentActivity
            if (activity == null) {
                viewModel.onBiometricFailed("Actividad no compatible con biometría")
            } else {
                BiometricAuthenticator.authenticate(activity) { result ->
                    when (result) {
                        BiometricResult.Success -> viewModel.onBiometricSuccess()
                        BiometricResult.Failed ->
                            viewModel.onBiometricFailed("Huella/rostro no reconocido")
                        BiometricResult.Cancelled -> viewModel.resetResult()
                        is BiometricResult.Error ->
                            viewModel.onBiometricFailed(result.message)
                        is BiometricResult.NotAvailable ->
                            viewModel.onBiometricFailed(result.reason)
                    }
                }
            }
        },
        onBack = onBack
    )
}

@Composable
private fun LockContent(
    state: LockUiState,
    onDigit: (String) -> Unit,
    onDelete: () -> Unit,
    onVerify: () -> Unit,
    onFaceDetected: (android.graphics.Bitmap, android.graphics.Rect) -> Unit,
    onBiometric: () -> Unit,
    onBack: () -> Unit
) {
    val isError = state.result is LockResult.Error
    val isLoading = state.result is LockResult.Loading
    val isUnlocked = state.result is LockResult.Success
    AuroraBackground {
        Scaffold(
            topBar = { AppHeader(title = "App bloqueada", onBack = onBack) },
            containerColor = Color.Transparent
        ) { innerPadding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 24.dp)
                    .padding(bottom = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Spacer(modifier = Modifier.height(4.dp))
                AuthHero(
                    icon = Icons.Default.Lock,
                    title = state.appLabel.ifBlank { "App protegida" },
                    subtitle = when {
                        isUnlocked -> "Desbloqueado"
                        state.faceEnrolled ->
                            "Solo el rostro registrado puede desbloquear."
                        else -> state.packageName.ifBlank {
                            "Verifica tu identidad para continuar"
                        }
                    }
                )

                // Pastilla de la app objetivo
                if (state.appLabel.isNotBlank()) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(100.dp))
                            .background(
                                Brush.horizontalGradient(
                                    listOf(
                                        MaterialTheme.colorScheme.primary.copy(alpha = 0.18f),
                                        MaterialTheme.colorScheme.secondary.copy(alpha = 0.14f)
                                    )
                                )
                            )
                            .padding(horizontal = 16.dp, vertical = 8.dp)
                    ) {
                        Text(
                            text = "🔒  ${state.appLabel}",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }

                // Cámara en vivo + SOLO rostro registrado desbloquea.
                // El PIN/huella de respaldo sigue visible debajo por si el
                // match no llega (nunca se queda sin salida).
                val showFace = state.faceEnrolled && !isUnlocked
                if (showFace) {
                    FaceCameraPreview(
                        scanning = !isLoading && !isUnlocked,
                        onFaceDetected = onFaceDetected,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(280.dp)
                    )
                    Text(
                        text = when {
                            isLoading -> "Verificando rostro…"
                            state.faceMatches > 0 ->
                                "Rostro reconocido ${state.faceMatches}/3 · mantén la mirada"
                            else -> "Mira a la cámara · solo tu rostro registrado"
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                    if (isLoading) {
                        LoadingIndicator(modifier = Modifier.size(48.dp))
                    }
                }

                if (!state.pinConfigured && !state.faceEnrolled) {
                    InfoBanner(
                        title = "Sin PIN configurado",
                        subtitle = "Configura un PIN primero desde el inicio para desbloquear apps."
                    )
                    return@Column
                }

                if (state.pinConfigured) {
                    PinDots(length = state.pin.length, error = isError)
                }

                if (isLoading) {
                    LoadingIndicator(modifier = Modifier.size(56.dp))
                }
                if (isError) {
                    ErrorBanner(message = (state.result as LockResult.Error).message)
                }

                if (state.pinConfigured) {
                    PinKeypad(onDigit = onDigit, onDelete = onDelete, enabled = !isLoading)
                }

                if (state.canUseBiometrics) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center,
                        modifier = Modifier
                            .clip(CircleShape)
                            .clickable(onClick = onBiometric)
                            .padding(horizontal = 20.dp, vertical = 10.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Fingerprint,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(22.dp)
                        )
                        Text(
                            text = "  Usar huella / rostro",
                            style = MaterialTheme.typography.labelLarge.copy(
                                fontWeight = FontWeight.Bold
                            ),
                            color = MaterialTheme.colorScheme.primary,
                            textAlign = TextAlign.Center
                        )
                    }
                } else if (state.pinConfigured && !showFace) {
                    AppButton(
                        text = "Desbloquear",
                        onClick = onVerify,
                        enabled = state.pin.length == 4 && !isLoading
                    )
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun LockScreenPreview() {
    FACEIDTheme {
        LockContent(
            state = LockUiState(
                packageName = "com.whatsapp",
                appLabel = "WhatsApp",
                pin = "12",
                canUseBiometrics = true
            ),
            onDigit = {},
            onDelete = {},
            onVerify = {},
            onFaceDetected = { _, _ -> },
            onBiometric = {},
            onBack = {}
        )
    }
}
