/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * SPDX-FileCopyrightText: Copyright 2025 Fcitx5 for Android Contributors
 */
package org.fcitx.fcitx5.android.input.ai

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import timber.log.Timber
import java.net.HttpURLConnection
import java.net.URL

object AiApiClient {

    private const val API_URL = "https://open.bigmodel.cn/api/paas/v4/chat/completions"
    private const val MODEL = "glm-4.7-flash"
    private const val API_KEY = "2fb3d54e024246048b4517b56aa5071f.yEHKhUxgpptTaA22"

    data class Suggestion(val text: String)

    /**
     * 根据对话上下文获取 3 条 AI 回复建议
     * @param contextMessages 最近的对话消息列表
     * @return 3 条建议回复
     */
    suspend fun getSuggestions(contextMessages: List<String>): Result<List<Suggestion>> =
        withContext(Dispatchers.IO) {
            try {
                val messages = JSONArray()
                // 系统提示
                messages.put(JSONObject().apply {
                    put("role", "system")
                    put("content", "你是一个聊天助手，请根据对话上下文生成3条简短、自然、有趣的回复建议。" +
                        "每条回复不超过50字，风格要贴近年轻人的聊天方式。" +
                        "请以JSON数组格式返回，每个元素是一个包含'text'字段的对象。" +
                        "示例: [{\"text\":\"好的我明白了\"},{\"text\":\"这个想法不错\"},{\"text\":\"哈哈确实\"}]")
                })
                // 对话上下文
                contextMessages.forEach { msg ->
                    messages.put(JSONObject().apply {
                        put("role", "user")
                        put("content", msg)
                    })
                }
                // 要求生成回复
                messages.put(JSONObject().apply {
                    put("role", "user")
                    put("content", "请根据以上对话，生成3条可能的回复建议，以JSON数组格式返回。")
                })

                val requestBody = JSONObject().apply {
                    put("model", MODEL)
                    put("messages", messages)
                    put("temperature", 0.8)
                    put("max_tokens", 500)
                }

                val response = httpPost(API_URL, requestBody.toString())
                val content = response
                    .getJSONArray("choices")
                    .getJSONObject(0)
                    .getJSONObject("message")
                    .getString("content")

                val suggestions = parseSuggestions(content)
                Result.success(suggestions)
            } catch (e: Exception) {
                Timber.e(e, "AI API request failed")
                Result.failure(e)
            }
        }

    private fun parseSuggestions(content: String): List<Suggestion> {
        return try {
            // 尝试从内容中提取JSON数组
            val jsonStart = content.indexOf('[')
            val jsonEnd = content.lastIndexOf(']')
            if (jsonStart >= 0 && jsonEnd > jsonStart) {
                val jsonStr = content.substring(jsonStart, jsonEnd + 1)
                val arr = JSONArray(jsonStr)
                (0 until arr.length()).map { i ->
                    val obj = arr.getJSONObject(i)
                    Suggestion(obj.getString("text"))
                }
            } else {
                // 回退：按行分割
                content.split("\n")
                    .map { it.trim() }
                    .filter { it.isNotEmpty() }
                    .take(3)
                    .map { Suggestion(it.removePrefix("- ").removePrefix("1. ").removePrefix("2. ").removePrefix("3. ")) }
            }
        } catch (e: Exception) {
            Timber.w(e, "Failed to parse AI suggestions")
            listOf(
                Suggestion("好的，我明白了！"),
                Suggestion("这个想法不错！"),
                Suggestion("哈哈，确实如此！")
            )
        }
    }

    private fun httpPost(urlStr: String, body: String): JSONObject {
        val url = URL(urlStr)
        val conn = url.openConnection() as HttpURLConnection
        conn.requestMethod = "POST"
        conn.doOutput = true
        conn.setRequestProperty("Content-Type", "application/json")
        conn.setRequestProperty("Authorization", "Bearer $API_KEY")
        conn.connectTimeout = 15000
        conn.readTimeout = 15000

        conn.outputStream.use { os ->
            os.write(body.toByteArray(Charsets.UTF_8))
        }

        val responseCode = conn.responseCode
        val responseStream = if (responseCode in 200..299) conn.inputStream else conn.errorStream
        val responseBody = responseStream?.bufferedReader()?.readText() ?: "{}"

        if (responseCode !in 200..299) {
            throw RuntimeException("API error $responseCode: $responseBody")
        }

        return JSONObject(responseBody)
    }
}
