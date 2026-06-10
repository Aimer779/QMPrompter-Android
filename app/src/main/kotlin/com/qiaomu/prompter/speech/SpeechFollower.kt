package com.qiaomu.prompter.speech

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.content.ContextCompat
import java.util.Locale

class SpeechFollower(
    private val context: Context
) {
    var state by mutableStateOf<SpeechFollowerState>(SpeechFollowerState.Idle)
        private set
    var transcript by mutableStateOf("")
        private set
    var progress by mutableDoubleStateOf(0.0)
        private set

    val isListening: Boolean
        get() = state == SpeechFollowerState.Starting ||
            state == SpeechFollowerState.Listening ||
            state == SpeechFollowerState.RestartPending

    val statusText: String
        get() = when (val current = state) {
            SpeechFollowerState.Idle -> "语音跟随"
            SpeechFollowerState.Starting -> "正在启动语音"
            SpeechFollowerState.Listening -> {
                if (transcript.isBlank()) "正在听" else "跟随${(progress * 100).toInt()}%"
            }
            SpeechFollowerState.RestartPending -> "正在继续听"
            SpeechFollowerState.Denied -> "语音权限未开启"
            SpeechFollowerState.Unavailable -> "语音识别不可用"
            SpeechFollowerState.Disposed -> "语音已停止"
            is SpeechFollowerState.Failed -> current.message
        }

    private val appContext = context.applicationContext
    private val mainHandler = Handler(Looper.getMainLooper())
    private var recognizer: SpeechRecognizer? = null
    private var scriptIndex = SpeechScriptIndex("")
    private var content = ""
    private var disposed = false
    private var userStopped = false
    private var sessionToken = 0L
    private var busyCount = 0
    private var recoverableErrorCount = 0
    private var networkErrorCount = 0

    val hasTranscript: Boolean
        get() = transcript.isNotBlank()

    fun toggle(content: String, initialProgress: Double = progress) {
        if (isListening) {
            stop()
        } else {
            start(content, initialProgress)
        }
    }

    fun start(content: String, initialProgress: Double = 0.0) {
        stopInternal(nextState = SpeechFollowerState.Idle)
        disposed = false
        userStopped = false
        sessionToken += 1
        this.content = content
        transcript = ""
        progress = initialProgress.coerceIn(0.0, 1.0)
        scriptIndex = SpeechScriptIndex(content, progress)
        busyCount = 0
        recoverableErrorCount = 0
        networkErrorCount = 0

        if (ContextCompat.checkSelfPermission(appContext, Manifest.permission.RECORD_AUDIO) !=
            PackageManager.PERMISSION_GRANTED
        ) {
            state = SpeechFollowerState.Denied
            return
        }

        if (!SpeechRecognizer.isRecognitionAvailable(appContext)) {
            state = SpeechFollowerState.Unavailable
            return
        }

        startListening(sessionToken)
    }

    fun stop() {
        userStopped = true
        stopInternal(nextState = SpeechFollowerState.Idle)
    }

    fun dispose() {
        disposed = true
        userStopped = true
        stopInternal(nextState = SpeechFollowerState.Disposed)
    }

    private fun startListening(token: Long) {
        if (disposed || userStopped || token != sessionToken) return
        state = SpeechFollowerState.Starting
        ensureRecognizer()

        try {
            recognizer?.startListening(recognizerIntent())
            state = SpeechFollowerState.Listening
        } catch (exception: RuntimeException) {
            fail("语音启动失败")
        }
    }

    private fun ensureRecognizer() {
        if (recognizer != null) return

        recognizer = SpeechRecognizer.createSpeechRecognizer(appContext).also { speechRecognizer ->
            speechRecognizer.setRecognitionListener(object : RecognitionListener {
                override fun onReadyForSpeech(params: Bundle?) = Unit
                override fun onBeginningOfSpeech() = Unit
                override fun onRmsChanged(rmsdB: Float) = Unit
                override fun onBufferReceived(buffer: ByteArray?) = Unit

                override fun onEndOfSpeech() {
                    val token = sessionToken
                    mainHandler.postDelayed({
                        if (token == sessionToken && state == SpeechFollowerState.Listening) {
                            scheduleRestart(token)
                        }
                    }, END_OF_SPEECH_FALLBACK_DELAY_MILLIS)
                }

                override fun onError(error: Int) {
                    when (error) {
                        SpeechRecognizer.ERROR_NO_MATCH,
                        SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> {
                            recoverableErrorCount += 1
                            scheduleRestart(sessionToken)
                        }
                        SpeechRecognizer.ERROR_NETWORK_TIMEOUT -> {
                            networkErrorCount += 1
                            recoverableErrorCount += 1
                            scheduleRestart(sessionToken)
                        }
                        SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> {
                            busyCount += 1
                            scheduleRestart(sessionToken)
                        }
                        SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> {
                            fail("语音权限未开启")
                        }
                        else -> {
                            fail("语音识别中断")
                        }
                    }
                }

                override fun onResults(results: Bundle?) {
                    updateTranscript(results)
                    scheduleRestart(sessionToken)
                }

                override fun onPartialResults(partialResults: Bundle?) {
                    updateTranscript(partialResults)
                }

                override fun onEvent(eventType: Int, params: Bundle?) = Unit
            })
        }
    }

    private fun scheduleRestart(token: Long) {
        if (disposed || userStopped || token != sessionToken || state == SpeechFollowerState.RestartPending) return
        state = SpeechFollowerState.RestartPending

        when {
            busyCount >= MAX_BUSY_COUNT -> {
                fail("语音服务繁忙，请稍后重试")
                return
            }
            networkErrorCount >= MAX_NETWORK_ERROR_COUNT -> {
                fail("语音网络不稳定")
                return
            }
            recoverableErrorCount >= MAX_RECOVERABLE_ERROR_COUNT -> {
                fail("语音识别暂时不可用")
                return
            }
        }

        destroyRecognizer()
        mainHandler.postDelayed({ startListening(token) }, RESTART_DELAY_MILLIS)
    }

    private fun updateTranscript(results: Bundle?) {
        val best = results
            ?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
            ?.firstOrNull()
            .orEmpty()
        if (best.isBlank()) return

        transcript = best
        progress = scriptIndex.progress(best)
    }

    private fun recognizerIntent(): Intent =
        Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(
                RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                RecognizerIntent.LANGUAGE_MODEL_FREE_FORM
            )
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.SIMPLIFIED_CHINESE.toLanguageTag())
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 3)
            if (android.os.Build.VERSION.SDK_INT >= 33) {
                putStringArrayListExtra(
                    RecognizerIntent.EXTRA_BIASING_STRINGS,
                    ArrayList(contextualPhrases(content))
                )
            }
        }

    private fun stopInternal(nextState: SpeechFollowerState) {
        sessionToken += 1
        mainHandler.removeCallbacksAndMessages(null)
        destroyRecognizer()
        state = nextState
    }

    private fun destroyRecognizer() {
        recognizer?.setRecognitionListener(null)
        recognizer?.cancel()
        recognizer?.destroy()
        recognizer = null
    }

    private fun fail(message: String) {
        sessionToken += 1
        mainHandler.removeCallbacksAndMessages(null)
        destroyRecognizer()
        state = if (message == "语音权限未开启") {
            SpeechFollowerState.Denied
        } else {
            SpeechFollowerState.Failed(message)
        }
    }

    private fun contextualPhrases(content: String): List<String> {
        val separators = charArrayOf('\n', '，', '。', '！', '？', '；', ',', '.', '!', '?', ';')
        return content
            .split(*separators)
            .map { it.trim() }
            .filter { it.length >= 2 }
            .take(80)
    }

    private companion object {
        private const val RESTART_DELAY_MILLIS = 150L
        private const val END_OF_SPEECH_FALLBACK_DELAY_MILLIS = 800L
        private const val MAX_BUSY_COUNT = 5
        private const val MAX_NETWORK_ERROR_COUNT = 4
        private const val MAX_RECOVERABLE_ERROR_COUNT = 10
    }
}

sealed interface SpeechFollowerState {
    data object Idle : SpeechFollowerState
    data object Starting : SpeechFollowerState
    data object Listening : SpeechFollowerState
    data object RestartPending : SpeechFollowerState
    data object Denied : SpeechFollowerState
    data object Unavailable : SpeechFollowerState
    data object Disposed : SpeechFollowerState
    data class Failed(val message: String) : SpeechFollowerState
}
