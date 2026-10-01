# Changelog

## 1.1.0 — prepared for testing

- Restored `/customgive` with no arguments: clipboard SNBT is applied to one stone.
- Added optional item ID and amount, including namespaced Minecraft item IDs.
- Added clear errors for invalid clipboard data, item IDs, amounts, and full inventories. The command finds an empty slot and leaves occupied slots alone.
- Kept item creation tied to Creative inventory packets. The server decides whether a change is accepted; Survival players cannot use the command to gain items.
- Added separate exact-version Fabric builds for Minecraft 1.20.1 through 26.3. Versions 1.20.1–1.20.4 use item NBT; 1.20.5 and newer use data components.

Compatibility claims for this release will be limited to the exact JARs and Minecraft versions that pass the retained client/server checks. The 1.0.3 Modrinth release remains available.
