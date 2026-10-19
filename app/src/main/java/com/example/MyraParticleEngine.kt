package com.example

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
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
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.CardSurfaceElevated
import com.example.ui.theme.CoralError
import com.example.ui.theme.ElectricViolet
import com.example.ui.theme.ElectricVioletLight
import com.example.ui.theme.EmeraldActive
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import kotlin.math.cos
import kotlin.math.sin

data class CyberParticle(
    val id: Int,
    val initialAngle: Float,
    val distanceRatio: Float,
    val size: Float,
    val speedFactor: Float,
    val color: Color
)

@Composable
fun MayaOrbWidget(
    voiceState: MyraVoiceState,
    rmsLevel: Float,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "hudTransition")

    // Continuous rotation for outer rings
    val ringRotation1 by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(12000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "rot1"
    )

    val ringRotation2 by infiniteTransition.animateFloat(
        initialValue = 360f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(8000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "rot2"
    )

    val corePulse by infiniteTransition.animateFloat(
        initialValue = 0.94f,
        targetValue = 1.06f,
        animationSpec = infiniteRepeatable(
            animation = tween(1400, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "corePulse"
    )

    val particleDrift by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 6.28318f, // 2 * PI
        animationSpec = infiniteRepeatable(
            animation = tween(5000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "particleDrift"
    )

    // Dynamic scale based on mic RMS level
    val smoothedRms by animateFloatAsState(
        targetValue = if (voiceState == MyraVoiceState.LISTENING) (1f + (rmsLevel / 18f).coerceIn(0f, 0.45f)) else 1f,
        label = "rmsScale"
    )

    // Dynamic state colors (Maya HUD aesthetic)
    val primaryColor by animateColorAsState(
        targetValue = when (voiceState) {
            MyraVoiceState.LISTENING -> EmeraldActive
            MyraVoiceState.SPEAKING -> NeonCyan
            MyraVoiceState.PROCESSING -> ElectricVioletLight
            MyraVoiceState.CONNECTING -> NeonCyan
            MyraVoiceState.ERROR -> CoralError
            MyraVoiceState.IDLE -> NeonCyan
        },
        label = "primaryColor"
    )

    val secondaryColor by animateColorAsState(
        targetValue = when (voiceState) {
            MyraVoiceState.LISTENING -> NeonCyan
            MyraVoiceState.SPEAKING -> ElectricViolet
            MyraVoiceState.PROCESSING -> NeonCyan
            MyraVoiceState.CONNECTING -> ElectricViolet
            MyraVoiceState.ERROR -> Color(0xFFEF4444)
            MyraVoiceState.IDLE -> ElectricViolet
        },
        label = "secColor"
    )

    // Seeded cyber particles
    val particles = remember {
        List(32) { i ->
            val angle = (i * (360f / 32)) * (Math.PI.toFloat() / 180f)
            val dist = 0.55f + ((i % 5) * 0.08f)
            val size = 2.0f + ((i % 4) * 1.2f)
            val speed = 0.8f + ((i % 3) * 0.5f)
            val color = if (i % 2 == 0) NeonCyan else ElectricVioletLight
            CyberParticle(i, angle, dist, size, speed, color)
        }
    }

    Box(
        modifier = modifier.size(190.dp),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val center = Offset(size.width / 2f, size.height / 2f)
            val maxRadius = size.minDimension / 2f

            // 1. Draw Floating Orbital Cyber Particles
            for (p in particles) {
                val currentAngle = p.initialAngle + (particleDrift * p.speedFactor)
                val r = (maxRadius * p.distanceRatio) * smoothedRms
                val px = center.x + (r * cos(currentAngle))
                val py = center.y + (r * sin(currentAngle))
                val alpha = (0.35f + (0.5f * sin(currentAngle))).coerceIn(0.15f, 0.95f)

                drawCircle(
                    color = p.color.copy(alpha = alpha),
                    radius = p.size,
                    center = Offset(px, py)
                )
            }

            // 2. Outer Segmented Tech Ring 1 (Rotating Clockwise)
            drawSegmentedTechRing(
                center = center,
                radius = maxRadius * 0.92f * smoothedRms,
                rotationDegrees = ringRotation1,
                segments = 12,
                color = primaryColor.copy(alpha = 0.45f),
                strokeWidth = 2.dp.toPx()
            )

            // 3. Counter-Rotating Inner Tech Ring 2
            drawSegmentedTechRing(
                center = center,
                radius = maxRadius * 0.78f,
                rotationDegrees = ringRotation2,
                segments = 8,
                color = secondaryColor.copy(alpha = 0.55f),
                strokeWidth = 1.5.dp.toPx()
            )

            // 4. Glowing Audio-Reactive Ambient Halo
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        primaryColor.copy(alpha = 0.35f),
                        secondaryColor.copy(alpha = 0.12f),
                        Color.Transparent
                    ),
                    center = center,
                    radius = maxRadius * 0.75f * corePulse * smoothedRms
                ),
                radius = maxRadius * 0.75f * corePulse * smoothedRms,
                center = center
            )

            // 5. Solid Glowing AI Energy Core (Maya Holographic Core)
            val coreRadius = maxRadius * 0.45f * corePulse * smoothedRms
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        Color.White,
                        primaryColor,
                        secondaryColor,
                        Color(0xFF030712)
                    ),
                    center = center,
                    radius = coreRadius
                ),
                radius = coreRadius,
                center = center
            )

            // 6. High-Tech Crosshair / HUD Reticle in center
            val reticleLen = 8.dp.toPx()
            drawLine(
                color = Color.White.copy(alpha = 0.8f),
                start = Offset(center.x - reticleLen, center.y),
                end = Offset(center.x + reticleLen, center.y),
                strokeWidth = 1.5.dp.toPx()
            )
            drawLine(
                color = Color.White.copy(alpha = 0.8f),
                start = Offset(center.x, center.y - reticleLen),
                end = Offset(center.x, center.y + reticleLen),
                strokeWidth = 1.5.dp.toPx()
            )
        }
    }
}

