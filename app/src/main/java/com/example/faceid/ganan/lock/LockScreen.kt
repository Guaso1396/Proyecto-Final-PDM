package com.example.faceid.ganan.lock

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.faceid.diana.biometric.BiometricAuthenticator
import com.example.faceid.diana.biometric.BiometricResult
import com.example.faceid.ganan.manager.LockManager
import com.example.faceid.kevin.components.AppButton
import com.example.faceid.kevin.components.AppCard
import com.example.faceid.kevin.components.AppHeader
import com.example.faceid.kevin.components.LoadingIndicator
import com.example.faceid.ui.theme.FACEIDTheme

/**
 * Pantalla de bloqueo de Ganan (ruta `Routes.LOCK_WITH_APP` de Kevin).
 *
 * Integración en `kevin/navigation/AppNavigation.kt` (la aplica Kevin,
 * aquí solo se deja lista la API):
 * ```
 * composable(Routes.LOCK_WITH_APP, ...) { backStackEntry ->
 *     val pkg = backStackEntry.arguments?.getString("packageName").orEmpty()
 *     LockScreen(
 *         packageName = pkg,
 *         onUnlocked = { navController.popBackStack() },
 *         onBack = { navController.popBackStack() }
 *     )
 * }
 * ```
 */
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

    LockContent(
        state = state,
        onPinChange = viewModel::onPinChange,
        onVerify = viewModel::verifyPin,
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
    onPinChange: (String) -> Unit,
    onVerify: () -> Unit,
    onBiometric: () -> Unit,
    onBack: () -> Unit
) {
    Scaffold(
        topBar = { AppHeader(title = "App bloqueada", onBack = onBack) }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            AppCard(
                title = state.appLabel.ifBlank { "App protegida" },
                subtitle = state.packageName.ifBlank { "Verifica tu identidad" }
            )
            if (!state.pinConfigured) {
                AppCard(
                    title = "Sin PIN",
                    subtitle = "Configura un PIN primero desde el inicio"
                )
                return@Column
            }
            OutlinedTextField(
                value = state.pin,
                onValueChange = onPinChange,
                modifier = Modifier.fillMaxWidth(),
                label = { Text("PIN") },
                visualTransformation = PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                singleLine = true
            )
            if (state.result is LockResult.Loading) {
                LoadingIndicator()
            }
            if (state.result is LockResult.Error) {
                AppCard(
                    title = "Error",
                    subtitle = (state.result as LockResult.Error).message
                )
            }
            AppButton(
                text = "Desbloquear",
                onClick = onVerify,
                enabled = state.pin.length == 4 && state.result !is LockResult.Loading
            )
            if (state.canUseBiometrics) {
                AppButton(
                    text = "Usar huella / rostro",
                    onClick = onBiometric,
                    isPrimary = false
                )
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
            onPinChange = {},
            onVerify = {},
            onBiometric = {},
            onBack = {}
        )
    }
}

