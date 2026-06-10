# QMPrompter Android 迁移计划

## Context

将现有 iOS SwiftUI 提词器应用（乔木提词器）完整迁移到 Android 平台。当前项目是纯 iOS 应用，使用 Swift/SwiftUI，零第三方依赖。目标是创建一个功能对等的 Android 应用，使用 Jetpack Compose + Room + Kotlin。

## 技术选型

| 维度 | iOS (当前) | Android (目标) |
|------|-----------|---------------|
| 语言 | Swift | Kotlin |
| UI | SwiftUI | Jetpack Compose (Material3) |
| 持久化 | JSON 文件 | Room 数据库 |
| 相机 | AVCaptureSession | CameraX |
| 语音识别 | SFSpeechRecognizer | Android SpeechRecognizer |
| API 密钥存储 | Keychain | SharedPreferences（见下方说明¹） |
| 网络 | URLSession | OkHttp + Coroutines |
| 动画帧 | CADisplayLink | withFrameNanos (Compose) |
| 导航 | NavigationStack | NavHost + NavController |
| Min SDK | iOS 17.0 | Android API 26 (8.0) |

> ¹ `androidx.security:security-crypto`（EncryptedSharedPreferences）已被 Google 弃用停更。本机单用户的个人应用用普通 SharedPreferences 风险可接受，**但必须配置备份排除规则防止 API Key 随 Auto Backup 上云**（见 Phase 1 第 7 条）；`AiProviderConfigStore` 做成接口，以后需要时可换 Android Keystore 手写加密实现。

## 工具链版本（开工当天锁定）

不预先死守具体版本号——下方数字仅为编写本计划时的参考基线，**开工第一天统一升到当时稳定版并锁定**，此后不随意漂移。必须遵守的配套关系：

- Kotlin 2.x 起 Compose Compiler 随 Kotlin 发布，必须引入 `org.jetbrains.kotlin.plugin.compose` 插件且版本与 Kotlin 一致，缺失即编译失败
- KSP 按官方 release 配套表选取与所用 Kotlin 匹配的版本（KSP2 起不再要求版本号前缀与 Kotlin 严格相等，以配套表为准）
- AGP 与 compileSdk 按官方兼容表配套

```kotlin
// 参考基线（开工时刷新）
id("com.android.application") version "8.7.3"
id("org.jetbrains.kotlin.android") version "2.0.21"
id("org.jetbrains.kotlin.plugin.compose") version "2.0.21"
id("com.google.devtools.ksp") version "2.0.21-1.0.28"
```

- compileSdk / targetSdk 取开工时最新稳定 SDK，minSdk = 26

## 非目标与架构取舍

- **不做 iOS 数据导入**：iOS 端的 `scripts.json` 不迁移，Android 全新数据（试用文稿由 Room SeedCallback 提供）
- **不上架 Google Play**：与 iOS 版同为个人侧载使用，Play 的 targetSdk 政策不约束本项目
- **不引入 DI 框架与 ViewModel 层**：应用竖屏锁定、提词状态为瞬态（无跨配置变更/进程死亡恢复的需求），Screen 直接持有 Repository/Engine 是匹配单人自用规模的有意取舍，非疏忽
- **不建测试基建**：不做 UI 自动化、设备矩阵、性能基准（Macrobenchmark）等；验收靠"验证方式"中的手动清单 + 纯逻辑单元测试承载

## 目录结构

