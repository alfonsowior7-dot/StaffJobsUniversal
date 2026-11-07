# STAFFJOBS UNIVERSAL 1.1.0

**Universal Staff Management Plugin for Paper servers**

STAFFJOBS UNIVERSAL combines staff management, player reports, moderation tools, activity history, death records, inventory restoration, and data backups in one plugin.

## Features

- Staff management GUI with permission and rank checks
- Player reports with assignment, acceptance, rejection, and reporter notifications
- Staff Activity Logs and per-staff history
- Death logs with repeatable inventory restoration
- Player freeze controls
- Kick, ban, temporary ban, warning, mute, and punishment history
- Online staff status and configurable staff notifications
- Manual and scheduled data backups
- Configurable timezone, notifications, retention limits, and freeze behavior

## 1.1.0 Release Notes

- Added report assignment to the currently viewing staff member.
- Added explicit report rejection and archived `REJECTED` status.
- Added reporter notifications for accepted and rejected reports, including offline delivery.
- Added report action audit entries and improved staff history navigation.
- Kept death inventory restoration repeatable and recorded the latest restore actor, timestamp, and count.
- Improved death rollback audit entries with before/after details.
- Added online staff status and persistent per-staff notification preferences.
- Added configurable notification sound, timezone handling, and punishment history limits.
- Added scheduled data saving and final data saving during plugin shutdown.
- Added staff AFK status and manual backup management through `/staffbackup`.
- Improved freeze interaction restrictions and inventory GUI handling.
- Fixed backup restore retention so the selected source backup is preserved during restore.
- Fixed GUI selection to use stable report IDs, death record IDs, and player UUIDs instead of relying on changing list positions.
- Expanded activity logging for report assignment, report acceptance, warnings, mutes, unmute actions, and freeze changes.

## Backup Commands

- `/staffbackup create` — create a manual data backup.
- `/staffbackup list` — list available backups.
- `/staffbackup restore <backup-name>` — restore data files from a named backup.

A safety backup is created before restoration. The selected backup is validated and protected from automatic retention cleanup. Restart the server after restoration so all in-memory plugin data is reloaded.

Requires `staffjobs.backup` (default: operators).

## Permissions

| Permission | Default | Purpose |
|---|---|---|
| `staffjobs.use` | op | Open the staff menu |
| `staffjobs.report` | true | Submit a player report |
| `staffjobs.backup` | op | Create, list, and restore backups |
| `staffjobs.kick` | op | Kick players |
| `staffjobs.ban` | op | Ban players |
| `staffjobs.freeze` | op | Freeze or unfreeze players |
| `staffjobs.invsee` | op | View player inventories |
| `staffjobs.warn` | op | Issue warnings |
| `staffjobs.mute` | op | Mute players |
| `staffjobs.unmute` | op | Remove mutes |
| `staffjobs.punishments` | op | View punishment history |
| `staffjobs.deathnotify` | op | Receive player death notifications |
| `staffjobs.protect.bypass` | false | Bypass staff hierarchy checks |
| `staffjobs.updatecheck` | op | Receive update notifications |

## Platform and Runtime

- Platform: Paper (Bukkit-compatible Paper API)
- Plugin bytecode target: Java 17
- Minimum API target: Paper 1.20.1
- Plugin API declaration: `1.20`

Paper's required Java runtime depends on the server version. Use the Java version required by the exact Paper build you install.

## Target Compatibility

The universal build is compiled against Paper API 1.20.1 and is intended for these Paper version families:

- Paper 1.20.x
- Paper 1.21.x
- Paper 26.x

A successful compilation does not guarantee runtime compatibility. Test the release JAR on each server version you intend to support before deploying it to production.

## Installation

1. Stop your Paper server.
2. Back up your server and existing STAFFJOBS data folder.
3. Place `StaffJobsUniversal-1.1.0.jar` in the server's `plugins` directory.
4. Start the server.
5. Check the console for successful plugin loading.

## Build

Requires JDK 17 and Gradle.

```bash
gradle clean check build
```

Output:

`build/libs/StaffJobsUniversal-1.1.0.jar`

## Versioning

STAFFJOBS UNIVERSAL follows Semantic Versioning:

- Patch releases (for example, 1.1.1) contain backward-compatible bug fixes.
- Minor releases (for example, 1.2.0) introduce backward-compatible features.
- Major releases (for example, 2.0.0) may contain breaking changes.

## Support and Source Code

- **Issue Tracker:** https://github.com/alfonsowior7-dot/StaffJobsUniversal/issues
- **Report a Bug:** https://github.com/alfonsowior7-dot/StaffJobsUniversal/issues/new
- **Source Code:** https://github.com/alfonsowior7-dot/StaffJobsUniversal

When reporting a bug, include the server version, Java version, plugin version, relevant console errors, and clear reproduction steps. Never include server passwords, tokens, or other private information in public issues.


## 2.0.0 Beta Development

The `beta-2.0.0` branch is the development line for the planned StaffJobs companion dashboard.

- The Paper plugin remains the authoritative source for permissions and server-side actions.
- The optional Fabric companion mod is intended for staff members only.
- The initial plugin-message bridge uses channel `staffjobs:ui` and protocol version `1`.
- The first bridge milestone implements only a permission-checked capability handshake. It does not yet expose player data or execute administrative actions.
- Dashboard permissions are granular and can be assigned through your server permission manager (for example, LuckPerms).
- Default visual theme: `BLACK_GREEN`.

Minecraft 1.20.x, 1.21.x, and 26.x remain compatibility targets, subject to separate build and runtime testing for each supported game/API generation.
