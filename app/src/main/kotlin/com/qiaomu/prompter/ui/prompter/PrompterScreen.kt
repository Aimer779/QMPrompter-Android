package com.qiaomu.prompter.ui.prompter

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.Settings
import android.view.WindowManager
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.border
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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Cameraswitch
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.VideocamOff
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
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
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.PointerEventPass
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
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.qiaomu.prompter.data.Script
import com.qiaomu.prompter.data.ScriptRepository
import com.qiaomu.prompter.data.TextColorPreset
import com.qiaomu.prompter.speech.SpeechFollower
import com.qiaomu.prompter.ui.camera.CameraPermissionState
import com.qiaomu.prompter.ui.camera.CameraPreview
import com.qiaomu.prompter.ui.component.glassSurface
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
    val speechFollower = remember(script.id) { SpeechFollower(context.applicationContext) }
    var cameraState by remember { mutableStateOf(CameraPermissionState.Checking) }
    var cameraPermissionRequestKey by remember { mutableStateOf(0) }
    var audioPermissionState by remember {
        mutableStateOf(
            if (ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) ==
                PackageManager.PERMISSION_GRANTED
            ) {
                AudioPermissionState.Authorized
            } else {
                AudioPermissionState.NotRequested
            }
        )
    }
    var lensFacing by remember { mutableStateOf(CameraSelector.LENS_FACING_FRONT) }
    var lastAvailableLensFacing by remember { mutableStateOf(CameraSelector.LENS_FACING_FRONT) }
    var dragMode by remember { mutableStateOf<DragMode?>(null) }
    var dragStartSpeed by remember { mutableDoubleStateOf(0.0) }
    var dragStartOffset by remember { mutableDoubleStateOf(0.0) }
    var dragTranslationY by remember { mutableDoubleStateOf(0.0) }
    var pendingSpeechInitialProgress by remember { mutableDoubleStateOf(0.0) }
    var viewportHeight by remember { mutableStateOf(0) }
    var displaySettings by remember(script.id) {
        mutableStateOf(DisplaySettings.fromScript(script))
    }
    var savedDisplaySettings by remember(script.id) {
        mutableStateOf(DisplaySettings.fromScript(script))
    }
    var displayPanelExpanded by remember { mutableStateOf(false) }
    val audioPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            audioPermissionState = AudioPermissionState.Authorized
            speechFollower.start(script.content, initialProgress = pendingSpeechInitialProgress)
        } else {
            audioPermissionState = audioDeniedState(activity)
        }
    }

    fun scriptWithDisplaySettings(settings: DisplaySettings): Script =
        script.copy(
            fontSize = settings.fontSize,
            scrollSpeed = settings.scrollSpeed,
            textColorPreset = settings.textColorPreset,
            overlayOpacity = settings.overlayOpacity
        )

    fun saveDisplaySettings(settings: DisplaySettings = displaySettings) {
        val normalized = settings.normalized()
        displaySettings = normalized
        if (normalized == savedDisplaySettings) return
        scope.launch {
            scriptRepository.save(scriptWithDisplaySettings(normalized))
            savedDisplaySettings = normalized
        }
    }

    fun saveDisplaySettingsAndThen(
        afterSave: () -> Unit,
        settings: DisplaySettings = displaySettings
    ) {
        val normalized = settings.normalized()
        displaySettings = normalized
        scope.launch {
            if (normalized != savedDisplaySettings) {
                scriptRepository.save(scriptWithDisplaySettings(normalized))
                savedDisplaySettings = normalized
            }
            afterSave()
        }
    }
    val latestDisplaySettings by rememberUpdatedState(displaySettings)
    val latestSaveDisplaySettings by rememberUpdatedState<(DisplaySettings) -> Unit>({
        saveDisplaySettings(it)
    })

    fun openAppSettings() {
        val intent = Intent(
            Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
            Uri.fromParts("package", context.packageName, null)
        )
        context.startActivity(intent)
    }

    fun startSpeechFollowing(initialProgress: Double) {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) ==
            PackageManager.PERMISSION_GRANTED
        ) {
            audioPermissionState = AudioPermissionState.Authorized
            speechFollower.start(script.content, initialProgress)
        } else {
            pendingSpeechInitialProgress = initialProgress
            audioPermissionState = AudioPermissionState.Requesting
            audioPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
        }
    }

    BackHandler {
        engine.pause()
        speechFollower.dispose()
        saveDisplaySettingsAndThen(onBack)
    }

    LaunchedEffect(script.fontSize, script.scrollSpeed, script.textColorPreset, script.overlayOpacity) {
        val externalSettings = DisplaySettings.fromScript(script)
        if (displaySettings == savedDisplaySettings && externalSettings != savedDisplaySettings) {
            displaySettings = externalSettings
            savedDisplaySettings = externalSettings
        }
    }

    LaunchedEffect(engine) {
        engine.runFrameLoop()
    }

    DisposableEffect(activity) {
        activity?.window?.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        activity?.window?.let { window ->
            WindowCompat.setDecorFitsSystemWindows(window, false)
            WindowInsetsControllerCompat(window, window.decorView).apply {
                hide(WindowInsetsCompat.Type.systemBars())
                systemBarsBehavior =
                    WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            }
        }
        onDispose {
            speechFollower.dispose()
            activity?.window?.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
            activity?.window?.let { window ->
                WindowInsetsControllerCompat(window, window.decorView)
                    .show(WindowInsetsCompat.Type.systemBars())
                WindowCompat.setDecorFitsSystemWindows(window, true)
            }
        }
    }

    DisposableEffect(context) {
        val lifecycleOwner = activity as? androidx.lifecycle.LifecycleOwner
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME &&
                ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) ==
                PackageManager.PERMISSION_GRANTED
            ) {
                audioPermissionState = AudioPermissionState.Authorized
            }
            if (event == Lifecycle.Event.ON_STOP) {
                latestSaveDisplaySettings(latestDisplaySettings)
            }
        }
        lifecycleOwner?.lifecycle?.addObserver(observer)
        onDispose {
            lifecycleOwner?.lifecycle?.removeObserver(observer)
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
        val fontSizeSp = displaySettings.fontSize.toFloat().sp
        val lineHeightSp = (displaySettings.fontSize * 1.18).toFloat().sp
        val targetCharacters = targetCharactersPerLine(
            widthPx = widthPx,
            fontSizePx = with(density) { fontSizeSp.toPx() }
        )
        val promptLines = remember(script.content, targetCharacters) {
            PromptFormatter.lines(script.content, targetCharacters)
        }
        val textStyle = TextStyle(
            color = displaySettings.textColorPreset.promptColor(),
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
        val speechLineIndex = remember(speechFollower.progress, promptLines) {
            speechLineIndex(
                progress = speechFollower.progress,
                promptLines = promptLines
            )
        }

        LaunchedEffect(displaySettings.scrollSpeed, averageLineHeight, averageCharacters, maximumOffset) {
            engine.configure(
                speed = displaySettings.scrollSpeed,
                lineHeight = averageLineHeight,
                averageCharactersPerLine = averageCharacters,
                maximumOffset = maximumOffset
            )
        }

        LaunchedEffect(maximumOffset, layouts, speechLineIndex, speechFollower.hasTranscript) {
            if (speechFollower.isListening && speechFollower.hasTranscript) {
                val targetOffset = speechTargetOffset(
                    speechLineIndex = speechLineIndex,
                    layouts = layouts,
                    topPadding = topPadding,
                    viewportHeight = heightPx.toDouble(),
                    maximumOffset = maximumOffset
                )
                engine.follow(to = targetOffset)
            }
        }

        CameraPreview(
            lensFacing = lensFacing,
            permissionRequestKey = cameraPermissionRequestKey,
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
                            displaySettings.overlayOpacity.toFloat()
                        } else {
                            0.78f
                        }
                    )
                )
        )

        if (cameraState != CameraPermissionState.Authorized) {
            CameraStatus(
                state = cameraState,
                onRetry = { cameraPermissionRequestKey++ },
                onOpenSettings = ::openAppSettings,
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
            highlightedLineIndex = if (speechFollower.isListening && speechFollower.hasTranscript) {
                speechLineIndex
            } else {
                null
            },
            modifier = Modifier
                .fillMaxSize()
                .clipToBounds()
        )

        GestureLayer(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = 120.dp),
            onTap = {
                if (!speechFollower.isListening) {
                    engine.toggle()
                }
            },
            onDragStart = { mode ->
                if (speechFollower.isListening && mode != DragMode.ManualScroll) {
                    dragMode = null
                } else {
                    if (speechFollower.isListening && mode == DragMode.ManualScroll) {
                        speechFollower.stop()
                        engine.stopFollowing()
                    }
                    dragMode = mode
                    dragStartSpeed = engine.speed
                    dragStartOffset = engine.offset
                    dragTranslationY = 0.0
                }
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
                if (dragMode == DragMode.Speed && engine.speed != displaySettings.scrollSpeed) {
                    val nextSettings = displaySettings.copy(scrollSpeed = engine.speed)
                    displaySettings = nextSettings
                    saveDisplaySettings(nextSettings)
                }
                dragMode = null
                dragTranslationY = 0.0
            }
        )

        PrompterControls(
            isPlaying = engine.isPlaying,
            speechActive = speechFollower.isListening,
            speechStatus = speechFollower.statusText,
            audioPermissionState = audioPermissionState,
            cameraEnabled = cameraState == CameraPermissionState.Authorized ||
                cameraState == CameraPermissionState.Unavailable,
            onBack = {
                engine.pause()
                speechFollower.dispose()
                saveDisplaySettingsAndThen(onBack)
            },
            onToggle = { engine.toggle() },
            onToggleSpeech = {
                if (speechFollower.isListening) {
                    speechFollower.stop()
                    engine.stopFollowing()
                } else if (audioPermissionState == AudioPermissionState.PermanentlyDenied) {
                    openAppSettings()
                } else {
                    val initialProgress = if (maximumOffset > 0) {
                        engine.offset / maximumOffset
                    } else {
                        0.0
                    }
                    startSpeechFollowing(initialProgress)
                }
            },
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

        if (!displayPanelExpanded) {
            DisplayPanelHandle(
                onClick = { displayPanelExpanded = true },
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 18.dp)
            )
        }

        DisplaySettingsPanel(
            visible = displayPanelExpanded,
            settings = displaySettings,
            onSettingsChange = { displaySettings = it.normalized() },
            onDismiss = {
                displayPanelExpanded = false
                saveDisplaySettings()
            },
            modifier = Modifier.align(Alignment.BottomCenter)
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
    highlightedLineIndex: Int?,
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
            val isHighlighted = layout.index == highlightedLineIndex
            if (isHighlighted) {
                Text(
                    text = lines[layout.index].text,
                    style = textStyle.copy(
                        color = Color.White.copy(alpha = 0.46f),
                        shadow = Shadow(
                            color = Color.White.copy(alpha = 0.80f),
                            offset = Offset.Zero,
                            blurRadius = 18f
                        )
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp)
                        .graphicsLayer {
                            translationY = (topPadding + layout.y - engine.offset).toFloat()
                        },
                    textAlign = TextAlign.Center
                )
            }
            Text(
                text = lines[layout.index].text,
                style = if (isHighlighted) {
                    textStyle.copy(
                        color = Color.White,
                        shadow = Shadow(
                            color = Color.Black.copy(alpha = 0.88f),
                            offset = Offset(0f, 2f),
                            blurRadius = 5f
                        )
                    )
                } else {
                    textStyle
                },
                color = if (isHighlighted) Color.White else textStyle.color.copy(alpha = 0.62f),
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
    speechActive: Boolean,
    speechStatus: String,
    audioPermissionState: AudioPermissionState,
    cameraEnabled: Boolean,
    onBack: () -> Unit,
    onToggle: () -> Unit,
    onToggleSpeech: () -> Unit,
    onFlipCamera: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(
            onClick = onBack,
            modifier = Modifier
                .size(42.dp)
                .glassSurface(CircleShape)
        ) {
            Icon(Icons.Default.ArrowBack, contentDescription = "返回", tint = Color.White)
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            IconButton(
                onClick = onToggleSpeech,
                modifier = Modifier
                    .size(42.dp)
                    .glassSurface(CircleShape)
            ) {
                Icon(
                    imageVector = if (speechActive) Icons.Default.Mic else Icons.Default.MicOff,
                    contentDescription = audioPermissionState.statusText(speechStatus),
                    tint = when {
                        speechActive -> Color.White
                        audioPermissionState == AudioPermissionState.PermanentlyDenied -> MaterialTheme.colorScheme.error
                        else -> Color.White.copy(alpha = 0.72f)
                    }
                )
            }
            IconButton(
                onClick = onToggle,
                modifier = Modifier
                    .size(42.dp)
                    .glassSurface(CircleShape)
            ) {
                Icon(
                    imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                    contentDescription = if (isPlaying) "暂停" else "播放",
                    tint = Color.White
                )
            }
            IconButton(
                onClick = onFlipCamera,
                enabled = cameraEnabled,
                modifier = Modifier
                    .size(42.dp)
                    .glassSurface(CircleShape)
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
private fun DisplayPanelHandle(
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    IconButton(
        onClick = onClick,
        modifier = modifier
            .size(46.dp)
            .glassSurface(CircleShape)
    ) {
        Icon(
            imageVector = Icons.Default.KeyboardArrowUp,
            contentDescription = "打开显示设置",
            tint = Color.White
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DisplaySettingsPanel(
    visible: Boolean,
    settings: DisplaySettings,
    onSettingsChange: (DisplaySettings) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    AnimatedVisibility(
        visible = visible,
        enter = slideInVertically(initialOffsetY = { it }),
        exit = slideOutVertically(targetOffsetY = { it }),
        modifier = modifier.fillMaxWidth()
    ) {
        val shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = 460.dp)
                .consumePanelPointers()
                .background(Color(0xFF101218).copy(alpha = 0.96f), shape)
                .glassSurface(shape)
                .padding(start = 18.dp, top = 24.dp, end = 18.dp, bottom = 28.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Box(
                modifier = Modifier.fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier
                        .size(38.dp)
                        .glassSurface(CircleShape)
                ) {
                    Icon(
                        imageVector = Icons.Default.KeyboardArrowDown,
                        contentDescription = "收起显示设置",
                        tint = Color.White
                    )
                }
            }
            DisplaySliderSetting(
                title = "字号",
                valueLabel = settings.fontSize.roundToInt().toString()
            ) {
                Slider(
                    value = settings.fontSize.toFloat(),
                    onValueChange = {
                        onSettingsChange(settings.copy(fontSize = it.toDouble()))
                    },
                    valueRange = 12f..110f,
                    colors = displaySliderColors(),
                    thumb = { DisplaySliderThumb() }
                )
            }
            DisplaySliderSetting(
                title = "滚动速度",
                valueLabel = "${settings.scrollSpeed.roundToInt()} 字/分"
            ) {
                Slider(
                    value = settings.scrollSpeed.toFloat(),
                    onValueChange = {
                        onSettingsChange(settings.copy(scrollSpeed = it.toDouble()))
                    },
                    valueRange = 20f..220f,
                    colors = displaySliderColors(),
                    thumb = { DisplaySliderThumb() }
                )
            }
            DisplayColorSetting(
                selected = settings.textColorPreset,
                onSelected = {
                    onSettingsChange(settings.copy(textColorPreset = it))
                }
            )
            val transparency = (1.0 - settings.overlayOpacity).coerceIn(0.18, 0.82)
            DisplaySliderSetting(
                title = "摄像头透明度",
                valueLabel = "${(transparency * 100).roundToInt()}%"
            ) {
                Slider(
                    value = transparency.toFloat(),
                    onValueChange = {
                        val nextOverlay = (1.0 - it.toDouble()).coerceIn(0.18, 0.82)
                        onSettingsChange(settings.copy(overlayOpacity = nextOverlay))
                    },
                    valueRange = 0.18f..0.82f,
                    colors = displaySliderColors(),
                    thumb = { DisplaySliderThumb() }
                )
            }
        }
    }
}

@Composable
private fun displaySliderColors() = SliderDefaults.colors(
    thumbColor = Color.White,
    activeTrackColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.86f),
    inactiveTrackColor = Color.White.copy(alpha = 0.24f)
)

@Composable
private fun DisplaySliderThumb() {
    Box(
        modifier = Modifier
            .size(24.dp)
            .background(Color.White, CircleShape)
            .border(2.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.78f), CircleShape)
    )
}

@Composable
private fun DisplaySliderSetting(
    title: String,
    valueLabel: String,
    content: @Composable () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                color = Color.White,
                modifier = Modifier.weight(1f)
            )
            Text(
                text = valueLabel,
                style = MaterialTheme.typography.bodyMedium,
                color = Color.White.copy(alpha = 0.66f)
            )
        }
        content()
    }
}

