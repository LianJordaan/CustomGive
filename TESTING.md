# CustomGive live release checks

The Gradle matrix compiles each exact-version JAR and runs the SNBT/component codec and input tests. A successful build does not establish that the client command and the server inventory handshake work in play.

For each version in the [matrix](versions.json), install its exact JAR and matching Fabric API on a Fabric client, then test against an ordinary server without CustomGive installed:

1. In Creative mode with a free inventory slot, copy `{}` and use `/customgive`. Confirm exactly one stone appears in the first empty slot and remains there after closing and reopening inventory.
2. Use `/customgive minecraft:diamond 2` with `{}`. Confirm two diamonds arrive. Use a modded item ID if available to check registry lookup beyond the old hardcoded list.
3. On 1.20.1/1.20.2, copy `{CustomModelData:7}`. On 1.21.11/26.2/26.3, copy `{"minecraft:custom_data":{customgive_test:1b}}`. Confirm the resulting item's data persists after relogging; component parsing also runs automatically during `gradlew test`.
4. Try malformed SNBT, an unknown item ID, `/customgive minecraft:diamond_sword 2`, and a full main inventory. Confirm useful chat errors and that no occupied slot changes.
5. Switch to Survival mode and try the command. Confirm that no item appears. Server-side enforcement of creative packets should also be covered by a separate integration test before release.

Run the first four checks in both singleplayer and multiplayer Creative mode. Inspect the client and server logs for exceptions. Record the exact Minecraft, Fabric Loader/API, Java and JAR SHA-512 used; do not add a Modrinth compatibility tag until that exact artifact passes. The published 1.0.3 version stays available in the meantime.
