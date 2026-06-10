# Handoff - Phase 2 Complete

## Project Goal

QMPrompter Android is a Kotlin/Jetpack Compose migration of the existing iOS SwiftUI teleprompter app. Android project files live at the repository root, with the app module under `app/`. The iOS implementation remains in `QMPrompter/` for behavior reference.

Primary reference documents:

- `MIGRATION_PLAN.md`
- `plans/phase-1-implementation-plan.md`
- `plans/phase-1.5-speech-poc-plan.md`
- `plans/phase-1.5-speech-poc-result.md`
- `plans/phase-2-implementation-plan.md`

## Current Status

- Phase 1 is complete: project shell, Room data layer, AI provider config storage, prompt formatter, Compose/NavHost app entry.
- Phase 1.5 is complete: disposable `SpeechRecognizer` continuous-listening PoC implemented and tested on a real device.
- Phase 2 is complete: list, editor, reusable glass action panel, and AI provider settings UI.
- Recommended next phase: Phase 3, camera preview + scroll engine + prompter screen skeleton/gestures from `MIGRATION_PLAN.md`.

Current git state at this handoff:

- Branch: `main`
- Branch relation: `main...origin/main [ahead 3]`
- Worktree has uncommitted Phase 2 changes:
  - Modified:
    - `app/src/main/kotlin/com/qiaomu/prompter/MainActivity.kt`
    - `tmp/handoff.md`
  - Added:
    - `app/src/main/kotlin/com/qiaomu/prompter/ui/component/GlassActionPanel.kt`
    - `app/src/main/kotlin/com/qiaomu/prompter/ui/scriptlist/ScriptCard.kt`
    - `app/src/main/kotlin/com/qiaomu/prompter/ui/scriptlist/ScriptListScreen.kt`
    - `app/src/main/kotlin/com/qiaomu/prompter/ui/editor/ScriptEditorScreen.kt`
    - `app/src/main/kotlin/com/qiaomu/prompter/ui/settings/AppSettingsScreen.kt`
    - `plans/phase-2-implementation-plan.md`

## Tooling

- Kotlin 2.2.10
- Android Gradle Plugin 9.2.0
- Gradle Wrapper 9.4.1
- JDK 21
- Jetpack Compose Material3
- Navigation Compose
- Room 2.8.4 + KSP
- CameraX
- Android SpeechRecognizer
- OkHttp + Coroutines
- JUnit 4

Important Gradle flags to keep for now:

- `android.builtInKotlin=false`
- `android.newDsl=false`
- `android.useAndroidX=true`

Removing the first two previously caused AGP/Kotlin plugin failures. Build output still contains AGP deprecation warnings; treat that as separate tooling cleanup, not feature-phase work.

## Phase 1 Summary

Implemented data and app foundation:

- `app/src/main/kotlin/com/qiaomu/prompter/data/Script.kt`
- `app/src/main/kotlin/com/qiaomu/prompter/data/TextColorPreset.kt`
- `app/src/main/kotlin/com/qiaomu/prompter/data/Converters.kt`
- `app/src/main/kotlin/com/qiaomu/prompter/data/ScriptDao.kt`
- `app/src/main/kotlin/com/qiaomu/prompter/data/AppDatabase.kt`
- `app/src/main/kotlin/com/qiaomu/prompter/data/ScriptRepository.kt`
- `app/src/main/kotlin/com/qiaomu/prompter/settings/AiProviderConfigStore.kt`
- `app/src/main/kotlin/com/qiaomu/prompter/util/PromptFormatter.kt`
- `app/src/test/kotlin/com/qiaomu/prompter/util/PromptFormatterTest.kt`
- `app/src/main/kotlin/com/qiaomu/prompter/QMPrompterApp.kt`

Key Phase 1 decisions:

- Android data is new; do not import iOS `scripts.json`.
- `Script.createdAt` and `Script.updatedAt` are epoch millis `Long`.
- `TextColorPreset` stores raw values only: `white`, `silver`, `graphite`.
- Do not add iOS legacy aliases such as `yellow` or `green` unless an explicit import requirement is added.
- Room uses `version = 1` and `exportSchema = true`.
- Generated schema files under `app/schemas/` are migration baselines.
- First-run sample script is seeded synchronously from `RoomDatabase.Callback.onCreate()` with `SupportSQLiteDatabase.execSQL`.
- `AiProviderConfigStore` uses SharedPreferences name `ai_provider_config`; backup exclusions must continue to exclude `ai_provider_config.xml`.
- `PromptFormatter` is ported from `QMPrompter/Utilities/PromptFormatter.swift`.
- Prompt formatter code-point counting is acceptable for current Chinese/ASCII punctuation parity. Emoji grapheme cluster parity is not a Phase 1 requirement.

## Phase 1.5 Summary

Implemented disposable PoC:

- `app/src/main/kotlin/com/qiaomu/prompter/poc/SpeechRecognizerPocActivity.kt`
- Registered in `app/src/main/AndroidManifest.xml`.
- Temporary speech test entry is still reachable from the list screen.
- Added `MODIFY_AUDIO_SETTINGS` permission for the beep-muting experiment.

