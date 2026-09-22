package com.example.faceid.kevin.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.faceid.kevin.components.AppCard
import com.example.faceid.kevin.components.AppHeader
import com.example.faceid.kevin.navigation.Routes
import com.example.faceid.ui.theme.FACEIDTheme

@Composable
fun HomeScreen(
    onNavigate: (String) -> Unit,
    viewModel: HomeViewModel = viewModel()
) {
    HomeContent(
        state = viewModel.uiState,
        onNavigate = onNavigate,
        onToggleBiometrics = viewModel::onToggleBiometrics
    )
}

@Composable
private fun HomeContent(
    state: HomeUiState,
    onNavigate: (String) -> Unit,
    onToggleBiometrics: (Boolean) -> Unit
) {
    Scaffold(
        topBar = { AppHeader(title = "FaceID Android") }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            AppCard(
                title = "Estado del bloqueo",
                subtitle = if (state.lockActive) "Activo" else "Inactivo"
            )
            AppCard(
                title = "Apps protegidas",
                subtitle = "${state.protectedAppsCount} aplicaciones",
                onClick = { onNavigate(Routes.APPS) }
            )
            if (state.pinConfigured) {
                AppCard(
                    title = "Cambiar PIN",
                    subtitle = "Actualiza tu código de seguridad",
                    onClick = { onNavigate(Routes.CHANGE_PIN) }
                )
            } else {
                AppCard(
                    title = "Crear PIN",
                    subtitle = "Configura un PIN de respaldo",
                    onClick = { onNavigate(Routes.CREATE_PIN) }
                )
            }
            AppCard(
                title = "Desbloqueo con rostro/huella",
                subtitle = if (state.biometricsEnabled) "Activado" else "Desactivado",
                trailing = {
                    Switch(
                        checked = state.biometricsEnabled,
                        onCheckedChange = onToggleBiometrics
                    )
                }
            )
            AppCard(
                title = "Ingresar con PIN",
                subtitle = "Prueba la pantalla de PIN",
                onClick = { onNavigate(Routes.ENTER_PIN) }
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun HomeScreenPreview() {
    FACEIDTheme {
        HomeContent(
            state = HomeUiState(),
            onNavigate = {},
            onToggleBiometrics = {}
        )
    }
}
