package io.github.lianjordaan.test;

import java.nio.file.Files;
import java.security.MessageDigest;
import java.util.HexFormat;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.MinecraftClient;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.text.Text;

/** Test-only tick state machine for older Fabric releases without client game tests. */
public final class CustomGiveLegacyLiveTest implements ClientModInitializer {
    private int ticks;
    private int actionAt;
    private int step;
    private boolean started;
    private boolean connecting;

    @Override
    public void onInitializeClient() {
        verifyProductionJar();
        ClientTickEvents.END_CLIENT_TICK.register(this::tick);
    }

    private void tick(MinecraftClient client) {
        ticks++;
        if (ticks > 2400) {
            throw new AssertionError("CustomGive live test timed out; connected=" +
                    (client.player != null));
        }
        if (client.player == null || client.world == null || client.interactionManager == null) {
            if (!connecting && ticks >= 200) {
                connecting = true;
                System.out.println("CUSTOMGIVE_LIVE launch screen=" +
                        (client.currentScreen == null ? "none" : client.currentScreen.getClass().getName()));
                LiveConnect.connect(client);
            } else if (connecting && ticks % 100 == 0) {
                System.out.println("CUSTOMGIVE_LIVE connection screen=" +
                        (client.currentScreen == null ? "none" : client.currentScreen.getClass().getName()) +
                        " title=" + (client.currentScreen == null ? "" :
                                client.currentScreen.getTitle().getString()));
                if (client.currentScreen != null) {
                    for (var field : client.currentScreen.getClass().getDeclaredFields()) {
                        if (!Text.class.isAssignableFrom(field.getType())) continue;
                        try {
                            field.setAccessible(true);
                            Text value = (Text) field.get(client.currentScreen);
                            if (value != null) {
                                System.out.println("CUSTOMGIVE_LIVE screen text=" + value.getString());
                            }
                        } catch (ReflectiveOperationException ignored) {
                            // Screen fields are only diagnostic for the private test.
                        }
                    }
                }
            }
            return;
        }
        if (!started) {
            started = true;
            System.out.println("CUSTOMGIVE_LIVE joined server as " + client.player.getName().getString());
        }
        String phase = System.getProperty("customgive.test.phase", "creative");
        if ("creative".equals(phase)) {
            creative(client);
        } else if ("verify_survival".equals(phase)) {
            survival(client);
        } else {
            throw new AssertionError("Unknown test phase: " + phase);
        }
    }

    private void creative(MinecraftClient client) {
        if (!client.player.getAbilities().creativeMode) return;
        if (step == 0) {
            if (!client.player.getInventory().getStack(0).isEmpty()) {
                throw new AssertionError("Initial hotbar slot was not empty");
            }
            client.keyboard.setClipboard("{}");
            client.player.networkHandler.sendChatCommand("customgive");
            actionAt = ticks;
            step = 1;
        } else if (step == 1 && ticks - actionAt >= 40) {
            ItemStack stone = client.player.getInventory().getStack(0);
            if (!stone.isOf(Items.STONE) || stone.getCount() != 1) {
                throw new AssertionError("No-argument stone was missing after server sync: " + stone);
            }
            client.keyboard.setClipboard("{\"minecraft:custom_data\":{customgive_test:1b}}");
            client.player.networkHandler.sendChatCommand("customgive minecraft:diamond 2");
            actionAt = ticks;
            step = 2;
        } else if (step == 2 && ticks - actionAt >= 40) {
            ItemStack diamond = client.player.getInventory().getStack(1);
            if (!diamond.isOf(Items.DIAMOND) || diamond.getCount() != 2 ||
                    diamond.get(DataComponentTypes.CUSTOM_DATA) == null ||
                    diamond.get(DataComponentTypes.CUSTOM_DATA).copyNbt()
                            .getByte("customgive_test") != 1) {
                throw new AssertionError("Namespaced diamond and clipboard components were not kept: " + diamond);
            }
            System.out.println("CUSTOMGIVE_LIVE creative PASS: stone 1 and custom-data diamond 2 survived server sync");
            client.disconnect();
            client.scheduleStop();
            step = 3;
        }
    }

    private void survival(MinecraftClient client) {
        if (client.player.getAbilities().creativeMode) {
            if (ticks % 100 == 0) System.out.println("CUSTOMGIVE_LIVE waiting for Survival gamemode");
            return;
        }
        if (step == 0) {
            ItemStack stone = client.player.getInventory().getStack(0);
            ItemStack diamond = client.player.getInventory().getStack(1);
            if (!stone.isOf(Items.STONE) || stone.getCount() != 1 ||
                    !diamond.isOf(Items.DIAMOND) || diamond.getCount() != 2) {
                throw new AssertionError("Creative inventory did not persist across relog");
            }
            client.keyboard.setClipboard("{}");
            client.player.networkHandler.sendChatCommand("customgive minecraft:emerald 1");
            actionAt = ticks;
            step = 1;
        } else if (step == 1 && ticks - actionAt >= 40) {
            if (!client.player.getInventory().getStack(2).isEmpty()) {
                throw new AssertionError("Survival command added an item");
            }
            System.out.println("CUSTOMGIVE_LIVE verify_survival PASS: inventory persisted; command denied");
            client.disconnect();
            client.scheduleStop();
            step = 2;
        }
    }

    private static void verifyProductionJar() {
        try {
            var mod = FabricLoader.getInstance().getModContainer("lian-customgive").orElseThrow();
            var paths = mod.getOrigin().getPaths();
            if (paths.size() != 1 || !Files.isRegularFile(paths.get(0))) {
                throw new AssertionError("CustomGive was not loaded from one packaged JAR: " + paths);
            }
            String actual = HexFormat.of().formatHex(MessageDigest.getInstance("SHA-512")
                    .digest(Files.readAllBytes(paths.get(0))));
            String expected = System.getProperty("customgive.test.expected_sha512", "");
            if (expected.isEmpty() || !actual.equalsIgnoreCase(expected)) {
                throw new AssertionError("CustomGive JAR hash differs from audited candidate: " + actual);
            }
            System.out.println("CUSTOMGIVE_LIVE loaded JAR: " + paths.get(0) + " sha512=" + actual);
        } catch (Exception exception) {
            throw new AssertionError("Cannot verify CustomGive JAR", exception);
        }
    }
}
