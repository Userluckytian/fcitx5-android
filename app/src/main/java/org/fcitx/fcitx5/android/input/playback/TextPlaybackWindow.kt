/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * SPDX-FileCopyrightText: Copyright 2025 Fcitx5 for Android Contributors
 */
package org.fcitx.fcitx5.android.input.playback

import android.view.Gravity
import android.view.View
import android.widget.EditText
import android.widget.TextView
import androidx.core.view.setPadding
import androidx.lifecycle.lifecycleScope
import androidx.transition.Slide
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import org.fcitx.fcitx5.android.R
import org.fcitx.fcitx5.android.data.PianoSoundManager
import org.fcitx.fcitx5.android.input.FcitxInputMethodService
import org.fcitx.fcitx5.android.input.dependency.context
import org.fcitx.fcitx5.android.input.dependency.inputMethodService
import org.fcitx.fcitx5.android.input.dependency.theme
import org.fcitx.fcitx5.android.input.wm.InputWindow
import splitties.dimensions.dp
import splitties.views.backgroundColor
import splitties.views.dsl.core.add
import splitties.views.dsl.core.editText
import splitties.views.dsl.core.lParams
import splitties.views.dsl.core.matchParent
import splitties.views.dsl.core.scrollView
import splitties.views.dsl.core.textView
import splitties.views.dsl.core.verticalLayout
import splitties.views.dsl.core.wrapContent
import splitties.views.gravityHorizontalCenter
import splitties.views.textAppearance
import splitties.views.textColor

class TextPlaybackWindow : InputWindow.ExtendedInputWindow<TextPlaybackWindow>() {

    private val context by manager.context()
    private val theme by manager.theme()
    private val service: FcitxInputMethodService by manager.inputMethodService()

    override val title: String
        get() = context.getString(R.string.text_playback)

    override val showTitle: Boolean = true

    override fun enterAnimation(lastWindow: InputWindow) = Slide().apply {
        slideEdge = Gravity.BOTTOM
    }

    private lateinit var textInput: EditText
    private lateinit var playButton: TextView
    private lateinit var statusText: TextView
    private var playbackJob: Job? = null
    private var isPlaying = false

    override fun onCreateView(): View {
        return scrollView {
            addView(verticalLayout {
                gravity = gravityHorizontalCenter

                textInput = editText {
                    hint = "粘贴文本到此处播放..."
                    setTextColor(theme.keyTextColor)
                    setHintTextColor(theme.altKeyTextColor)
                    setBackgroundColor(theme.keyBackgroundColor)
                    setPadding(dp(12), dp(12), dp(12), dp(12))
                    minLines = 4
                    gravity = Gravity.TOP
                }
                addView(textInput, lParams(matchParent, wrapContent) {
                    horizontalMargin = dp(16)
                    topMargin = dp(8)
                })

                playButton = textView {
                    text = "▶ 播放"
                    textAppearance = android.R.style.TextAppearance_Large
                    textColor = theme.accentKeyTextColor
                    gravity = Gravity.CENTER
                    setPadding(dp(12), dp(12), dp(12), dp(12))
                    setBackgroundColor(theme.accentKeyBackgroundColor)
                    setOnClickListener { togglePlayback() }
                }
                addView(playButton, lParams(matchParent, wrapContent) {
                    horizontalMargin = dp(16)
                    topMargin = dp(12)
                })

                statusText = textView {
                    text = "输入文本后点击播放，每个字符将演奏对应钢琴音"
                    textAppearance = android.R.style.TextAppearance_Small
                    textColor = theme.keyTextColor
                    gravity = Gravity.CENTER
                }
                addView(statusText, lParams(matchParent, wrapContent) {
                    horizontalMargin = dp(16)
                    topMargin = dp(12)
                    bottomMargin = dp(16)
                })
            }, lParams(matchParent, wrapContent))
        }
    }

    private fun togglePlayback() {
        if (isPlaying) {
            stopPlayback()
        } else {
            startPlayback()
        }
    }

    private fun startPlayback() {
        val text = textInput.text.toString()
        if (text.isBlank()) {
            statusText.text = "⚠ 请先输入文本"
            return
        }

        isPlaying = true
        playButton.text = "⏸ 停止"
        statusText.text = "🎵 正在播放..."

        playbackJob = service.lifecycleScope.launch {
            text.forEach { char ->
                if (!isActive) return@launch
                PianoSoundManager.playPianoSound(char)
                delay(150) // 每个音符间隔 150ms
            }
            if (isActive) {
                isPlaying = false
                playButton.text = "▶ 播放"
                statusText.text = "✅ 播放完成"
            }
        }
    }

    private fun stopPlayback() {
        playbackJob?.cancel()
        playbackJob = null
        isPlaying = false
        playButton.text = "▶ 播放"
        statusText.text = "⏹ 已停止"
    }
}
