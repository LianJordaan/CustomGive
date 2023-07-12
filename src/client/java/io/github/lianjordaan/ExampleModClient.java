package io.github.lianjordaan;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.StringNbtReader;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

import java.awt.*;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.awt.event.KeyEvent;
import java.io.IOException;

public class ExampleModClient implements ClientModInitializer {

	@Override
	public void onInitializeClient() {
		ClientCommandRegistrationCallback.EVENT.register((dispatcher, dedicated) -> {
			dispatcher.register(ClientCommandManager.literal("customgive")
					.executes(context -> {
						giveCustomItemFromClipboard();

						return 1;
					}));

		});
	}

	private void giveCustomItemFromClipboard() {
		MinecraftClient minecraftClient = MinecraftClient.getInstance();
		ClientPlayerEntity player = minecraftClient.player;
		if (player != null) {
			Thread clipboardThread = new Thread(() -> {
				String clipboard = MinecraftClient.getInstance().keyboard.getClipboard();
				ItemStack itemStack = createItemStackFromNBT(clipboard);
				if (itemStack != null) {
					minecraftClient.execute(() -> {
						player.giveItemStack(itemStack);
						player.sendMessage(Text.of("Custom item given!"));
					});
				} else {
					minecraftClient.execute(() -> {
						player.sendMessage(Text.of("Invalid NBT data."));
					});
				}
			});
			clipboardThread.start();
		}
	}

	private ItemStack createItemStackFromNBT(String nbtData) {
		try {
			NbtCompound nbt = StringNbtReader.parse(nbtData);
			ItemStack itemStack = new ItemStack(Items.STONE);
			itemStack.setNbt(nbt);
			return itemStack;
		} catch (Exception e) {
			return null;
		}
	}
}