package com.fonzzscripter.staffjobsui;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import org.lwjgl.glfw.GLFW;

public final class StaffJobsUI implements ClientModInitializer {
    public static final String MOD_ID = "staffjobsui";
    public static final Identifier CHANNEL = new Identifier("staffjobs", "ui");
    private static KeyBinding openDashboardKey;
    private static boolean handshakeAccepted;

    @Override
    public void onInitializeClient() {
        openDashboardKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.staffjobsui.open_dashboard",
                InputUtil.Type.KEYSYM,
                GLFW.GLFW_KEY_RIGHT_ALT,
                "category.staffjobsui"
        ));

        ClientPlayNetworking.registerGlobalReceiver(CHANNEL, (client, handler, buf, responseSender) -> {
            String status = buf.readString(64);
            if ("WELCOME".equals(status)) {
                int protocol = Integer.parseInt(buf.readString(16));
                String pluginVersion = buf.readString(64);
                String playerId = buf.readString(64);
                client.execute(() -> {
                    handshakeAccepted = protocol == 1;
                    if (client.player != null && handshakeAccepted) {
                        client.setScreen(new DashboardScreen());
                    }
                    if (client.player != null) {
                        client.player.sendMessage(Text.literal(handshakeAccepted
                                ? "StaffJobs UI connected (plugin " + pluginVersion + ")."
                                : "StaffJobs UI protocol mismatch."), false);
                    }
                });
            } else if ("DENIED".equals(status)) {
                String reason = buf.readString(256);
                client.execute(() -> {
                    handshakeAccepted = false;
                    if (client.player != null) {
                        client.player.sendMessage(Text.literal("StaffJobs access denied: " + reason), false);
                    }
                });
            }
        });

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            while (openDashboardKey.wasPressed()) {
                if (client.player == null || client.getNetworkHandler() == null) {
                    continue;
                }
                if (!ClientPlayNetworking.canSend(CHANNEL)) {
                    client.player.sendMessage(Text.literal("This server does not provide StaffJobs UI."), false);
                    continue;
                }
                handshakeAccepted = false;
                net.minecraft.network.PacketByteBuf payload =
                        net.fabricmc.fabric.api.networking.v1.PacketByteBufs.create();
                payload.writeString("HELLO");
                ClientPlayNetworking.send(CHANNEL, payload);
                client.player.sendMessage(Text.literal("Connecting to StaffJobs..."), false);
            }
        });
    }

    public static boolean isHandshakeAccepted() {
        return handshakeAccepted;
    }
}
