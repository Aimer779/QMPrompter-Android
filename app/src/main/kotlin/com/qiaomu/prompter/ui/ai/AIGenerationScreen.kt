package com.qiaomu.prompter.ui.ai

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Stop
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.qiaomu.prompter.ai.ScriptGenerator
import com.qiaomu.prompter.ai.ScriptPromptStyle
import com.qiaomu.prompter.ai.ScriptPromptStyles
import com.qiaomu.prompter.data.Script
import com.qiaomu.prompter.data.ScriptRepository
import com.qiaomu.prompter.speech.PromptDictation
import com.qiaomu.prompter.ui.component.glassSurface
import kotlinx.coroutines.launch

private val PromptShortcuts = listOf(
    "产品介绍",
    "短视频口播",
    "直播开场",
    "课程讲稿",
    "发布会"
)

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
    var selectedStyle by remember { mutableStateOf(ScriptPromptStyles.QmTalk) }
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
    val toggleDictation = {
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
    val generateScript = {
        val cleanedPrompt = prompt.trim()
        if (cleanedPrompt.isNotEmpty() && !isGenerating) {
            dictation.stop()
            errorMessage = null
            isGenerating = true
            scope.launch {
                try {
                    val content = scriptGenerator.generateScript(
                        prompt = cleanedPrompt,
                        systemPrompt = selectedStyle.systemPrompt
                    )
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
                }
            )
        },
        bottomBar = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(horizontal = 18.dp, vertical = 14.dp)
                    .glassSurface(RoundedCornerShape(26.dp))
                    .padding(12.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .glassSurface(CircleShape)
                        .clickable(enabled = !isGenerating, onClick = toggleDictation),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (dictation.isRecording) Icons.Default.Stop else Icons.Default.Mic,
                        contentDescription = if (dictation.isRecording) "停止输入" else "语音输入",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
                Button(
                    enabled = prompt.trim().isNotEmpty() && !isGenerating,
                    onClick = generateScript,
                    modifier = Modifier.weight(1f)
                ) {
                    if (isGenerating) {
                        CircularProgressIndicator(
                            modifier = Modifier
                                .padding(end = 8.dp)
                                .size(18.dp),
                            strokeWidth = 2.dp
                        )
                    }
                    Text(if (isGenerating) "生成中" else "生成文稿")
                }
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            PromptEditor(
                prompt = prompt,
                enabled = !isGenerating,
                onPromptChange = { prompt = it }
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                PromptShortcuts.forEach { shortcut ->
                    PromptShortcutChip(
                        text = shortcut,
                        enabled = !isGenerating,
                        onClick = {
                            prompt = if (prompt.isBlank()) {
                                shortcut
                            } else {
                                "${prompt.trimEnd()}\n$shortcut"
                            }
                        }
                    )
                }
            }

            PromptStyleSelector(
                styles = ScriptPromptStyles.All,
                selectedStyle = selectedStyle,
                enabled = !isGenerating,
                onStyleSelected = { selectedStyle = it }
            )

            val message = errorMessage ?: dictation.errorMessage
            if (message != null) {
                StatusText(
                    text = message,
                    color = MaterialTheme.colorScheme.error
                )
            }

            if (dictation.isRecording) {
                StatusText(
                    text = "正在听",
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

@Composable
private fun PromptStyleSelector(
    styles: List<ScriptPromptStyle>,
    selectedStyle: ScriptPromptStyle,
    enabled: Boolean,
    onStyleSelected: (ScriptPromptStyle) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .glassSurface(RoundedCornerShape(22.dp))
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Text(
            text = "风格提示词",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            styles.forEach { style ->
                PromptStyleChip(
                    text = style.name,
                    selected = style == selectedStyle,
                    enabled = enabled,
                    onClick = { onStyleSelected(style) }
                )
            }
        }
        Text(
            text = selectedStyle.preview,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun PromptStyleChip(
    text: String,
    selected: Boolean,
    enabled: Boolean,
    onClick: () -> Unit
) {
    val textColor = if (selected) {
        MaterialTheme.colorScheme.primary
    } else {
        MaterialTheme.colorScheme.onSurface
    }

    Box(
        modifier = Modifier
            .glassSurface(RoundedCornerShape(18.dp))
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 9.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold,
            color = textColor
        )
    }
}

@Composable
private fun PromptEditor(
    prompt: String,
    enabled: Boolean,
    onPromptChange: (String) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 320.dp)
            .glassSurface(RoundedCornerShape(28.dp))
            .padding(18.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Text(
            text = "创作提示",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold
        )
        BasicTextField(
            value = prompt,
            onValueChange = onPromptChange,
            enabled = enabled,
            textStyle = MaterialTheme.typography.bodyLarge.merge(
                TextStyle(color = MaterialTheme.colorScheme.onSurface)
            ),
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            decorationBox = { innerTextField ->
                Box(modifier = Modifier.fillMaxSize()) {
                    if (prompt.isBlank()) {
                        Text(
                            text = "说出主题、场景、语气或时长要求",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    innerTextField()
                }
            }
        )
    }
}

@Composable
private fun PromptShortcutChip(
    text: String,
    enabled: Boolean,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .glassSurface(RoundedCornerShape(18.dp))
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 9.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
private fun StatusText(
    text: String,
    color: androidx.compose.ui.graphics.Color
) {
    Text(
        text = text,
        color = color,
        style = MaterialTheme.typography.bodyMedium,
        fontWeight = FontWeight.SemiBold,
        modifier = Modifier
            .fillMaxWidth()
            .glassSurface(RoundedCornerShape(18.dp))
            .padding(horizontal = 14.dp, vertical = 10.dp)
    )
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
