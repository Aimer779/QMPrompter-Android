# Handoff - Phase 1 Data Layer

## 项目目标

将现有 iOS SwiftUI 提词器应用迁移到 Android，使用 Kotlin + Jetpack Compose + Room + CameraX + Android SpeechRecognizer + OkHttp，逐步实现与 iOS 版功能对等。

当前 Android 工程位于仓库根目录，app module 位于 `app/`。

## 当前技术栈

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

## 当前代码状态

- 当前分支：`main`
- 当前 HEAD：`ec7f6e278cbe32860465fc63713fa1f1ae8638c1`
- 分支状态：`main...origin/main [ahead 1]`
- 工作区状态：有未提交修改

当前 `git status --short --branch` 显示的变更范围：

- 修改：
  - `.gitignore`
  - `AGENTS.md`
  - `app/build.gradle.kts`
  - `app/src/main/kotlin/com/qiaomu/prompter/MainActivity.kt`
  - `app/src/main/kotlin/com/qiaomu/prompter/QMPrompterApp.kt`
  - `tmp/handoff.md`
- 新增：
  - `app/schemas/`
  - `app/src/main/kotlin/com/qiaomu/prompter/data/`
  - `app/src/main/kotlin/com/qiaomu/prompter/settings/`
  - `app/src/main/kotlin/com/qiaomu/prompter/util/`
  - `app/src/test/kotlin/com/qiaomu/prompter/util/`
  - `plans/`

`.kotlin/` 已加入 `.gitignore`，本地 Kotlin 构建日志/缓存不应提交。

## 本轮完成

### Phase 1 实施计划

- 新增 `plans/phase-1-implementation-plan.md`。
- 评估过 `plans/phase-1-evaluation.md` 中提到的方向，但当前工作树里该文件不存在。
- 关键取舍以 `MIGRATION_PLAN.md` 和 `plans/phase-1-implementation-plan.md` 为准：
  - Room 日期使用 epoch millis `Long`，不使用 ISO 8601 String。
  - `TextColorPreset` 不兼容 iOS 历史 JSON alias `yellow/green`。
  - `AiProviderConfigStore` 使用 SharedPreferences，不引入 DataStore。
  - 不引入 ViewModel 层、DI 框架或 EncryptedSharedPreferences。

### 数据层

新增：

- `app/src/main/kotlin/com/qiaomu/prompter/data/Script.kt`
- `app/src/main/kotlin/com/qiaomu/prompter/data/TextColorPreset.kt`
- `app/src/main/kotlin/com/qiaomu/prompter/data/Converters.kt`
- `app/src/main/kotlin/com/qiaomu/prompter/data/ScriptDao.kt`
- `app/src/main/kotlin/com/qiaomu/prompter/data/AppDatabase.kt`
- `app/src/main/kotlin/com/qiaomu/prompter/data/ScriptRepository.kt`

实现内容：

- `Script` Room entity：
  - `id: String`
  - `createdAt/updatedAt: Long`
  - `fontSize` 默认 `42.0`
  - `scrollSpeed` 默认 `80.0`
  - `textColorPreset` 默认 `white`
  - `overlayOpacity` 默认 `0.48`
- `TextColorPreset` raw values：
  - `white`
  - `silver`
  - `graphite`
- `Converters` 只存 raw value，未知值 fallback 到 `white`。
- `ScriptDao` 支持 observe all、get by id、save、delete、deleteById。
- `observeScripts()` 通过 SQL `ORDER BY updated_at DESC` 排序。
- `AppDatabase`：
  - `version = 1`
  - `exportSchema = true`
  - `RoomDatabase.Callback.onCreate()` 中用 `SupportSQLiteDatabase.execSQL` 同步插入 iOS 试用文稿。
- `ScriptRepository` 是 DAO 的薄封装，并暴露 `StateFlow<List<Script>>`。

### Room schema

- `app/build.gradle.kts` 已配置：
  - `ksp { arg("room.schemaLocation", "$projectDir/schemas") }`
- 已生成 Room schema：
  - `app/schemas/com.qiaomu.prompter.data.AppDatabase/1.json`
- `app/schemas/` 是迁移基线，应提交。

### AI 配置存储

新增：

- `app/src/main/kotlin/com/qiaomu/prompter/settings/AiProviderConfigStore.kt`

实现内容：

- SharedPreferences name：`ai_provider_config`
- 备份排除文件路径：`ai_provider_config.xml`
- 字段：
  - `apiKey`
  - `baseUrl`
  - `model`
- `apiKey` 保存时 trim。
- `hasApiKey` 使用 `isNotBlank()`。
- `backup_rules.xml` 和 `data_extraction_rules.xml` 已保持排除 `ai_provider_config.xml`。

### PromptFormatter

新增：

- `app/src/main/kotlin/com/qiaomu/prompter/util/PromptFormatter.kt`
- `app/src/test/kotlin/com/qiaomu/prompter/util/PromptFormatterTest.kt`

实现内容：

- 按 `QMPrompter/Utilities/PromptFormatter.swift` 迁移：
  - CRLF/CR 归一化。
  - 中文/ASCII 强标点断句。
  - 软标点断句。
  - hard limit 断句。
  - 空行保留规则。
  - 纯标点片段合并到上一行。
- 单测覆盖 6 个代表场景。
- 当前 Kotlin 实现使用 code point 计数；满足中文/ASCII 标点目标。emoji grapheme cluster 完全等价不是 Phase 1 目标。

### App shell

修改：

- `app/src/main/kotlin/com/qiaomu/prompter/QMPrompterApp.kt`
- `app/src/main/kotlin/com/qiaomu/prompter/MainActivity.kt`

实现内容：

