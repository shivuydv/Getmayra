package com.example

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.OpenWith
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.CardSurface
import com.example.ui.theme.ElectricViolet
import com.example.ui.theme.ElectricVioletLight
import com.example.ui.theme.EmeraldActive
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.TextPrimary
import kotlinx.coroutines.delay
import kotlin.math.roundToInt

enum class AnimeMood {
    IDLE,
    SPEAKING,
    LISTENING,
    NAKHRE,
    BLUSHING,
    HAPPY
}

/**
 * Interactive, Draggable Virtual Anime Companion for MYRA.
 * Users can touch and drag her anywhere on the screen!
 * Features live blinking, mouth lip-sync when speaking, body physics,
 * hair sway, expressive moods (Nakhre, Sweet, Blushing, Speaking).
 */
@Composable
fun MyraAnimeCompanion(
    voiceState: MyraVoiceState,
    latestSpeech: String,
    modifier: Modifier = Modifier
) {
    // Draggable position state (User can move her anywhere on screen!)
    var dragOffsetX by remember { mutableFloatStateOf(0f) }
    var dragOffsetY by remember { mutableFloatStateOf(0f) }

    // Avatar presentation size: Normal vs Mini
    var isMiniAvatar by remember { mutableStateOf(false) }

    // Interactive tap response (Cute blush & heart reaction without speech interruption)
    var isTappedReaction by remember { mutableStateOf(false) }

    LaunchedEffect(isTappedReaction) {
        if (isTappedReaction) {
            delay(2400)
            isTappedReaction = false
        }
    }

    // Blinking eye loop
    var isBlinking by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        while (true) {
            delay(2800)
            isBlinking = true
            delay(140)
            isBlinking = false
            // Occasional double blink
            if ((0..2).random() == 0) {
                delay(120)
                isBlinking = true
                delay(120)
                isBlinking = false
            }
        }
    }

    // Idle physics & animation loops
    val infiniteTransition = rememberInfiniteTransition(label = "animePhysics")

    // Breathing motion
    val breathingOffset by infiniteTransition.animateFloat(
        initialValue = -3f,
        targetValue = 3f,
        animationSpec = infiniteRepeatable(
            animation = tween(1700, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "breath"
    )

    // Hair sway
    val hairSway by infiniteTransition.animateFloat(
        initialValue = -5.5f,
        targetValue = 5.5f,
        animationSpec = infiniteRepeatable(
            animation = tween(2100, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "hair"
    )

    // Mouth animation when speaking (syllable movement)
    val mouthMover by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(150, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "mouth"
    )

    // Hand wave & gesture animation
    val handWave by infiniteTransition.animateFloat(
        initialValue = -12f,
        targetValue = 14f,
        animationSpec = infiniteRepeatable(
            animation = tween(650, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "hand"
    )

    // Determine current anime mood
    val mood = when {
        isTappedReaction -> AnimeMood.BLUSHING
        voiceState == MyraVoiceState.SPEAKING -> AnimeMood.SPEAKING
        voiceState == MyraVoiceState.LISTENING -> AnimeMood.LISTENING
        voiceState == MyraVoiceState.PROCESSING -> AnimeMood.NAKHRE
        voiceState == MyraVoiceState.ERROR -> AnimeMood.NAKHRE
        else -> AnimeMood.HAPPY
    }

    Box(
        modifier = modifier
            .offset { IntOffset(dragOffsetX.roundToInt(), dragOffsetY.roundToInt()) }
            .pointerInput(Unit) {
                detectDragGestures { change, dragAmount ->
                    change.consume()
                    dragOffsetX += dragAmount.x
                    dragOffsetY += dragAmount.y
                }
            }
            .testTag("anime_companion_avatar"),
        contentAlignment = Alignment.TopCenter
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Drag handle & Position helper pill
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0xFF0F172A).copy(alpha = 0.85f))
                    .border(1.dp, NeonCyan.copy(alpha = 0.4f), RoundedCornerShape(12.dp))
                    .padding(horizontal = 8.dp, vertical = 2.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Default.OpenWith,
                    contentDescription = "Drag to Move",
                    tint = NeonCyan,
                    modifier = Modifier.size(11.dp)
                )
                Spacer(Modifier.width(4.dp))
                Text(
                    text = "DRAG ME ANYWHERE",
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold,
                    color = NeonCyan,
                    letterSpacing = 0.6.sp
                )

                if (dragOffsetX != 0f || dragOffsetY != 0f) {
                    Spacer(Modifier.width(6.dp))
                    IconButton(
                        onClick = {
                            dragOffsetX = 0f
                            dragOffsetY = 0f
                        },
                        modifier = Modifier.size(14.dp)
                    ) {
                        Icon(
                            Icons.Default.Refresh,
                            contentDescription = "Reset Position",
                            tint = Color.White,
                            modifier = Modifier.size(11.dp)
                        )
                    }
                }
            }

            // Live Speech Bubble floating above anime girl
            if (latestSpeech.isNotBlank() && (voiceState == MyraVoiceState.SPEAKING || voiceState == MyraVoiceState.PROCESSING)) {
                Box(
                    modifier = Modifier
                        .padding(top = 4.dp, bottom = 4.dp)
                        .widthIn(max = 290.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(CardSurface.copy(alpha = 0.95f))
                        .border(1.5.dp, ElectricVioletLight.copy(alpha = 0.85f), RoundedCornerShape(16.dp))
                        .padding(horizontal = 14.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = latestSpeech,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        color = TextPrimary,
                        maxLines = 3,
                        textAlign = TextAlign.Center
                    )
                }
            } else if (isTappedReaction) {
                // Playful tap bubble
                Box(
                    modifier = Modifier
                        .padding(top = 4.dp, bottom = 4.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(Color(0xFF831843).copy(alpha = 0.9f))
                        .border(1.dp, Color(0xFFF472B6), RoundedCornerShape(14.dp))
                        .padding(horizontal = 12.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = "Aww! शरमाओ मत Jaan! 💕",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
            }

            // Anime Girl Avatar Canvas (Tappable for cute reaction, Draggable everywhere)
            val canvasWidth = if (isMiniAvatar) 110.dp else 160.dp
            val canvasHeight = if (isMiniAvatar) 150.dp else 220.dp

            Box(
                modifier = Modifier
                    .size(width = canvasWidth, height = canvasHeight)
                    .offset(y = breathingOffset.dp)
                    .clickable {
                        isTappedReaction = true
                    },
                contentAlignment = Alignment.Center
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    drawFullAnimeGirl(
                        mood = mood,
                        isBlinking = isBlinking,
                        hairSway = hairSway,
                        mouthOpenFactor = if (mood == AnimeMood.SPEAKING) mouthMover else 0f,
                        handRotation = handWave,
                        isMini = isMiniAvatar
                    )
                }
            }

            // Interactive Bottom Nametag & Size Toggle
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(14.dp))
                    .background(Color(0xFF1E1B4B).copy(alpha = 0.9f))
                    .border(1.dp, ElectricVioletLight, RoundedCornerShape(14.dp))
                    .padding(horizontal = 10.dp, vertical = 3.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(7.dp)
                        .clip(CircleShape)
                        .background(
                            when (voiceState) {
                                MyraVoiceState.SPEAKING -> NeonCyan
                                MyraVoiceState.LISTENING -> EmeraldActive
                                MyraVoiceState.PROCESSING -> ElectricViolet
                                else -> Color(0xFFF43F5E)
                            }
                        )
                )
                Spacer(Modifier.width(6.dp))
                Text(
                    text = if (mood == AnimeMood.NAKHRE) "MYRA (Nakhre Mode 🔥)" else "MYRA 💕 AI Girlfriend",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Spacer(Modifier.width(6.dp))
                // Mini/Full Size Toggle
                Text(
                    text = if (isMiniAvatar) "↗ Max" else "↘ Mini",
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold,
                    color = NeonCyan,
                    modifier = Modifier.clickable { isMiniAvatar = !isMiniAvatar }
                )
            }
        }
    }
}

/**
 * Custom Vector Drawing for High-Quality Anime Girl Character.
 * Complete with twin-tail hair physics, shiny eyes, blinking, mouth flaps,
 * stylish outfit, waving hands, and cute anime girlfriend proportions!
 */
private fun DrawScope.drawFullAnimeGirl(
    mood: AnimeMood,
    isBlinking: Boolean,
    hairSway: Float,
    mouthOpenFactor: Float,
    handRotation: Float,
    isMini: Boolean
) {
    val scale = if (isMini) 0.72f else 1.0f
    val centerX = size.width / 2f
    val headCenterY = 62f * scale
    val headRadius = 36f * scale

    // 1. Back flowing hair (Twin Tails with Gradient & Natural Sway)
    val hairDark = Color(0xFF581C87) // Deep Royal Purple
    val hairLight = Color(0xFFA855F7) // Vibrant Neon Violet
    val hairHighlight = Color(0xFFF3E8FF)

    // Left Twin Tail
    val leftTail = Path().apply {
        moveTo(centerX - 24f * scale, headCenterY - 10f * scale)
        cubicTo(
            centerX - 60f * scale + hairSway, headCenterY + 40f * scale,
            centerX - 72f * scale + hairSway * 1.6f, headCenterY + 110f * scale,
            centerX - 42f * scale + hairSway, headCenterY + 140f * scale
        )
        cubicTo(
            centerX - 36f * scale, headCenterY + 95f * scale,
            centerX - 30f * scale, headCenterY + 38f * scale,
            centerX - 20f * scale, headCenterY + 10f * scale
        )
        close()
    }
    drawPath(leftTail, brush = Brush.verticalGradient(listOf(hairDark, hairLight)))

    // Right Twin Tail
    val rightTail = Path().apply {
        moveTo(centerX + 24f * scale, headCenterY - 10f * scale)
        cubicTo(
            centerX + 60f * scale - hairSway, headCenterY + 40f * scale,
            centerX + 72f * scale - hairSway * 1.6f, headCenterY + 110f * scale,
            centerX + 42f * scale - hairSway, headCenterY + 140f * scale
        )
        cubicTo(
            centerX + 36f * scale, headCenterY + 95f * scale,
            centerX + 30f * scale, headCenterY + 38f * scale,
            centerX + 20f * scale, headCenterY + 10f * scale
        )
        close()
    }
    drawPath(rightTail, brush = Brush.verticalGradient(listOf(hairDark, hairLight)))

    // Hair Ribbons / Bows at base of twin-tails
    val ribbonColor = Color(0xFFF43F5E)
    drawCircle(ribbonColor, 5.5f * scale, Offset(centerX - 24f * scale, headCenterY - 8f * scale))
    drawCircle(ribbonColor, 5.5f * scale, Offset(centerX + 24f * scale, headCenterY - 8f * scale))

    // 2. Neck & Body (Stylish Anime Outfit & Skirt)
    val skinTone = Color(0xFFFFDFC4)
    val skinShadow = Color(0xFFF7C8A9)
    val neckWidth = 13f * scale

    // Neck
    drawRect(
        color = skinShadow,
        topLeft = Offset(centerX - neckWidth / 2f, headCenterY + 20f * scale),
        size = Size(neckWidth, 20f * scale)
    )

    val chestTop = headCenterY + 38f * scale
    val outfitMain = Color(0xFF1E293B) // Dark Navy / Cyber Blazer
    val outfitAccent = Color(0xFF38BDF8) // Cyan Trim
    val skirtColor = Color(0xFF475569) // Pleated Skirt

    // Outfit Torso / Jacket
    val dress = Path().apply {
        moveTo(centerX - 26f * scale, chestTop)
        lineTo(centerX + 26f * scale, chestTop)
        lineTo(centerX + 32f * scale, chestTop + 54f * scale)
        lineTo(centerX - 32f * scale, chestTop + 54f * scale)
        close()
    }
    drawPath(dress, outfitMain)

    // Inner White Shirt & Tie/Bow
    drawRect(
        color = Color(0xFFF8FAFC),
        topLeft = Offset(centerX - 7f * scale, chestTop),
        size = Size(14f * scale, 24f * scale)
    )

    // Red Necktie / Ribbon
    val tiePath = Path().apply {
        moveTo(centerX - 5f * scale, chestTop + 4f * scale)
        lineTo(centerX + 5f * scale, chestTop + 4f * scale)
        lineTo(centerX + 3.5f * scale, chestTop + 24f * scale)
        lineTo(centerX, chestTop + 28f * scale)
        lineTo(centerX - 3.5f * scale, chestTop + 24f * scale)
        close()
    }
    drawPath(tiePath, Color(0xFFE11D48))

    // Cute Pleated Skirt
    val skirtTop = chestTop + 54f * scale
    val skirtPath = Path().apply {
        moveTo(centerX - 32f * scale, skirtTop)
        lineTo(centerX + 32f * scale, skirtTop)
        lineTo(centerX + 44f * scale, skirtTop + 36f * scale)
        lineTo(centerX - 44f * scale, skirtTop + 36f * scale)
        close()
    }
    drawPath(skirtPath, skirtColor)
    // Skirt Pleat Highlights
    drawArc(
        color = outfitAccent,
        startAngle = 10f,
        sweepAngle = 160f,
        useCenter = false,
        topLeft = Offset(centerX - 40f * scale, skirtTop + 32f * scale),
        size = Size(80f * scale, 6f * scale),
        style = Stroke(width = 2.5f * scale)
    )

    // Legs / Thigh-High Stockings
    val legTop = skirtTop + 36f * scale
    val legW = 10f * scale
    // Left leg
    drawRect(Color(0xFF0F172A), Offset(centerX - 20f * scale, legTop), Size(legW, 40f * scale))
    // Right leg
    drawRect(Color(0xFF0F172A), Offset(centerX + 10f * scale, legTop), Size(legW, 40f * scale))
    // Cute shoes
    drawCircle(Color(0xFFE11D48), 6f * scale, Offset(centerX - 15f * scale, legTop + 38f * scale))
    drawCircle(Color(0xFFE11D48), 6f * scale, Offset(centerX + 15f * scale, legTop + 38f * scale))

    // Animated Left Arm (Resting on hip or posing sassy)
    val leftArm = Path().apply {
        moveTo(centerX - 24f * scale, chestTop + 4f * scale)
        lineTo(centerX - 38f * scale, chestTop + 28f * scale)
        lineTo(centerX - 32f * scale, chestTop + 32f * scale)
        lineTo(centerX - 18f * scale, chestTop + 14f * scale)
        close()
    }
    drawPath(leftArm, outfitMain)
    drawCircle(skinTone, 5.5f * scale, Offset(centerX - 36f * scale, chestTop + 34f * scale))

    // Animated Right Arm (Waving dynamically when speaking or greeting!)
    val waveOffset = if (mood == AnimeMood.SPEAKING || mood == AnimeMood.HAPPY || mood == AnimeMood.LISTENING) handRotation * scale else 0f
    val rightArm = Path().apply {
        moveTo(centerX + 24f * scale, chestTop + 4f * scale)
        lineTo(centerX + 40f * scale + waveOffset, chestTop + 18f * scale)
        lineTo(centerX + 34f * scale + waveOffset, chestTop + 24f * scale)
        lineTo(centerX + 18f * scale, chestTop + 14f * scale)
        close()
    }
    drawPath(rightArm, outfitMain)
    drawCircle(skinTone, 5.5f * scale, Offset(centerX + 40f * scale + waveOffset, chestTop + 18f * scale))

    // 3. Anime Head & Face Structure
    val headPath = Path().apply {
        moveTo(centerX - headRadius + 3f * scale, headCenterY - 10f * scale)
        cubicTo(
            centerX - headRadius, headCenterY + 12f * scale,
            centerX - headRadius + 10f * scale, headCenterY + 34f * scale,
            centerX, headCenterY + 38f * scale
        )
        cubicTo(
            centerX + headRadius - 10f * scale, headCenterY + 34f * scale,
            centerX + headRadius, headCenterY + 12f * scale,
            centerX + headRadius - 3f * scale, headCenterY - 10f * scale
        )
        cubicTo(
            centerX + headRadius - 3f * scale, headCenterY - 34f * scale,
            centerX - headRadius + 3f * scale, headCenterY - 34f * scale,
            centerX - headRadius + 3f * scale, headCenterY - 10f * scale
        )
        close()
    }
    drawPath(headPath, skinTone)

    // Glowing Cheeks (Blushing)
    val blushColor = if (mood == AnimeMood.NAKHRE || mood == AnimeMood.BLUSHING) {
        Color(0xFFFF3377).copy(alpha = 0.65f)
    } else {
        Color(0xFFFF85A1).copy(alpha = 0.38f)
    }
    drawCircle(blushColor, 8.5f * scale, Offset(centerX - 20f * scale, headCenterY + 14f * scale))
    drawCircle(blushColor, 8.5f * scale, Offset(centerX + 20f * scale, headCenterY + 14f * scale))

    // Cute blush marks lines
    drawLine(
        color = Color(0xFFFF1493),
        start = Offset(centerX - 24f * scale, headCenterY + 12f * scale),
        end = Offset(centerX - 16f * scale, headCenterY + 16f * scale),
        strokeWidth = 1.6f * scale
    )
    drawLine(
        color = Color(0xFFFF1493),
        start = Offset(centerX + 16f * scale, headCenterY + 12f * scale),
        end = Offset(centerX + 24f * scale, headCenterY + 16f * scale),
        strokeWidth = 1.6f * scale
    )

    // 4. Expressive Anime Eyes
    val eyeY = headCenterY + 4f * scale
    val eyeSpacing = 16f * scale

    if (isBlinking) {
        // Closed / Happy curved anime eyelids
        drawArc(
            color = Color(0xFF1E1B4B),
            startAngle = 10f,
            sweepAngle = 160f,
            useCenter = false,
            topLeft = Offset(centerX - eyeSpacing - 10f * scale, eyeY - 2f * scale),
            size = Size(20f * scale, 7.5f * scale),
            style = Stroke(width = 3.2f * scale)
        )
        drawArc(
            color = Color(0xFF1E1B4B),
            startAngle = 10f,
            sweepAngle = 160f,
            useCenter = false,
            topLeft = Offset(centerX + eyeSpacing - 10f * scale, eyeY - 2f * scale),
            size = Size(20f * scale, 7.5f * scale),
            style = Stroke(width = 3.2f * scale)
        )
    } else {
        // Sparkling anime eyes
        drawAnimeEye(centerX - eyeSpacing, eyeY, scale, isHeartPupil = (mood == AnimeMood.BLUSHING))
        drawAnimeEye(centerX + eyeSpacing, eyeY, scale, isHeartPupil = (mood == AnimeMood.BLUSHING))
    }

    // Eyebrows
    val browOffset = if (mood == AnimeMood.NAKHRE) -3.5f * scale else 0f
    drawArc(
        color = Color(0xFF4C1D95),
        startAngle = 190f,
        sweepAngle = 140f,
        useCenter = false,
        topLeft = Offset(centerX - eyeSpacing - 9f * scale, eyeY - 14f * scale + browOffset),
        size = Size(18f * scale, 5.5f * scale),
        style = Stroke(width = 2.4f * scale)
    )
    drawArc(
        color = Color(0xFF4C1D95),
        startAngle = 210f,
        sweepAngle = 140f,
        useCenter = false,
        topLeft = Offset(centerX + eyeSpacing - 9f * scale, eyeY - 14f * scale),
        size = Size(18f * scale, 5.5f * scale),
        style = Stroke(width = 2.4f * scale)
    )

    // Cute Tiny Nose
    drawCircle(Color(0xFFD4A373), 1.3f * scale, Offset(centerX, headCenterY + 14f * scale))

    // 5. Expressive Animated Mouth
    val mouthY = headCenterY + 23f * scale
    when (mood) {
        AnimeMood.SPEAKING -> {
            // Lip sync mouth opening and closing smoothly
            val mouthOpenH = (5f + mouthOpenFactor * 8f) * scale
            val mouthW = (10f + mouthOpenFactor * 4f) * scale
            val mouthPath = Path().apply {
                moveTo(centerX - mouthW / 2f, mouthY)
                cubicTo(
                    centerX - mouthW / 2f, mouthY + mouthOpenH,
                    centerX + mouthW / 2f, mouthY + mouthOpenH,
                    centerX + mouthW / 2f, mouthY
                )
                close()
            }
            drawPath(mouthPath, Color(0xFF881337))
            // Tongue inside
            drawCircle(Color(0xFFFB7185), 3f * scale, Offset(centerX, mouthY + mouthOpenH - 1.8f * scale))
        }
        AnimeMood.NAKHRE -> {
            // Cute pouting lips (Tsundere girlfriend nakhre)
            val poutPath = Path().apply {
                moveTo(centerX - 6f * scale, mouthY + 2f * scale)
                cubicTo(
                    centerX - 2f * scale, mouthY - 2.5f * scale,
                    centerX + 2f * scale, mouthY - 2.5f * scale,
                    centerX + 6f * scale, mouthY + 2f * scale
                )
            }
            drawPath(poutPath, Color(0xFFBE123C), style = Stroke(width = 2.5f * scale))
        }
        else -> {
            // Sweet welcoming girlfriend smile
            val smilePath = Path().apply {
                moveTo(centerX - 7f * scale, mouthY)
                cubicTo(
                    centerX - 2.5f * scale, mouthY + 4f * scale,
                    centerX + 2.5f * scale, mouthY + 4f * scale,
                    centerX + 7f * scale, mouthY
                )
            }
            drawPath(smilePath, Color(0xFFBE123C), style = Stroke(width = 2.4f * scale))
        }
    }

    // 6. Front Anime Bangs & Hair Gloss
    val bangs = Path().apply {
        moveTo(centerX - headRadius, headCenterY - 14f * scale)
        cubicTo(
            centerX - 25f * scale, headCenterY - 38f * scale,
            centerX + 25f * scale, headCenterY - 38f * scale,
            centerX + headRadius, headCenterY - 14f * scale
        )
        lineTo(centerX + 30f * scale, headCenterY - 2f * scale)
        lineTo(centerX + 16f * scale, headCenterY - 10f * scale)
        lineTo(centerX + 6f * scale, headCenterY + 3f * scale)
        lineTo(centerX - 6f * scale, headCenterY - 10f * scale)
        lineTo(centerX - 18f * scale, headCenterY - 1f * scale)
        lineTo(centerX - 30f * scale, headCenterY - 8f * scale)
        close()
    }
    drawPath(bangs, brush = Brush.verticalGradient(listOf(hairDark, hairLight)))

    // Glowing Hair Halo Light Reflection
    drawArc(
        brush = Brush.horizontalGradient(
            listOf(Color.Transparent, hairHighlight.copy(alpha = 0.55f), Color.Transparent)
        ),
        startAngle = 200f,
        sweepAngle = 140f,
        useCenter = false,
        topLeft = Offset(centerX - 30f * scale, headCenterY - 35f * scale),
        size = Size(60f * scale, 20f * scale),
        style = Stroke(width = 3.6f * scale)
    )

    // Cute Star Hair Clip
    val clipX = centerX + 24f * scale
    val clipY = headCenterY - 18f * scale
    drawCircle(Color(0xFF38BDF8), 4.8f * scale, Offset(clipX, clipY))
    drawCircle(Color.White, 2.2f * scale, Offset(clipX, clipY))
}

/**
 * Detailed sparkling Anime Eye with deep gradient iris and specular highlights.
 */
private fun DrawScope.drawAnimeEye(
    eyeCenterX: Float,
    eyeCenterY: Float,
    scale: Float,
    isHeartPupil: Boolean
) {
    val eyeW = 17f * scale
    val eyeH = 22f * scale

    // Eye Sclera (White)
    val eyePath = Path().apply {
        addOval(
            Rect(
                eyeCenterX - eyeW / 2f,
                eyeCenterY - eyeH / 2f,
                eyeCenterX + eyeW / 2f,
                eyeCenterY + eyeH / 2f
            )
        )
    }
    drawPath(eyePath, Color.White)

    // Iris (Cyan to Deep Indigo gradient)
    val irisRadius = 7f * scale
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(Color(0xFF38BDF8), Color(0xFF6366F1), Color(0xFF1E1B4B)),
            center = Offset(eyeCenterX, eyeCenterY + 1f * scale),
            radius = irisRadius
        ),
        radius = irisRadius,
        center = Offset(eyeCenterX, eyeCenterY + 1f * scale)
    )

    // Pupil
    if (isHeartPupil) {
        drawCircle(Color(0xFFFF2D55), 4f * scale, Offset(eyeCenterX, eyeCenterY + 1f * scale))
    } else {
        drawCircle(Color(0xFF0F172A), 3.8f * scale, Offset(eyeCenterX, eyeCenterY + 1f * scale))
    }

    // Specular Sparkles & Highlights (Anime eye shine)
    drawCircle(Color.White, 2.8f * scale, Offset(eyeCenterX - 2.2f * scale, eyeCenterY - 2.8f * scale))
    drawCircle(Color.White, 1.4f * scale, Offset(eyeCenterX + 2.2f * scale, eyeCenterY + 3.2f * scale))

    // Eyelashes
    val eyelash = Path().apply {
        moveTo(eyeCenterX - eyeW / 2f - 2f * scale, eyeCenterY - 5f * scale)
        cubicTo(
            eyeCenterX, eyeCenterY - eyeH / 2f - 2f * scale,
            eyeCenterX + eyeW / 2f, eyeCenterY - eyeH / 2f + 1f * scale,
            eyeCenterX + eyeW / 2f + 2f * scale, eyeCenterY - 5f * scale
        )
    }
    drawPath(eyelash, Color(0xFF0F172A), style = Stroke(width = 3f * scale))
}
