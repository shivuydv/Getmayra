package com.example

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.concurrent.thread
import kotlin.math.PI
import kotlin.math.sin

/**
 * Emergency Siren synthesizer for Maya.
 * Generates an authentic rising and falling emergency siren sound.
 */
object MayaSirenPlayer {

    private val isPlaying = AtomicBoolean(false)
    private var audioTrack: AudioTrack? = null

    fun isSirenActive(): Boolean = isPlaying.get()

    fun startSiren(): Boolean {
        if (isPlaying.getAndSet(true)) return true

        thread(name = "MayaSirenThread") {
            val sampleRate = 44100
            val bufferSize = AudioTrack.getMinBufferSize(
                sampleRate,
                AudioFormat.CHANNEL_OUT_MONO,
                AudioFormat.ENCODING_PCM_16BIT
            ).coerceAtLeast(sampleRate / 4)

            try {
                val track = AudioTrack.Builder()
                    .setAudioAttributes(
                        AudioAttributes.Builder()
                            .setUsage(AudioAttributes.USAGE_ALARM)
                            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                            .build()
                    )
                    .setAudioFormat(
                        AudioFormat.Builder()
                            .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                            .setSampleRate(sampleRate)
                            .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                            .build()
                    )
                    .setBufferSizeInBytes(bufferSize)
                    .setTransferMode(AudioTrack.MODE_STREAM)
                    .build()

                audioTrack = track
                track.play()

                val buffer = ShortArray(bufferSize)
                var phase = 0.0
                var time = 0.0

                while (isPlaying.get()) {
                    for (i in buffer.indices) {
                        // Modulate frequency between 600Hz and 1100Hz smoothly (2.0s cycle)
                        val freq = 850.0 + 250.0 * sin(2.0 * PI * 0.8 * time)
                        phase += 2.0 * PI * freq / sampleRate
                        if (phase > 2.0 * PI) phase -= 2.0 * PI
                        buffer[i] = (sin(phase) * 28000).toInt().toShort()
                        time += 1.0 / sampleRate
                    }
                    track.write(buffer, 0, buffer.size)
                }

                track.stop()
                track.release()
                audioTrack = null
            } catch (e: Exception) {
                isPlaying.set(false)
            }
        }
        return true
    }

    fun stopSiren(): Boolean {
        isPlaying.set(false)
        try {
            audioTrack?.stop()
            audioTrack?.release()
            audioTrack = null
        } catch (e: Exception) {
            // ignore
        }
        return true
    }
}
