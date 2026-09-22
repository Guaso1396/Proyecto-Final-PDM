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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.faceid.kevin.components.AppButton
import com.example.faceid.kevin.components.AppCard
import com.example.faceid.kevin.components.AppHeader
import com.example.faceid.kevin.components.LoadingIndicator
import com.example.faceid.ui.theme.FACEIDTheme

/**
 * Pantalla de Diana (ruta Routes.CHANGE_PIN).
 * Integración en AppNavigation:
 *   composable(Routes.CHANGE_PIN) {
 *       ChangePinScreen(onSuccess = { navController.popBackStack() },
 *           onBack = { navController.popBackStack() })
 *   }
 */
@Composable
fun ChangePinScreen(
    onSuccess: () -> Unit = {},
    onBack: () -> Unit = {},
    viewModel: AuthenticationViewModel = viewModel()
) {
    val state = viewModel.uiState

    LaunchedEffect(state.result) {
        if (state.result is AuthResult.Success) onSuccess()
    }

    ChangePinContent(
        state = state,
        onCurrentChange = viewModel::onCurrentPinChange,
        onPinChange = viewModel::onPinChange,
        onConfirmChange = viewModel::onConfirmChange,
        onChange = viewModel::changePin,
        onBack = onBack
    )
}

@Composable
private fun ChangePinContent(
    state: AuthUiState,
    onCurrentChange: (String) -> Unit,
    onPinChange: (String) -> Unit,
    onConfirmChange: (String) -> Unit,
    onChange: () -> Unit,
    onBack: () -> Unit
) {
    Scaffold(
        topBar = { AppHeader(title = "Cambiar PIN", onBack = onBack) }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            AppCard(
                title = "Actualiza tu código",
                subtitle = "Ingresa el actual y el nuevo de 4 dígitos"
            )
            OutlinedTextField(
                value = state.currentPin,
                onValueChange = onCurrentChange,
                modifier = Modifier.fillMaxWidth(),
                label = { Text("PIN actual") },
                visualTransformation = PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                singleLine = true
            )
            OutlinedTextField(
                value = state.pin,
                onValueChange = onPinChange,
                modifier = Modifier.fillMaxWidth(),
                label = { Text("PIN nuevo") },
                visualTransformation = PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                singleLine = true
            )
            OutlinedTextField(
                value = state.confirmPin,
                onValueChange = onConfirmChange,
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Confirmar PIN nuevo") },
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
                text = "Cambiar PIN",
                onClick = onChange,
                enabled = state.currentPin.length == 4 &&
                    state.pin.length == 4 &&
                    state.confirmPin.length == 4 &&
                    state.result !is AuthResult.Loading
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun ChangePinPreview() {
    FACEIDTheme {
        ChangePinContent(
            state = AuthUiState(currentPin = "1234", pin = "5678", confirmPin = "5678"),
            onCurrentChange = {},
            onPinChange = {},
            onConfirmChange = {},
            onChange = {},
            onBack = {}
        )
    }
}
