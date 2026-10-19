package com.example

import android.app.WallpaperManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RadialGradient
import android.graphics.RectF
import android.graphics.Shader
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.service.wallpaper.WallpaperService
import android.view.MotionEvent
import android.view.SurfaceHolder
import kotlin.math.cos
import kotlin.math.sin

/**
 * Maya Live Wallpaper Service.
 * "Maya herself on your home screen. She breathes, looks up when you start talking to her,
 * and reacts when you tap her. Pauses completely whenever the home screen is covered."
 */
class MayaWallpaperService : WallpaperService() {

    companion object {
        fun launchLiveWallpaperPicker(context: Context): Boolean {
            val component = ComponentName(context, MayaWallpaperService::class.java)
            val intent = Intent(WallpaperManager.ACTION_CHANGE_LIVE_WALLPAPER).apply {
                putExtra(WallpaperManager.EXTRA_LIVE_WALLPAPER_COMPONENT, component)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            return try {
                context.startActivity(intent)
                true
            } catch (e: Exception) {
                // Fallback to general live wallpaper chooser
                try {
                    val fallbackIntent = Intent(WallpaperManager.ACTION_LIVE_WALLPAPER_CHOOSER).apply {
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                    context.startActivity(fallbackIntent)
                    true
                } catch (e2: Exception) {
                    false
                }
            }
        }
    }

    override fun onCreateEngine(): Engine {
        return MayaWallpaperEngine()
    }

    inner class MayaWallpaperEngine : Engine() {

        private val handler = Handler(Looper.getMainLooper())
        private var isVisibleOnScreen = false

        private val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG)
        private val hairBackPaint = Paint(Paint.ANTI_ALIAS_FLAG)
        private val hairFrontPaint = Paint(Paint.ANTI_ALIAS_FLAG)
        private val skinPaint = Paint(Paint.ANTI_ALIAS_FLAG)
        private val eyeWhitePaint = Paint(Paint.ANTI_ALIAS_FLAG)
        private val eyeIrisPaint = Paint(Paint.ANTI_ALIAS_FLAG)
        private val eyePupilPaint = Paint(Paint.ANTI_ALIAS_FLAG)
        private val eyeHighlightPaint = Paint(Paint.ANTI_ALIAS_FLAG)
        private val mouthPaint = Paint(Paint.ANTI_ALIAS_FLAG)
        private val blushPaint = Paint(Paint.ANTI_ALIAS_FLAG)
        private val clothingPaint = Paint(Paint.ANTI_ALIAS_FLAG)
        private val particlePaint = Paint(Paint.ANTI_ALIAS_FLAG)
        private val glowPaint = Paint(Paint.ANTI_ALIAS_FLAG)
        private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG)

        // Animation timing
        private var startTime = SystemClock.elapsedRealtime()
        private var lastTapTime = 0L
        private var isReactingToTap = false
        private var tapX = 0f
        private var tapY = 0f

        // Star / Cyber particle dust
        private val particles = List(28) {
            WallpaperParticle(
                xNorm = (it * 37 % 100) / 100f,
                yNorm = (it * 61 % 100) / 100f,
                speed = 0.05f + (it % 5) * 0.02f,
                radius = 2f + (it % 4),
                color = if (it % 2 == 0) Color.argb(140, 0, 229, 255) else Color.argb(120, 157, 78, 221)
            )
        }

        private val drawRunnable = object : Runnable {
            override fun run() {
                drawFrame()
                if (isVisibleOnScreen) {
                    handler.postDelayed(this, 33) // ~30 FPS smooth & battery friendly
                }
            }
        }

        init {
            setTouchEventsEnabled(true)
        }

        override fun onVisibilityChanged(visible: Boolean) {
            super.onVisibilityChanged(visible)
            isVisibleOnScreen = visible
            if (visible) {
                startTime = SystemClock.elapsedRealtime()
                handler.post(drawRunnable)
            } else {
                // "Pauses completely whenever the home screen is covered"
                handler.removeCallbacks(drawRunnable)
            }
        }

        override fun onSurfaceDestroyed(holder: SurfaceHolder?) {
            super.onSurfaceDestroyed(holder)
            isVisibleOnScreen = false
            handler.removeCallbacks(drawRunnable)
        }

        override fun onTouchEvent(event: MotionEvent) {
            super.onTouchEvent(event)
            if (event.action == MotionEvent.ACTION_DOWN) {
                lastTapTime = SystemClock.elapsedRealtime()
                isReactingToTap = true
                tapX = event.x
                tapY = event.y
                if (isVisibleOnScreen) {
                    drawFrame()
                }
            }
        }

        private fun drawFrame() {
            val holder = surfaceHolder ?: return
            var canvas: Canvas? = null
            try {
                canvas = holder.lockCanvas()
                if (canvas != null) {
                    renderWallpaper(canvas)
                }
            } catch (e: Exception) {
                // Surface may be locked or releasing
            } finally {
                if (canvas != null) {
                    try {
                        holder.unlockCanvasAndPost(canvas)
                    } catch (e: Exception) {
                        // ignore
                    }
                }
            }
        }

        private fun renderWallpaper(canvas: Canvas) {
            val w = canvas.width.toFloat().coerceAtLeast(100f)
            val h = canvas.height.toFloat().coerceAtLeast(100f)
            val now = SystemClock.elapsedRealtime()
            val elapsedSec = (now - startTime) / 1000f

            // Reaction lasts for 1.8 seconds after a tap
            val reactionElapsed = (now - lastTapTime) / 1000f
            isReactingToTap = reactionElapsed in 0f..1.8f

            // 1. Deep Obsidian / Midnight Space Background
            val bgGradient = LinearGradient(
                0f, 0f, 0f, h,
                intArrayOf(
                    Color.rgb(8, 11, 20),
                    Color.rgb(13, 20, 36),
                    Color.rgb(5, 7, 12)
                ),
                floatArrayOf(0f, 0.5f, 1f),
                Shader.TileMode.CLAMP
            )
            bgPaint.shader = bgGradient
            canvas.drawRect(0f, 0f, w, h, bgPaint)

            // 2. Cyber Glow Aura around Maya
            val cx = w * 0.5f
            val cy = h * 0.44f
            val auraColor = if (isReactingToTap) Color.argb(70, 255, 94, 150) else Color.argb(45, 0, 229, 255)
            val glowRadius = w * 0.55f
            val auraGradient = RadialGradient(
                cx, cy, glowRadius,
                intArrayOf(auraColor, Color.TRANSPARENT),
                floatArrayOf(0f, 1f),
                Shader.TileMode.CLAMP
            )
            glowPaint.shader = auraGradient
            canvas.drawCircle(cx, cy, glowRadius, glowPaint)

            // 3. Floating Ambient Particles (Subtle stars)
            particles.forEach { p ->
                p.yNorm -= p.speed * 0.008f
                if (p.yNorm < 0f) p.yNorm = 1f
                particlePaint.color = p.color
                canvas.drawCircle(p.xNorm * w, p.yNorm * h, p.radius, particlePaint)
            }

            // 4. Character Animation Parameters
            // Natural smooth breathing cycle (~3.5 seconds)
            val breathe = (sin(elapsedSec * 1.8) * 6.0).toFloat()
            // Natural blinking cycle (~3.8s interval, 160ms blink duration)
            val blinkMod = elapsedSec % 3.8f
            val isBlinking = blinkMod in 3.64f..3.80f

            // Character Center
            val charCenterX = cx
            val charCenterY = cy + breathe

            // Hair Back (Cyber Dark Indigo/Violet)
            hairBackPaint.color = Color.rgb(28, 23, 48)
            canvas.drawOval(
                RectF(
                    charCenterX - w * 0.28f,
                    charCenterY - h * 0.17f,
                    charCenterX + w * 0.28f,
                    charCenterY + h * 0.19f
                ),
                hairBackPaint
            )

            // Neck & Shoulders / Clothing
            clothingPaint.color = Color.rgb(18, 24, 38)
            val bodyPath = Path().apply {
                moveTo(charCenterX - w * 0.24f, charCenterY + h * 0.14f)
                quadTo(charCenterX, charCenterY + h * 0.08f, charCenterX + w * 0.24f, charCenterY + h * 0.14f)
                lineTo(charCenterX + w * 0.35f, h)
                lineTo(charCenterX - w * 0.35f, h)
                close()
            }
            canvas.drawPath(bodyPath, clothingPaint)

            // Collar Accent (Neon Cyan Line)
            clothingPaint.color = Color.rgb(0, 229, 255)
            clothingPaint.style = Paint.Style.STROKE
            clothingPaint.strokeWidth = 4f
            canvas.drawLine(charCenterX - w * 0.12f, charCenterY + h * 0.11f, charCenterX, charCenterY + h * 0.14f, clothingPaint)
            canvas.drawLine(charCenterX, charCenterY + h * 0.14f, charCenterX + w * 0.12f, charCenterY + h * 0.11f, clothingPaint)
            clothingPaint.style = Paint.Style.FILL

            // Face (Soft Peachy anime skin tone)
            skinPaint.color = Color.rgb(255, 234, 224)
            val faceRadius = w * 0.22f
            canvas.drawCircle(charCenterX, charCenterY, faceRadius, skinPaint)

            // Chin curve
            val chinPath = Path().apply {
                moveTo(charCenterX - faceRadius * 0.85f, charCenterY)
                quadTo(charCenterX, charCenterY + faceRadius * 1.25f, charCenterX + faceRadius * 0.85f, charCenterY)
                close()
            }
            canvas.drawPath(chinPath, skinPaint)

            // Ears
            canvas.drawCircle(charCenterX - faceRadius * 0.95f, charCenterY + 4f, 18f, skinPaint)
            canvas.drawCircle(charCenterX + faceRadius * 0.95f, charCenterY + 4f, 18f, skinPaint)

            // Blushing Cheeks
            val blushAlpha = if (isReactingToTap) 190 else 100
            blushPaint.color = Color.argb(blushAlpha, 255, 110, 140)
            canvas.drawOval(
                RectF(charCenterX - faceRadius * 0.65f, charCenterY + 18f, charCenterX - faceRadius * 0.30f, charCenterY + 36f),
                blushPaint
            )
            canvas.drawOval(
                RectF(charCenterX + faceRadius * 0.30f, charCenterY + 18f, charCenterX + faceRadius * 0.65f, charCenterY + 36f),
                blushPaint
            )

            // Eyes / Reaction
            val eyeSpacing = faceRadius * 0.44f
            val eyeY = charCenterY - 6f
            val eyeW = faceRadius * 0.32f
            val eyeH = faceRadius * 0.40f

            if (isReactingToTap) {
                // Cute happy closed anime eyes (^  ^)
                eyeIrisPaint.color = Color.rgb(20, 20, 35)
                eyeIrisPaint.style = Paint.Style.STROKE
                eyeIrisPaint.strokeWidth = 7f
                eyeIrisPaint.strokeCap = Paint.Cap.ROUND

                val leftSmileEye = Path().apply {
                    moveTo(charCenterX - eyeSpacing - eyeW * 0.6f, eyeY)
                    quadTo(charCenterX - eyeSpacing, eyeY - 18f, charCenterX - eyeSpacing + eyeW * 0.6f, eyeY)
                }
                val rightSmileEye = Path().apply {
                    moveTo(charCenterX + eyeSpacing - eyeW * 0.6f, eyeY)
                    quadTo(charCenterX + eyeSpacing, eyeY - 18f, charCenterX + eyeSpacing + eyeW * 0.6f, eyeY)
                }
                canvas.drawPath(leftSmileEye, eyeIrisPaint)
                canvas.drawPath(rightSmileEye, eyeIrisPaint)
                eyeIrisPaint.style = Paint.Style.FILL
            } else if (isBlinking) {
                // Natural soft blink slit
                eyeIrisPaint.color = Color.rgb(30, 25, 45)
                eyeIrisPaint.style = Paint.Style.STROKE
                eyeIrisPaint.strokeWidth = 5f
                eyeIrisPaint.strokeCap = Paint.Cap.ROUND
                canvas.drawLine(charCenterX - eyeSpacing - eyeW * 0.5f, eyeY, charCenterX - eyeSpacing + eyeW * 0.5f, eyeY, eyeIrisPaint)
                canvas.drawLine(charCenterX + eyeSpacing - eyeW * 0.5f, eyeY, charCenterX + eyeSpacing + eyeW * 0.5f, eyeY, eyeIrisPaint)
                eyeIrisPaint.style = Paint.Style.FILL
            } else {
                // Open Expressive Anime Eyes (Deep Violet with vibrant Cyan highlight)
                eyeWhitePaint.color = Color.WHITE
                canvas.drawOval(RectF(charCenterX - eyeSpacing - eyeW * 0.5f, eyeY - eyeH * 0.5f, charCenterX - eyeSpacing + eyeW * 0.5f, eyeY + eyeH * 0.5f), eyeWhitePaint)
                canvas.drawOval(RectF(charCenterX + eyeSpacing - eyeW * 0.5f, eyeY - eyeH * 0.5f, charCenterX + eyeSpacing + eyeW * 0.5f, eyeY + eyeH * 0.5f), eyeWhitePaint)

                // Irises
                eyeIrisPaint.color = Color.rgb(138, 43, 226) // Electric Violet
                canvas.drawCircle(charCenterX - eyeSpacing, eyeY + 2f, eyeW * 0.44f, eyeIrisPaint)
                canvas.drawCircle(charCenterX + eyeSpacing, eyeY + 2f, eyeW * 0.44f, eyeIrisPaint)

                // Pupils
                eyePupilPaint.color = Color.rgb(20, 10, 35)
                canvas.drawCircle(charCenterX - eyeSpacing, eyeY + 3f, eyeW * 0.24f, eyePupilPaint)
                canvas.drawCircle(charCenterX + eyeSpacing, eyeY + 3f, eyeW * 0.24f, eyePupilPaint)

                // Catch-lights (Sparkling highlights)
                eyeHighlightPaint.color = Color.WHITE
                canvas.drawCircle(charCenterX - eyeSpacing - 5f, eyeY - 4f, 6f, eyeHighlightPaint)
                canvas.drawCircle(charCenterX + eyeSpacing - 5f, eyeY - 4f, 6f, eyeHighlightPaint)
                canvas.drawCircle(charCenterX - eyeSpacing + 4f, eyeY + 6f, 3f, eyeHighlightPaint)
                canvas.drawCircle(charCenterX + eyeSpacing + 4f, eyeY + 6f, 3f, eyeHighlightPaint)
            }

            // Eyebrows
            mouthPaint.color = Color.rgb(70, 50, 90)
            mouthPaint.style = Paint.Style.STROKE
            mouthPaint.strokeWidth = 3.5f
            mouthPaint.strokeCap = Paint.Cap.ROUND
            canvas.drawLine(charCenterX - eyeSpacing - 18f, eyeY - eyeH * 0.65f, charCenterX - eyeSpacing + 18f, eyeY - eyeH * 0.75f, mouthPaint)
            canvas.drawLine(charCenterX + eyeSpacing - 18f, eyeY - eyeH * 0.75f, charCenterX + eyeSpacing + 18f, eyeY - eyeH * 0.65f, mouthPaint)

            // Small cute nose
            mouthPaint.strokeWidth = 2.5f
            canvas.drawPoint(charCenterX, charCenterY + 12f, mouthPaint)

            // Mouth (Cute smile or blushing reaction)
            mouthPaint.style = Paint.Style.STROKE
            mouthPaint.color = Color.rgb(220, 90, 120)
            mouthPaint.strokeWidth = 4f
            val mouthPath = Path().apply {
                if (isReactingToTap) {
                    // Open happy laughing mouth
                    mouthPaint.style = Paint.Style.FILL
                    moveTo(charCenterX - 14f, charCenterY + 34f)
                    quadTo(charCenterX, charCenterY + 48f, charCenterX + 14f, charCenterY + 34f)
                    close()
                } else {
                    mouthPaint.style = Paint.Style.STROKE
                    moveTo(charCenterX - 12f, charCenterY + 36f)
                    quadTo(charCenterX, charCenterY + 42f, charCenterX + 12f, charCenterY + 36f)
                }
            }
            canvas.drawPath(mouthPath, mouthPaint)
            mouthPaint.style = Paint.Style.FILL

            // Hair Front / Bangs & Side Twintails (Swaying with breath)
            hairFrontPaint.color = Color.rgb(45, 36, 75) // Dark Stylish Violet
            val hairSway = (cos(elapsedSec * 1.8) * 3f).toFloat()

            // Bangs
            val bangs = Path().apply {
                moveTo(charCenterX - faceRadius, charCenterY - faceRadius * 0.4f)
                quadTo(charCenterX - faceRadius * 0.4f, charCenterY + faceRadius * 0.2f, charCenterX - faceRadius * 0.1f, charCenterY - faceRadius * 0.35f)
                quadTo(charCenterX + faceRadius * 0.2f, charCenterY + faceRadius * 0.15f, charCenterX + faceRadius * 0.5f, charCenterY - faceRadius * 0.35f)
                quadTo(charCenterX + faceRadius * 0.8f, charCenterY, charCenterX + faceRadius, charCenterY - faceRadius * 0.4f)
                quadTo(charCenterX, charCenterY - faceRadius * 1.3f, charCenterX - faceRadius, charCenterY - faceRadius * 0.4f)
                close()
            }
            canvas.drawPath(bangs, hairFrontPaint)

            // Side hair locks
            val leftLock = Path().apply {
                moveTo(charCenterX - faceRadius * 0.85f, charCenterY - faceRadius * 0.2f)
                quadTo(charCenterX - faceRadius * 1.1f + hairSway, charCenterY + faceRadius * 0.8f, charCenterX - faceRadius * 0.75f, charCenterY + faceRadius * 1.4f)
                lineTo(charCenterX - faceRadius * 0.95f, charCenterY + faceRadius * 0.5f)
                close()
            }
            val rightLock = Path().apply {
                moveTo(charCenterX + faceRadius * 0.85f, charCenterY - faceRadius * 0.2f)
                quadTo(charCenterX + faceRadius * 1.1f - hairSway, charCenterY + faceRadius * 0.8f, charCenterX + faceRadius * 0.75f, charCenterY + faceRadius * 1.4f)
                lineTo(charCenterX + faceRadius * 0.95f, charCenterY + faceRadius * 0.5f)
                close()
            }
            canvas.drawPath(leftLock, hairFrontPaint)
            canvas.drawPath(rightLock, hairFrontPaint)

            // Cyber Hairpins (Neon Cyan & Coral accents)
            hairFrontPaint.color = Color.rgb(0, 229, 255)
            canvas.drawRect(charCenterX - faceRadius * 0.7f, charCenterY - faceRadius * 0.65f, charCenterX - faceRadius * 0.45f, charCenterY - faceRadius * 0.58f, hairFrontPaint)
            hairFrontPaint.color = Color.rgb(255, 107, 107)
            canvas.drawRect(charCenterX - faceRadius * 0.7f, charCenterY - faceRadius * 0.55f, charCenterX - faceRadius * 0.45f, charCenterY - faceRadius * 0.48f, hairFrontPaint)

            // 5. If tapped / reacting: Draw glowing heart and sparkle ring!
            if (isReactingToTap) {
                val heartAlpha = ((1f - (reactionElapsed / 1.8f)) * 255).toInt().coerceIn(0, 255)
                val heartRise = reactionElapsed * 45f
                val heartX = charCenterX + faceRadius * 0.8f
                val heartY = charCenterY - faceRadius * 0.9f - heartRise

                glowPaint.color = Color.argb(heartAlpha, 255, 94, 150)
                glowPaint.style = Paint.Style.FILL

                // Heart icon
                val heartPath = Path().apply {
                    moveTo(heartX, heartY)
                    cubicTo(heartX - 16f, heartY - 18f, heartX - 28f, heartY + 8f, heartX, heartY + 24f)
                    cubicTo(heartX + 28f, heartY + 8f, heartX + 16f, heartY - 18f, heartX, heartY)
                    close()
                }
                canvas.drawPath(heartPath, glowPaint)

                // Tap ring ripple
                if (tapX > 0f && tapY > 0f) {
                    val rippleRadius = reactionElapsed * 90f
                    val rippleAlpha = ((1f - (reactionElapsed / 1.8f)) * 140).toInt().coerceIn(0, 140)
                    glowPaint.style = Paint.Style.STROKE
                    glowPaint.strokeWidth = 3f
                    glowPaint.color = Color.argb(rippleAlpha, 0, 229, 255)
                    canvas.drawCircle(tapX, tapY, rippleRadius, glowPaint)
                    glowPaint.style = Paint.Style.FILL
                }
            }

            // 6. Subtitle at bottom
            textPaint.color = Color.argb(160, 150, 175, 215)
            textPaint.textSize = 28f
            textPaint.textAlign = Paint.Align.CENTER
            val statusCaption = if (isReactingToTap) "Maya is happy! ❤️" else "Maya • Home Screen Companion"
            canvas.drawText(statusCaption, w * 0.5f, h * 0.86f, textPaint)
        }
    }
}

private data class WallpaperParticle(
    var xNorm: Float,
    var yNorm: Float,
    val speed: Float,
    val radius: Float,
    val color: Int
)
