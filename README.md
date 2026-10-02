# CustomGive

CustomGive is a client-side Fabric mod for Creative-mode players who have an item tag or component compound copied as SNBT. It places the resulting stack in an empty inventory slot through Minecraft's normal creative-inventory packet. The server decides whether that action is allowed; it cannot grant items on a server where you lack Creative mode.

## Commands

- `/customgive` — one stone with the clipboard data (the original command behavior).
- `/customgive <item>` — one item from the Minecraft item registry. For example, `minecraft:diamond_sword` or `diamond_sword`.
- `/customgive <item> <amount>` — a stack of 1–64, up to that item's own stack limit.

On Minecraft 1.20.1–1.20.4, copy an item **NBT compound**, such as `{display:{Name:'{"text":"Example"}'}}`. On Minecraft 1.20.5 and newer, copy a **data-components compound**, such as `{"minecraft:custom_data":{customgive_test:1b}}`; a serialized stack's `{components:{...}}` wrapper is also accepted. The command's item and amount take precedence over any `id` or `count` in the clipboard. Use `{}` for an unchanged item.

The clipboard must contain a valid compound and be no longer than 32,768 characters. CustomGive reports invalid items, malformed data, an overlarge stack, or a full inventory in chat. It never overwrites an occupied slot. The success message means the stack was sent to the server, which may still reject it under its own rules.

## Version matrix

CustomGive 1.1.0 has a separate Fabric JAR for each exact Minecraft version below. All 23 frozen JARs passed a real-client check against an isolated server: `/customgive` created a stone item from clipboard data, `/customgive minecraft:diamond 2` sent custom data that survived server inventory sync, the items persisted after reconnecting, and Survival mode denied the command. The source used for the frozen JARs is revision `d22c2fa352d079613e748a23e70e76e29258c648`. Exact SHA-512 hashes, server pins, client logs, and receipts are retained in the parent workspace under `testing/customgive/`.

| Minecraft | JAR Java minimum | Client test Java | Test server | Result |
| --- | ---: | ---: | --- | --- |
| 1.20.1 | 17 | 17 and 21 | Paper, stable build | Passed |
| 1.20.2 | 17 | 21 | Paper, stable build | Passed |
| 1.20.3 | 17 | 21 | Vanilla | Passed |
| 1.20.4 | 17 | 21 | Paper, stable build | Passed |
| 1.20.5 | 21 | 21 | Paper, **alpha** build | Passed |
| 1.20.6 | 21 | 21 | Paper, stable build | Passed |
| 1.21 | 21 | 21 | Paper, stable build | Passed |
| 1.21.1 | 21 | 21 | Paper, stable build | Passed |
| 1.21.2 | 21 | 21 | Vanilla | Passed |
| 1.21.3 | 21 | 21 | Paper, stable build | Passed |
| 1.21.4 | 21 | 21 | Paper, stable build | Passed |
| 1.21.5 | 21 | 21 | Paper, **alpha** build | Passed |
| 1.21.6 | 21 | 21 | Paper, stable build | Passed |
| 1.21.7 | 21 | 21 | Paper, stable build | Passed |
| 1.21.8 | 21 | 21 | Paper, stable build | Passed |
| 1.21.9 | 21 | 21 | Paper, **alpha** build | Passed |
| 1.21.10 | 21 | 21 | Paper, stable build | Passed |
| 1.21.11 | 21 | 21 | Paper, stable build | Passed |
| 26.1 | 25 | 25 | Vanilla | Passed |
| 26.1.1 | 25 | 25 | Paper, **alpha** build | Passed |
| 26.1.2 | 25 | 25 | Paper, stable build | Passed |
| 26.2 | 25 | 25 | Paper, stable build | Passed |
| 26.3 | 25 | 25 | Paper, **beta** build | Passed |

Minecraft 26.3 is a stable Minecraft release; the Paper build used for this check was beta. Paper builds were unavailable for the three Vanilla rows. The checks used standalone offline-mode servers and synthetic client identities. They do not establish authenticated multiplayer or singleplayer behavior. The 1.20.1–1.20.4 core client checks used Java 21 even though those JARs target Java 17; 1.20.1 also passed an additional test with an actual Java 17 client. Java 17 client execution remains untested for 1.20.2–1.20.4. The 26.3 client check ran on Linux with Xvfb/Mesa after the Windows graphics driver crashed before gameplay; the exact JAR passed on Linux.

The pinned Fabric API, mappings, Java level, and source track for each target are in [versions.json](versions.json); the shared Fabric Loader pin is in [gradle.properties](gradle.properties). Minecraft 1.20.5 changed item metadata from NBT to data components, and 26.x uses Mojang names and Java 25. Each change has a small version-specific adapter.

Build on JDK 25 with `./gradlew build` (or `gradlew.bat build` on Windows). The deployable JARs are in `versions/<minecraft>/build/libs/`; use the JAR without `-sources`. Install the JAR for your exact Minecraft version on the **client**, along with Fabric Loader and Fabric API. Server-side installation is unnecessary.

LianJordaan created and coded CustomGive in 2023 and wrote or substantially revised code in the current 1.1.0 release. The compatibility ports, revised command, tests, documentation, and Modrinth page text also received substantial generative-AI contributions. The Modrinth page carries the corresponding code and text disclosure.

The existing Modrinth 1.0.3 release remains available. All 23 tested 1.1.0 Fabric versions are publicly listed on Modrinth, each with its own exact JAR. This repository's [LICENSE](LICENSE) is CC0-1.0, matching the Modrinth page.
