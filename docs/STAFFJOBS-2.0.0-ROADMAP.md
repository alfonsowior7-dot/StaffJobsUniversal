# StaffJobs Universal 2.0.0 Beta Roadmap

## Architecture

1. **StaffJobs Universal (Paper plugin):** authoritative permissions, persistence, validation, audit logging, and server actions.
2. **StaffJobs UI (Fabric companion mod):** dashboard window and client-side navigation, intended only for staff with the relevant permissions.
3. **StaffJobs Resource Pack:** optional shared visual assets such as icon glyphs, textures, and fonts.

The Fabric companion mod scaffold now lives in `staffjobs-ui/` in this repository. It has a dashboard shell, protocol receiver, and read-only player views.

## Completed foundation

- [x] Keep development isolated on `beta-2.0.0`.
- [x] Add the `staffjobs:ui` plugin messaging channel.
- [x] Add protocol v1 permission-checked HELLO/WELCOME/DENIED handshake.
- [x] Add dashboard and granular permission nodes.
- [x] Add BLACK_GREEN theme defaults and configurable color fields.
- [x] Document the initial protocol and security expectations.

## Next milestones

- [ ] Create the separate Fabric companion mod repository.
- [x] Implement the client handshake and protocol-version validation.
- [x] Implement the dark dashboard shell and navigation (theme configuration loader remains pending).
- [x] Add permission-checked overview summary data.
- [x] Add read-only online player list and online player profile.
- [x] Add client-side name filtering to the online player list.
- [ ] Add server-side search pagination and permission-aware management actions.
- [ ] Add Report Center, Activity History, Death Logs, and Punishments.
- [ ] Add analytics and settings screens.
- [ ] Add optional resource-pack assets.
- [ ] Build and test each supported Minecraft version family separately.

## Compatibility policy

The intended targets are Minecraft 1.20.x, 1.21.x, and 26.x. Support is conditional on compatible Fabric toolchains and server-side API availability for each version family. Do not advertise a version as supported until both the plugin and client mod have been built and tested against it.

## Permission nodes

- `staffjobs.dashboard`
- `staffjobs.players`
- `staffjobs.reports`
- `staffjobs.analytics`
- `staffjobs.activity`
- `staffjobs.deathlogs`
- `staffjobs.punishments`
- `staffjobs.settings`
- `staffjobs.admin` (parent permission for the dashboard permission set)

Existing permissions such as `staffjobs.kick`, `staffjobs.ban`, `staffjobs.freeze`, and `staffjobs.invsee` continue to govern their respective actions.
