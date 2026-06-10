# Phase 4 Implementation Plan

## Scope

Implement Phase 4 from `MIGRATION_PLAN.md`:

1. Final `SpeechFollower`.
2. `SpeechScriptIndex` matching logic and focused unit tests.
3. Current-line highlighting while voice following.
4. `PromptDictation`.
5. OpenAI-compatible AI script generation.
6. `AIGenerationScreen`.
7. Default voice following when entering the prompter screen.

Do not implement Phase 5 work, broad test infrastructure, DI, ViewModel, XML UI, iOS data import, or new provider systems.

## Assumptions

- Phase 3 real-device validation has passed.
- Use the Phase 1.5 SpeechRecognizer PoC result as the Android voice-following baseline.
- Prefer destroy/recreate for continuous listening restarts.
- Treat `ERROR_NO_MATCH`, `ERROR_SPEECH_TIMEOUT`, and `ERROR_NETWORK_TIMEOUT` as recoverable, but do not retry forever.
- Keep transcript segments independent across Android recognition restarts; feed each segment to one persistent `SpeechScriptIndex`.
- Use existing `AiProviderConfigStore`; empty base URL/model fall back to DeepSeek defaults.

## Implementation Approach

1. Implement pure speech matching first:
   - Port `SpeechScriptIndex` from iOS.
   - Preserve candidate fragments, committed offset monotonic progress, plausibility checks, scoring, and normalization.
   - Add JUnit tests before UI integration.

2. Implement final Android `SpeechFollower`:
   - Main-thread `SpeechRecognizer` state machine:
     `Idle -> Starting -> Listening -> RestartPending -> Starting`.
   - `Disposed` is terminal for screen exit or mode switch.
   - Use destroy/recreate restart strategy first.
   - Surface readable failed/unavailable states.
   - Restore any temporary audio changes on all stop/dispose/error paths.

3. Integrate voice following into `PrompterScreen`:
   - Start voice following by default after entering the prompter.
   - Convert speech progress to line index and target scroll offset, then call `ScrollEngine.follow(to)`.
   - Highlight current line in voice mode.
   - In voice mode, tapping does not toggle speed playback; manual center drag stops voice following.
   - Keep draw-phase scrolling behavior intact.

4. Implement `PromptDictation`:
   - Simpler one-shot/toggle SpeechRecognizer flow for AI prompt input.
   - Provide recording state, transcript, readable error messages, and cleanup.

5. Implement AI generation:
   - Add `ScriptGenerator` interface.
   - Add `OpenAICompatGenerator` using OkHttp and coroutines.
   - Build `{effectiveBaseUrl}/chat/completions`.
   - Empty config values use:
     - `https://api.deepseek.com`
     - `deepseek-v4-flash`
   - Attach DeepSeek-only `thinking: { type: "disabled" }` only when the effective base URL contains `deepseek.com`.
   - Port iOS system prompt, user prompt, and `cleanGeneratedScript`.

6. Implement `AIGenerationScreen` and navigation:
   - Add homepage AI generation entry.
   - Allow typed or dictated prompt.
   - Generate and save a new `Script`.
   - Navigate using existing Compose/NavHost patterns with minimal changes.

## Android Files

Expected new files:

- `app/src/main/kotlin/com/qiaomu/prompter/speech/SpeechScriptIndex.kt`
- `app/src/main/kotlin/com/qiaomu/prompter/speech/SpeechFollower.kt`
- `app/src/main/kotlin/com/qiaomu/prompter/speech/PromptDictation.kt`
- `app/src/main/kotlin/com/qiaomu/prompter/ai/ScriptGenerator.kt`
- `app/src/main/kotlin/com/qiaomu/prompter/ai/OpenAICompatGenerator.kt`
- `app/src/main/kotlin/com/qiaomu/prompter/ui/ai/AIGenerationScreen.kt`
- `app/src/test/kotlin/com/qiaomu/prompter/speech/SpeechScriptIndexTest.kt`

Expected modified files:

- `app/src/main/kotlin/com/qiaomu/prompter/ui/prompter/PrompterScreen.kt`
- `app/src/main/kotlin/com/qiaomu/prompter/ui/prompter/ScrollEngine.kt` only if required
- `app/src/main/kotlin/com/qiaomu/prompter/ui/scriptlist/ScriptListScreen.kt`
- `app/src/main/kotlin/com/qiaomu/prompter/MainActivity.kt`
- `app/build.gradle.kts` only if OkHttp is missing
- `app/src/main/AndroidManifest.xml` only if required declarations are missing

## iOS Reference Paths

- `QMPrompter/Services/SpeechFollower.swift`
- `QMPrompter/Services/PromptDictation.swift`
- `QMPrompter/Services/DeepSeekScriptGenerator.swift`
- `QMPrompter/Views/AIGenerationView.swift`
- `QMPrompter/Views/PrompterView.swift`
- `QMPrompter/Views/ScriptListView.swift`

## Acceptance

Command verification, sequentially:

1. `rtk ./gradlew.bat test --stacktrace`
2. `rtk ./gradlew.bat assembleDebug --stacktrace`

Manual real-device acceptance:

1. Entering the prompter starts voice following by default.
2. Continuous listening has no obvious beep and does not enter a busy loop.
3. `ERROR_NO_MATCH` and transient network errors recover or degrade with readable status.
4. Spoken progress advances monotonically and scrolls smoothly.
5. Current spoken line is highlighted; non-voice mode has no current-line highlight.
6. Manual center drag stops voice following.
7. Voice service unavailable path shows a readable fallback.
8. AI generation with only API Key uses DeepSeek defaults.
9. AI generation with a third-party OpenAI-compatible base URL/model works.
10. Missing API Key, network failure, HTTP failure, and empty response show readable errors.
11. Generated scripts save to Room and survive app restart.
