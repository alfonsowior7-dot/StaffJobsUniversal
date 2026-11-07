# StaffJobs UI Plugin Message Protocol

Status: protocol version 1; read-only overview endpoint implemented.

## Transport
- Channel: `staffjobs:ui`
- Direction: Fabric client mod to Paper plugin; replies return to the same player.
- Every field is a VarInt-length-prefixed UTF-8 string (compatible with Fabric `PacketByteBuf.writeString/readString`).
- Maximum accepted request payload: 1024 bytes.
- The server is authoritative; client-side state never grants permission.

## Handshake
Client sends `HELLO`.
Server checks `staffjobs.dashboard` and replies with:
- `WELCOME`, protocol version, plugin version, player UUID
or:
- `DENIED`, short reason.

A successful handshake creates a per-player session. Repeating HELLO resets that session.

## Implemented read-only request
Client sends `GET_OVERVIEW` after WELCOME. Server rechecks the session and `staffjobs.dashboard` on every request, then replies:
- `OVERVIEW`
- online player count
- maximum player count
- online staff count (players with `staffjobs.use`)

Requests are rate-limited per player. Malformed, oversized, or trailing payload data is rejected. No IP addresses or other private player details are transmitted.

## Security requirements for future messages
- Check each section/action's own permission server-side.
- Validate target existence, state, and all arguments.
- Audit every administrative action.
- Never trust a permission level or action result supplied by the client.
- Reject unsupported protocol versions safely.

## Planned, not yet implemented
- Player search/profile
- Report list and moderation actions
- Staff analytics and activity pages
- Death logs
- Punishment history/actions
- Settings read/update
