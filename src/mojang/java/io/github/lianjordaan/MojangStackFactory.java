package io.github.lianjordaan;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.TagParser;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.RegistryOps;
import net.minecraft.world.item.ItemStack;

/** Apply data components through Minecraft's registry-aware item codec. */
final class MojangStackFactory {
    private MojangStackFactory() {
    }

    static ItemStack fromClipboard(String clipboard, Identifier id, int amount,
                                   HolderLookup.Provider registries) throws CommandSyntaxException {
        CompoundTag input = TagParser.parseCompoundFully(clipboard);
        CompoundTag components = input.getCompound("components").orElse(input);
        CompoundTag encoded = new CompoundTag();
        encoded.putString("id", id.toString());
        encoded.putInt("count", amount);
        encoded.put("components", components);
        return ItemStack.CODEC.parse(RegistryOps.create(NbtOps.INSTANCE, registries), encoded)
                .result().orElse(null);
    }
}
