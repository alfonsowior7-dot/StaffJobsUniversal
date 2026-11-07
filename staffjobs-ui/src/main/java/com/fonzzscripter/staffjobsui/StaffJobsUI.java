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
                "key.staffjobsui.open_dashboard", InputUtil.Type.KEYSYM,
                GLFW.GLFW_KEY_RIGHT_ALT, "category.staffjobsui"));

        ClientPlayNetworking.registerGlobalReceiver(CHANNEL, (client, handler, buf, responseSender) -> {
            String status = buf.readString(64);
            if ("WELCOME".equals(status)) {
                int protocol = Integer.parseInt(buf.readString(16));
                String pluginVersion = buf.readString(64);
                buf.readString(64); // authenticated player UUID
                client.execute(() -> {
                    handshakeAccepted = protocol == 1;
                    if (client.player != null && handshakeAccepted) {
                        client.setScreen(new DashboardScreen());
                        requestOverview();
                        client.player.sendMessage(Text.literal("StaffJobs UI connected (plugin " + pluginVersion + ")."), false);
                    } else if (client.player != null) {
                        client.player.sendMessage(Text.literal("StaffJobs UI protocol mismatch."), false);
                    }
                });
            } else if ("OVERVIEW".equals(status)) {
                int online = Integer.parseInt(buf.readString(16));
                int max = Integer.parseInt(buf.readString(16));
                int staff = Integer.parseInt(buf.readString(16));
                client.execute(() -> DashboardScreen.updateOverview(online, max, staff));
            } else if ("PLAYERS".equals(status)) {
                int count = Integer.parseInt(buf.readString(8));
                if (count < 0 || count > 100) return;
                java.util.List<String> names = new java.util.ArrayList<>();
                for (int i = 0; i < count; i++) names.add(buf.readString(64));
                client.execute(() -> DashboardScreen.updatePlayers(names));
            } else if ("REPORTS".equals(status)) {
                int count = Integer.parseInt(buf.readString(8));
                if (count < 0 || count > 20) return;
                java.util.List<String[]> reports = new java.util.ArrayList<>();
                for (int i = 0; i < count; i++) {
                    reports.add(new String[]{buf.readString(96), buf.readString(32), buf.readString(64),
                            buf.readString(24), buf.readString(40), buf.readString(300)});
                }
                client.execute(() -> DashboardScreen.updateReports(reports));
            } else if ("REPORT_ACTION_RESULT".equals(status)) {
                String result = buf.readString(24);
                String message = buf.readString(160);
                client.execute(() -> DashboardScreen.updateReportActionResult(result, message));
            } else if ("SECTION_DENIED".equals(status)) {
                String section = buf.readString(32);
                String reason = buf.readString(128);
                client.execute(() -> DashboardScreen.updateSectionDenied(section, reason));
            } else if ("PLAYER_PROFILE".equals(status)) {
                String name = buf.readString(64);
                String gameMode = buf.readString(32);
                String world = buf.readString(128);
                int ping = Integer.parseInt(buf.readString(16));
                client.execute(() -> DashboardScreen.updatePlayerProfile(name, gameMode, world, ping));
            } else if ("PROFILE_ERROR".equals(status)) {
                String reason = buf.readString(128);
                client.execute(() -> DashboardScreen.updatePlayerProfileError(reason));
            } else if ("DENIED".equals(status)) {
                String reason = buf.readString(256);
                client.execute(() -> {
                    handshakeAccepted = false;
                    if (client.player != null) client.player.sendMessage(Text.literal("StaffJobs access denied: " + reason), false);
                });
            }
        });

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            while (openDashboardKey.wasPressed()) {
                if (client.player == null || client.getNetworkHandler() == null) continue;
                if (!ClientPlayNetworking.canSend(CHANNEL)) {
                    client.player.sendMessage(Text.literal("This server does not provide StaffJobs UI."), false);
                    continue;
                }
                handshakeAccepted = false;
                DashboardScreen.clearOverview();
                net.minecraft.network.PacketByteBuf payload = net.fabricmc.fabric.api.networking.v1.PacketByteBufs.create();
                payload.writeString("HELLO");
                ClientPlayNetworking.send(CHANNEL, payload);
                client.player.sendMessage(Text.literal("Connecting to StaffJobs..."), false);
            }
        });
    }

    private static void requestOverview() {
        if (!handshakeAccepted || !ClientPlayNetworking.canSend(CHANNEL)) return;
        net.minecraft.network.PacketByteBuf payload = net.fabricmc.fabric.api.networking.v1.PacketByteBufs.create();
        payload.writeString("GET_OVERVIEW");
        ClientPlayNetworking.send(CHANNEL, payload);
    }

    public static void requestReports() {
        if (!handshakeAccepted || !ClientPlayNetworking.canSend(CHANNEL)) return;
        net.minecraft.network.PacketByteBuf payload = net.fabricmc.fabric.api.networking.v1.PacketByteBufs.create();
        payload.writeString("GET_REPORTS");
        ClientPlayNetworking.send(CHANNEL, payload);
    }
    public static void performReportAction(String action, String reportId) {
        if (!handshakeAccepted || !ClientPlayNetworking.canSend(CHANNEL) || reportId == null) return;
        net.minecraft.network.PacketByteBuf payload = net.fabricmc.fabric.api.networking.v1.PacketByteBufs.create();
        payload.writeString("REPORT_ACTION");
        payload.writeString(action);
        payload.writeString(reportId);
        ClientPlayNetworking.send(CHANNEL, payload);
    }
    public static void requestPlayers() {
        if (!handshakeAccepted || !ClientPlayNetworking.canSend(CHANNEL)) return;
        net.minecraft.network.PacketByteBuf payload = net.fabricmc.fabric.api.networking.v1.PacketByteBufs.create();
        payload.writeString("GET_PLAYERS");
        ClientPlayNetworking.send(CHANNEL, payload);
    }

    public static void requestPlayerProfile(String playerName) {
        if (!handshakeAccepted || !ClientPlayNetworking.canSend(CHANNEL)) return;
        net.minecraft.network.PacketByteBuf payload = net.fabricmc.fabric.api.networking.v1.PacketByteBufs.create();
        payload.writeString("GET_PLAYER_PROFILE");
        payload.writeString(playerName);
        ClientPlayNetworking.send(CHANNEL, payload);
    }

    public static boolean isHandshakeAccepted() { return handshakeAccepted; }
}
