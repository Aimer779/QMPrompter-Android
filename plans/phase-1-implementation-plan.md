# Phase 1 Implementation Plan

## Scope

Implement the remaining Phase 1 work from `MIGRATION_PLAN.md`: data model, Room persistence, repository, AI provider config persistence, prompt formatting logic, tests, and the minimal Compose/NavHost application shell.

Already completed and only to be rechecked:
- Gradle Android project skeleton.
- Basic `MainActivity` and `QMPrompterApp` files.
- Manifest permissions, `SpeechRecognizer` query, portrait orientation, and backup exclusion XML.

Out of scope for this phase:
- iOS `scripts.json` import.
- Full script list/editor UI.
- Camera preview and teleprompter scrolling.
- SpeechRecognizer continuous listening PoC.
- AI request generation.
- DI frameworks, ViewModel layer, XML/AppCompat UI, or encrypted shared preferences.

## Technical Stack

- Kotlin 2.2.10.
- Android Gradle Plugin 9.2.0.
- Gradle Wrapper 9.4.1.
- JDK 21, Kotlin JVM target 21.
- Jetpack Compose + Material3 for the app shell.
- Navigation Compose for the minimal route graph.
- Room 2.8.4 + KSP for local script persistence.
- Kotlin Coroutines and Flow/StateFlow for repository state.
- SharedPreferences for AI provider config.
- JUnit 4 for pure logic tests.

Keep these existing compatibility settings:
- `android.builtInKotlin=false`.
- `android.newDsl=false`.
- `android.useAndroidX=true`.

## Logic Flow

1. App starts through `QMPrompterApp`.
2. `QMPrompterApp` lazily creates `AppDatabase`.
3. `AppDatabase` exposes `ScriptDao`.
4. On first database creation, `RoomDatabase.Callback` inserts the sample script from the iOS app.
5. `ScriptRepository` wraps `ScriptDao` and exposes scripts as `StateFlow<List<Script>>`, ordered by `updatedAt` descending.
6. `MainActivity` sets Compose content and hosts a minimal `NavHost`.
7. The first screen remains a shell for Phase 2, but it should be wired to the app-level repository boundary without implementing full list/edit behavior.
8. `AiProviderConfigStore` persists `apiKey`, `baseUrl`, and `model` in `ai_provider_config.xml`.
9. `backup_rules.xml` and `data_extraction_rules.xml` continue excluding `ai_provider_config.xml`.
10. `PromptFormatter` converts script content into prompt lines using behavior matched to the iOS implementation.

## Android Files To Add Or Modify

Add:
- `app/src/main/kotlin/com/qiaomu/prompter/data/Script.kt`
- `app/src/main/kotlin/com/qiaomu/prompter/data/TextColorPreset.kt`
- `app/src/main/kotlin/com/qiaomu/prompter/data/Converters.kt`
- `app/src/main/kotlin/com/qiaomu/prompter/data/ScriptDao.kt`
- `app/src/main/kotlin/com/qiaomu/prompter/data/AppDatabase.kt`
- `app/src/main/kotlin/com/qiaomu/prompter/data/ScriptRepository.kt`
- `app/src/main/kotlin/com/qiaomu/prompter/settings/AiProviderConfigStore.kt`
- `app/src/main/kotlin/com/qiaomu/prompter/util/PromptFormatter.kt`
- `app/src/test/kotlin/com/qiaomu/prompter/util/PromptFormatterTest.kt`

Likely modify:
- `app/src/main/kotlin/com/qiaomu/prompter/QMPrompterApp.kt`
- `app/src/main/kotlin/com/qiaomu/prompter/MainActivity.kt`
- `app/build.gradle.kts` if Room schema output is not configured.
- `app/src/main/AndroidManifest.xml` only if the existing app/backup declarations need alignment.
- `app/src/main/res/xml/backup_rules.xml` only if the shared preferences filename changes.
- `app/src/main/res/xml/data_extraction_rules.xml` only if the shared preferences filename changes.

Do not modify unless a compile or verification issue requires it:
- Gradle wrapper files.
- Toolchain versions.
- iOS source files.
- `local.properties`.

## iOS Reference Code

Use these files as behavioral references:

- `QMPrompter/Models/Script.swift`
  - `Script` fields and defaults.
  - `TextColorPreset` raw values and display choices.
  - `preview` trimming behavior.

- `QMPrompter/Services/ScriptStore.swift`
  - Create/save/delete semantics.
  - Sort by `updatedAt` descending.
  - Default unnamed title.
  - First-launch sample script content.

- `QMPrompter/Utilities/PromptFormatter.swift`
  - Prompt line splitting algorithm.
  - Newline normalization.
  - Strong and soft punctuation boundaries.
  - Speakable-character merge behavior.

- `QMPrompter/Services/APIKeyStore.swift`
  - Trim API key before persistence.
  - Treat blank key as absent.
  - `hasDeepSeekAPIKey` behavior, adapted to the Android config model.

