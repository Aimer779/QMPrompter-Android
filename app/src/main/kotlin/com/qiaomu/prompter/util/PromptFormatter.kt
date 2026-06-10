package com.qiaomu.prompter.util

import kotlin.math.ceil
import kotlin.math.max

data class PromptLine(
    val text: String,
    val characterCount: Int
)

object PromptFormatter {
    fun lines(content: String, targetCharactersPerLine: Int = 18): List<PromptLine> {
        val normalized = content
            .replace("\r\n", "\n")
            .replace("\r", "\n")

        val result = mutableListOf<PromptLine>()
        for (paragraph in normalized.split("\n")) {
            val trimmed = paragraph.trim()
            if (trimmed.isEmpty()) {
                result += PromptLine(text = "", characterCount = targetCharactersPerLine)
            } else {
                result += split(trimmed, targetCharactersPerLine)
            }
        }
        return result.filter { it.text.isNotEmpty() || result.size > 1 }
    }

    private fun split(text: String, target: Int): List<PromptLine> {
        val lines = mutableListOf<PromptLine>()
        val current = StringBuilder()
        val semanticMinimumLength = max(8, target)
        val hardLimit = max(max(semanticMinimumLength, target + 4), ceil(target * 1.35).toInt())

        for (character in text) {
            current.append(character)
            val currentCount = current.codePointCount()
            val shouldBreakAtStrongPunctuation = character in STRONG_PUNCTUATION && currentCount >= 4
            val shouldBreakAtSoftPunctuation = character in SOFT_PUNCTUATION && currentCount >= semanticMinimumLength
            val shouldBreakAtLength = currentCount >= hardLimit

            if (shouldBreakAtStrongPunctuation || shouldBreakAtSoftPunctuation || shouldBreakAtLength) {
                append(current.toString(), lines, target)
                current.clear()
            }
        }

        append(current.toString(), lines, target)
        return lines
    }

    private fun append(text: String, lines: MutableList<PromptLine>, fallbackCount: Int) {
        val trimmed = text.trim()
        if (trimmed.isEmpty()) return

        if (!containsSpeakableCharacter(trimmed) && lines.isNotEmpty()) {
            val previous = lines.removeAt(lines.lastIndex)
            val merged = previous.text + trimmed
            lines += PromptLine(
                text = merged,
                characterCount = max(previous.characterCount, merged.codePointCount())
            )
            return
        }

        lines += PromptLine(
            text = trimmed,
            characterCount = max(max(1, trimmed.codePointCount()), fallbackCount / 2)
        )
    }

    private fun containsSpeakableCharacter(text: String): Boolean =
        text.any { it.isLetterOrDigit() }

    private fun CharSequence.codePointCount(): Int =
        Character.codePointCount(this, 0, length)

    private const val STRONG_PUNCTUATION = "。！？；.!?;：:"
    private const val SOFT_PUNCTUATION = "，、,"
}
