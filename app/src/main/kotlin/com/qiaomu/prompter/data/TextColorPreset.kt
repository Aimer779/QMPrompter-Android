package com.qiaomu.prompter.data

enum class TextColorPreset(val rawValue: String, val displayName: String) {
    White("white", "白"),
    Silver("silver", "银灰"),
    Graphite("graphite", "深灰");

    companion object {
        val editorChoices: List<TextColorPreset> = listOf(White, Graphite)

        fun fromRawValue(rawValue: String): TextColorPreset =
            entries.firstOrNull { it.rawValue == rawValue } ?: White
    }
}
