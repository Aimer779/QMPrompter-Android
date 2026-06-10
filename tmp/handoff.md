# Handoff - Android Template Initialization

## 项目目标
- 将现有 iOS SwiftUI 提词器应用迁移到 Android，使用 Kotlin + Jetpack Compose + Room + CameraX，先完成可编译的 Android 项目骨架。

## 当前技术栈
- Kotlin 2.2.10
- Android Gradle Plugin 9.2.0
- Gradle Wrapper 9.4.1
- JDK 21
- Jetpack Compose Material3
- Room + KSP
- CameraX
- Navigation Compose
- OkHttp

## 代码状态
- 当前分支：main
- 当前 HEAD：ec7f6e278cbe32860465fc63713fa1f1ae8638c1
- 工作区状态：有未提交修改
- 主要改动文件：
  - .gitignore
  - settings.gradle.kts
  - build.gradle.kts
  - gradle.properties
  - gradle/libs.versions.toml
  - gradle/wrapper/gradle-wrapper.properties
  - gradlew
  - gradlew.bat
  - app/build.gradle.kts
  - app/src/main/AndroidManifest.xml
  - app/src/main/kotlin/com/qiaomu/prompter/QMPrompterApp.kt
  - app/src/main/kotlin/com/qiaomu/prompter/MainActivity.kt
  - app/src/main/res/values/strings.xml
  - app/src/main/res/values/themes.xml
  - app/src/main/res/values-night/themes.xml
  - app/src/main/res/xml/backup_rules.xml
  - app/src/main/res/xml/data_extraction_rules.xml
  - app/src/test/kotlin/com/qiaomu/prompter/ExampleUnitTest.kt
  - local.properties（被 .gitignore 忽略，本机 SDK 路径）
  - INIT_FROM_TEMPLATE.md
  - MIGRATION_PLAN.md
  - CLAUDE.md

## 已完成的功能
- Android Gradle 项目骨架已位于仓库根目录，包含 app 模块、Gradle wrapper、version catalog 和基础配置。
- app 包名已切换为 com.qiaomu.prompter。
- Compose 空壳入口可构建，启动 Activity 显示 QMPrompter 文本。
- Manifest 已包含相机、录音、网络权限，SpeechRecognizer queries，竖屏锁定，以及 API Key SharedPreferences 备份排除规则。
- Placemark 模板业务代码、XML 页面、菜单、旧包名和旧依赖残留已清理。

## 本轮完成
- 将复制来的 Placemark 模板从子目录整理到仓库根目录。
- 改写 settings.gradle.kts、build.gradle.kts、app/build.gradle.kts 和 gradle/libs.versions.toml，切到 QMPrompter 的 Compose/Room/CameraX/Navigation/OkHttp 骨架。
- 删除 Placemark 示例源码、layout、menu、测试包，新增 QMPrompterApp、MainActivity 和最小单元测试。
- 写入 local.properties，配置 sdk.dir=D\:\\android-sdk。
- 补齐 INIT_FROM_TEMPLATE.md 要求的 android.useAndroidX=true。
- 使用 subagent 按 INIT_FROM_TEMPLATE.md 做只读验收；subagent 发现 android.useAndroidX=true 缺失，本轮已修复。

## 重要约束

### 相关源代码文件
- app/src/main/kotlin/com/qiaomu/prompter/QMPrompterApp.kt
- app/src/main/kotlin/com/qiaomu/prompter/MainActivity.kt
- app/src/main/AndroidManifest.xml
- app/build.gradle.kts
- settings.gradle.kts
- gradle.properties
- gradle/libs.versions.toml

### 相关文档
- INIT_FROM_TEMPLATE.md
- MIGRATION_PLAN.md
- CLAUDE.md
- tmp/handoff.md

### 工程约束
- 当前 AGP 9.2.0 + Kotlin Android 插件组合需要保留 gradle.properties 中的 android.builtInKotlin=false 和 android.newDsl=false，否则构建会在插件应用阶段失败。
- 上述两个 flag 已产生弃用警告，AGP 10 会移除；后续应评估降级到更稳的 AGP/Kotlin 组合，或迁移到 AGP 9 新 DSL。
- local.properties 是本机配置，已被 .gitignore 忽略，不应提交。
- Gradle wrapper 版本来自模板：Gradle 9.4.1；目前已能使用本机 D:\android-sdk 构建。
- compileSdk/targetSdk 当前为 36，minSdk 为 26。
- 依赖版本采用模板当前可解析版本，不追最新稳定版。

### 业务约束
- Android 版是全新数据，不迁移 iOS scripts.json。
- API Key 暂用 SharedPreferences，但必须排除 Auto Backup；当前排除文件名为 ai_provider_config.xml。
- 应用按 iOS 行为约束为竖屏。
- 后续语音识别需要 Android 11+ package visibility queries，否则 SpeechRecognizer 可用性检查可能静默失败。

## 残留问题

