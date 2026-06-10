# Phase 5 Implementation Plan

## Scope

Phase 5 is final polish for the Android migration:

1. Immersive prompter mode.
2. Glass surface polish with API 31+ real blur and API 26-30 fallback.
3. Permission-denial handling with guidance to system settings.
4. Final command verification and real-device acceptance checklist.

Do not change the Phase 4 product decision: voice following stays off by default when entering `PrompterScreen`; users start it manually with the microphone button.

## Implementation Approach

### Immersive Mode

- Apply immersive mode only while `PrompterScreen` is active.
- Hide status and navigation bars on entry, then restore them on exit.
- Keep the existing `FLAG_KEEP_SCREEN_ON` behavior.
- Prefer `WindowInsetsControllerCompat`/AndroidX APIs so behavior is scoped and reversible.

### Glass Polish

- Keep the shared styling centered on `Modifier.glassSurface()`.
- API 31+:
  - Use a stronger translucent tint, border, and shadow while preserving text readability.
  - Do not blur the whole composable layer if it makes icons/text soft; true background-only blur can be revisited with a dedicated View/RenderNode implementation.
- API 26-30:
  - Keep the simulated translucent gradient and border fallback.
- Apply only to existing glass components and prompter controls where needed; avoid layout rewrites.

### Permission Handling

- Camera:
  - First launch requests camera permission as today.
  - Denied-once state shows a readable message and a retry action.
  - Permanently denied state shows an action that opens the app details settings screen.
  - Camera unavailable remains distinct from permission denial.
- Microphone:
  - Requested only when the user taps the microphone button.
  - Denied-once and permanently-denied states show readable prompter status.
  - Permanently denied opens app details settings.
  - Speech service unavailable remains handled by `SpeechFollower` state, not by permission UI.

## Android Files

- `app/src/main/kotlin/com/qiaomu/prompter/ui/prompter/PrompterScreen.kt`
  - Immersive mode lifecycle.
  - Microphone permission denial state and settings guidance.
  - Camera denial UI actions.
  - Prompter control glass polish if needed.
- `app/src/main/kotlin/com/qiaomu/prompter/ui/camera/CameraPreview.kt`
  - Camera permission state detail and retry support.
- `app/src/main/kotlin/com/qiaomu/prompter/ui/component/GlassActionPanel.kt`
  - Shared `Modifier.glassSurface()` improvements.
- Optional only if duplication is meaningful:
  - `app/src/main/kotlin/com/qiaomu/prompter/ui/permission/PermissionGuidance.kt`

## iOS Reference Files

- `QMPrompter/Views/PrompterView.swift`
  - `.statusBarHidden(true)` / `.persistentSystemOverlays(.hidden)`.
  - `cameraStatusView` copy and status handling.
  - `glassCapsule()`, `glassCircle()`, and `glassPanel()`.
- `QMPrompter/Views/GlassActionPanel.swift`
  - Bottom action panel surface, row surface, tint, border, and shadow values.
- `QMPrompter/Services/SpeechFollower.swift`
  - Speech state/status wording reference.

## Verification

Command verification:

1. `rtk ./gradlew.bat test --stacktrace`
2. `rtk ./gradlew.bat assembleDebug --stacktrace`

Real-device acceptance:

1. Entering the prompter hides status and navigation bars.
2. Leaving the prompter restores status and navigation bars on list/editor/settings screens.
3. The screen stays awake while the prompter is active.
4. First camera permission grant shows the preview.
5. Camera denied once shows a readable retry path.
6. Camera permanently denied shows a system settings action, and returning from settings refreshes the camera state.
7. Camera unavailable is not presented as a permission problem.
8. Microphone permission is requested only after tapping the microphone button.
9. Microphone denied once shows readable status and allows another user-triggered attempt.
10. Microphone permanently denied shows a system settings action.
11. Speech recognition unavailable remains a speech status, separate from microphone permission denial.
12. Glass panels and prompter controls remain readable on API 31+ and API 26-30.
13. 5k and 20k character scripts still scroll without obvious new jank.
14. Phase 4 behavior does not regress: voice following is default off, microphone starts it manually, current-line highlight works, and AI generation remains reachable.
