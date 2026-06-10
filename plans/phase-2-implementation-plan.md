# Phase 2 Implementation Plan

## Goal

Implement the Android script list, editor, shared glass action panel, and AI provider settings UI from `MIGRATION_PLAN.md` Phase 2.

This phase must produce a usable local script management flow:

- View seeded and user-created scripts.
- Search scripts.
- Create, edit, and delete scripts.
- Persist edits through Room.
- Configure AI provider fields already backed by `AiProviderConfigStore`.

## Scope

In scope:

1. `ScriptListScreen`
   - `LazyColumn` script list.
   - Search by title or content.
   - Create draft script.
   - Navigate to editor.
   - Swipe-to-delete.
   - Settings entry.
   - Keep temporary `Speech PoC` entry.

2. `ScriptCard`
   - Compact card showing title, preview, and updated date.
   - Glass-style surface matching the iOS visual direction without overbuilding Phase 5 polish.

3. `ScriptEditorScreen`
   - `TabRow` with `文稿` and `显示`.
   - Title editing dialog.
   - Body editor.
   - Paste body button.
   - Clear body confirmation via bottom glass panel.
   - Display sliders for font size, scroll speed, and camera transparency.
   - Text color preset choices from `TextColorPreset.editorChoices`.
   - Save through `ScriptRepository`.
   - Camera transparency maps to `overlayOpacity` as `1 - transparency`, clamped to `0.18..0.82`.

4. `GlassActionPanel`
   - Reusable bottom panel for create/clear actions.
   - Simple glass modifier shared by list/editor cards.

5. `AppSettingsScreen`
   - API Key, Base URL, and Model inputs.
   - Base URL and Model placeholders show DeepSeek defaults.
   - Explicit save button writes all fields to `AiProviderConfigStore`.

Out of scope:

- Camera preview.
- Prompter screen.
- Scroll engine.
- Final speech following.
- AI generation screen/network implementation.
- iOS `scripts.json` import.
- New DI/ViewModel frameworks.
- UI automation or broad test infrastructure.

## Android Files

Expected new files:

- `app/src/main/kotlin/com/qiaomu/prompter/ui/component/GlassActionPanel.kt`
- `app/src/main/kotlin/com/qiaomu/prompter/ui/scriptlist/ScriptCard.kt`
- `app/src/main/kotlin/com/qiaomu/prompter/ui/scriptlist/ScriptListScreen.kt`
- `app/src/main/kotlin/com/qiaomu/prompter/ui/editor/ScriptEditorScreen.kt`
- `app/src/main/kotlin/com/qiaomu/prompter/ui/settings/AppSettingsScreen.kt`

Expected modified files:

- `app/src/main/kotlin/com/qiaomu/prompter/MainActivity.kt`
- `app/src/main/kotlin/com/qiaomu/prompter/QMPrompterApp.kt` if `AiProviderConfigStore` is not already exposed.

Possible small support edits:

- `app/src/main/kotlin/com/qiaomu/prompter/data/ScriptRepository.kt` only if a thin helper is needed.
- `app/src/main/kotlin/com/qiaomu/prompter/data/TextColorPreset.kt` only if UI color mapping is needed.

## iOS References

Primary references:

- `QMPrompter/Views/ScriptListView.swift`
  - List structure, search behavior, create panel, settings entry, delete flow.
- `QMPrompter/Views/ScriptEditorView.swift`
  - Editor tabs, title editing, paste/clear actions, slider ranges, camera transparency mapping.
- `QMPrompter/Views/GlassActionPanel.swift`
  - Bottom action panel and action row behavior.
- `QMPrompter/Views/AppSettingsView.swift`
  - Settings save flow.
- `QMPrompter/Services/ScriptStore.swift`
  - Draft creation, save, delete semantics.
- `QMPrompter/Models/Script.swift`
  - Defaults and text color choices.

Secondary references:

- `QMPrompter/Views/AIGenerationView.swift`
  - Entry relationship only. AI generation remains Phase 4.

## Implementation Notes

- Keep screens directly wired to repositories/stores. Do not add ViewModel or DI framework.
- Use `rememberCoroutineScope()` for repository writes from UI actions.
- Keep edits surgical and avoid unrelated design cleanup.
- Use existing Room model fields as-is.
- Normalize empty titles to `Script.UNTITLED` before saving.
- Clamp display values in the editor before saving.
- Keep `TextColorPreset` raw values unchanged: `white`, `silver`, `graphite`.
- Do not add iOS legacy aliases.

## Acceptance

Command-line verification, run sequentially:

```powershell
rtk ./gradlew.bat test --stacktrace
rtk ./gradlew.bat assembleDebug --stacktrace
```

Manual verification:

1. First launch shows the seeded sample script.
2. New script can be created and opened in the editor.
3. Title and body edits persist after returning to the list.
4. Paste appends clipboard text when body is non-empty and replaces when empty.
5. Clear body keeps title and display settings.
6. Font size, scroll speed, text color, and camera transparency persist.
7. Search filters by title and body.
8. Swipe/delete removes a script.
9. Killing and reopening the app preserves created/edited/deleted data.
10. Settings page saves API Key, Base URL, and Model; reopening settings reads them back.
11. Temporary `Speech PoC` entry still opens the PoC activity.
