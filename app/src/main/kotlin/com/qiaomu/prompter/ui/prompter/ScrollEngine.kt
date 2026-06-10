package com.qiaomu.prompter.ui.prompter

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.isActive
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

class ScrollEngine(
    initialSpeed: Double = 80.0
) {
    var offset by mutableDoubleStateOf(0.0)
        private set

    var isPlaying by mutableStateOf(false)
        private set

    var speed by mutableDoubleStateOf(initialSpeed.coerceIn(MIN_SPEED, MAX_SPEED))
        private set

    private var lineHeight = 84.0
    private var averageCharactersPerLine = 18.0
    private var maximumOffset = 0.0
    private var followTargetOffset: Double? = null

    fun configure(
        speed: Double,
        lineHeight: Double,
        averageCharactersPerLine: Double,
        maximumOffset: Double
    ) {
        this.speed = speed.coerceIn(MIN_SPEED, MAX_SPEED)
        this.lineHeight = max(40.0, lineHeight)
        this.averageCharactersPerLine = max(6.0, averageCharactersPerLine)
        this.maximumOffset = max(0.0, maximumOffset)
        offset = offset.coerceIn(0.0, this.maximumOffset)
        followTargetOffset = followTargetOffset?.coerceIn(0.0, this.maximumOffset)
    }

    fun play() {
        followTargetOffset = null
        isPlaying = true
    }

    fun pause() {
        isPlaying = false
    }

    fun toggle() {
        if (isPlaying) pause() else play()
    }

    fun updateSpeed(value: Double) {
        speed = value.coerceIn(MIN_SPEED, MAX_SPEED)
    }

    fun updateOffset(value: Double) {
        followTargetOffset = null
        applyOffset(value)
    }

    fun follow(to: Double) {
        followTargetOffset = to.coerceIn(0.0, maximumOffset)
        isPlaying = false
    }

    fun stopFollowing() {
        followTargetOffset = null
    }

    fun reset() {
        offset = 0.0
        followTargetOffset = null
        pause()
    }

    suspend fun runFrameLoop() {
        var lastFrameNanos = 0L
        while (currentCoroutineContext().isActive) {
            val frameNanos = withFrameNanos { it }
            if (!isPlaying && followTargetOffset == null) {
                lastFrameNanos = frameNanos
                continue
            }
            if (lastFrameNanos == 0L) {
                lastFrameNanos = frameNanos
                continue
            }

            val delta = (frameNanos - lastFrameNanos) / 1_000_000_000.0
            lastFrameNanos = frameNanos

            val target = followTargetOffset
            if (target != null) {
                val response = min(1.0, delta * 12.0)
                val nextOffset = offset + (target - offset) * response
                if (abs(nextOffset - target) < 0.5) {
                    applyOffset(target)
                    followTargetOffset = null
                } else {
                    applyOffset(nextOffset)
                }
            } else {
                val linesPerSecond = (speed / averageCharactersPerLine) / 60.0 * VISUAL_TUNING_FACTOR
                val pixelsPerSecond = linesPerSecond * lineHeight
                applyOffset(offset + pixelsPerSecond * delta)
            }
        }
    }

    private fun applyOffset(value: Double) {
        offset = value.coerceIn(0.0, maximumOffset)
        if (offset >= maximumOffset) {
            pause()
        }
    }

    companion object {
        private const val MIN_SPEED = 20.0
        private const val MAX_SPEED = 260.0
        private const val VISUAL_TUNING_FACTOR = 1.85
    }
}