@Composable
private fun DisplayColorSetting(
    selected: TextColorPreset,
    onSelected: (TextColorPreset) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = "文字颜色",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold,
            color = Color.White
        )
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            TextColorPreset.editorChoices.forEach { preset ->
                val isSelected = selected == preset
                FilterChip(
                    selected = isSelected,
                    onClick = { onSelected(preset) },
                    label = {
                        Text(
                            text = preset.displayName,
                            color = if (isSelected) {
                                MaterialTheme.colorScheme.primary
                            } else {
                                Color.White.copy(alpha = 0.74f)
                            }
                        )
                    },
                    leadingIcon = {
                        Box(
                            modifier = Modifier
                                .padding(start = 2.dp)
                                .background(preset.promptColor(), MaterialTheme.shapes.small)
                                .padding(7.dp)
                        )
                    },
                    colors = FilterChipDefaults.filterChipColors(
                        containerColor = Color.White.copy(alpha = 0.08f),
                        selectedContainerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.18f)
                    ),
                    border = FilterChipDefaults.filterChipBorder(
                        enabled = true,
                        selected = isSelected,
                        borderColor = Color.White.copy(alpha = 0.32f),
                        selectedBorderColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.82f)
                    )
                )
            }
        }
    }
}

private fun Modifier.consumePanelPointers(): Modifier =
    pointerInput(Unit) {
        awaitPointerEventScope {
            while (true) {
                val event = awaitPointerEvent(PointerEventPass.Final)
                event.changes.forEach { it.consume() }
            }
        }
    }

