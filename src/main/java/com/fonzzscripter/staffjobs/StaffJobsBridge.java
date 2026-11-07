package com.fonzzscripter.staffjobs;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.plugin.messaging.PluginMessageListener;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/** Secure, read-only first-stage bridge for the optional StaffJobs Fabric UI. */
public final class StaffJobsBridge implements PluginMessageListener {
    public static final String CHANNEL = "staffjobs:ui";
    public static final int PROTOCOL_VERSION = 1;
    private static final long REQUEST_COOLDOWN_MS = 750L;
    private final JavaPlugin plugin;
    private final Map<UUID, Long> lastRequestAt = new ConcurrentHashMap<>();
    private final Map<UUID, Long> lastHelloAt = new ConcurrentHashMap<>();
    private final Set<UUID> authenticated = ConcurrentHashMap.newKeySet();

    private StaffJobsBridge(JavaPlugin plugin) { this.plugin = plugin; }

    public static void register(JavaPlugin plugin) {
        StaffJobsBridge bridge = new StaffJobsBridge(plugin);
        plugin.getServer().getMessenger().registerIncomingPluginChannel(plugin, CHANNEL, bridge);
        plugin.getServer().getMessenger().registerOutgoingPluginChannel(plugin, CHANNEL);
        plugin.getLogger().info("StaffJobs UI bridge registered on " + CHANNEL
                + " (protocol " + PROTOCOL_VERSION + ").");
    }

