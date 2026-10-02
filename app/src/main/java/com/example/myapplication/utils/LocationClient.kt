package com.example.myapplication.utils

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withTimeout

class LocationClient(private val context: Context) {
    private val client = LocationServices.getFusedLocationProviderClient(context)
    fun hasPermission(): Boolean = listOf(Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.ACCESS_FINE_LOCATION)
        .any { ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED }

    @Suppress("MissingPermission")
    suspend fun current(): Pair<Double, Double> {
        check(hasPermission()) { "Permite el acceso a ubicación para guardar tu gimnasio." }
        val precise = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
        val token = CancellationTokenSource()
        try {
            val location = withTimeout(20_000) {
                client.getCurrentLocation(
                    if (precise) Priority.PRIORITY_HIGH_ACCURACY else Priority.PRIORITY_BALANCED_POWER_ACCURACY,
                    token.token
                ).await()
            } ?: error("No se obtuvo ubicación. Activa la ubicación del dispositivo y vuelve a intentar.")
            return location.latitude to location.longitude
        } finally {
            token.cancel()
        }
    }
}
