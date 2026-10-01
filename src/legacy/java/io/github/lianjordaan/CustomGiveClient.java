package io.github.lianjordaan;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.command.CommandSource;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.registry.Registries;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

/** Client-only command. The server validates the resulting creative inventory packet. */
public final class CustomGiveClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) -> {
            var command = ClientCommandManager.literal("customgive")
                    .executes(context -> give(null, 1));
            var item = ClientCommandManager.argument("item", StringArgumentType.word())
                    .suggests((context, builder) -> CommandSource.suggestIdentifiers(Registries.ITEM.getIds(), builder))
                    .executes(context -> give(StringArgumentType.getString(context, "item"), 1));
            item.then(ClientCommandManager.argument("amount", IntegerArgumentType.integer(1, 64))
                    .executes(context -> give(
                            StringArgumentType.getString(context, "item"),
                            IntegerArgumentType.getInteger(context, "amount"))));
            command.then(item);
            dispatcher.register(command);
        });
    }

    private static int give(String itemInput, int amount) {
        MinecraftClient client = MinecraftClient.getInstance();
        ClientPlayerEntity player = client.player;
        if (player == null || client.interactionManager == null) {
            return 0;
        }
        final GiveRequest request;
        try {
            request = GiveRequest.of(itemInput, amount);
        } catch (IllegalArgumentException exception) {
            message(player, exception.getMessage());
            return 0;
        }
        if (!player.getAbilities().creativeMode) {
            message(player, "CustomGive requires Creative mode. The server decides whether you can use it.");
            return 0;
        }
        Identifier id = Identifier.tryParse(request.itemId());
        if (id == null || !Registries.ITEM.containsId(id)) {
            message(player, "Unknown item: " + request.itemId());
            return 0;
        }
        Item item = Registries.ITEM.get(id);
        if (item == Items.AIR) {
            message(player, "Air cannot be given as an item.");
            return 0;
        }
        int emptySlot = player.getInventory().getEmptySlot();
        if (emptySlot < 0) {
            message(player, "Your main inventory is full. Make one empty slot first.");
            return 0;
        }

        final String clipboard;
        try {
            clipboard = client.keyboard.getClipboard();
            GiveRequest.validateClipboard(clipboard);
        } catch (RuntimeException exception) {
            message(player, "Could not read a clipboard SNBT compound (maximum 32,768 characters).");
            return 0;
        }
        final ItemStack stack;
        try {
            stack = LegacyStackFactory.fromClipboard(clipboard, item, request.amount());
        } catch (CommandSyntaxException exception) {
            message(player, "Clipboard text is not a valid SNBT compound. Try {CustomModelData:7}.");
            return 0;
        }
        if (request.amount() > stack.getMaxCount()) {
            message(player, "That item can stack to at most " + stack.getMaxCount() + ".");
            return 0;
        }
        // A creative-inventory packet is handled by the server; changing only the
        // client inventory would create a ghost item and bypass server authority.
        client.interactionManager.clickCreativeStack(stack, GiveRequest.creativeSlot(emptySlot));
        message(player, "Sent " + request.amount() + " " + id + " with clipboard NBT to the server.");
        return 1;
    }

    private static void message(ClientPlayerEntity player, String text) {
        player.sendMessage(Text.literal(text), false);
    }
}
