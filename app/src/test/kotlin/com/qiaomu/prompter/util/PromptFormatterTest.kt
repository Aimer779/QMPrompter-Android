package com.qiaomu.prompter.util

import org.junit.Assert.assertEquals
import org.junit.Test

class PromptFormatterTest {
    @Test
    fun splitsAtStrongChinesePunctuation() {
        val lines = PromptFormatter.lines("大家好，这是第一句。这里是第二句。", targetCharactersPerLine = 18)

        assertEquals(listOf("大家好，这是第一句。", "这里是第二句。"), lines.map { it.text })
    }

    @Test
    fun splitsAtSoftPunctuationAfterSemanticMinimum() {
        val lines = PromptFormatter.lines("这是一个比较长的句子，会在逗号之后断开", targetCharactersPerLine = 10)

        assertEquals(listOf("这是一个比较长的句子，", "会在逗号之后断开"), lines.map { it.text })
    }

    @Test
    fun normalizesCrLfAndKeepsBlankLineWhenMultipleLinesExist() {
        val lines = PromptFormatter.lines("第一行\r\n\r\n第二行", targetCharactersPerLine = 18)

        assertEquals(listOf("第一行", "", "第二行"), lines.map { it.text })
    }

    @Test
    fun filtersSingleEmptyInputLine() {
        val lines = PromptFormatter.lines("   ", targetCharactersPerLine = 18)

        assertEquals(emptyList<PromptLine>(), lines)
    }

    @Test
    fun mergesPunctuationOnlyFragmentIntoPreviousLine() {
        val lines = PromptFormatter.lines("这是前半句。！！", targetCharactersPerLine = 18)

        assertEquals(listOf("这是前半句。！！"), lines.map { it.text })
    }

    @Test
    fun breaksAtHardLimitWhenNoPunctuationExists() {
        val lines = PromptFormatter.lines("abcdefghijklmnopqrstuvwxyz", targetCharactersPerLine = 10)

        assertEquals(listOf("abcdefghijklmn", "opqrstuvwxyz"), lines.map { it.text })
    }
}
