package com.qiaomu.prompter.speech

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.content.ContextCompat
import java.util.Locale

class PromptDictation(
    context: Context
) {
    var isRecording by mutableStateOf(false)
        private set
    var transcript by mutableStateOf("")
        private set
    var errorMessage by mutableStateOf<String?>(null)
        private set

    private val appContext = context.applicationContext
    private var recognizer: SpeechRecognizer? = null

    fun toggle() {
        if (isRecording) {
            stop()
        } else {
            start()
        }
    }

    fun start() {
        stop()
        errorMessage = null
        transcript = ""

        if (ContextCompat.checkSelfPermission(appContext, Manifest.permission.RECORD_AUDIO) !=
            PackageManager.PERMISSION_GRANTED
        ) {
            errorMessage = "麦克风权限未开启。"
            return
        }

        if (!SpeechRecognizer.isRecognitionAvailable(appContext)) {
            errorMessage = "当前语音识别不可用。"
            return
        }

        recognizer = SpeechRecognizer.createSpeechRecognizer(appContext).also { speechRecognizer ->
            speechRecognizer.setRecognitionListener(object : RecognitionListener {
                override fun onReadyForSpeech(params: Bundle?) = Unit
                override fun onBeginningOfSpeech() = Unit
                override fun onRmsChanged(rmsdB: Float) = Unit
                override fun onBufferReceived(buffer: ByteArray?) = Unit
                override fun onEndOfSpeech() = Unit

                override fun onError(error: Int) {
                    if (isRecording) {
                        errorMessage = when (error) {
                            SpeechRecognizer.ERROR_NO_MATCH,
                            SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> "没有识别到语音。"
                            SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> "麦克风权限未开启。"
                            else -> "语音输入中断。"
                        }
                    }
                    stop()
                }

                override fun onResults(results: Bundle?) {
                    updateTranscript(results)
                    stop()
                }

                override fun onPartialResults(partialResults: Bundle?) {
                    updateTranscript(partialResults)
                }

                override fun onEvent(eventType: Int, params: Bundle?) = Unit
            })
        }

        try {
            recognizer?.startListening(recognizerIntent())
            isRecording = true
        } catch (exception: RuntimeException) {
            errorMessage = "语音输入启动失败。"
            stop()
        }
    }

    fun stop() {
        isRecording = false
        recognizer?.setRecognitionListener(null)
        recognizer?.cancel()
        recognizer?.destroy()
        recognizer = null
    }

    private fun updateTranscript(results: Bundle?) {
        val best = results
            ?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
            ?.firstOrNull()
            .orEmpty()
        if (best.isNotBlank()) {
            transcript = best
        }
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
        }
}
