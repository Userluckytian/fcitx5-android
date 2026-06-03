/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * SPDX-FileCopyrightText: Copyright 2025 Fcitx5 for Android Contributors
 */
package org.fcitx.fcitx5.android.input.ai

import android.view.Gravity
import android.view.View
import android.widget.TextView
import androidx.transition.Slide
import org.fcitx.fcitx5.android.R
import org.fcitx.fcitx5.android.input.dependency.context
import org.fcitx.fcitx5.android.input.dependency.theme
import org.fcitx.fcitx5.android.input.wm.InputWindow
import splitties.views.dsl.core.textView
import splitties.views.dsl.core.wrapContent

class AiSuggestionWindow : InputWindow.ExtendedInputWindow<AiSuggestionWindow>() {

    private val context by manager.context()
    private val theme by manager.theme()

    override val title: String
        get() = context.getString(R.string.ai_suggestion)

    override val showTitle: Boolean = true

    override fun enterAnimation(lastWindow: InputWindow) = Slide().apply {
        slideEdge = Gravity.BOTTOM
    }

    override fun onCreateView(): View {
        return textView {
            text = "AI Suggestion Panel\n\nPowered by GLM-4.7 Flash\n\nComing soon..."
            textSize = 16f
            setTextColor(theme.keyTextColor)
            gravity = Gravity.CENTER
        }
    }
}
