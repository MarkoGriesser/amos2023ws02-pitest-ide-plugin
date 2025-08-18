# PITMutationMate IntelliJ/Android Studio Plugin – Developer Guide

## Overview

This document provides guidance for future developers working on the PITMutationMate plugin, with a focus on maintainability, extensibility, and addressing known issues from the AV-1479 roadmap.

---

## Project Structure

- **pitmutationmate/**: Main IntelliJ/Android Studio plugin code.
- **pitmutationmate-override-plugin/**: Gradle plugin for project-side configuration and communication.
- **coverage-reporter/**: Coverage reporting utilities.
- **Deliverables/**, **Documentation/**: Project management and documentation assets.

---

## Key Concepts

### 1. **Companion Plugin Detection**
- The plugin checks for a marker file (`.pitmutationmate_companion_present`) to verify the override plugin is applied in the project.
- On Gradle sync, the marker file is deleted if the override plugin is removed.
- **Tip:** See `GradleSyncDeleteCompanionFileListener.kt` for logic.

### 2. **Build Variant Handling (Android)**
- The plugin can read the active build variant for each module using Android Studio APIs.
- **Recommended:** Use `AndroidModuleModel.get(module)?.selectedVariant?.name` to automatically detect the current variant.
- **Fallback:** If unavailable, the variant can be set manually in the Run Configuration.

### 3. **Gradle Task Execution**
- PIT runs are triggered via Gradle tasks, with the build variant appended to the task name for Android projects.
- **Known Issue:** PIT is currently invoked at the root project level, which may cause result overwrites in multi-module setups. Consider scoping PIT runs to the relevant subproject.

### 4. **Error Handling & UI Feedback**
- If the override plugin is missing, the plugin disables PIT actions and shows a clear error message.
- UI updates are handled via the `ToolWindowFactory` utilities.

---

## Development & Testing

### 1. **IDE Compatibility**
- The plugin is compatible with both IntelliJ IDEA and Android Studio.
- **Android APIs:** Only available in Android Studio. Use runtime checks for Android-specific features.
- **Testing in AS:** Use the custom Gradle task `runIdeAndroidStudio` and set the `androidStudioHome` property.

### 2. **Java Compatibility**
- Override plugin should target Java 11 for maximum compatibility (see AV-1479).

### 3. **Plugin Installation**
- Developers can install the plugin manually from disk via the Plugins settings in IntelliJ/Android Studio.

---

## Roadmap & Known Issues (from AV-1479)

- **Automatic build variant detection:** Implement logic to read the active variant from the IDE, not just from RunConfig.
---

## Contribution Guidelines

- **No proprietary dependencies:** Do not introduce dependencies on internal (eso) infrastructure.
- **OSS compliance:** Maintain open-source compatibility for future public releases.
- **Documentation:** Update this guide and code comments for any significant changes.

---

## References

- [IntelliJ Platform SDK](https://plugins.jetbrains.com/docs/intellij/welcome.html)
- [Android Studio Plugin Development](https://developer.android.com/studio/write/plugins)
- [PITMutationMate GitHub](https://github.com/amosproj/amos2023ws02-pitest-ide-plugin)
- [IntelliJ Marketplace Listing](https://plugins.jetbrains.com/plugin/23575-pitmutationmate)

---