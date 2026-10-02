package com.example.myapplication.ui.components

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.platform.LocalContext
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat

private fun Context.activity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.activity()
    else -> null
}

/** Any granted permission is sufficient, allowing approximate location on Android 12+. */
@Composable
fun rememberPermissionAction(permissions: List<String>, explanation: String, onGranted: () -> Unit): () -> Unit {
    val context = LocalContext.current
    val currentAction by rememberUpdatedState(onGranted)
    var requested by rememberSaveable(permissions.joinToString()) { mutableStateOf(false) }
    var dialog by remember { mutableStateOf<String?>(null) }
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { result ->
        requested = true
        if (result.values.any { it }) currentAction()
        else {
            val activity = context.activity()
            dialog = if (activity != null && permissions.any { ActivityCompat.shouldShowRequestPermissionRationale(activity, it) }) "rationale" else "settings"
        }
    }
    val request = {
        requested = true
        launcher.launch(permissions.toTypedArray())
    }
    if (dialog != null) {
        val settings = dialog == "settings"
        AlertDialog(
            onDismissRequest = { dialog = null }, title = { Text(if (settings) "Permiso desactivado" else "Permiso necesario") },
            text = { Text(explanation + if (settings) " Puedes habilitarlo en los ajustes de la app y volver a intentarlo." else "") },
            confirmButton = {
                TextButton(onClick = {
                    dialog = null
                    if (settings) context.startActivity(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:" + context.packageName)))
                    else request()
                }) { Text(if (settings) "Abrir ajustes" else "Continuar") }
            }, dismissButton = { TextButton(onClick = { dialog = null }) { Text("Ahora no") } }
        )
    }
    return {
        val granted = permissions.any { ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED }
        val activity = context.activity()
        when {
            granted -> currentAction()
            activity != null && permissions.any { ActivityCompat.shouldShowRequestPermissionRationale(activity, it) } -> dialog = "rationale"
            requested -> dialog = "settings"
            else -> request()
        }
    }
}
