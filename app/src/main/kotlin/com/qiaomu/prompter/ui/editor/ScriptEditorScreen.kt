package com.qiaomu.prompter.ui.editor

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.qiaomu.prompter.data.Script
import com.qiaomu.prompter.data.ScriptRepository
import com.qiaomu.prompter.data.TextColorPreset
import com.qiaomu.prompter.ui.component.GlassActionPanel
import com.qiaomu.prompter.ui.component.GlassActionRow
import com.qiaomu.prompter.ui.component.GlassPanelHeader
import kotlinx.coroutines.launch

private enum class EditorTab(val title: String) {
    Script("文稿"),
    Display("显示")
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScriptEditorScreen(
    scriptId: String,
    scriptRepository: ScriptRepository,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val scripts by scriptRepository.scripts.collectAsState()
    val scope = rememberCoroutineScope()
    val clipboardManager = LocalClipboardManager.current

    var script by remember(scriptId) { mutableStateOf<Script?>(null) }
    var selectedTab by remember { mutableStateOf(EditorTab.Script) }
    var showTitleDialog by remember { mutableStateOf(false) }
    var titleDraft by remember { mutableStateOf("") }
    var showClearPanel by remember { mutableStateOf(false) }

    LaunchedEffect(scriptId, scripts) {
        if (script == null) {
            script = scripts.firstOrNull { it.id == scriptId }
        }
    }

    fun normalized(current: Script): Script =
        current.copy(
            title = current.title.trim().ifEmpty { Script.UNTITLED },
            fontSize = current.fontSize.coerceIn(12.0, 110.0),
            scrollSpeed = current.scrollSpeed.coerceIn(20.0, 220.0),
            textColorPreset = if (current.textColorPreset in TextColorPreset.editorChoices) {
                current.textColorPreset
            } else {
                TextColorPreset.White
            },
            overlayOpacity = current.overlayOpacity.coerceIn(0.18, 0.82)
        )

    fun saveAndThen(afterSave: () -> Unit = {}) {
        val current = script ?: return
        val toSave = normalized(current)
        script = toSave
        scope.launch {
            scriptRepository.save(toSave)
            afterSave()
        }
    }

    BackHandler {
        saveAndThen(onBack)
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = {
                    Text(script?.title?.ifBlank { Script.UNTITLED } ?: "文稿")
                },
                navigationIcon = {
                    IconButton(onClick = { saveAndThen(onBack) }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "返回")
                    }
                },
                actions = {
                    IconButton(onClick = {
                        titleDraft = script?.title?.ifBlank { Script.UNTITLED }.orEmpty()
                        showTitleDialog = true
                    }) {
                        Icon(Icons.Default.Edit, contentDescription = "编辑标题")
                    }
                    IconButton(onClick = { saveAndThen() }) {
                        Icon(Icons.Default.Save, contentDescription = "保存")
                    }
                }
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            val current = script
            if (current == null) {
                MissingScriptState()
            } else {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    TabRow(selectedTabIndex = selectedTab.ordinal) {
                        EditorTab.entries.forEach { tab ->
                            Tab(
                                selected = selectedTab == tab,
                                onClick = { selectedTab = tab },
                                text = { Text(tab.title) }
                            )
                        }
                    }

                    when (selectedTab) {
                        EditorTab.Script -> ScriptBodyEditor(
                            script = current,
                            onScriptChange = { script = it },
                            onPaste = {
                                val pasted = clipboardManager.getText()?.text.orEmpty()
                                if (pasted.trim().isNotEmpty()) {
                                    script = current.copy(
                                        content = if (current.content.trim().isEmpty()) {
                                            pasted
                                        } else {
                                            current.content + "\n" + pasted
                                        }
                                    )
                                }
                            },
                            onClear = { showClearPanel = true },
                            modifier = Modifier.weight(1f)
                        )

                        EditorTab.Display -> DisplaySettingsEditor(
                            script = current,
                            onScriptChange = { script = it },
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Button(
                        onClick = { saveAndThen() },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.Save, contentDescription = null)
                        Text("保存", modifier = Modifier.padding(start = 8.dp))
                    }
                }

                GlassActionPanel(
                    visible = showClearPanel,
                    onDismiss = { showClearPanel = false }
                ) {
                    GlassPanelHeader(
                        title = "清空正文",
                        onDismiss = { showClearPanel = false }
                    )
                    Text(
                        text = "正文会被清空，文稿名和显示设置会保留。",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    GlassActionRow(
                        icon = Icons.Default.Delete,
                        title = "清空正文",
                        subtitle = null,
                        destructive = true
                    ) {
                        script = current.copy(content = "")
                        showClearPanel = false
                    }
                }
            }
        }
    }

