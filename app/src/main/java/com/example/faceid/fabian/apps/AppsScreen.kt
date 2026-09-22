package com.example.faceid.fabian.apps

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.faceid.fabian.model.AppInfo
import com.example.faceid.kevin.components.AppCard
import com.example.faceid.kevin.components.AppHeader
import com.example.faceid.kevin.components.LoadingIndicator
import com.example.faceid.ui.theme.FACEIDTheme

/**
 * Pantalla de Fabian (ruta Routes.APPS).
 * Integración en AppNavigation:
 *   composable(Routes.APPS) { AppsScreen(onBack = { navController.popBackStack() }) }
 */
@Composable
fun AppsScreen(
    onBack: () -> Unit = {},
    viewModel: AppsViewModel = viewModel()
) {
    AppsContent(
        state = viewModel.uiState,
        onBack = onBack,
        onQueryChange = viewModel::onQueryChange,
        onToggleProtect = viewModel::onToggleProtect,
        onRetry = viewModel::loadApps
    )
}

@Composable
private fun AppsContent(
    state: AppsUiState,
    onBack: () -> Unit,
    onQueryChange: (String) -> Unit,
    onToggleProtect: (String, Boolean) -> Unit,
    onRetry: () -> Unit
) {
    Scaffold(
        topBar = {
            AppHeader(
                title = "Apps protegidas",
                onBack = onBack,
                actions = {
                    IconButton(onClick = onRetry) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Recargar"
                        )
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            AppCard(
                title = "Protegidas",
                subtitle = "${state.protectedCount} de ${state.apps.size} aplicaciones"
            )

            OutlinedTextField(
                value = state.query,
                onValueChange = onQueryChange,
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Buscar app o paquete") },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = null
                    )
                },
                singleLine = true
            )

            when {
                state.isLoading -> LoadingIndicator()
                state.error != null -> AppCard(
                    title = "Error",
                    subtitle = state.error,
                    onClick = onRetry
                )
                state.filteredApps.isEmpty() -> AppCard(
                    title = "Sin resultados",
                    subtitle = if (state.apps.isEmpty()) {
                        "No se encontraron aplicaciones"
                    } else {
                        "Ninguna coincide con \"${state.query}\""
                    }
                )
                else -> LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(
                        items = state.filteredApps,
                        key = { it.packageName }
                    ) { app ->
                        AppItem(
                            app = app,
                            onToggleProtect = { checked ->
                                onToggleProtect(app.packageName, checked)
                            }
                        )
                    }
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun AppsScreenPreview() {
    FACEIDTheme {
        AppsContent(
            state = AppsUiState(
                isLoading = false,
                query = "",
                apps = listOf(
                    AppInfo("com.whatsapp", "WhatsApp", null, false, true),
                    AppInfo("com.android.settings", "Ajustes", null, true, false)
                )
            ),
            onBack = {},
            onQueryChange = {},
            onToggleProtect = { _, _ -> },
            onRetry = {}
        )
    }
}
