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
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.LinearProgressIndicator
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
import androidx.compose.ui.draw.clip
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
fun ChangePinScreen(
    onSuccess: () -> Unit = {},
    onBack: () -> Unit = {},
    viewModel: AuthenticationViewModel = viewModel()
) {
    val state = viewModel.uiState
    var step by remember { mutableIntStateOf(0) } // 0 actual, 1 nuevo, 2 confirmar

    LaunchedEffect(state.result) {
        if (state.result is AuthResult.Success) onSuccess()
    }

    fun currentValue(): String = when (step) {
        0 -> state.currentPin
        1 -> state.pin
        else -> state.confirmPin
    }

    ChangePinContent(
        state = state,
        step = step,
        onDigit = { d ->
            if (currentValue().length < 4) {
                when (step) {
                    0 -> viewModel.onCurrentPinChange(state.currentPin + d)
                    1 -> viewModel.onPinChange(state.pin + d)
                    else -> viewModel.onConfirmChange(state.confirmPin + d)
                }
            }
        },
        onDelete = {
            when (step) {
                0 -> viewModel.onCurrentPinChange(state.currentPin.dropLast(1))
                1 -> viewModel.onPinChange(state.pin.dropLast(1))
                else -> viewModel.onConfirmChange(state.confirmPin.dropLast(1))
            }
        },
        onNext = { step += 1 },
        onPrev = { if (step > 0) step -= 1 },
        onChange = viewModel::changePin,
        onBack = { if (step > 0) step -= 1 else onBack() }
    )
}

@Composable
private fun ChangePinContent(
    state: AuthUiState,
    step: Int,
    onDigit: (String) -> Unit,
    onDelete: () -> Unit,
    onNext: () -> Unit,
    onPrev: () -> Unit,
    onChange: () -> Unit,
    onBack: () -> Unit
) {
    val isError = state.result is AuthResult.Error
    val isLoading = state.result is AuthResult.Loading
    val len = when (step) {
        0 -> state.currentPin.length
        1 -> state.pin.length
        else -> state.confirmPin.length
    }
    val titles = listOf("PIN actual", "PIN nuevo", "Confirma el nuevo")
    val subtitles = listOf(
        "Primero verifica que eres tú",
        "Elige 4 dígitos distintos al anterior",
        "Repite el nuevo código"
    )
    AuroraBackground {
        Scaffold(
            topBar = { AppHeader(title = "Cambiar PIN", onBack = onBack) },
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
                Text(
                    text = "PASO ${step + 1} DE 3",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary
                )
                LinearProgressIndicator(
                    progress = { (step + 1) / 3f },
                    modifier = Modifier
                        .fillMaxSize(0.6f)
                        .height(6.dp)
                        .clip(androidx.compose.foundation.shape.RoundedCornerShape(100.dp)),
                    color = MaterialTheme.colorScheme.primary,
                    trackColor = MaterialTheme.colorScheme.surfaceVariant
                )
                AuthHero(
                    icon = Icons.Default.Refresh,
                    title = titles[step],
                    subtitle = subtitles[step]
                )
                PinDots(length = len, error = isError)
                if (isError) {
                    ErrorBanner(message = (state.result as AuthResult.Error).message)
                }
                PinKeypad(onDigit = onDigit, onDelete = onDelete, enabled = !isLoading)
                if (step < 2) {
                    AppButton(
                        text = "Continuar",
                        onClick = onNext,
                        enabled = len == 4 && !isLoading
                    )
                } else {
                    AppButton(
                        text = if (isLoading) "Cambiando…" else "Cambiar PIN",
                        onClick = onChange,
                        enabled = state.currentPin.length == 4 &&
                            state.pin.length == 4 &&
                            state.confirmPin.length == 4 && !isLoading
                    )
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun ChangePinPreview() {
    FACEIDTheme {
        ChangePinContent(
            state = AuthUiState(currentPin = "1234", pin = "5678", confirmPin = "56"),
            step = 2,
            onDigit = {},
            onDelete = {},
            onNext = {},
            onPrev = {},
            onChange = {},
            onBack = {}
        )
    }
}
