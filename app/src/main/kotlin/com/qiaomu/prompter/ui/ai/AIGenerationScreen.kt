package com.qiaomu.prompter.ui.ai

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.qiaomu.prompter.ai.ScriptGenerator
import com.qiaomu.prompter.data.Script
import com.qiaomu.prompter.data.ScriptRepository
import com.qiaomu.prompter.speech.PromptDictation
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AIGenerationScreen(
    scriptRepository: ScriptRepository,
    scriptGenerator: ScriptGenerator,
    onBack: () -> Unit,
    onGenerated: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val dictation = remember { PromptDictation(context.applicationContext) }
    var prompt by remember { mutableStateOf("") }
    var isGenerating by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    val audioPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            dictation.start()
        } else {
            errorMessage = "麦克风权限未开启。"
        }
    }

    LaunchedEffect(dictation.transcript) {
        if (dictation.transcript.isNotBlank()) {
            prompt = if (prompt.isBlank()) {
                dictation.transcript
            } else if (prompt.contains(dictation.transcript)) {
                prompt
            } else {
                "${prompt.trimEnd()}\n${dictation.transcript}"
            }
        }
    }

    DisposableEffect(dictation) {
        onDispose { dictation.stop() }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = { Text("AI 生成") },
                navigationIcon = {
                    IconButton(onClick = {
                        dictation.stop()
                        onBack()
                    }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "返回")
                    }
                },
                actions = {
                    Button(
                        enabled = prompt.trim().isNotEmpty() && !isGenerating,
                        onClick = {
                            val cleanedPrompt = prompt.trim()
                            if (cleanedPrompt.isEmpty() || isGenerating) return@Button

                            dictation.stop()
                            errorMessage = null
                            isGenerating = true
                            scope.launch {
                                try {
                                    val content = scriptGenerator.generateScript(cleanedPrompt)
                                    val script = Script.create(
                                        title = scriptTitle(cleanedPrompt),
                                        content = content
                                    )
                                    scriptRepository.save(script)
                                    onGenerated(script.id)
                                } catch (exception: Exception) {
                                    errorMessage = exception.message ?: "AI 生成失败。"
                                } finally {
                                    isGenerating = false
                                }
                            }
                        }
                    ) {
                        if (isGenerating) {
                            CircularProgressIndicator(
                                modifier = Modifier.padding(end = 8.dp),
                                strokeWidth = 2.dp
                            )
                        }
                        Text(if (isGenerating) "生成中" else "生成")
                    }
                }
            )
        },
        bottomBar = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 14.dp),
                horizontalArrangement = Arrangement.Center
            ) {
                OutlinedButton(
                    enabled = !isGenerating,
                    onClick = {
                        if (dictation.isRecording) {
                            dictation.stop()
                        } else if (
                            ContextCompat.checkSelfPermission(
                                context,
                                Manifest.permission.RECORD_AUDIO
                            ) == PackageManager.PERMISSION_GRANTED
                        ) {
                            dictation.start()
                        } else {
                            audioPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                        }
                    }
                ) {
                    Icon(
                        imageVector = if (dictation.isRecording) Icons.Default.Stop else Icons.Default.Mic,
                        contentDescription = null
                    )
                    Text(
                        text = if (dictation.isRecording) "停止输入" else "语音输入",
                        modifier = Modifier.padding(start = 8.dp)
                    )
                }
            }
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                OutlinedTextField(
                    value = prompt,
                    onValueChange = { prompt = it },
                    enabled = !isGenerating,
                    placeholder = { Text("输入或说出你想生成的内容") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 260.dp)
                )

                val message = errorMessage ?: dictation.errorMessage
                if (message != null) {
                    Text(
                        text = message,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }

                if (dictation.isRecording) {
                    Text(
                        text = "正在听",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            if (isGenerating) {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
            }
        }
    }
}

private fun scriptTitle(prompt: String): String {
    val cleaned = prompt
        .lineSequence()
        .joinToString(separator = " ")
        .trim()

    if (cleaned.isEmpty()) return "AI 生成文稿"
    val prefix = cleaned.take(16)
    return if (prefix.length < cleaned.length) "$prefix..." else prefix
}
