package com.qiaomu.prompter.ai

interface ScriptGenerator {
    suspend fun generateScript(
        prompt: String,
        systemPrompt: String = ScriptPromptStyles.QmTalk.systemPrompt
    ): String
}

class ScriptGenerationException(message: String) : Exception(message)
