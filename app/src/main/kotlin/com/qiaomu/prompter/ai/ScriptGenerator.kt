package com.qiaomu.prompter.ai

interface ScriptGenerator {
    suspend fun generateScript(prompt: String): String
}

class ScriptGenerationException(message: String) : Exception(message)
