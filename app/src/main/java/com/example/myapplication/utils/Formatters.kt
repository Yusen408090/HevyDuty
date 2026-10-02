package com.example.myapplication.utils

import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

fun Double.display(): String = if (this % 1.0 == 0.0) toLong().toString() else String.format(Locale.US, "%.1f", this)
fun String.decimal(): Double? = replace(',', '.').toDoubleOrNull()?.takeIf { it.isFinite() }
fun dateLabel(time: Long): String = Instant.ofEpochMilli(time).atZone(ZoneId.systemDefault())
    .format(DateTimeFormatter.ofPattern("d MMM yyyy", Locale.forLanguageTag("es")))
fun shortDate(time: Long): String = Instant.ofEpochMilli(time).atZone(ZoneId.systemDefault())
    .format(DateTimeFormatter.ofPattern("d MMM", Locale.forLanguageTag("es")))
fun clockLabel(seconds: Long): String = "%02d:%02d".format(seconds.coerceAtLeast(0) / 60, seconds.coerceAtLeast(0) % 60)
