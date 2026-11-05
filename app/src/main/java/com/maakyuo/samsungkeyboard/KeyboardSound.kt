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

    @Synchronized
    fun initialize(context: Context) {
        if (ready) return
        val attrs = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_ASSISTANCE_SONIFICATION)
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION).build()
        val player = SoundPool.Builder().setMaxStreams(12).setAudioAttributes(attrs).build()
        pool = player
        val dir = File(context.applicationContext.cacheDir, "keyboard_tones").apply { mkdirs() }
        for (i in 0 until 30) {
            val file = File(dir, "tone_${i + 1}.wav")
            if (!file.exists() || file.length() < 100) makeWave(file, i)
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
        if (id != 0) pool?.play(id, 0.9f, 0.9f, 1, 0, 1f)
    }

    private fun makeWave(file: File, profile: Int) {
        val duration = 52 + profile % 6 * 6
        val samples = ShortArray(RATE * duration / 1000)
        val low = 120.0 + profile * 43 % 390
        val mid = 180.0 + profile * 61 % 520
        val high = 850.0 + profile * 127 % 2500
        var seed = 0x13579B + profile * 7919
        for (i in samples.indices) {
            val t = i.toDouble() / RATE
            val attack = (i / (RATE * 0.0015)).coerceAtMost(1.0)
            val decay = exp(-t * (36.0 + profile % 7 * 5.0))
            seed = seed * 1103515245 + 12345
            val noise = (((seed ushr 16) and 0x7fff) / 16384.0 - 1.0)
            val mixed = (sin(2 * PI * low * t) * 0.42 +
                sin(2 * PI * mid * t) * 0.32 +
                sin(2 * PI * high * t) * 0.12 +
                noise * if (profile % 3 == 0) 0.18 else 0.06) * decay * attack * (0.58 + profile % 5 * 0.06)
            samples[i] = (mixed.coerceIn(-1.0, 1.0) * 32767).toInt().toShort()
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