- `QMPrompter/QMPrompterApp.swift`
  - App entry relationship.

- `QMPrompter/Views/ScriptListView.swift`
  - Navigation topology only. Do not implement full list UI in Phase 1.

Do not use these files for Phase 1 implementation beyond broad context:
- `QMPrompter/Views/PrompterView.swift`
- `QMPrompter/Services/ScrollEngine.swift`
- `QMPrompter/Services/CameraPreview.swift`
- `QMPrompter/Services/SpeechFollower.swift`
- `QMPrompter/Services/PromptDictation.swift`
- `QMPrompter/Services/DeepSeekScriptGenerator.swift`
- `QMPrompter/Views/AIGenerationView.swift`
- `QMPrompter/Views/ScriptEditorView.swift`

## Data Model Decisions

`Script` Room entity:
- `id: String`, generated from UUID.
- `title: String`.
- `content: String`.
- `createdAt: Long`, epoch millis.
- `updatedAt: Long`, epoch millis.
- `fontSize: Double`, default `42.0`.
- `scrollSpeed: Double`, default `80.0`.
- `textColorPreset: TextColorPreset`, default `white`.
- `overlayOpacity: Double`, default `0.48`.

`TextColorPreset`:
- Raw values: `white`, `silver`, `graphite`.
- Android does not support iOS JSON legacy aliases `yellow` and `green`, because there is no iOS data import in this migration.
- TypeConverter stores the raw value string.
- Unknown database value should fall back to `white` to keep the app openable after bad local data.

Room:
- `version = 1`.
- `exportSchema = true`.
- No destructive migrations.
- Add schema output if not already configured.

DAO minimum:
- Observe all scripts ordered by `updatedAt DESC`.
- Get script by id.
- Insert or update script.
- Delete script by id or entity.

Repository:
- Keep as a thin wrapper over `ScriptDao`.
- Expose `StateFlow` for the script list.
- Avoid extra provider or abstraction layers in Phase 1.

## Ai Provider Config Decisions

Persist with SharedPreferences file:
- `ai_provider_config`

XML backup exclusion file path:
- `ai_provider_config.xml`

Fields:
- `apiKey`, default empty string.
- `baseUrl`, default empty string.
- `model`, default empty string.

Behavior:
- Trim `apiKey` when saving.
- Blank API key means no usable key.
- Do not introduce `androidx.security:security-crypto`.
- Keep backup exclusion aligned with the preferences file name.

## PromptFormatter Decisions

The Kotlin implementation should match `QMPrompter/Utilities/PromptFormatter.swift`:
- Normalize `\r\n` and `\r` to `\n`.
- Preserve paragraph breaks as empty prompt lines when multiple lines exist.
- Strong punctuation: `。！？；.!?;：:`.
- Soft punctuation: `，、,`.
- Hard limit: max of semantic minimum, `target + 4`, and `ceil(target * 1.35)`.
- Non-speakable fragments merge into the previous line.
- Character counting must be Unicode-codepoint aware enough for Chinese text and punctuation. Avoid byte-length logic.

Tests should cover:
- Chinese punctuation splitting.
- Mixed Chinese and ASCII punctuation.
- Blank lines.
- CRLF normalization.
- Punctuation-only fragment merging.
- Empty or whitespace-only input.

## Implementation Order

1. Recheck current working tree and avoid overwriting unrelated changes.
2. Add data model, enum, converters, DAO, database, and seed callback.
3. Configure Room schema output if needed.
4. Add repository wrapper.
5. Add `AiProviderConfigStore` and verify backup exclusion filename alignment.
6. Port `PromptFormatter`.
7. Add focused unit tests for `PromptFormatter`.
8. Wire `QMPrompterApp` and `MainActivity` into a minimal app shell and `NavHost`.
9. Run tests and debug build.
10. Update handoff only if requested or if the implementation outcome needs to be recorded.

## Acceptance Criteria

Build and tests:
- `rtk ./gradlew.bat test --stacktrace` passes.
- `rtk ./gradlew.bat assembleDebug --stacktrace` passes.

Data layer:
- Room compiles through KSP.
- Room schema for version 1 is generated and committed if produced.
- First database creation seeds exactly one sample script when no scripts exist.
- Seed script title and content match the iOS sample script.
- Scripts are observed in `updatedAt DESC` order.

Prompt formatter:
- Unit tests pass.
- Test cases prove behavior parity with the iOS formatter for representative inputs.

Config persistence:
- `AiProviderConfigStore` persists `apiKey`, `baseUrl`, and `model`.
- Blank API key is treated as missing.
- SharedPreferences filename remains aligned with `backup_rules.xml` and `data_extraction_rules.xml`.

App shell:
- App still launches into Compose.
- A minimal `NavHost` exists.
- No Phase 2 UI behavior is implemented prematurely.

Constraints:
- No new DI framework.
- No ViewModel layer.
- No iOS JSON import.
- No encrypted shared preferences dependency.
- No broad refactor or unrelated cleanup.
