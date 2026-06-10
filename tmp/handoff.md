# Handoff - Phase 4 Implemented

## Project Goal

QMPrompter Android is a Kotlin/Jetpack Compose migration of the existing iOS SwiftUI teleprompter app. Android project files live at the repository root, with the app module under `app/`. The iOS implementation remains in `QMPrompter/` for behavior reference.

Feature target: parity with the current iOS app using Jetpack Compose, Room, CameraX, Android SpeechRecognizer, and OkHttp for OpenAI-compatible AI generation.

## Current Status

- Phase 1 complete: project shell, Room data layer, AI provider config storage, prompt formatter, Compose/NavHost app entry.
- Phase 1.5 complete: disposable `SpeechRecognizer` continuous-listening PoC implemented and tested on a real device.
- Phase 2 complete: list, editor, reusable glass action panel, and AI provider settings UI.
- Phase 3 complete and manually validated by the user on a real device.
- Phase 4 implemented:
  - final speech-following implementation
  - `SpeechScriptIndex` port and unit tests
  - current-line highlighting while voice following
  - AI prompt dictation
  - OpenAI-compatible AI script generation
  - AI generation screen and homepage entry
- Product decision after validation: voice following is **default off** when entering the prompter. Users manually enable it with the top microphone button.
- `Speech PoC` is no longer exposed in the production homepage UI. The PoC Activity/file still exists for reference and was not deleted.

Current git state at this handoff:

- Branch: `main`
- Branch relation: `main...origin/main [ahead 5]`
- Worktree has uncommitted Phase 4 changes:
  - Modified:
    - `app/src/main/kotlin/com/qiaomu/prompter/MainActivity.kt`
    - `app/src/main/kotlin/com/qiaomu/prompter/ui/prompter/PrompterScreen.kt`
    - `app/src/main/kotlin/com/qiaomu/prompter/ui/scriptlist/ScriptListScreen.kt`
  - Untracked:
    - `app/src/main/kotlin/com/qiaomu/prompter/ai/`
    - `app/src/main/kotlin/com/qiaomu/prompter/speech/`
    - `app/src/main/kotlin/com/qiaomu/prompter/ui/ai/`
    - `app/src/test/kotlin/com/qiaomu/prompter/speech/`
    - `plans/phase-4-implementation-plan.md`
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

## Phase 4 Plan

The Phase 4 implementation plan is saved at:

- `plans/phase-4-implementation-plan.md`

The plan originally followed `MIGRATION_PLAN.md` Phase 4. One intentional product change was made after user validation:

- Voice following does **not** auto-start on entering `PrompterScreen`; it starts only when the user taps the microphone button.

This intentionally differs from `MIGRATION_PLAN.md` item 25 and should be preserved unless the user changes their mind.

## Phase 4 Implementation Summary

New files:

- `app/src/main/kotlin/com/qiaomu/prompter/speech/SpeechScriptIndex.kt`
  - Kotlin port of iOS `SpeechScriptIndex`.
  - Normalizes content/transcripts to letters and digits.
  - Uses candidate fragments, committed offset, plausibility checks, and scoring.
  - Keeps progress monotonic.

- `app/src/test/kotlin/com/qiaomu/prompter/speech/SpeechScriptIndexTest.kt`
  - Focused JUnit tests for normalization, Chinese transcript progress, monotonic progress, initial-progress anchoring, and empty input.

- `app/src/main/kotlin/com/qiaomu/prompter/speech/SpeechFollower.kt`
  - Android `SpeechRecognizer`-based continuous listener.
  - States: `Idle`, `Starting`, `Listening`, `RestartPending`, `Denied`, `Unavailable`, `Disposed`, `Failed`.
  - Uses destroy/recreate restart strategy with `150ms` delay.
  - Uses `sessionToken` so stale delayed restarts cannot revive an old session.
  - Treats `ERROR_NO_MATCH`, `ERROR_SPEECH_TIMEOUT`, and `ERROR_NETWORK_TIMEOUT` as recoverable until thresholds.
  - Counts busy/network/recoverable failures and fails with readable messages instead of retrying forever.
  - Does not mute system audio streams. Phase 1.5 found no audible beep on the tested device; broad muting was removed after review.

- `app/src/main/kotlin/com/qiaomu/prompter/speech/PromptDictation.kt`
  - Simpler `SpeechRecognizer` wrapper for AI prompt voice input.
  - Exposes `isRecording`, `transcript`, and `errorMessage`.
  - Cleans up recognizer on stop.

- `app/src/main/kotlin/com/qiaomu/prompter/ai/ScriptGenerator.kt`
  - Minimal generator interface plus `ScriptGenerationException`.

- `app/src/main/kotlin/com/qiaomu/prompter/ai/OpenAICompatGenerator.kt`
  - OkHttp implementation for OpenAI-compatible `/chat/completions`.
  - Uses `AiProviderConfigStore`.
  - Empty base URL/model fall back to:
    - `https://api.deepseek.com`
    - `deepseek-v4-flash`
  - Adds DeepSeek-only `thinking: { type: "disabled" }` only when effective base URL contains `deepseek.com`.
  - Uses `max_tokens: 2800`, non-streaming requests, iOS system prompt, and generated-script cleanup logic.

