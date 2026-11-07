package com.fonzzscripter.staffjobsui;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import org.lwjgl.glfw.GLFW;

public final class StaffJobsUI implements ClientModInitializer {
    public static final String MOD_ID = "staffjobsui";
    public static final Identifier CHANNEL = Identifier.of("staffjobs", "ui");
    private static KeyBinding openDashboardKey;
    private static boolean handshakeAccepted;

    @Override
    public void onInitializeClient() {
        openDashboardKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.staffjobsui.open_dashboard", InputUtil.Type.KEYSYM,
                GLFW.GLFW_KEY_RIGHT_ALT, KeyBinding.Category.create(Identifier.of(MOD_ID, "general"))));

        PayloadTypeRegistry.playC2S().register(StaffJobsPayload.ID, StaffJobsPayload.CODEC);
        PayloadTypeRegistry.playS2C().register(StaffJobsPayload.ID, StaffJobsPayload.CODEC);
        ClientPlayNetworking.registerGlobalReceiver(StaffJobsPayload.ID, (payload, context) -> {
            PacketByteBuf buf = PacketByteBufs.create();
            buf.writeBytes(payload.data());
            String status = buf.readString(64);
            if ("WELCOME".equals(status)) {
                int protocol = Integer.parseInt(buf.readString(16));
                String pluginVersion = buf.readString(64);
                buf.readString(64); // authenticated player UUID
                context.client().execute(() -> {
                    handshakeAccepted = protocol == 1;
                    if (context.client().player != null && handshakeAccepted) {
                        context.client().setScreen(new DashboardScreen());
                        requestOverview();
                        context.client().player.sendMessage(Text.literal("StaffJobs UI connected (plugin " + pluginVersion + ")."), false);
                    } else if (context.client().player != null) {
                        context.client().player.sendMessage(Text.literal("StaffJobs UI protocol mismatch."), false);
                    }
                });
            } else if ("OVERVIEW".equals(status)) {
                int online = Integer.parseInt(buf.readString(16));
                int max = Integer.parseInt(buf.readString(16));
                int staff = Integer.parseInt(buf.readString(16));
                context.client().execute(() -> DashboardScreen.updateOverview(online, max, staff));
            } else if ("PLAYERS".equals(status)) {
                int count = Integer.parseInt(buf.readString(8));
                if (count < 0 || count > 100) return;
                java.util.List<String> names = new java.util.ArrayList<>();
                for (int i = 0; i < count; i++) names.add(buf.readString(64));
                context.client().execute(() -> DashboardScreen.updatePlayers(names));
            } else if ("REPORTS".equals(status)) {
                int count = Integer.parseInt(buf.readString(8));
                if (count < 0 || count > 20) return;
                java.util.List<String[]> reports = new java.util.ArrayList<>();
                for (int i = 0; i < count; i++) {
                    reports.add(new String[]{buf.readString(96), buf.readString(32), buf.readString(64),
                            buf.readString(24), buf.readString(40), buf.readString(300)});
                }
                context.client().execute(() -> DashboardScreen.updateReports(reports));
            } else if ("SETTINGS".equals(status)) {
                String[] theme = new String[]{buf.readString(32), buf.readString(16), buf.readString(16),
                        buf.readString(16), buf.readString(16), buf.readString(16), buf.readString(8)};
                context.client().execute(() -> DashboardScreen.updateSettings(theme));
            } else if ("ANALYTICS".equals(status)) {
                int currentOnline = Integer.parseInt(buf.readString(8));
                int currentAfk = Integer.parseInt(buf.readString(8));
                int tracked = Integer.parseInt(buf.readString(8));
                int count = Integer.parseInt(buf.readString(8));
                if (count < 0 || count > 10) return;
                java.util.List<String[]> entries = new java.util.ArrayList<>();
                for (int i = 0; i < count; i++) {
                    entries.add(new String[]{buf.readString(48), buf.readString(16), buf.readString(24),
                            buf.readString(24), buf.readString(12)});
                }
                context.client().execute(() -> DashboardScreen.updateAnalytics(currentOnline, currentAfk, tracked, entries));
            } else if ("PUNISHMENTS".equals(status)) {
                int count = Integer.parseInt(buf.readString(8));
                if (count < 0 || count > 30) return;
                java.util.List<String[]> entries = new java.util.ArrayList<>();
                for (int i = 0; i < count; i++) {
                    entries.add(new String[]{buf.readString(48), buf.readString(240)});
                }
                context.client().execute(() -> DashboardScreen.updatePunishments(entries));
            } else if ("DEATH_LOGS".equals(status)) {
                int count = Integer.parseInt(buf.readString(8));
                if (count < 0 || count > 20) return;
                java.util.List<String[]> entries = new java.util.ArrayList<>();
                for (int i = 0; i < count; i++) {
                    entries.add(new String[]{buf.readString(48), buf.readString(32), buf.readString(64),
                            buf.readString(112), buf.readString(180), buf.readString(16)});
                }
                context.client().execute(() -> DashboardScreen.updateDeaths(entries));
            } else if ("ACTIVITY".equals(status)) {
                int count = Integer.parseInt(buf.readString(8));
                if (count < 0 || count > 10) return;
                java.util.List<String> entries = new java.util.ArrayList<>();
                for (int i = 0; i < count; i++) entries.add(buf.readString(280));
                context.client().execute(() -> DashboardScreen.updateActivity(entries));
            } else if ("REPORT_ACTION_RESULT".equals(status)) {
                String result = buf.readString(24);
                String message = buf.readString(160);
                context.client().execute(() -> DashboardScreen.updateReportActionResult(result, message));
            } else if ("SECTION_DENIED".equals(status)) {
                String section = buf.readString(32);
                String reason = buf.readString(128);
                context.client().execute(() -> DashboardScreen.updateSectionDenied(section, reason));
            } else if ("PLAYER_PROFILE".equals(status)) {
                String name = buf.readString(64);
                String gameMode = buf.readString(32);
                String world = buf.readString(128);
                int ping = Integer.parseInt(buf.readString(16));
                context.client().execute(() -> DashboardScreen.updatePlayerProfile(name, gameMode, world, ping));
            } else if ("PROFILE_ERROR".equals(status)) {
                String reason = buf.readString(128);
                context.client().execute(() -> DashboardScreen.updatePlayerProfileError(reason));
            } else if ("DENIED".equals(status)) {
                String reason = buf.readString(256);
                context.client().execute(() -> {
                    handshakeAccepted = false;
                    if (context.client().player != null) context.client().player.sendMessage(Text.literal("StaffJobs access denied: " + reason), false);
                });
            }
        });

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            while (openDashboardKey.wasPressed()) {
                if (client.player == null || client.getNetworkHandler() == null) continue;
                if (!ClientPlayNetworking.canSend(StaffJobsPayload.ID)) {
                    client.player.sendMessage(Text.literal("This server does not provide StaffJobs UI."), false);
                    continue;
                }
                handshakeAccepted = false;
                DashboardScreen.clearOverview();
                PacketByteBuf payload = PacketByteBufs.create();
                payload.writeString("HELLO");
                sendPayload(payload);
                client.player.sendMessage(Text.literal("Connecting to StaffJobs..."), false);
            }
        });
    }

    private static void requestOverview() {
        if (!handshakeAccepted || !ClientPlayNetworking.canSend(StaffJobsPayload.ID)) return;
        net.minecraft.network.PacketByteBuf payload = net.fabricmc.fabric.api.networking.v1.PacketByteBufs.create();
        payload.writeString("GET_OVERVIEW");
        sendPayload(payload);
    }

    public static void requestSettings() {
        if (!handshakeAccepted || !ClientPlayNetworking.canSend(StaffJobsPayload.ID)) return;
        net.minecraft.network.PacketByteBuf payload = net.fabricmc.fabric.api.networking.v1.PacketByteBufs.create();
        payload.writeString("GET_SETTINGS");
        sendPayload(payload);
    }

    public static void requestAnalytics() {
        if (!handshakeAccepted || !ClientPlayNetworking.canSend(StaffJobsPayload.ID)) return;
        net.minecraft.network.PacketByteBuf payload = net.fabricmc.fabric.api.networking.v1.PacketByteBufs.create();
        payload.writeString("GET_ANALYTICS");
        sendPayload(payload);
    }

    public static void requestPunishments() {
        if (!handshakeAccepted || !ClientPlayNetworking.canSend(StaffJobsPayload.ID)) return;
        net.minecraft.network.PacketByteBuf payload = net.fabricmc.fabric.api.networking.v1.PacketByteBufs.create();
        payload.writeString("GET_PUNISHMENTS");
        sendPayload(payload);
    }

    public static void requestDeathLogs() {
        if (!handshakeAccepted || !ClientPlayNetworking.canSend(StaffJobsPayload.ID)) return;
        net.minecraft.network.PacketByteBuf payload = net.fabricmc.fabric.api.networking.v1.PacketByteBufs.create();
        payload.writeString("GET_DEATH_LOGS");
        sendPayload(payload);
    }

    public static void requestActivity() {
        if (!handshakeAccepted || !ClientPlayNetworking.canSend(StaffJobsPayload.ID)) return;
        net.minecraft.network.PacketByteBuf payload = net.fabricmc.fabric.api.networking.v1.PacketByteBufs.create();
        payload.writeString("GET_ACTIVITY");
        sendPayload(payload);
    }
    public static void requestReports() {
        if (!handshakeAccepted || !ClientPlayNetworking.canSend(StaffJobsPayload.ID)) return;
        net.minecraft.network.PacketByteBuf payload = net.fabricmc.fabric.api.networking.v1.PacketByteBufs.create();
        payload.writeString("GET_REPORTS");
        sendPayload(payload);
    }
    public static void performReportAction(String action, String reportId) {
        if (!handshakeAccepted || !ClientPlayNetworking.canSend(StaffJobsPayload.ID) || reportId == null) return;
        net.minecraft.network.PacketByteBuf payload = net.fabricmc.fabric.api.networking.v1.PacketByteBufs.create();
        payload.writeString("REPORT_ACTION");
        payload.writeString(action);
        payload.writeString(reportId);
        sendPayload(payload);
    }
    public static void requestPlayers() {
        if (!handshakeAccepted || !ClientPlayNetworking.canSend(StaffJobsPayload.ID)) return;
        net.minecraft.network.PacketByteBuf payload = net.fabricmc.fabric.api.networking.v1.PacketByteBufs.create();
        payload.writeString("GET_PLAYERS");
        sendPayload(payload);
    }

    public static void requestPlayerProfile(String playerName) {
        if (!handshakeAccepted || !ClientPlayNetworking.canSend(StaffJobsPayload.ID)) return;
        net.minecraft.network.PacketByteBuf payload = net.fabricmc.fabric.api.networking.v1.PacketByteBufs.create();
        payload.writeString("GET_PLAYER_PROFILE");
        payload.writeString(playerName);
        sendPayload(payload);
    }

    private static void sendPayload(PacketByteBuf buffer) {
        byte[] bytes = new byte[buffer.readableBytes()];
        buffer.readBytes(bytes);
        ClientPlayNetworking.send(new StaffJobsPayload(bytes));
    }

    public static boolean isHandshakeAccepted() { return handshakeAccepted; }
}
