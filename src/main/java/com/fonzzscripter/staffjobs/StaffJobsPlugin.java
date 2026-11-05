package com.fonzzscripter.staffjobs;

import org.bukkit.BanList;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.configuration.serialization.ConfigurationSerialization;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerSwapHandItemsEvent;
import org.bukkit.event.player.PlayerItemHeldEvent;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.event.player.PlayerPickupItemEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.Sound;
import org.bukkit.event.player.PlayerItemConsumeEvent;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

public class StaffJobsPlugin extends JavaPlugin implements Listener {
    private static final String PREFIX = ChatColor.YELLOW + "[ʜᴇʟᴘᴇʀꜱᴛᴀꜰꜰ] ";
    private static final String MENU = "§8STAFFJOBS";
    private static final String MENU_XRAY = "§8STAFFJOBS - Page 2";
    private static final String PLAYERS = "§8Select Player";
    private static final String ACTIONS = "§8Manage Player";
    private static final String CONFIRM = "§8Confirmation";
    private static final String REPORTS = "§8Reports";
    private static final String REPORT_DETAIL = "§8Report Details";
    private static final String ONLINE_STAFF = "§8Online Staff Status";
    private static final String NOTIFICATION_SETTINGS = "§8Notification Settings";
    private static final String STAFF_HISTORY_DETAIL = "§8Staff History Details";

    private final Set<UUID> frozen = new HashSet<>();
    private final Set<UUID> xrayEnabled = java.util.concurrent.ConcurrentHashMap.newKeySet();
    private final Map<UUID, Set<Location>> xrayHiddenBlocks = new java.util.concurrent.ConcurrentHashMap<>();
    private final Map<UUID, UUID> selected = new HashMap<>();
    private final Map<UUID, String> pendingAction = new java.util.concurrent.ConcurrentHashMap<>();
    private final Map<UUID, String> pendingReason = new java.util.concurrent.ConcurrentHashMap<>();
    private final Map<UUID, Integer> reportPages = new HashMap<>();
    private final Map<UUID, Integer> playerPages = new HashMap<>();
    private final Map<UUID, String> selectedReports = new HashMap<>();

    private File frozenFile, reportsFile, notificationsFile, logsFile, deathsFile, punishmentsFile, staffStatusFile;
    private YamlConfiguration staffStatusData;
    private final Map<UUID, Long> lastActivity = new java.util.concurrent.ConcurrentHashMap<>();
    private final Set<UUID> afkPlayers = java.util.concurrent.ConcurrentHashMap.newKeySet();
    private YamlConfiguration frozenData, reportsData, notificationsData, logsData, deathsData, punishmentsData;
    private final Map<UUID, Long> mutedUntil = new java.util.concurrent.ConcurrentHashMap<>();
    private final DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm:ss");
    private ZoneId getConfiguredZone() {
        String configured = getConfig().getString("settings.timezone", "Asia/Jakarta");
        try {
            return ZoneId.of(configured == null || configured.isBlank() ? "Asia/Jakarta" : configured);
        } catch (RuntimeException exception) {
            getLogger().warning("Invalid settings.timezone '" + configured + "'. Falling back to Asia/Jakarta.");
            return ZoneId.of("Asia/Jakarta");
        }
    }

