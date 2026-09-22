package com.example.faceid.diana.authentication

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.faceid.diana.biometric.BiometricAuthenticator
import com.example.faceid.diana.biometric.BiometricResult
import com.example.faceid.kevin.components.AppButton
import com.example.faceid.kevin.components.AppHeader
import com.example.faceid.kevin.components.AuroraBackground
import com.example.faceid.kevin.components.ErrorBanner
import com.example.faceid.kevin.components.LoadingIndicator
import com.example.faceid.kevin.components.PinDots
import com.example.faceid.kevin.components.PinKeypad
import com.example.faceid.ui.theme.FACEIDTheme

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
    // Auto-verificar al completar 4 dígitos
    LaunchedEffect(state.pin) {
        if (state.pin.length == 4 && state.result !is AuthResult.Loading) {
            viewModel.verifyPin()
        }
    }

    EnterPinContent(
        state = state,
        onDigit = { d -> if (state.pin.length < 4) viewModel.onPinChange(state.pin + d) },
        onDelete = { viewModel.onPinChange(state.pin.dropLast(1)) },
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
    onDigit: (String) -> Unit,
    onDelete: () -> Unit,
    onVerify: () -> Unit,
    onBiometric: () -> Unit,
    onBack: () -> Unit
) {
    val isError = state.result is AuthResult.Error
    val isLoading = state.result is AuthResult.Loading
    AuroraBackground {
        Scaffold(
            topBar = { AppHeader(title = "Desbloquear", onBack = onBack) },
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
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Spacer(modifier = Modifier.height(6.dp))
                AuthHero(
                    icon = Icons.Default.LockOpen,
                    title = "Bienvenido de vuelta",
                    subtitle = "Ingresa tu PIN o usa tu biometría"
                )
                Spacer(modifier = Modifier.height(6.dp))
                PinDots(length = state.pin.length, error = isError)
                if (isLoading) {
                    LoadingIndicator(modifier = Modifier.size(72.dp))
                }
                if (isError) {
                    ErrorBanner(message = (state.result as AuthResult.Error).message)
                }
                Spacer(modifier = Modifier.height(4.dp))
                PinKeypad(
                    onDigit = onDigit,
                    onDelete = onDelete,
                    enabled = !isLoading
                )
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
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                } else {
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
private fun EnterPinPreview() {
    FACEIDTheme {
        EnterPinContent(
            state = AuthUiState(pin = "12", canUseBiometrics = true),
            onDigit = {},
            onDelete = {},
            onVerify = {},
            onBiometric = {},
            onBack = {}
        )
    }
}
