# CustomGive

CustomGive is a client-side Fabric mod for Creative-mode players who have an item tag or component compound copied as SNBT. It places the resulting stack in an empty inventory slot through Minecraft's normal creative-inventory packet. The server decides whether that action is allowed; it cannot grant items on a server where you lack Creative mode.

## Commands

- `/customgive` — one stone with the clipboard data (the original command behavior).
- `/customgive <item>` — one item from the Minecraft item registry. For example, `minecraft:diamond_sword` or `diamond_sword`.
- `/customgive <item> <amount>` — a stack of 1–64, up to that item's own stack limit.

On Minecraft 1.20.1–1.20.4, copy an item **NBT compound**, such as `{display:{Name:'{"text":"Example"}'}}`. On Minecraft 1.20.5 and newer, copy a **data-components compound**, such as `{"minecraft:custom_data":{customgive_test:1b}}`; a serialized stack's `{components:{...}}` wrapper is also accepted. The command's item and amount take precedence over any `id` or `count` in the clipboard. Use `{}` for an unchanged item.

The clipboard must contain a valid compound and be no longer than 32,768 characters. CustomGive reports invalid items, malformed data, an overlarge stack, or a full inventory in chat. It never overwrites an occupied slot. The success message means the stack was sent to the server, which may still reject it under its own rules.

## Version matrix

The live results in this table are staging checks on earlier `1.1.0-dev` JARs. The final `1.1.0` JARs have now built and passed unit/artifact checks for all 23 exact Minecraft targets; fresh client-server checks against their new hashes are underway. The parent workspace retains the frozen artifact manifest at `testing/customgive/candidates/manifest-1.1.0.json`.

| Minecraft | Java | Clipboard format | Status |
| --- | --- | --- | --- |
| 1.20.1 | 17 | Item NBT | Build, unit tests, and Creative/relog/Survival client-server checks pass |
| 1.20.2 | 17 | Item NBT | Build, unit tests, and Creative/relog/Survival client-server checks pass |
| 1.20.3 | 17 | Item NBT | Build and unit tests pass; live client check pending |
| 1.20.4 | 17 | Item NBT | Build and unit tests pass; live client check pending |
| 1.20.5 | 21 | Data components | Build, unit tests, and Creative/relog/Survival client-server checks pass on Paper ALPHA |
| 1.20.6 | 21 | Data components | Build and unit tests pass; live client check pending |
| 1.21–1.21.4 | 21 | Data components | Each exact-version JAR builds and passes unit tests; live checks pending |
| 1.21.5 | 21 | Data components | Build, unit tests, and Creative/relog/Survival client-server checks pass on Paper ALPHA |
| 1.21.6–1.21.10 | 21 | Data components | Each exact-version JAR builds and passes unit tests; live checks pending |
| 1.21.11 | 21 | Data components | Build, unit tests, and Creative/relog/Survival client-server checks pass |
| 26.1 | 25 | Data components | Build, unit tests, and Creative/relog/Survival client-server checks pass on vanilla server |
| 26.1.1–26.1.2 | 25 | Data components | Each exact-version JAR builds and passes unit tests; live checks pending |
| 26.2 | 25 | Data components | Build, unit tests, and Creative/relog/Survival client-server checks pass |
| 26.3 | 25 | Data components | Build and unit tests pass; live check blocked by this host's graphics crash |

The pinned Fabric API, mappings, Java level, and source track for each target live in [versions.json](versions.json); the shared Fabric Loader pin is in [gradle.properties](gradle.properties). Each target produces a separate JAR with an exact Minecraft dependency. Minecraft 1.20.5 changed item metadata from NBT to data components. Minecraft 26.x uses Mojang names and Java 25. Each change has a small version-specific adapter.

Build on JDK 25 with `./gradlew build` (or `gradlew.bat build` on Windows). The deployable JARs are in `versions/<minecraft>/build/libs/`; use the JAR without `-sources`. Install the JAR for your exact Minecraft version on the **client**, along with Fabric Loader and Fabric API. A server-side installation is unnecessary.

The existing Modrinth 1.0.3 release stays published while the 1.1.0 replacement is tested in live clients. The repository's [LICENSE](LICENSE) is CC0-1.0; the Modrinth page currently says MIT and needs an owner review before a new upload.

The 1.21.11 live check used the packaged JAR with SHA-512 prefix `23b97050c12c` against Paper 1.21.11 build 132 in a private offline-mode server. The exact artifact, logs, screenshot, and result receipt are retained in the parent workspace under `testing/customgive/live-1.21.11-2026-10-01/`. This checks the Creative inventory transfer and persistence on that server; it does not establish compatibility for untested Minecraft releases or authenticated multiplayer sessions.

The same checks passed on 26.2 against Paper build 129 with packaged JAR SHA-512 prefix `d41240ed1171`. Its evidence is under `testing/customgive/live-26.2-2026-10-01/` in the parent workspace.

The 1.20.1 and 1.20.2 checks passed against Paper builds 196 and 318 with exact packaged JAR SHA-512 prefixes `3d4a8ca2e862` and `d7afdfa75674`. Their receipts and client logs are under `testing/customgive/live-1.20.1-2026-10-02/` and `testing/customgive/live-1.20.2-2026-10-02/`. The client ran on Java 21 while those servers ran on Java 17; these checks do not establish Java 17 client execution.

Staging checks also passed on 1.20.5 (Paper build 22 ALPHA), 1.21.5 (Paper build 114 ALPHA), and 26.1 (vanilla server). Their earlier development-JAR receipts are under the corresponding `testing/customgive/live-<version>-2026-10-02/` directories in the parent workspace. They must be repeated against the final `1.1.0` hashes before publication.

The current Modrinth releases are 1.0.2 and 1.0.3. A future update needs a distinct version number and must keep the published releases available.

On this Windows test host, the 26.3 client exits during graphics/resource initialization before CustomGive's gameplay test starts, even with Vulkan selected. Its JAR is therefore not live-qualified. The failed-launch log and exact hash are retained under `testing/customgive/live-26.3-graphics-blocked-2026-10-01/` in the parent workspace.
