package com.qiaomu.prompter.ai

import com.qiaomu.prompter.settings.AiProviderConfigStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException
import java.util.concurrent.TimeUnit

class OpenAICompatGenerator(
    private val configStore: AiProviderConfigStore,
    private val client: OkHttpClient = defaultClient
) : ScriptGenerator {
    override suspend fun generateScript(
        prompt: String,
        systemPrompt: String
    ): String =
        withContext(Dispatchers.IO) {
            val config = configStore.read()
            val apiKey = config.apiKey.trim()
            if (apiKey.isBlank()) {
                throw ScriptGenerationException("请先填写 API Key。")
            }

            val effectiveBaseUrl = config.baseUrl.trim().ifBlank { DEFAULT_BASE_URL }
            val effectiveModel = config.model.trim().ifBlank { DEFAULT_MODEL }
            val endpoint = "${effectiveBaseUrl.trimEnd('/')}/chat/completions"
            val body = requestBody(
                model = effectiveModel,
                prompt = prompt,
                systemPrompt = systemPrompt,
                includeDeepSeekThinking = effectiveBaseUrl.contains("deepseek.com", ignoreCase = true)
            )

            val request = Request.Builder()
                .url(endpoint)
                .header("Content-Type", "application/json")
                .header("Authorization", "Bearer $apiKey")
                .post(body.toString().toRequestBody(JSON_MEDIA_TYPE))
                .build()

            val response = try {
                client.newCall(request).execute()
            } catch (exception: IOException) {
                throw ScriptGenerationException("网络请求失败，请检查网络后重试。")
            }

            response.use {
                val responseText = it.body.string()
                if (!it.isSuccessful) {
                    throw ScriptGenerationException(errorMessage(responseText, it.code))
                }

                val content = runCatching {
                    JSONObject(responseText)
                        .getJSONArray("choices")
                        .getJSONObject(0)
                        .getJSONObject("message")
                        .getString("content")
                }.getOrElse {
                    throw ScriptGenerationException("AI 返回格式异常。")
                }

                val cleaned = cleanGeneratedScript(content)
                if (cleaned.isBlank()) {
                    throw ScriptGenerationException("AI 没有生成可用正文。")
                }
                cleaned
            }
        }

    private fun requestBody(
        model: String,
        prompt: String,
        systemPrompt: String,
        includeDeepSeekThinking: Boolean
    ): JSONObject {
        val messages = JSONArray()
            .put(JSONObject().put("role", "system").put("content", systemPrompt))
            .put(JSONObject().put("role", "user").put("content", userPrompt(prompt)))

        return JSONObject()
            .put("model", model)
            .put("messages", messages)
            .put("max_tokens", 2800)
            .put("stream", false)
            .also { body ->
                if (includeDeepSeekThinking) {
                    body.put("thinking", JSONObject().put("type", "disabled"))
                }
            }
    }

    private fun userPrompt(prompt: String): String =
        """
        用户的生成需求：
        ${prompt.trim()}

        请输出一篇可直接放进提词器朗读的中文口播文稿。
        """.trimIndent()

    private fun errorMessage(responseText: String, statusCode: Int): String {
        val apiMessage = runCatching {
            JSONObject(responseText)
                .getJSONObject("error")
                .getString("message")
        }.getOrNull()

        return apiMessage ?: "AI 请求失败：HTTP $statusCode。"
    }

    internal companion object {
        private const val DEFAULT_BASE_URL = "https://api.deepseek.com"
        private const val DEFAULT_MODEL = "deepseek-v4-flash"
        private val JSON_MEDIA_TYPE = "application/json; charset=utf-8".toMediaType()
        private val defaultClient = OkHttpClient.Builder()
            .callTimeout(90, TimeUnit.SECONDS)
            .build()

        fun cleanGeneratedScript(content: String): String {
            var result = content
                .replace("```text", "")
                .replace("```markdown", "")
                .replace("```", "")
                .replace("**", "")
                .replace("`", "")

            val lines = result
                .lineSequence()
                .map { it.trim() }
                .filter { line ->
                    line.isEmpty() ||
                        (!line.startsWith("#") &&
                            !line.startsWith("- ") &&
                            !line.startsWith("* ") &&
                            !line.startsWith(">") &&
                            !line.contains("预估时长"))
                }
                .toList()

            result = lines.joinToString(separator = "\n")
            while (result.contains("\n\n\n")) {
                result = result.replace("\n\n\n", "\n\n")
            }

            return result.trim()
        }
    }
}
