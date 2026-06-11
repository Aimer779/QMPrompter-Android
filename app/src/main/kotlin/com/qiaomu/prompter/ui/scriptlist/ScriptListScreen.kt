package com.qiaomu.prompter.ui.scriptlist

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.qiaomu.prompter.data.Script
import com.qiaomu.prompter.data.ScriptRepository
import com.qiaomu.prompter.ui.component.GlassActionPanel
import com.qiaomu.prompter.ui.component.GlassActionRow
import com.qiaomu.prompter.ui.component.GlassPanelHeader
import com.qiaomu.prompter.ui.component.glassSurface
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScriptListScreen(
    scriptRepository: ScriptRepository,
    onOpenScript: (String) -> Unit,
    onOpenSettings: () -> Unit,
    onOpenAiGeneration: () -> Unit,
    modifier: Modifier = Modifier
) {
    val scripts by scriptRepository.scripts.collectAsState()
    val scope = rememberCoroutineScope()
    var searchText by remember { mutableStateOf("") }
    var showCreatePanel by remember { mutableStateOf(false) }
    var pendingDeleteScript by remember { mutableStateOf<Script?>(null) }

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
                title = {
                    Text(
                        text = "prompter",
                        style = MaterialTheme.typography.headlineSmall,
                        fontFamily = FontFamily.Cursive,
                        fontWeight = FontWeight.SemiBold
                    )
                },
                actions = {
                    IconButton(
                        onClick = onOpenSettings,
                        modifier = Modifier
                            .size(42.dp)
                            .glassSurface(CircleShape)
                    ) {
                        Icon(Icons.Default.Settings, contentDescription = "设置")
                    }
                }
            )
        },
        floatingActionButton = {
            if (!showCreatePanel && pendingDeleteScript == null) {
                Box(
                    modifier = Modifier
                        .size(58.dp)
                        .glassSurface(CircleShape)
                        .clickable(onClick = { showCreatePanel = true }),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "新建",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(30.dp)
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
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                GlassSearchField(
                    value = searchText,
                    onValueChange = { searchText = it },
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
                                        scope.launch {
                                            delay(120)
                                            pendingDeleteScript = script
                                        }
                                    }
                                    false
                                }
                            )
                            SwipeToDismissBox(
                                state = dismissState,
                                backgroundContent = {},
                                enableDismissFromStartToEnd = false
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
                    icon = Icons.Default.AutoAwesome,
                    title = "AI 生成",
                    subtitle = "根据想法生成口播文稿"
                ) {
                    showCreatePanel = false
                    onOpenAiGeneration()
                }
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

            val deleteScript = pendingDeleteScript
            if (deleteScript != null) {
                DeleteConfirmDialog(
                    scriptTitle = deleteScript.title.ifBlank { Script.UNTITLED },
                    onDismiss = { pendingDeleteScript = null },
                    onConfirm = {
                        scope.launch { scriptRepository.delete(deleteScript) }
                        pendingDeleteScript = null
                    }
                )
            }
        }
    }
}

@Composable
private fun DeleteConfirmDialog(
    scriptTitle: String,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.36f))
            .clickable(onClick = onDismiss)
            .padding(horizontal = 28.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .confirmDialogSurface(RoundedCornerShape(24.dp))
                .clickable(enabled = false) {}
                .padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "删除文稿",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = "删除后无法恢复。确定要删除“$scriptTitle”吗？",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                ConfirmDialogButton(
                    text = "取消",
                    onClick = onDismiss,
                    modifier = Modifier.weight(1f)
                )
                ConfirmDialogButton(
                    text = "删除",
                    onClick = onConfirm,
                    destructive = true,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

private fun Modifier.confirmDialogSurface(shape: RoundedCornerShape): Modifier =
    this
        .clip(shape)
        .background(Color(0xFFF8F3FA).copy(alpha = 0.96f))
        .border(
            width = 1.dp,
            brush = Brush.linearGradient(
                colors = listOf(
                    Color.White.copy(alpha = 0.86f),
                    Color.White.copy(alpha = 0.26f),
                    Color.Black.copy(alpha = 0.12f)
                )
            ),
            shape = shape
        )

@Composable
private fun ConfirmDialogButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    destructive: Boolean = false
) {
    Box(
        modifier = modifier
            .height(46.dp)
            .glassSurface(RoundedCornerShape(16.dp))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            color = if (destructive) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface,
            fontWeight = FontWeight.SemiBold
        )
    }
}

@Composable
private fun GlassSearchField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    height: Dp = 56.dp
) {
    val shape = RoundedCornerShape(16.dp)
    BasicTextField(
        value = value,
        onValueChange = onValueChange,
        singleLine = true,
        textStyle = MaterialTheme.typography.bodyLarge.merge(
            TextStyle(color = MaterialTheme.colorScheme.onSurface)
        ),
        modifier = modifier
            .height(height)
            .clip(shape)
            .glassSurface(shape),
        decorationBox = { innerTextField ->
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(PaddingValues(horizontal = 16.dp)),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Search,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(22.dp)
                )
                Box(
                    modifier = Modifier.weight(1f),
                    contentAlignment = Alignment.CenterStart
                ) {
                    if (value.isEmpty()) {
                        Text(
                            text = "搜索文稿",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    innerTextField()
                }
            }
        }
    )
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
