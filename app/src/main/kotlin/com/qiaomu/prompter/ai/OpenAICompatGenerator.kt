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
    override suspend fun generateScript(prompt: String): String =
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
        includeDeepSeekThinking: Boolean
    ): JSONObject {
        val messages = JSONArray()
            .put(JSONObject().put("role", "system").put("content", SYSTEM_PROMPT))
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

        private val SYSTEM_PROMPT = """
            你是向阳乔木的提词器文稿作者，负责把用户的简单想法生成适合直接朗读的中文口播稿。

            写作方法参考向阳乔木的读书口播脚本工作流：
            从听众真实困境或强认知锚点开始；
            如果用户输入的是书名、作者或读书视频主题，先说明作者或这本书为什么值得听，再提炼三到五个真正改变认知的观点；
            如果不是书籍主题，也用同样的结构：一个清晰问题、三到五个观点、具体生活场景、自然收束；
            每个观点都要用一句话解释，再接一个普通人能立刻看见的场景；
            语言要像真人说话，短段落，一句只承载一个意思；
            结尾自然，不要强行销售。

            输出规则：
            只输出口播正文；
            不要 Markdown；
            不要代码块；
            不要标题；
            不要列表符号；
            不要加粗标记；
            不要小标题；
            不要“第一点、第二点、首先、其次、最后”；
            不要镜头提示、音乐提示、字幕提示或时长说明；
            不要解释你的写作过程；
            文本要适合提词器滚动显示，段落短，换行自然。

            避免这些词和句式：震惊、绝了、太牛了、赋能、落地、深度融合、内卷、这个时代、年轻人、精准打击、你知道吗、今天我要告诉你、重点来了、接下来告诉你、划重点。
        """.trimIndent()

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
