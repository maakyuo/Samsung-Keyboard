package com.maakyuo.samsungkeyboard

import android.content.Context
import android.media.AudioAttributes
import android.media.SoundPool
import java.io.File
import java.io.FileOutputStream
import kotlin.math.PI
import kotlin.math.exp
import kotlin.math.sin

object KeyboardSound {
    const val PREFS = "keyboard_settings"
    const val KEY_ENABLED = "sound_enabled"
    const val KEY_INDEX = "sound_index"
    private const val RATE = 22050
    private var pool: SoundPool? = null
    private val ids = IntArray(30)
    private var ready = false
    private val pending = mutableListOf<Pair<Int, Boolean>>()

    @Synchronized
    fun initialize(context: Context) {
        if (ready) return
        val attrs = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_ASSISTANCE_SONIFICATION)
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION).build()
        val player = SoundPool.Builder().setMaxStreams(12).setAudioAttributes(attrs).build()
        pool = player
        player.setOnLoadCompleteListener { _, sampleId, status ->
            if (status == 0) {
                synchronized(this) {
                    val waiting = pending.toList()
                    pending.clear()
                    waiting.forEach { (id, preview) ->
                        if (id == sampleId) player.play(id, 0.95f, 0.95f, 1, 0, 1f)
                    }
                }
            }
        }
        val dir = File(context.applicationContext.cacheDir, "keyboard_tones").apply { mkdirs() }
        for (i in 0 until 30) {
            val file = File(dir, "tone_${i + 1}.wav")
            makeWave(file, i)
            ids[i] = player.load(file.absolutePath, 1)
        }
        ready = true
    }

    fun play(context: Context) {
        val p = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        if (p.getBoolean(KEY_ENABLED, true)) playIndex(p.getInt(KEY_INDEX, 0))
    }

    fun preview(context: Context, index: Int) {
        initialize(context)
        playIndex(index)
    }

    private fun playIndex(index: Int) {
        val id = ids[index.coerceIn(0, 29)]
        if (id != 0) pool?.play(id, 0.95f, 0.95f, 1, 0, 1f)
    }

    private fun makeWave(file: File, profile: Int) {
        val family = profile % 10
        val variant = profile / 10
        val duration = when (family) {
            0 -> 28 + variant * 8
            1 -> 42 + variant * 5
            2 -> 85 + variant * 12
            3 -> 32 + variant * 6
            4 -> 110 + variant * 10
            5 -> 55 + variant * 9
            6 -> 24 + variant * 7
            7 -> 75 + variant * 11
            8 -> 48 + variant * 8
            else -> 125 + variant * 10
        }
        val samples = ShortArray(RATE * duration / 1000)
        val fundamental = 90.0 + profile * 47.0
        var seed = 0x13579B + profile * 7919
        for (i in samples.indices) {
            val t = i.toDouble() / RATE
            val progress = i.toDouble() / samples.size.coerceAtLeast(1)
            val attack = (i / (RATE * (0.0006 + variant * 0.00025))).coerceAtMost(1.0)
            val decay = exp(-progress * when (family) {
                0 -> 22.0
                1 -> 48.0
                2 -> 8.0
                3 -> 65.0
                4 -> 7.0
                5 -> 30.0
                6 -> 82.0
                7 -> 13.0
                8 -> 38.0
                else -> 5.0
            })
            seed = seed * 1103515245 + 12345
            val noise = (((seed ushr 16) and 0x7fff) / 16384.0) - 1.0
            val f1 = fundamental * (1.0 + family * 0.13)
            val f2 = f1 * (1.37 + variant * 0.21)
            val f3 = f1 * (2.11 + family * 0.07)
            val wave = when (family) {
                0 -> sin(2 * PI * f1 * t) * 0.8 + sin(2 * PI * f2 * t) * 0.2
                1 -> noise * 0.78 + sin(2 * PI * (1700 + variant * 700) * t) * 0.22
                2 -> sin(2 * PI * f1 * t) * 0.48 + sin(2 * PI * f2 * t) * 0.3 + sin(2 * PI * f3 * t) * 0.22
                3 -> sin(2 * PI * (f1 * (1.0 - progress * 0.3)) * t) * 0.9 + noise * 0.1
                4 -> sin(2 * PI * f1 * t) * 0.65 + sin(2 * PI * f3 * t) * 0.35
                5 -> sin(2 * PI * (f1 + 350 * sin(2 * PI * 8 * t)) * t) * 0.8 + noise * 0.2
                6 -> (if (sin(2 * PI * f1 * t) > 0) 1.0 else -1.0) * 0.55 + sin(2 * PI * f2 * t) * 0.45
                7 -> noise * 0.4 + sin(2 * PI * f1 * t) * 0.4 + sin(2 * PI * f3 * t) * 0.2
                8 -> sin(2 * PI * (f1 + progress * (1300 + variant * 500)) * t) * 0.8 + sin(2 * PI * f2 * t) * 0.2
                else -> {
                    val pulse = if (progress < 0.32 || progress in 0.58..0.76) 1.0 else 0.0
                    (sin(2 * PI * f1 * t) * 0.7 + noise * 0.3) * pulse
                }
            }
            val level = when (family) { 1, 7 -> 0.34; 3, 6 -> 0.48; 2, 4, 9 -> 0.72; else -> 0.58 }
            samples[i] = (wave * decay * attack * level * 32767).toInt().coerceIn(-32767, 32767).toShort()
        }
        FileOutputStream(file).use { out ->
            fun le(v: Int) { out.write(v and 255); out.write(v shr 8 and 255); out.write(v shr 16 and 255); out.write(v shr 24 and 255) }
            fun s16(v: Int) { out.write(v and 255); out.write(v shr 8 and 255) }
            out.write("RIFF".toByteArray()); le(36 + samples.size * 2)
            out.write("WAVEfmt ".toByteArray()); le(16)
            s16(1); s16(1); le(RATE); le(RATE * 2); s16(2); s16(16)
            out.write("data".toByteArray()); le(samples.size * 2)
            samples.forEach { s16(it.toInt() and 65535) }
        }
    }
}