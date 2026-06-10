# Phase 3 Implementation Plan

## Scope

Implement Phase 3 from `MIGRATION_PLAN.md`:

1. Camera preview with CameraX.
2. Scroll engine with frame-based delta-time math.
3. Prompter screen skeleton with camera background, dark overlay, scrolling text, and keep-screen-on behavior.
4. Gesture controls: left-side speed drag, center manual scroll, right-side progress drag, and tap to play or pause.
5. Lightweight camera flip: default to the front camera, with an in-prompter control to switch between front and back cameras.

Do not implement final speech following, current-line speech highlighting, dictation, or AI generation in this phase.

## Implementation Approach

### Camera

- Add `app/src/main/kotlin/com/qiaomu/prompter/ui/camera/CameraPreview.kt`.
- Use CameraX `PreviewView` inside Compose `AndroidView`.
- Request and observe Android camera permission from Compose.
- Bind `ProcessCameraProvider` to the current lifecycle owner.
- Default to `CameraSelector.LENS_FACING_FRONT`.
- Accept a `lensFacing` parameter so `PrompterScreen` can switch front/back cameras.
- If the requested camera is unavailable, keep the UI stable and report an unavailable state rather than crashing.

### Scroll Engine

- Add `app/src/main/kotlin/com/qiaomu/prompter/ui/prompter/ScrollEngine.kt`.
- Port the iOS `ScrollEngine` math:
  - speed clamp: `20..260`
  - minimum line height: `40`
  - minimum average characters per line: `6`
  - `visualTuningFactor = 1.85`
  - `linesPerSecond = speed / averageCharactersPerLine / 60 * visualTuningFactor`
  - follow smoothing: `next = offset + (target - offset) * min(1, delta * 12)`
  - snap to target when distance is below `0.5px`
  - pause automatically at `maximumOffset`
- Use `withFrameNanos` from a Compose coroutine loop.
- Keep it as a lightweight state holder; do not introduce a ViewModel layer.

### Prompter Screen

- Add `app/src/main/kotlin/com/qiaomu/prompter/ui/prompter/PrompterScreen.kt`.
- Load the selected script from the existing `ScriptRepository`.
- Render:
  - full-screen camera preview
  - black overlay using `script.overlayOpacity`
  - formatted prompt text
  - small controls for back, play/pause, settings/progress as needed, and camera flip
- Format prompt lines with existing `PromptFormatter`.
- Measure text with Compose `TextMeasurer`.
- Render only the visible line window around the viewport.
- Apply scroll offset with draw-phase reads through `graphicsLayer` or lambda-based offset to avoid full recomposition on each frame.
- Set `FLAG_KEEP_SCREEN_ON` while the screen is active, and restore it on dispose.

### Gestures

- Add three transparent vertical hit zones over the prompter:
  - left third: drag vertically to change speed with `delta = -translationY * 0.42`
  - center third: drag vertically for manual scroll with `offset = startOffset - translationY * 1.05`
  - right third: drag vertically for progress with `offset = startOffset - translationY * 1.35`
- Tap toggles play/pause.
- Persist `script.scrollSpeed` when it changes.
- Do not persist scroll offset.

### Navigation

- Update `MainActivity.kt` with a `prompter/{scriptId}` route.
- Update `ScriptEditorScreen.kt` with a "start prompter" entry that opens the new route.
- Keep list-card click behavior as editor navigation to avoid expanding Phase 3 beyond the current app flow.

## Android Files

Expected new files:

- `app/src/main/kotlin/com/qiaomu/prompter/ui/camera/CameraPreview.kt`
- `app/src/main/kotlin/com/qiaomu/prompter/ui/prompter/ScrollEngine.kt`
- `app/src/main/kotlin/com/qiaomu/prompter/ui/prompter/PrompterScreen.kt`

Expected modified files:

- `app/src/main/kotlin/com/qiaomu/prompter/MainActivity.kt`
- `app/src/main/kotlin/com/qiaomu/prompter/ui/editor/ScriptEditorScreen.kt`

Possible small modification:

- `app/src/main/kotlin/com/qiaomu/prompter/data/TextColorPreset.kt`, only if a Compose color mapping helper is needed.

## iOS Reference Files

- `QMPrompter/Views/PrompterView.swift`
- `QMPrompter/Services/ScrollEngine.swift`
- `QMPrompter/Services/CameraPreview.swift`
- `QMPrompter/Views/ScriptEditorView.swift`
- `QMPrompter/Models/Script.swift`

The iOS app only uses the front camera. Android will keep front camera as the default and add a small camera flip control as a Phase 3 extension.

## Acceptance

Command verification:

1. `rtk ./gradlew.bat test --stacktrace`
2. `rtk ./gradlew.bat assembleDebug --stacktrace`

Manual verification:

1. Entering the prompter defaults to the front camera.
2. Camera flip switches to the back camera, and another tap switches back to front.
3. Permission denial shows a stable fallback state and does not crash.
4. Tap play/pause works; reaching the end pauses automatically.
5. Left drag changes speed and saves it.
6. Center drag manually scrolls the text.
7. Right drag changes progress.
8. The screen stays awake during prompting.
9. After lock/background and return, camera preview resumes and scroll offset remains.
10. 5k and 20k character scripts scroll without obvious jank.