### 问题 1：AGP 9 兼容 flag 有弃用警告
- 影响：当前构建可通过，但 Gradle 输出警告；未来 AGP 10 移除这些选项后会阻塞升级。
- 已尝试：曾移除 android.builtInKotlin=false，构建失败，报 Cannot add extension with name 'kotlin'；恢复后又因缺 android.newDsl=false 触发 BaseExtension 类型转换失败；两个 flag 都恢复后构建通过。
- 可能原因：AGP 9.2.0 默认内置 Kotlin 与显式 Kotlin Android 插件、KSP/旧 DSL 兼容路径存在冲突。
- 下一步建议：短期保留两个 flag；等 Phase 1 骨架稳定后，单独开一轮评估更稳的 AGP/Kotlin/KSP 版本组合，避免和业务开发混在一起。

### 问题 2：尚未做设备或模拟器运行验证
- 影响：只能证明 Gradle 构建和单元测试通过，不能证明 APK 在设备上启动、权限声明和主题表现正常。
- 已尝试：已完成 assembleDebug 和 test；未安装到设备。
- 可能原因：本轮目标是模板整理，没有启动模拟器或连接真机。
- 下一步建议：下一轮完成数据层或入口导航后，用真机/模拟器安装 debug APK，确认首屏显示和 Manifest 无运行期问题。

### 问题 3：业务数据层尚未实现
- 影响：当前只是 Compose 空壳，尚不具备文稿列表、Room 数据库、Repository、SeedCallback 等迁移计划 Phase 1 功能。
- 已尝试：未开始实现，避免和模板整理混杂。
- 可能原因：本轮目标仅为 INIT_FROM_TEMPLATE.md 骨架整理验收。
- 下一步建议：按 MIGRATION_PLAN.md Phase 1 第 2 条开始，实现 Script Entity、TextColorPreset 和 TypeConverter。

## 下一轮目标
实现 MIGRATION_PLAN.md Phase 1 的数据层最小闭环：新增 Script Room Entity、TextColorPreset TypeConverter、ScriptDao、AppDatabase SeedCallback 和 ScriptRepository，并补一组可运行的单元或构建验证，确保空壳项目从“可编译”进入“有本地数据模型”的状态。

## 关键决策
- 保留模板 Gradle wrapper 9.4.1 和 AGP 9.2.0，原因是当前本机环境已经能构建，符合 INIT_FROM_TEMPLATE.md 的“优先复用本机已缓存且满足下限版本”原则。
- 将 Android 工程骨架放在仓库根目录，而不是保留 Placemark 子目录，原因是 MIGRATION_PLAN.md 的目标目录结构要求 app、gradle、settings.gradle.kts 等位于 QMPrompter-Android 根目录。
- 删除 Placemark 示例业务代码，只保留最小 Compose 入口，原因是该模板是传统 XML/AppCompat CRUD 示例，与目标 Compose 架构不一致。
- local.properties 指向 D:\android-sdk，且不提交，原因是 SDK 路径是机器本地状态。
- 暂时保留 android.builtInKotlin=false 和 android.newDsl=false，原因是移除后构建失败；这是当前工具链组合的兼容条件。

## 本轮的验证结果
- [x] 构建：`rtk ./gradlew.bat assembleDebug --stacktrace` 通过，最后一次执行退出码 0。
- [x] 测试：`rtk ./gradlew.bat test --stacktrace` 通过，输出 `BUILD SUCCESSFUL in 2s`，`26 actionable tasks: 4 executed, 22 up-to-date`。
- [x] 静态检查：`rtk rg -n "Placemark|placemark|org\\.wit|appcompat|viewBinding|parcelize|picasso|timber|constraintlayout|ActivityPlacemark|CardPlacemark" .` 无匹配，命令退出码 1，表示未找到模板残留。
- [x] 配置检查：`rtk rg -n "android.useAndroidX|android.builtInKotlin|android.newDsl|sdk.dir|rootProject.name|namespace|applicationId|minSdk|RecognitionService" settings.gradle.kts app/build.gradle.kts gradle.properties local.properties app/src/main/AndroidManifest.xml` 确认 rootProject、包名、minSdk、SDK 路径、AndroidX、兼容 flag 和 SpeechRecognizer queries 均存在。
- [x] subagent 验收：按 INIT_FROM_TEMPLATE.md 只读检查，结论为整体完成；发现缺少 android.useAndroidX=true，已修复并重新构建/测试通过。
- [ ] 未验证：真机或模拟器安装运行，原因：本轮未启动设备环境，目标集中在模板整理和 Gradle 骨架验证。
- [ ] 未验证：Android Studio Sync，原因：本轮只使用命令行 Gradle 验证，未打开 IDE。

## 必须测试的场景
- 运行 `rtk ./gradlew.bat assembleDebug --stacktrace`，确认构建仍通过。
- 运行 `rtk ./gradlew.bat test --stacktrace`，确认单元测试任务仍通过。
- 在真机或模拟器安装 debug APK，确认应用可启动并显示 QMPrompter 首屏。
- 后续加入 Room 后，验证首次启动能创建数据库并插入试用文稿。
- 后续加入 AiProviderConfigStore 后，验证 ai_provider_config.xml 不进入 Auto Backup 规则。
- 后续加入 SpeechRecognizer 后，验证 `android.speech.RecognitionService` queries 在目标设备上能让可用性检查正常工作。