```
QMPrompter-Android/
  QMPrompter/                    (现有 iOS，保留)
  app/
    src/main/
      kotlin/com/qiaomu/prompter/
        QMPrompterApp.kt         — Application 类 (初始化 DB/Repository)
        MainActivity.kt          — Compose 入口 + NavHost + CompositionLocalProvider
        model/
          Script.kt              — Room @Entity
          TextColorPreset.kt     — 枚举 + TypeConverter
        data/
          ScriptDao.kt           — Room @Dao
          AppDatabase.kt         — Room Database + SeedCallback
          ScriptRepository.kt    — StateFlow<List<Script>> 封装
          AiProviderConfigStore.kt — apiKey/baseUrl/model 三字段持久化（SharedPreferences，可替换为 Keystore 实现）
        service/
          ScrollEngine.kt        — withFrameNanos 帧循环，delta-time 滚动 + follow 平滑跟随
          CameraPreview.kt       — CameraX PreviewView in AndroidView
          SpeechFollower.kt      — Android SpeechRecognizer + SpeechScriptIndex
          PromptDictation.kt     — 语音输入（简化版 SpeechRecognizer）
          ScriptGenerator.kt     — 生成接口 + OkHttp 实现（OpenAI 兼容协议，DeepSeek 为默认）
        ui/
          theme/
            Theme.kt / Color.kt / Type.kt
            GlassSurface.kt      — Modifier.glassSurface() 扩展
          scriptlist/
            ScriptListScreen.kt  — 首页：列表 + 搜索 + 滑动删除
            ScriptCard.kt        — 玻璃质感卡片
          editor/
            ScriptEditorScreen.kt — TabRow (文稿/显示) + 编辑器
          prompter/
            PrompterScreen.kt    — 全屏提词：相机背景 + 滚动文字 + 手势
          ai/
            AIGenerationScreen.kt — AI 生成界面
          settings/
            AppSettingsScreen.kt — AI 供应商配置（API Key / Base URL / Model）
          component/
            GlassActionPanel.kt  — 底部玻璃操作面板
        util/
          PromptFormatter.kt     — 文本分行（纯逻辑，直接移植）
      AndroidManifest.xml        — 权限 + queries 声明 + 竖屏锁定（见 Phase 1 第 7 条）
      res/
  build.gradle.kts               (project-level)
  app/build.gradle.kts           (app-level)
  settings.gradle.kts
  gradle.properties
```

## 实施阶段

### Phase 1: 项目骨架 + 数据层
1. 创建 Gradle 项目结构（project/app build.gradle.kts, settings.gradle.kts）——网络受限环境从既有 Kotlin 项目复制骨架，按 `INIT_FROM_TEMPLATE.md` 执行；版本锁定为"本机已缓存且满足下限"的组合
2. 实现 `Script` Room Entity + `TextColorPreset` 枚举（TypeConverter 直接存 rawValue 即可；iOS 端 "yellow"/"green" 旧值兼容是其历史 JSON 遗留，Android 全新安装无旧数据，不移植。UUID 主键存 String，Date 存 epoch millis）
3. 实现 `ScriptDao` + `AppDatabase`（含 SeedCallback 插入试用文稿；`version = 1` + `exportSchema = true`，后续字段变更一律走 Migration 不删库）
4. 实现 `ScriptRepository`（StateFlow 封装 Dao）
5. 实现 `AiProviderConfigStore`（apiKey/baseUrl/model 三字段，SharedPreferences 持久化，baseUrl/model 默认空串）
6. 实现 `PromptFormatter`（纯逻辑移植）+ 单元测试（与 iOS 同输入对比输出）
7. 编写 `AndroidManifest.xml`：
   - 权限：`CAMERA`、`RECORD_AUDIO`、`INTERNET`
   - **`<queries>` 声明 `android.speech.RecognitionService`**（Android 11+ package visibility，缺了 `SpeechRecognizer.isRecognitionAvailable()` 静默返回 false）
   - **备份排除**：`dataExtractionRules`（API 31+）+ `fullBackupContent`（API 26–30）排除 `AiProviderConfigStore` 的 prefs 文件——SharedPreferences 默认会随 Auto Backup 上传用户的 Google 云端备份，API Key 不能跟着出去
   - Activity 设 `screenOrientation="portrait"`（对齐 iOS 仅竖屏）
8. 实现 `QMPrompterApp` + `MainActivity` 骨架 + NavHost 路由

### Phase 1.5: SpeechRecognizer 连续监听 PoC（风险前置）
9. 写一个抛弃式 Activity，在目标真机上验证三件事并得出明确结论：
   - 每次 `startListening()` 的系统提示音能否消除（AudioManager 静音方案是否有效）
   - 回调内重启的 ERROR_RECOGNIZER_BUSY 能否规避（延迟重启 vs destroy 重建，取实测可行者）
   - 重启间隙丢音频的时长是否在可接受范围
   任何一项不成立，先调整语音跟随方案再继续，避免 UI 工作量投入后才暴露最大风险

