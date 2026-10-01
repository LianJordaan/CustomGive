package io.github.lianjordaan;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.StringNbtReader;

/** Apply the pre-1.20.5 item NBT tag without changing its meaning. */
final class LegacyStackFactory {
    private LegacyStackFactory() {
    }

    static ItemStack fromClipboard(String clipboard, Item item, int amount) throws CommandSyntaxException {
        NbtCompound nbt = StringNbtReader.parse(clipboard);
        ItemStack stack = new ItemStack(item, amount);
        stack.setNbt(nbt);
        return stack;
    }
}
