# Handoff - Phase 1 + Phase 1.5 Complete

## Project Goal

QMPrompter Android is a Kotlin/Jetpack Compose migration of the existing iOS SwiftUI teleprompter app. The Android project lives at the repository root, with the app module under `app/`. The iOS implementation remains in `QMPrompter/` for behavior reference.

Primary reference documents:

- `MIGRATION_PLAN.md`
- `plans/phase-1-implementation-plan.md`
- `plans/phase-1.5-speech-poc-plan.md`
- `plans/phase-1.5-speech-poc-result.md`

## Current Status

- Phase 1 is complete: project shell, Room data layer, AI provider config storage, prompt formatter, minimal Compose/NavHost shell.
- Phase 1.5 is complete: disposable `SpeechRecognizer` continuous-listening PoC implemented and tested on a real device.
- Recommended next phase: Phase 2, list + editor UI.

Current git state at handoff time:

- Branch: `main`
- Branch relation: `main...origin/main [ahead 2]`
- Worktree has uncommitted Phase 1.5 changes:
  - Modified:
    - `app/src/main/AndroidManifest.xml`
    - `app/src/main/kotlin/com/qiaomu/prompter/MainActivity.kt`
    - `tmp/handoff.md`
  - Added:
    - `app/src/main/kotlin/com/qiaomu/prompter/poc/SpeechRecognizerPocActivity.kt`
    - `plans/phase-1.5-speech-poc-plan.md`
    - `plans/phase-1.5-speech-poc-result.md`

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

Removing the first two previously caused AGP/Kotlin plugin failures. Build output still contains AGP deprecation warnings; treat that as a separate tooling cleanup later, not part of feature phases.

## Phase 1 Summary

Implemented data and app shell files:

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
- `app/src/main/kotlin/com/qiaomu/prompter/MainActivity.kt`

Key Phase 1 decisions:

- Android data is new; do not import iOS `scripts.json`.
- `Script.createdAt` and `Script.updatedAt` are epoch millis `Long`.
- `TextColorPreset` stores raw values only: `white`, `silver`, `graphite`.
- No iOS legacy aliases such as `yellow` or `green`.
- Room uses `version = 1` and `exportSchema = true`.
- Generated schema is under `app/schemas/` and should be committed as migration baseline.
- First-run sample script is seeded synchronously from `RoomDatabase.Callback.onCreate()` with `SupportSQLiteDatabase.execSQL`.
- `AiProviderConfigStore` uses SharedPreferences name `ai_provider_config`.
- Backup exclusions must continue to exclude `ai_provider_config.xml`.
- `PromptFormatter` is ported from `QMPrompter/Utilities/PromptFormatter.swift`.
- Prompt formatter code-point counting is acceptable for current Chinese/ASCII punctuation parity. Emoji grapheme cluster parity is not a Phase 1 requirement.

## Phase 1.5 Summary

Implemented disposable PoC:

- `app/src/main/kotlin/com/qiaomu/prompter/poc/SpeechRecognizerPocActivity.kt`
- Registered in `app/src/main/AndroidManifest.xml`.
- Temporary `Speech PoC` entry added in `MainActivity`.
- Added `MODIFY_AUDIO_SETTINGS` permission for the beep-muting experiment.

PoC capabilities:

- Requests microphone permission.
- Uses Android `SpeechRecognizer` directly.
- Shows state, transcript, restart count, busy count, failure count, last error, restart gap, audio muted/restored state, and event log.
- Supports two restart strategies:
  - `Delay 150ms`
  - `Destroy/recreate`
- Restarts after recoverable speech events/errors.
- Counts `ERROR_RECOGNIZER_BUSY`.
- Stops after repeated failures instead of retrying forever.
- Temporarily mutes audio streams when the mute toggle is enabled, and restores on stop/failure/dispose.

Real-device Phase 1.5 results:

- `ERROR_RECOGNIZER_BUSY` was not reproduced.
- `Destroy/recreate` was more stable than `Delay 150ms`.
- `ERROR_NO_MATCH` occurs.
- `ERROR_NETWORK_TIMEOUT` occurs.
- No audible system beep was heard on the tested device.
- With `Destroy/recreate`, observed restart gap was about 152 ms.
- Restart gap is acceptable on the tested device.

Phase 4 recommendation based on PoC:

- Implement final `SpeechFollower` with destroy/recreate as the first restart strategy.
- Keep delayed restart as a fallback only if another tested device shows worse behavior with destroy/recreate.
- Treat `ERROR_NO_MATCH` and `ERROR_SPEECH_TIMEOUT` as normal recoverable events and restart.
- Treat `ERROR_NETWORK_TIMEOUT` as recoverable, but show a readable state such as `语音网络不稳定` if it repeats.
- Keep busy counting and a failure threshold even though busy was not reproduced.
- Never retry forever; repeated busy/network failures should enter a failed state until the user manually restarts voice following.

## iOS Reference Paths

Use these for later phases:

- `QMPrompter/Models/Script.swift`
- `QMPrompter/Utilities/PromptFormatter.swift`
- `QMPrompter/Services/ScrollEngine.swift`
- `QMPrompter/Services/SpeechFollower.swift`
- `QMPrompter/Services/PromptDictation.swift`
- `QMPrompter/Services/DeepSeekScriptGenerator.swift`
- `QMPrompter/Views/PrompterView.swift`
- `QMPrompter/Views/AIGenerationView.swift`

For Phase 2, the most relevant references are the iOS list/editor views. Locate them with:

```powershell
rtk rg "ScriptList|Editor|ScriptEditor|NavigationStack|ForEach" QMPrompter
```

## Verification

Last successful command-line verification:

```powershell
rtk ./gradlew.bat test --stacktrace
rtk ./gradlew.bat assembleDebug --stacktrace
```

Both passed with `BUILD SUCCESSFUL`.

Run Gradle verification sequentially. Do not run `test` and `assembleDebug` in parallel because Kotlin incremental compilation previously raced on `app/build/tmp/kotlin-classes`.

Known warning:

- AGP 9.2.0 deprecation warnings are expected due to compatibility flags. Do not mix this cleanup into Phase 2.

## Constraints To Preserve

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

## Next Recommended Work

Start Phase 2 from `MIGRATION_PLAN.md`:

1. Implement `ScriptListScreen`.
2. Implement `ScriptCard`.
3. Implement `ScriptEditorScreen`.
4. Implement `GlassActionPanel`.
5. Implement `AppSettingsScreen`.

Phase 2 scope guard:

- Build actual list/editor/settings UI, but do not start Phase 3 camera/scrolling work.
- Do not integrate final speech following yet; Phase 1.5 PoC result is for Phase 4.
- Keep the temporary `Speech PoC` entry until final `SpeechFollower` implementation makes it obsolete.

## Common Commands

```powershell
rtk git status --short --branch
rtk rg "<pattern>"
rtk ./gradlew.bat test --stacktrace
rtk ./gradlew.bat assembleDebug --stacktrace
rtk ./gradlew.bat installDebug --stacktrace
```
