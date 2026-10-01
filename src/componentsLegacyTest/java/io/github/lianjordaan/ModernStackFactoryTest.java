package io.github.lianjordaan;

import net.minecraft.Bootstrap;
import net.minecraft.SharedConstants;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.registry.DynamicRegistryManager;
import net.minecraft.util.Identifier;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;

class ModernStackFactoryTest {
    @BeforeAll
    static void bootstrapMinecraft() {
        SharedConstants.createGameVersion();
        Bootstrap.initialize();
    }

    @Test
    void emptyComponentsKeepTheRequestedStoneAndCount() throws Exception {
        ItemStack stack = ModernStackFactory.fromClipboard("{}", Identifier.of("minecraft", "stone"), 2,
                DynamicRegistryManager.EMPTY);
        assertNotNull(stack);
        assertSame(Items.STONE, stack.getItem());
        assertEquals(2, stack.getCount());
    }

    @Test
    void customDataSurvivesTheVanillaComponentCodec() throws Exception {
        ItemStack stack = ModernStackFactory.fromClipboard(
                "{components:{\"minecraft:custom_data\":{customgive_test:1b}}}",
                Identifier.of("minecraft", "stone"), 1, DynamicRegistryManager.EMPTY);
        assertNotNull(stack);
        assertNotNull(stack.get(DataComponentTypes.CUSTOM_DATA));
        assertEquals((byte) 1, stack.get(DataComponentTypes.CUSTOM_DATA)
                .copyNbt().getByte("customgive_test"));
    }
}
