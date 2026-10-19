package com.example

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BatteryChargingFull
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.SdStorage
import androidx.compose.material.icons.filled.Thermostat
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material.icons.filled.WifiOff
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.CardSurface
import com.example.ui.theme.CardSurfaceElevated
import com.example.ui.theme.CoralError
import com.example.ui.theme.ElectricViolet
import com.example.ui.theme.ElectricVioletLight
import com.example.ui.theme.EmeraldActive
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.TextTertiary
import kotlinx.coroutines.delay
import java.util.Locale

@Composable
fun SystemTelemetryCard(
    modifier: Modifier = Modifier,
    onExpandClick: () -> Unit
) {
    val context = LocalContext.current
    var telemetry by remember { mutableStateOf(AndroidSystemMonitor.getTelemetry(context)) }

    // Auto refresh telemetry every 3 seconds
    LaunchedEffect(Unit) {
        while (true) {
            delay(3000)
            telemetry = AndroidSystemMonitor.getTelemetry(context)
        }
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable { onExpandClick() },
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = CardSurface),
        border = androidx.compose.foundation.BorderStroke(1.dp, CardSurfaceElevated)
    ) {
        Column(modifier = Modifier.padding(10.dp)) {
            // Header: Live System Telemetry Label & Online Dot
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(6.dp)
                            .clip(CircleShape)
                            .background(if (telemetry.isOnline) EmeraldActive else CoralError)
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        text = "MAYA SYSTEM TELEMETRY",
                        style = MaterialTheme.typography.labelSmall.copy(
                            letterSpacing = 1.sp,
                            fontWeight = FontWeight.Bold
                        ),
                        color = NeonCyan
                    )
                }

                Text(
                    text = "${telemetry.deviceModel} • ${telemetry.uptimeFormatted}",
                    fontSize = 10.sp,
                    color = TextTertiary
                )
            }

            Spacer(Modifier.height(8.dp))

            // 4 Mini Gauges: Battery, RAM, Storage, Network
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Battery Box
                TelemetryMiniBox(
                    icon = Icons.Default.BatteryChargingFull,
                    iconTint = if (telemetry.batteryPct < 20) CoralError else EmeraldActive,
                    label = "BATTERY",
                    value = "${telemetry.batteryPct}%",
                    subtext = telemetry.chargingSource,
                    modifier = Modifier.weight(1f)
                )

                // RAM Box
                TelemetryMiniBox(
                    icon = Icons.Default.Memory,
                    iconTint = if (telemetry.usedRamPct > 80) CoralError else NeonCyan,
                    label = "RAM USED",
                    value = "${telemetry.usedRamPct}%",
                    subtext = "${String.format(Locale.US, "%.1f", telemetry.availRamGb)}G Free",
                    modifier = Modifier.weight(1f)
                )

                // Storage Box
                TelemetryMiniBox(
                    icon = Icons.Default.SdStorage,
                    iconTint = ElectricVioletLight,
                    label = "STORAGE",
                    value = "${telemetry.usedStoragePct}%",
                    subtext = "${String.format(Locale.US, "%.0f", telemetry.availStorageGb)}G Free",
                    modifier = Modifier.weight(1f)
                )

                // Network Box
                TelemetryMiniBox(
                    icon = if (telemetry.isOnline) Icons.Default.Wifi else Icons.Default.WifiOff,
                    iconTint = if (telemetry.isOnline) NeonCyan else CoralError,
                    label = "NETWORK",
                    value = if (telemetry.networkType.contains("Wi-Fi")) "Wi-Fi" else if (telemetry.networkType.contains("Cellular")) "4G/5G" else "Offline",
                    subtext = "${telemetry.batteryTempCelsius}°C",
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun TelemetryMiniBox(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    iconTint: Color,
    label: String,
    value: String,
    subtext: String,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(10.dp),
        color = CardSurfaceElevated
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 6.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = iconTint,
                modifier = Modifier.size(16.dp)
            )
            Spacer(Modifier.height(2.dp))
            Text(
                text = value,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = TextPrimary
            )
            Text(
                text = subtext,
                fontSize = 9.sp,
                color = TextTertiary,
                maxLines = 1
            )
        }
    }
}
