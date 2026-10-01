package io.github.lianjordaan;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class GiveRequestTest {
    @Test
    void noArgumentsPreserveStoneAndOneItem() {
        GiveRequest request = GiveRequest.of(null, 1);
        assertEquals("minecraft:stone", request.itemId());
        assertEquals(1, request.amount());
    }

    @Test
    void ordinaryNamesAndNamespacedIdsAreAccepted() {
        assertEquals("diamond", GiveRequest.of("  DIAMOND ", 2).itemId());
        assertEquals("example:custom_item", GiveRequest.of("EXAMPLE:Custom_Item", 1).itemId());
        assertThrows(IllegalArgumentException.class, () -> GiveRequest.of("stone", 0));
        assertThrows(IllegalArgumentException.class, () -> GiveRequest.of("stone", 65));
    }

    @Test
    void emptySlotMapsToTheActualCreativeContainerSlot() {
        assertEquals(36, GiveRequest.creativeSlot(0));
        assertEquals(44, GiveRequest.creativeSlot(8));
        assertEquals(9, GiveRequest.creativeSlot(9));
        assertEquals(35, GiveRequest.creativeSlot(35));
        assertThrows(IllegalArgumentException.class, () -> GiveRequest.creativeSlot(-1));
        assertThrows(IllegalArgumentException.class, () -> GiveRequest.creativeSlot(36));
    }

    @Test
    void clipboardMustBeAReasonablyBoundedSnBtInput() {
        assertDoesNotThrow(() -> GiveRequest.validateClipboard("{}"));
        assertThrows(IllegalArgumentException.class, () -> GiveRequest.validateClipboard(null));
        assertThrows(IllegalArgumentException.class, () -> GiveRequest.validateClipboard("  "));
        assertThrows(IllegalArgumentException.class,
                () -> GiveRequest.validateClipboard("a".repeat(GiveRequest.MAX_CLIPBOARD_CHARS + 1)));
    }
}
