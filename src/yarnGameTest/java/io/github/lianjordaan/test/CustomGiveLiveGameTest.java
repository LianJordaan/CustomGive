package io.github.lianjordaan.test;

import java.nio.file.Files;
import java.security.MessageDigest;
import java.util.HexFormat;

import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.minecraft.client.gui.screen.multiplayer.ConnectScreen;
import net.minecraft.client.gui.screen.TitleScreen;
import net.minecraft.client.network.ServerAddress;
import net.minecraft.client.network.ServerInfo;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;

/** Private game test for the exact packaged 1.21.11 production mod. */
@SuppressWarnings("UnstableApiUsage")
public final class CustomGiveLiveGameTest implements FabricClientGameTest {
    @Override
    public void runTest(ClientGameTestContext context) {
        verifyProductionJar();
        String address = System.getProperty("customgive.test.address", "127.0.0.1:27210");
        context.runOnClient(client -> ConnectScreen.connect(null, client,
                ServerAddress.parse(address),
                new ServerInfo("CustomGive private test", address, ServerInfo.ServerType.OTHER),
                false, null));
        context.waitFor(client -> client.player != null && client.world != null, 1200);
        String phase = System.getProperty("customgive.test.phase", "creative");
        if ("creative".equals(phase)) {
            testCreative(context);
        } else if ("verify_survival".equals(phase)) {
            testPersistenceAndSurvival(context);
        } else {
            throw new AssertionError("Unknown test phase: " + phase);
        }
    }

    private static void testCreative(ClientGameTestContext context) {
        context.waitFor(client -> client.player.getAbilities().creativeMode, 200);
        context.runOnClient(client -> {
            if (!client.player.getInventory().getStack(0).isEmpty()) {
                throw new AssertionError("The first hotbar slot was not empty at test start");
            }
            client.keyboard.setClipboard("{}");
            client.player.networkHandler.sendChatCommand("customgive");
        });
        context.waitFor(client -> client.player != null &&
                client.player.getInventory().getStack(0).isOf(Items.STONE) &&
                client.player.getInventory().getStack(0).getCount() == 1, 200);
        context.waitTicks(40);
        context.runOnClient(client -> {
            if (!client.player.getInventory().getStack(0).isOf(Items.STONE)) {
                throw new AssertionError("Stone vanished after the server acknowledged Creative inventory");
            }
            client.keyboard.setClipboard("{\"minecraft:custom_data\":{customgive_test:1b}}");
            client.player.networkHandler.sendChatCommand("customgive minecraft:diamond 2");
        });
        context.waitFor(client -> client.player != null &&
                client.player.getInventory().getStack(1).isOf(Items.DIAMOND) &&
                client.player.getInventory().getStack(1).getCount() == 2, 200);
        context.waitTicks(40);
        context.runOnClient(client -> {
            ItemStack diamond = client.player.getInventory().getStack(1);
            if (diamond.get(DataComponentTypes.CUSTOM_DATA) == null ||
                    diamond.get(DataComponentTypes.CUSTOM_DATA).copyNbt()
                            .getByte("customgive_test").orElse((byte) 0) != 1) {
                throw new AssertionError("Custom data was not kept by the server");
            }
        });
        context.takeScreenshot("customgive-creative-1.21.11");
        System.out.println("CUSTOMGIVE_LIVE creative PASS: stone 1 and custom-data diamond 2 survived server sync");
        context.runOnClient(client -> client.disconnect(new TitleScreen(), false));
        context.waitFor(client -> client.player == null, 200);
    }

    private static void testPersistenceAndSurvival(ClientGameTestContext context) {
        context.waitFor(client -> !client.player.getAbilities().creativeMode, 1200);
        context.runOnClient(client -> {
            ItemStack stone = client.player.getInventory().getStack(0);
            ItemStack diamond = client.player.getInventory().getStack(1);
            if (!stone.isOf(Items.STONE) || stone.getCount() != 1 ||
                    !diamond.isOf(Items.DIAMOND) || diamond.getCount() != 2) {
                throw new AssertionError("Creative items did not persist across logout/login");
            }
            client.keyboard.setClipboard("{}");
            client.player.networkHandler.sendChatCommand("customgive minecraft:emerald 1");
        });
        context.waitTicks(40);
        context.runOnClient(client -> {
            if (!client.player.getInventory().getStack(2).isEmpty()) {
                throw new AssertionError("Survival-mode command added an item");
            }
        });
        System.out.println("CUSTOMGIVE_LIVE verify_survival PASS: items persisted; command denied in Survival");
        context.runOnClient(client -> client.disconnect(new TitleScreen(), false));
        context.waitFor(client -> client.player == null, 200);
    }

    private static void verifyProductionJar() {
        try {
            var mod = FabricLoader.getInstance().getModContainer("lian-customgive").orElseThrow();
            var paths = mod.getOrigin().getPaths();
            if (paths.size() != 1 || !Files.isRegularFile(paths.getFirst())) {
                throw new AssertionError("CustomGive was not loaded from one packaged JAR: " + paths);
            }
            String actual = HexFormat.of().formatHex(MessageDigest.getInstance("SHA-512")
                    .digest(Files.readAllBytes(paths.getFirst())));
            String expected = System.getProperty("customgive.test.expected_sha512", "");
            if (expected.isEmpty() || !actual.equalsIgnoreCase(expected)) {
                throw new AssertionError("CustomGive JAR hash differs from the audited candidate: " + actual);
            }
            System.out.println("CUSTOMGIVE_LIVE loaded JAR: " + paths.getFirst() + " sha512=" + actual);
        } catch (Exception exception) {
            throw new AssertionError("Cannot verify the loaded CustomGive JAR", exception);
        }
    }
}
