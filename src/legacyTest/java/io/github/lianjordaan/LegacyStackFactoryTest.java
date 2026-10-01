package io.github.lianjordaan;

import net.minecraft.Bootstrap;
import net.minecraft.SharedConstants;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class LegacyStackFactoryTest {
    @BeforeAll
    static void bootstrapMinecraft() {
        SharedConstants.createGameVersion();
        Bootstrap.initialize();
    }

    @Test
    void copiedTagIsAppliedToTheRequestedItemAndCount() throws Exception {
        ItemStack stack = LegacyStackFactory.fromClipboard("{CustomModelData:7}", Items.STONE, 2);
        assertEquals(Items.STONE, stack.getItem());
        assertEquals(2, stack.getCount());
        assertEquals(7, stack.getNbt().getInt("CustomModelData"));
    }

    @Test
    void malformedCompoundFailsBeforeAnyInventoryPacket() {
        assertThrows(Exception.class, () -> LegacyStackFactory.fromClipboard("not SNBT", Items.STONE, 1));
    }
}