Real-device Phase 1.5 results:

- `ERROR_RECOGNIZER_BUSY` was not reproduced.
- `Destroy/recreate` was more stable than `Delay 150ms`.
- `ERROR_NO_MATCH` occurs.
- `ERROR_NETWORK_TIMEOUT` occurs.
- No audible system beep was heard on the tested device.
- With `Destroy/recreate`, observed restart gap was about 152 ms and acceptable.

Phase 4 recommendation based on PoC:

- Implement final `SpeechFollower` with destroy/recreate as the first restart strategy.
- Keep delayed restart as a fallback only if another tested device shows worse behavior with destroy/recreate.
- Treat `ERROR_NO_MATCH` and `ERROR_SPEECH_TIMEOUT` as normal recoverable events and restart.
- Treat `ERROR_NETWORK_TIMEOUT` as recoverable, but show a readable state such as `语音网络不稳定` if it repeats.
- Keep busy counting and a failure threshold even though busy was not reproduced.
- Never retry forever; repeated busy/network failures should enter a failed state until the user manually restarts voice following.

## Phase 2 Summary

Implemented UI files:

- `app/src/main/kotlin/com/qiaomu/prompter/ui/component/GlassActionPanel.kt`
- `app/src/main/kotlin/com/qiaomu/prompter/ui/scriptlist/ScriptCard.kt`
- `app/src/main/kotlin/com/qiaomu/prompter/ui/scriptlist/ScriptListScreen.kt`
- `app/src/main/kotlin/com/qiaomu/prompter/ui/editor/ScriptEditorScreen.kt`
- `app/src/main/kotlin/com/qiaomu/prompter/ui/settings/AppSettingsScreen.kt`
- `plans/phase-2-implementation-plan.md`

Modified app wiring:

- `app/src/main/kotlin/com/qiaomu/prompter/MainActivity.kt`
  - Replaced the temporary shell screen with `NavHost` routes:
    - `scripts`
    - `editor/{scriptId}`
    - `settings`
  - Passes `ScriptRepository` and `AiProviderConfigStore` from `QMPrompterApp`.
  - Keeps the temporary speech test entry by launching `SpeechRecognizerPocActivity`.

Implemented behavior:

- Script list:
  - Observes `ScriptRepository.scripts`.
  - Searches title and content.
  - Creates a blank draft with `Script.createDraft()` then saves and opens it.
  - Opens existing scripts in the editor.
  - Supports swipe-to-delete.
  - Opens AI settings.
  - Keeps temporary speech test entry.

- Script card:
  - Shows title, preview, updated date, content length, font size, and scroll speed.
  - Uses shared glass-style surface.

- Editor:
  - Loads script by id from repository state.
  - Tabs: `文稿` and `显示`.
  - Supports title editing dialog.
  - Supports body editing, paste, and clear-body confirmation panel.
  - Saves on explicit save and back navigation.
  - Normalizes empty title to `Script.UNTITLED`.
  - Clamps font size to `12..110`, scroll speed to `20..220`, and overlay opacity to `0.18..0.82`.
  - Text color choices use `TextColorPreset.editorChoices`.
  - Camera transparency UI maps to `overlayOpacity` with reverse mapping: `1 - transparency`, clamped to `0.18..0.82`.

- Settings:
  - Reads/writes existing `AiProviderConfigStore`.
  - Inputs: API Key, Base URL, Model.
  - Base URL placeholder: `https://api.deepseek.com`.
  - Model placeholder: `deepseek-v4-flash`.
  - Explicit save button writes all fields at once.

Phase 2 scope guard:

- No CameraX preview was implemented.
- No `ScrollEngine` was implemented.
- No `PrompterScreen` was implemented.
- No final `SpeechFollower` integration was implemented.
- No AI generation/network implementation was added.
- No ViewModel, DI framework, XML UI, schema change, DataStore, or broad test infrastructure was added.

Subagent review after implementation:

- A subagent checked Phase 2 implementation against the plan.
- It found no Phase 3/4 mixing and no architecture/schema violations.
- It flagged two visible internal/explanatory UI strings:
  - `Speech PoC` content description in the list entry.
  - Settings helper text explaining default behavior.
- Both were fixed:
  - Speech entry content description is now `语音测试`.
  - Settings screen now only shows `已保存` after saving; default values remain as placeholders.

## Known Warnings

Gradle/AGP warnings:

- `android.builtInKotlin=false` is deprecated.
- `android.newDsl=false` is deprecated.
- Legacy AGP variant API warnings appear.
- These are known compatibility warnings and should not be mixed into feature phases.

Compose warnings:

- `rememberSwipeToDismissBoxState(confirmValueChange = ...)` is deprecated in current Material3.
- `Icons.Filled.ArrowBack` warns to use AutoMirrored in one settings import path.
- These warnings do not fail build. They were left alone to avoid expanding Phase 2 beyond the requested implementation.

