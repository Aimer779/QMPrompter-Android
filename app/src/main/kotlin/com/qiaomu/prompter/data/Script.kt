package com.qiaomu.prompter.data

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(tableName = "scripts")
data class Script(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val title: String,
    val content: String,
    @ColumnInfo(name = "created_at") val createdAt: Long = System.currentTimeMillis(),
    @ColumnInfo(name = "updated_at") val updatedAt: Long = System.currentTimeMillis(),
    @ColumnInfo(name = "font_size") val fontSize: Double = DEFAULT_FONT_SIZE,
    @ColumnInfo(name = "scroll_speed") val scrollSpeed: Double = 80.0,
    @ColumnInfo(name = "text_color_preset") val textColorPreset: TextColorPreset = TextColorPreset.White,
    @ColumnInfo(name = "overlay_opacity") val overlayOpacity: Double = 0.48
) {
    val preview: String
        get() = content.replace('\n', ' ').trim()

    companion object {
        const val DEFAULT_FONT_SIZE = 42.0
        const val UNTITLED = "未命名文稿"

        fun createDraft(): Script = Script(title = UNTITLED, content = "")

        fun create(title: String, content: String): Script =
            Script(
                title = title.trim().ifEmpty { UNTITLED },
                content = content
            )
    }
}