    private String getZoneLabel() {
        return switch (getConfiguredZone().getId()) {
            case "Asia/Jakarta" -> "WIB";
            case "Asia/Makassar" -> "WITA";
            case "Asia/Jayapura" -> "WIT";
            default -> getConfiguredZone().getId();
        };
    }
    private final Map<UUID, String> deathSelection = new HashMap<>();
    private final Map<UUID, Integer> deathPages = new HashMap<>();
    private final Map<UUID, Integer> activityPages = new HashMap<>();
    private final Map<UUID, Integer> staffHistoryPages = new HashMap<>();
    private final Map<UUID, String> selectedHistoryStaff = new HashMap<>();
    private final Map<UUID, Long> lastReportAt = new java.util.concurrent.ConcurrentHashMap<>();
    private volatile String latestVersion;
    private volatile boolean updateCheckCompleted;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        loadFrozen();
        loadReports();
        loadNotifications();
        loadLogs();
        loadDeaths();
        loadPunishments();
        loadStaffStatus();
        getServer().getPluginManager().registerEvents(this, this);
        Bukkit.getScheduler().runTaskTimer(this, this::refreshAllXrayViews, 40L, 40L);
        if (getConfig().getBoolean("settings.update-check.enabled", true)) checkForUpdates();
        Objects.requireNonNull(getCommand("staffmenu")).setExecutor(this);
        Objects.requireNonNull(getCommand("report")).setExecutor(this);
        Bukkit.getScheduler().runTaskTimer(this, this::removeExpiredReports, 20L * 60L, 20L * 60L);
        long afkMinutes = Math.max(1L, getConfig().getLong("settings.staff.afk-minutes", 5L));
        Bukkit.getScheduler().runTaskTimer(this, () -> {
            long cutoff = System.currentTimeMillis() - afkMinutes * 60_000L;
            for (Player online : Bukkit.getOnlinePlayers()) {
                if (!online.hasPermission("staffjobs.use")) continue;
                long last = lastActivity.getOrDefault(online.getUniqueId(), System.currentTimeMillis());
                if (last <= cutoff) afkPlayers.add(online.getUniqueId());
                else afkPlayers.remove(online.getUniqueId());
            }
        }, 20L * 30L, 20L * 30L);
        if (getConfig().getBoolean("settings.backups.enabled", true)) {
            long interval = Math.max(1L, getConfig().getLong("settings.backups.interval-hours", 24L));
            long ticks = interval * 60L * 60L * 20L;
            Bukkit.getScheduler().runTaskTimer(this, this::createDataBackup, ticks, ticks);
        }
        if (getConfig().getBoolean("settings.auto-save", true)) {
            Bukkit.getScheduler().runTaskTimer(this, this::saveAllData, 20L * 60L * 5L, 20L * 60L * 5L);
        }
        getLogger().info("STAFFJOBS has been enabled successfully!");
    }

    private void saveAllData() {
        saveFrozen();
        saveReports();
        savePunishments();
        saveNotifications();
        saveLogs();
        saveDeaths();
        saveStaffStatus();
    }

    @Override
    public void onDisable() {
        for (Player online : Bukkit.getOnlinePlayers()) disableXray(online, false);
        saveAllData();
        selected.clear();
        pendingAction.clear();
        pendingReason.clear();
        reportPages.clear();
        playerPages.clear();
        activityPages.clear();
        selectedReports.clear();
    }

    // DATA STORAGE

    private void loadFrozen() {
        frozenFile = new File(getDataFolder(), "frozen.yml");
        if (!getDataFolder().exists()) getDataFolder().mkdirs();
        frozenData = YamlConfiguration.loadConfiguration(frozenFile);
        for (String value : frozenData.getStringList("frozen-players")) {
            try {
                frozen.add(UUID.fromString(value));
            } catch (IllegalArgumentException ignored) {
                getLogger().warning("Invalid freeze UUID: " + value);
            }
        }
    }

    private void saveFrozen() {
        if (frozenData == null) return;
        List<String> values = new ArrayList<>();
        for (UUID uuid : frozen) values.add(uuid.toString());
        frozenData.set("frozen-players", values);
        saveFile(frozenData, frozenFile);
    }

    private void loadReports() {
        reportsFile = new File(getDataFolder(), "reports.yml");
        reportsData = YamlConfiguration.loadConfiguration(reportsFile);
        if (!reportsData.contains("reports")) reportsData.set("reports", new ArrayList<>());
        if (!reportsData.contains("history")) reportsData.set("history", new ArrayList<>());
        saveReports();
    }

    private void saveReports() {
        if (reportsData != null) saveFile(reportsData, reportsFile);
    }

    private void loadNotifications() {
        notificationsFile = new File(getDataFolder(), "notifications.yml");
        notificationsData = YamlConfiguration.loadConfiguration(notificationsFile);
    }

    private void saveNotifications() {
        if (notificationsData != null) saveFile(notificationsData, notificationsFile);
    }

    private void saveFile(YamlConfiguration data, File file) {
        try {
            data.save(file);
        } catch (IOException exception) {
            getLogger().severe("Failed to save " + file.getName());
            exception.printStackTrace();
        }
    }

    // COMMANDS

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (command.getName().equalsIgnoreCase("staffbackup")) {
            if (!sender.hasPermission("staffjobs.backup")) {
                sender.sendMessage(PREFIX + ChatColor.RED + "You do not have permission.");
                return true;
            }
            if (args.length == 0 || args[0].equalsIgnoreCase("create")) {
                if (createDataBackup()) sender.sendMessage(PREFIX + ChatColor.GREEN + "Manual data backup created successfully.");
                else sender.sendMessage(PREFIX + ChatColor.RED + "Backup failed. Check the server console for details.");
            } else if (args[0].equalsIgnoreCase("restore") && args.length == 2) {
                restoreDataBackup(sender, args[1]);
            } else if (args[0].equalsIgnoreCase("list")) {
                File folder = new File(getDataFolder(), "backups");
                File[] backups = folder.listFiles(File::isDirectory);
                sender.sendMessage("§eAvailable STAFFJOBS backups:");
                if (backups == null || backups.length == 0) sender.sendMessage("§7No backups found.");
                else { Arrays.sort(backups, Comparator.comparingLong(File::lastModified).reversed()); for (File item : backups) sender.sendMessage("§7- §f" + item.getName()); }
            } else sender.sendMessage("§eUsage: /staffbackup [create|list|restore <backup-name>]");
            return true;
        }

        if (command.getName().equalsIgnoreCase("staffjobs")) {
            String version = getDescription().getVersion();
            String author = getDescription().getAuthors().isEmpty()
                    ? "Unknown"
                    : String.join(", ", getDescription().getAuthors());
            sender.sendMessage("§8§m----------------------------------------");
            sender.sendMessage("§b§lSTAFFJOBS §7Plugin Information");
            sender.sendMessage("§7Version: §a" + version);
            sender.sendMessage("§7Author: §f" + author);
            sender.sendMessage("§7Description: §f" + getDescription().getDescription());
            sender.sendMessage("§8§m----------------------------------------");
            return true;
        }

        if (command.getName().equalsIgnoreCase("staffmenu")) {
            if (!(sender instanceof Player player)) {
                sender.sendMessage("This command can only be used by players.");
                return true;
            }
            if (!player.hasPermission("staffjobs.use")) {
                player.sendMessage(PREFIX + ChatColor.RED + "You do not have permission.");
                return true;
            }
            openStaffMenu(player);
            return true;
        }


        if (command.getName().equalsIgnoreCase("warn")
                || command.getName().equalsIgnoreCase("mute")
                || command.getName().equalsIgnoreCase("unmute")
                || command.getName().equalsIgnoreCase("punishments")) {
            String action = command.getName().toLowerCase(Locale.ROOT);
            if (!sender.hasPermission("staffjobs." + action)) {
                sender.sendMessage(PREFIX + ChatColor.RED + "You do not have permission.");
                return true;
            }
            int minimum = action.equals("warn") ? 2 : action.equals("mute") ? 3 : 1;
            if (args.length < minimum) {
                sender.sendMessage(PREFIX + ChatColor.RED + "Usage: /" + action + " " +
                        (action.equals("warn") ? "<player> <reason>" :
                         action.equals("mute") ? "<player> <duration> <reason>" : "<player>"));
                return true;
            }
            Player target = Bukkit.getPlayerExact(args[0]);
            if (target == null) {
                sender.sendMessage(PREFIX + ChatColor.RED + "Player must be online.");
                return true;
            }
            if (sender instanceof Player staff && !canPunish(staff, target)) {
                sender.sendMessage(PREFIX + ChatColor.RED + "You cannot punish yourself or an equal/higher-ranked player.");
                return true;
            }
            String actor = sender.getName();
            if (action.equals("warn")) {
                String reason = String.join(" ", Arrays.copyOfRange(args, 1, args.length));
                addPunishment(target, "WARN", reason, actor, 0L);
                target.sendMessage(PREFIX + ChatColor.YELLOW + "You have received a warning: " + reason);
                sender.sendMessage(PREFIX + ChatColor.GREEN + "Warning issued to " + target.getName() + ".");
            } else if (action.equals("mute")) {
                Date expiry = parseBanExpiry(args[1]);
                if (expiry == null) {
                    sender.sendMessage(PREFIX + ChatColor.RED + "Invalid duration. Examples: 10m, 2h, 1d.");
                    return true;
                }
                String reason = String.join(" ", Arrays.copyOfRange(args, 2, args.length));
                long until = expiry.getTime();
                mutedUntil.put(target.getUniqueId(), until);
                addPunishment(target, "MUTE", reason, actor, until);
                target.sendMessage(PREFIX + ChatColor.RED + "You have been muted for " + args[1] + ". Reason: " + reason);
                sender.sendMessage(PREFIX + ChatColor.GREEN + target.getName() + " muted for " + args[1] + ".");
            } else if (action.equals("unmute")) {
                mutedUntil.remove(target.getUniqueId());
                addPunishment(target, "UNMUTE", "Mute removed", actor, 0L);
                sender.sendMessage(PREFIX + ChatColor.GREEN + target.getName() + " has been unmuted.");
                target.sendMessage(PREFIX + ChatColor.GREEN + "Your mute has been removed.");
            } else {
                List<String> history = punishmentsData.getStringList("players." + target.getUniqueId());
                sender.sendMessage("§8§m---------------- STAFFJOBS HISTORY ----------------");
                sender.sendMessage("§ePunishment history for §f" + target.getName());
                if (history.isEmpty()) sender.sendMessage("§7No punishment records.");
                else for (String entry : history) sender.sendMessage("§7" + entry);
                sender.sendMessage("§8§m---------------------------------------------------");
            }
            savePunishments();
            return true;
        }

        if (command.getName().equalsIgnoreCase("report")) {
            if (!(sender instanceof Player player)) {
                sender.sendMessage("This command can only be used by players.");
                return true;
            }
            if (!player.hasPermission("staffjobs.report")) {
                player.sendMessage(PREFIX + ChatColor.RED + "You do not have permission.");
                return true;
            }
            if (args.length == 0 && !getConfig().getBoolean("settings.reports.allow-empty-message", false)) {
                player.sendMessage(PREFIX + ChatColor.RED + "Usage: /report <message>");
                return true;
            }
            String message = String.join(" ", args).trim();
            if (message.isEmpty() && !getConfig().getBoolean("settings.reports.allow-empty-message", false)) {
                player.sendMessage(PREFIX + ChatColor.RED + "Report message cannot be empty.");
                return true;
            }
            submitReport(player, message);
            return true;
        }
        return false;
    }


    private boolean canPunish(Player staff, Player target) {
        if (staff.getUniqueId().equals(target.getUniqueId())) return false;
        return staff.hasPermission("staffjobs.protect.bypass") || getRank(staff) > getRank(target);
    }

    private void loadPunishments() {
        punishmentsFile = new File(getDataFolder(), "punishments.yml");
        punishmentsData = YamlConfiguration.loadConfiguration(punishmentsFile);
        if (punishmentsData.getConfigurationSection("mute-expiry") != null) {
            for (String key : punishmentsData.getConfigurationSection("mute-expiry").getKeys(false)) {
                try {
                    long expiry = punishmentsData.getLong("mute-expiry." + key);
                    if (expiry > System.currentTimeMillis()) mutedUntil.put(UUID.fromString(key), expiry);
                } catch (IllegalArgumentException ignored) {
                    getLogger().warning("Invalid muted player UUID: " + key);
                }
            }
        }
    }

    private void savePunishments() {
        if (punishmentsData == null) return;
        punishmentsData.set("mute-expiry", null);
        for (Map.Entry<UUID, Long> entry : mutedUntil.entrySet()) {
            if (entry.getValue() > System.currentTimeMillis()) {
                punishmentsData.set("mute-expiry." + entry.getKey(), entry.getValue());
            }
        }
        saveFile(punishmentsData, punishmentsFile);
    }

    private void addPunishment(Player target, String type, String reason, String actor, long expiry) {
        String path = "players." + target.getUniqueId();
        List<String> history = punishmentsData.getStringList(path);
        String record = ZonedDateTime.now(getConfiguredZone()).format(dateFormatter) + " WIB | " + type
                + " | By: " + actor + " | Reason: " + reason
                + (expiry > 0 ? " | Expires: " + ZonedDateTime.ofInstant(
                        java.time.Instant.ofEpochMilli(expiry), getConfiguredZone()).format(dateFormatter) + " " + getZoneLabel() : "");
        history.add(record);
        int maxHistory = Math.max(1, getConfig().getInt("settings.punishments.max-history-per-player", 100));
        while (history.size() > maxHistory) history.remove(0);
        punishmentsData.set(path, history);
    }

    @EventHandler
    public void onMutedChat(AsyncPlayerChatEvent event) {
        Long expiry = mutedUntil.get(event.getPlayer().getUniqueId());
        if (expiry == null) return;
        if (expiry <= System.currentTimeMillis()) {
            mutedUntil.remove(event.getPlayer().getUniqueId());
            Bukkit.getScheduler().runTask(this, this::savePunishments);
            return;
        }
        event.setCancelled(true);
        String muteMessage = getConfig().getString("settings.punishments.mute-message", "You are muted and cannot chat.");
        event.getPlayer().sendMessage(PREFIX + ChatColor.RED + (muteMessage == null ? "You are muted and cannot chat." : muteMessage)
                + " (" + Math.max(1, (expiry - System.currentTimeMillis()) / 60000) + " minute(s) remaining).");
    }

    // REPORT SYSTEM

    private List<Map<?, ?>> getReports() {
        return reportsData.getMapList("reports");
    }

    private String reportValue(Map<?, ?> report, String key, String fallback) {
        Object value = report.get(key);
        return value == null ? fallback : String.valueOf(value);
    }

    private void submitReport(Player player, String message) {
        long now = System.currentTimeMillis();
        long cooldown = Math.max(0L, getConfig().getLong("settings.reports.cooldown-seconds", 60L)) * 1000L;
        long previous = lastReportAt.getOrDefault(player.getUniqueId(), 0L);
        if (cooldown > 0L && now - previous < cooldown) {
            long remaining = Math.max(1L, (cooldown - (now - previous) + 999L) / 1000L);
            player.sendMessage(PREFIX + ChatColor.RED + "Please wait " + remaining + " second(s) before submitting another report.");
            return;
        }
        for (Map<?, ?> report : getReports()) {
            if (player.getUniqueId().toString().equals(reportValue(report, "uuid", ""))) {
                player.sendMessage(PREFIX + ChatColor.RED + "You already have an unresolved report.");
                return;
            }
        }

        Map<String, Object> report = new LinkedHashMap<>();
        report.put("id", "R-" + Long.toString(now, 36).toUpperCase(Locale.ROOT) + "-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase(Locale.ROOT));
        report.put("uuid", player.getUniqueId().toString());
        report.put("player", player.getName());
        report.put("message", message);
        report.put("timestamp", now);
        report.put("date", ZonedDateTime.now(getConfiguredZone()).format(dateFormatter) + " " + getZoneLabel());
        report.put("status", "PENDING");
        report.put("assigned-staff", "Unassigned");

        List<Map<?, ?>> reports = new ArrayList<>(getReports());
        reports.add(report);
        int max = Math.max(1, getConfig().getInt("settings.max-reports", 100));
        while (reports.size() > max) reports.remove(0);
        reportsData.set("reports", reports);
        saveReports();
        lastReportAt.put(player.getUniqueId(), now);

        player.sendMessage(PREFIX + ChatColor.GREEN + "Your report has been sent to the staff team. ID: " + report.get("id"));
        if (getConfig().getBoolean("settings.reports.notify-staff", true)) {
            for (Player staff : Bukkit.getOnlinePlayers()) {
                if (staff.hasPermission("staffjobs.use")) {
                    if (notificationsData.getBoolean("preferences." + staff.getUniqueId() + ".chat", true)) {
                        staff.sendMessage(PREFIX + ChatColor.YELLOW + player.getName() + " submitted a new report!");
                        staff.sendMessage(ChatColor.GRAY + "Use /staffmenu to view reports.");
                    }
                    if (notificationsData.getBoolean("preferences." + staff.getUniqueId() + ".sound", true)) {
                        try {
                            Sound sound = Sound.valueOf(getConfig().getString("settings.notifications.sound", "BLOCK_NOTE_BLOCK_PLING").toUpperCase(Locale.ROOT));
                            staff.playSound(staff.getLocation(), sound, 0.8f, 1.2f);
                        } catch (IllegalArgumentException ignored) {
                            getLogger().warning("Invalid notification sound in config.yml.");
                        }
                    }
                }
            }
        }
    }

    private void openReportLogs(Player player, int page) {
        List<Map<?, ?>> reports = getReports();
        int maxPages = Math.max(1, (reports.size() + 44) / 45);
        page = Math.max(0, Math.min(page, maxPages - 1));
        reportPages.put(player.getUniqueId(), page);

        Inventory inventory = Bukkit.createInventory(null, 54, REPORTS + " §7(" + (page + 1) + "/" + maxPages + ")");
        int start = page * 45;
        int end = Math.min(start + 45, reports.size());

        for (int i = start; i < end; i++) {
            Map<?, ?> report = reports.get(i);
            inventory.setItem(i - start, createItem(
                    Material.PAPER,
                    ChatColor.YELLOW + reportValue(report, "player", "Unknown"),
                    ChatColor.GRAY + "Time: " + reportValue(report, "date", "Unknown"),
                    ChatColor.GRAY + "Report: " + reportValue(report, "message", ""),
                    "",
                    ChatColor.GREEN + "Click to view details."
            ));
        }
        inventory.setItem(45, createItem(Material.ARROW, "§ePrevious Page"));
        inventory.setItem(49, createItem(Material.BARRIER, "§cBack"));
        inventory.setItem(53, createItem(Material.ARROW, "§eNext Page"));
        player.openInventory(inventory);
    }

    private void openReportDetail(Player player, String id) {
        Map<?, ?> report = findReport(id);
        if (report == null) {
            player.sendMessage(PREFIX + ChatColor.RED + "Report not found.");
            openReportLogs(player, 0);
            return;
        }
        selectedReports.put(player.getUniqueId(), id);
        Inventory inventory = Bukkit.createInventory(null, 27, REPORT_DETAIL);
        inventory.setItem(13, createItem(
                Material.PAPER,
                ChatColor.YELLOW + "Report from " + reportValue(report, "player", "Unknown"),
                ChatColor.GRAY + "Time: " + reportValue(report, "date", "Unknown"),
                ChatColor.GRAY + "Message:",
                ChatColor.WHITE + reportValue(report, "message", "")
        ));
        inventory.setItem(15, createItem(Material.LIME_WOOL, "§aACCEPT REPORT", "§7Accept and archive this report."));
        inventory.setItem(16, createItem(Material.RED_WOOL, "§cREJECT REPORT", "§7Reject and archive this report."));
        inventory.setItem(17, createItem(Material.NAME_TAG, "§bASSIGN TO ME", "§7Assign this report to you."));
        inventory.setItem(11, createItem(Material.ARROW, "§eBack to report list"));
        player.openInventory(inventory);
    }

    private Map<?, ?> findReport(String id) {
        for (Map<?, ?> report : getReports()) {
            if (id.equals(reportValue(report, "id", ""))) return report;
        }
        return null;
    }

    private void completeReport(Player staff, String id) {
        List<Map<?, ?>> reports = new ArrayList<>(getReports());
        Map<?, ?> target = null;
        for (Map<?, ?> report : reports) {
            if (id.equals(reportValue(report, "id", ""))) {
                target = report;
                break;
            }
        }
        if (target == null) {
            staff.sendMessage(PREFIX + ChatColor.RED + "Report not found.");
            return;
        }

        String reporterName = reportValue(target, "player", "Unknown");
        String reporterUUID = reportValue(target, "uuid", "");
        Map<String, Object> archived = new LinkedHashMap<>();
        for (Map.Entry<?, ?> entry : target.entrySet()) archived.put(String.valueOf(entry.getKey()), entry.getValue());
        archived.put("status", "ACCEPTED");
        archived.put("assigned-staff", staff.getName());
        archived.put("resolved-timestamp", System.currentTimeMillis());
        archived.put("resolved-date", ZonedDateTime.now(getConfiguredZone()).format(dateFormatter) + " " + getZoneLabel());
        List<Map<?, ?>> history = new ArrayList<>(reportsData.getMapList("history"));
        history.add(archived);
        int historyLimit = Math.max(1, getConfig().getInt("settings.reports.max-history", 500));
        while (history.size() > historyLimit) history.remove(0);
        reportsData.set("history", history);
        reports.remove(target);
        reportsData.set("reports", reports);
        saveReports();

        String staffMessage = PREFIX + ChatColor.YELLOW + staff.getName()
                + " resolved the report from " + reporterName;
        for (Player online : Bukkit.getOnlinePlayers()) {
            if (online.hasPermission("staffjobs.use")) online.sendMessage(staffMessage);
        }

        String notification = PREFIX + ChatColor.GREEN + "Hello, the staff team has resolved your report.\n"
                + PREFIX + ChatColor.YELLOW + "If your issue has not been resolved, please submit another report.";
        try {
            UUID uuid = UUID.fromString(reporterUUID);
            Player reporter = Bukkit.getPlayer(uuid);
            if (reporter != null && reporter.isOnline()) {
                reporter.sendMessage(notification);
            } else {
                String path = "pending." + reporterUUID;
                List<String> pending = notificationsData.getStringList(path);
                pending.add(notification);
                notificationsData.set(path, pending);
                saveNotifications();
            }
        } catch (IllegalArgumentException exception) {
            getLogger().warning("Invalid reporter UUID: " + reporterUUID);
        }

        staff.sendMessage(PREFIX + ChatColor.GREEN + "Report from " + reporterName + " was resolved successfully.");
        openReportLogs(staff, 0);
    }

    private void rejectReport(Player staff, String id) {
        archiveReport(staff, id, "REJECTED", "Report rejected by " + staff.getName() + ".");
    }

    private void assignReport(Player staff, String id) {
        List<Map<?, ?>> reports = new ArrayList<>(getReports());
        for (Map<?, ?> raw : reports) {
            if (!id.equals(reportValue(raw, "id", ""))) continue;
            Map<String, Object> updated = new LinkedHashMap<>();
            for (Map.Entry<?, ?> entry : raw.entrySet()) updated.put(String.valueOf(entry.getKey()), entry.getValue());
            String assigned = reportValue(raw, "assigned-staff", "Unassigned");
            if (!assigned.equals("Unassigned") && !assigned.equalsIgnoreCase(staff.getName())) {
                staff.sendMessage(PREFIX + ChatColor.RED + "This report is already assigned to " + assigned + ".");
                return;
            }
            updated.put("assigned-staff", staff.getName());
            updated.put("assigned-timestamp", System.currentTimeMillis());
            reports.set(reports.indexOf(raw), updated);
            reportsData.set("reports", reports);
            saveReports();
            staff.sendMessage(PREFIX + ChatColor.GREEN + "Report " + id + " assigned to you.");
            return;
        }
        staff.sendMessage(PREFIX + ChatColor.RED + "Report not found.");
    }

    private void archiveReport(Player staff, String id, String status, String reporterMessage) {
        List<Map<?, ?>> reports = new ArrayList<>(getReports());
        Map<?, ?> target = null;
        for (Map<?, ?> report : reports) {
            if (id.equals(reportValue(report, "id", ""))) { target = report; break; }
        }
        if (target == null) {
            staff.sendMessage(PREFIX + ChatColor.RED + "Report not found.");
            return;
        }
        String reporterName = reportValue(target, "player", "Unknown");
        String reporterUUID = reportValue(target, "uuid", "");
        Map<String, Object> archived = new LinkedHashMap<>();
        for (Map.Entry<?, ?> entry : target.entrySet()) archived.put(String.valueOf(entry.getKey()), entry.getValue());
        archived.put("status", status);
        archived.put("assigned-staff", staff.getName());
        archived.put("resolved-timestamp", System.currentTimeMillis());
        archived.put("resolved-date", ZonedDateTime.now(getConfiguredZone()).format(dateFormatter) + " " + getZoneLabel());
        List<Map<?, ?>> history = new ArrayList<>(reportsData.getMapList("history"));
        history.add(archived);
        int limit = Math.max(1, getConfig().getInt("settings.reports.max-history", 500));
        while (history.size() > limit) history.remove(0);
        reportsData.set("history", history);
        reports.remove(target);
        reportsData.set("reports", reports);
        saveReports();
        logAction(staff, "REPORT_" + status, reporterName + " [" + id + "]");
        try {
            UUID uuid = UUID.fromString(reporterUUID);
            Player reporter = Bukkit.getPlayer(uuid);
            if (reporter != null) reporter.sendMessage(PREFIX + ChatColor.YELLOW + reporterMessage);
            else {
                String path = "pending." + reporterUUID;
                List<String> pending = notificationsData.getStringList(path);
                pending.add(PREFIX + ChatColor.YELLOW + reporterMessage);
                notificationsData.set(path, pending);
                saveNotifications();
            }
        } catch (IllegalArgumentException exception) {
            getLogger().warning("Invalid reporter UUID: " + reporterUUID);
        }
        staff.sendMessage(PREFIX + ChatColor.GREEN + "Report " + id + " " + status.toLowerCase(Locale.ROOT) + ".");
        openReportLogs(staff, 0);
    }

    private void removeExpiredReports() {
        long now = System.currentTimeMillis();
        long expirationHours = getConfig().getLong("settings.reports.expiration-hours", 0L);
        if (expirationHours <= 0L) return;
        long expiration = expirationHours * 60L * 60L * 1000L;
        List<Map<?, ?>> reports = new ArrayList<>(getReports());
        boolean changed = false;
        Iterator<Map<?, ?>> iterator = reports.iterator();

        while (iterator.hasNext()) {
            Map<?, ?> report = iterator.next();
            long timestamp;
            try {
                timestamp = Long.parseLong(reportValue(report, "timestamp", "0"));
            } catch (NumberFormatException exception) {
                timestamp = 0;
            }
            if (timestamp > 0 && now - timestamp >= expiration) {
                iterator.remove();
                changed = true;
            }
        }
        if (changed) {
            reportsData.set("reports", reports);
            saveReports();
            getLogger().info("Expired reports have been cleared.");
        }
    }

    // VERSION UPDATE CHECK

    private void checkForUpdates() {
        Bukkit.getScheduler().runTaskAsynchronously(this, () -> {
            HttpURLConnection connection = null;
            String foundVersion = null;
            try {
                String repositoryUrl = getConfig().getString("settings.update-check.repository", "");
                if (repositoryUrl == null || repositoryUrl.isBlank()) {
                    String apiVersion = getDescription().getAPIVersion();
                    String repository = apiVersion != null && apiVersion.startsWith("26.")
                            ? "STAFFJOBS-26.3" : "STAFFJOBS";
                    repositoryUrl = "https://raw.githubusercontent.com/alfonsowior7-dot/" + repository
                            + "/main/src/main/resources/plugin.yml";
                }
                URL url = new URL(repositoryUrl.trim());
                connection = (HttpURLConnection) url.openConnection();
                connection.setRequestMethod("GET");
                connection.setConnectTimeout(5000);
                connection.setReadTimeout(5000);
                connection.setRequestProperty("User-Agent", "STAFFJOBS-VersionChecker");
                if (connection.getResponseCode() == HttpURLConnection.HTTP_OK) {
                    try (BufferedReader reader = new BufferedReader(
                            new InputStreamReader(connection.getInputStream(), StandardCharsets.UTF_8))) {
                        String line;
                        Pattern pattern = Pattern.compile("^\\s*version\\s*:\\s*['\\\"]?([^'\\\"\\s]+)");
                        while ((line = reader.readLine()) != null) {
                            Matcher matcher = pattern.matcher(line);
                            if (matcher.find()) {
                                foundVersion = matcher.group(1).trim();
                                break;
                            }
                        }
                    }
                }
            } catch (Exception exception) {
                getLogger().warning("Could not check for updates: " + exception.getMessage());
            } finally {
                if (connection != null) connection.disconnect();
            }

            final String remoteVersion = foundVersion;
            Bukkit.getScheduler().runTask(this, () -> {
                latestVersion = remoteVersion;
                updateCheckCompleted = true;
                if (latestVersion == null || latestVersion.isBlank()) {
                    getLogger().warning("Version check could not retrieve the latest version.");
                    return;
                }
                for (Player online : Bukkit.getOnlinePlayers()) {
                    sendVersionNotice(online);
                }
                if (isNewerVersion(latestVersion, getDescription().getVersion())) {
                    getLogger().warning("A new version is available: " + latestVersion
                            + " (installed: " + getDescription().getVersion() + ").");
                }
            });
        });
    }

    private void sendVersionNotice(Player player) {
        if (!player.hasPermission("staffjobs.updatecheck")) return;
        if (latestVersion == null || latestVersion.isBlank()) return;
        String current = getDescription().getVersion();
        player.sendMessage("§8§m----------------------------------------");
        player.sendMessage("§b§lSTAFFJOBS §7Version Information");
        player.sendMessage("§7Installed version: §f" + current);
        if (isNewerVersion(latestVersion, current)) {
            player.sendMessage("§7Latest version: §a" + latestVersion + " §e(UPDATE AVAILABLE)");
            player.sendMessage("§ePlease ask an administrator to update STAFFJOBS.");
        } else {
            player.sendMessage("§7Latest version: §a" + latestVersion + " §a(UP TO DATE)");
        }
        player.sendMessage("§8§m----------------------------------------");
    }

    private boolean isNewerVersion(String latest, String current) {
        try {
            String[] latestParts = latest.replaceFirst("^[vV]", "").split("[.-]");
            String[] currentParts = current.replaceFirst("^[vV]", "").split("[.-]");
            int length = Math.max(latestParts.length, currentParts.length);
            for (int i = 0; i < length; i++) {
                int latestPart = i < latestParts.length ? Integer.parseInt(latestParts[i]) : 0;
                int currentPart = i < currentParts.length ? Integer.parseInt(currentParts[i]) : 0;
                if (latestPart != currentPart) return latestPart > currentPart;
            }
        } catch (NumberFormatException ignored) {
            return !latest.equalsIgnoreCase(current);
        }
        return false;
    }

    // OFFLINE NOTIFICATIONS

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        if (updateCheckCompleted) {
            sendVersionNotice(player);
        }
        lastActivity.put(player.getUniqueId(), System.currentTimeMillis());
        afkPlayers.remove(player.getUniqueId());
        String staffPath = "staff." + player.getUniqueId();
        staffStatusData.set(staffPath + ".name", player.getName());
        saveStaffStatus();
        if (frozen.contains(player.getUniqueId())) {
            player.sendMessage(PREFIX + ChatColor.RED + "You are still FROZEN by a staff member.");
        }

        String path = "pending." + player.getUniqueId();
        List<String> pending = notificationsData.getStringList(path);
        if (!pending.isEmpty()) {
            for (String message : pending) player.sendMessage(message);
            notificationsData.set(path, null);
            saveNotifications();
        }
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        if (player.hasPermission("staffjobs.use")) {
            String path = "staff." + player.getUniqueId();
            staffStatusData.set(path + ".name", player.getName());
            staffStatusData.set(path + ".last-online", System.currentTimeMillis());
            saveStaffStatus();
        }
        lastActivity.remove(player.getUniqueId());
        afkPlayers.remove(player.getUniqueId());
    }

    @EventHandler
    public void onStaffActivity(PlayerMoveEvent event) {
        Player player = event.getPlayer();
        if (!player.hasPermission("staffjobs.use")) return;
        if (event.getTo() != null && (event.getFrom().getBlockX() != event.getTo().getBlockX()
                || event.getFrom().getBlockY() != event.getTo().getBlockY()
                || event.getFrom().getBlockZ() != event.getTo().getBlockZ())) {
            markStaffActivity(player);
        }
    }

    @EventHandler public void onStaffChatActivity(AsyncPlayerChatEvent event) { markStaffActivity(event.getPlayer()); }
    @EventHandler public void onStaffInteractActivity(PlayerInteractEvent event) { markStaffActivity(event.getPlayer()); }
    @EventHandler public void onStaffEntityInteractActivity(PlayerInteractEntityEvent event) { markStaffActivity(event.getPlayer()); }
    @EventHandler public void onStaffCommandActivity(PlayerCommandPreprocessEvent event) { markStaffActivity(event.getPlayer()); }
    @EventHandler public void onStaffBlockBreakActivity(BlockBreakEvent event) { markStaffActivity(event.getPlayer()); }
    @EventHandler public void onStaffBlockPlaceActivity(BlockPlaceEvent event) { markStaffActivity(event.getPlayer()); }
    @EventHandler public void onStaffInventoryActivity(InventoryClickEvent event) {
        if (event.getWhoClicked() instanceof Player player) markStaffActivity(player);
    }

    private void markStaffActivity(Player player) {
        if (!player.hasPermission("staffjobs.use")) return;
        lastActivity.put(player.getUniqueId(), System.currentTimeMillis());
        afkPlayers.remove(player.getUniqueId());
    }

    // STAFF MENU

    private void openStaffMenu(Player player) {
        Inventory inventory = Bukkit.createInventory(null, 54, MENU);
        inventory.setItem(10, createItem(Material.PLAYER_HEAD, "§eManage Players"));
        inventory.setItem(13, createItem(Material.BOOK, "§eReports", "§7Total reports: " + getReports().size()));
        inventory.setItem(16, createItem(Material.WRITABLE_BOOK, "§eStaff Activity Logs"));
        inventory.setItem(22, createItem(Material.TOTEM_OF_UNDYING, "§cDeath History"));
        inventory.setItem(28, createItem(Material.NAME_TAG, "§bOnline Staff", "§7View currently connected staff."));
        inventory.setItem(34, createItem(Material.BELL, "§eNotification Settings", "§7Configure your staff alerts."));
        inventory.setItem(31, createItem(Material.BARRIER, "§cClose Menu"));
        inventory.setItem(53, createItem(Material.ARROW, "§eNext Page"));
        player.openInventory(inventory);
    }

    private void openStaffMenuPageTwo(Player player) {
        Inventory inventory = Bukkit.createInventory(null, 54, MENU_XRAY);
        boolean enabled = xrayEnabled.contains(player.getUniqueId());
        inventory.setItem(22, createItem(enabled ? Material.LIME_DYE : Material.GRAY_DYE,
                "§eX-Ray: " + (enabled ? "§aENABLED" : "§cDISABLED"),
                "§7Toggle ore visibility through nearby blocks.",
                "§7Visual only; world blocks are not changed."));
        inventory.setItem(45, createItem(Material.ARROW, "§ePrevious Page"));
        inventory.setItem(49, createItem(Material.BARRIER, "§cClose Menu"));
        player.openInventory(inventory);
    }

    private void openPlayers(Player player) {
        openPlayers(player, playerPages.getOrDefault(player.getUniqueId(), 0));
    }

    private void openPlayers(Player player, int requestedPage) {
        List<Player> online = new ArrayList<>(Bukkit.getOnlinePlayers());
        int pages = Math.max(1, (online.size() + 44) / 45);
        int page = Math.max(0, Math.min(requestedPage, pages - 1));
        playerPages.put(player.getUniqueId(), page);
        Inventory inventory = Bukkit.createInventory(null, 54, PLAYERS);
        int start = page * 45;
        for (int i = start; i < Math.min(online.size(), start + 45); i++) {
            Player target = online.get(i);
            inventory.setItem(i - start, createItem(Material.PLAYER_HEAD, "§e" + target.getName(), "§7Click to manage this player."));
        }
        inventory.setItem(45, createItem(Material.ARROW, "§ePrevious Page"));
        inventory.setItem(49, createItem(Material.ARROW, "§eBack", "§7Page " + (page + 1) + "/" + pages));
        inventory.setItem(53, createItem(Material.ARROW, "§eNext Page"));
        player.openInventory(inventory);
    }

    private void openActions(Player staff, Player target) {
        selected.put(staff.getUniqueId(), target.getUniqueId());
        Inventory inventory = Bukkit.createInventory(null, 27, ACTIONS);
        inventory.setItem(10, createItem(Material.IRON_SWORD, "§cKick Player", "§7Click, then enter a reason in chat."));
        inventory.setItem(12, createItem(Material.BARRIER, "§4Permanent Ban", "§7Click, then enter a reason in chat."));
        inventory.setItem(14, createItem(
                frozen.contains(target.getUniqueId()) ? Material.LIME_WOOL : Material.ICE,
                frozen.contains(target.getUniqueId()) ? "§aUnfreeze Player" : "§bFreeze Player"
        ));
        inventory.setItem(16, createItem(Material.CLOCK, "§6Temporary Ban", "§7Enter a reason and duration in chat.", "§7Example duration: 30m, 2h, 7d."));
        inventory.setItem(20, createItem(Material.CHEST, "§bView Inventory (InvSee)", "§7View and manage this player's inventory.", "§7Requires: staffjobs.invsee"));
        inventory.setItem(22, createItem(Material.ARROW, "§eBack"));
        staff.openInventory(inventory);
    }

    private void openConfirm(Player staff, String action) {
        pendingAction.put(staff.getUniqueId(), action);
        Inventory inventory = Bukkit.createInventory(null, 27, CONFIRM);
        inventory.setItem(11, createItem(Material.LIME_WOOL, "§aCONFIRM"));
        inventory.setItem(15, createItem(Material.RED_WOOL, "§cCANCEL"));
        staff.openInventory(inventory);
    }

    // INVENTORY EVENTS

    @EventHandler
    public void onStaffLogsInventoryDrag(InventoryDragEvent event) {
        String title = event.getView().getTitle();
        if (title.equals("§8Staff Activity Logs") || title.equals(STAFF_HISTORY_DETAIL)
                || title.equals(MENU) || title.equals(MENU_XRAY)) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;
        String title = event.getView().getTitle();

        if (frozen.contains(player.getUniqueId())
                && getConfig().getBoolean("settings.freeze.prevent-interaction", true)) {
            event.setCancelled(true);
            return;
        }

        if (!title.equals(MENU) && !title.equals(MENU_XRAY) && !title.startsWith(REPORTS) && !title.equals(REPORT_DETAIL)
                && !title.equals(PLAYERS) && !title.equals(ACTIONS) && !title.equals(CONFIRM)
                && !title.equals("§8Staff Activity Logs") && !title.equals(STAFF_HISTORY_DETAIL)
                && !title.startsWith("§8Death History") && !title.equals("§8Death Rollback")
                && !title.equals(ONLINE_STAFF) && !title.equals(NOTIFICATION_SETTINGS)) return;

        event.setCancelled(true);
        if (!player.hasPermission("staffjobs.use")) {
            player.closeInventory();
            return;
        }

        int slot = event.getRawSlot();
        if (slot < 0 || slot >= event.getInventory().getSize()) return;

        if (title.equals(MENU)) {
            if (slot == 53) openStaffMenuPageTwo(player);
            else if (slot == 10) openPlayers(player);
            else if (slot == 13) openReportLogs(player, 0);
            else if (slot == 16) openActivityLogs(player, 0);
            else if (slot == 22) openDeathHistory(player, 0);
            else if (slot == 28) openOnlineStaff(player);
            else if (slot == 34) openNotificationSettings(player);
            else if (slot == 31) player.closeInventory();
            return;
        }

        if (title.equals(MENU_XRAY)) {
            if (slot == 45) openStaffMenu(player);
            else if (slot == 49) player.closeInventory();
            else if (slot == 22) {
                if (!player.hasPermission("staffjobs.xray")) {
                    player.sendMessage(PREFIX + ChatColor.RED + "You do not have permission to use X-Ray.");
                    return;
                }
                if (xrayEnabled.contains(player.getUniqueId())) disableXray(player, true);
                else enableXray(player);
                openStaffMenuPageTwo(player);
            }
            return;
        }

        if (title.startsWith(REPORTS)) {
            if (slot == 49) {
                openStaffMenu(player);
                return;
            }
            int page = reportPages.getOrDefault(player.getUniqueId(), 0);
            if (slot == 45) {
                openReportLogs(player, page - 1);
                return;
            }
            if (slot == 53) {
                openReportLogs(player, page + 1);
                return;
            }
            ItemStack clicked = event.getCurrentItem();
            if (clicked == null || clicked.getType() != Material.PAPER) return;
            List<Map<?, ?>> reports = getReports();
            int index = page * 45 + slot;
            if (index >= 0 && index < reports.size()) {
                openReportDetail(player, reportValue(reports.get(index), "id", ""));
            }
            return;
        }

        if (title.equals(REPORT_DETAIL)) {
            if (slot == 11) openReportLogs(player, 0);
            else if (slot == 15) {
                String id = selectedReports.get(player.getUniqueId());
                if (id != null) completeReport(player, id);
            } else if (slot == 16) {
                String id = selectedReports.get(player.getUniqueId());
                if (id != null) rejectReport(player, id);
            } else if (slot == 17) {
                String id = selectedReports.get(player.getUniqueId());
                if (id != null) assignReport(player, id);
            }
            return;
        }

        if (title.equals(ONLINE_STAFF)) {
            if (slot == 49) openStaffMenu(player);
            else if (slot == 48) openOnlineStaff(player);
            return;
        }
        if (title.equals(NOTIFICATION_SETTINGS)) {
            if (slot == 11) {
                String path = "preferences." + player.getUniqueId() + ".chat";
                notificationsData.set(path, !notificationsData.getBoolean(path, true));
                saveNotifications(); openNotificationSettings(player);
            } else if (slot == 15) {
                String path = "preferences." + player.getUniqueId() + ".sound";
                notificationsData.set(path, !notificationsData.getBoolean(path, true));
                saveNotifications(); openNotificationSettings(player);
            } else if (slot == 22) openStaffMenu(player);
            return;
        }
        if (title.equals(STAFF_HISTORY_DETAIL)) {
            String staffName = selectedHistoryStaff.get(player.getUniqueId());
            if (staffName == null) { openActivityLogs(player, 0); return; }
            int page = staffHistoryPages.getOrDefault(player.getUniqueId(), 0);
            if (slot == 45) { openStaffHistory(player, staffName, page - 1); return; }
            if (slot == 48) { openStaffHistory(player, staffName, page); return; }
            if (slot == 49) { openActivityLogs(player, activityPages.getOrDefault(player.getUniqueId(), 0)); return; }
            if (slot == 53) { openStaffHistory(player, staffName, page + 1); return; }
            return;
        }

        if (title.startsWith("§8Staff Activity Logs")) {
            int page = activityPages.getOrDefault(player.getUniqueId(), 0);
            if (slot == 45) { openActivityLogs(player, page - 1); return; }
            if (slot == 48) { openActivityLogs(player, page); return; }
            if (slot == 49) { openStaffMenu(player); return; }
            if (slot == 53) { openActivityLogs(player, page + 1); return; }
            ItemStack clicked = event.getCurrentItem();
            if (clicked == null || clicked.getType() != Material.WRITABLE_BOOK) return;
            int index = page * 45 + slot;
            List<String> names = getLoggedStaffNames();
            if (index >= 0 && index < names.size()) openStaffHistory(player, names.get(index), 0);
            return;
        }

        if (title.startsWith("§8Death History")) {
            if (slot == 49) { openStaffMenu(player); return; }
            int page = deathPages.getOrDefault(player.getUniqueId(), 0);
            if (slot == 45) { openDeathHistory(player, page - 1); return; }
            if (slot == 53) { openDeathHistory(player, page + 1); return; }
            ItemStack clicked = event.getCurrentItem();
            if (clicked == null || clicked.getType() != Material.SKELETON_SKULL) return;
            List<String> ids = new ArrayList<>(deathsData.getConfigurationSection("deaths") == null
                    ? Collections.emptySet() : deathsData.getConfigurationSection("deaths").getKeys(false));
            Collections.reverse(ids);
            int index = page * 45 + slot;
            if (index >= 0 && index < ids.size()) openDeathDetail(player, ids.get(index));
            return;
        }

        if (title.equals("§8Death Rollback")) {
            if (slot == 50) openDeathHistory(player, deathPages.getOrDefault(player.getUniqueId(), 0));
            else if (slot == 48) {
                String id = deathSelection.get(player.getUniqueId());
                if (id != null) restoreDeath(player, id);
                player.closeInventory();
            }
            return;
        }

        if (title.equals(PLAYERS)) {
            int page = playerPages.getOrDefault(player.getUniqueId(), 0);
            if (slot == 45) { openPlayers(player, page - 1); return; }
            if (slot == 49) { openStaffMenu(player); return; }
            if (slot == 53) { openPlayers(player, page + 1); return; }
            if (slot >= 45) return;
            ItemStack clicked = event.getCurrentItem();
            if (clicked == null || clicked.getType() != Material.PLAYER_HEAD) return;
            List<Player> online = new ArrayList<>(Bukkit.getOnlinePlayers());
            int index = page * 45 + slot;
            if (index < online.size()) {
                Player target = online.get(index);
                if (!canManage(player, target)) {
                    player.sendMessage(PREFIX + ChatColor.RED + "That player's rank is too high.");
                    return;
                }
                openActions(player, target);
            }
            return;
        }

        if (title.equals(ACTIONS)) {
            UUID targetUUID = selected.get(player.getUniqueId());
            if (targetUUID == null) return;
            Player target = Bukkit.getPlayer(targetUUID);
            if (target == null) {
                player.sendMessage(PREFIX + ChatColor.RED + "That player is offline.");
                openPlayers(player);
                return;
            }
            if (slot == 10) beginModerationChat(player, "kick");
            else if (slot == 12) beginModerationChat(player, "ban");
            else if (slot == 16) beginModerationChat(player, "tempban");
            else if (slot == 14) {
                if (!player.hasPermission("staffjobs.freeze")) {
                    player.sendMessage(PREFIX + ChatColor.RED + "You do not have permission to freeze players.");
                    return;
                }
                if (player.getUniqueId().equals(target.getUniqueId())) {
                    player.sendMessage(PREFIX + ChatColor.RED + "You cannot freeze yourself.");
                    return;
                }
                toggleFreeze(target);
                openActions(player, target);
            } else if (slot == 20) {
                if (!player.hasPermission("staffjobs.invsee")) {
                    player.sendMessage(PREFIX + ChatColor.RED + "You do not have permission to use InvSee.");
                    return;
                }
                if (!canManage(player, target)) {
                    player.sendMessage(PREFIX + ChatColor.RED + "You cannot inspect that player's inventory.");
                    openPlayers(player);
                    return;
                }
                if (player.getUniqueId().equals(target.getUniqueId())) {
                    player.sendMessage(PREFIX + ChatColor.RED + "You cannot use InvSee on yourself.");
                    return;
                }
                logAction(player, "INVSEE", target.getName());
                player.openInventory(target.getInventory());
                player.sendMessage(PREFIX + ChatColor.GREEN + "Opened " + target.getName() + "'s inventory.");
            } else if (slot == 22) openPlayers(player);
            return;
        }

        if (title.equals(CONFIRM)) {
            if (slot == 15) {
                pendingAction.remove(player.getUniqueId());
                openStaffMenu(player);
                return;
            }
            if (slot != 11) return;
            UUID targetUUID = selected.get(player.getUniqueId());
            String action = pendingAction.remove(player.getUniqueId());
            if (targetUUID == null || action == null) return;
            Player target = Bukkit.getPlayer(targetUUID);
            if (target == null) {
                player.sendMessage(PREFIX + ChatColor.RED + "That player is offline.");
                openPlayers(player);
                return;
            }
            if (!canManage(player, target)) {
                player.sendMessage(PREFIX + ChatColor.RED + "You cannot manage that player.");
                openStaffMenu(player);
                return;
            }
            if (action.equals("kick")) {
                logAction(player, "KICK", target.getName());
                target.kickPlayer(ChatColor.RED + "You were kicked by a staff member.");
                player.sendMessage(PREFIX + ChatColor.GREEN + "Player was kicked successfully.");
            } else if (action.equals("ban")) {
                logAction(player, "BAN", target.getName());
                Bukkit.getBanList(BanList.Type.NAME).addBan(target.getName(), "Banned by a staff member", null, player.getName());
                target.kickPlayer(ChatColor.RED + "You were banned by a staff member.");
                player.sendMessage(PREFIX + ChatColor.GREEN + "Player was banned successfully.");
            }
            openStaffMenu(player);
        }
    }

    // CHAT-BASED KICK / BAN SYSTEM

    private void beginModerationChat(Player staff, String action) {
        pendingAction.put(staff.getUniqueId(), action + "_reason");
        pendingReason.remove(staff.getUniqueId());
        staff.closeInventory();
        String targetName = "the selected player";
        UUID targetId = selected.get(staff.getUniqueId());
        if (targetId != null) {
            Player target = Bukkit.getPlayer(targetId);
            if (target != null) targetName = target.getName();
        }
        staff.sendMessage(PREFIX + ChatColor.YELLOW + "Enter the reason for " + targetName + " in chat.");
        staff.sendMessage(ChatColor.GRAY + "Type 'cancel' to cancel this action.");
    }

    @EventHandler
    public void onModerationChat(AsyncPlayerChatEvent event) {
        Player staff = event.getPlayer();
        UUID staffId = staff.getUniqueId();
        String state = pendingAction.get(staffId);
        if (state == null || !(state.endsWith("_reason") || state.equals("tempban_duration"))) return;

        event.setCancelled(true);
        String input = event.getMessage().trim();
        Bukkit.getScheduler().runTask(this, () -> {
            if (!pendingAction.containsKey(staffId)) return;
            if (input.equalsIgnoreCase("cancel")) {
                pendingAction.remove(staffId);
                pendingReason.remove(staffId);
                staff.sendMessage(PREFIX + ChatColor.YELLOW + "Moderation action cancelled.");
                return;
            }
            if (input.isEmpty()) {
                staff.sendMessage(PREFIX + ChatColor.RED + "Input cannot be empty. Type 'cancel' to cancel.");
                return;
            }

            if (state.equals("tempban_reason")) {
                pendingReason.put(staffId, input);
                pendingAction.put(staffId, "tempban_duration");
                staff.sendMessage(PREFIX + ChatColor.YELLOW + "Now enter the temporary ban duration.");
                staff.sendMessage(ChatColor.GRAY + "Examples: 10s, 5m, 2h, 7d, 2w, 1mo, 1y.");
                return;
            }

            if (state.equals("tempban_duration")) {
                Date expiry = parseBanExpiry(input);
                if (expiry == null) {
                    staff.sendMessage(PREFIX + ChatColor.RED + "Invalid duration. Use formats such as 10s, 5m, 2h, 7d, 2w, 1mo or 1y.");
                    return;
                }
                String reason = pendingReason.getOrDefault(staffId, "No reason provided");
                pendingAction.remove(staffId);
                pendingReason.remove(staffId);
                executeModeration(staff, "tempban", reason, expiry, input);
                return;
            }

            String action = state.substring(0, state.length() - "_reason".length());
            pendingAction.remove(staffId);
            pendingReason.remove(staffId);
            executeModeration(staff, action, input, null, null);
        });
    }

    private Date parseBanExpiry(String input) {
        String normalized = input.replaceAll("\\s+", "").toLowerCase(Locale.ROOT);
        Matcher matcher = Pattern.compile("^(\\d+)(s|sec|secs|second|seconds|detik|m|min|mins|minute|minutes|menit|h|hr|hrs|hour|hours|jam|d|day|days|hari|w|week|weeks|minggu|mo|month|months|bulan|y|year|years|tahun)$")
                .matcher(normalized);
        if (!matcher.matches()) return null;
        try {
            long amount = Long.parseLong(matcher.group(1));
            if (amount <= 0) return null;
            String unit = matcher.group(2);
            ZonedDateTime expiry = ZonedDateTime.now(getConfiguredZone());
            switch (unit) {
                case "s", "sec", "secs", "second", "seconds", "detik" -> expiry = expiry.plusSeconds(amount);
                case "m", "min", "mins", "minute", "minutes", "menit" -> expiry = expiry.plusMinutes(amount);
                case "h", "hr", "hrs", "hour", "hours", "jam" -> expiry = expiry.plusHours(amount);
                case "d", "day", "days", "hari" -> expiry = expiry.plusDays(amount);
                case "w", "week", "weeks", "minggu" -> expiry = expiry.plusWeeks(amount);
                case "mo", "month", "months", "bulan" -> expiry = expiry.plusMonths(amount);
                case "y", "year", "years", "tahun" -> expiry = expiry.plusYears(amount);
                default -> { return null; }
            }
            return Date.from(expiry.toInstant());
        } catch (RuntimeException exception) {
            return null;
        }
    }

    private void executeModeration(Player staff, String action, String reason, Date expiry, String durationText) {
        UUID targetId = selected.get(staff.getUniqueId());
        Player target = targetId == null ? null : Bukkit.getPlayer(targetId);
        if (target == null) {
            staff.sendMessage(PREFIX + ChatColor.RED + "The selected player is no longer online.");
            openPlayers(staff);
            return;
        }
        if (!canPunish(staff, target)) {
            staff.sendMessage(PREFIX + ChatColor.RED + "Self-punishment and equal/higher-rank punishment are disabled.");
            openStaffMenu(staff);
            return;
        }
        String permission = action.equals("kick") ? "staffjobs.kick" : "staffjobs.ban";
        if (!staff.hasPermission(permission)) {
            staff.sendMessage(PREFIX + ChatColor.RED + "You do not have permission for this action.");
            openActions(staff, target);
            return;
        }

        String cleanReason = ChatColor.stripColor(reason);
        if (cleanReason == null || cleanReason.isBlank()) cleanReason = "No reason provided";
        String kickMessage = ChatColor.RED + "You were " + (action.equals("kick") ? "kicked" : "banned")
                + " by " + staff.getName() + ".\n" + ChatColor.GRAY + "Reason: " + cleanReason;

        if (action.equals("kick")) {
            logAction(staff, "KICK", target.getName() + " | Reason: " + cleanReason);
            target.kickPlayer(kickMessage);
            staff.sendMessage(PREFIX + ChatColor.GREEN + target.getName() + " was kicked. Reason: " + cleanReason);
        } else if (action.equals("ban")) {
            Bukkit.getBanList(BanList.Type.NAME).addBan(target.getName(), cleanReason, null, staff.getName());
            logAction(staff, "BAN", target.getName() + " | Reason: " + cleanReason);
            target.kickPlayer(kickMessage);
            staff.sendMessage(PREFIX + ChatColor.GREEN + target.getName() + " was permanently banned. Reason: " + cleanReason);
        } else if (action.equals("tempban")) {
            Bukkit.getBanList(BanList.Type.NAME).addBan(target.getName(), cleanReason, expiry, staff.getName());
            logAction(staff, "TEMPBAN", target.getName() + " | Duration: " + durationText + " | Reason: " + cleanReason);
            target.kickPlayer(ChatColor.RED + "You were temporarily banned by " + staff.getName()
                    + ".\n" + ChatColor.GRAY + "Duration: " + durationText + "\nReason: " + cleanReason);
            staff.sendMessage(PREFIX + ChatColor.GREEN + target.getName() + " was temporarily banned for "
                    + durationText + ". Reason: " + cleanReason);
        }
        openStaffMenu(staff);
    }

    @EventHandler
    public void onFrozenInventoryDrag(InventoryDragEvent event) {
        if (event.getWhoClicked() instanceof Player player
                && frozen.contains(player.getUniqueId())
                && getConfig().getBoolean("settings.freeze.prevent-interaction", true)) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onFrozenSwapHands(PlayerSwapHandItemsEvent event) {
        if (frozen.contains(event.getPlayer().getUniqueId())
                && getConfig().getBoolean("settings.freeze.prevent-interaction", true)) event.setCancelled(true);
    }

    @EventHandler
    public void onFrozenHotbarChange(PlayerItemHeldEvent event) {
        if (frozen.contains(event.getPlayer().getUniqueId())
                && getConfig().getBoolean("settings.freeze.prevent-interaction", true)) event.setCancelled(true);
    }

    // FREEZE SYSTEM

    private void toggleFreeze(Player target) {
        UUID uuid = target.getUniqueId();
        if (frozen.contains(uuid)) {
            frozen.remove(uuid);
            target.sendMessage(PREFIX + ChatColor.GREEN + "Your freeze has been lifted.");
        } else {
            frozen.add(uuid);
            target.sendMessage(PREFIX + ChatColor.RED + "You have been frozen by a staff member.");
        }
        saveFrozen();
    }

    @EventHandler
    public void onMove(PlayerMoveEvent event) {
        Player player = event.getPlayer();
        if (!frozen.contains(player.getUniqueId()) || event.getTo() == null) return;
        if (getConfig().getBoolean("settings.freeze.prevent-movement", true)) {
            if (event.getFrom().getBlockX() != event.getTo().getBlockX()
                    || event.getFrom().getBlockY() != event.getTo().getBlockY()
                    || event.getFrom().getBlockZ() != event.getTo().getBlockZ()) {
                event.setTo(event.getFrom());
            }
        }
    }

    @EventHandler
    public void onAttack(EntityDamageByEntityEvent event) {
        Player attacker = null;
        if (event.getDamager() instanceof Player player) {
            attacker = player;
        } else if (event.getDamager() instanceof Projectile projectile
                && projectile.getShooter() instanceof Player player) {
            attacker = player;
        }
        if (attacker != null && frozen.contains(attacker.getUniqueId())
                && getConfig().getBoolean("settings.freeze.prevent-attack", true)) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onFrozenInteract(PlayerInteractEvent event) {
        if (frozen.contains(event.getPlayer().getUniqueId())
                && getConfig().getBoolean("settings.freeze.prevent-interaction", true)) event.setCancelled(true);
    }

    @EventHandler
    public void onFrozenEntityInteract(PlayerInteractEntityEvent event) {
        if (frozen.contains(event.getPlayer().getUniqueId())
                && getConfig().getBoolean("settings.freeze.prevent-interaction", true)) event.setCancelled(true);
    }

    @EventHandler
    public void onFrozenConsume(PlayerItemConsumeEvent event) {
        if (frozen.contains(event.getPlayer().getUniqueId())
                && getConfig().getBoolean("settings.freeze.prevent-interaction", true)) event.setCancelled(true);
    }

    @EventHandler
    public void onFrozenBreak(BlockBreakEvent event) {
        if (frozen.contains(event.getPlayer().getUniqueId())
                && getConfig().getBoolean("settings.freeze.prevent-interaction", true)) event.setCancelled(true);
    }

    @EventHandler
    public void onFrozenPlace(BlockPlaceEvent event) {
        if (frozen.contains(event.getPlayer().getUniqueId())
                && getConfig().getBoolean("settings.freeze.prevent-interaction", true)) event.setCancelled(true);
    }

    @EventHandler
    public void onFrozenDrop(PlayerDropItemEvent event) {
        if (frozen.contains(event.getPlayer().getUniqueId())
                && getConfig().getBoolean("settings.freeze.prevent-interaction", true)) event.setCancelled(true);
    }

    @EventHandler
    public void onFrozenPickup(PlayerPickupItemEvent event) {
        if (frozen.contains(event.getPlayer().getUniqueId())
                && getConfig().getBoolean("settings.freeze.prevent-interaction", true)) event.setCancelled(true);
    }

    // DEATH ROLLBACK: snapshot is captured before Bukkit drops/clears inventory.
    @EventHandler
    public void onDeath(PlayerDeathEvent event) {
        Player player = event.getEntity();
        String id = UUID.randomUUID().toString();
        String base = "deaths." + id;
        PlayerInventory inv = player.getInventory();
        deathsData.set(base + ".uuid", player.getUniqueId().toString());
        deathsData.set(base + ".player", player.getName());
        deathsData.set(base + ".time", ZonedDateTime.now(getConfiguredZone()).format(dateFormatter) + " " + getZoneLabel());
        deathsData.set(base + ".world", player.getWorld().getName());
        deathsData.set(base + ".x", player.getLocation().getBlockX());
        deathsData.set(base + ".y", player.getLocation().getBlockY());
        deathsData.set(base + ".z", player.getLocation().getBlockZ());
        deathsData.set(base + ".cause", event.getDeathMessage() == null ? "Unknown" : ChatColor.stripColor(event.getDeathMessage()));
        deathsData.set(base + ".inventory", new ArrayList<>(Arrays.asList(inv.getStorageContents())));
        deathsData.set(base + ".armor", new ArrayList<>(Arrays.asList(inv.getArmorContents())));
        deathsData.set(base + ".offhand", inv.getItemInOffHand());
        deathsData.set(base + ".level", player.getLevel());
        deathsData.set(base + ".exp", player.getExp());
        deathsData.set(base + ".total-exp", player.getTotalExperience());
        trimDeathHistory();
        saveDeaths();

        String notice = PREFIX + ChatColor.RED + "PLAYER DEATH: " + ChatColor.YELLOW + player.getName()
                + ChatColor.GRAY + " | World: " + player.getWorld().getName()
                + " | Location: " + player.getLocation().getBlockX() + ", " + player.getLocation().getBlockY() + ", " + player.getLocation().getBlockZ()
                + " | Cause: " + (event.getDeathMessage() == null ? "Unknown" : ChatColor.stripColor(event.getDeathMessage()))
                + " | Time: " + ZonedDateTime.now(getConfiguredZone()).format(DateTimeFormatter.ofPattern("HH:mm:ss")) + " " + getZoneLabel();
        if (getConfig().getBoolean("settings.death-notifications.enabled", true)) {
            String permission = getConfig().getString("settings.death-notifications.permission", "staffjobs.deathnotify");
            if (permission == null || permission.isBlank()) permission = "staffjobs.deathnotify";
            for (Player staff : Bukkit.getOnlinePlayers()) {
                if (staff.hasPermission(permission)) staff.sendMessage(notice);
            }
        }
        logAction(null, "PLAYER_DEATH", player.getName() + " [" + id + "]");
    }

    private void openDeathHistory(Player staff, int page) {
        List<String> ids = new ArrayList<>(deathsData.getConfigurationSection("deaths") == null
                ? Collections.emptySet() : deathsData.getConfigurationSection("deaths").getKeys(false));
        Collections.reverse(ids);
        int pages = Math.max(1, (ids.size() + 44) / 45);
        page = Math.max(0, Math.min(page, pages - 1));
        deathPages.put(staff.getUniqueId(), page);
        Inventory inv = Bukkit.createInventory(null, 54, "§8Death History §7(" + (page + 1) + "/" + pages + ")");
        for (int i = page * 45; i < Math.min(ids.size(), (page + 1) * 45); i++) {
            String id = ids.get(i);
            String base = "deaths." + id;
            inv.setItem(i - page * 45, createItem(Material.SKELETON_SKULL,
                    "§c" + deathsData.getString(base + ".player", "Unknown"),
                    "§7Time: " + deathsData.getString(base + ".time", ""),
                    "§7World: " + deathsData.getString(base + ".world", ""),
                    "§7Location: " + deathsData.getInt(base + ".x") + ", " + deathsData.getInt(base + ".y") + ", " + deathsData.getInt(base + ".z"),
                    "§eClick to select and restore."));
        }
        inv.setItem(45, createItem(Material.ARROW, "§ePrevious"));
        inv.setItem(49, createItem(Material.ARROW, "§eBack"));
        inv.setItem(53, createItem(Material.ARROW, "§eNext"));
        staff.openInventory(inv);
    }

    private void openDeathDetail(Player staff, String id) {
        String base = "deaths." + id;
        deathSelection.put(staff.getUniqueId(), id);
        Inventory inv = Bukkit.createInventory(null, 54, "§8Death Rollback");

        // Display the saved inventory contents so staff can review them before restoring.
        ItemStack[] storage = readStoredItems(base + ".inventory", 36);
        for (int i = 0; i < Math.min(36, storage.length); i++) {
            if (storage[i] != null && storage[i].getType() != Material.AIR) {
                inv.setItem(i, storage[i].clone());
            }
        }
        ItemStack[] armor = readStoredItems(base + ".armor", 4);
        for (int i = 0; i < Math.min(4, armor.length); i++) {
            if (armor[i] != null && armor[i].getType() != Material.AIR) {
                inv.setItem(36 + i, armor[i].clone());
            }
        }
        Object offhand = deathsData.get(base + ".offhand");
        if (offhand instanceof ItemStack item && item.getType() != Material.AIR) {
            inv.setItem(40, item.clone());
        }

        inv.setItem(45, createItem(Material.PAPER, "§c" + deathsData.getString(base + ".player", "Unknown"),
                "§7Time: " + deathsData.getString(base + ".time", ""),
                "§7Location: " + deathsData.getString(base + ".world", "") + " "
                        + deathsData.getInt(base + ".x") + ", " + deathsData.getInt(base + ".y") + ", " + deathsData.getInt(base + ".z"),
                "§7Level: " + deathsData.getInt(base + ".level")));
        inv.setItem(48, createItem(Material.LIME_WOOL, "§aRESTORE INVENTORY", "§7Restore the saved items to the player."));
        inv.setItem(50, createItem(Material.ARROW, "§eBack"));
        staff.openInventory(inv);
    }

    private ItemStack[] readStoredItems(String path, int expectedSize) {
        ItemStack[] result = new ItemStack[expectedSize];
        Object raw = deathsData.get(path);
        if (raw instanceof ItemStack[] array) {
            for (int i = 0; i < Math.min(expectedSize, array.length); i++) {
                if (array[i] != null) result[i] = array[i].clone();
            }
            return result;
        }
        if (!(raw instanceof List<?> stored)) return result;

        for (int i = 0; i < Math.min(expectedSize, stored.size()); i++) {
            Object value = stored.get(i);
            if (value instanceof ItemStack item) {
                result[i] = item.clone();
            } else if (value instanceof Map<?, ?> serialized) {
                try {
                    Map<String, Object> itemData = new HashMap<>();
                    for (Map.Entry<?, ?> entry : serialized.entrySet()) {
                        if (entry.getKey() instanceof String key) {
                            itemData.put(key, entry.getValue());
                        }
                    }
                    ItemStack decodedItem = null;
                    if (itemData.containsKey("==")) {
                        try {
                            Object decoded = ConfigurationSerialization.deserializeObject(itemData);
                            if (decoded instanceof ItemStack item) {
                                decodedItem = item.clone();
                            }
                        } catch (Exception ignored) {
                            // Try Bukkit's direct ItemStack deserializer below for older or unusual YAML data.
                        }
                    }
                    if (decodedItem == null) {
                        try {
                            decodedItem = ItemStack.deserialize(itemData);
                        } catch (Exception exception) {
                            getLogger().warning("Could not read saved item at " + path + "[" + i + "]: "
                                    + exception.getMessage());
                        }
                    }
                    result[i] = decodedItem;
                } catch (Exception exception) {
                    getLogger().warning("Could not read saved item at " + path + "[" + i + "]: "
                            + exception.getMessage());
                }
            }
        }
        return result;
    }

    private void restoreDeath(Player staff, String id) {
        String base = "deaths." + id;
        if (!deathsData.contains(base + ".uuid")) {
            staff.sendMessage(PREFIX + ChatColor.RED + "Death record does not contain a player UUID.");
            return;
        }
        Player target;
        try { target = Bukkit.getPlayer(UUID.fromString(deathsData.getString(base + ".uuid"))); }
        catch (Exception ex) { target = null; }
        if (target == null) {
            staff.sendMessage(PREFIX + ChatColor.RED + "The player must be online to restore their inventory.");
            return;
        }

        ItemStack[] storage = readStoredItems(base + ".inventory", 36);
        ItemStack[] armor = readStoredItems(base + ".armor", 4);
        if (getConfig().getBoolean("settings.rollback.replace-inventory", true)) {
            target.getInventory().setStorageContents(storage);
        } else {
            for (ItemStack item : storage) {
                if (item == null || item.getType() == Material.AIR) continue;
                Map<Integer, ItemStack> leftovers = target.getInventory().addItem(item.clone());
                for (ItemStack leftover : leftovers.values()) {
                    target.getWorld().dropItemNaturally(target.getLocation(), leftover);
                }
            }
        }
        if (getConfig().getBoolean("settings.rollback.restore-armor", true)) {
            target.getInventory().setArmorContents(armor);
        }
        if (getConfig().getBoolean("settings.rollback.restore-offhand", true)) {
            Object offhand = deathsData.get(base + ".offhand");
            target.getInventory().setItemInOffHand(offhand instanceof ItemStack item ? item.clone() : null);
        }
        target.setLevel(deathsData.getInt(base + ".level"));
        target.setExp((float) deathsData.getDouble(base + ".exp"));
        target.setTotalExperience(deathsData.getInt(base + ".total-exp"));
        target.updateInventory();
        int restoreCount = deathsData.getInt(base + ".restore-count", 0) + 1;
        deathsData.set(base + ".restore-count", restoreCount);
        deathsData.set(base + ".last-restored-by", staff.getName());
        deathsData.set(base + ".last-restored-at", System.currentTimeMillis());
        saveDeaths();

        logAction(staff, "DEATH_ROLLBACK", target.getName() + " record " + id,
                "Saved death snapshot", "Snapshot applied (restore #" + restoreCount + ")");
        staff.sendMessage(PREFIX + ChatColor.GREEN + "Death inventory successfully restored for " + target.getName() + ".");
        target.sendMessage(PREFIX + ChatColor.GREEN + "Your inventory has been restored by a staff member.");
    }


    private void loadStaffStatus() {
        staffStatusFile = new File(getDataFolder(), "staff-status.yml");
        staffStatusData = YamlConfiguration.loadConfiguration(staffStatusFile);
    }

    private void saveStaffStatus() {
        if (staffStatusData != null) saveFile(staffStatusData, staffStatusFile);
    }

    private void restoreDataBackup(CommandSender sender, String backupName) {
        File root = new File(getDataFolder(), "backups").getAbsoluteFile();
        File backup = new File(root, backupName).getAbsoluteFile();
        try {
            if (!backup.getParentFile().equals(root) || !backup.isDirectory()
                    || !backup.getName().matches("backup-[0-9]{8}-[0-9]{9}")) {
                sender.sendMessage(PREFIX + ChatColor.RED + "Invalid backup name.");
                return;
            }
            if (!createDataBackup()) {
                sender.sendMessage(PREFIX + ChatColor.RED + "Could not create the required safety backup. Restore was cancelled.");
                return;
            }
            String[] names = {"frozen.yml", "reports.yml", "notifications.yml", "staff-logs.yml", "deaths.yml", "punishments.yml", "staff-status.yml"};
            for (String name : names) {
                File source = new File(backup, name);
                if (source.isFile()) java.nio.file.Files.copy(source.toPath(), new File(getDataFolder(), name).toPath(), java.nio.file.StandardCopyOption.REPLACE_EXISTING);
            }
            sender.sendMessage(PREFIX + ChatColor.GREEN + "Backup restored. Restart the server to reload all plugin data safely.");
            getLogger().warning("A data backup was restored by " + sender.getName() + ". Restart required to reload in-memory data.");
        } catch (IOException exception) {
            sender.sendMessage(PREFIX + ChatColor.RED + "Backup restore failed: " + exception.getMessage());
            getLogger().severe("Backup restore failed: " + exception.getMessage());
        }
    }

    private boolean createDataBackup() {
        saveAllData();
        File backupDir = new File(getDataFolder(), "backups");
        if (!backupDir.exists() && !backupDir.mkdirs()) {
            getLogger().warning("Could not create backup directory: " + backupDir.getAbsolutePath());
            return false;
        }
        String stamp = java.time.LocalDateTime.now(getConfiguredZone()).format(DateTimeFormatter.ofPattern("yyyyMMdd-HHmmssSSS"));
        File destination = new File(backupDir, "backup-" + stamp);
        if (!destination.mkdirs()) {
            getLogger().warning("Could not create unique backup directory: " + destination.getName());
            return false;
        }
        File[] files = { frozenFile, reportsFile, notificationsFile, logsFile, deathsFile, punishmentsFile, staffStatusFile };
        try {
            for (File source : files) {
                if (source != null && source.isFile()) java.nio.file.Files.copy(source.toPath(), new File(destination, source.getName()).toPath(), java.nio.file.StandardCopyOption.REPLACE_EXISTING);
            }
            int keep = Math.max(1, getConfig().getInt("settings.backups.keep", 7));
            File[] snapshots = backupDir.listFiles(File::isDirectory);
            if (snapshots != null) {
                Arrays.sort(snapshots, Comparator.comparingLong(File::lastModified).reversed());
                for (int i = keep; i < snapshots.length; i++) deleteBackupTree(snapshots[i]);
            }
            getLogger().info("Created data backup: " + destination.getName());
            return true;
        } catch (IOException exception) {
            getLogger().severe("Data backup failed: " + exception.getMessage());
            try { deleteBackupTree(destination); }
            catch (IOException cleanupException) { getLogger().severe("Could not remove incomplete backup: " + cleanupException.getMessage()); }
            return false;
        }
    }

    private void deleteBackupTree(File file) throws java.io.IOException {
        if (file.isDirectory()) { File[] children = file.listFiles(); if (children != null) for (File child : children) deleteBackupTree(child); }
        java.nio.file.Files.deleteIfExists(file.toPath());
    }
    private void openOnlineStaff(Player viewer) {
        Inventory inv = Bukkit.createInventory(null, 54, ONLINE_STAFF);
        List<Player> staff = new ArrayList<>();
        for (Player online : Bukkit.getOnlinePlayers()) if (online.hasPermission("staffjobs.use")) staff.add(online);
        staff.sort(Comparator.comparing(Player::getName, String.CASE_INSENSITIVE_ORDER));
        for (int i = 0; i < Math.min(45, staff.size()); i++) {
            Player member = staff.get(i);
            String lastText = "Online now";
            String status = afkPlayers.contains(member.getUniqueId()) ? "§eAFK" : "§aONLINE";
            inv.setItem(i, createItem(Material.PLAYER_HEAD, "§a" + member.getName(), "§7Status: " + status, "§7Rank: §f" + getRank(member), "§7Last online: §f" + lastText));
        }
        inv.setItem(48, createItem(Material.CLOCK, "§eRefresh", "§7Update the online staff list."));
        inv.setItem(49, createItem(Material.ARROW, "§eBack"));
        inv.setItem(53, createItem(Material.PAPER, "§bOnline staff: §f" + staff.size()));
        viewer.openInventory(inv);
    }

    private void openNotificationSettings(Player player) {
        String base = "preferences." + player.getUniqueId();
        boolean chat = notificationsData.getBoolean(base + ".chat", true);
        boolean sound = notificationsData.getBoolean(base + ".sound", true);
        Inventory inv = Bukkit.createInventory(null, 27, NOTIFICATION_SETTINGS);
        inv.setItem(11, createItem(chat ? Material.LIME_DYE : Material.GRAY_DYE, "§eChat notifications", chat ? "§aEnabled" : "§cDisabled", "§7Click to toggle."));
        inv.setItem(15, createItem(sound ? Material.NOTE_BLOCK : Material.GRAY_DYE, "§eNotification sound", sound ? "§aEnabled" : "§cDisabled", "§7Click to toggle."));
        inv.setItem(22, createItem(Material.ARROW, "§eBack"));
        player.openInventory(inv);
    }

    private List<String> getLoggedStaffNames() {
        Set<String> names = new TreeSet<>(String.CASE_INSENSITIVE_ORDER);
        for (String entry : logsData.getStringList("entries")) {
            String actor = getLogField(entry, "Actor");
            if (!actor.isBlank()) names.add(actor);
        }
        return new ArrayList<>(names);
    }

    private String getLogField(String entry, String field) {
        String marker = field + ": ";
        for (String part : entry.split(" \\| ")) {
            if (part.startsWith(marker)) return part.substring(marker.length()).trim();
        }
        return "";
    }

    private void openActivityLogs(Player staff, int requestedPage) {
        List<String> names = getLoggedStaffNames();
        int pages = Math.max(1, (names.size() + 44) / 45);
        int page = Math.max(0, Math.min(requestedPage, pages - 1));
        activityPages.put(staff.getUniqueId(), page);
        Inventory inv = Bukkit.createInventory(null, 54, "§8Staff Activity Logs");
        int start = page * 45;
        int slot = 0;
        for (int i = start; i < Math.min(names.size(), start + 45); i++) {
            String name = names.get(i);
            int count = 0;
            for (String entry : logsData.getStringList("entries")) {
                if (name.equalsIgnoreCase(getLogField(entry, "Actor"))) count++;
            }
            // Keep the staff book clean: activity descriptions are shown only in the detail GUI.
            inv.setItem(slot++, createItem(Material.WRITABLE_BOOK, "§e" + name,
                    "§7Activity entries: " + count, "§eClick to view history."));
        }
        inv.setItem(45, createItem(Material.ARROW, "§ePrevious Page"));
        inv.setItem(48, createItem(Material.CLOCK, "§bRefresh"));
        inv.setItem(49, createItem(Material.ARROW, "§eBack", "§7Page " + (page + 1) + "/" + pages));
        inv.setItem(53, createItem(Material.ARROW, "§eNext Page"));
        staff.openInventory(inv);
    }

    private void openStaffHistory(Player viewer, String staffName, int requestedPage) {
        List<String> matching = new ArrayList<>();
        for (String entry : logsData.getStringList("entries")) {
            if (staffName.equalsIgnoreCase(getLogField(entry, "Actor"))) matching.add(entry);
        }
        int pages = Math.max(1, (matching.size() + 44) / 45);
        int page = Math.max(0, Math.min(requestedPage, pages - 1));
        staffHistoryPages.put(viewer.getUniqueId(), page);
        selectedHistoryStaff.put(viewer.getUniqueId(), staffName);
        Inventory inv = Bukkit.createInventory(null, 54, STAFF_HISTORY_DETAIL);
        int end = matching.size() - (page * 45);
        int start = Math.max(0, end - 45);
        int slot = 0;
        for (int i = end - 1; i >= start && slot < 45; i--) {
            String entry = matching.get(i);
            String action = getLogField(entry, "Action");
            if (action.isBlank()) action = "Staff activity";
            inv.setItem(slot++, createItem(Material.PAPER, "§e" + action,
                    "§7When: §f" + entry.split(" \\| ", 2)[0],
                    "§7Target: §f" + getLogField(entry, "Target"),
                    "§7Before: §f" + getLogField(entry, "Before"),
                    "§7After: §f" + getLogField(entry, "After")));
        }
        inv.setItem(45, createItem(Material.ARROW, "§ePrevious Page"));
        inv.setItem(48, createItem(Material.CLOCK, "§bRefresh"));
        inv.setItem(49, createItem(Material.ARROW, "§eBack to staff list"));
        inv.setItem(53, createItem(Material.ARROW, "§eNext Page"));
        viewer.openInventory(inv);
    }

    private void logAction(Player staff, String action, String target) {
        logAction(staff, action, target, "N/A", "N/A");
    }

    private void logAction(Player staff, String action, String target, String before, String after) {
        if (logsData == null) return;
        List<String> entries = logsData.getStringList("entries");
        String actor = staff == null ? "SYSTEM" : staff.getName();
        String stamp = ZonedDateTime.now(getConfiguredZone()).format(dateFormatter) + " " + getZoneLabel();
        entries.add(stamp + " | Actor: " + actor + " | Target: " + target + " | Action: " + action
                + " | Before: " + before + " | After: " + after);
        int maxEntries = getConfig().getInt("settings.activity-logs.max-entries", 500);
        while (maxEntries > 0 && entries.size() > maxEntries) entries.remove(0);
        logsData.set("entries", entries);
        saveLogs();
    }

    private void loadLogs() {
        logsFile = new File(getDataFolder(), "staff-logs.yml");
        logsData = YamlConfiguration.loadConfiguration(logsFile);
    }
    private void saveLogs() { if (logsData != null) saveFile(logsData, logsFile); }
    private void trimDeathHistory() {
        int maxEntries = getConfig().getInt("settings.max-death-logs", 100);
        if (maxEntries <= 0) return;
        if (deathsData == null || deathsData.getConfigurationSection("deaths") == null) return;
        List<String> ids = new ArrayList<>(deathsData.getConfigurationSection("deaths").getKeys(false));
        int excess = ids.size() - maxEntries;
        for (int i = 0; i < excess; i++) {
            deathsData.set("deaths." + ids.get(i), null);
        }
    }

    private void loadDeaths() {
        deathsFile = new File(getDataFolder(), "deaths.yml");
        deathsData = YamlConfiguration.loadConfiguration(deathsFile);
        trimDeathHistory();
        saveDeaths();
    }
    private void saveDeaths() { if (deathsData != null) saveFile(deathsData, deathsFile); }


    private boolean isOre(Material type) {
        return type.name().endsWith("_ORE") || type == Material.ANCIENT_DEBRIS;
    }

    private void enableXray(Player player) {
        if (!player.hasPermission("staffjobs.xray")) return;
        xrayEnabled.add(player.getUniqueId());
        refreshXrayView(player);
        player.sendMessage(PREFIX + ChatColor.GREEN + "X-Ray enabled.");
    }

    private void disableXray(Player player, boolean notify) {
        UUID id = player.getUniqueId();
        xrayEnabled.remove(id);
        Set<Location> hidden = xrayHiddenBlocks.remove(id);
        if (hidden != null) {
            for (Location location : hidden) {
                World world = location.getWorld();
                if (world == null || !world.isChunkLoaded(location.getBlockX() >> 4, location.getBlockZ() >> 4)) continue;
                player.sendBlockChange(location, world.getBlockAt(location).getBlockData());
            }
        }
        if (notify) player.sendMessage(PREFIX + ChatColor.YELLOW + "X-Ray disabled.");
    }

    private void refreshAllXrayViews() {
        for (UUID id : new HashSet<>(xrayEnabled)) {
            Player player = Bukkit.getPlayer(id);
            if (player == null || !player.isOnline() || !player.hasPermission("staffjobs.xray")) {
                if (player != null) disableXray(player, false);
                else xrayEnabled.remove(id);
                continue;
            }
            refreshXrayView(player);
        }
    }

    private void refreshXrayView(Player player) {
        if (!xrayEnabled.contains(player.getUniqueId())) return;
        Set<Location> previous = xrayHiddenBlocks.remove(player.getUniqueId());
        if (previous != null) {
            for (Location location : previous) {
                World world = location.getWorld();
                if (world != null && world.isChunkLoaded(location.getBlockX() >> 4, location.getBlockZ() >> 4)) {
                    player.sendBlockChange(location, world.getBlockAt(location).getBlockData());
                }
            }
        }

        Set<Location> hidden = new HashSet<>();
        Location center = player.getLocation();
        World world = center.getWorld();
        int cx = center.getBlockX(), cy = center.getBlockY(), cz = center.getBlockZ();
        for (int x = cx - 6; x <= cx + 6; x++) {
            for (int y = Math.max(world.getMinHeight(), cy - 5); y <= Math.min(world.getMaxHeight() - 1, cy + 5); y++) {
                for (int z = cz - 6; z <= cz + 6; z++) {
                    if (!world.isChunkLoaded(x >> 4, z >> 4)) continue;
                    org.bukkit.block.Block block = world.getBlockAt(x, y, z);
                    Material type = block.getType();
                    if (!type.isSolid() || isOre(type) || block.getState() instanceof org.bukkit.block.TileState) continue;
                    Location location = block.getLocation();
                    player.sendBlockChange(location, Material.AIR.createBlockData());
                    hidden.add(location);
                }
            }
        }
        xrayHiddenBlocks.put(player.getUniqueId(), hidden);
    }

    @EventHandler
    public void onXrayPlayerQuit(PlayerQuitEvent event) {
        disableXray(event.getPlayer(), false);
    }

    // STAFF HIERARCHY

    private int getRank(Player player) {
        if (player.isOp()) return 100;
        for (int i = 99; i >= 1; i--) {
            if (player.hasPermission("staffjobs.rank." + i)) return i;
        }
        return 0;
    }

    private boolean canManage(Player staff, Player target) {
        if (staff.getUniqueId().equals(target.getUniqueId())) return true;
        if (staff.hasPermission("staffjobs.protect.bypass")) return true;
        return getRank(staff) > getRank(target);
    }

    // ITEM HELPERS

    private ItemStack createItem(Material material, String name, String... lore) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(name);
            if (lore.length > 0) meta.setLore(Arrays.asList(lore));
            item.setItemMeta(meta);
        }
        return item;
    }
}
