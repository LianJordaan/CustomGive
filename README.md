# CustomGive

CustomGive is a client-side Fabric mod for Creative-mode players who have an item tag or component compound copied as SNBT. It places the resulting stack in an empty inventory slot through Minecraft's normal creative-inventory packet. The server decides whether that action is allowed; it cannot grant items on a server where you lack Creative mode.

## Commands

- `/customgive` — one stone with the clipboard data (the original command behavior).
- `/customgive <item>` — one item from the Minecraft item registry. For example, `minecraft:diamond_sword` or `diamond_sword`.
- `/customgive <item> <amount>` — a stack of 1–64, up to that item's own stack limit.

On Minecraft 1.20.1–1.20.2, copy an item **NBT compound**, such as `{display:{Name:'{"text":"Example"}'}}`. On Minecraft 1.21.11, 26.2 and 26.3, copy a **data-components compound**, such as `{"minecraft:custom_data":{customgive_test:1b}}`; a serialized stack's `{components:{...}}` wrapper is also accepted. The command's item and amount take precedence over any `id` or `count` in the clipboard. Use `{}` for an unchanged item.

The clipboard must contain a valid compound and be no longer than 32,768 characters. CustomGive reports invalid items, malformed data, an overlarge stack, or a full inventory in chat. It never overwrites an occupied slot. The success message means the stack was sent to the server, which may still reject it under its own rules.

## Version matrix

| Minecraft | Java | Clipboard format | Status |
| --- | --- | --- | --- |
| 1.20.1 | 17 | Item NBT | Build and unit tests pass; live client check pending |
| 1.20.2 | 17 | Item NBT | Build and unit tests pass; live client check pending |
| 1.21.11 | 21 | Data components | Build and unit tests pass; live client check pending |
| 26.2 | 25 | Data components | Build and unit tests pass; live client check pending |
| 26.3 | 25 | Data components | Build and unit tests pass; live client check pending |

The pinned Fabric API, mappings, Java level, and source track for each target live in [versions.json](versions.json); the shared Fabric Loader pin is in [gradle.properties](gradle.properties). Each target produces a separate JAR with an exact Minecraft dependency. Minecraft 1.20.5 changed item metadata from NBT to data components. Minecraft 26.x uses Mojang names and Java 25. Each change has a small version-specific adapter.

Build on JDK 25 with `./gradlew build` (or `gradlew.bat build` on Windows). The deployable JARs are in `versions/<minecraft>/build/libs/`; use the JAR without `-sources`. Install the JAR for your exact Minecraft version on the **client**, along with Fabric Loader and Fabric API. A server-side installation is unnecessary.

These are development candidates. The existing Modrinth 1.0.3 release stays published while the replacement is tested in live clients. The repository's [LICENSE](LICENSE) is CC0-1.0; the Modrinth page currently says MIT and needs an owner review before a new upload.
