/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * SPDX-FileCopyrightText: Copyright 2025 Fcitx5 for Android Contributors
 */
package org.fcitx.fcitx5.android.input.settings

import android.view.Gravity
import android.view.View
import android.widget.ScrollView
import androidx.transition.Slide
import org.fcitx.fcitx5.android.R
import org.fcitx.fcitx5.android.input.bar.ui.settings.PianoSettingsUi
import org.fcitx.fcitx5.android.input.dependency.context
import org.fcitx.fcitx5.android.input.dependency.theme
import org.fcitx.fcitx5.android.input.wm.InputWindow
import splitties.dimensions.dp
import splitties.views.dsl.core.lParams
import splitties.views.dsl.core.matchParent
import splitties.views.dsl.core.scrollView
import splitties.views.dsl.core.textView
import splitties.views.dsl.core.verticalLayout
import splitties.views.dsl.core.wrapContent
import splitties.views.gravityHorizontalCenter
import splitties.views.textAppearance
import splitties.views.textColor

class SettingsWindow : InputWindow.ExtendedInputWindow<SettingsWindow>() {

    private val context by manager.context()
    private val theme by manager.theme()

    override val title: String
        get() = context.getString(R.string.settings)

    override val showTitle: Boolean = true

    override fun enterAnimation(lastWindow: InputWindow) = Slide().apply {
        slideEdge = Gravity.BOTTOM
    }

    override fun onCreateView(): View {
        return scrollView {
            addView(verticalLayout {
                gravity = gravityHorizontalCenter

                // 钢琴音设置区域
                addView(sectionTitle("🎹 钢琴音设置"))
                addView(PianoSettingsUi(context, theme).root, lParams(matchParent, wrapContent))

                // AI 模型设置占位
                addView(sectionTitle("🤖 AI 模型"), lParams(matchParent, wrapContent) {
                    topMargin = dp(16)
                })
                addView(textView {
                    text = "GLM-4.7 Flash\n配置 API Key 和模型参数\n\nComing soon..."
                    textAppearance = android.R.style.TextAppearance_Small
                    textColor = theme.keyTextColor
                    gravity = Gravity.CENTER
                }, lParams(matchParent, wrapContent) {
                    topMargin = dp(8)
                    bottomMargin = dp(16)
                })

                // GIF 图源设置占位
                addView(sectionTitle("🎬 GIF 图源"), lParams(matchParent, wrapContent) {
                    topMargin = dp(16)
                })
                addView(textView {
                    text = "配置 GIF 搜索 API\n选择图源和搜索参数\n\nComing soon..."
                    textAppearance = android.R.style.TextAppearance_Small
                    textColor = theme.keyTextColor
                    gravity = Gravity.CENTER
                }, lParams(matchParent, wrapContent) {
                    topMargin = dp(8)
                    bottomMargin = dp(16)
                })
            }, lParams(matchParent, wrapContent))
        }
    }

    private fun sectionTitle(title: String) = textView {
        text = title
        textAppearance = android.R.style.TextAppearance_Large
        textColor = theme.keyTextColor
        gravity = Gravity.CENTER
    }
}
