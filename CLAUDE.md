# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

QMPrompter (乔木提词器) is a personal-use iOS teleprompter app written entirely in Swift/SwiftUI. It displays scrolling text over a front-camera live preview for speech practice. No video recording in v1. Despite the repo name containing "Android", this is a pure iOS project.

## Build Commands

```bash
# Simulator build
xcodebuild -project QMPrompter.xcodeproj -target QMPrompter -configuration Debug -sdk iphonesimulator build

# Device build (no signing)
xcodebuild -project QMPrompter.xcodeproj -target QMPrompter -configuration Debug -sdk iphoneos CODE_SIGNING_ALLOWED=NO build

# Device build (with signing — needs Personal Team provisioning)
xcodebuild -project QMPrompter.xcodeproj -target QMPrompter -configuration Debug -sdk iphoneos -allowProvisioningUpdates -allowProvisioningDeviceRegistration build
```

There are no tests, no third-party package managers (no SPM, CocoaPods, Carthage), and no CI/CD.

## Architecture

MVVM-like SwiftUI app. The entry point (`QMPrompterApp`) injects a single `ScriptStore` as `@EnvironmentObject` into the view tree.

### Layers

- **Models/** — `Script` (the core data model with display settings: fontSize, scrollSpeed, textColorPreset, overlayOpacity) and `TextColorPreset` enum
- **Services/** — Business logic and system integration, all `@MainActor`:
  - `ScriptStore` — CRUD + JSON persistence (`Documents/scripts.json`), ISO 8601 dates
  - `CameraPreview` — `UIViewRepresentable` wrapping `AVCaptureVideoPreviewLayer` (front camera)
  - `ScrollEngine` — `CADisplayLink`-based scroll animation with pause/resume/speed/jump
  - `SpeechFollower` — `SFSpeechRecognizer` (zh_CN) to track spoken progress and auto-scroll
  - `PromptDictation` — Voice input for AI prompt text
  - `APIKeyStore` — DeepSeek API key stored in iOS Keychain
  - `DeepSeekScriptGenerator` — Async DeepSeek API call (`deepseek-v4-flash` model) for AI script generation
- **Utilities/** — `PromptFormatter` for line-breaking text for teleprompter display
- **Views/** — SwiftUI views: `ScriptListView` (home), `ScriptEditorView` (edit), `PrompterView` (full-screen teleprompter), `AIGenerationView` (AI generation), `GlassActionPanel` (reusable glass UI), `AppSettingsView`

### Key Patterns

- Zero external dependencies — all Apple frameworks (SwiftUI, AVFoundation, Speech, Security, QuartzCore, UIKit)
- iOS 17.0 minimum target; iOS 26.0 Liquid Glass support via `#available(iOS 26.0, *)` with `.ultraThinMaterial` fallback
- `@StateObject` / `@Published` for reactive state; `@EnvironmentObject` for dependency injection
- `async/await` for network calls to DeepSeek API
- JSON file persistence (not Core Data) in the app's Documents directory

## Signing

Personal Team (`58PYLV965G`, `85378058@qq.com`). Provisioning profiles expire after ~7 days and require rebuild. The old Team `L9N9V4J722` is revoked.
