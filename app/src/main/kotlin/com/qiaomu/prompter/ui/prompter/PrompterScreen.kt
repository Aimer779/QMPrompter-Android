package com.qiaomu.prompter.ui.prompter

import android.app.Activity
import android.view.WindowManager
import androidx.camera.core.CameraSelector
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Cameraswitch
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.VideocamOff
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.qiaomu.prompter.data.Script
import com.qiaomu.prompter.data.ScriptRepository
import com.qiaomu.prompter.data.TextColorPreset
import com.qiaomu.prompter.ui.camera.CameraPermissionState
import com.qiaomu.prompter.ui.camera.CameraPreview
import com.qiaomu.prompter.util.PromptFormatter
import com.qiaomu.prompter.util.PromptLine
import kotlinx.coroutines.launch
import kotlin.math.max
import kotlin.math.roundToInt

@Composable
fun PrompterScreen(
    scriptId: String,
    scriptRepository: ScriptRepository,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val scripts by scriptRepository.scripts.collectAsState()
    val script = scripts.firstOrNull { it.id == scriptId }

    if (script == null) {
        Surface(modifier = modifier.fillMaxSize(), color = Color.Black) {
            Box(contentAlignment = Alignment.Center) {
                Text("文稿不存在", color = Color.White)
            }
        }
        return
    }

    PrompterContent(
        script = script,
        scriptRepository = scriptRepository,
        onBack = onBack,
        modifier = modifier
    )
}

@Composable
private fun PrompterContent(
    script: Script,
    scriptRepository: ScriptRepository,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val activity = context as? Activity
    val scope = rememberCoroutineScope()
    val engine = remember(script.id) { ScrollEngine(script.scrollSpeed) }
    var cameraState by remember { mutableStateOf(CameraPermissionState.Checking) }
    var lensFacing by remember { mutableStateOf(CameraSelector.LENS_FACING_FRONT) }
    var lastAvailableLensFacing by remember { mutableStateOf(CameraSelector.LENS_FACING_FRONT) }
    var dragMode by remember { mutableStateOf<DragMode?>(null) }
    var dragStartSpeed by remember { mutableDoubleStateOf(0.0) }
    var dragStartOffset by remember { mutableDoubleStateOf(0.0) }
    var dragTranslationY by remember { mutableDoubleStateOf(0.0) }
    var viewportHeight by remember { mutableStateOf(0) }

    LaunchedEffect(engine) {
        engine.runFrameLoop()
    }

    DisposableEffect(activity) {
        activity?.window?.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        onDispose {
            activity?.window?.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
    }

    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black)
            .onSizeChanged { viewportHeight = it.height }
    ) {
        val density = LocalDensity.current
        val widthPx = constraints.maxWidth
        val heightPx = constraints.maxHeight
        val textMeasurer = rememberTextMeasurer()
        val horizontalPaddingPx = with(density) { 20.dp.roundToPx() }
        val fontSizeSp = script.fontSize.toFloat().sp
        val lineHeightSp = (script.fontSize * 1.18).toFloat().sp
        val targetCharacters = targetCharactersPerLine(
            widthPx = widthPx,
            fontSizePx = with(density) { fontSizeSp.toPx() }
        )
        val promptLines = remember(script.content, targetCharacters) {
            PromptFormatter.lines(script.content, targetCharacters)
        }
        val textStyle = TextStyle(
            color = script.textColorPreset.promptColor(),
            fontSize = fontSizeSp,
            fontWeight = FontWeight.SemiBold,
            lineHeight = lineHeightSp,
            textAlign = TextAlign.Center
        )
        val layouts = remember(promptLines, textStyle, widthPx, horizontalPaddingPx) {
            promptLineLayouts(
                lines = promptLines,
                textStyle = textStyle,
                textMeasurer = textMeasurer,
                maxWidthPx = max(1, widthPx - horizontalPaddingPx * 2)
            )
        }
        val contentHeight = layouts.lastOrNull()?.let { it.y + it.height } ?: 0.0
        val averageLineHeight = layouts
            .takeIf { it.isNotEmpty() }
            ?.map { it.height }
            ?.average()
            ?.coerceAtLeast(with(density) { lineHeightSp.toPx().toDouble() })
            ?: with(density) { lineHeightSp.toPx().toDouble() }
        val topPadding = heightPx * 0.40
        val bottomPadding = heightPx * 0.34
        val maximumOffset = max(0.0, contentHeight + topPadding + bottomPadding - heightPx)
        val averageCharacters = promptLines
            .takeIf { it.isNotEmpty() }
            ?.map { it.characterCount }
            ?.average()
            ?: targetCharacters.toDouble()

        LaunchedEffect(script.scrollSpeed, averageLineHeight, averageCharacters, maximumOffset) {
            engine.configure(
                speed = script.scrollSpeed,
                lineHeight = averageLineHeight,
                averageCharactersPerLine = averageCharacters,
                maximumOffset = maximumOffset
            )
        }

        CameraPreview(
            lensFacing = lensFacing,
            onPermissionStateChange = {
                cameraState = it
                if (it == CameraPermissionState.Authorized) {
                    lastAvailableLensFacing = lensFacing
                }
            },
            onCameraUnavailable = {
                lensFacing = lastAvailableLensFacing
            },
            modifier = Modifier.fillMaxSize()
        )

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Color.Black.copy(
                        alpha = if (cameraState == CameraPermissionState.Authorized) {
                            script.overlayOpacity.toFloat()
                        } else {
                            0.78f
                        }
                    )
                )
        )

        if (cameraState != CameraPermissionState.Authorized) {
            CameraStatus(
                state = cameraState,
                modifier = Modifier.align(Alignment.Center)
            )
        }

        PromptLayer(
            lines = promptLines,
            layouts = layouts,
            textStyle = textStyle,
            topPadding = topPadding,
            viewportHeight = heightPx.toDouble(),
            engine = engine,
            modifier = Modifier
                .fillMaxSize()
                .clipToBounds()
        )

        GestureLayer(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = 120.dp),
            onTap = { engine.toggle() },
            onDragStart = { mode ->
                dragMode = mode
                dragStartSpeed = engine.speed
                dragStartOffset = engine.offset
                dragTranslationY = 0.0
            },
            onDrag = { dragAmount ->
                dragTranslationY += dragAmount
                when (dragMode) {
                    DragMode.Speed -> {
                        engine.updateSpeed(dragStartSpeed - dragTranslationY * 0.42)
                    }
                    DragMode.ManualScroll -> {
                        engine.updateOffset(dragStartOffset - dragTranslationY * 1.05)
                    }
                    DragMode.Progress -> {
                        engine.updateOffset(dragStartOffset - dragTranslationY * 1.35)
                    }
                    null -> Unit
                }
            },
            onDragEnd = {
                if (dragMode == DragMode.Speed && engine.speed != script.scrollSpeed) {
                    scope.launch {
                        scriptRepository.save(script.copy(scrollSpeed = engine.speed))
                    }
                }
                dragMode = null
                dragTranslationY = 0.0
            }
        )

        PrompterControls(
            isPlaying = engine.isPlaying,
            cameraEnabled = cameraState == CameraPermissionState.Authorized ||
                cameraState == CameraPermissionState.Unavailable,
            onBack = {
                engine.pause()
                onBack()
            },
            onToggle = { engine.toggle() },
            onFlipCamera = {
                lensFacing = if (lensFacing == CameraSelector.LENS_FACING_FRONT) {
                    CameraSelector.LENS_FACING_BACK
                } else {
                    CameraSelector.LENS_FACING_FRONT
                }
            },
            modifier = Modifier
                .align(Alignment.TopCenter)
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp)
        )

        if (dragMode != null) {
            Text(
                text = dragStatusText(dragMode, engine, maximumOffset),
                color = Color.White,
                style = MaterialTheme.typography.labelLarge,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 28.dp)
                    .background(Color.Black.copy(alpha = 0.52f), MaterialTheme.shapes.small)
                    .padding(horizontal = 14.dp, vertical = 8.dp)
            )
        }
    }
}

