/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * SPDX-FileCopyrightText: Copyright 2025 Fcitx5 for Android Contributors
 */
package org.fcitx.fcitx5.android.input.bar.ui.settings

import android.content.Context
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.SeekBar
import android.widget.TextView
import androidx.core.view.setPadding
import org.fcitx.fcitx5.android.data.PianoSoundManager
import org.fcitx.fcitx5.android.data.theme.Theme
import splitties.dimensions.dp
import splitties.views.backgroundColor
import splitties.views.dsl.core.Ui
import splitties.views.dsl.core.frameLayout
import splitties.views.dsl.core.lParams
import splitties.views.dsl.core.matchParent
import splitties.views.dsl.core.seekBar
import splitties.views.dsl.core.textView
import splitties.views.dsl.core.verticalLayout
import splitties.views.dsl.core.wrapContent
import splitties.views.gravityHorizontalCenter
import splitties.views.textAppearance
import splitties.views.textColor

class PianoSettingsUi(override val ctx: Context, private val theme: Theme) : Ui {

    private var currentVolume = 50
    private var currentMood = PianoSoundManager.Mood.Default

    override val root = verticalLayout {
        gravity = gravityHorizontalCenter
        setPadding(dp(16))
    }

    private val volumeLabel = textView {
        text = "钢琴音量: $currentVolume%"
        textAppearance = android.R.style.TextAppearance_Medium
        textColor = theme.keyTextColor
        gravity = Gravity.CENTER
    }

    private val volumeSeekBar = seekBar {
        max = 100
        progress = currentVolume
        setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar, progress: Int, fromUser: Boolean) {
                currentVolume = progress
                volumeLabel.text = "钢琴音量: $progress%"
                PianoSoundManager.setVolume(progress)
            }
            override fun onStartTrackingTouch(seekBar: SeekBar) {}
            override fun onStopTrackingTouch(seekBar: SeekBar) {}
        })
    }

    private val moodLabel = textView {
        text = "钢琴风格"
        textAppearance = android.R.style.TextAppearance_Medium
        textColor = theme.keyTextColor
        gravity = Gravity.CENTER
    }

    private val moodButtons = LinearLayout(ctx).apply {
        orientation = LinearLayout.HORIZONTAL
        gravity = Gravity.CENTER
    }

    init {
        PianoSoundManager.Mood.entries.forEach { mood ->
            val btn = textView {
                text = mood.label
                textAppearance = android.R.style.TextAppearance_Small
                textColor = theme.keyTextColor
                gravity = Gravity.CENTER
                setPadding(dp(12), dp(8), dp(12), dp(8))
                setBackgroundColor(
                    if (mood == currentMood) theme.accentKeyBackgroundColor
                    else theme.keyBackgroundColor
                )
                setOnClickListener {
                    currentMood = mood
                    PianoSoundManager.setMood(mood)
                    updateMoodButtonStyles()
                }
            }
            moodButtons.addView(btn, LinearLayout.LayoutParams(
                0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f
            ))
        }

        root.addView(volumeLabel, lParams(matchParent, wrapContent) {
            topMargin = dp(16)
        })
        root.addView(volumeSeekBar, lParams(matchParent, wrapContent) {
            topMargin = dp(8)
            horizontalMargin = dp(16)
        })
        root.addView(moodLabel, lParams(matchParent, wrapContent) {
            topMargin = dp(24)
        })
        root.addView(moodButtons, lParams(matchParent, wrapContent) {
            topMargin = dp(8)
            horizontalMargin = dp(16)
        })
    }

    private fun updateMoodButtonStyles() {
        for (i in 0 until moodButtons.childCount) {
            val btn = moodButtons.getChildAt(i) as TextView
            val mood = PianoSoundManager.Mood.entries[i]
            btn.setBackgroundColor(
                if (mood == currentMood) theme.accentKeyBackgroundColor
                else theme.keyBackgroundColor
            )
        }
    }
}
