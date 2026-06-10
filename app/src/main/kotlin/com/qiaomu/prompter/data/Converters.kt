package com.qiaomu.prompter.data

import androidx.room.TypeConverter

class Converters {
    @TypeConverter
    fun textColorPresetToRawValue(value: TextColorPreset): String = value.rawValue

    @TypeConverter
    fun rawValueToTextColorPreset(value: String): TextColorPreset =
        TextColorPreset.fromRawValue(value)
}
