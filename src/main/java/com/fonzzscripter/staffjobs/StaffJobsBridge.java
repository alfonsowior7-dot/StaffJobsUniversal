package com.fonzzscripter.staffjobs;

import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.plugin.messaging.PluginMessageListener;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Initial, versioned plugin-message bridge for the optional StaffJobs Fabric UI mod.
 *
 * Protocol v1 intentionally exposes only a capability handshake. Dashboard data and
 * administrative actions will be added in later steps after their permission checks
 * and request schemas are implemented.
 */
public final class StaffJobsBridge implements PluginMessageListener {
    public static final String CHANNEL = "staffjobs:ui";
    public static final int PROTOCOL_VERSION = 1;

    private final JavaPlugin plugin;
    private final Map<UUID, Long> lastHelloAt = new ConcurrentHashMap<>();

    private StaffJobsBridge(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    public static void register(JavaPlugin plugin) {
        StaffJobsBridge bridge = new StaffJobsBridge(plugin);
        plugin.getServer().getMessenger().registerIncomingPluginChannel(plugin, CHANNEL, bridge);
        plugin.getServer().getMessenger().registerOutgoingPluginChannel(plugin, CHANNEL);
        plugin.getLogger().info("StaffJobs UI bridge registered on " + CHANNEL
                + " (protocol " + PROTOCOL_VERSION + ").");
    }

    @Override
    public void onPluginMessageReceived(String channel, Player player, byte[] message) {
        if (!CHANNEL.equals(channel) || message == null || message.length == 0 || message.length > 1024) {
            return;
        }

        String request;
        try (DataInputStream input = new DataInputStream(new ByteArrayInputStream(message))) {
            request = readString(input);
        } catch (IOException exception) {
            plugin.getLogger().fine("Ignored malformed StaffJobs UI message from " + player.getName());
            return;
        }

        if (!"HELLO".equals(request)) {
            return;
        }

        long now = System.currentTimeMillis();
        Long previous = lastHelloAt.put(player.getUniqueId(), now);
        if (previous != null && now - previous < 1000L) {
            return;
        }

        if (!player.hasPermission("staffjobs.dashboard")) {
            sendResponse(player, "DENIED", "Missing permission: staffjobs.dashboard");
            return;
        }

        sendResponse(player, "WELCOME", Integer.toString(PROTOCOL_VERSION),
                plugin.getDescription().getVersion(), player.getUniqueId().toString());
    }

    private static String readString(DataInputStream input) throws IOException {
        int length = readVarInt(input);
        if (length < 0 || length > 1024 || length > input.available()) {
            throw new IOException("Invalid string length");
        }
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
        int value = 0;
        int position = 0;
        byte current;
        do {
            current = input.readByte();
            value |= (current & 0x7F) << position;
            position += 7;
            if (position >= 32) throw new IOException("VarInt too large");
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
                for (String field : fields) {
                    writeString(output, field);
                }
            }
            player.sendPluginMessage(plugin, CHANNEL, bytes.toByteArray());
        } catch (IOException exception) {
            plugin.getLogger().warning("Could not send StaffJobs UI response to " + player.getName()
                    + ": " + exception.getMessage());
        }
    }
}
