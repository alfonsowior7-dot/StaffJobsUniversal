# StaffJobs UI Plugin Message Protocol

Status: protocol version 1; overview, player views, reports, activity, staff analytics, and read-only history endpoints implemented.

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

## Implemented staff analytics endpoint

Client sends `GET_ANALYTICS`. Server checks `staffjobs.analytics` and returns live online staff count, AFK staff count, tracked staff count, and up to 10 roster entries (name, ONLINE/AFK/OFFLINE status, last-online timestamp). The roster only counts records explicitly marked as staff. Historical session duration and cumulative playtime are not currently tracked. Unauthorized clients receive `SECTION_DENIED` for the analytics section.

## Implemented activity endpoint

Client sends `GET_ACTIVITY`. Server checks `staffjobs.activity` and returns up to 10 most recent staff audit entries, each bounded to 240 characters. Users without permission receive `SECTION_DENIED` for the activity section. The dashboard displays the entries read-only.

## Implemented report endpoint

Client sends `GET_REPORTS`. Server checks `staffjobs.reports` and replies with up to 20 most recent reports, each containing report ID, reporter name, date, status, assigned staff, and a bounded message preview. Users without that permission receive `SECTION_DENIED` for the reports section. The companion UI supports selecting a report and sending `REPORT_ACTION` with an action (`ACCEPT`, `REJECT`, or `ASSIGN`) and report ID. The server rechecks `staffjobs.reports`, validates the ID and action, rejects duplicate assignment, and delegates to the existing audited report handlers. The server replies with `REPORT_ACTION_RESULT`, a result code, and a bounded status message. Report actions are executed server-side; client-side permission state is never trusted.

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

## Implemented punishment history endpoint

Client sends `GET_PUNISHMENTS`. Server checks `staffjobs.punishments` and returns up to 30 newest stored history records as player name plus record text. The Fabric dashboard displays the first 10 records and indicates when more are available. This endpoint is read-only: no punishment, unmute, or removal action is accepted from the client. Unauthorized clients receive `SECTION_DENIED` for the `punishments` section.

## Implemented death log endpoint

Client sends `GET_DEATH_LOGS`. Server checks `staffjobs.deathlogs` and returns up to 20 newest records with record ID, player name, timestamp, world and block coordinates, death cause, and level. The Fabric dashboard renders the latest 10 entries. Inventory, armor, offhand, and other rollback snapshot contents are deliberately not transmitted. Users without permission receive `SECTION_DENIED` for the `deaths` section.

## Security requirements for future messages
- Check each section/action's own permission server-side.
- Validate target existence, state, and all arguments.
- Audit every administrative action.
- Never trust a permission level or action result supplied by the client.
- Reject unsupported protocol versions safely.

## Planned, not yet implemented
- Server-side player search (current search filters the received online-name list locally); richer profile fields
- Report action result refresh/pagination and richer report detail view
- Staff session duration / cumulative playtime tracking
- Death logs
- Punishment history/actions
- Settings read/update
