package io.github.lianjordaan;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.StringNbtReader;
import net.minecraft.registry.DefaultedRegistry;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.registry.*;
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
					.then(ClientCommandManager.argument("block_or_item", StringArgumentType.string())
							.then(ClientCommandManager.argument("amount", IntegerArgumentType.integer())
									.executes(context -> {
										String blockOrItem = StringArgumentType.getString(context, "block_or_item");
										int amount = IntegerArgumentType.getInteger(context, "amount");
										giveCustomItemFromClipboard(blockOrItem, amount);

										return 1;
									}))
					)
			);
		});
	}

	private void giveCustomItemFromClipboard(String blockOrItem, int amount) {
		MinecraftClient minecraftClient = MinecraftClient.getInstance();
		ClientPlayerEntity player = minecraftClient.player;
		if (player != null) {
			Thread clipboardThread = new Thread(() -> {
				String clipboard = MinecraftClient.getInstance().keyboard.getClipboard();
				ItemStack itemStack = createItemStackFromNBT(clipboard, blockOrItem, amount);
				if (itemStack != null) {
					minecraftClient.execute(() -> {
						player.giveItemStack(itemStack);
						player.sendMessage(Text.of("Custom item given!"));
					});
				} else {
					minecraftClient.execute(() -> {
						player.sendMessage(Text.of("Invalid NBT data or block/item name."));
					});
				}
			});
			clipboardThread.start();
		}
	}

	private ItemStack createItemStackFromNBT(String nbtData, String blockOrItem, int amount) {
		try {
			NbtCompound nbt = StringNbtReader.parse(nbtData);
			Item item = (blockOrItem != null) ? getItemByName(blockOrItem) : Items.STONE;
			ItemStack itemStack = new ItemStack(item, amount);
			itemStack.setNbt(nbt);
			return itemStack;
		} catch (Exception e) {
			return null;
		}
	}


	public static Item getItemByName(String itemName) {
		// Convert the item name to lowercase for case-insensitive matching
		itemName = itemName.toLowerCase();

		// Custom mapping of item names to item instances
		return switch (itemName) {
			case "stone" -> Items.STONE;
			case "cobblestone" -> Items.COBBLESTONE;
			case "diamond" -> Items.DIAMOND;
			case "iron_ingot" -> Items.IRON_INGOT;
			case "gold_ingot" -> Items.GOLD_INGOT;
			case "emerald" -> Items.EMERALD;
			case "apple" -> Items.APPLE;
			case "carrot" -> Items.CARROT;
			case "stick" -> Items.STICK;
			case "bucket" -> Items.BUCKET;
			case "water_bucket" -> Items.WATER_BUCKET;
			case "lava_bucket" -> Items.LAVA_BUCKET;
			case "milk_bucket" -> Items.MILK_BUCKET;
			case "wheat" -> Items.WHEAT;
			case "bread" -> Items.BREAD;
			case "egg" -> Items.EGG;
			case "sugar" -> Items.SUGAR;
			case "cake" -> Items.CAKE;
			case "redstone" -> Items.REDSTONE;
			case "glowstone_dust" -> Items.GLOWSTONE_DUST;
			case "flint" -> Items.FLINT;
			case "coal" -> Items.COAL;
			case "wooden_sword" -> Items.WOODEN_SWORD;
			case "stone_sword" -> Items.STONE_SWORD;
			case "iron_sword" -> Items.IRON_SWORD;
			case "diamond_sword" -> Items.DIAMOND_SWORD;
			case "wooden_pickaxe" -> Items.WOODEN_PICKAXE;
			case "stone_pickaxe" -> Items.STONE_PICKAXE;
			case "netherite_hoe" -> Items.NETHERITE_HOE;
			case "iron_pickaxe" -> Items.IRON_PICKAXE;
			case "diamond_pickaxe" -> Items.DIAMOND_PICKAXE;
			case "wooden_axe" -> Items.WOODEN_AXE;
			case "stone_axe" -> Items.STONE_AXE;
			case "iron_axe" -> Items.IRON_AXE;
			case "diamond_axe" -> Items.DIAMOND_AXE;
			case "bow" -> Items.BOW;
			case "arrow" -> Items.ARROW;
			case "fishing_rod" -> Items.FISHING_ROD;
			case "clock" -> Items.CLOCK;
			case "compass" -> Items.COMPASS;
			case "red_bed" -> Items.RED_BED;
			case "paper" -> Items.PAPER;
			case "shears" -> Items.SHEARS;
			// Add more items as needed
			default -> null; // Item not found
		};

	}
}
