# Handoff - Phase 3 Implemented

## Project Goal

QMPrompter Android is a Kotlin/Jetpack Compose migration of the existing iOS SwiftUI teleprompter app. Android project files live at the repository root, with the app module under `app/`. The iOS implementation remains in `QMPrompter/` for behavior reference.

Feature target: parity with the current iOS app using Jetpack Compose, Room, CameraX, Android SpeechRecognizer, and OkHttp for OpenAI-compatible AI generation.

## Current Status

- Phase 1 complete: project shell, Room data layer, AI provider config storage, prompt formatter, Compose/NavHost app entry.
- Phase 1.5 complete: disposable `SpeechRecognizer` continuous-listening PoC implemented and tested on a real device.
- Phase 2 complete: list, editor, reusable glass action panel, and AI provider settings UI.
- Phase 3 implemented: CameraX preview, prompter screen skeleton, scroll engine, gestures, keep-screen-on, and a lightweight front/back camera flip control.
- Phase 3 command verification passes.
- Phase 3 still needs manual device validation, especially camera permission paths, camera flip behavior, background/lock recovery, and long-script scroll smoothness.
- Editor explicit save actions now show a short "已保存" snackbar. It is offset above the bottom save button and dismisses after about 0.5 seconds. Auto-save on back/start-prompter remains silent.

Current git state at this handoff:

- Branch: `main`
- Branch relation: `main...origin/main [ahead 4]`
- Worktree has uncommitted Phase 3 changes:
  - Modified:
    - `app/src/main/kotlin/com/qiaomu/prompter/MainActivity.kt`
    - `app/src/main/kotlin/com/qiaomu/prompter/ui/editor/ScriptEditorScreen.kt`
  - Untracked:
    - `app/src/main/kotlin/com/qiaomu/prompter/ui/camera/`
    - `app/src/main/kotlin/com/qiaomu/prompter/ui/prompter/`
    - `plans/phase-3-implementation-plan.md`
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

## Phase 3 Plan

The approved Phase 3 plan is saved at:

- `plans/phase-3-implementation-plan.md`

Phase 3 scope:

1. Camera preview with CameraX.
2. Scroll engine with frame-based delta-time math.
3. Prompter screen skeleton with camera background, dark overlay, scrolling text, and keep-screen-on behavior.
4. Gesture controls: left-side speed drag, center manual scroll, right-side progress drag, and tap to play/pause.
5. Lightweight camera flip: default to front camera, with an in-prompter control to switch between front and back cameras.

Explicitly out of Phase 3:

- Final speech following.
- Current-line speech highlighting.
- Dictation.
- AI generation.
- Persisted camera selection or a camera settings system.

## Phase 3 Implementation Summary

New files:

- `app/src/main/kotlin/com/qiaomu/prompter/ui/camera/CameraPreview.kt`
  - Compose `AndroidView` wrapper around CameraX `PreviewView`.
  - Requests camera permission with `rememberLauncherForActivityResult`.
  - Binds CameraX `Preview` to the current lifecycle.
  - Accepts `lensFacing` so the prompter can flip front/back cameras.
  - Reports `Checking`, `Authorized`, `Denied`, and `Unavailable` states.

- `app/src/main/kotlin/com/qiaomu/prompter/ui/prompter/ScrollEngine.kt`
  - Lightweight state holder, no ViewModel.
  - Uses Compose `withFrameNanos`.
  - Ports iOS delta-time math:
    - speed clamp `20..260`
    - line height minimum `40`
    - average characters minimum `6`
    - visual tuning factor `1.85`
    - follow smoothing `next = offset + (target - offset) * min(1, delta * 12)`
    - snap to target below `0.5px`
    - pause automatically at maximum offset

- `app/src/main/kotlin/com/qiaomu/prompter/ui/prompter/PrompterScreen.kt`
  - Loads the selected `Script` from `ScriptRepository.scripts`.
  - Renders full-screen camera preview, overlay, prompt text, controls, and gesture zones.
  - Uses existing `PromptFormatter`.
  - Uses `TextMeasurer` for line measurement.
  - Renders only a visible line window around the viewport.
  - Applies per-line scroll offset with `graphicsLayer`.
  - Sets `FLAG_KEEP_SCREEN_ON` while active.
  - Provides back, play/pause, and camera flip controls.
  - Default camera is front-facing.
  - Gesture zones:
    - left: speed drag with cumulative translation `-translationY * 0.42`
    - center: manual scroll with cumulative translation `-translationY * 1.05`
    - right: progress drag with cumulative translation `-translationY * 1.35`
  - Top `120.dp` is reserved so gestures do not cover the control area.
  - Speed changes are saved to Room on drag end.
  - Scroll offset is not persisted.

Modified files:

- `app/src/main/kotlin/com/qiaomu/prompter/MainActivity.kt`
  - Added `prompter/{scriptId}` route.
  - Routes `PrompterScreen` with existing `ScriptRepository`.