private fun DrawScope.drawSegmentedTechRing(
    center: Offset,
    radius: Float,
    rotationDegrees: Float,
    segments: Int,
    color: Color,
    strokeWidth: Float
) {
    val totalArc = 360f / segments
    val gap = 12f
    val sweep = totalArc - gap

    for (i in 0 until segments) {
        val startAngle = (i * totalArc) + rotationDegrees
        drawArc(
            color = color,
            startAngle = startAngle,
            sweepAngle = sweep,
            useCenter = false,
            topLeft = Offset(center.x - radius, center.y - radius),
            size = androidx.compose.ui.geometry.Size(radius * 2, radius * 2),
            style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
        )
    }
}

@Composable
fun MayaMicVisualizer(
    rmsLevel: Float,
    isActive: Boolean,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "barAnim")
    val waveOffset by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 6.28318f,
        animationSpec = infiniteRepeatable(
            animation = tween(1100, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "wave"
    )

    val barCount = 18
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(28.dp)
            .padding(horizontal = 24.dp),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically
    ) {
        for (i in 0 until barCount) {
            val phase = (i.toFloat() / barCount) * Math.PI.toFloat() * 2f
            val baseWave = (sin(phase + waveOffset) + 1f) / 2f // 0 to 1

            val dynamicHeightPct = if (isActive) {
                val rmsFactor = (rmsLevel / 12f).coerceIn(0.15f, 1f)
                (0.2f + (baseWave * 0.75f * rmsFactor)).coerceIn(0.12f, 1f)
            } else {
                (0.15f + (baseWave * 0.15f)).coerceIn(0.1f, 0.3f)
            }

            Box(
                modifier = Modifier
                    .width(3.5.dp)
                    .height((28.dp * dynamicHeightPct).coerceAtLeast(3.dp))
                    .padding(vertical = 1.dp)
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val brush = Brush.verticalGradient(
                        colors = listOf(
                            NeonCyan,
                            ElectricVioletLight
                        )
                    )
                    drawRoundRect(
                        brush = brush,
                        cornerRadius = androidx.compose.ui.geometry.CornerRadius(4f, 4f)
                    )
                }
            }
        }
    }
}
