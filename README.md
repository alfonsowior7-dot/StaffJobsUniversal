# STAFFJOBS Universal

A single-JAR build of STAFFJOBS targeting Paper servers across these Minecraft version families:

- Minecraft 1.20.x
- Minecraft 1.21.x
- Minecraft 26.x (including 26.1, 26.2, and 26.3)

## Build strategy

- One shared source code and one distributable JAR.
- Compiled against the oldest supported Paper API (1.20.1).
- Java 17 bytecode target to retain compatibility with older supported server runtimes.
- `plugin.yml` uses API version `1.20`.

## Compatibility notice

A successful build does not prove runtime compatibility with every Paper release. Test the JAR on representative servers from each version family, especially 26.x, before advertising full support. If a newer Paper release removes an API used by STAFFJOBS, the compatibility layer must be updated without raising the minimum supported server version.

## Build

Run `gradle build`.

Output: `StaffJobs1.0.3-Universal-1.20.x_1.21.x_26.x.jar`
