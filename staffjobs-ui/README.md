# StaffJobs UI

Client-side Fabric companion mod for StaffJobs Universal.

## Current status

This is the first development scaffold for Minecraft 1.20.1. It currently includes:

- Client-only Fabric entrypoint.
- Right Alt keybind to send the StaffJobs UI handshake.
- Protocol v1 HELLO / WELCOME / DENIED handling.
- Initial dark black-and-green dashboard shell.
- Server-side permission remains authoritative.

The dashboard cards and navigation are visual placeholders. Player data, report actions, punishment actions, analytics, and settings are not wired up yet.

## Build

From this directory, use a Java 17 JDK and run:

```sh
gradle build
```

The output jar is placed in `build/libs/`.

## Compatibility

Minecraft 1.20.1 is the initial compile target. Additional 1.20.x, 1.21.x, and 26.x versions require version-specific builds/adapters and actual client/server testing before being marked supported.

## Server

Install StaffJobs Universal 2.0.0 beta on the Paper server. A staff member also needs this client mod. The server checks `staffjobs.dashboard`; each future dashboard feature will require its own permission check.


### Local UI preferences

The Fabric client stores per-device presentation preferences in `config/staffjobs-ui.properties`: `compactRows` controls staff roster density and `showOfflineStaff` controls whether offline staff appear in the analytics roster. These settings are local to each client and do not change server configuration or permissions.
