# Project Overview
QMPrompter Android is a Kotlin migration of the existing iOS SwiftUI teleprompter app. The goal is feature parity with the current iOS app using Jetpack Compose for UI, Room for local scripts, CameraX for camera preview, Android SpeechRecognizer for voice following/dictation, and OkHttp for OpenAI-compatible AI script generation.

The existing iOS project remains in the same repository for reference. Android project files live at the repository root with the app module under `app/`.

## Tech Stack
- Kotlin 2.2.10
- Android Gradle Plugin 9.2.0
- Gradle Wrapper 9.4.1
- JDK 21
- Jetpack Compose + Material3
- Room + KSP
- CameraX
- Navigation Compose
- Android SpeechRecognizer
- OkHttp + Coroutines
- JUnit 4 for focused pure-logic tests

Do NOT introduce unless explicitly requested:
- DI frameworks such as Hilt, Dagger, or Koin
- ViewModel layer or lifecycle-viewmodel-compose
- XML/AppCompat/RecyclerView UI for new Android screens
- androidx.security:security-crypto / EncryptedSharedPreferences
- iOS data import from `scripts.json`
- UI automation, device matrix testing, Macrobenchmark, or broad test infrastructure
- Extra abstraction, configuration, or provider systems beyond the migration plan

### Common Command
- Build debug APK: `rtk ./gradlew.bat assembleDebug --stacktrace`
- Test: `rtk ./gradlew.bat test --stacktrace`
- Install debug APK: `rtk ./gradlew.bat installDebug --stacktrace`
- Show Gradle version: `.\gradlew.bat --version`
- Search code/text: `rtk rg "<pattern>"`
- Check working tree: `rtk git status --short --branch`
- Run Gradle verification sequentially. Do not run `test` and `assembleDebug` in parallel; Kotlin incremental compilation can race on `app/build/tmp/kotlin-classes`.

## Project Context
- Android 迁移计划：`MIGRATION_PLAN.md`
- Phase 1 实施计划：`plans/phase-1-implementation-plan.md`
- Phase 1.5 SpeechRecognizer PoC 计划：`plans/phase-1.5-speech-poc-plan.md`
- Phase 1.5 SpeechRecognizer PoC 结果：`plans/phase-1.5-speech-poc-result.md`
- Phase 2 实施计划：`plans/phase-2-implementation-plan.md`
- Phase 3 实施计划：`plans/phase-3-implementation-plan.md`
- Phase 4 实施计划：`plans/phase-4-implementation-plan.md`
- Phase 5 实施计划：`plans/phase-5-implementation-plan.md`
- Kotlin 模板初始化指南：`INIT_FROM_TEMPLATE.md`
- 当前交接记录：`tmp/handoff.md`
- iOS 参考实现：`QMPrompter/`
- Xcode 工程：`QMPrompter.xcodeproj/`

## Working Rules

### 1. 先思考，后编码

**不要假设。不要掩饰困惑。暴露权衡。**

实现之前：
- 明确陈述你的假设。如果不确定，就提问。
- 如果存在多种解释，把它们列出来——不要默默选一个。
- 如果存在更简单的方法，就说出来。在必要时提出反对意见。
- 如果有不清楚的地方，停下来。说出困惑之处。提问。

### 2. 简单至上

**解决问题所需的最少代码。不写推测性内容。**

- 不添加超出需求范围的功能
- 不为只使用一次的代码做抽象
- 不添加未被要求的“灵活性”或“可配置性”
- 不为不可能发生的场景做错误处理
- 如果你写了 200 行代码而本来可以只写 50 行，那就重写

问自己：“资深工程师会觉得这过于复杂吗？” 如果是，就简化。

### 3. 手术式修改

**只动必须动的地方。只清理自己造成的混乱。**

编辑现有代码时：
- 不要“改进”相邻的代码、注释或格式
- 不要重构没有坏掉的东西
- 匹配现有风格，即使你会用不同的方式做
- 如果你注意到无关的废弃代码，可以提一下——但不要删除它

当你的修改造成孤儿代码时：
- 移除因你的修改而变得未使用的导入/变量/函数
- 除非被要求，否则不要删除原本就存在的废弃代码

