# Handoff - Phase 5 Implemented

## Project Goal

QMPrompter Android is a Kotlin/Jetpack Compose migration of the existing iOS SwiftUI teleprompter app. Android project files live at the repository root, with the app module under `app/`. The iOS implementation remains in `QMPrompter/` for behavior reference.

Feature target: parity with the current iOS app using Jetpack Compose, Room, CameraX, Android SpeechRecognizer, and OkHttp for OpenAI-compatible AI generation.

## Current Status

- Phase 1 complete: project shell, Room data layer, AI provider config storage, prompt formatter, Compose/NavHost app entry.
- Phase 1.5 complete: disposable `SpeechRecognizer` continuous-listening PoC implemented and tested on a real device.
- Phase 2 complete: list, editor, reusable glass action panel, and AI provider settings UI.
- Phase 3 complete and manually validated by the user on a real device.
- Phase 4 complete: speech following, current-line highlighting, dictation, OpenAI-compatible generation, AI generation screen.
- Phase 5 implemented:
  - prompter-only immersive mode
  - scoped system bar restoration on exit
  - camera denied/permanently-denied UI with retry/settings actions
  - microphone denied/permanently-denied handling with settings action
  - glass surface polish for shared panels, homepage FAB/settings action, action rows, and prompter controls
  - Phase 5 plan saved at `plans/phase-5-implementation-plan.md`
- Product decision preserved: voice following is **default off** when entering `PrompterScreen`. Users manually enable it with the top microphone button.
- `Speech PoC` is still not exposed in the production homepage UI. The PoC Activity/file remains for reference.

Current git state at this handoff:

- Branch: `main`
- Branch relation before final status check: `main...origin/main [ahead 6]`
- Worktree has Phase 5 changes:
  - Modified:
    - `app/src/main/kotlin/com/qiaomu/prompter/ui/camera/CameraPreview.kt`
    - `app/src/main/kotlin/com/qiaomu/prompter/ui/component/GlassActionPanel.kt`
    - `app/src/main/kotlin/com/qiaomu/prompter/ui/prompter/PrompterScreen.kt`
  - Untracked:
    - `plans/phase-5-implementation-plan.md`
    - `tmp/handoff.md`

## Tooling

Always read `C:\Users\Max\.codex\RTK.md` before using commands at the beginning of a session.

Use `rtk` for ordinary, non-interactive plain-text commands. Use `rtk rg` for code/text search. Do not use PowerShell `Get-Content` or `Select-String` for searching. Do not use `rtk` for structured parsing, process management, env/path operations, shell syntax, interactive commands, destructive commands, or privileged operations.

Common commands:

- Build debug APK: `rtk ./gradlew.bat assembleDebug --stacktrace`
- Test: `rtk ./gradlew.bat test --stacktrace`
- Install debug APK: `rtk ./gradlew.bat installDebug --stacktrace`
- Check working tree: `rtk git status --short --branch`
- Search code/text: `rtk rg "<pattern>"`

Run Gradle verification sequentially. Do not run `test` and `assembleDebug` in parallel; Kotlin incremental compilation can race on `app/build/tmp/kotlin-classes`.

## Phase 5 Implementation Summary

New files:

- `plans/phase-5-implementation-plan.md`
  - Scope, implementation approach, iOS references, Android files, and acceptance checklist.

Modified files:

- `app/src/main/kotlin/com/qiaomu/prompter/ui/prompter/PrompterScreen.kt`
  - Hides system bars while the prompter is active and restores them on dispose.
  - Keeps existing `FLAG_KEEP_SCREEN_ON` behavior.
  - Adds app settings deep link for permanently denied camera/microphone permissions.
  - Tracks microphone permission states: not requested, requesting, authorized, denied, permanently denied.
  - Preserves pending speech initial progress while waiting for microphone permission.
  - Shows camera retry/settings actions through `CameraStatus`.
  - Applies shared `glassSurface(CircleShape)` to top prompter controls.

- `app/src/main/kotlin/com/qiaomu/prompter/ui/camera/CameraPreview.kt`
  - Adds `CameraPermissionState.PermanentlyDenied`.
  - Adds `permissionRequestKey` for explicit retry.
  - Distinguishes denied-once from permanently denied using `shouldShowRequestPermissionRationale`.
  - Refreshes authorization state on resume after returning from system settings.

- `app/src/main/kotlin/com/qiaomu/prompter/ui/component/GlassActionPanel.kt`
  - Slightly strengthens API 31+ glass tint while keeping API 26-30 fallback.
  - Keeps the shared surface readable instead of blurring the entire composable layer, which made text/icons soft with the current Compose API surface.
  - Applies shared glass styling to the panel close button, action rows, and row icon circles.

- `app/src/main/kotlin/com/qiaomu/prompter/ui/scriptlist/ScriptListScreen.kt`
  - Applies shared glass styling to the homepage settings icon button.
  - Applies shared glass styling to the homepage FAB while preserving the existing create-panel behavior.

## iOS Reference Paths

General references:

- `QMPrompter/Models/Script.swift`
- `QMPrompter/Services/ScriptStore.swift`
- `QMPrompter/Utilities/PromptFormatter.swift`
- `QMPrompter/Views/ScriptListView.swift`
- `QMPrompter/Views/ScriptEditorView.swift`
- `QMPrompter/Views/GlassActionPanel.swift`
- `QMPrompter/Views/AppSettingsView.swift`

Prompter/Phase 5 references:

- `QMPrompter/Views/PrompterView.swift`
  - `.statusBarHidden(true)`, `.persistentSystemOverlays(.hidden)`, `cameraStatusView`, `glassCapsule()`, `glassCircle()`, `glassPanel()`.
- `QMPrompter/Views/GlassActionPanel.swift`
  - Bottom action panel surface, row surface, tint, border, and shadow values.
- `QMPrompter/Services/SpeechFollower.swift`
  - Speech state/status wording reference.

Phase 4 references:

- `QMPrompter/Services/SpeechFollower.swift`
- `QMPrompter/Services/PromptDictation.swift`
- `QMPrompter/Services/DeepSeekScriptGenerator.swift`
- `QMPrompter/Views/AIGenerationView.swift`
- `QMPrompter/Views/PrompterView.swift`
- `QMPrompter/Views/ScriptListView.swift`

## Verification

Latest command verification passed after Phase 5 implementation:

- `rtk ./gradlew.bat test --stacktrace`
- `rtk ./gradlew.bat assembleDebug --stacktrace`

Known warnings:

- Existing AGP warnings about deprecated `android.builtInKotlin=false`, `android.newDsl=false`, and obsolete variant APIs.
- Existing/expected Compose warning: `Icons.Default.ArrowBack` deprecated in favor of AutoMirrored.
- New warning from current dependency surface: `LocalLifecycleOwner` moved to `androidx.lifecycle.compose`; left as-is to avoid mixing dependency/API cleanup into Phase 5.

## Manual Acceptance To Run Next

Run on a real device:

1. Entering the prompter hides status and navigation bars.
2. Leaving the prompter restores status and navigation bars on list/editor/settings screens.
3. The screen stays awake while the prompter is active.
4. First camera permission grant shows the preview.
5. Camera denied once shows a readable retry path.
6. Camera permanently denied shows a system settings action, and returning from settings refreshes the camera state.
7. Camera unavailable is not presented as a permission problem.
8. Microphone permission is requested only after tapping the microphone button.
9. Microphone denied once shows readable status and allows another user-triggered attempt.
10. Microphone permanently denied opens app details settings from the microphone button.
11. Speech recognition unavailable remains a speech status, separate from microphone permission denial.
12. Glass panels and prompter controls remain readable on API 31+ and API 26-30.
13. Homepage FAB and settings button use the same glass visual language and remain tappable/readable.
14. 5k and 20k character scripts still scroll without obvious new jank.
15. Voice following is still default off; microphone starts it manually.
16. Current-line highlight still works after speech is recognized.
17. AI generation remains reachable from the homepage create panel.

## Constraints To Preserve

- Do not introduce DI frameworks such as Hilt, Dagger, or Koin.
- Do not introduce ViewModel or lifecycle-viewmodel-compose unless explicitly requested.
- Do not introduce XML/AppCompat/RecyclerView UI for new Android screens.
- Do not introduce `androidx.security:security-crypto` or `EncryptedSharedPreferences`.
- Do not add iOS `scripts.json` import.
- Do not add UI automation, device matrix testing, Macrobenchmark, or broad test infrastructure unless explicitly requested.
- Keep `local.properties` ignored and machine-specific.
- Keep `android.builtInKotlin=false`, `android.newDsl=false`, and `android.useAndroidX=true` in `gradle.properties`.
- Manifest must keep camera, microphone, internet permissions, SpeechRecognizer queries, portrait orientation, and backup exclusions for `ai_provider_config.xml`.
- `MODIFY_AUDIO_SETTINGS` remains in manifest from earlier PoC/history, but final `SpeechFollower` currently does not mute system streams.
- Room uses `exportSchema = true`; keep generated schema files under `app/schemas/`.
- `Script.createdAt` and `Script.updatedAt` are epoch millis `Long`.
- `TextColorPreset` stores raw values only: `white`, `silver`, `graphite`.
- First-run sample data is seeded from `RoomDatabase.Callback.onCreate()` with `SupportSQLiteDatabase.execSQL`.
- `AiProviderConfigStore` SharedPreferences name must stay aligned with backup exclusion path `ai_provider_config.xml`.
- `PromptFormatter` code point counting is acceptable for current parity target.
- `.kotlin/` is local build cache/log output and should not be committed.

## Next Recommended Work

Immediate next steps:

1. Install the debug APK on a real device.
2. Run the manual Phase 5 acceptance checklist above, especially permission denial/permanent denial paths and system bar restoration.
3. If manual validation passes, commit Phase 5 changes including `plans/phase-5-implementation-plan.md`.

Optional cleanup only if requested:

- Remove or archive `SpeechRecognizerPocActivity` and its manifest entry after final speech following is trusted.
- Revisit true background-only blur with a dedicated View/RenderNode implementation if the current glass polish is not visually sufficient.