### Phase 2: 列表 + 编辑器
10. 实现 `ScriptListScreen`（LazyColumn + SearchBar + SwipeToDismissBox）
11. 实现 `ScriptCard`（玻璃质感 Modifier）
12. 实现 `ScriptEditorScreen`（TabRow + TextField + Slider 设置项；含粘贴正文/清空正文按钮、标题编辑弹窗、摄像头透明度反向映射 `1 - overlayOpacity`，clamp 0.18–0.82）
13. 实现 `GlassActionPanel` 底部面板
14. 实现 `AppSettingsScreen`（API Key + Base URL + Model 三个输入框，后两个可选、placeholder 显示 DeepSeek 默认值，显式"保存"按钮一次性写入）

### Phase 3: 相机 + 滚动
15. 实现 `CameraPreview`（CameraX + PreviewView + 权限请求）
16. 实现 `ScrollEngine`（withFrameNanos 帧循环，移植 delta-time 数学）：
    - 匀速模式：`linesPerSecond = speed / avgCharsPerLine / 60 * 1.85`（visualTuningFactor），速度 clamp 20–260
    - **follow 平滑跟随模式**（语音跟随的核心）：指数趋近 `next = offset + (target - offset) * min(1, delta * 12)`，距目标 < 0.5px 时吸附
    - 到达 maxOffset 自动暂停
17. 实现 `PrompterScreen` 骨架（相机背景 + 滚动文字 + 暗色遮罩）：
    - **滚动偏移必须用 `Modifier.graphicsLayer { translationY = ... }` 或 lambda 版 `Modifier.offset { }` 读取**，把状态读取推迟到绘制阶段，避免每帧全量 recomposition（长文稿掉帧的根因）
    - 可见行裁剪：自绘布局 + 移植 iOS 的 visibleLayoutRange 窗口逻辑（不用 LazyColumn，与像素级滚动控制冲突）
    - 行高测量用 Compose `TextMeasurer` 精确测量，替代 iOS 的字符宽度估算
    - 进入屏幕即设置 `FLAG_KEEP_SCREEN_ON`（iOS 版缺失 idleTimerDisabled，是原版疏漏，Android 版补上）
18. 实现手势层（左侧拖动调速、右侧拖动调进度、中间手动滚动、点击播放/暂停），完整规则见"关键技术细节"

### Phase 4: 语音 + AI
19. 实现 `SpeechFollower`（按"关键技术细节"的状态机实现连续监听，落实 Phase 1.5 验证过的方案）
20. 移植 `SpeechScriptIndex` 匹配逻辑（候选片段 + committedOffset 单调推进 + 匹配评分，原样移植）+ 单元测试
21. 实现**当前行高亮**（README 已声明的功能，防止行为回退）：语音跟随时当前行纯白 + 白色光晕/黑色投影双层 shadow，其余行按 textColorPreset 颜色的 62% 不透明度；非语音模式所有行统一 62% 不透明度、无高亮
22. 实现 `PromptDictation`
23. 实现 `ScriptGenerator` 接口 + `OpenAICompatGenerator` 实现（OkHttp + 系统提示词 + cleanGeneratedScript 清洗逻辑，`max_tokens: 2800`；baseUrl/model 从 `AiProviderConfigStore` 读取，规则见"关键技术细节 → AI 供应商可配置"）
24. 实现 `AIGenerationScreen`
25. 对齐 iOS 行为：**进入提词页默认自动启动语音跟随**（首次进入会连续弹相机 + 麦克风/语音权限，权限请求流程按此设计）

### Phase 5: 收尾
26. 沉浸模式（隐藏状态栏/导航栏）
27. 完善玻璃质感效果（API 31+ 真实模糊，以下版本模拟）
28. 权限拒绝处理 + 引导用户去设置
29. 真机测试

## 关键技术细节

### Glassmorphism 策略
- API 31+：`RenderEffect.createBlurEffect()` 实现真实背景模糊
- API 26-30：半透明白色渐变模拟毛玻璃效果
- 统一封装为 `Modifier.glassSurface()` 扩展函数

### SpeechRecognizer 连续监听（最难点，Phase 1.5 先行验证）
Android SpeechRecognizer 会在检测到静音后自动停止，与 iOS 行为差异远不止重启一项。实现为显式状态机，避免重启竞态：

