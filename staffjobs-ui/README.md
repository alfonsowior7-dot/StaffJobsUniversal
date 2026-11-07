# StaffJobs UI

Client-side Fabric companion mod for StaffJobs Universal.

## Current target

- Minecraft Java: **1.21.11**
- Java runtime / compile target: **21**
- Fabric Loader: **0.18.1 or newer**
- Fabric API: **0.141.6+1.21.11**
- Yarn mappings: **1.21.11+build.6**
- Mod version: **2.0.0-beta.2**

## Features

- Client-only Fabric entrypoint.
- Right Alt keybind to open the StaffJobs dashboard handshake.
- Protocol v1 HELLO / WELCOME / DENIED handling.
- Black-and-green dashboard UI.
- Player list and online player profile requests.
- Report center and server-authoritative report actions.
- Staff analytics, activity history, death logs, punishment history and theme settings.
- Local preferences stored in `config/staffjobs-ui.properties`.

## Build

Use a Java 21 JDK. From this directory:

```sh
gradle build
```

The output jar is placed in `build/libs/`.

## Compatibility

This beta build targets Minecraft 1.21.11 only. Other 1.20.x, 1.21.x, and 26.x versions require separate version-specific builds/adapters and testing.

## Server

Install StaffJobs Universal 2.0.0 beta on the Paper server. Staff members need this client mod. The server remains authoritative and checks `staffjobs.dashboard` during the handshake and every request; each dashboard section has its own permission check.

## Important testing status

A successful CI build verifies compilation and packaging only. The Paper-to-Fabric connection, dashboard rendering, and actions still require in-game testing on a 1.21.11 client and compatible Paper server.