## Verification

Last successful command-line verification after Phase 2 and subagent-review fixes:

```powershell
rtk ./gradlew.bat test --stacktrace
rtk ./gradlew.bat assembleDebug --stacktrace
```

Both passed with `BUILD SUCCESSFUL`.

Run Gradle verification sequentially. Do not run `test` and `assembleDebug` in parallel because Kotlin incremental compilation previously raced on `app/build/tmp/kotlin-classes`.

Manual Phase 2 checks still recommended on device/emulator:

1. First launch shows the seeded sample script.
2. New script can be created and opened.
3. Title/body edits persist after returning to list.
4. Paste appends clipboard text when body is non-empty and replaces when empty.
5. Clear body keeps title and display settings.
6. Font size, scroll speed, text color, and camera transparency persist.
7. Search filters by title and body.
8. Swipe/delete removes a script.
9. Killing and reopening the app preserves created/edited/deleted data.
10. Settings page saves API Key, Base URL, and Model; reopening settings reads them back.
11. Temporary speech test entry still opens the PoC activity.

## Constraints To Preserve

- Always read `C:\Users\Max\.codex\RTK.md` first at session start before commands.
- Use `rtk` for ordinary external CLI commands.
- Use `rg` for code/log/plain-text search; if `rtk` is available, use `rtk rg`.
- Do not introduce DI frameworks such as Hilt, Dagger, or Koin.
- Do not introduce ViewModel layer or lifecycle-viewmodel-compose.
- Do not introduce XML/AppCompat/RecyclerView UI for new Android screens.
- Do not introduce `androidx.security:security-crypto` or EncryptedSharedPreferences.
- Do not introduce DataStore unless explicitly requested.
- Do not implement iOS data import from `scripts.json`.
- Do not add broad UI automation or test infrastructure.
- Keep app portrait-only.
- Keep Manifest permissions:
  - `CAMERA`
  - `RECORD_AUDIO`
  - `INTERNET`
  - `MODIFY_AUDIO_SETTINGS` while the PoC or final beep handling needs it
- Keep Manifest `queries` for `android.speech.RecognitionService`.
- Keep backup exclusions aligned with `AiProviderConfigStore` SharedPreferences file `ai_provider_config.xml`.
- Keep `.kotlin/` ignored; it contains local Kotlin build cache/logs.
- Keep `local.properties` machine-specific and ignored.
- Do not expose internal phase/PoC/TODO explanatory wording in production UI. Temporary PoC activity itself still contains diagnostic UI by design until it is removed.

## iOS Reference Paths

General references:

- `QMPrompter/Models/Script.swift`
- `QMPrompter/Utilities/PromptFormatter.swift`
- `QMPrompter/Services/ScrollEngine.swift`
- `QMPrompter/Services/SpeechFollower.swift`
- `QMPrompter/Services/PromptDictation.swift`
- `QMPrompter/Services/DeepSeekScriptGenerator.swift`
- `QMPrompter/Views/PrompterView.swift`
- `QMPrompter/Views/AIGenerationView.swift`

Phase 2 references already used:

- `QMPrompter/Views/ScriptListView.swift`
- `QMPrompter/Views/ScriptEditorView.swift`
- `QMPrompter/Views/GlassActionPanel.swift`
- `QMPrompter/Views/AppSettingsView.swift`
- `QMPrompter/Services/ScriptStore.swift`
- `QMPrompter/Models/Script.swift`

Phase 3 references to use next:

- `QMPrompter/Views/PrompterView.swift`
- `QMPrompter/Services/ScrollEngine.swift`
- `MIGRATION_PLAN.md` Phase 3 and key technical details:
  - Camera preview.
  - `ScrollEngine` delta-time math.
  - `graphicsLayer`/draw-phase offset reads to avoid per-frame full recomposition.
  - Visible line windowing.
  - Gesture mapping.
  - Keep screen on.

## Next Recommended Work

Start Phase 3 from `MIGRATION_PLAN.md`:

1. Implement `CameraPreview` with CameraX + `PreviewView` in `AndroidView`.
2. Implement `ScrollEngine` with `withFrameNanos`:
   - speed clamp `20..260`
   - visual tuning factor `1.85`
   - follow smoothing formula for later Phase 4 use
3. Implement `PrompterScreen` skeleton:
   - camera background
   - overlay opacity from `Script.overlayOpacity`
   - scrolling text
   - keep screen on while active
   - route from editor/list when ready
4. Implement gesture layer:
   - left third speed
   - middle manual scroll
   - right third progress
   - persist on drag end

Do not integrate final speech following until Phase 4. Keep the temporary speech test entry until final `SpeechFollower` makes it obsolete.

## Common Commands

```powershell
rtk git status --short --branch
rtk rg "<pattern>"
rtk ./gradlew.bat test --stacktrace
rtk ./gradlew.bat assembleDebug --stacktrace
rtk ./gradlew.bat installDebug --stacktrace
```
