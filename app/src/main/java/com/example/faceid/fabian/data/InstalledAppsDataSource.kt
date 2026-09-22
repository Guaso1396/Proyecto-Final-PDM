package com.example.faceid.fabian.data

import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import com.example.faceid.fabian.model.AppInfo

/**
 * Fuente de datos de Fabian: lee apps instaladas con PackageManager.
 * Requiere QUERY_ALL_PACKAGES (ya declarado en el Manifest).
 * Mostrar todas: incluye apps del sistema con flag [isSystemApp].
 */
class InstalledAppsDataSource(private val context: Context) {

    fun getInstalledApps(): List<AppInfo> {
        val pm: PackageManager = context.packageManager
        val installed = pm.getInstalledApplications(PackageManager.GET_META_DATA)

        return installed.mapNotNull { appInfo ->
            try {
                val label = pm.getApplicationLabel(appInfo)?.toString()
                    ?: appInfo.packageName
                val icon = try {
                    pm.getApplicationIcon(appInfo)
                } catch (_: Exception) {
                    null
                }
                val isSystem = (appInfo.flags and ApplicationInfo.FLAG_SYSTEM) != 0 ||
                    (appInfo.flags and ApplicationInfo.FLAG_UPDATED_SYSTEM_APP) != 0

                AppInfo(
                    packageName = appInfo.packageName,
                    label = label,
                    icon = icon,
                    isSystemApp = isSystem,
                    isProtected = false
                )
            } catch (_: Exception) {
                null
            }
        }.sortedWith(
            // Usuario primero, luego sistema; alfabético por label.
            compareBy<AppInfo> { it.isSystemApp }.thenBy { it.label.lowercase() }
        )
    }
}
