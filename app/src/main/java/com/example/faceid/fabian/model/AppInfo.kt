package com.example.faceid.fabian.model

import android.graphics.drawable.Drawable

/**
 * Modelo de Fabian: representa una app instalada y su estado de protección.
 *
 * @param packageName nombre del paquete (id único).
 * @param label nombre visible para el usuario.
 * @param icon icono cargado desde PackageManager (nullable por rendimiento).
 * @param isSystemApp true si es app del sistema (FLAG_SYSTEM).
 * @param isProtected true si el usuario la marcó como protegida.
 */
data class AppInfo(
    val packageName: String,
    val label: String,
    val icon: Drawable? = null,
    val isSystemApp: Boolean = false,
    val isProtected: Boolean = false
)