检验标准：每个被修改的行都应该能直接追溯到用户的需求。

---

### Lessons
- `local.properties` is machine-specific and currently points to `D:\android-sdk`; keep it ignored.
- Current AGP 9.2.0 setup needs `android.builtInKotlin=false` and `android.newDsl=false` in `gradle.properties`; removing either caused Gradle plugin failures.
- Keep `android.useAndroidX=true` in `gradle.properties`; it is required by `INIT_FROM_TEMPLATE.md`.
- Manifest must keep camera, microphone, internet permissions, SpeechRecognizer queries, portrait orientation, and backup exclusions for `ai_provider_config.xml`.
- `MODIFY_AUDIO_SETTINGS` remains in the manifest from the SpeechRecognizer PoC/history, but final `SpeechFollower` currently does not mute system streams because Phase 1.5 found no audible beep on the tested device.
- Current command-line verification is `rtk ./gradlew.bat test --stacktrace` followed by `rtk ./gradlew.bat assembleDebug --stacktrace`; this passed after Phase 4 implementation, subagent-review fixes, and the voice-following default-off change.
- Room uses `exportSchema = true`; keep generated schema files under `app/schemas/` as migration baselines.
- `Script.createdAt` and `Script.updatedAt` are epoch millis `Long` values, not ISO 8601 strings.
- `TextColorPreset` stores raw values only (`white`, `silver`, `graphite`); do not add iOS JSON legacy aliases such as `yellow` or `green` unless an explicit data import requirement is added.
- First-run sample data is seeded from `RoomDatabase.Callback.onCreate()` with `SupportSQLiteDatabase.execSQL`; avoid singleton/coroutine fire-and-forget seeding.
- `AiProviderConfigStore` uses SharedPreferences name `ai_provider_config`, which maps to backup exclusion path `ai_provider_config.xml`; keep these aligned.
- `PromptFormatter` is ported from iOS with focused JUnit tests; code point counting is acceptable for the Chinese/ASCII punctuation parity target, while emoji grapheme cluster parity is not a Phase 1 requirement.
- `.kotlin/` contains local Kotlin build logs/cache and should not be committed.
- Phase 1.5 SpeechRecognizer PoC is complete; results live in `plans/phase-1.5-speech-poc-result.md`.
- On the tested real device, `ERROR_RECOGNIZER_BUSY` was not reproduced.
- `Destroy/recreate` was more stable than `Delay 150ms`; final `SpeechFollower` uses destroy/recreate with a 150ms restart delay.
- Observed restart gap with destroy/recreate was about 152ms and acceptable.
- No audible system beep was heard on the tested device.
- `ERROR_NO_MATCH` and `ERROR_NETWORK_TIMEOUT` still occur; final `SpeechFollower` treats them as recoverable until thresholds and then fails with readable status instead of retrying forever.
- Phase 3 camera preview, scroll engine, prompter screen skeleton, gestures, keep-screen-on, and lightweight camera flip are implemented and user-validated on a real device.
- Phase 4 speech following, current-line highlight, dictation, OpenAI-compatible generation, and AI generation screen are implemented.
- Phase 5 immersive mode, system-bar restoration, permission-denial/settings guidance, and glass UI polish are implemented.
- Current command-line verification after Phase 5/UI polish is `rtk ./gradlew.bat test --stacktrace` followed by `rtk ./gradlew.bat assembleDebug --stacktrace`; this passed after the latest editor-title-dialog opacity fix.
- Voice following is intentionally default off when entering `PrompterScreen`; users manually start it with the top microphone button. Preserve this product decision unless explicitly changed.
- `Speech PoC`/phase wording must not be exposed in production-facing UI. The PoC Activity/file still exists for reference but the homepage entry is removed.
- `glassSurface()` uses a diagonal glass treatment: left-top/right-bottom highlights and left-bottom/right-top darker edges. Preserve this direction because it keeps small card metadata readable.
- Do not apply `Modifier.blur()` directly to glass controls; it blurs text/icons too. True background-only blur should be a dedicated View/RenderNode implementation if needed.
- Do not wrap Material3 `FloatingActionButton` directly with `glassSurface()`; it creates a double-container effect. The homepage FAB is intentionally a custom `Box + glassSurface(CircleShape)`.
- `ScriptListScreen` uses a custom `GlassSearchField` based on `BasicTextField`; keep the search field visually lighter than script cards and avoid reverting it to a hard `OutlinedTextField`.
- `ScriptEditorScreen` uses glass-styled top icon buttons, a custom `GlassSegmentedTabs`, a glass `BasicTextField` body editor, and a bottom glass save/start action row.
- Multiple glass circle buttons in `TopAppBar` actions must be wrapped in a `Row` with explicit spacing to avoid overlapping outer circles.
- `ScriptEditorScreen` title editing uses custom `TitleEditDialog`; do not revert to default `AlertDialog` unless explicitly requested.
- `TitleEditDialog` uses a high-opacity `dialogGlassSurface()` so underlying editor text does not show through and reduce readability.
- In `ScriptEditorScreen`, empty titles save as `Script.UNTITLED`; font size clamps to 12..110, scroll speed clamps to 20..220, and overlay opacity clamps to 0.18..0.82.
- `ScriptEditorScreen` explicit save icon/button shows `已保存` snackbar for about 0.5s, offset above the bottom save button; auto-save on back/start-prompter remains silent.
- Camera transparency UI is the inverse of `Script.overlayOpacity`: display/edit `transparency = 1 - overlayOpacity`, then persist `overlayOpacity = 1 - transparency` with the same 0.18..0.82 clamp.
- `AppSettingsScreen` uses the existing `AiProviderConfigStore`; Base URL and Model remain optional, with DeepSeek defaults shown as placeholders rather than explanatory in-app text.
- `OpenAICompatGenerator` uses `AiProviderConfigStore`; empty Base URL and Model fall back to `https://api.deepseek.com` and `deepseek-v4-flash`; DeepSeek-only `thinking: {type: "disabled"}` is added only when the effective base URL contains `deepseek.com`.
- `AIGenerationScreen` saves generated scripts to Room and navigates to the editor; prompt dictation appends recognized text when typed text already exists instead of replacing it.
- `rememberSwipeToDismissBoxState(confirmValueChange = ...)` currently emits a Material3 deprecation warning but builds successfully; do not mix Compose API cleanup into feature phases unless explicitly requested.
- `CameraPreview` uses CameraX `PreviewView` in Compose `AndroidView`; default lens is front camera, with lightweight in-prompter front/back flip.
- `CameraPreview` distinguishes camera `Denied` from `PermanentlyDenied`, supports explicit retry through `permissionRequestKey`, and refreshes authorization after returning from app settings.
- If a requested camera is unavailable, keep a recovery path to the last available lens; do not leave `PrompterScreen` stuck in `Unavailable`.
- `PrompterScreen` uses `FLAG_KEEP_SCREEN_ON` while active and hides system bars only while the prompter screen is active; restore system bars on exit.
- Microphone permission should be requested only after the user taps the microphone button; permanently denied microphone permission opens app details settings from that button.
- `ScrollEngine` uses Compose `withFrameNanos`; keep iOS math: speed clamp 20..260, visual tuning factor 1.85, follow smoothing `delta * 12`, snap below 0.5px.
- Prompter text offset should stay draw-phase via `graphicsLayer` or lambda offset; avoid per-frame full recomposition.
- Prompter drag gestures must use cumulative translation from drag start, not per-event `dragAmount`, for speed/manual-scroll/progress formulas.
- Top controls need a gesture reserve area so prompter drag zones do not hijack control interactions.
- In voice-following mode, taps must not toggle speed playback; center/manual drag stops voice following and allows manual scroll; left/right drag are ignored.
- Current-line highlight should appear only after `SpeechFollower` has transcript; startup/restart gaps must not falsely highlight the first line.
- `SpeechFollower` uses a session token to prevent stale delayed restarts from reviving an old recognizer session after rapid stop/start.
- Final `SpeechFollower` should not reintroduce broad long-lived system stream muting unless a new real-device beep regression requires it and restore behavior is scoped carefully.
