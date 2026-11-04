package com.fonzzscripter.staffjobs;

import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerPickupItemEvent;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

/**
 * Detects unusually large amounts of configured rare materials picked up
 * by a player during a rolling time window. Alerts are investigative signals,
 * never automatic proof of cheating.
 */
@SuppressWarnings("deprecation")
public final class XrayDetectionListener implements Listener {
    private final StaffJobsPlugin plugin;
    private final Map<UUID, Map<Material, Deque<Pickup>>> pickups = new HashMap<>();
    private final Map<String, Long> lastAlert = new HashMap<>();

    public XrayDetectionListener(StaffJobsPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onRareMaterialPickup(PlayerPickupItemEvent event) {
        if (!plugin.getConfig().getBoolean("settings.xray-detection.enabled", true)) return;

        Player player = event.getPlayer();
        if (player.getGameMode() == GameMode.CREATIVE || player.getGameMode() == GameMode.SPECTATOR) return;

        Material material = event.getItem().getItemStack().getType();
        String base = "settings.xray-detection.materials." + material.name();
        if (!plugin.getConfig().getBoolean(base + ".enabled", false)) return;

        int threshold = Math.max(1, plugin.getConfig().getInt(base + ".threshold", Integer.MAX_VALUE));
        long windowMs = Math.max(1L, plugin.getConfig().getLong("settings.xray-detection.time-window-seconds", 60L)) * 1000L;
        long now = System.currentTimeMillis();

        Map<Material, Deque<Pickup>> byMaterial = pickups.computeIfAbsent(player.getUniqueId(), key -> new HashMap<>());
        Deque<Pickup> history = byMaterial.computeIfAbsent(material, key -> new ArrayDeque<>());
        history.addLast(new Pickup(now, event.getItem().getItemStack().getAmount()));

        while (!history.isEmpty() && now - history.peekFirst().time > windowMs) history.removeFirst();

        int total = 0;
        for (Pickup pickup : history) total += pickup.amount;
        if (total < threshold) return;

        String alertKey = player.getUniqueId() + ":" + material.name();
        long cooldown = Math.max(0L, plugin.getConfig().getLong("settings.xray-detection.alert-cooldown-seconds", 60L)) * 1000L;
        long previous = lastAlert.getOrDefault(alertKey, 0L);
        if (cooldown > 0L && now - previous < cooldown) return;
        lastAlert.put(alertKey, now);

        if (!plugin.getConfig().getBoolean("settings.xray-detection.notify-staff", true)) return;
        String message = "§8[§cSTAFFJOBS X-Ray§8] §e" + player.getName()
                + " §7picked up §c" + total + " " + material.name().toLowerCase(Locale.ROOT)
                + " §7within §f" + (windowMs / 1000L) + "s§7. §cInvestigate manually; this is not proof of cheating.";
        for (Player staff : plugin.getServer().getOnlinePlayers()) {
            if (staff.hasPermission("staffjobs.xray.alert") || staff.hasPermission("staffjobs.protect.bypass")) {
                staff.sendMessage(message);
            }
        }
    }

    private static final class Pickup {
        private final long time;
        private final int amount;

        private Pickup(long time, int amount) {
            this.time = time;
            this.amount = amount;
        }
    }
}