@Composable
private fun CameraStatus(
    state: CameraPermissionState,
    onRetry: () -> Unit,
    onOpenSettings: () -> Unit,
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
                CameraPermissionState.PermanentlyDenied -> "相机权限未开启"
                CameraPermissionState.Unavailable -> "摄像头不可用"
            },
            color = Color.White,
            style = MaterialTheme.typography.titleMedium,
            textAlign = TextAlign.Center
        )
        if (state == CameraPermissionState.Denied || state == CameraPermissionState.PermanentlyDenied) {
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = if (state == CameraPermissionState.PermanentlyDenied) {
                    "请到系统设置里允许乔木提词器访问相机。"
                } else {
                    "允许相机权限后可以显示取景画面。"
                },
                color = Color.White.copy(alpha = 0.76f),
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(14.dp))
            Button(
                onClick = if (state == CameraPermissionState.PermanentlyDenied) onOpenSettings else onRetry
            ) {
                Text(if (state == CameraPermissionState.PermanentlyDenied) "去系统设置" else "重新授权")
            }
        }
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

private fun speechLineIndex(progress: Double, promptLines: List<PromptLine>): Int? {
    if (promptLines.isEmpty()) return null

    val totalCharacters = promptLines.sumOf { line ->
        speechCharacterCount(line.text)
    }
    if (totalCharacters <= 0) return null

    val target = (totalCharacters * progress.coerceIn(0.0, 1.0)).roundToInt()
    var consumed = 0

    promptLines.forEachIndexed { index, line ->
        val count = speechCharacterCount(line.text)
        if (count <= 0) return@forEachIndexed

        consumed += count
        if (target <= consumed) {
            return index
        }
    }

    return promptLines.indexOfLast { speechCharacterCount(it.text) > 0 }.takeIf { it >= 0 }
}