- `QMPrompterApp` 持有：
  - `AppDatabase`
  - `ScriptRepository`
  - `AiProviderConfigStore`
- `MainActivity` 接入最小 Compose `NavHost`。
- 首屏仍是 Phase 1 壳，不实现 Phase 2 的列表/编辑器 UI。

### 文档与规则

修改：

- `AGENTS.md`
- `.gitignore`

更新内容：

- `AGENTS.md` 增加 Phase 1 数据层相关 Lessons。
- `AGENTS.md` 记录 Gradle 验证应顺序执行，不要并行跑 `test` 和 `assembleDebug`。
- `.gitignore` 增加 `.kotlin/`。

## 重要约束

### 工具链约束

- 保留 `gradle.properties` 中：
  - `android.builtInKotlin=false`
  - `android.newDsl=false`
  - `android.useAndroidX=true`
- 当前 AGP 9.2.0 + Kotlin Android 插件组合依赖上述兼容 flag。
- 构建会输出 AGP deprecated warnings；短期接受，不要和业务 Phase 混在一起处理。
- `local.properties` 是本机配置，当前指向 `D:\android-sdk`，已被 `.gitignore` 忽略，不应提交。

### 业务约束

- Android 版是全新数据，不导入 iOS `scripts.json`。
- API Key 用普通 SharedPreferences，但必须通过 backup rules 排除。
- 应用保持竖屏。
- Manifest 必须保留：
  - `CAMERA`
  - `RECORD_AUDIO`
  - `INTERNET`
  - `android.speech.RecognitionService` queries
  - portrait orientation
  - `ai_provider_config.xml` backup exclusions
- 不引入：
  - DI 框架
  - ViewModel layer
  - XML/AppCompat/RecyclerView 新 UI
  - EncryptedSharedPreferences
  - DataStore
  - iOS JSON data import

### 命名约束

- Kotlin `const val` 已统一为 `UPPER_SNAKE_CASE`。
- 最近修复了 `StrongPunctuation` 这类 PascalCase 常量名导致的 lint/style 问题。

## 验证结果

最后一次按顺序执行：

- `rtk ./gradlew.bat test --stacktrace`
  - 通过
  - `BUILD SUCCESSFUL`
- `rtk ./gradlew.bat assembleDebug --stacktrace`
  - 通过
  - `BUILD SUCCESSFUL`

注意：

- 不要并行运行上述两个 Gradle 命令。并行执行时曾触发 Kotlin incremental compilation 对 `app/build/tmp/kotlin-classes` 的临时文件竞争；顺序执行无问题。
- 仍会看到 AGP 9.2.0 相关 deprecated warnings，这是已知工具链问题。

## Subagent Review 结果

本轮使用 subagent 做过只读对抗性 review。

结论：

- 无 blocking issue。
- Phase 1 实现整体符合 `plans/phase-1-implementation-plan.md`。

Review 指出的有效风险及处理：

- 原先 `AppDatabase` seed 使用 async coroutine + singleton 回调，subagent 认为验收上不够硬。
- 已改为 `RoomDatabase.Callback.onCreate()` 中直接 `SupportSQLiteDatabase.execSQL` 同步插入样本文稿。
- 修改后重新顺序执行 `test` 和 `assembleDebug`，均通过。

保留的残余风险：

- `PromptFormatter` 使用 code point 计数，不完全等同 iOS grapheme cluster 计数；对中文/ASCII 标点场景可接受。
- 未做真机/模拟器安装后的数据库 seed 手动验证。本轮只完成命令行构建和单测验证。

## 残留问题

### 问题 1：AGP 9 兼容 flag 有弃用警告

- 影响：当前构建可通过，但 Gradle 输出警告；AGP 10 移除这些选项后会阻塞升级。
- 已尝试：之前移除 `android.builtInKotlin=false` 和 `android.newDsl=false` 会导致 Gradle 插件失败。
- 下一步建议：短期保留；等 Phase 1/Phase 1.5 稳定后，单独评估 AGP/Kotlin/KSP 组合或迁移新 DSL。

### 问题 2：Phase 1 缺少运行时 seed 手动确认

- 影响：命令行构建可证明 Room/KSP 编译和 schema 生成正常，但未直接证明首次安装后数据库内确实出现试用文稿。
- 已尝试：用同步 SQL seed 降低运行时风险。
- 下一步建议：在模拟器/真机安装 debug APK，清应用数据后启动，确认 Room 首次创建并插入“试用文稿”。

### 问题 3：`plans/phase-1-evaluation.md` 不在当前工作树

- 影响：用户曾要求查看该文件，但当前 `plans/` 中只有 `phase-1-implementation-plan.md`。
- 已尝试：根据已读取过的内容判断其中 ISO 日期、DataStore、ViewModel 等建议不应采纳。
- 下一步建议：如果需要保留评估记录，重新创建或恢复该文件；否则以 implementation plan 为准。

## 下一轮建议

优先顺序：

1. 手动运行 APK，清应用数据后确认首次启动 seed “试用文稿”。
2. 如确认 Phase 1 完成，进入 Phase 1.5：SpeechRecognizer 连续监听 PoC。
3. Phase 1.5 需要真机验证：
   - beep 是否可消除。
   - `ERROR_RECOGNIZER_BUSY` 是否可规避。
   - 连续监听重启间隙是否可接受。
4. Phase 2 之前不要引入 ViewModel、DI、DataStore 或额外 UI 基础设施。

## 常用命令

- 查看工作树：`rtk git status --short --branch`
- 搜索代码：`rtk rg "<pattern>"`
- 跑测试：`rtk ./gradlew.bat test --stacktrace`
- 构建 debug APK：`rtk ./gradlew.bat assembleDebug --stacktrace`

Gradle 验证请顺序执行。
