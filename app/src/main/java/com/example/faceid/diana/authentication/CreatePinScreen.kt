package com.example.faceid.diana.authentication

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Pin
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.faceid.kevin.components.AppButton
import com.example.faceid.kevin.components.AppHeader
import com.example.faceid.kevin.components.AuroraBackground
import com.example.faceid.kevin.components.ErrorBanner
import com.example.faceid.kevin.components.PinDots
import com.example.faceid.kevin.components.PinKeypad
import com.example.faceid.ui.theme.FACEIDTheme

@Composable
fun CreatePinScreen(
    onSuccess: () -> Unit = {},
    onBack: () -> Unit = {},
    viewModel: AuthenticationViewModel = viewModel()
) {
    val state = viewModel.uiState
    var step by remember { mutableIntStateOf(0) } // 0 = nuevo, 1 = confirmar

    LaunchedEffect(state.result) {
        if (state.result is AuthResult.Success) onSuccess()
    }

    CreatePinContent(
        state = state,
        step = step,
        onDigit = { d ->
            if (step == 0 && state.pin.length < 4) viewModel.onPinChange(state.pin + d)
            else if (step == 1 && state.confirmPin.length < 4) viewModel.onConfirmChange(state.confirmPin + d)
        },
        onDelete = {
            if (step == 0) viewModel.onPinChange(state.pin.dropLast(1))
            else viewModel.onConfirmChange(state.confirmPin.dropLast(1))
        },
        onNext = { step = 1 },
        onBackStep = { step = 0 },
        onCreate = viewModel::createPin,
        onBack = if (step == 1) ({ step = 0 }) else onBack
    )
}

@Composable
private fun CreatePinContent(
    state: AuthUiState,
    step: Int,
    onDigit: (String) -> Unit,
    onDelete: () -> Unit,
    onNext: () -> Unit,
    onBackStep: () -> Unit,
    onCreate: () -> Unit,
    onBack: () -> Unit
) {
    val isError = state.result is AuthResult.Error
    val isLoading = state.result is AuthResult.Loading
    val currentLen = if (step == 0) state.pin.length else state.confirmPin.length
    AuroraBackground {
        Scaffold(
            topBar = { AppHeader(title = "Crear PIN", onBack = onBack) },
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
                Spacer(modifier = Modifier.height(6.dp))
                // Paso 1/2
                Text(
                    text = "PASO ${step + 1} DE 2",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary
                )
                AuthHero(
                    icon = Icons.Default.Pin,
                    title = if (step == 0) "Crea tu PIN" else "Confírmalo",
                    subtitle = if (step == 0) "Un código de 4 dígitos como respaldo"
                    else "Repite el código para verificar"
                )
                Spacer(modifier = Modifier.height(4.dp))
                PinDots(length = currentLen, error = isError)
                if (isError) {
                    ErrorBanner(message = (state.result as AuthResult.Error).message)
                }
                // Mini resumen del primer PIN al confirmar
                if (step == 1) {
                    Text(
                        text = "← Volver a editar",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(vertical = 2.dp)
                    )
                }
                PinKeypad(onDigit = onDigit, onDelete = onDelete, enabled = !isLoading)
                if (step == 0) {
                    AppButton(
                        text = "Continuar",
                        onClick = onNext,
                        enabled = state.pin.length == 4 && !isLoading
                    )
                } else {
                    AppButton(
                        text = if (isLoading) "Guardando…" else "Guardar PIN",
                        onClick = onCreate,
                        enabled = state.pin.length == 4 && state.confirmPin.length == 4 && !isLoading
                    )
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun CreatePinPreview() {
    FACEIDTheme {
        CreatePinContent(
            state = AuthUiState(pin = "1234", confirmPin = "12"),
            step = 1,
            onDigit = {},
            onDelete = {},
            onNext = {},
            onBackStep = {},
            onCreate = {},
            onBack = {}
        )
    }
}
