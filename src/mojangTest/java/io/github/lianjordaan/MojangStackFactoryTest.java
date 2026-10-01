package io.github.lianjordaan;

import net.minecraft.SharedConstants;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.core.component.DataComponents;
import net.minecraft.resources.Identifier;
import net.minecraft.server.Bootstrap;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;

class MojangStackFactoryTest {
    @BeforeAll
    static void bootstrapMinecraft() {
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();
        // 26.2 binds item prototypes during datapack loading, which a plain
        // JUnit bootstrap does not run. Bind this test item's vanilla stack
        // limit so the production item codec can be exercised in isolation.
        Items.STONE.builtInRegistryHolder().bindComponents(DataComponentMap.builder()
                .set(DataComponents.MAX_STACK_SIZE, 64).build());
    }

    @Test
    void emptyComponentsKeepTheRequestedStoneAndCount() throws Exception {
        ItemStack stack = MojangStackFactory.fromClipboard("{}", Identifier.parse("minecraft:stone"), 2,
                RegistryAccess.EMPTY);
        assertNotNull(stack);
        assertSame(Items.STONE, stack.getItem());
        assertEquals(2, stack.getCount());
    }

    @Test
    void customDataSurvivesTheVanillaComponentCodec() throws Exception {
        ItemStack stack = MojangStackFactory.fromClipboard(
                "{components:{\"minecraft:custom_data\":{customgive_test:1b}}}",
                Identifier.parse("minecraft:stone"), 1, RegistryAccess.EMPTY);
        assertNotNull(stack);
        assertNotNull(stack.get(DataComponents.CUSTOM_DATA));
        assertEquals((byte) 1, stack.get(DataComponents.CUSTOM_DATA)
                .copyTag().getByte("customgive_test").orElseThrow());
    }
}
