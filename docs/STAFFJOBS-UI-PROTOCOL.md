# StaffJobs UI Plugin Message Protocol

Status: draft, protocol version 1.

## Transport

- Minecraft plugin messaging channel: `staffjobs:ui`
- Direction: client companion mod to Paper plugin, with replies from Paper to the same player.
- Payload encoding: Java `DataOutputStream.writeUTF` fields in sequence.
- Maximum request payload accepted by the initial bridge: 1024 bytes.
- The server is authoritative. The client must never be trusted to grant permissions or decide whether an action is allowed.

## Version 1 handshake

Client sends one UTF field:

```text
HELLO
```

If the player has `staffjobs.dashboard`, server replies with four UTF fields:

1. `WELCOME`
2. Protocol version (currently `1`)
3. Installed StaffJobs plugin version
4. Player UUID

If the player lacks permission, server replies with:

1. `DENIED`
2. A short reason

The server rate-limits repeated HELLO requests per player.

## Security requirements for future messages

- Every request must be validated server-side.
- Every dashboard section and action must check its own permission node.
- Never accept a permission level, target UUID, or action result as authoritative merely because it came from the client.
- Validate target existence, online/offline state, arguments, and action-specific constraints.
- Administrative actions must be audited using the existing StaffJobs activity log system.
- Do not send private player data until the client has completed the handshake and the player has the relevant permission.
- Future protocol changes must use an explicit protocol version and reject unsupported versions safely.

## Planned message families

These are design placeholders, not implemented messages:

- `DASHBOARD_SUMMARY`
- `PLAYER_SEARCH` / `PLAYER_PROFILE`
- `REPORT_LIST` / `REPORT_ACTION`
- `ACTIVITY_PAGE`
- `DEATHLOG_PAGE`
- `PUNISHMENT_HISTORY` / `PUNISHMENT_ACTION`
- `SETTINGS_READ` / `SETTINGS_UPDATE`

No player data or administrative action messages are implemented in protocol v1.
