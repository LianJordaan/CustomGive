# Changelog

## 1.1.0

- Restored `/customgive` with no arguments: clipboard SNBT is applied to one stone.
- Added optional item ID and amount, including namespaced Minecraft item IDs.
- Added clear errors for invalid clipboard data, item IDs, amounts, and full inventories. The command finds an empty slot and leaves occupied slots alone.
- Kept item creation tied to Creative inventory packets. The server decides whether a change is accepted; Survival players cannot use the command to gain items.
- Added separate exact-version Fabric builds for Minecraft 1.20.1 through 26.3. Versions 1.20.1–1.20.4 use item NBT; 1.20.5 and newer use data components.

All 23 exact-version JARs from Minecraft 1.20.1 through 26.3 passed real Fabric client checks against isolated offline-mode servers with Creative inventory sync, reconnect persistence, and Survival denial. The 1.20.1 JAR also passed an additional test with an actual Java 17 client. Minecraft 1.20.3, 1.21.2, and 26.1 used Vanilla servers; Paper 1.20.5, 1.21.5, 1.21.9, and 26.1.1 used alpha builds, and Paper 26.3 used a beta build. Minecraft 26.3 itself is a stable release. See the README for the exact compatibility matrix and testing limits. The 1.0.3 Modrinth release remains available until 1.1.0 is published.