@Composable
private fun PromptLayer(
    lines: List<PromptLine>,
    layouts: List<PromptLineLayout>,
    textStyle: TextStyle,
    topPadding: Double,
    viewportHeight: Double,
    engine: ScrollEngine,
    modifier: Modifier = Modifier
) {
    val visibleRange by remember(layouts, viewportHeight, topPadding) {
        derivedStateOf {
            visibleLayoutRange(
                layouts = layouts,
                offset = engine.offset,
                topPadding = topPadding,
                viewportHeight = viewportHeight
            )
        }
    }

    Box(modifier = modifier) {
        visibleRange.forEach { index ->
            val layout = layouts[index]
            Text(
                text = lines[layout.index].text,
                style = textStyle,
                color = textStyle.color.copy(alpha = 0.72f),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
                    .graphicsLayer {
                        translationY = (topPadding + layout.y - engine.offset).toFloat()
                    },
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
private fun GestureLayer(
    onTap: () -> Unit,
    onDragStart: (DragMode) -> Unit,
    onDrag: (Float) -> Unit,
    onDragEnd: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(modifier = modifier) {
        DragZone(
            mode = DragMode.Speed,
            onTap = onTap,
            onDragStart = onDragStart,
            onDrag = onDrag,
            onDragEnd = onDragEnd,
            modifier = Modifier.weight(1f)
        )
        DragZone(
            mode = DragMode.ManualScroll,
            onTap = onTap,
            onDragStart = onDragStart,
            onDrag = onDrag,
            onDragEnd = onDragEnd,
            modifier = Modifier.weight(1f)
        )
        DragZone(
            mode = DragMode.Progress,
            onTap = onTap,
            onDragStart = onDragStart,
            onDrag = onDrag,
            onDragEnd = onDragEnd,
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun DragZone(
    mode: DragMode,
    onTap: () -> Unit,
    onDragStart: (DragMode) -> Unit,
    onDrag: (Float) -> Unit,
    onDragEnd: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .pointerInput(mode) {
                detectTapGestures(onTap = { onTap() })
            }
            .pointerInput(mode) {
                detectVerticalDragGestures(
                    onDragStart = { onDragStart(mode) },
                    onVerticalDrag = { _, dragAmount -> onDrag(dragAmount) },
                    onDragEnd = onDragEnd,
                    onDragCancel = onDragEnd
                )
            }
    )
}

@Composable
private fun PrompterControls(
    isPlaying: Boolean,
    cameraEnabled: Boolean,
    onBack: () -> Unit,
    onToggle: () -> Unit,
    onFlipCamera: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = onBack) {
            Icon(Icons.Default.ArrowBack, contentDescription = "返回", tint = Color.White)
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            IconButton(onClick = onToggle) {
                Icon(
                    imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                    contentDescription = if (isPlaying) "暂停" else "播放",
                    tint = Color.White
                )
            }
            IconButton(
                onClick = onFlipCamera,
                enabled = cameraEnabled
            ) {
                Icon(
                    imageVector = Icons.Default.Cameraswitch,
                    contentDescription = "切换摄像头",
                    tint = if (cameraEnabled) Color.White else Color.White.copy(alpha = 0.38f)
                )
            }
        }
    }
}

@Composable
private fun CameraStatus(
    state: CameraPermissionState,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.padding(horizontal = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            Icons.Default.VideocamOff,
            contentDescription = null,
            tint = Color.White.copy(alpha = 0.82f),
            modifier = Modifier.size(42.dp)
        )
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            text = when (state) {
                CameraPermissionState.Checking -> "正在请求相机权限"
                CameraPermissionState.Authorized -> ""
                CameraPermissionState.Denied -> "相机权限未开启"
                CameraPermissionState.Unavailable -> "摄像头不可用"
            },
            color = Color.White,
            style = MaterialTheme.typography.titleMedium,
            textAlign = TextAlign.Center
        )
    }
}

private fun promptLineLayouts(
    lines: List<PromptLine>,
    textStyle: TextStyle,
    textMeasurer: androidx.compose.ui.text.TextMeasurer,
    maxWidthPx: Int
): List<PromptLineLayout> {
    var y = 0.0
    return lines.mapIndexed { index, line ->
        val measured = textMeasurer.measure(
            text = line.text,
            style = textStyle,
            constraints = Constraints(maxWidth = maxWidthPx)
        )
        val height = max(1, measured.size.height).toDouble()
        PromptLineLayout(index = index, y = y, height = height).also {
            y += height
        }
    }
}

private fun visibleLayoutRange(
    layouts: List<PromptLineLayout>,
    offset: Double,
    topPadding: Double,
    viewportHeight: Double
): IntRange {
    if (layouts.isEmpty()) return IntRange.EMPTY

    val buffer = viewportHeight * 0.45
    val minY = offset - topPadding - buffer
    val maxY = offset - topPadding + viewportHeight + buffer
    val first = layouts.indexOfFirst { it.y + it.height >= minY }.takeIf { it >= 0 } ?: 0
    val last = layouts.indexOfLast { it.y <= maxY }.takeIf { it >= 0 } ?: layouts.lastIndex
    return first.coerceAtMost(layouts.lastIndex)..last.coerceAtLeast(first)
}

private fun targetCharactersPerLine(widthPx: Int, fontSizePx: Float): Int {
    val estimatedCharacterWidth = max(1f, fontSizePx * 0.58f)
    return (widthPx / estimatedCharacterWidth).roundToInt().coerceIn(8, 32)
}

private fun TextColorPreset.promptColor(): Color =
    when (this) {
        TextColorPreset.White -> Color.White
        TextColorPreset.Silver -> Color(0xFFC9CDD4)
        TextColorPreset.Graphite -> Color(0xFF8E949E)
    }

private fun dragStatusText(mode: DragMode?, engine: ScrollEngine, maximumOffset: Double): String =
    when (mode) {
        DragMode.Speed -> "${engine.speed.roundToInt()} 字/分"
        DragMode.ManualScroll,
        DragMode.Progress -> {
            val progress = if (maximumOffset > 0) engine.offset / maximumOffset else 0.0
            "${(progress * 100).roundToInt().coerceIn(0, 100)}%"
        }
        null -> ""
    }

private data class PromptLineLayout(
    val index: Int,
    val y: Double,
    val height: Double
)

private enum class DragMode {
    Speed,
    ManualScroll,
    Progress
}