```
Idle → Starting → Listening → RestartPending → Starting（循环）
                     ↓               ↓
                  Disposed ←———— 任意状态可达（用户退出/切模式；终态，资源已释放，不再参与循环）
```

- **线程约束**：`createSpeechRecognizer` / `startListening` / `stopListening` / `destroy` 及 listener 回调全部在主线程（官方要求），状态流转不需要额外锁
- **生命周期**：离开提词页或切换到速度模式时调用 `destroy()` 进入 Disposed 终态，RestartPending 期间收到销毁请求要取消挂起的重启（否则泄漏 + 后台占用麦克风）；重新进入语音模式时从 Idle 新建实例
- **重启时机**：`onResults` 后进入 RestartPending 再重启；`onError(ERROR_NO_MATCH/ERROR_SPEECH_TIMEOUT)` 同样处理
- **ERROR_RECOGNIZER_BUSY**：立即重启常触发 busy，按 Phase 1.5 实测结论二选一——延迟 100–200ms 重启，或 `destroy()` 后重建实例；连续失败 N 次进入失败态提示用户，不无限重试
- **系统提示音**：很多设备每次 `startListening()` 播放 beep，提词场景不可接受——用 `AudioManager` 在监听期间临时静音对应音频流（Phase 1.5 验证有效性）；**静音仅限监听期间，停止/退出/异常路径都必须恢复原音量**，不长期静音
- **transcript 不累积，但不要拼接缓冲区**：iOS 的 partial results 在一次会话内持续增长，而 `SpeechScriptIndex` 的进度依赖 `committedOffset` 单调推进——所以 Android 重启后把每段新 transcript 独立喂给**同一个** index 实例即可。拼接累积缓冲区反而会让候选片段的后缀匹配跨段产生错误片段
- **已知能力差距**：iOS 用 `contextualStrings`（取文稿前 80 个短语）提升识别准确率，Android 仅 API 33+ 有 `EXTRA_BIASING_STRINGS` 可部分对标；minSdk 26 上低版本设备无此优化，接受

### 帧循环移植
iOS `CADisplayLink` → Android `withFrameNanos`（Compose 内置帧时钟）
- 滚动数学完全基于 delta-time，与帧率无关
- 120Hz 设备自动适配，无需特殊处理
- ScrollEngine 两种模式都要移植：匀速滚动（visualTuningFactor 1.85，速度 clamp 20–260）和 follow 平滑跟随（指数趋近系数 12，吸附阈值 0.5px）

### Compose 滚动渲染性能
- 每帧更新的 offset 不能被组合作用域直接读取，否则每帧全量 recomposition
- 用 `Modifier.graphicsLayer { translationY }` / lambda 版 `Modifier.offset { }` 把读取推迟到绘制阶段
- 移植 iOS 的可见行窗口裁剪逻辑（visibleLayoutRange：可视区上下各扩 0.35x/1.35x 视口高 + 前后 2 行余量）

### 手势映射
- 单个手势层分三个等宽区域：左 1/3=调速，中间=手动滚动，右 1/3=调进度
- 拖动判定 `minimumDistance ≈ 12dp`，区分点击与拖动；模式按拖动起始位置锁定，直到手指抬起
- 灵敏度系数：速度 0.42（上滑加速）、进度 1.35、手动 1.05
- 顶部保留区（状态栏 + 96dp）和底部保留区（面板打开时加高）不响应手势
- 点击规则：设置面板打开时点击 = 关闭面板；语音跟随中点击不切换播放；其余 = 播放/暂停
- 语音跟随中：左右分区拖动禁用，仅中间手动滚动可用，且触发即停止语音跟随
- 拖动结束时持久化（onSave）

### 权限请求时机
进入提词页即自动启动语音跟随（对齐 iOS），首次进入会依次请求相机、麦克风权限；任一被拒走对应的降级 UI（相机被拒显示引导卡片、语音被拒显示状态 pill），互不阻塞。备选项（第一版不做）：若实测连续弹窗体验不佳，可改为进入页面只请求相机，点语音 pill 时再请求麦克风——记录为有意偏离对等的候选优化

### AI 供应商可配置（DeepSeek 为默认）
iOS 版把 endpoint/model 硬编码在 `DeepSeekScriptGenerator` 里；Android 版改为可配置，默认行为与 iOS 完全一致：

