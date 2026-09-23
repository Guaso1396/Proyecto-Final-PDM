package com.example.faceid.diana.face

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Face
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.faceid.diana.authentication.AuthHero
import com.example.faceid.kevin.components.AppButton

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FaceEnrollmentScreen(
    onEnrollmentComplete: () -> Unit,
    onBack: () -> Unit,
    viewModel: FaceViewModel = viewModel()
) {
    val state = viewModel.uiState

    // Relee si hay rostro cada vez que se abre la pantalla.
    LaunchedEffect(Unit) {
        viewModel.checkEnrollment()
    }

    // Fuente de verdad: store, no solo uiState (evita perderse el menú).
    // Se recalcula en cada recomposición (uiState cambia al abrir/borrar).
    val hasStoredFace = state.enrolled || viewModel.hasStoredFace()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Registro Facial") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "Volver"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent
                )
            )
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when {
                // Con rostro guardado SIEMPRE se ven Actualizar / Eliminar.
                hasStoredFace || state.enrolled || state.done -> {
                    ManageEnrolledFaceContent(
                        justRegistered = state.done,
                        onDelete = { viewModel.clearEnrollment() },
                        onUpdate = { viewModel.updateEnrollment() },
                        onContinue = onEnrollmentComplete
                    )
                }
                else -> {
                    FaceScanContent(
                        statusText = state.status,
                        errorText = state.error,
                        scanning = state.scanning,
                        onFaceDetected = { bitmap, faceBox ->
                            viewModel.onFaceFrame(bitmap, faceBox)
                        },
                        onRequestCamera = {
                            viewModel.resetError()
                        },
                        onRetry = {
                            viewModel.resetError()
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun EnrollmentSuccessContent(
    onContinue: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.weight(1f))

        AuthHero(
            icon = Icons.Default.CheckCircle,
            title = "¡Registro Exitoso!",
            subtitle = "Tu rostro ha sido guardado de forma segura en este dispositivo"
        )

        Spacer(modifier = Modifier.weight(1f))

        AppButton(
            text = "Continuar",
            onClick = onContinue,
            enabled = true
        )

        Spacer(modifier = Modifier.height(16.dp))
    }
}

/**
 * Rostro ya en la bóveda: permite eliminarlo o actualizarlo por uno nuevo
 * sin perder el resto de la configuración.
 */
@Composable
private fun ManageEnrolledFaceContent(
    justRegistered: Boolean,
    onDelete: () -> Unit,
    onUpdate: () -> Unit,
    onContinue: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(24.dp))

        AuthHero(
            icon = if (justRegistered) Icons.Default.CheckCircle else Icons.Default.Face,
            title = if (justRegistered) "¡Registro Exitoso!" else "Rostro registrado",
            subtitle = if (justRegistered) {
                "Tu rostro está listo. Puedes actualizarlo o eliminarlo cuando quieras"
            } else {
                "Puedes eliminarlo o actualizarlo por uno nuevo"
            }
        )

        Spacer(modifier = Modifier.height(32.dp))

        AppButton(
            text = "Actualizar rostro",
            onClick = onUpdate,
            enabled = true
        )

        Spacer(modifier = Modifier.height(12.dp))

        AppButton(
            text = "Eliminar rostro",
            onClick = onDelete,
            enabled = true,
            isPrimary = false
        )

        Spacer(modifier = Modifier.weight(1f))

        AppButton(
            text = "Continuar",
            onClick = onContinue,
            enabled = true,
            isPrimary = false
        )

        Spacer(modifier = Modifier.height(8.dp))
    }
}
