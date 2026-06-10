package com.qiaomu.prompter.poc

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.media.AudioManager
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import java.util.Locale
import kotlin.math.max

class SpeechRecognizerPocActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    SpeechRecognizerPocScreen()
                }
            }
        }
    }
}

@Composable
private fun SpeechRecognizerPocScreen() {
    val context = LocalContext.current
    val controller = remember {
        SpeechRecognizerPocController(context.applicationContext)
    }
    var hasPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.RECORD_AUDIO
            ) == PackageManager.PERMISSION_GRANTED
        )
    }
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasPermission = granted
    }

    DisposableEffect(controller) {
        onDispose {
            controller.dispose()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text("SpeechRecognizer PoC", style = MaterialTheme.typography.headlineSmall)
        Text(
            text = "Disposable Phase 1.5 screen for beep, busy, and restart-gap validation.",
            style = MaterialTheme.typography.bodyMedium
        )

        HorizontalDivider()

        if (!hasPermission) {
            Text("Microphone permission is required.")
            Button(onClick = { permissionLauncher.launch(Manifest.permission.RECORD_AUDIO) }) {
                Text("Grant microphone")
            }
        } else {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Button(
                    enabled = !controller.isRunning,
                    onClick = { controller.start() }
                ) {
                    Text("Start")
                }
                OutlinedButton(
                    enabled = controller.isRunning,
                    onClick = { controller.stop() }
                ) {
                    Text("Stop")
                }
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text("Mute beep while listening")
            Switch(
                checked = controller.muteBeep,
                onCheckedChange = { controller.muteBeep = it }
            )
        }

        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Button(
                enabled = controller.restartStrategy != RestartStrategy.Delay150Ms,
                onClick = { controller.restartStrategy = RestartStrategy.Delay150Ms }
            ) {
                Text("Delay 150ms")
            }
            Button(
                enabled = controller.restartStrategy != RestartStrategy.DestroyRecreate,
                onClick = { controller.restartStrategy = RestartStrategy.DestroyRecreate }
            ) {
                Text("Destroy/recreate")
            }
        }

        HorizontalDivider()

        PocRow("State", controller.state.label)
        PocRow("Strategy", controller.restartStrategy.label)
        PocRow("Speech available", controller.isRecognitionAvailable.toString())
        PocRow("Restart count", controller.restartCount.toString())
        PocRow("Busy count", controller.busyCount.toString())
        PocRow("Failure count", controller.failureCount.toString())
        PocRow("Last error", controller.lastError)
        PocRow("Last restart gap", controller.lastRestartGapText)
        PocRow("Audio muted", controller.audioMuted.toString())
        PocRow("Volume restored", controller.volumeRestored.toString())

        Spacer(modifier = Modifier.height(4.dp))
        Text("Transcript", style = MaterialTheme.typography.titleMedium)
        Text(
            text = controller.transcript.ifBlank { "(empty)" },
            style = MaterialTheme.typography.bodyLarge
        )

        Spacer(modifier = Modifier.height(4.dp))
        Text("Events", style = MaterialTheme.typography.titleMedium)
        Text(
            text = controller.events.joinToString(separator = "\n").ifBlank { "(none)" },
            style = MaterialTheme.typography.bodySmall
        )
    }
}

@Composable
private fun PocRow(label: String, value: String) {
    Column {
        Text(label, style = MaterialTheme.typography.labelMedium)
        Text(value, style = MaterialTheme.typography.bodyMedium)
    }
}

