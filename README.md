# STAFFJOBS UNIVERSAL 1.0.0

**Official Stable Release**

STAFFJOBS UNIVERSAL is a Minecraft staff management plugin designed to bring essential moderation and server administration tools together in one plugin.

## Features

- Staff management system
- Player report system
- Staff activity logs
- Death logs and item restoration
- Player freeze system
- Punishment management and history
- Staff notifications
- Configurable plugin settings

## 1.0.0 Release Notes

- Established the first official stable release of STAFFJOBS UNIVERSAL.
- Improved frozen-player interaction restrictions, including inventory clicks, dragging, hotbar switching, and off-hand swapping.
- Kept death notifications private to staff with the configured permission.
- Added configurable timezone handling.
- Added configurable punishment history limits and mute messages.
- Improved empty-report message validation.
- Added periodic data saving when auto-save is enabled.
- Added a final data save during plugin shutdown.

## Platform and Runtime

- Platform: Paper
- Java: 17
- Minimum API target: Paper 1.20.1
- Plugin API declaration: 1.20

## Target Compatibility

The universal build is intended for these Paper version families:

- Paper 1.20.x
- Paper 1.21.x
- Paper 26.1
- Paper 26.2
- Paper 26.3

**Important:** A successful build does not guarantee runtime compatibility. Each advertised server version must be tested with the same release JAR before being marked as verified.

## Installation

1. Stop your Paper server.
2. Place `StaffJobsUniversal-1.0.0.jar` in the server's `plugins` directory.
3. Start the server.
4. Check the console for successful plugin loading.

Back up your server and plugin data before installing or updating.

## Build

Run:

```bash
gradle clean build
```

Output:

`build/libs/StaffJobsUniversal-1.0.0.jar`

## Versioning

STAFFJOBS UNIVERSAL follows Semantic Versioning:

- Patch releases (for example, 1.0.1) contain bug fixes.
- Minor releases (for example, 1.1.0) introduce backward-compatible features.
- Major releases (for example, 2.0.0) may contain breaking changes.

## Support and Feedback

Please report bugs with the server version, Java version, plugin version, relevant console errors, and clear reproduction steps.
