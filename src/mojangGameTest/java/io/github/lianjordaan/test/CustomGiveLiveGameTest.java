package io.github.lianjordaan.test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.HexFormat;

import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.minecraft.client.gui.screens.ConnectScreen;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.client.multiplayer.resolver.ServerAddress;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/** Private production-JAR smoke test against an app-created remote server. */
@SuppressWarnings("UnstableApiUsage")
public final class CustomGiveLiveGameTest implements FabricClientGameTest {
    @Override
    public void runTest(ClientGameTestContext context) {
        verifyProductionJar();
        String address = System.getProperty("customgive.test.address", "127.0.0.1:27210");
        context.runOnClient(client -> ConnectScreen.startConnecting(null, client,
                ServerAddress.parseString(address),
                new ServerData("CustomGive private test", address, ServerData.Type.OTHER),
                false, null));
        context.waitFor(client -> client.player != null && client.level != null, 1200);
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
        context.waitFor(client -> client.player.getAbilities().instabuild, 200);
        context.runOnClient(client -> {
            if (!client.player.getInventory().getItem(0).isEmpty()) {
                throw new AssertionError("The first hotbar slot was not empty at test start");
            }
            client.keyboardHandler.setClipboard("{}");
            client.player.connection.sendCommand("customgive");
        });
        context.waitTicks(20);
        context.runOnClient(client -> {
            for (int slot = 0; slot < 36; slot++) {
                ItemStack item = client.player.getInventory().getItem(slot);
                if (!item.isEmpty()) {
                    System.out.println("CUSTOMGIVE_LIVE slot=" + slot + " item=" + item +
                            " count=" + item.getCount());
                }
            }
        });
        context.waitFor(client -> client.player != null &&
                client.player.getInventory().getItem(0).is(Items.STONE) &&
                client.player.getInventory().getItem(0).getCount() == 1, 200);
        context.waitTicks(40);
        context.runOnClient(client -> {
            if (!client.player.getInventory().getItem(0).is(Items.STONE)) {
                throw new AssertionError("Stone vanished after the server acknowledged Creative inventory");
            }
            client.keyboardHandler.setClipboard("{\"minecraft:custom_data\":{customgive_test:1b}}");
            client.player.connection.sendCommand("customgive minecraft:diamond 2");
        });
        context.waitFor(client -> client.player != null &&
                client.player.getInventory().getItem(1).is(Items.DIAMOND) &&
                client.player.getInventory().getItem(1).getCount() == 2, 200);
        context.waitTicks(40);
        context.runOnClient(client -> {
            ItemStack diamond = client.player.getInventory().getItem(1);
            if (diamond.get(DataComponents.CUSTOM_DATA) == null ||
                    diamond.get(DataComponents.CUSTOM_DATA).copyTag()
                            .getByte("customgive_test").orElse((byte) 0) != 1) {
                throw new AssertionError("Custom data was not kept by the server");
            }
        });
        context.takeScreenshot("customgive-creative-" +
                System.getProperty("customgive.test.minecraft", "unknown"));
        System.out.println("CUSTOMGIVE_LIVE creative PASS: stone 1 and custom-data diamond 2 survived server sync");
        context.runOnClient(client -> client.disconnect(new TitleScreen(), false));
        context.waitFor(client -> client.player == null, 200);
    }

    private static void testPersistenceAndSurvival(ClientGameTestContext context) {
        context.waitFor(client -> !client.player.getAbilities().instabuild, 1200);
        context.runOnClient(client -> {
            ItemStack stone = client.player.getInventory().getItem(0);
            ItemStack diamond = client.player.getInventory().getItem(1);
            if (!stone.is(Items.STONE) || stone.getCount() != 1 ||
                    !diamond.is(Items.DIAMOND) || diamond.getCount() != 2) {
                throw new AssertionError("Creative items did not persist across logout/login");
            }
            client.keyboardHandler.setClipboard("{}");
            client.player.connection.sendCommand("customgive minecraft:emerald 1");
        });
        context.waitTicks(40);
        context.runOnClient(client -> {
            if (!client.player.getInventory().getItem(2).isEmpty()) {
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
            Path path = paths.getFirst();
            String actual = HexFormat.of().formatHex(MessageDigest.getInstance("SHA-512")
                    .digest(Files.readAllBytes(path)));
            String expected = System.getProperty("customgive.test.expected_sha512", "");
            if (expected.isEmpty() || !actual.equalsIgnoreCase(expected)) {
                throw new AssertionError("CustomGive production JAR hash differs from the audited candidate: " + actual);
            }
            System.out.println("CUSTOMGIVE_LIVE loaded JAR: " + path + " sha512=" + actual);
        } catch (Exception exception) {
            throw new AssertionError("Cannot verify the loaded CustomGive production JAR", exception);
        }
    }
}
