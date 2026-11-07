# StaffJobs UI Plugin Message Protocol

Status: protocol version 1; read-only overview, online player list, and online player profile endpoints implemented. Client-side player-name filtering is available in the Fabric dashboard.

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

## Implemented report endpoint

Client sends `GET_REPORTS`. Server checks `staffjobs.reports` and replies with up to 20 most recent reports, each containing report ID, reporter name, date, status, assigned staff, and a bounded message preview. Users without that permission receive `SECTION_DENIED` for the reports section. This endpoint is read-only; accepting, rejecting, or assigning reports remains unavailable in the companion UI.

## Implemented player endpoints

Client sends `GET_PLAYERS`. Server checks `staffjobs.players` and replies:
- `PLAYERS`
- number of entries
- that many online player names (maximum 100)

Client sends `GET_PLAYER_PROFILE`, followed by an exact online player name (1–16 ASCII letters, digits, or underscores). Server checks `staffjobs.players`, resolves the target on the server, and replies:
- `PLAYER_PROFILE`, player name, game mode, world name, ping in milliseconds
or:
- `PROFILE_ERROR`, short reason.

Profiles are read-only. No IP address, coordinates, inventory, or other private player data is sent. A target who disconnects before lookup receives an error.

## Security requirements for future messages
- Check each section/action's own permission server-side.
- Validate target existence, state, and all arguments.
- Audit every administrative action.
- Never trust a permission level or action result supplied by the client.
- Reject unsupported protocol versions safely.

## Planned, not yet implemented
- Server-side player search (current search filters the received online-name list locally); richer profile fields
- Report moderation actions (accept/reject/assign)
- Staff analytics and activity pages
- Death logs
- Punishment history/actions
- Settings read/update
