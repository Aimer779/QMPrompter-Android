package com.qiaomu.prompter.speech

internal class SpeechScriptIndex(
    content: String,
    initialProgress: Double = 0.0
) {
    private val normalizedContent = normalize(content)
    private var committedOffset = (normalizedContent.length * initialProgress)
        .toInt()
        .coerceIn(0, normalizedContent.length)

    fun progress(transcript: String): Double {
        val spoken = normalize(transcript)
        if (spoken.isEmpty() || normalizedContent.isEmpty()) {
            return progressAtOffset(committedOffset)
        }

        val matchOffset = bestMatchEndOffset(spoken)
        if (matchOffset != null) {
            committedOffset = maxOf(committedOffset, matchOffset)
        } else if (committedOffset == 0) {
            committedOffset = maxOf(committedOffset, commonPrefixCount(spoken))
        }

        return progressAtOffset(committedOffset)
    }

    private fun bestMatchEndOffset(spoken: String): Int? {
        var bestEndOffset: Int? = null
        var bestScore: Int? = null

        for (fragment in candidateFragments(spoken)) {
            for (range in rangesOf(fragment)) {
                val startOffset = range.first
                val endOffset = range.last + 1
                if (!isPlausibleMatch(startOffset, endOffset, fragment.length)) {
                    continue
                }

                val score = matchScore(startOffset, endOffset, fragment.length)
                if (bestScore == null || score > bestScore) {
                    bestEndOffset = endOffset
                    bestScore = score
                }
            }
        }

        return bestEndOffset
    }

    private fun candidateFragments(spoken: String): List<String> {
        val fragments = mutableListOf<String>()

        fun append(fragment: String) {
            if (fragment.length >= 2 && fragment !in fragments) {
                fragments += fragment
            }
        }

        if (spoken.length <= 96) {
            append(spoken)
        }

        val maximumSuffixLength = minOf(48, spoken.length)
        if (maximumSuffixLength >= 4) {
            var length = maximumSuffixLength
            while (length >= 4) {
                append(spoken.takeLast(length))
                length -= 4
            }
        }

        if (spoken.length >= 3) {
            append(spoken.takeLast(3))
        }

        return fragments
    }

    private fun rangesOf(fragment: String): List<IntRange> {
        val ranges = mutableListOf<IntRange>()
        var startIndex = 0

        while (startIndex < normalizedContent.length) {
            val index = normalizedContent.indexOf(fragment, startIndex)
            if (index < 0) {
                break
            }
            ranges += index until index + fragment.length
            startIndex = index + fragment.length
        }

        return ranges
    }

    private fun isPlausibleMatch(startOffset: Int, endOffset: Int, fragmentLength: Int): Boolean {
        if (committedOffset == 0) {
            val earlyWindow = minOf(
                maxOf(80, normalizedContent.length / 5),
                maxOf(80, fragmentLength * 16)
            )
            return fragmentLength >= 8 || startOffset <= earlyWindow
        }

        val backwardTolerance = maxOf(12, fragmentLength * 2)
        if (endOffset + backwardTolerance < committedOffset) {
            return false
        }

        val forwardTolerance = maxOf(80, fragmentLength * 18)
        if (fragmentLength < 12 && startOffset > committedOffset + forwardTolerance) {
            return false
        }

        return true
    }

    private fun matchScore(startOffset: Int, endOffset: Int, fragmentLength: Int): Int {
        val anchorDistance = kotlin.math.abs(startOffset - committedOffset)
        val backwardPenalty = maxOf(0, committedOffset - endOffset)
        val initialPenalty = if (committedOffset == 0) startOffset * 3 else 0

        return fragmentLength * 1_000 -
            anchorDistance * 3 -
            backwardPenalty * 8 -
            initialPenalty
    }

    private fun progressAtOffset(offset: Int): Double =
        if (normalizedContent.isEmpty()) {
            0.0
        } else {
            (offset.toDouble() / normalizedContent.length.toDouble()).coerceIn(0.0, 1.0)
        }

    private fun commonPrefixCount(spoken: String): Int =
        normalizedContent.zip(spoken).takeWhile { (left, right) -> left == right }.count()

    internal companion object {
        fun normalize(value: String): String =
            value.lowercase().filter { it.isLetterOrDigit() }
    }
}
