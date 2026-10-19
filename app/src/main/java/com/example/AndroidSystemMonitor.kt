package com.example

import android.app.ActivityManager
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.BatteryManager
import android.os.Build
import android.os.Environment
import android.os.StatFs
import android.os.SystemClock
import java.io.File
import java.util.Locale

data class DeviceTelemetry(
    val batteryPct: Int,
    val isCharging: Boolean,
    val chargingSource: String,
    val batteryTempCelsius: Float,
    val batteryHealth: String,
    val totalRamGb: Float,
    val availRamGb: Float,
    val usedRamPct: Int,
    val isLowRam: Boolean,
    val totalStorageGb: Float,
    val availStorageGb: Float,
    val usedStoragePct: Int,
    val networkType: String,
    val isOnline: Boolean,
    val deviceModel: String,
    val androidVersion: String,
    val uptimeFormatted: String
)

object AndroidSystemMonitor {

    fun getTelemetry(context: Context): DeviceTelemetry {
        // 1. Battery Telemetry
        val batteryFilter = IntentFilter(Intent.ACTION_BATTERY_CHANGED)
        val batteryStatus = context.registerReceiver(null, batteryFilter)
        val level = batteryStatus?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: -1
        val scale = batteryStatus?.getIntExtra(BatteryManager.EXTRA_SCALE, -1) ?: -1
        val status = batteryStatus?.getIntExtra(BatteryManager.EXTRA_STATUS, -1) ?: -1
        val plugged = batteryStatus?.getIntExtra(BatteryManager.EXTRA_PLUGGED, -1) ?: -1
        val tempRaw = batteryStatus?.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, -1) ?: -1
        val healthRaw = batteryStatus?.getIntExtra(BatteryManager.EXTRA_HEALTH, -1) ?: -1

        val batteryPct = if (level >= 0 && scale > 0) ((level.toFloat() / scale.toFloat()) * 100).toInt() else 50
        val isCharging = status == BatteryManager.BATTERY_STATUS_CHARGING || status == BatteryManager.BATTERY_STATUS_FULL
        val chargingSource = when (plugged) {
            BatteryManager.BATTERY_PLUGGED_AC -> "AC Power"
            BatteryManager.BATTERY_PLUGGED_USB -> "USB Charging"
            BatteryManager.BATTERY_PLUGGED_WIRELESS -> "Wireless"
            else -> if (isCharging) "Charging" else "On Battery"
        }
        val tempCelsius = if (tempRaw > 0) tempRaw / 10.0f else 28.5f
        val batteryHealth = when (healthRaw) {
            BatteryManager.BATTERY_HEALTH_GOOD -> "Good (उत्तम)"
            BatteryManager.BATTERY_HEALTH_OVERHEAT -> "Overheat (गरम)"
            BatteryManager.BATTERY_HEALTH_DEAD -> "Dead"
            BatteryManager.BATTERY_HEALTH_OVER_VOLTAGE -> "Over Voltage"
            else -> "Normal"
        }

        // 2. RAM Telemetry
        val actManager = context.getSystemService(Context.ACTIVITY_SERVICE) as? ActivityManager
        val memInfo = ActivityManager.MemoryInfo()
        actManager?.getMemoryInfo(memInfo)
        val totalRamBytes = memInfo.totalMem.toDouble()
        val availRamBytes = memInfo.availMem.toDouble()
        val totalRamGb = (totalRamBytes / (1024.0 * 1024.0 * 1024.0)).toFloat()
        val availRamGb = (availRamBytes / (1024.0 * 1024.0 * 1024.0)).toFloat()
        val usedRamPct = if (totalRamBytes > 0) (((totalRamBytes - availRamBytes) / totalRamBytes) * 100).toInt() else 0

        // 3. Storage Telemetry
        val stat = StatFs(Environment.getDataDirectory().path)
        val totalStorageBytes = stat.blockCountLong * stat.blockSizeLong
        val availStorageBytes = stat.availableBlocksLong * stat.blockSizeLong
        val totalStorageGb = (totalStorageBytes / (1024.0 * 1024.0 * 1024.0)).toFloat()
        val availStorageGb = (availStorageBytes / (1024.0 * 1024.0 * 1024.0)).toFloat()
        val usedStoragePct = if (totalStorageBytes > 0) (((totalStorageBytes - availStorageBytes).toDouble() / totalStorageBytes.toDouble()) * 100).toInt() else 0

        // 4. Network Telemetry
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
        val network = cm?.activeNetwork
        val caps = cm?.getNetworkCapabilities(network)
        val isOnline = caps?.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) == true
        val networkType = when {
            caps?.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) == true -> "Wi-Fi High Speed"
            caps?.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) == true -> "Cellular 4G/5G"
            caps?.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) == true -> "Ethernet"
            isOnline -> "Online"
            else -> "Offline"
        }

        // 5. Device Info & Uptime
        val deviceModel = "${Build.MANUFACTURER.replaceFirstChar { it.uppercase() }} ${Build.MODEL}"
        val androidVersion = "Android ${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT})"
        val uptimeMs = SystemClock.elapsedRealtime()
        val hours = (uptimeMs / (1000 * 60 * 60)).toInt()
        val minutes = ((uptimeMs / (1000 * 60)) % 60).toInt()
        val uptimeFormatted = "${hours}h ${minutes}m"

        return DeviceTelemetry(
            batteryPct = batteryPct,
            isCharging = isCharging,
            chargingSource = chargingSource,
            batteryTempCelsius = tempCelsius,
            batteryHealth = batteryHealth,
            totalRamGb = totalRamGb,
            availRamGb = availRamGb,
            usedRamPct = usedRamPct,
            isLowRam = memInfo.lowMemory,
            totalStorageGb = totalStorageGb,
            availStorageGb = availStorageGb,
            usedStoragePct = usedStoragePct,
            networkType = networkType,
            isOnline = isOnline,
            deviceModel = deviceModel,
            androidVersion = androidVersion,
            uptimeFormatted = uptimeFormatted
        )
    }

    fun getVoiceSummary(context: Context, languageCode: String): String {
        val t = getTelemetry(context)
        val isHi = languageCode.startsWith("hi")
        return if (isHi) {
            "फोन की बैटरी ${t.batteryPct}% है और ${t.chargingSource} है। रैम ${t.usedRamPct}% उपयोग में है (${String.format(Locale.US, "%.1f", t.availRamGb)} GB खाली)। इंटरनेट स्थिति: ${t.networkType}।"
        } else {
            "Battery is at ${t.batteryPct}% (${t.chargingSource}). RAM usage is at ${t.usedRamPct}% with ${String.format(Locale.US, "%.1f", t.availRamGb)} GB free. Network status: ${t.networkType}."
        }
    }
}
