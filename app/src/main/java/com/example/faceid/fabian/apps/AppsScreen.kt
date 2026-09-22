package com.example.faceid.fabian.apps

import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.faceid.fabian.model.AppInfo
import com.example.faceid.kevin.components.AppHeader
import com.example.faceid.kevin.components.AuroraBackground
import com.example.faceid.kevin.components.ErrorBanner
import com.example.faceid.kevin.components.LoadingIndicator
import com.example.faceid.ui.theme.FACEIDTheme
import com.example.faceid.ui.theme.GradientIrisCyan

/**
 * Pantalla de Fabian (ruta Routes.APPS).
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
    AuroraBackground {
        Scaffold(
            topBar = {
                AppHeader(
                    title = "Mis apps",
                    onBack = onBack,
                    actions = {
                        IconButton(onClick = onRetry) {
                            Box(
                                modifier = Modifier
                                    .size(38.dp)
                                    .background(
                                        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f),
                                        CircleShape
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Refresh,
                                    contentDescription = "Recargar",
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }
                )
            },
            containerColor = Color.Transparent
        ) { innerPadding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(horizontal = 20.dp)
                    .padding(bottom = 16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Banner de conteo con gradiente
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(24.dp))
                        .background(Brush.linearGradient(GradientIrisCyan))
                        .padding(18.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(52.dp)
                                .background(Color.White.copy(alpha = 0.22f), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Shield,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(28.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(14.dp))
                        Column {
                            Text(
                                text = "${state.protectedCount} protegidas",
                                style = MaterialTheme.typography.headlineSmall,
                                color = Color.White
                            )
                            Text(
                                text = "de ${state.apps.size} aplicaciones instaladas",
                                style = MaterialTheme.typography.bodyMedium,
                                color = Color.White.copy(alpha = 0.85f)
                            )
                        }
                    }
                }

                OutlinedTextField(
                    value = state.query,
                    onValueChange = onQueryChange,
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("Buscar app o paquete…") },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = null
                        )
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(18.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outline,
                        focusedContainerColor = MaterialTheme.colorScheme.surface,
                        unfocusedContainerColor = MaterialTheme.colorScheme.surface
                    )
                )

                when {
                    state.isLoading -> LoadingIndicator()
                    state.error != null -> ErrorBanner(message = state.error)
                    state.filteredApps.isEmpty() -> {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(
                                    MaterialTheme.colorScheme.surface,
                                    RoundedCornerShape(20.dp)
                                )
                                .padding(24.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = "🔍",
                                    style = MaterialTheme.typography.displaySmall
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = "Sin resultados",
                                    style = MaterialTheme.typography.titleMedium
                                )
                                Text(
                                    text = if (state.apps.isEmpty()) "No se encontraron aplicaciones"
                                    else "Ninguna coincide con \"${state.query}\"",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                    else -> {
                        Text(
                            text = "${state.filteredApps.size} aplicaciones",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
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
