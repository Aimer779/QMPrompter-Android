# 提词界面底部显示控制抽屉实施方案

## 背景

当前用户需要在文稿编辑器的“显示”tab 中调整字号、滚动速度、摄像头透明度和文字颜色，再切回提词界面查看效果。这个流程会打断提词位置和演示状态，体验不好。

本轮改为在提词界面增加一个从底部向上弹出的显示控制抽屉，让用户在提词场景中直接调参并实时预览。

## 做什么

- 在 `PrompterScreen` 底部中央增加一个玻璃风格上箭头按钮。
- 点击上箭头后，从底部向上滑出显示控制抽屉。
- 抽屉顶部提供下箭头按钮用于收起。
- 抽屉内提供显示参数：
  - 字号：`12..110`
  - 滚动速度：`20..220`
  - 摄像头透明度：UI 显示透明度，内部继续保存 `overlayOpacity = 1 - transparency`
  - 文字颜色：沿用 `TextColorPreset.editorChoices`
- 调整时实时预览：
  - 正文字号和排版立即更新
  - 自动滚动速度立即更新
  - 摄像头遮罩立即更新
  - 文字颜色立即更新
- 收起抽屉、退出提词页时保存当前显示设置。
- 抽屉区域消费触摸事件，避免底层提词拖拽/调速手势误响应。

## 不做什么

- 不删除编辑器里的“显示”tab，先保留旧入口。
- 不新增数据字段，不修改 Room schema，不做数据库迁移。
- 不改变语音跟随默认关闭的产品决策。
- 不把语音、相机权限、AI 配置放入这个抽屉。
- 不使用默认 `AlertDialog`。
- 不实现拖拽关闭抽屉，第一版只做点击箭头收起。
- 不修改 iOS 代码，只参考现有交互和参数范围。

## 实现思路

- 在 `PrompterScreen` 内维护当前显示参数的局部状态，初始值来自当前 `script`。
- 提词正文、遮罩、滚动引擎配置读取局部状态，而不是直接读取 `script` 的显示字段。
- 当外部 `script` 更新且当前没有未保存显示改动时，同步局部显示状态。
- slider 和颜色选择只更新局部状态，并立即反映到当前界面。
- 滚动速度变化时同步调用 `engine.configure(...)`，保持滚动状态连续。
- 收起抽屉、点击返回、Composable dispose 时保存显示参数。
- 保存仍调用现有 `ScriptRepository.save()`，由 repository 统一更新 `updatedAt`。
- 抽屉 UI 作为 `PrompterScreen.kt` 内的私有 composable，避免过早抽象。

## 涉及 Android 文件

- 主要修改：
  - `app/src/main/kotlin/com/qiaomu/prompter/ui/prompter/PrompterScreen.kt`
- 只读参考：
  - `app/src/main/kotlin/com/qiaomu/prompter/ui/editor/ScriptEditorScreen.kt`
  - `app/src/main/kotlin/com/qiaomu/prompter/data/Script.kt`
  - `app/src/main/kotlin/com/qiaomu/prompter/data/TextColorPreset.kt`
  - `app/src/main/kotlin/com/qiaomu/prompter/data/ScriptRepository.kt`
  - `app/src/main/kotlin/com/qiaomu/prompter/ui/component/GlassActionPanel.kt`

## 参考 iOS 文件路径

- 提词界面、实时保存、显示控制参考：
  - `QMPrompter/Views/PrompterView.swift`
- 编辑器显示设置参数范围、透明度反向映射参考：
  - `QMPrompter/Views/ScriptEditorView.swift`
- 数据模型默认值和颜色枚举参考：
  - `QMPrompter/Models/Script.swift`
- 滚动引擎行为参考：
  - `QMPrompter/Services/ScrollEngine.swift`

## 验收方案

### 命令行验证

- `rtk ./gradlew.bat test --stacktrace`
- `rtk ./gradlew.bat assembleDebug --stacktrace`

### 手工验证

- 进入提词页，底部中央显示上箭头按钮。
- 点击上箭头，显示控制抽屉从底部向上弹出。
- 点击抽屉顶部下箭头，抽屉收起。
- 调整字号，正文立即变大/变小，排版不崩。
- 调整滚动速度，自动滚动速度立即变化。
- 调整摄像头透明度，背景遮罩立即变化，透明度越高摄像头越清楚。
- 切换文字颜色，正文立即变化。
- 收起抽屉后退出再进入，设置仍保留。
- 抽屉打开时操作抽屉区域不会触发底层提词拖拽。
- 语音跟随开启时打开抽屉不自动停止语音。
