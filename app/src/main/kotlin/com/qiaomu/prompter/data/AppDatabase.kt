package com.qiaomu.prompter.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [Script::class],
    version = 1,
    exportSchema = true
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun scriptDao(): ScriptDao

    companion object {
        private const val DATABASE_NAME = "qmprompter.db"

        @Volatile
        private var instance: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase =
            instance ?: synchronized(this) {
                instance ?: buildDatabase(context.applicationContext).also { instance = it }
            }

        private fun buildDatabase(context: Context): AppDatabase =
            Room.databaseBuilder(context, AppDatabase::class.java, DATABASE_NAME)
                .addCallback(SeedCallback())
                .build()
    }

    private class SeedCallback : Callback() {
        override fun onCreate(db: SupportSQLiteDatabase) {
            super.onCreate(db)
            val script = createSampleScript()
            db.execSQL(
                """
                    INSERT INTO scripts (
                        id,
                        title,
                        content,
                        created_at,
                        updated_at,
                        font_size,
                        scroll_speed,
                        text_color_preset,
                        overlay_opacity
                    ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
                """.trimIndent(),
                arrayOf<Any>(
                    script.id,
                    script.title,
                    script.content,
                    script.createdAt,
                    script.updatedAt,
                    script.fontSize,
                    script.scrollSpeed,
                    script.textColorPreset.rawValue,
                    script.overlayOpacity
                )
            )
        }
    }
}

private fun createSampleScript(): Script {
    val now = System.currentTimeMillis()
    return Script(
        title = "试用文稿",
        content = """
            大家好，这里是乔木提词器的第一版测试。

            这个版本先不录视频，只显示前置摄像头预览，让我可以看着镜头练习表达。

            点击屏幕中央可以播放或暂停。
            左侧上下滑动可以调整速度。
            右侧上下滑动可以跳转进度。

            如果这套基础体验顺手，下一步再接远程网页粘贴和同步 API。
        """.trimIndent(),
        createdAt = now,
        updatedAt = now,
        fontSize = Script.DEFAULT_FONT_SIZE,
        scrollSpeed = 78.0
    )
}