    @Override
    public void onPluginMessageReceived(String channel, Player player, byte[] message) {
        if (!CHANNEL.equals(channel) || message == null || message.length == 0 || message.length > 1024) return;
        String request;
        String targetName = null;
        String reportAction = null;
        String reportId = null;
        try (DataInputStream input = new DataInputStream(new ByteArrayInputStream(message))) {
            request = readString(input);
            if ("GET_PLAYER_PROFILE".equals(request)) targetName = readString(input);
        if ("REPORT_ACTION".equals(request)) { reportAction = readString(input); reportId = readString(input); }
            if (input.available() != 0) throw new IOException("Unexpected trailing payload");
        } catch (IOException exception) {
            plugin.getLogger().fine("Ignored malformed StaffJobs UI message from " + player.getName());
            return;
        }

        long now = System.currentTimeMillis();
        if ("HELLO".equals(request)) {
            Long previousHello = lastHelloAt.put(player.getUniqueId(), now);
            if (previousHello != null && now - previousHello < 1000L) return;
            authenticated.remove(player.getUniqueId());
            if (!player.hasPermission("staffjobs.dashboard")) {
                sendResponse(player, "DENIED", "Missing permission: staffjobs.dashboard");
                return;
            }
            authenticated.add(player.getUniqueId());
            sendResponse(player, "WELCOME", Integer.toString(PROTOCOL_VERSION),
                    plugin.getDescription().getVersion(), player.getUniqueId().toString());
            return;
        }

        Long previousRequest = lastRequestAt.put(player.getUniqueId(), now);
        if (previousRequest != null && now - previousRequest < REQUEST_COOLDOWN_MS) return;

        if (!authenticated.contains(player.getUniqueId())
                || !player.hasPermission("staffjobs.dashboard")) {
            authenticated.remove(player.getUniqueId());
            sendResponse(player, "DENIED", "Dashboard session is not authorized. Reconnect and try again.");
            return;
        }

            if ("GET_PUNISHMENTS".equals(request)) {
            if (!player.hasPermission("staffjobs.punishments")) {
                sendResponse(player, "SECTION_DENIED", "punishments", "Missing permission: staffjobs.punishments");
                return;
            }
            if (!(plugin instanceof StaffJobsPlugin)) { sendResponse(player, "PUNISHMENTS", "0"); return; }
            java.util.List<String[]> entries = ((StaffJobsPlugin) plugin).getDashboardPunishments();
            String[] response = new String[2 + entries.size() * 2];
            response[0] = "PUNISHMENTS";
            response[1] = Integer.toString(entries.size());
            for (int i = 0; i < entries.size(); i++) {
                response[2 + i * 2] = safeField(entries.get(i)[0], 36);
                response[3 + i * 2] = safeField(entries.get(i)[1], 220);
            }
            sendResponse(player, response);
            return;
        }

        if ("GET_DEATH_LOGS".equals(request)) {
            if (!player.hasPermission("staffjobs.deathlogs")) {
                sendResponse(player, "SECTION_DENIED", "deaths", "Missing permission: staffjobs.deathlogs");
                return;
            }
            if (!(plugin instanceof StaffJobsPlugin)) { sendResponse(player, "DEATH_LOGS", "0"); return; }
            java.util.List<String[]> entries = ((StaffJobsPlugin) plugin).getDashboardDeaths();
            String[] response = new String[2 + entries.size() * 6];
            response[0] = "DEATH_LOGS";
            response[1] = Integer.toString(entries.size());
            for (int i = 0; i < entries.size(); i++) {
                String[] entry = entries.get(i);
                int offset = 2 + i * 6;
                response[offset] = safeField(entry[0], 36);
                response[offset + 1] = safeField(entry[1], 16);
                response[offset + 2] = safeField(entry[2], 48);
                response[offset + 3] = safeField(entry[3], 96);
                response[offset + 4] = safeField(entry[4], 160);
                response[offset + 5] = safeField(entry[5], 12);
            }
            sendResponse(player, response);
            return;
        }

        if ("GET_ACTIVITY".equals(request)) {
            if (!player.hasPermission("staffjobs.activity")) {
                sendResponse(player, "SECTION_DENIED", "activity", "Missing permission: staffjobs.activity");
                return;
            }
            if (!(plugin instanceof StaffJobsPlugin)) { sendResponse(player, "ACTIVITY", "0"); return; }
            java.util.List<String> entries = ((StaffJobsPlugin) plugin).getDashboardActivity();
            String[] response = new String[2 + entries.size()];
            response[0] = "ACTIVITY";
            response[1] = Integer.toString(entries.size());
            for (int i = 0; i < entries.size(); i++) response[i + 2] = safeField(entries.get(i), 240);
            sendResponse(player, response);
            return;
        }
        if ("GET_OVERVIEW".equals(request)) {
            if (!player.hasPermission("staffjobs.dashboard")) {
                sendResponse(player, "DENIED", "Missing permission: staffjobs.dashboard");
                return;
            }
            int onlinePlayers = Bukkit.getOnlinePlayers().size();
            int maxPlayers = Bukkit.getMaxPlayers();
            int onlineStaff = 0;
            for (Player online : Bukkit.getOnlinePlayers()) {
                if (online.hasPermission("staffjobs.use")) onlineStaff++;
            }
            sendResponse(player, "OVERVIEW", Integer.toString(onlinePlayers),
                    Integer.toString(maxPlayers), Integer.toString(onlineStaff));
            return;
        }

        if ("GET_PLAYER_PROFILE".equals(request)) {
            if (!player.hasPermission("staffjobs.players")) {
                sendResponse(player, "DENIED", "Missing permission: staffjobs.players");
                return;
            }
            if (targetName == null || targetName.length() < 1 || targetName.length() > 16
                    || !targetName.matches("[A-Za-z0-9_]+")) {
                sendResponse(player, "PROFILE_ERROR", "Invalid player name.");
                return;
            }
            Player target = Bukkit.getPlayerExact(targetName);
            if (target == null) {
                sendResponse(player, "PROFILE_ERROR", "Player is no longer online.");
                return;
            }
            sendResponse(player, "PLAYER_PROFILE", target.getName(), target.getGameMode().name(),
                    target.getWorld().getName(), Integer.toString(Math.max(0, target.getPing())));
            return;
        }

        if ("REPORT_ACTION".equals(request)) {
            if (!player.hasPermission("staffjobs.reports")) {
                sendResponse(player, "REPORT_ACTION_RESULT", "DENIED", "Missing permission: staffjobs.reports");
                return;
            }
            if (!(plugin instanceof StaffJobsPlugin) || reportAction == null || reportId == null
                    || reportAction.length() > 12 || reportId.length() > 80) {
                sendResponse(player, "REPORT_ACTION_RESULT", "INVALID", "Invalid report action.");
                return;
            }
            boolean success = ((StaffJobsPlugin) plugin).performDashboardReportAction(player, reportAction, reportId);
            sendResponse(player, "REPORT_ACTION_RESULT", success ? "OK" : "FAILED",
                    success ? "Report action processed." : "Report not found or action rejected.");
            return;
        }
        if ("GET_REPORTS".equals(request)) {
            if (!player.hasPermission("staffjobs.reports")) {
                sendResponse(player, "SECTION_DENIED", "reports", "Missing permission: staffjobs.reports");
                return;
            }
            if (!(plugin instanceof StaffJobsPlugin)) {
                sendResponse(player, "REPORTS", "0");
                return;
            }
            java.util.List<java.util.Map<?, ?>> reports = ((StaffJobsPlugin) plugin).getDashboardReports();
            int count = Math.min(20, reports.size());
            String[] response = new String[2 + count * 6];
            response[0] = "REPORTS";
            response[1] = Integer.toString(count);
            for (int i = 0; i < count; i++) {
                java.util.Map<?, ?> report = reports.get(reports.size() - 1 - i);
                int offset = 2 + i * 6;
                response[offset] = safeField(report.get("id"), 80);
                response[offset + 1] = safeField(report.get("player"), 16);
                response[offset + 2] = safeField(report.get("date"), 48);
                response[offset + 3] = safeField(report.get("status"), 16);
                response[offset + 4] = safeField(report.get("assigned-staff"), 32);
                response[offset + 5] = safeField(report.get("message"), 256);
            }
            sendResponse(player, response);
            return;
        }
        if ("GET_PLAYERS".equals(request)) {
            if (!player.hasPermission("staffjobs.players")) {
                sendResponse(player, "DENIED", "Missing permission: staffjobs.players");
                return;
            }
            java.util.List<String> names = new java.util.ArrayList<>();
            for (Player online : Bukkit.getOnlinePlayers()) {
                if (names.size() >= 100) break;
                names.add(online.getName());
            }
            String[] response = new String[names.size() + 2];
            response[0] = "PLAYERS";
            response[1] = Integer.toString(names.size());
            for (int i = 0; i < names.size(); i++) response[i + 2] = names.get(i);
            sendResponse(player, response);
        }
    }