- **配置存储**：`AiProviderConfigStore` 持久化 apiKey/baseUrl/model 三字段，设置页显式"保存"后写入，下次启动自动读回，用户只填一次
- **留空即默认**：baseUrl/model 为空时回落 DeepSeek——
  ```kotlin
  val effectiveBaseUrl = config.baseUrl.ifBlank { "https://api.deepseek.com" }
  val effectiveModel   = config.model.ifBlank { "deepseek-v4-flash" }
  ```
  只填 API Key 的用户体验与 iOS 版完全相同；换供应商（Kimi/Qwen/智谱/OpenRouter/Ollama 等 OpenAI 兼容端点）只需改设置，零代码改动
- **专有字段隔离**：`thinking: {type: "disabled"}` 是 DeepSeek 专有参数，仅当 effectiveBaseUrl 包含 `deepseek.com` 时附加，避免其他供应商对未知字段报 400
- **请求路径**：`{effectiveBaseUrl}/chat/completions`（标准 OpenAI 兼容协议，非流式）
- **已知限制**：单 key 槽位，切换供应商需重新粘贴对应 key（个人应用可接受，第一版不做按 baseUrl 分槽）；非 OpenAI 兼容协议（如 Anthropic Messages API）不在支持范围

## 依赖清单

```kotlin
// ⚠️ 历史参考基线（2024-12 时点快照），不可直接复制——开工当天统一刷新为最新稳定版
// Compose BOM
implementation(platform("androidx.compose:compose-bom:2024.12.01"))
implementation("androidx.compose.material3:material3")
implementation("androidx.compose.material:material-icons-extended")

// Activity & Lifecycle
implementation("androidx.activity:activity-compose:1.9.3")
implementation("androidx.lifecycle:lifecycle-runtime-compose:2.8.7")
// 不引入 lifecycle-viewmodel-compose：本项目无 ViewModel 层，Screen 直接持有 Repository/Engine

// Navigation
implementation("androidx.navigation:navigation-compose:2.8.5")

// Room
implementation("androidx.room:room-runtime:2.6.1")
implementation("androidx.room:room-ktx:2.6.1")
ksp("androidx.room:room-compiler:2.6.1")

// CameraX
implementation("androidx.camera:camera-camera2:1.4.1")
implementation("androidx.camera:camera-lifecycle:1.4.1")
implementation("androidx.camera:camera-view:1.4.1")

// 不引入 androidx.security:security-crypto（已弃用），API Key 用 SharedPreferences

// OkHttp
implementation("com.squareup.okhttp3:okhttp:4.12.0")

// Coroutines
implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.9.0")

// 测试（PromptFormatter / SpeechScriptIndex 纯逻辑单测）
testImplementation("junit:junit:4.13.2")
```

以上为编写计划时的参考基线。与工具链一致：开工当天统一升到当时稳定版（Compose 用 BOM 对齐）并锁定。

## 验证方式

每个 Phase 完成后：
1. `./gradlew assembleDebug` 编译通过（含 `./gradlew test` 跑纯逻辑单测）
2. 模拟器/真机安装运行
3. Phase 1: 空列表 → 首次启动显示试用文稿；PromptFormatter 单测与 iOS 同输入输出一致
4. Phase 1.5: 真机 PoC 三项结论明确——beep 可消除、BUSY 可规避、重启间隙可接受；任一不成立先改方案
5. Phase 2: 可创建/编辑/删除文稿；杀进程重启后数据完好
6. Phase 3: 前置相机预览 + 文字滚动 + 手势控制；**5k 字与 20k 字文稿滚动目测无卡顿**（验证 graphicsLayer 方案，必要时开 GPU 渲染条复核）；提词中屏幕不熄灭；锁屏/切后台返回后相机恢复、滚动位置不丢
7. Phase 4: 语音跟随 + **当前行高亮** + AI 生成文稿；真机验证连续监听无 beep、重启无 BUSY、说话进度单调推进；语音服务不可用的设备显示"语音识别不可用"降级提示；无网络/超时的 AI 生成报错可读；AI 配置留空走 DeepSeek 默认，填第三方 OpenAI 兼容端点可正常生成
8. Phase 5: 沉浸模式；权限三条路径全覆盖——首次请求、拒绝一次（可再次请求）、永久拒绝（引导去系统设置）各有对应 UI