    if (showTitleDialog) {
        AlertDialog(
            onDismissRequest = { showTitleDialog = false },
            title = { Text("编辑标题") },
            text = {
                OutlinedTextField(
                    value = titleDraft,
                    onValueChange = { titleDraft = it },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        script = script?.copy(title = titleDraft.trim().ifEmpty { Script.UNTITLED })
                        showTitleDialog = false
                    }
                ) {
                    Text("完成")
                }
            },
            dismissButton = {
                TextButton(onClick = { showTitleDialog = false }) {
                    Text("取消")
                }
            }
        )
    }
}

@Composable
private fun ScriptBodyEditor(
    script: Script,
    onScriptChange: (Script) -> Unit,
    onPaste: () -> Unit,
    onClear: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Button(
                onClick = onPaste,
                modifier = Modifier.weight(1f)
            ) {
                Icon(Icons.Default.ContentPaste, contentDescription = null)
                Text("粘贴正文", modifier = Modifier.padding(start = 8.dp))
            }
            Button(
                onClick = onClear,
                modifier = Modifier.weight(1f),
                enabled = script.content.isNotEmpty()
            ) {
                Icon(Icons.Default.Delete, contentDescription = null)
                Text("清空正文", modifier = Modifier.padding(start = 8.dp))
            }
        }
        OutlinedTextField(
            value = script.content,
            onValueChange = { onScriptChange(script.copy(content = it)) },
            placeholder = { Text("在这里输入提词正文") },
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
        )
    }
}

@Composable
private fun DisplaySettingsEditor(
    script: Script,
    onScriptChange: (Script) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(22.dp)
    ) {
        SliderSetting(
            title = "字号",
            valueLabel = script.fontSize.toInt().toString()
        ) {
            Slider(
                value = script.fontSize.toFloat(),
                onValueChange = { onScriptChange(script.copy(fontSize = it.toDouble())) },
                valueRange = 12f..110f,
                steps = 97
            )
        }
        SliderSetting(
            title = "滚动速度",
            valueLabel = script.scrollSpeed.toInt().toString()
        ) {
            Slider(
                value = script.scrollSpeed.toFloat(),
                onValueChange = { onScriptChange(script.copy(scrollSpeed = it.toDouble())) },
                valueRange = 20f..220f,
                steps = 99
            )
        }
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(
                text = "文字颜色",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold
            )
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                TextColorPreset.editorChoices.forEach { preset ->
                    FilterChip(
                        selected = script.textColorPreset == preset,
                        onClick = { onScriptChange(script.copy(textColorPreset = preset)) },
                        label = { Text(preset.displayName) },
                        leadingIcon = {
                            Box(
                                modifier = Modifier
                                    .padding(start = 2.dp)
                                    .background(preset.toUiColor(), MaterialTheme.shapes.small)
                                    .padding(7.dp)
                            )
                        }
                    )
                }
            }
        }
        val transparency = (1.0 - script.overlayOpacity).coerceIn(0.18, 0.82)
        SliderSetting(
            title = "摄像头透明度",
            valueLabel = "${(transparency * 100).toInt()}%"
        ) {
            Slider(
                value = transparency.toFloat(),
                onValueChange = {
                    val nextOverlay = (1.0 - it.toDouble()).coerceIn(0.18, 0.82)
                    onScriptChange(script.copy(overlayOpacity = nextOverlay))
                },
                valueRange = 0.18f..0.82f,
                steps = 31
            )
        }
    }
}

@Composable
private fun SliderSetting(
    title: String,
    valueLabel: String,
    content: @Composable () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.weight(1f)
            )
            Text(
                text = valueLabel,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        content()
    }
}

@Composable
private fun MissingScriptState() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "正在加载文稿",
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

private fun TextColorPreset.toUiColor(): Color =
    when (this) {
        TextColorPreset.White -> Color.White
        TextColorPreset.Silver -> Color(0xFFC7CBD1)
        TextColorPreset.Graphite -> Color(0xFF7F858C)
    }