- `app/src/main/kotlin/com/qiaomu/prompter/ui/editor/ScriptEditorScreen.kt`
  - Added `onStartPrompter` callback.
  - Added top bar play icon for "开始提词".
  - Click saves the current script before navigating to the prompter.
  - Explicit save icon/button shows "已保存" feedback for about 0.5 seconds.

## Subagent Review Fixes

A subagent reviewed the Phase 3 implementation against `plans/phase-3-implementation-plan.md` and `MIGRATION_PLAN.md`.

Findings fixed:

- Drag gestures originally used per-event delta as if it were total translation. Fixed by accumulating drag translation from drag start before applying speed/manual-scroll/progress formulas.
- Requested unavailable camera could leave UI stuck in `Unavailable`. Fixed by tracking the last available lens and allowing camera flip while unavailable.
- Full-screen gesture layer originally covered the top controls. Fixed by adding a top gesture reserve.

Finding noted but not a runtime bug:

- New files are untracked. If committing, add the new camera/prompter directories and the Phase 3 plan file.

No Phase 4 feature creep was found.

## Verification

Latest command verification passed:

- `rtk ./gradlew.bat test --stacktrace`
- `rtk ./gradlew.bat assembleDebug --stacktrace`

Known warnings:

- Existing AGP warnings about deprecated `android.builtInKotlin=false`, `android.newDsl=false`, and obsolete variant APIs.
- Existing Compose warnings in editor code.
- New warning: `LocalLifecycleOwner` import location is deprecated; fixing would require lifecycle-runtime-compose API cleanup and was intentionally not mixed into Phase 3.
- New warning: `Icons.Default.ArrowBack` is deprecated in favor of AutoMirrored; also intentionally left out of Phase 3 cleanup.

## Manual Phase 3 Acceptance Still Needed

Run on a real device or emulator:

1. From editor, tap the play icon and enter the prompter.
2. First camera permission request works.
3. Permission denied path shows a stable fallback and does not crash.
4. Default preview is front camera.
5. Camera flip switches to back camera and back to front.
6. Tap play/pause works.
7. Reaching the end pauses automatically.
8. Left drag changes speed and persists it.
9. Center drag manually scrolls.
10. Right drag changes progress.
11. Top controls remain usable and are not hijacked by drag gestures.
12. Screen stays awake while prompting.
13. Lock/background then return: camera resumes and scroll offset remains.
14. 5k and 20k character scripts scroll without obvious jank.

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
- Keep `MODIFY_AUDIO_SETTINGS` while SpeechRecognizer beep handling may need audio muting.
- Room uses `exportSchema = true`; keep generated schema files under `app/schemas/`.
- `Script.createdAt` and `Script.updatedAt` are epoch millis `Long`.
- `TextColorPreset` stores raw values only: `white`, `silver`, `graphite`.
- First-run sample data is seeded from `RoomDatabase.Callback.onCreate()` with `SupportSQLiteDatabase.execSQL`.
- `AiProviderConfigStore` SharedPreferences name must stay aligned with backup exclusion path `ai_provider_config.xml`.
- `PromptFormatter` code point counting is acceptable for current parity target.
- `.kotlin/` is local build cache/log output and should not be committed.

## iOS Reference Paths

General references:

- `QMPrompter/Models/Script.swift`
- `QMPrompter/Services/ScriptStore.swift`
- `QMPrompter/Utilities/PromptFormatter.swift`
- `QMPrompter/Views/ScriptListView.swift`
- `QMPrompter/Views/ScriptEditorView.swift`
- `QMPrompter/Views/GlassActionPanel.swift`
- `QMPrompter/Views/AppSettingsView.swift`

Phase 3 references:

- `QMPrompter/Views/PrompterView.swift`
- `QMPrompter/Services/ScrollEngine.swift`
- `QMPrompter/Services/CameraPreview.swift`

Phase 4 references for later:

- `QMPrompter/Services/SpeechFollower.swift`
- `QMPrompter/Services/PromptDictation.swift`
- `QMPrompter/Services/DeepSeekScriptGenerator.swift`
- `QMPrompter/Views/AIGenerationView.swift`

## Next Recommended Work

Immediate next step:

1. Install the debug APK on a real device.
2. Complete the manual Phase 3 acceptance checklist above.
3. Fix any real-device CameraX or gesture issues found during manual validation.
4. If validation passes, commit Phase 3 changes, including untracked files.

After Phase 3 manual validation:

- Start Phase 4 from `MIGRATION_PLAN.md`:
  - final `SpeechFollower`
  - `SpeechScriptIndex`
  - current-line highlighting
  - dictation
  - OpenAI-compatible script generation
  - AI generation screen

Keep the temporary speech test entry until final `SpeechFollower` makes it obsolete, but do not expose `Speech PoC` or phase wording in production-facing UI.