- `app/src/main/kotlin/com/qiaomu/prompter/ui/ai/AIGenerationScreen.kt`
  - Prompt text input.
  - Voice input button using `PromptDictation`.
  - Generate button using `ScriptGenerator`.
  - Saves generated content as a new `Script`.
  - Navigates to the editor for the generated script.
  - If prompt already has typed text, dictation appends instead of replacing.

Modified files:

- `app/src/main/kotlin/com/qiaomu/prompter/MainActivity.kt`
  - Adds `ai-generation` route.
  - Instantiates `OpenAICompatGenerator`.
  - Routes generated scripts to the editor.
  - Removes production homepage `Speech PoC` entry wiring.

- `app/src/main/kotlin/com/qiaomu/prompter/ui/scriptlist/ScriptListScreen.kt`
  - Adds `AI 生成` action in the create panel.
  - Keeps `空白文稿`.
  - Removes visible speech PoC button from homepage.

- `app/src/main/kotlin/com/qiaomu/prompter/ui/prompter/PrompterScreen.kt`
  - Adds `SpeechFollower` integration.
  - Adds top microphone toggle.
  - Voice following starts manually only.
  - When voice following has transcript, progress maps to current line and target scroll offset.
  - Current line renders white with glow/shadow; non-highlight lines render at 62% alpha.
  - Startup/restart pending states do not highlight the first line before transcript exists.
  - In voice-following mode:
    - taps do not toggle speed playback
    - center/manual drag stops voice following and allows manual scroll
    - left/right drag are ignored
  - Disposes speech follower on exit.

## Subagent Review Fixes

A subagent reviewed the Phase 4 implementation against `plans/phase-4-implementation-plan.md` and `MIGRATION_PLAN.md`.

Findings fixed:

- Stale delayed restarts in `SpeechFollower` could race with rapid stop/start. Fixed with `sessionToken`.
- Broad long-lived stream muting could mute media/notifications for the whole prompt session. Removed system audio muting.
- First line could be highlighted during `Starting`/`RestartPending` before any speech was recognized. Fixed by requiring `hasTranscript` for highlight/follow.
- AI dictation could replace typed prompt text. Fixed by appending dictation when prompt already has text.

## Verification

Latest command verification passed after the default-off voice-following change:

- `rtk ./gradlew.bat test --stacktrace`
- `rtk ./gradlew.bat assembleDebug --stacktrace`

Known warnings:

- Existing AGP warnings about deprecated `android.builtInKotlin=false`, `android.newDsl=false`, and obsolete variant APIs.
- Existing/expected Compose warning: `Icons.Default.ArrowBack` deprecated in favor of AutoMirrored.
- Existing Material3 warning: `rememberSwipeToDismissBoxState(confirmValueChange = ...)` deprecated.

These warnings were intentionally not mixed into Phase 4 cleanup.

## Manual Acceptance To Run Next

Run on a real device:

1. Homepage create panel shows `AI 生成` and `空白文稿`.
2. Homepage does not show `Speech PoC` or phase/test wording.
3. Entering prompter does not request microphone and does not auto-start voice following.
4. Top microphone button requests microphone permission and starts voice following.
5. When speech is recognized, current line highlights and scrolling follows smoothly.
6. Startup/restart gaps do not highlight the first line before transcript exists.
7. Silence/no match/network timeout recovers or fails with readable status; no infinite retry.
8. Rapidly toggle microphone on/off; no duplicate recognizer loop or stuck microphone.
9. Manual center drag during voice following stops voice following and allows manual scroll.
10. Exit prompter; microphone is released.
11. AI generation with only API Key uses DeepSeek defaults.
12. AI generation with a third-party OpenAI-compatible base URL/model works.
13. Missing API Key, no network, HTTP failure, and empty response show readable errors.
14. Generated script is saved, appears in the list, opens in editor, and survives app restart.
15. AI prompt dictation works; if text already exists, speech text appends instead of replacing.

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

Phase 4 references:

- `QMPrompter/Services/SpeechFollower.swift`
- `QMPrompter/Services/PromptDictation.swift`
- `QMPrompter/Services/DeepSeekScriptGenerator.swift`
- `QMPrompter/Views/AIGenerationView.swift`
- `QMPrompter/Views/PrompterView.swift`
- `QMPrompter/Views/ScriptListView.swift`

## Next Recommended Work

Immediate next steps:

1. Install the debug APK on a real device.
2. Run the manual Phase 4 acceptance checklist above, especially voice-following toggle/restart behavior and AI provider error paths.
3. If manual validation passes, commit Phase 4 changes including new untracked directories and `plans/phase-4-implementation-plan.md`.

Likely later work:

- Phase 5 from `MIGRATION_PLAN.md`: immersive mode, final glass polish, and broader permission-denial/settings guidance.
- Optional cleanup only if requested: remove or archive `SpeechRecognizerPocActivity` and its manifest entry after final speech following is trusted.
