# Project Overview
QMPrompter Android is a Kotlin migration of the existing iOS SwiftUI teleprompter app. The goal is feature parity with the current iOS app using Jetpack Compose for UI, Room for local scripts, CameraX for camera preview, Android SpeechRecognizer for voice following/dictation, and OkHttp for OpenAI-compatible AI script generation.

The existing iOS project remains in the same repository for reference. Android project files live at the repository root with the app module under `app/`.

## Tech Stack
- Kotlin 2.2.10
- Android Gradle Plugin 9.2.0
- Gradle Wrapper 9.4.1
- JDK 21
- Jetpack Compose + Material3
- Room + KSP
- CameraX
- Navigation Compose
- Android SpeechRecognizer
- OkHttp + Coroutines
- JUnit 4 for focused pure-logic tests

Do NOT introduce unless explicitly requested:
- DI frameworks such as Hilt, Dagger, or Koin
- ViewModel layer or lifecycle-viewmodel-compose
- XML/AppCompat/RecyclerView UI for new Android screens
- androidx.security:security-crypto / EncryptedSharedPreferences
- iOS data import from `scripts.json`
- UI automation, device matrix testing, Macrobenchmark, or broad test infrastructure
- Extra abstraction, configuration, or provider systems beyond the migration plan

### Common Command
- Build debug APK: `.\gradlew.bat assembleDebug`
- Test: `.\gradlew.bat test`
- Show Gradle version: `.\gradlew.bat --version`
- Search code/text: `rtk rg "<pattern>"`
- Check working tree: `rtk git status --short --branch`

## Project Context
- Android 迁移计划：`MIGRATION_PLAN.md`
- Kotlin 模板初始化指南：`INIT_FROM_TEMPLATE.md`
- 当前交接记录：`tmp/handoff.md`
- iOS 参考实现：`QMPrompter/`
- Xcode 工程：`QMPrompter.xcodeproj/`

## Working Rules

### 1. 先思考，后编码

**不要假设。不要掩饰困惑。暴露权衡。**

实现之前：
- 明确陈述你的假设。如果不确定，就提问。
- 如果存在多种解释，把它们列出来——不要默默选一个。
- 如果存在更简单的方法，就说出来。在必要时提出反对意见。
- 如果有不清楚的地方，停下来。说出困惑之处。提问。

### 2. 简单至上

**解决问题所需的最少代码。不写推测性内容。**

- 不添加超出需求范围的功能
- 不为只使用一次的代码做抽象
- 不添加未被要求的“灵活性”或“可配置性”
- 不为不可能发生的场景做错误处理
- 如果你写了 200 行代码而本来可以只写 50 行，那就重写

问自己：“资深工程师会觉得这过于复杂吗？” 如果是，就简化。

### 3. 手术式修改

**只动必须动的地方。只清理自己造成的混乱。**

编辑现有代码时：
- 不要“改进”相邻的代码、注释或格式
- 不要重构没有坏掉的东西
- 匹配现有风格，即使你会用不同的方式做
- 如果你注意到无关的废弃代码，可以提一下——但不要删除它

当你的修改造成孤儿代码时：
- 移除因你的修改而变得未使用的导入/变量/函数
- 除非被要求，否则不要删除原本就存在的废弃代码

检验标准：每个被修改的行都应该能直接追溯到用户的需求。

---

### Lessons
- `local.properties` is machine-specific and currently points to `D:\android-sdk`; keep it ignored.
- Current AGP 9.2.0 setup needs `android.builtInKotlin=false` and `android.newDsl=false` in `gradle.properties`; removing either caused Gradle plugin failures.
- Keep `android.useAndroidX=true` in `gradle.properties`; it is required by `INIT_FROM_TEMPLATE.md`.
- Manifest must keep camera, microphone, internet permissions, SpeechRecognizer queries, portrait orientation, and backup exclusions for `ai_provider_config.xml`.
- A successful skeleton verification is `.\gradlew.bat assembleDebug` plus `.\gradlew.bat test`.
