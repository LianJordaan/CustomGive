package io.github.lianjordaan;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.command.v2.ClientCommands;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/** 26.x uses Mojang names and Java 25; the command remains client-side. */
public final class CustomGiveClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) -> {
            var command = ClientCommands.literal("customgive")
                    .executes(context -> give(null, 1));
            var item = ClientCommands.argument("item", StringArgumentType.word())
                    .suggests((context, builder) -> SharedSuggestionProvider.suggestResource(
                            BuiltInRegistries.ITEM.keySet(), builder))
                    .executes(context -> give(StringArgumentType.getString(context, "item"), 1));
            item.then(ClientCommands.argument("amount", IntegerArgumentType.integer(1, 64))
                    .executes(context -> give(
                            StringArgumentType.getString(context, "item"),
                            IntegerArgumentType.getInteger(context, "amount"))));
            command.then(item);
            dispatcher.register(command);
        });
    }

    private static int give(String itemInput, int amount) {
        Minecraft client = Minecraft.getInstance();
        LocalPlayer player = client.player;
        if (player == null || client.gameMode == null || client.level == null) {
            return 0;
        }
        final GiveRequest request;
        try {
            request = GiveRequest.of(itemInput, amount);
        } catch (IllegalArgumentException exception) {
            message(player, exception.getMessage());
            return 0;
        }
        if (!player.getAbilities().instabuild) {
            message(player, "CustomGive requires Creative mode. The server decides whether you can use it.");
            return 0;
        }
        Identifier id = Identifier.tryParse(request.itemId());
        if (id == null || !BuiltInRegistries.ITEM.containsKey(id)) {
            message(player, "Unknown item: " + request.itemId());
            return 0;
        }
        Item item = BuiltInRegistries.ITEM.getValue(id);
        if (item == Items.AIR) {
            message(player, "Air cannot be given as an item.");
            return 0;
        }
        int emptySlot = player.getInventory().getFreeSlot();
        if (emptySlot < 0) {
            message(player, "Your main inventory is full. Make one empty slot first.");
            return 0;
        }

        final String clipboard;
        try {
            clipboard = client.keyboardHandler.getClipboard();
            GiveRequest.validateClipboard(clipboard);
        } catch (RuntimeException exception) {
            message(player, "Could not read a clipboard SNBT compound (maximum 32,768 characters).");
            return 0;
        }
        final ItemStack stack;
        try {
            stack = MojangStackFactory.fromClipboard(clipboard, id, request.amount(),
                    client.level.registryAccess());
        } catch (CommandSyntaxException exception) {
            message(player, "Clipboard text is not a valid SNBT compound.");
            return 0;
        }
        if (stack == null || stack.isEmpty()) {
            message(player, "Clipboard components could not be applied to this item.");
            return 0;
        }
        if (request.amount() > stack.getMaxStackSize()) {
            message(player, "That item can stack to at most " + stack.getMaxStackSize() + ".");
            return 0;
        }
        client.gameMode.handleCreativeModeItemAdd(stack, GiveRequest.creativeSlot(emptySlot));
        message(player, "Sent " + request.amount() + " " + id + " with clipboard components to the server.");
        return 1;
    }

    private static void message(LocalPlayer player, String text) {
        player.sendSystemMessage(Component.literal(text));
    }
}
