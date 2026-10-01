package io.github.lianjordaan.test;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;

import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.TitleScreen;
import net.minecraft.client.network.ServerAddress;
import net.minecraft.client.network.ServerInfo;

/** Private test launcher. Older Minecraft clients ignore the --server command-line option. */
final class LiveConnect {
    private LiveConnect() {}

    static void connect(MinecraftClient client) {
        try {
            String address = System.getProperty("customgive.test.address", "127.0.0.1:27210");
            int separator = address.lastIndexOf(':');
            if (separator <= 0 || separator == address.length() - 1) {
                throw new IllegalArgumentException("Expected host:port, got " + address);
            }
            ServerAddress endpoint = new ServerAddress(address.substring(0, separator),
                    Integer.parseInt(address.substring(separator + 1)));
            ServerInfo server = serverInfo(address);
            Class<?> connectScreen = connectScreen();
            for (Method method : connectScreen.getMethods()) {
                Class<?>[] types = method.getParameterTypes();
                if (!Modifier.isStatic(method.getModifiers()) || method.getReturnType() != void.class ||
                        (types.length != 5 && types.length != 6) ||
                        types[0] != Screen.class || types[1] != MinecraftClient.class ||
                        types[2] != ServerAddress.class || types[3] != ServerInfo.class ||
                        types[4] != boolean.class) continue;
                Object[] args = new Object[types.length];
                args[0] = client.currentScreen != null ? client.currentScreen : new TitleScreen();
                args[1] = client;
                args[2] = endpoint;
                args[3] = server;
                args[4] = false;
                if (types.length == 6) {
                    // CookieStorage is only supplied for a transfer from another server.
                    // A fresh direct connection must pass null or Minecraft reports a
                    // misleading transfer failure when the socket cannot be opened.
                    args[5] = null;
                }
                method.invoke(null, args);
                System.out.println("CUSTOMGIVE_LIVE connecting to " + address);
                return;
            }
            throw new NoSuchMethodException("ConnectScreen.connect for " + address);
        } catch (ReflectiveOperationException exception) {
            throw new AssertionError("Could not start private multiplayer test", exception);
        }
    }

    private static ServerInfo serverInfo(String address) throws ReflectiveOperationException {
        for (Constructor<?> constructor : ServerInfo.class.getConstructors()) {
            Class<?>[] types = constructor.getParameterTypes();
            if (types.length != 3 || types[0] != String.class || types[1] != String.class) continue;
            Object type;
            if (types[2] == boolean.class) {
                type = false;
            } else if (types[2].isEnum() && types[2].getEnumConstants().length == 3) {
                // ServerType order in 1.20.2–1.21.4: LAN, REALM, OTHER.
                type = types[2].getEnumConstants()[2];
            } else {
                continue;
            }
            return (ServerInfo) constructor.newInstance("CustomGive test", address, type);
        }
        throw new NoSuchMethodException("ServerInfo(name, address, type)");
    }

    private static Class<?> connectScreen() throws ClassNotFoundException {
        for (String named : new String[] {
                "net.minecraft.client.gui.screen.ConnectScreen",
                "net.minecraft.client.gui.screen.multiplayer.ConnectScreen"}) {
            String runtime = FabricLoader.getInstance().getMappingResolver()
                    .mapClassName("named", named);
            try {
                return Class.forName(runtime);
            } catch (ClassNotFoundException ignored) {
                // The class moved packages after Minecraft 1.20.1.
            }
        }
        if ("intermediary".equals(FabricLoader.getInstance().getMappingResolver()
                .getCurrentRuntimeNamespace())) {
            // All pinned Yarn mappings from 1.20.1 through 1.21.4 use class_412.
            return Class.forName("net.minecraft.class_412");
        }
        throw new ClassNotFoundException("Minecraft ConnectScreen");
    }
}