private fun speechTargetOffset(
    speechLineIndex: Int?,
    layouts: List<PromptLineLayout>,
    topPadding: Double,
    viewportHeight: Double,
    maximumOffset: Double
): Double {
    val lineIndex = speechLineIndex ?: return 0.0
    val layout = layouts.firstOrNull { it.index == lineIndex } ?: return 0.0
    val desiredLineY = viewportHeight * 0.42
    return (topPadding + layout.y - desiredLineY).coerceIn(0.0, maximumOffset)
}

private fun speechCharacterCount(text: String): Int =
    text.count { it.isLetterOrDigit() }

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

private data class DisplaySettings(
    val fontSize: Double,
    val scrollSpeed: Double,
    val textColorPreset: TextColorPreset,
    val overlayOpacity: Double
) {
    fun normalized(): DisplaySettings =
        copy(
            fontSize = fontSize.coerceIn(12.0, 110.0),
            scrollSpeed = scrollSpeed.coerceIn(20.0, 220.0),
            textColorPreset = if (textColorPreset in TextColorPreset.editorChoices) {
                textColorPreset
            } else {
                TextColorPreset.White
            },
            overlayOpacity = overlayOpacity.coerceIn(0.18, 0.82)
        )

    companion object {
        fun fromScript(script: Script): DisplaySettings =
            DisplaySettings(
                fontSize = script.fontSize,
                scrollSpeed = script.scrollSpeed,
                textColorPreset = script.textColorPreset,
                overlayOpacity = script.overlayOpacity
            ).normalized()
    }
}

private enum class DragMode {
    Speed,
    ManualScroll,
    Progress
}

private enum class AudioPermissionState {
    NotRequested,
    Requesting,
    Authorized,
    Denied,
    PermanentlyDenied
}

private fun AudioPermissionState.statusText(fallback: String): String =
    when (this) {
        AudioPermissionState.NotRequested -> fallback
        AudioPermissionState.Requesting -> "正在请求麦克风权限"
        AudioPermissionState.Authorized -> fallback
        AudioPermissionState.Denied -> "麦克风权限未开启"
        AudioPermissionState.PermanentlyDenied -> "去系统设置开启麦克风"
    }

private fun audioDeniedState(activity: Activity?): AudioPermissionState {
    val canAskAgain = activity?.let {
        ActivityCompat.shouldShowRequestPermissionRationale(it, Manifest.permission.RECORD_AUDIO)
    } ?: true
    return if (canAskAgain) AudioPermissionState.Denied else AudioPermissionState.PermanentlyDenied
}
