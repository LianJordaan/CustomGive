package io.github.lianjordaan;

import java.util.Locale;

/** Minecraft-independent input and slot rules shared by the command and tests. */
record GiveRequest(String itemId, int amount) {
    static final int MAX_CLIPBOARD_CHARS = 32_768;

    static GiveRequest of(String item, int amount) {
        if (amount < 1 || amount > 64) {
            throw new IllegalArgumentException("Amount must be from 1 to 64");
        }
        String id = item == null || item.isBlank() ? "minecraft:stone" : item.trim().toLowerCase(Locale.ROOT);
        if (id.length() > 256) {
            throw new IllegalArgumentException("Item ID is too long");
        }
        return new GiveRequest(id, amount);
    }

    static void validateClipboard(String clipboard) {
        if (clipboard == null || clipboard.isBlank() || clipboard.length() > MAX_CLIPBOARD_CHARS) {
            throw new IllegalArgumentException("Clipboard is empty or exceeds the safe input limit");
        }
    }

    static int creativeSlot(int inventorySlot) {
        if (inventorySlot < 0 || inventorySlot >= 36) {
            throw new IllegalArgumentException("Only main inventory slots can receive a stack");
        }
        return inventorySlot < 9 ? 36 + inventorySlot : inventorySlot;
    }
}
