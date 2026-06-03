/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * SPDX-FileCopyrightText: Copyright 2025 Fcitx5 for Android Contributors
 */
package org.fcitx.fcitx5.android.input.gif

import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.view.setPadding
import androidx.lifecycle.lifecycleScope
import androidx.transition.Slide
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.fcitx.fcitx5.android.R
import org.fcitx.fcitx5.android.input.FcitxInputMethodService
import org.fcitx.fcitx5.android.input.dependency.context
import org.fcitx.fcitx5.android.input.dependency.inputMethodService
import org.fcitx.fcitx5.android.input.dependency.theme
import org.fcitx.fcitx5.android.input.wm.InputWindow
import org.json.JSONObject
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
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

class GifStickerWindow : InputWindow.ExtendedInputWindow<GifStickerWindow>() {

    private val context by manager.context()
    private val theme by manager.theme()
    private val service: FcitxInputMethodService by manager.inputMethodService()

    override val title: String
        get() = context.getString(R.string.gif_sticker)

    override val showTitle: Boolean = true

    override fun enterAnimation(lastWindow: InputWindow) = Slide().apply {
        slideEdge = Gravity.BOTTOM
    }

    private lateinit var searchInput: EditText
    private lateinit var resultsContainer: LinearLayout
    private lateinit var loadingView: TextView

    override fun onCreateView(): View {
        return scrollView {
            addView(verticalLayout {
                gravity = gravityHorizontalCenter

                // 搜索栏
                searchInput = editText {
                    hint = "搜索 GIF 表情..."
                    setTextColor(theme.keyTextColor)
                    setHintTextColor(theme.altKeyTextColor)
                    setBackgroundColor(theme.keyBackgroundColor)
                    setPadding(dp(12), dp(8), dp(12), dp(8))
                    inputType = EditorInfo.TYPE_CLASS_TEXT
                    imeOptions = EditorInfo.IME_ACTION_SEARCH
                    setOnEditorActionListener { _, actionId, _ ->
                        if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                            searchGifs(searchInput.text.toString())
                            true
                        } else false
                    }
                }
                addView(searchInput, lParams(matchParent, wrapContent) {
                    horizontalMargin = dp(16)
                    topMargin = dp(8)
                })

                loadingView = textView {
                    text = "🔍 输入关键词搜索 GIF"
                    textAppearance = android.R.style.TextAppearance_Small
                    textColor = theme.keyTextColor
                    gravity = Gravity.CENTER
                }
                addView(loadingView, lParams(matchParent, wrapContent) {
                    topMargin = dp(16)
                    bottomMargin = dp(16)
                })

                resultsContainer = LinearLayout(context).apply {
                    orientation = LinearLayout.VERTICAL
                }
                addView(resultsContainer, lParams(matchParent, wrapContent))
            }, lParams(matchParent, wrapContent))
        }
    }

    private fun searchGifs(query: String) {
        if (query.isBlank()) return
        loadingView.text = "🔍 搜索中..."
        loadingView.visibility = View.VISIBLE
        resultsContainer.removeAllViews()

        service.lifecycleScope.launch {
            try {
                val results = searchTenor(query)
                showResults(results)
            } catch (e: Exception) {
                loadingView.text = "❌ 搜索失败: ${e.message}"
            }
        }
    }

    private suspend fun searchTenor(query: String): List<GifResult> = withContext(Dispatchers.IO) {
        val encodedQuery = URLEncoder.encode(query, "UTF-8")
        val url = "https://tenor.googleapis.com/v2/search?q=$encodedQuery&key=LIVDSRZULELA&limit=10&media_filter=gif"
        val conn = URL(url).openConnection() as HttpURLConnection
        conn.connectTimeout = 10000
        conn.readTimeout = 10000

        val response = conn.inputStream.bufferedReader().readText()
        val json = JSONObject(response)
        val results = json.getJSONArray("results")
        (0 until results.length()).map { i ->
            val item = results.getJSONObject(i)
            val media = item.getJSONArray("media_formats").getJSONObject(0)
            GifResult(
                url = media.getString("url"),
                description = item.optString("content_description", "GIF")
            )
        }
    }

    private fun showResults(results: List<GifResult>) {
        loadingView.visibility = View.GONE
        resultsContainer.removeAllViews()

        results.forEach { gif ->
            val item = textView {
                text = "🎬 ${gif.description}\n[点击发送GIF链接]"
                textAppearance = android.R.style.TextAppearance_Small
                textColor = theme.keyTextColor
                setPadding(dp(12), dp(12), dp(12), dp(12))
                setBackgroundColor(theme.keyBackgroundColor)
                setOnClickListener {
                    service.commitText("[GIF] ${gif.url}")
                    manager.detachWindow(this@GifStickerWindow)
                }
            }
            resultsContainer.addView(item, LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply {
                setMargins(dp(16), dp(4), dp(16), dp(4))
            })
        }
    }

    data class GifResult(val url: String, val description: String)
}
