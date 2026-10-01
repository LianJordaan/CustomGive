package io.github.lianjordaan;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.StringNbtReader;
import net.minecraft.registry.RegistryOps;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.util.Identifier;

/** Parse data components using Minecraft's own registry-aware item codec. */
final class ModernStackFactory {
    private ModernStackFactory() {
    }

    static ItemStack fromClipboard(String clipboard, Identifier id, int amount,
                                   RegistryWrapper.WrapperLookup registries) throws CommandSyntaxException {
        NbtCompound input = StringNbtReader.readCompound(clipboard);
        // Accept a components compound or a serialized stack's {components:{...}}
        // wrapper. The command's explicit item and amount always win.
        NbtCompound components = input.getCompound("components").orElse(input);
        NbtCompound encoded = new NbtCompound();
        encoded.putString("id", id.toString());
        encoded.putInt("count", amount);
        encoded.put("components", components);
        return ItemStack.CODEC.parse(RegistryOps.of(NbtOps.INSTANCE, registries), encoded)
                .result().orElse(null);
    }
}
