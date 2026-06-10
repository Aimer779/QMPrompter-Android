package com.qiaomu.prompter.speech

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SpeechScriptIndexTest {
    @Test
    fun normalizeKeepsLettersAndDigitsOnly() {
        assertEquals(
            "hello世界123",
            SpeechScriptIndex.normalize("Hello，世界！ 123.")
        )
    }

    @Test
    fun progressesForMatchingChineseTranscript() {
        val index = SpeechScriptIndex("今天我们聊一本书。\n它讲的是普通人如何面对变化。")

        val progress = index.progress("今天我们聊一本书")

        assertTrue(progress > 0.30)
        assertTrue(progress < 0.40)
    }

    @Test
    fun progressDoesNotMoveBackward() {
        val index = SpeechScriptIndex("第一段内容很短。第二段内容继续推进。第三段自然收束。")

        val later = index.progress("第二段内容继续推进")
        val earlier = index.progress("第一段内容")

        assertTrue(later > 0.5)
        assertEquals(later, earlier, 0.0001)
    }

    @Test
    fun initialProgressAnchorsShortFragmentsNearCurrentPosition() {
        val content = "开头先提出一个问题。中间展开一个场景。结尾自然收束。"
        val index = SpeechScriptIndex(content, initialProgress = 0.45)

        val progress = index.progress("场景")

        assertTrue(progress > 0.65)
    }

    @Test
    fun emptyInputKeepsInitialProgress() {
        val index = SpeechScriptIndex("一二三四五六七八九十", initialProgress = 0.4)

        assertEquals(0.4, index.progress(""), 0.0001)
    }
}
