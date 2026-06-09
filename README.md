# 乔木提词器

自用 iOS 提词器第一版。当前版本只做摄像头预览背景和提词，不录视频。

## 已实现

- 文稿列表，新建、编辑、保存、删除。
- 本地 JSON 存储，首次启动内置一篇试用文稿。
- 前置摄像头实时预览。
- 全屏提词页，当前行高亮。
- Liquid Glass 风格提词层和可收起控制胶囊。
- 手动匀速滚动，点击播放/暂停。
- 左侧上下滑动调速度，右侧上下滑动调进度。
- 字号、速度、文字颜色、背景透明度设置。
- 抽象玻璃质感 App Icon。

## 工程配置

- Xcode project: `QMPrompter.xcodeproj`
- Target: `QMPrompter`
- Bundle ID: `com.qiaomu.Prompter`
- Team ID: `58PYLV965G` (Personal Team)
- iOS deployment target: `17.0`

## 本机验证

已通过：

```bash
xcodebuild -project QMPrompter.xcodeproj -target QMPrompter -configuration Debug -sdk iphonesimulator build
xcodebuild -project QMPrompter.xcodeproj -target QMPrompter -configuration Debug -sdk iphoneos CODE_SIGNING_ALLOWED=NO build
```

真机安装已通过：

```bash
xcodebuild -project QMPrompter.xcodeproj -target QMPrompter -configuration Debug -sdk iphoneos -allowProvisioningUpdates -allowProvisioningDeviceRegistration build
xcrun devicectl device install app --device 00008130-00121D002E01001C build/Debug-iphoneos/QMPrompter.app
xcrun devicectl device process launch --device 00008130-00121D002E01001C com.qiaomu.Prompter
```

签名说明：

- 当前使用 `85378058@qq.com` 的 Personal Team：`58PYLV965G`。
- 自动生成的 provisioning profile 有效期到 `2026-06-17 00:54:09 CST`。
- Personal Team 安装包通常 7 天后需要重新构建/安装。
- 旧 Team `L9N9V4J722` 的本机证书已被系统标记为 revoked，不能用于签名。

## 真机测试

1. 打开 `QMPrompter.xcodeproj`。
2. Xcode > Settings > Accounts 里确认 `85378058@qq.com` 的 Personal Team 可用。
3. Xcode > Settings > Components 里补齐当前 iPhone 系统对应的 iOS platform support。
4. 连接 iPhone，选择 `QMPrompter` scheme 和真机。
5. 在 target 的 Signing & Capabilities 确认 Team 可用。
6. 点击 Run。
