package com.qiaomu.prompter.ui.scriptlist

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.qiaomu.prompter.data.Script
import com.qiaomu.prompter.data.ScriptRepository
import com.qiaomu.prompter.ui.component.GlassActionPanel
import com.qiaomu.prompter.ui.component.GlassActionRow
import com.qiaomu.prompter.ui.component.GlassPanelHeader
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScriptListScreen(
    scriptRepository: ScriptRepository,
    onOpenScript: (String) -> Unit,
    onOpenSettings: () -> Unit,
    onOpenSpeechPoc: () -> Unit,
    modifier: Modifier = Modifier
) {
    val scripts by scriptRepository.scripts.collectAsState()
    val scope = rememberCoroutineScope()
    var searchText by remember { mutableStateOf("") }
    var showCreatePanel by remember { mutableStateOf(false) }

    val filteredScripts = remember(scripts, searchText) {
        val query = searchText.trim()
        if (query.isEmpty()) {
            scripts
        } else {
            scripts.filter { script ->
                script.title.contains(query, ignoreCase = true) ||
                    script.content.contains(query, ignoreCase = true)
            }
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = { Text("乔木提词器") },
                actions = {
                    IconButton(onClick = onOpenSpeechPoc) {
                        Icon(Icons.Default.Mic, contentDescription = "语音测试")
                    }
                    IconButton(onClick = onOpenSettings) {
                        Icon(Icons.Default.Settings, contentDescription = "设置")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { showCreatePanel = true }) {
                Icon(Icons.Default.Add, contentDescription = "新建")
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
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                OutlinedTextField(
                    value = searchText,
                    onValueChange = { searchText = it },
                    placeholder = { Text("搜索文稿") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                if (filteredScripts.isEmpty()) {
                    EmptyScriptsState(
                        isSearching = searchText.isNotBlank(),
                        modifier = Modifier.weight(1f)
                    )
                } else {
                    LazyColumn(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(
                            items = filteredScripts,
                            key = { it.id }
                        ) { script ->
                            val dismissState = rememberSwipeToDismissBoxState(
                                confirmValueChange = { value ->
                                    if (value != SwipeToDismissBoxValue.Settled) {
                                        scope.launch { scriptRepository.delete(script) }
                                        true
                                    } else {
                                        false
                                    }
                                }
                            )
                            SwipeToDismissBox(
                                state = dismissState,
                                backgroundContent = {
                                    DeleteBackground(
                                        modifier = Modifier.fillMaxSize()
                                    )
                                }
                            ) {
                                ScriptCard(
                                    script = script,
                                    onClick = { onOpenScript(script.id) }
                                )
                            }
                        }
                    }
                }
            }

            GlassActionPanel(
                visible = showCreatePanel,
                onDismiss = { showCreatePanel = false }
            ) {
                GlassPanelHeader(
                    title = "新建文稿",
                    onDismiss = { showCreatePanel = false }
                )
                GlassActionRow(
                    icon = Icons.Default.Description,
                    title = "空白文稿",
                    subtitle = "从标题和正文开始编辑"
                ) {
                    showCreatePanel = false
                    val draft = Script.createDraft()
                    scope.launch {
                        scriptRepository.save(draft)
                        onOpenScript(draft.id)
                    }
                }
            }
        }
    }
}

@Composable
private fun EmptyScriptsState(
    isSearching: Boolean,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier.fillMaxWidth(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.padding(24.dp)
        ) {
            Text(
                text = if (isSearching) "没有匹配的文稿" else "还没有文稿",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = if (isSearching) "换个关键词试试" else "点击右下角新建",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun DeleteBackground(
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 18.dp),
        horizontalArrangement = Arrangement.End,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Spacer(modifier = Modifier.weight(1f))
        Icon(
            imageVector = Icons.Default.Delete,
            contentDescription = null,
            tint = Color.White,
            modifier = Modifier.size(26.dp)
        )
    }
}
