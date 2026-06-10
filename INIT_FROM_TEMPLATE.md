# 从既有 Kotlin 项目复制骨架初始化指南

配合 `MIGRATION_PLAN.md` Phase 1 第 1 条使用。适用场景：网络受限环境下，不通过 Android Studio 新建向导/在线下载 Gradle，而是复制一份手头已有的 Kotlin 项目作为骨架，复用本机已缓存的 Gradle 发行版与依赖。

## 原理

- Gradle Wrapper 按 `gradle/wrapper/gradle-wrapper.properties` 中的 `distributionUrl` 在 `~/.gradle/wrapper/dists/` 查找本地缓存，命中即零下载——所以**保持 wrapper 配置与源项目一致**是省时间的关键。
- Gradle 只构建 `settings.gradle.kts` 中 `include` 的模块，仓库根目录的 iOS 工程（`QMPrompter/`、`QMPrompter.xcodeproj`）对构建零影响，可以同仓共存、随时参考 Swift 实现。
- 版本策略：**"锁定为本机已缓存且满足下限的版本"优先于"追最新稳定版"**。MIGRATION_PLAN 中"开工当天锁定"的本意是版本不漂移，不是必须最新。

## 第 0 步：核对源项目版本下限

复制之前先确认源项目的工具链满足迁移计划的最低要求，任一不满足则需升级（升级才需要联网，提前在网络可用时做）：

| 项目 | 下限要求 | 在哪确认 |
|---|---|---|
| Kotlin | 2.0+ | 根 build.gradle.kts / libs.versions.toml |
| Compose Compiler 插件 | `org.jetbrains.kotlin.plugin.compose`，版本与 Kotlin 一致 | 同上（Kotlin 2.x 必需，没有则 Compose 编不过） |
| KSP | 与所用 Kotlin 版本配套（按官方 release 配套表） | 同上 |
| AGP | 支持目标 compileSdk（如 compileSdk 35 需 AGP 8.6+） | 同上 |
| Gradle | 满足该 AGP 的最低 Gradle 要求（如 AGP 8.7 需 Gradle 8.9+） | gradle-wrapper.properties |
| JDK | AGP 8.x 需 JDK 17+ | gradle.properties / IDE 设置 |

源项目最好本身就是 Compose 项目；如果不是，需要额外补 Compose BOM 依赖和 `buildFeatures.compose = true`，工作量略增但仍可行。

## 第 1 步：复制骨架文件

从源项目复制以下内容到本仓库根目录（`D:\code\self-project\QMPrompter-Android\`）：

```
gradle/                      ← 整个目录（含 wrapper jar + properties，版本号不要动）
gradlew                      ← Unix 启动脚本
gradlew.bat                  ← Windows 启动脚本
settings.gradle.kts
build.gradle.kts             ← 根构建脚本（通常只有 plugins 声明）
gradle.properties
gradle/libs.versions.toml    ← 如源项目使用 version catalog
app/
  build.gradle.kts
  proguard-rules.pro
  src/main/AndroidManifest.xml
```

**不要复制**：源项目的 `.gradle/`、`build/`、`.idea/`、`local.properties`（含机器相关路径，让 IDE 重新生成）、签名文件（`*.jks`/`*.keystore`）。

## 第 2 步：清理改名清单

逐项过一遍，全部完成再做首次 sync：

- [ ] `settings.gradle.kts`：`rootProject.name = "QMPrompter"`；`include` 只留 `":app"`
- [ ] `app/build.gradle.kts`：
  - [ ] `namespace = "com.qiaomu.prompter"`
  - [ ] `applicationId = "com.qiaomu.prompter"`
  - [ ] `minSdk = 26`，compileSdk/targetSdk 维持源项目值（满足下限即可）
  - [ ] 删除源项目特有依赖，按 MIGRATION_PLAN「依赖清单」对齐（版本号用源项目已缓存的近似版本即可，不必照抄计划里的基线数字）
  - [ ] 删除 `signingConfigs` 等源项目签名配置
- [ ] 源码目录：删掉源项目代码，重建 `app/src/main/kotlin/com/qiaomu/prompter/`（源项目若用 `java/` 目录，改用 `kotlin/` 需在 sourceSets 确认或直接沿用 `java/` 目录名——目录名不影响 Kotlin 编译，统一即可）
- [ ] `app/src/main/res/`：清掉源项目资源，只留 `values/`（themes/strings/colors 改名为本项目）和启动图标占位
- [ ] `AndroidManifest.xml`：按 MIGRATION_PLAN Phase 1 第 7 条重写（权限、`<queries>`、备份排除、竖屏锁定），不要沿用源项目的 application/activity 配置
- [ ] `gradle.properties`：源项目的代理、私有仓库凭据、无关 flag 按需清理；保留 `org.gradle.jvmargs`、`android.useAndroidX=true`
- [ ] 根 `.gitignore` 补 Android 条目：

  ```gitignore
  .gradle/
  build/
  app/build/
  local.properties
  .idea/
  *.jks
  *.keystore
  ```

## 第 3 步：仓库镜像配置（依赖下载提速）

Wrapper 缓存只解决 Gradle 本体，AndroidX 依赖仍走远端仓库。源项目如已配国内镜像则保留；没有则在 `settings.gradle.kts` 配置（阿里云示例）：

```kotlin
dependencyResolutionManagement {
    repositories {
        maven("https://maven.aliyun.com/repository/google")
        maven("https://maven.aliyun.com/repository/central")
        maven("https://maven.aliyun.com/repository/gradle-plugin")
        google()        // 镜像未命中时兜底
        mavenCentral()
    }
}
```

`pluginManagement.repositories` 同理配置一份。

## 第 4 步：验证

```bash
./gradlew --version          # 确认命中本地缓存的 Gradle，无下载动作
./gradlew assembleDebug      # 空壳工程编译通过
```

两条都通过后，在 `MainActivity` 放一个 `Text("QMPrompter")` 装到真机/模拟器确认能跑，即可按 MIGRATION_PLAN Phase 1 第 2 条继续。

## 与 iOS 工程共存的说明

- Android Studio 直接打开仓库根目录；`QMPrompter/`（Swift 源码）在工程树中显示为普通文件夹，参考逻辑时直接打开 `.swift` 文件阅读，不需要任何额外配置。
- Xcode 侧不受影响，`QMPrompter.xcodeproj` 照常打开构建。
- 两套构建产物互不可见：Xcode 产物在 `build/`（已被 iOS 侧忽略），Gradle 产物在 `.gradle/`、`app/build/`（上面 .gitignore 已覆盖）。
