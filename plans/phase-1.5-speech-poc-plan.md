# Phase 1.5 SpeechRecognizer PoC Plan

## Goal

Implement a disposable Android `SpeechRecognizer` continuous-listening PoC before investing in the full teleprompter UI. The PoC must answer three risks from `MIGRATION_PLAN.md`:

- Whether the system beep on every `startListening()` can be suppressed.
- Whether `ERROR_RECOGNIZER_BUSY` can be avoided by delayed restart or by destroying and recreating the recognizer.
- Whether the restart gap after silence/results is acceptable on the target device.

This phase does not implement the final `SpeechFollower`, `SpeechScriptIndex`, prompter UI integration, or dictation feature.

## Assumptions

- Phase 1 data/app shell is complete.
- The PoC runs on a real device because emulator behavior is not enough for beep and restart-gap validation.
- The PoC can be temporary and may be removed or hidden before later release work.
- Android runtime microphone permission is requested by the PoC Activity.

## Android Files

- `app/src/main/kotlin/com/qiaomu/prompter/poc/SpeechRecognizerPocActivity.kt`
  - Disposable Compose screen and controller.
  - Uses `SpeechRecognizer` directly.
  - Tracks state, transcript, error codes, restart count, busy count, and restart gaps.
  - Supports delayed restart and destroy/recreate restart strategies.
  - Supports temporary `AudioManager` muting during listening.

- `app/src/main/AndroidManifest.xml`
  - Registers the PoC Activity.
  - Keeps existing `RECORD_AUDIO` permission and `android.speech.RecognitionService` query.

- `app/src/main/kotlin/com/qiaomu/prompter/MainActivity.kt`
  - Adds a temporary entry button from the Phase 1 shell to launch the PoC.

- `tmp/handoff.md`
  - Records that Phase 1.5 PoC code exists and lists the manual validation still required.

## iOS Reference Paths

- `QMPrompter/Services/SpeechFollower.swift`
  - Reference for user-facing speech states, start/stop/reset shape, authorization flow, transcript state, and later progress matching.
  - Android Phase 1.5 intentionally diverges because Android `SpeechRecognizer` stops after silence and must be restarted.

- `QMPrompter/Views/PrompterView.swift`
  - Reference for later Phase 4 integration behavior: default speech follow on entering prompter, status pill, and stopping speech follow during manual positioning.
  - Not implemented in Phase 1.5.

- `QMPrompter/Services/PromptDictation.swift`
  - Not used for this phase beyond broad context because Phase 1.5 validates continuous listening, not one-shot dictation.

## Implementation Steps

1. Add the plan document under `plans/`.
2. Add `SpeechRecognizerPocActivity` with a small Compose UI.
3. Implement the explicit PoC state loop:
   - `Idle`
   - `Starting`
   - `Listening`
   - `RestartPending`
   - `Failed`
   - `Disposed`
4. Restart after:
   - `onResults`
   - `ERROR_NO_MATCH`
   - `ERROR_SPEECH_TIMEOUT`
   - `onEndOfSpeech` fallback only if no result/error arrives soon after silence.
5. Compare restart strategies:
   - Delayed restart using 150 ms.
   - Destroy and recreate recognizer before restart.
6. Count `ERROR_RECOGNIZER_BUSY`; stop after repeated failures instead of retrying forever.
7. Add temporary muting around listening and restore volume on stop, failure, and Activity destroy.
8. Add a temporary button in `MainActivity` to open the PoC.
9. Run Gradle verification sequentially.
10. Manually validate on a real device and record results.

## Acceptance

Command-line verification:

```powershell
rtk ./gradlew.bat test --stacktrace
rtk ./gradlew.bat assembleDebug --stacktrace
```

Manual device verification:

1. Install and launch the debug APK.
2. Open `Speech PoC` from the Phase 1 shell.
3. Grant microphone permission.
4. Start continuous listening and speak several Chinese phrases with pauses.
5. Toggle beep muting and confirm whether the system beep is suppressed.
6. Test delayed restart and destroy/recreate restart.
7. Confirm whether `ERROR_RECOGNIZER_BUSY` appears and which strategy avoids it.
8. Check displayed restart gaps and decide whether they are acceptable.
9. Stop listening and exit the Activity; confirm the device volume is restored.

Final Phase 1.5 result must explicitly state:

- Beep can or cannot be suppressed.
- Busy can or cannot be avoided, with the chosen strategy.
- Restart gap is or is not acceptable.

If any item fails, do not proceed with the final `SpeechFollower` design until the voice-following approach is revised.
