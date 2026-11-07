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
        try (DataInputStream input = new DataInputStream(new ByteArrayInputStream(message))) {
            request = readString(input);
            if (input.available() != 0) throw new IOException("Unexpected trailing payload");
        } catch (IOException exception) {
            plugin.getLogger().fine("Ignored malformed StaffJobs UI message from " + player.getName());
            return;
        }

        long now = System.currentTimeMillis();
        Long previous = lastRequestAt.put(player.getUniqueId(), now);
        if (previous != null && now - previous < REQUEST_COOLDOWN_MS) return;

        if ("HELLO".equals(request)) {
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

        if (!authenticated.contains(player.getUniqueId())
                || !player.hasPermission("staffjobs.dashboard")) {
            authenticated.remove(player.getUniqueId());
            sendResponse(player, "DENIED", "Dashboard session is not authorized. Reconnect and try again.");
            return;
        }

        if ("GET_OVERVIEW".equals(request)) {
            int onlinePlayers = Bukkit.getOnlinePlayers().size();
            int maxPlayers = Bukkit.getMaxPlayers();
            int onlineStaff = 0;
            for (Player online : Bukkit.getOnlinePlayers()) {
                if (online.hasPermission("staffjobs.use")) onlineStaff++;
            }
            sendResponse(player, "OVERVIEW", Integer.toString(onlinePlayers),
                    Integer.toString(maxPlayers), Integer.toString(onlineStaff));
        }
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
