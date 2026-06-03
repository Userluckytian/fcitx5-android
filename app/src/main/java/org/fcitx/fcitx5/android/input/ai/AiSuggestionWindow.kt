/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * SPDX-FileCopyrightText: Copyright 2025 Fcitx5 for Android Contributors
 */
package org.fcitx.fcitx5.android.input.ai

import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import androidx.core.view.setPadding
import androidx.lifecycle.lifecycleScope
import androidx.transition.Slide
import kotlinx.coroutines.launch
import org.fcitx.fcitx5.android.R
import org.fcitx.fcitx5.android.input.FcitxInputMethodService
import org.fcitx.fcitx5.android.input.dependency.context
import org.fcitx.fcitx5.android.input.dependency.inputMethodService
import org.fcitx.fcitx5.android.input.dependency.theme
import org.fcitx.fcitx5.android.input.wm.InputWindow
import splitties.dimensions.dp
import splitties.views.dsl.core.add
import splitties.views.dsl.core.lParams
import splitties.views.dsl.core.matchParent
import splitties.views.dsl.core.scrollView
import splitties.views.dsl.core.textView
import splitties.views.dsl.core.verticalLayout
import splitties.views.dsl.core.wrapContent
import splitties.views.gravityHorizontalCenter
import splitties.views.textAppearance
import splitties.views.textColor

class AiSuggestionWindow : InputWindow.ExtendedInputWindow<AiSuggestionWindow>() {

    private val context by manager.context()
    private val theme by manager.theme()
    private val service: FcitxInputMethodService by manager.inputMethodService()

    override val title: String
        get() = context.getString(R.string.ai_suggestion)

    override val showTitle: Boolean = true

    override fun enterAnimation(lastWindow: InputWindow) = Slide().apply {
        slideEdge = Gravity.BOTTOM
    }

    private lateinit var suggestionsContainer: LinearLayout
    private lateinit var loadingView: TextView
    private lateinit var regenerateButton: TextView

    override fun onCreateView(): View {
        return scrollView {
            addView(verticalLayout {
                gravity = gravityHorizontalCenter

                loadingView = textView {
                    text = "🤔 AI 正在思考..."
                    textAppearance = android.R.style.TextAppearance_Medium
                    textColor = theme.keyTextColor
                    gravity = Gravity.CENTER
                }
                addView(loadingView, lParams(matchParent, wrapContent) {
                    topMargin = dp(24)
                    bottomMargin = dp(24)
                })

                suggestionsContainer = LinearLayout(context).apply {
                    orientation = LinearLayout.VERTICAL
                }
                addView(suggestionsContainer, lParams(matchParent, wrapContent))

                regenerateButton = textView {
                    text = "🔄 换一批建议"
                    textAppearance = android.R.style.TextAppearance_Medium
                    textColor = theme.keyTextColor
                    gravity = Gravity.CENTER
                    setPadding(dp(12), dp(12), dp(12), dp(12))
                    setBackgroundColor(theme.keyBackgroundColor)
                    setOnClickListener { fetchSuggestions() }
                }
                addView(regenerateButton, lParams(matchParent, wrapContent) {
                    topMargin = dp(16)
                    bottomMargin = dp(16)
                    horizontalMargin = dp(16)
                })
            }, lParams(matchParent, wrapContent))
        }.also {
            fetchSuggestions()
        }
    }

    private fun fetchSuggestions() {
        showLoading()
        val scope = service.lifecycleScope
        scope.launch {
            val contextMessages = getRecentMessages()
            val result = AiApiClient.getSuggestions(contextMessages)
            result.onSuccess { suggestions ->
                showSuggestions(suggestions)
            }.onFailure { e ->
                showError(e.message ?: "Unknown error")
            }
        }
    }

    private fun getRecentMessages(): List<String> {
        val messages = mutableListOf<String>()
        try {
            // 获取光标前的文本作为上下文
            val ic = service.currentInputConnection ?: return messages
            val beforeText = ic.getTextBeforeCursor(500, 0)?.toString() ?: ""
            if (beforeText.isNotBlank()) {
                messages.add(beforeText)
            }
            val afterText = ic.getTextAfterCursor(200, 0)?.toString() ?: ""
            if (afterText.isNotBlank()) {
                messages.add(afterText)
            }
        } catch (_: Exception) {}
        if (messages.isEmpty()) {
            messages.add("你好")
        }
        return messages
    }

    private fun showLoading() {
        suggestionsContainer.removeAllViews()
        loadingView.visibility = View.VISIBLE
        regenerateButton.visibility = View.GONE
    }

    private fun showSuggestions(suggestions: List<AiApiClient.Suggestion>) {
        loadingView.visibility = View.GONE
        regenerateButton.visibility = View.VISIBLE
        suggestionsContainer.removeAllViews()

        suggestions.forEach { suggestion ->
            val card = createSuggestionCard(suggestion)
            suggestionsContainer.addView(card, LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply {
                setMargins(dp(16), dp(8), dp(16), dp(8))
            })
        }
    }

    private fun showError(message: String) {
        loadingView.visibility = View.VISIBLE
        loadingView.text = "❌ 出错了: $message\n点击重试"
        loadingView.setOnClickListener { fetchSuggestions() }
        regenerateButton.visibility = View.VISIBLE
        suggestionsContainer.removeAllViews()
    }

    private fun createSuggestionCard(suggestion: AiApiClient.Suggestion): View {
        return verticalLayout {
            setBackgroundColor(theme.keyBackgroundColor)
            setPadding(dp(16), dp(16), dp(16), dp(16))

            addView(textView {
                text = suggestion.text
                textAppearance = android.R.style.TextAppearance_Medium
                textColor = theme.keyTextColor
            }, lParams(matchParent, wrapContent) {
                bottomMargin = dp(12)
            })

            val actionsRow = LinearLayout(context).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.END
            }

            val copyBtn = textView {
                text = "📋 复制"
                textAppearance = android.R.style.TextAppearance_Small
                textColor = theme.keyTextColor
                setPadding(dp(12), dp(8), dp(12), dp(8))
                setBackgroundColor(theme.altKeyBackgroundColor)
                setOnClickListener {
                    service.currentInputConnection?.setComposingText(suggestion.text, 1)
                }
            }
            actionsRow.addView(copyBtn, LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply {
                rightMargin = dp(8)
            })

            val sendBtn = textView {
                text = "📤 发送"
                textAppearance = android.R.style.TextAppearance_Small
                textColor = theme.accentKeyTextColor
                setPadding(dp(12), dp(8), dp(12), dp(8))
                setBackgroundColor(theme.accentKeyBackgroundColor)
                setOnClickListener {
                    service.commitText(suggestion.text)
                    manager.detachWindow(this@AiSuggestionWindow)
                }
            }
            actionsRow.addView(sendBtn)

            addView(actionsRow, lParams(matchParent, wrapContent))
        }
    }
}
