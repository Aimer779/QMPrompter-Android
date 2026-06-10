# Phase 1.5 SpeechRecognizer PoC Result

## Test Context

- Test target: real Android device, not emulator.
- PoC entry: `Speech PoC` button in the Phase 1 shell.
- Code path: `app/src/main/kotlin/com/qiaomu/prompter/poc/SpeechRecognizerPocActivity.kt`.

## Observed Results

- `ERROR_RECOGNIZER_BUSY` was not reproduced during manual testing.
- `Destroy/recreate` restart strategy is more stable than the `Delay 150ms` strategy on the tested device.
- `ERROR_NO_MATCH` still occurs.
- `ERROR_NETWORK_TIMEOUT` still occurs.
- No audible system beep was heard during testing.
- With `Destroy/recreate`, the observed restart gap is about 152 ms.
- Because `ERROR_NETWORK_TIMEOUT` appears, the active speech service likely depends on network recognition or has unstable network access.

## Phase 4 Recommendation

- Use destroy/recreate as the first candidate restart strategy for the final `SpeechFollower`.
- Keep the delayed restart strategy available as a fallback only if another device shows worse behavior with destroy/recreate.
- Treat `ERROR_NO_MATCH` as a normal recoverable event and restart listening.
- Treat `ERROR_SPEECH_TIMEOUT` as a normal recoverable event and restart listening.
- Treat `ERROR_NETWORK_TIMEOUT` as recoverable, but surface a readable status such as "语音网络不稳定" if it repeats.
- Keep `ERROR_RECOGNIZER_BUSY` counting and a failure threshold even though it was not reproduced, because it is device/service dependent.
- Do not retry forever. Repeated busy/network failures should enter a failed state until the user manually restarts voice following.

## Acceptance Status

- Busy risk: acceptable on the tested device because busy was not reproduced.
- Restart strategy: choose destroy/recreate for Phase 4 unless another tested device proves it worse.
- Restart gap: acceptable on the tested device, observed at about 152 ms with destroy/recreate.
- Network stability: unresolved risk. Final speech following must tolerate `ERROR_NETWORK_TIMEOUT` and degrade clearly.
- Beep suppression: no audible beep was present on the tested device, so there is no user-facing beep issue on this device. Keep restore-on-exit safeguards if muting remains in the final implementation.
