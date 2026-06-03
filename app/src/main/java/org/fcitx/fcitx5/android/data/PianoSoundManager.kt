/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * SPDX-FileCopyrightText: Copyright 2021-2025 Fcitx5 for Android Contributors
 */
package org.fcitx.fcitx5.android.data

import android.content.Context
import android.media.AudioAttributes
import android.media.SoundPool
import org.fcitx.fcitx5.android.R
import org.fcitx.fcitx5.android.utils.appContext
import timber.log.Timber

object PianoSoundManager {

    enum class Mood(val label: String) {
        Default("默认"),
        Cheerful("欢快"),
        Gentle("舒缓"),
        Tense("紧张")
    }

    /**
     * Typatone 2019 用户输入数据中的英文字母频率顺序 (高频 → 低频):
     * e a o i t s n h r l d u y m g f c w k b p j v q z x
     *
     * 映射策略:
     * - 高频字母 (e,a,o,i,t,s,n,h,r) → 旋律音区 c4~c6
     * - 中频字母 (l,d,u,y,m,g,f,c,w) → bass 音区 c3~b3
     * - 低频字母 (k,b,p,j,v,q,z,x) → accent 音区 a-1~b2
     */
    private val charToNote: Map<Char, String> = buildMap {
        // 高频字母 → 旋律音区 (c4~c6, 15 notes)
        val highFreq = listOf('e', 'a', 'o', 'i', 't', 's', 'n', 'h', 'r')
        val highNotes = listOf("c4", "d4", "e4", "f4", "g4", "a4", "b4", "c5", "d5")
        highFreq.forEachIndexed { i, c -> put(c, highNotes[i]) }

        // 中频字母 → bass 音区 (c3~b3, 12 notes)
        val midFreq = listOf('l', 'd', 'u', 'y', 'm', 'g', 'f', 'c', 'w')
        val midNotes = listOf("c3", "d3", "e3", "f3", "g3", "a3", "b3", "c4", "d4")
        midFreq.forEachIndexed { i, c -> put(c, midNotes[i]) }

        // 低频字母 → accent 音区 (a_1~b0)
        val lowFreq = listOf('k', 'b', 'p', 'j', 'v', 'q', 'z', 'x')
        val lowNotes = listOf("a_1", "b_1", "c0", "d0", "e0", "f0", "g0", "a0")
        lowFreq.forEachIndexed { i, c -> put(c, lowNotes[i]) }
    }

    /** 音符名 → raw 资源 ID 映射 */
    private val noteToResId: Map<String, Int> = mapOf(
        "a_1" to R.raw.a_1, "b_1" to R.raw.b_1,
        "c0" to R.raw.c0, "d0" to R.raw.d0, "e0" to R.raw.e0, "f0" to R.raw.f0, "g0" to R.raw.g0,
        "a0" to R.raw.a0, "b0" to R.raw.b0,
        "c1" to R.raw.c1, "d1" to R.raw.d1, "e1" to R.raw.e1, "f1" to R.raw.f1, "g1" to R.raw.g1,
        "a1" to R.raw.a1, "b1" to R.raw.b1,
        "c2" to R.raw.c2, "d2" to R.raw.d2, "e2" to R.raw.e2, "f2" to R.raw.f2, "g2" to R.raw.g2,
        "a2" to R.raw.a2, "b2" to R.raw.b2,
        "c3" to R.raw.c3, "d3" to R.raw.d3, "e3" to R.raw.e3, "f3" to R.raw.f3, "g3" to R.raw.g3,
        "a3" to R.raw.a3, "b3" to R.raw.b3,
        "c4" to R.raw.c4, "d4" to R.raw.d4, "e4" to R.raw.e4, "f4" to R.raw.f4, "g4" to R.raw.g4,
        "a4" to R.raw.a4, "b4" to R.raw.b4,
        "c5" to R.raw.c5, "d5" to R.raw.d5, "e5" to R.raw.e5, "f5" to R.raw.f5, "g5" to R.raw.g5,
        "a5" to R.raw.a5, "b5" to R.raw.b5,
        "c6" to R.raw.c6, "d6" to R.raw.d6, "e6" to R.raw.e6, "f6" to R.raw.f6, "g6" to R.raw.g6,
        "a6" to R.raw.a6, "b6" to R.raw.b6,
        "c7" to R.raw.c7
    )

    private var soundPool: SoundPool? = null
    private var loadedSoundIds: MutableMap<Int, Int>? = null
    private var currentVolume: Float = 0.5f
    private var currentMood: Mood = Mood.Default

    @Volatile
    private var initialized = false

    fun initialize(context: Context = appContext) {
        if (initialized) return
        synchronized(this) {
            if (initialized) return
            val audioAttributes = AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_ASSISTANCE_SONIFICATION)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build()
            soundPool = SoundPool.Builder()
                .setMaxStreams(8)
                .setAudioAttributes(audioAttributes)
                .build()
            val ids = mutableMapOf<Int, Int>()
            noteToResId.values.distinct().forEach { resId ->
                val soundId = soundPool!!.load(context, resId, 1)
                ids[resId] = soundId
            }
            loadedSoundIds = ids
            initialized = true
            Timber.d("PianoSoundManager initialized with %d sounds", ids.size)
        }
    }

    fun setVolume(volumePercent: Int) {
        currentVolume = (volumePercent.coerceIn(0, 100) / 100f)
    }

    fun setMood(mood: Mood) {
        currentMood = mood
    }

    /**
     * 根据字符播放对应的钢琴音
     * @param char 按键字符
     */
    fun playPianoSound(char: Char) {
        if (!initialized) {
            Timber.w("PianoSoundManager not initialized")
            return
        }
        val lowerChar = char.lowercaseChar()
        val note = charToNote[lowerChar]
        if (note == null) {
            // 非字母字符: 用 Unicode 取模映射到全音域
            playFallbackSound(char)
            return
        }
        val resId = noteToResId[note] ?: return
        val soundId = loadedSoundIds?.get(resId) ?: return
        val volume = getMoodVolume(currentMood)
        soundPool?.play(soundId, volume, volume, 1, 0, 1f)
    }

    private fun playFallbackSound(char: Char) {
        // 非字母字符按 Unicode 取模映射
        val allNotes = noteToResId.keys.toList()
        val index = char.code % allNotes.size
        val note = allNotes[index]
        val resId = noteToResId[note] ?: return
        val soundId = loadedSoundIds?.get(resId) ?: return
        val volume = getMoodVolume(currentMood)
        soundPool?.play(soundId, volume, volume, 1, 0, 1f)
    }

    private fun getMoodVolume(mood: Mood): Float = when (mood) {
        Mood.Default -> currentVolume
        Mood.Cheerful -> (currentVolume * 1.1f).coerceAtMost(1f)
        Mood.Gentle -> (currentVolume * 0.7f)
        Mood.Tense -> (currentVolume * 0.9f)
    }

    fun release() {
        synchronized(this) {
            soundPool?.release()
            soundPool = null
            loadedSoundIds = null
            initialized = false
        }
    }
}
