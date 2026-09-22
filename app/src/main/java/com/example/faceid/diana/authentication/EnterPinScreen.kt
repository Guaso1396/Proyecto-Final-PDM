package com.example.faceid.diana.authentication

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
import com.example.faceid.kevin.components.AppButton
import com.example.faceid.kevin.components.AppCard
import com.example.faceid.kevin.components.AppHeader
import com.example.faceid.kevin.components.LoadingIndicator
import com.example.faceid.ui.theme.FACEIDTheme

/**
 * Pantalla de Diana (ruta Routes.ENTER_PIN).
 * Integración en AppNavigation:
 *   composable(Routes.ENTER_PIN) {
 *       EnterPinScreen(onSuccess = { navController.popBackStack() },
 *           onBack = { navController.popBackStack() })
 *   }
 */
@Composable
fun EnterPinScreen(
    onSuccess: () -> Unit = {},
    onBack: () -> Unit = {},
    viewModel: AuthenticationViewModel = viewModel()
) {
    val state = viewModel.uiState
    val context = LocalContext.current

    LaunchedEffect(state.result) {
        if (state.result is AuthResult.Success) onSuccess()
    }

    EnterPinContent(
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
private fun EnterPinContent(
    state: AuthUiState,
    onPinChange: (String) -> Unit,
    onVerify: () -> Unit,
    onBiometric: () -> Unit,
    onBack: () -> Unit
) {
    Scaffold(
        topBar = { AppHeader(title = "Ingresar PIN", onBack = onBack) }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            AppCard(
                title = "Desbloqueo con PIN",
                subtitle = "Ingresa tu código de 4 dígitos"
            )
            OutlinedTextField(
                value = state.pin,
                onValueChange = onPinChange,
                modifier = Modifier.fillMaxWidth(),
                label = { Text("PIN") },
                visualTransformation = PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                singleLine = true
            )
            if (state.result is AuthResult.Loading) {
                LoadingIndicator()
            }
            if (state.result is AuthResult.Error) {
                AppCard(
                    title = "Error",
                    subtitle = (state.result as AuthResult.Error).message
                )
            }
            AppButton(
                text = "Desbloquear",
                onClick = onVerify,
                enabled = state.pin.length == 4 && state.result !is AuthResult.Loading
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
private fun EnterPinPreview() {
    FACEIDTheme {
        EnterPinContent(
            state = AuthUiState(pin = "12", canUseBiometrics = true),
            onPinChange = {},
            onVerify = {},
            onBiometric = {},
            onBack = {}
        )
    }
}