    private static String safeField(Object value, int maxLength) {
        if (value == null) return "";
        String text = String.valueOf(value).replaceAll("[\\r\\n§]", " ");
        return text.length() <= maxLength ? text : text.substring(0, maxLength);
    }
    private static String readString(DataInputStream input) throws IOException {
        int length = readVarInt(input);
        if (length < 0 || length > 1024 || length > input.available()) throw new IOException("Invalid string length");
        byte[] bytes = new byte[length];
        input.readFully(bytes);
        return new String(bytes, StandardCharsets.UTF_8);
    }

    private static void writeString(DataOutputStream output, String value) throws IOException {
        byte[] bytes = value.getBytes(StandardCharsets.UTF_8);
        writeVarInt(output, bytes.length);
        output.write(bytes);
    }

    private static int readVarInt(DataInputStream input) throws IOException {
        int value = 0, position = 0;
        byte current;
        do {
            if (position >= 35) throw new IOException("VarInt too large");
            current = input.readByte();
            value |= (current & 0x7F) << position;
            position += 7;
        } while ((current & 0x80) != 0);
        return value;
    }

    private static void writeVarInt(DataOutputStream output, int value) throws IOException {
        while ((value & 0xFFFFFF80) != 0) {
            output.writeByte((value & 0x7F) | 0x80);
            value >>>= 7;
        }
        output.writeByte(value);
    }

    private void sendResponse(Player player, String... fields) {
        try {
            ByteArrayOutputStream bytes = new ByteArrayOutputStream();
            try (DataOutputStream output = new DataOutputStream(bytes)) {
                for (String field : fields) writeString(output, field);
            }
            player.sendPluginMessage(plugin, CHANNEL, bytes.toByteArray());
        } catch (IOException exception) {
            plugin.getLogger().warning("Could not send StaffJobs UI response to " + player.getName()
                    + ": " + exception.getMessage());
        }
    }
}
