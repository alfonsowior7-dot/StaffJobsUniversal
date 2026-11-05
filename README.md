# STAFFJOBS UNIVERSAL 1.1.0

**Universal Staff Management Plugin — 1.1.0**

STAFFJOBS UNIVERSAL is a Minecraft staff management plugin that brings moderation, staff tools, activity tracking, and configurable server administration features together in one plugin.

## Features

- Staff management system
- Player report system
- Staff activity logs
- Death logs and repeatable item restoration
- Player freeze system
- Punishment management and history
- Staff notifications
- Configurable plugin settings

## 1.1.0 Release Notes

- Added an operator-only, per-player X-Ray view in the second STAFFJOBS menu page.
- Toggle X-Ray on or off from `/staffmenu` → Next Page.
- X-Ray changes are visual only and do not modify world blocks.

## Previous 1.0.2 Improvements

- Added report assignment to the currently viewing staff member, with protection against taking a report already assigned to another staff member.
- Added explicit report rejection and archived `REJECTED` status alongside accepted reports.
- Added reporter notifications for accepted and rejected reports, including offline delivery.
- Added report action audit entries.
- Added refresh control to Staff Activity Logs.
- Kept death inventory restoration repeatable; every restore is allowed and the latest staff, timestamp, and total restore count are recorded.
- Improved death rollback audit entries with explicit before/after state.
- Added an Online Staff GUI showing connected staff and their permission rank.
- Added persistent per-staff chat and sound notification toggles for new reports.
- Added configurable notification sound through `settings.notifications.sound`.
- Improved frozen-player interaction restrictions.
- Added configurable timezone handling and punishment history limits.
- Added periodic data saving when auto-save is enabled and a final data save during plugin shutdown.
- Added staff AFK status and manual backup management through `/staffbackup`.

## Backup Commands

- `/staffbackup create` — create a manual data backup.
- `/staffbackup list` — list available backups.
- `/staffbackup restore <backup-name>` — restore data files from a named backup. A safety backup is created first; restart the server after restoration so in-memory data reloads.

Requires `staffjobs.backup` (default: operators).

## Platform and Runtime

- Platform: Paper (Bukkit-compatible Paper API)
- Plugin bytecode target / build toolchain: Java 17
- Minimum API target: Paper 1.20.1
- Plugin API declaration: 1.20
- Server runtime Java depends on the Paper version: Java 17 for Paper 1.20.1–1.20.4; Java 21 for Paper 1.20.5–1.21.11; Java 25 for Paper 26.x. Always follow the requirements of the exact Paper build you run.

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
2. Place `StaffJobsUniversal-1.1.0.jar` in the server's `plugins` directory.
3. Start the server.
4. Check the console for successful plugin loading.

Back up your server and plugin data before installing or updating.

## Build

Run:

```bash
gradle clean build
```

Output:

`build/libs/StaffJobsUniversal-1.1.0.jar`

## Versioning

STAFFJOBS UNIVERSAL follows Semantic Versioning:

- Patch releases (for example, 1.0.2) contain bug fixes.
- Minor releases (for example, 1.1.0) introduce backward-compatible features.
- Major releases (for example, 2.0.0) may contain breaking changes.

## Support, Issue Tracker, and Source Code

- **Issue Tracker:** https://github.com/alfonsowior7-dot/StaffJobsUniversal/issues
- **Report a Bug:** https://github.com/alfonsowior7-dot/StaffJobsUniversal/issues/new
- **Source Code:** https://github.com/alfonsowior7-dot/StaffJobsUniversal

Please report bugs with the server version, Java version, plugin version, relevant console errors, and clear reproduction steps. Do not include server passwords, tokens, or other private information in public issues.
