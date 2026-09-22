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
 * Pantalla de Diana (ruta Routes.CREATE_PIN).
 * Integración en AppNavigation:
 *   composable(Routes.CREATE_PIN) {
 *       CreatePinScreen(onSuccess = { navController.popBackStack() },
 *           onBack = { navController.popBackStack() })
 *   }
 */
@Composable
fun CreatePinScreen(
    onSuccess: () -> Unit = {},
    onBack: () -> Unit = {},
    viewModel: AuthenticationViewModel = viewModel()
) {
    val state = viewModel.uiState

    LaunchedEffect(state.result) {
        if (state.result is AuthResult.Success) onSuccess()
    }

    CreatePinContent(
        state = state,
        onPinChange = viewModel::onPinChange,
        onConfirmChange = viewModel::onConfirmChange,
        onCreate = viewModel::createPin,
        onBack = onBack
    )
}

@Composable
private fun CreatePinContent(
    state: AuthUiState,
    onPinChange: (String) -> Unit,
    onConfirmChange: (String) -> Unit,
    onCreate: () -> Unit,
    onBack: () -> Unit
) {
    Scaffold(
        topBar = { AppHeader(title = "Crear PIN", onBack = onBack) }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            AppCard(
                title = "PIN de respaldo",
                subtitle = "Código de 4 dígitos para cuando falle la biometría"
            )
            OutlinedTextField(
                value = state.pin,
                onValueChange = onPinChange,
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Nuevo PIN") },
                visualTransformation = PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                singleLine = true
            )
            OutlinedTextField(
                value = state.confirmPin,
                onValueChange = onConfirmChange,
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Confirmar PIN") },
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
                text = "Guardar PIN",
                onClick = onCreate,
                enabled = state.pin.length == 4 &&
                    state.confirmPin.length == 4 &&
                    state.result !is AuthResult.Loading
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun CreatePinPreview() {
    FACEIDTheme {
        CreatePinContent(
            state = AuthUiState(pin = "1234", confirmPin = "123"),
            onPinChange = {},
            onConfirmChange = {},
            onCreate = {},
            onBack = {}
        )
    }
}
