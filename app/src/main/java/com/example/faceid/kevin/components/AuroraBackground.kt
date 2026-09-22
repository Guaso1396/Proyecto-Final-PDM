package com.example.faceid.kevin.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

/**
 * Fondo "aurora": base del theme + dos orbes de color suaves arriba.
 * Da profundidad sin imágenes y funciona en light/dark.
 */
@Composable
fun AuroraBackground(
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit
) {
    val scheme = MaterialTheme.colorScheme
    val isDark = scheme.background.red < 0.3f
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(scheme.background)
    ) {
        // Orbe superior iris
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.radialGradient(
                        colors = if (isDark) {
                            listOf(
                                Color(0xFF6D5CFF).copy(alpha = 0.34f),
                                Color.Transparent
                            )
                        } else {
                            listOf(
                                Color(0xFF6D5CFF).copy(alpha = 0.16f),
                                Color.Transparent
                            )
                        },
                        radius = 900f
                    )
                )
        )
        // Orbe cian lateral
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.radialGradient(
                        colors = if (isDark) {
                            listOf(
                                Color(0xFF22D3EE).copy(alpha = 0.20f),
                                Color.Transparent
                            )
                        } else {
                            listOf(
                                Color(0xFF22D3EE).copy(alpha = 0.12f),
                                Color.Transparent
                            )
                        },
                        center = androidx.compose.ui.geometry.Offset(900f, 240f),
                        radius = 800f
                    )
                )
        )
        content()
    }
}