private class SpeechRecognizerPocController(
    private val context: Context
) {
    var state by mutableStateOf(PocState.Idle)
        private set
    var transcript by mutableStateOf("")
        private set
    var restartCount by mutableStateOf(0)
        private set
    var busyCount by mutableStateOf(0)
        private set
    var failureCount by mutableStateOf(0)
        private set
    var lastError by mutableStateOf("none")
        private set
    var lastRestartGapText by mutableStateOf("none")
        private set
    var audioMuted by mutableStateOf(false)
        private set
    var volumeRestored by mutableStateOf(true)
        private set
    var muteBeep by mutableStateOf(true)
    var restartStrategy by mutableStateOf(RestartStrategy.Delay150Ms)
    var events by mutableStateOf<List<String>>(emptyList())
        private set

    val isRunning: Boolean
        get() = state == PocState.Starting ||
            state == PocState.Listening ||
            state == PocState.RestartPending

    val isRecognitionAvailable: Boolean =
        SpeechRecognizer.isRecognitionAvailable(context)

    private val mainHandler = Handler(Looper.getMainLooper())
    private val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    private val savedVolumes = linkedMapOf<Int, Int>()
    private var recognizer: SpeechRecognizer? = null
    private var disposed = false
    private var userStopped = false
    private var sessionStartedAt = 0L
    private var lastSessionEndedAt: Long? = null

    fun start() {
        if (disposed || isRunning) return
        if (!isRecognitionAvailable) {
            state = PocState.Failed
            lastError = "SpeechRecognizer unavailable"
            appendEvent(lastError)
            return
        }

        userStopped = false
        transcript = ""
        restartCount = 0
        busyCount = 0
        failureCount = 0
        lastError = "none"
        lastRestartGapText = "none"
        events = emptyList()
        startListening()
    }

    fun stop() {
        userStopped = true
        mainHandler.removeCallbacksAndMessages(null)
        destroyRecognizer()
        restoreAudio()
        state = PocState.Idle
        appendEvent("stopped")
    }

    fun dispose() {
        disposed = true
        userStopped = true
        mainHandler.removeCallbacksAndMessages(null)
        destroyRecognizer()
        restoreAudio()
        state = PocState.Disposed
    }

    private fun startListening() {
        if (disposed || userStopped) return
        state = PocState.Starting
        ensureRecognizer()
        if (muteBeep) {
            muteAudio()
        } else {
            restoreAudio()
        }

        val now = System.currentTimeMillis()
        lastSessionEndedAt?.let { endedAt ->
            lastRestartGapText = "${max(0, now - endedAt)} ms"
        }
        sessionStartedAt = now

        try {
            recognizer?.startListening(recognizerIntent())
            state = PocState.Listening
            appendEvent("startListening")
        } catch (exception: RuntimeException) {
            fail("startListening failed: ${exception.message}")
        }
    }

    private fun ensureRecognizer() {
        if (recognizer != null) return
        recognizer = SpeechRecognizer.createSpeechRecognizer(context).also { speechRecognizer ->
            speechRecognizer.setRecognitionListener(object : RecognitionListener {
                override fun onReadyForSpeech(params: Bundle?) {
                    appendEvent("ready")
                }

                override fun onBeginningOfSpeech() {
                    appendEvent("beginning")
                }

                override fun onRmsChanged(rmsdB: Float) = Unit

                override fun onBufferReceived(buffer: ByteArray?) = Unit

                override fun onEndOfSpeech() {
                    appendEvent("endOfSpeech")
                    mainHandler.postDelayed({
                        if (state == PocState.Listening) {
                            scheduleRestart("endOfSpeech fallback")
                        }
                    }, END_OF_SPEECH_FALLBACK_DELAY_MILLIS)
                }

                override fun onError(error: Int) {
                    lastError = errorName(error)
                    appendEvent("error: $lastError")
                    if (error == SpeechRecognizer.ERROR_RECOGNIZER_BUSY) {
                        busyCount += 1
                    } else {
                        failureCount += 1
                    }

                    when (error) {
                        SpeechRecognizer.ERROR_NO_MATCH,
                        SpeechRecognizer.ERROR_SPEECH_TIMEOUT,
                        SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> scheduleRestart("error")
                        else -> fail(lastError)
                    }
                }

                override fun onResults(results: Bundle?) {
                    updateTranscript(results, final = true)
                    appendEvent("results")
                    scheduleRestart("results")
                }

                override fun onPartialResults(partialResults: Bundle?) {
                    updateTranscript(partialResults, final = false)
                }

                override fun onEvent(eventType: Int, params: Bundle?) = Unit
            })
        }
    }

    private fun scheduleRestart(reason: String) {
        if (disposed || userStopped || state == PocState.RestartPending) return
        lastSessionEndedAt = System.currentTimeMillis()
        state = PocState.RestartPending
        restartCount += 1
        appendEvent("restartPending: $reason")

        if (busyCount >= MAX_BUSY_COUNT || failureCount >= MAX_FAILURE_COUNT) {
            fail("too many failures: busy=$busyCount failure=$failureCount")
            return
        }

        val delayMillis = when (restartStrategy) {
            RestartStrategy.Delay150Ms -> 150L
            RestartStrategy.DestroyRecreate -> {
                destroyRecognizer()
                150L
            }
        }

        mainHandler.postDelayed({ startListening() }, delayMillis)
    }

    private fun updateTranscript(results: Bundle?, final: Boolean) {
        val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
        val best = matches?.firstOrNull().orEmpty()
        if (best.isNotBlank()) {
            transcript = if (final) best else "$best ..."
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

    private fun destroyRecognizer() {
        recognizer?.setRecognitionListener(null)
        recognizer?.cancel()
        recognizer?.destroy()
        recognizer = null
    }

    private fun muteAudio() {
        if (audioMuted) return
        val streams = listOf(
            AudioManager.STREAM_MUSIC,
            AudioManager.STREAM_NOTIFICATION,
            AudioManager.STREAM_SYSTEM
        )
        savedVolumes.clear()
        streams.forEach { stream ->
            savedVolumes[stream] = audioManager.getStreamVolume(stream)
            runCatching {
                audioManager.setStreamVolume(stream, 0, 0)
            }.onFailure { exception ->
                appendEvent("mute failed for stream $stream: ${exception.message}")
            }
        }
        audioMuted = true
        volumeRestored = false
        appendEvent("audio muted")
    }

    private fun restoreAudio() {
        if (!audioMuted && savedVolumes.isEmpty()) {
            volumeRestored = true
            return
        }
        savedVolumes.forEach { (stream, volume) ->
            runCatching {
                audioManager.setStreamVolume(stream, volume, 0)
            }.onFailure { exception ->
                appendEvent("restore failed for stream $stream: ${exception.message}")
            }
        }
        savedVolumes.clear()
        audioMuted = false
        volumeRestored = true
        appendEvent("audio restored")
    }

    private fun fail(message: String) {
        mainHandler.removeCallbacksAndMessages(null)
        destroyRecognizer()
        restoreAudio()
        lastError = message
        state = PocState.Failed
        appendEvent("failed: $message")
    }

    private fun appendEvent(message: String) {
        val line = "${System.currentTimeMillis() % 100000}: $message"
        events = (listOf(line) + events).take(40)
    }

    private fun errorName(error: Int): String =
        when (error) {
            SpeechRecognizer.ERROR_AUDIO -> "ERROR_AUDIO"
            SpeechRecognizer.ERROR_CLIENT -> "ERROR_CLIENT"
            SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> "ERROR_INSUFFICIENT_PERMISSIONS"
            SpeechRecognizer.ERROR_NETWORK -> "ERROR_NETWORK"
            SpeechRecognizer.ERROR_NETWORK_TIMEOUT -> "ERROR_NETWORK_TIMEOUT"
            SpeechRecognizer.ERROR_NO_MATCH -> "ERROR_NO_MATCH"
            SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> "ERROR_RECOGNIZER_BUSY"
            SpeechRecognizer.ERROR_SERVER -> "ERROR_SERVER"
            SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> "ERROR_SPEECH_TIMEOUT"
            else -> "ERROR_$error"
        }

    companion object {
        private const val MAX_BUSY_COUNT = 5
        private const val MAX_FAILURE_COUNT = 8
        private const val END_OF_SPEECH_FALLBACK_DELAY_MILLIS = 800L
    }
}

private enum class PocState(val label: String) {
    Idle("Idle"),
    Starting("Starting"),
    Listening("Listening"),
    RestartPending("RestartPending"),
    Failed("Failed"),
    Disposed("Disposed")
}

private enum class RestartStrategy(val label: String) {
    Delay150Ms("Delay 150ms"),
    DestroyRecreate("Destroy/recreate")
}
