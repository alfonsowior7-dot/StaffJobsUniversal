package com.fonzzscripter.staffjobsui;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.Click;
import net.minecraft.client.input.CharInput;
import net.minecraft.client.input.KeyInput;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;

/** Black-green dashboard with live read-only server overview cards. */
public final class DashboardScreen extends Screen {
    private static volatile int BACKGROUND = 0xF0101412, PANEL = 0xFF18211C,
            PANEL_ALT = 0xFF202B24, GREEN = 0xFF00E676, ACCENT = 0xFF39FF88,
            MUTED = 0xFF9AA79F, TEXT = 0xFFFFFFFF;
    private static volatile String[] serverTheme = null;
    private static boolean compactRows = false;
    private static boolean showOfflineStaff = true;
    private static boolean preferencesLoaded = false;
    private static final java.nio.file.Path PREFERENCES_FILE =
            net.fabricmc.loader.api.FabricLoader.getInstance().getConfigDir().resolve("staffjobs-ui.properties");

    private static void loadPreferences() {
        if (preferencesLoaded) return;
        preferencesLoaded = true;
        java.util.Properties properties = new java.util.Properties();
        if (java.nio.file.Files.exists(PREFERENCES_FILE)) {
            try (java.io.InputStream input = java.nio.file.Files.newInputStream(PREFERENCES_FILE)) {
                properties.load(input);
                compactRows = Boolean.parseBoolean(properties.getProperty("compactRows", "false"));
                showOfflineStaff = Boolean.parseBoolean(properties.getProperty("showOfflineStaff", "true"));
            } catch (java.io.IOException exception) {
                System.err.println("[StaffJobs UI] Could not load preferences: " + exception.getMessage());
            }
        }
    }

    private static void savePreferences() {
        loadPreferences();
        java.util.Properties properties = new java.util.Properties();
        properties.setProperty("compactRows", Boolean.toString(compactRows));
        properties.setProperty("showOfflineStaff", Boolean.toString(showOfflineStaff));
        try {
            java.nio.file.Files.createDirectories(PREFERENCES_FILE.getParent());
            try (java.io.OutputStream output = java.nio.file.Files.newOutputStream(PREFERENCES_FILE)) {
                properties.store(output, "StaffJobs UI local preferences");
            }
        } catch (java.io.IOException exception) {
            System.err.println("[StaffJobs UI] Could not save preferences: " + exception.getMessage());
        }
    }

    private static volatile String settingsError = null;
    private static volatile int online = -1, maxPlayers = -1, staffOnline = -1;
    private static volatile java.util.List<String> players = java.util.Collections.emptyList();
    private static volatile boolean playersLoaded;
    private static volatile String[] selectedProfile = null;
    private static volatile String profileError = null;
    private int selectedPage = 0;
    private String searchQuery = "";
    private boolean searchFocused = false;
    private static volatile java.util.List<String[]> reports = java.util.Collections.emptyList();
    private static volatile String reportError = null;
    private static volatile String reportActionMessage = null;
    private int selectedReportIndex = -1;
    private static volatile java.util.List<String> activity = java.util.Collections.emptyList();
    private static volatile String activityError = null;
    private static volatile java.util.List<String[]> deaths = java.util.Collections.emptyList();
    private static volatile String deathError = null;
    private static volatile java.util.List<String[]> punishments = java.util.Collections.emptyList();
    private static volatile String punishmentError = null;
    private static volatile int analyticsOnline = -1, analyticsAfk = -1, analyticsTracked = -1;
    private static volatile java.util.List<String[]> analyticsStaff = java.util.Collections.emptyList();
    private static volatile String analyticsError = null;

    public DashboardScreen() { super(Text.literal("StaffJobs Dashboard")); loadPreferences(); }

    public static void updateOverview(int currentOnline, int maximum, int currentStaff) {
        online = currentOnline; maxPlayers = maximum; staffOnline = currentStaff;
    }
    public static void clearOverview() { online = -1; maxPlayers = -1; staffOnline = -1; players = java.util.Collections.emptyList(); playersLoaded = false; }
    public static void updatePlayers(java.util.List<String> names) { players = java.util.Collections.unmodifiableList(new java.util.ArrayList<>(names)); playersLoaded = true; }
    public static void updatePlayerProfile(String name, String gameMode, String world, int ping) {
        selectedProfile = new String[]{name, gameMode, world, Integer.toString(ping)};
        profileError = null;
    }
    public static void updatePlayerProfileError(String reason) { selectedProfile = null; profileError = reason; }
    public static void updateReports(java.util.List<String[]> value) { reports = java.util.Collections.unmodifiableList(new java.util.ArrayList<>(value)); reportError = null; }
    public static void updateActivity(java.util.List<String> value) { activity = java.util.Collections.unmodifiableList(new java.util.ArrayList<>(value)); activityError = null; }
    public static void updateDeaths(java.util.List<String[]> value) { deaths = java.util.Collections.unmodifiableList(new java.util.ArrayList<>(value)); deathError = null; }
    public static void updatePunishments(java.util.List<String[]> value) { punishments = java.util.Collections.unmodifiableList(new java.util.ArrayList<>(value)); punishmentError = null; }
    public static void updateSettings(String[] theme) {
        if (theme == null || theme.length != 7) return;
        serverTheme = theme.clone();
        BACKGROUND = withAlpha(parseColor(theme[3], 0x00101412), 0xF0);
        GREEN = parseColor(theme[1], 0xFF00E676);
        PANEL_ALT = parseColor(theme[2], 0xFF202B24);
        TEXT = parseColor(theme[4], 0xFFFFFFFF);
        ACCENT = parseColor(theme[5], 0xFF39FF88);
        settingsError = null;
    }
    private static int parseColor(String value, int fallback) {
        try {
            String hex = value.startsWith("#") ? value.substring(1) : value;
            if (hex.length() != 6) return fallback;
            return 0xFF000000 | Integer.parseInt(hex, 16);
        } catch (RuntimeException exception) { return fallback; }
    }
    private static int withAlpha(int color, int alpha) { return (color & 0x00FFFFFF) | (alpha << 24); }
    public static void updateAnalytics(int online, int afk, int tracked, java.util.List<String[]> entries) {
        analyticsOnline = online; analyticsAfk = afk; analyticsTracked = tracked;
        analyticsStaff = java.util.Collections.unmodifiableList(new java.util.ArrayList<>(entries));
        analyticsError = null;
    }
    public static void updateSectionDenied(String section, String reason) { if ("reports".equals(section)) reportError = reason; else if ("activity".equals(section)) activityError = reason; else if ("deaths".equals(section)) deathError = reason; else if ("punishments".equals(section)) punishmentError = reason; else if ("analytics".equals(section)) analyticsError = reason; else if ("settings".equals(section)) settingsError = reason; }
    public static void updateReportActionResult(String result, String message) { reportActionMessage = message; if ("OK".equals(result)) { reports = java.util.Collections.emptyList(); reportError = "Action completed. Refresh Report Center to reload."; } else reportError = message; }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        renderBackground(context);
        int left = Math.max(18, (width - 760) / 2), top = Math.max(18, (height - 440) / 2);
        int right = Math.min(width - 18, left + 760), bottom = Math.min(height - 18, top + 440);
        context.fill(left, top, right, bottom, BACKGROUND);
        context.fill(left, top, left + 172, bottom, PANEL);
        context.fill(left, top, right, top + 3, GREEN);
        context.drawTextWithShadow(textRenderer, "STAFFJOBS", left + 18, top + 18, GREEN);
        context.drawTextWithShadow(textRenderer, "UNIVERSAL 2.0", left + 18, top + 34, MUTED);

        String[] nav = {"OVERVIEW", "PLAYERS", "REPORT CENTER", "STAFF ANALYTICS",
                "ACTIVITY HISTORY", "DEATH LOGS", "PUNISHMENTS", "SETTINGS"};
        for (int i = 0; i < nav.length; i++) drawNav(context, left + 14, top + 82 + i * 30, nav[i], i == selectedPage);

        int contentLeft = left + 194;
        if (selectedPage == 1) {
            context.drawTextWithShadow(textRenderer, "Online Players", contentLeft, top + 22, 0xFFFFFFFF);
            context.drawTextWithShadow(textRenderer, "Server-authoritative player list · names only", contentLeft, top + 40, MUTED);
            int panelTop = top + 76;
            int split = contentLeft + 236;
            context.fill(contentLeft, panelTop, right - 18, bottom - 22, PANEL);
            java.util.List<String> filteredPlayers = getFilteredPlayers();
            context.drawTextWithShadow(textRenderer, playersLoaded ? "ONLINE (" + filteredPlayers.size() + "/" + players.size() + ")" : "LOADING...", contentLeft + 14, panelTop + 16, GREEN);
            int searchLeft = contentLeft + 10, searchTop = panelTop + 34;
            context.fill(searchLeft, searchTop, split - 10, searchTop + 22, searchFocused ? 0xFF26382D : PANEL_ALT);
            drawOutline(context, searchLeft, searchTop, split - searchLeft - 10, 22, searchFocused ? GREEN : 0xFF34443A);
            String searchLabel = searchQuery.isEmpty() ? "Search players..." : searchQuery;
            context.drawTextWithShadow(textRenderer, searchLabel, searchLeft + 7, searchTop + 7, searchQuery.isEmpty() ? MUTED : 0xFFFFFFFF);
            if (playersLoaded && filteredPlayers.isEmpty()) {
                context.drawTextWithShadow(textRenderer, searchQuery.isEmpty() ? "No players online." : "No matching players.", contentLeft + 14, panelTop + 78, MUTED);
            } else {
                for (int i = 0; i < filteredPlayers.size() && i < 12; i++) {
                    int rowY = panelTop + 68 + i * 22;
                    context.fill(contentLeft + 8, rowY - 4, split - 8, rowY + 15, (i % 2 == 0) ? PANEL_ALT : PANEL);
                    context.drawTextWithShadow(textRenderer, filteredPlayers.get(i), contentLeft + 16, rowY, 0xFFFFFFFF);
                }
                if (filteredPlayers.size() > 12) context.drawTextWithShadow(textRenderer, "+" + (filteredPlayers.size() - 12) + " more", contentLeft + 14, bottom - 38, MUTED);
            }
            context.fill(split, panelTop + 12, split + 1, bottom - 34, 0xFF34443A);
            context.drawTextWithShadow(textRenderer, "PLAYER PROFILE", split + 16, panelTop + 18, GREEN);
            if (selectedProfile != null) {
                context.drawTextWithShadow(textRenderer, selectedProfile[0], split + 16, panelTop + 48, 0xFFFFFFFF);
                context.drawTextWithShadow(textRenderer, "Game mode: " + selectedProfile[1], split + 16, panelTop + 72, MUTED);
                context.drawTextWithShadow(textRenderer, "World: " + selectedProfile[2], split + 16, panelTop + 94, MUTED);
                context.drawTextWithShadow(textRenderer, "Ping: " + selectedProfile[3] + " ms", split + 16, panelTop + 116, MUTED);
            } else {
                context.drawTextWithShadow(textRenderer, profileError == null ? "Select an online player." : profileError,
                        split + 16, panelTop + 48, MUTED);
            }
        } else if (selectedPage == 2) {
            context.drawTextWithShadow(textRenderer, "Report Center", contentLeft, top + 22, 0xFFFFFFFF);
            context.drawTextWithShadow(textRenderer, "Read-only report queue · latest 20 reports", contentLeft, top + 40, MUTED);
            int panelTop = top + 76;
            context.fill(contentLeft, panelTop, right - 18, bottom - 22, PANEL);
            if (reportError != null) {
                context.drawTextWithShadow(textRenderer, reportError, contentLeft + 16, panelTop + 24, 0xFFFF7777);
            } else if (reports.isEmpty()) {
                context.drawTextWithShadow(textRenderer, "No reports loaded, or there are no reports.", contentLeft + 16, panelTop + 24, MUTED);
            } else {
                for (int i = 0; i < reports.size() && i < 8; i++) {
                    String[] report = reports.get(i);
                    int rowY = panelTop + 16 + i * 28;
                    context.fill(contentLeft + 10, rowY, right - 28, rowY + 24,
                            i == selectedReportIndex ? 0xFF294333 : (i % 2 == 0) ? PANEL_ALT : PANEL);
                    if (i == selectedReportIndex) drawOutline(context, contentLeft + 10, rowY, right - contentLeft - 38, 24, GREEN);
                    context.drawTextWithShadow(textRenderer, report[1] + " · " + report[3], contentLeft + 18, rowY + 3, GREEN);
                    String message = report[5].length() > 54 ? report[5].substring(0, 54) + "…" : report[5];
                    context.drawTextWithShadow(textRenderer, message, contentLeft + 18, rowY + 14, MUTED);
                }
                if (selectedReportIndex >= 0 && selectedReportIndex < reports.size()) {
                    int actionY = bottom - 54;
                    int buttonW = 104, buttonGap = 8;
                    String[] labels = {"ACCEPT", "REJECT", "ASSIGN"};
                    int[] fills = {0xFF1D5938, 0xFF632D32, 0xFF244A5A};
                    for (int i = 0; i < labels.length; i++) {
                        int x = contentLeft + 12 + i * (buttonW + buttonGap);
                        context.fill(x, actionY, x + buttonW, actionY + 22, fills[i]);
                        drawOutline(context, x, actionY, buttonW, 22, 0xFF607568);
                        context.drawTextWithShadow(textRenderer, labels[i], x + 28, actionY + 7, 0xFFFFFFFF);
                    }
                }
            }
        } else if (selectedPage == 3) {
            context.drawTextWithShadow(textRenderer, "Staff Analytics", contentLeft, top + 22, 0xFFFFFFFF);
            context.drawTextWithShadow(textRenderer, "Live staff presence · AFK status · tracked roster", contentLeft, top + 40, MUTED);
            int panelTop = top + 76;
            context.fill(contentLeft, panelTop, right - 18, bottom - 22, PANEL);
            if (analyticsError != null) {
                context.drawTextWithShadow(textRenderer, analyticsError, contentLeft + 16, panelTop + 24, 0xFFFF7777);
            } else if (analyticsOnline < 0) {
                context.drawTextWithShadow(textRenderer, "Loading staff analytics...", contentLeft + 16, panelTop + 24, MUTED);
            } else {
                int cardW = 148, gap = 12;
                drawCard(context, contentLeft + 12, panelTop + 12, cardW, 66, "STAFF ONLINE", Integer.toString(analyticsOnline), "Currently connected");
                drawCard(context, contentLeft + 12 + cardW + gap, panelTop + 12, cardW, 66, "STAFF AFK", Integer.toString(analyticsAfk), "Idle by AFK threshold");
                drawCard(context, contentLeft + 12 + (cardW + gap) * 2, panelTop + 12, cardW, 66, "TRACKED STAFF", Integer.toString(analyticsTracked), "Known staff roster");
                context.drawTextWithShadow(textRenderer, "STAFF ROSTER", contentLeft + 16, panelTop + 94, GREEN);
                if (analyticsStaff.isEmpty()) {
                    context.drawTextWithShadow(textRenderer, "No tracked staff records yet.", contentLeft + 16, panelTop + 116, MUTED);
                } else {
                    java.util.List<String[]> visibleStaff = new java.util.ArrayList<>();
                    for (String[] entry : analyticsStaff) {
                        if (showOfflineStaff || !"OFFLINE".equals(entry[1])) visibleStaff.add(entry);
                    }
                    for (int i = 0; i < visibleStaff.size() && i < 8; i++) {
                        String[] entry = visibleStaff.get(i);
                        int rowHeight = compactRows ? 19 : 23;
                        int rowGap = compactRows ? 22 : 27;
                        int rowY = panelTop + 110 + i * rowGap;
                        context.fill(contentLeft + 10, rowY, right - 28, rowY + rowHeight, (i % 2 == 0) ? PANEL_ALT : PANEL);
                        context.drawTextWithShadow(textRenderer, entry[0], contentLeft + 18, rowY + 7, 0xFFFFFFFF);
                        context.drawTextWithShadow(textRenderer, entry[1], contentLeft + 250, rowY + 7,
                                "ONLINE".equals(entry[1]) ? GREEN : MUTED);
                        String seen = "ONLINE".equals(entry[1]) ? "Now" : "OFFLINE".equals(entry[1]) ? "Last seen: " + entry[2] : "AFK";
                        context.drawTextWithShadow(textRenderer, seen, contentLeft + 340, rowY + 7, MUTED);
                        context.drawTextWithShadow(textRenderer, "Playtime: " + formatDuration(parseLong(entry, 3))
                                + " · Sessions: " + entry[4], contentLeft + 340, rowY + 16, ACCENT);
                    }
                }
                context.drawTextWithShadow(textRenderer, "Playtime includes completed sessions plus the current online session.", contentLeft + 16, bottom - 38, MUTED);
            }
        } else if (selectedPage == 7) {
            context.drawTextWithShadow(textRenderer, "Dashboard Settings", contentLeft, top + 22, TEXT);
            context.drawTextWithShadow(textRenderer, "Server-managed theme configuration · permission: staffjobs.settings", contentLeft, top + 40, MUTED);
            int panelTop = top + 76;
            context.fill(contentLeft, panelTop, right - 18, bottom - 22, PANEL);
            if (settingsError != null) {
                context.drawTextWithShadow(textRenderer, settingsError, contentLeft + 16, panelTop + 24, 0xFFFF7777);
            } else if (serverTheme == null) {
                context.drawTextWithShadow(textRenderer, "Loading server theme settings...", contentLeft + 16, panelTop + 24, MUTED);
            } else {
                context.drawTextWithShadow(textRenderer, "SERVER THEME", contentLeft + 16, panelTop + 18, GREEN);
                context.drawTextWithShadow(textRenderer, "Preset: " + serverTheme[0], contentLeft + 16, panelTop + 48, TEXT);
                String[] labels = {"PRIMARY", "SECONDARY", "BACKGROUND", "TEXT", "ACCENT"};
                int[] colors = {parseColor(serverTheme[1], GREEN), parseColor(serverTheme[2], PANEL_ALT),
                        parseColor(serverTheme[3], BACKGROUND), parseColor(serverTheme[4], TEXT),
                        parseColor(serverTheme[5], ACCENT)};
                for (int i = 0; i < labels.length; i++) {
                    int x = contentLeft + 16 + i * 104;
                    context.fill(x, panelTop + 76, x + 88, panelTop + 118, PANEL_ALT);
                    context.fill(x + 8, panelTop + 84, x + 80, panelTop + 91, colors[i]);
                    context.drawTextWithShadow(textRenderer, labels[i], x + 8, panelTop + 100, TEXT);
                    context.drawTextWithShadow(textRenderer, serverTheme[i + 1], x + 8, panelTop + 126, MUTED);
                }
                context.drawTextWithShadow(textRenderer, "Gradient: " + serverTheme[6], contentLeft + 16, panelTop + 164, MUTED);
                context.drawTextWithShadow(textRenderer, "To change the shared theme, edit interface.theme in the server config.yml.", contentLeft + 16, panelTop + 204, TEXT);
                context.drawTextWithShadow(textRenderer, "Theme changes are applied when clients reconnect / reopen the dashboard.", contentLeft + 16, panelTop + 226, MUTED);
                context.drawTextWithShadow(textRenderer, "LOCAL UI PREFERENCES", contentLeft + 16, panelTop + 266, GREEN);
                drawPreference(context, contentLeft + 16, panelTop + 290, "Compact roster rows", compactRows);
                drawPreference(context, contentLeft + 260, panelTop + 290, "Show offline staff", showOfflineStaff);
                context.drawTextWithShadow(textRenderer, "Saved on this device in config/staffjobs-ui.properties.", contentLeft + 16, panelTop + 322, MUTED);
            }
        } else if (selectedPage == 6) {
            context.drawTextWithShadow(textRenderer, "Punishment History", contentLeft, top + 22, 0xFFFFFFFF);
            context.drawTextWithShadow(textRenderer, "Latest records · read-only · permission: staffjobs.punishments", contentLeft, top + 40, MUTED);
            int panelTop = top + 76;
            context.fill(contentLeft, panelTop, right - 18, bottom - 22, PANEL);
            if (punishmentError != null) {
                context.drawTextWithShadow(textRenderer, punishmentError, contentLeft + 16, panelTop + 24, 0xFFFF7777);
            } else if (punishments.isEmpty()) {
                context.drawTextWithShadow(textRenderer, "No punishment records available.", contentLeft + 16, panelTop + 24, MUTED);
            } else {
                for (int i = 0; i < punishments.size() && i < 10; i++) {
                    String[] entry = punishments.get(i);
                    int rowY = panelTop + 10 + i * 34;
                    context.fill(contentLeft + 10, rowY, right - 28, rowY + 30, (i % 2 == 0) ? PANEL_ALT : PANEL);
                    context.drawTextWithShadow(textRenderer, entry[0], contentLeft + 17, rowY + 4, GREEN);
                    String detail = entry[1].length() > 100 ? entry[1].substring(0, 100) + "…" : entry[1];
                    context.drawTextWithShadow(textRenderer, detail, contentLeft + 17, rowY + 17, MUTED);
                }
                if (punishments.size() > 10) context.drawTextWithShadow(textRenderer, "+" + (punishments.size() - 10) + " more records", contentLeft + 14, bottom - 38, MUTED);
            }
        } else if (selectedPage == 5) {
            context.drawTextWithShadow(textRenderer, "Death Logs", contentLeft, top + 22, 0xFFFFFFFF);
            context.drawTextWithShadow(textRenderer, "Latest death records · metadata only · no inventory snapshots", contentLeft, top + 40, MUTED);
            int panelTop = top + 76;
            context.fill(contentLeft, panelTop, right - 18, bottom - 22, PANEL);
            if (deathError != null) {
                context.drawTextWithShadow(textRenderer, deathError, contentLeft + 16, panelTop + 24, 0xFFFF7777);
            } else if (deaths.isEmpty()) {
                context.drawTextWithShadow(textRenderer, "No death records available.", contentLeft + 16, panelTop + 24, MUTED);
            } else {
                for (int i = 0; i < deaths.size() && i < 10; i++) {
                    String[] entry = deaths.get(i);
                    int rowY = panelTop + 10 + i * 34;
                    context.fill(contentLeft + 10, rowY, right - 28, rowY + 30, (i % 2 == 0) ? PANEL_ALT : PANEL);
                    context.drawTextWithShadow(textRenderer, entry[1] + " · " + entry[4], contentLeft + 17, rowY + 4, GREEN);
                    String detail = entry[2] + " · " + entry[3];
                    if (detail.length() > 78) detail = detail.substring(0, 78) + "…";
                    context.drawTextWithShadow(textRenderer, detail, contentLeft + 17, rowY + 17, MUTED);
                }
            }
        } else if (selectedPage == 4) {
            context.drawTextWithShadow(textRenderer, "Activity History", contentLeft, top + 22, 0xFFFFFFFF);
            context.drawTextWithShadow(textRenderer, "Latest staff actions · read-only audit entries", contentLeft, top + 40, MUTED);
            int panelTop = top + 76;
            context.fill(contentLeft, panelTop, right - 18, bottom - 22, PANEL);
            if (activityError != null) {
                context.drawTextWithShadow(textRenderer, activityError, contentLeft + 16, panelTop + 24, 0xFFFF7777);
            } else if (activity.isEmpty()) {
                context.drawTextWithShadow(textRenderer, "No activity entries available.", contentLeft + 16, panelTop + 24, MUTED);
            } else {
                for (int i = 0; i < activity.size() && i < 10; i++) {
                    int rowY = panelTop + 12 + i * 29;
                    context.fill(contentLeft + 10, rowY, right - 28, rowY + 25, (i % 2 == 0) ? PANEL_ALT : PANEL);
                    String entry = activity.get(i);
                    if (entry.length() > 104) entry = entry.substring(0, 104) + "…";
                    context.drawTextWithShadow(textRenderer, entry, contentLeft + 17, rowY + 9, 0xFFFFFFFF);
                }
            }
        } else {
            context.drawTextWithShadow(textRenderer, "Staff Dashboard", contentLeft, top + 22, 0xFFFFFFFF);
            context.drawTextWithShadow(textRenderer, "Live server overview · read-only", contentLeft, top + 40, MUTED);
            int gap = 12, cardWidth = Math.max(100, (right - contentLeft - 28 - gap * 2) / 3);
            drawCard(context, contentLeft, top + 76, cardWidth, 72, "ONLINE PLAYERS",
                    online < 0 ? "…" : online + " / " + maxPlayers, online < 0 ? "Loading server data" : "Connected players");
            drawCard(context, contentLeft + cardWidth + gap, top + 76, cardWidth, 72,
                    "OPEN REPORTS", "—", "Report endpoint not connected");
            drawCard(context, contentLeft + (cardWidth + gap) * 2, top + 76, cardWidth, 72,
                    "STAFF ONLINE", staffOnline < 0 ? "…" : Integer.toString(staffOnline),
                    staffOnline < 0 ? "Loading server data" : "Players with staff permission");
            int lowerTop = top + 164;
            context.fill(contentLeft, lowerTop, right - 18, bottom - 22, PANEL);
            context.drawTextWithShadow(textRenderer, "RECENT ACTIVITY", contentLeft + 16, lowerTop + 16, GREEN);
            context.drawTextWithShadow(textRenderer, "Live overview data received from the Paper server.", contentLeft + 16, lowerTop + 42, MUTED);
            context.drawTextWithShadow(textRenderer, "Other dashboard sections are being integrated in stages.", contentLeft + 16, lowerTop + 58, MUTED);
        }
        super.render(context, mouseX, mouseY, delta);
    }

    @Override
    public boolean mouseClicked(Click click, boolean doubled) {
        double mouseX = click.x();
        double mouseY = click.y();
        int button = click.button();
        if (button == 0) {
            loadPreferences();
            int left = Math.max(18, (width - 760) / 2);
            int topForPreferences = Math.max(18, (height - 440) / 2);
            int contentForPreferences = left + 194;
            int panelForPreferences = topForPreferences + 76;
            if (selectedPage == 7 && mouseY >= panelForPreferences + 290 && mouseY <= panelForPreferences + 312) {
                if (mouseX >= contentForPreferences + 16 && mouseX <= contentForPreferences + 236) {
                    compactRows = !compactRows;
                    savePreferences();
                    return true;
                }
                if (mouseX >= contentForPreferences + 260 && mouseX <= contentForPreferences + 490) {
                    showOfflineStaff = !showOfflineStaff;
                    savePreferences();
                    return true;
                }
            }
            int top = Math.max(18, (height - 440) / 2);
            for (int i = 0; i < 8; i++) {
                int y = top + 77 + i * 30;
                if (mouseX >= left + 8 && mouseX <= left + 164 && mouseY >= y && mouseY <= y + 24) {
                    selectedPage = i;
                    if (i == 1) {
                        playersLoaded = false;
                        selectedProfile = null;
                        profileError = null;
                        StaffJobsUI.requestPlayers();
                    } else if (i == 7) {
                        serverTheme = null;
                        settingsError = null;
                        StaffJobsUI.requestSettings();
                    } else if (i == 3) {
                        analyticsOnline = -1;
                        analyticsError = null;
                        analyticsStaff = java.util.Collections.emptyList();
                        StaffJobsUI.requestAnalytics();
                    } else if (i == 2) {
                        reports = java.util.Collections.emptyList();
                        reportError = null;
                        selectedReportIndex = -1;
                        StaffJobsUI.requestReports();
                    }
                    else if (i == 6) {
                        punishments = java.util.Collections.emptyList();
                        punishmentError = null;
                        StaffJobsUI.requestPunishments();
                    }
                    else if (i == 5) {
                        deaths = java.util.Collections.emptyList();
                        deathError = null;
                        StaffJobsUI.requestDeathLogs();
                    }
                    else if (i == 4) {
                        activity = java.util.Collections.emptyList();
                        activityError = null;
                        StaffJobsUI.requestActivity();
                    }
                    return true;
                }
            }
            if (selectedPage == 2) {
                int contentLeft = left + 194;
                int panelTop = top + 76;
                int bottom = Math.min(height - 18, top + 440);
                for (int i = 0; i < reports.size() && i < 8; i++) {
                    int rowY = panelTop + 16 + i * 28;
                    if (mouseX >= contentLeft + 10 && mouseX <= width - 46
                            && mouseY >= rowY && mouseY <= rowY + 24) {
                        selectedReportIndex = i;
                        return true;
                    }
                }
                if (selectedReportIndex >= 0 && selectedReportIndex < reports.size()) {
                    int actionY = bottom - 54;
                    int buttonW = 104, buttonGap = 8;
                    for (int i = 0; i < 3; i++) {
                        int x = contentLeft + 12 + i * (buttonW + buttonGap);
                        if (mouseX >= x && mouseX <= x + buttonW && mouseY >= actionY && mouseY <= actionY + 22) {
                            String action = i == 0 ? "ACCEPT" : i == 1 ? "REJECT" : "ASSIGN";
                            StaffJobsUI.performReportAction(action, reports.get(selectedReportIndex)[0]);
                            reportError = "Processing " + action.toLowerCase(java.util.Locale.ROOT) + "...";
                            return true;
                        }
                    }
                }
            }
            if (selectedPage == 1) {
                int contentLeft = left + 194;
                int panelTop = top + 76;
                int split = contentLeft + 236;
                int searchLeft = contentLeft + 10, searchTop = panelTop + 34;
                searchFocused = mouseX >= searchLeft && mouseX <= split - 10
                        && mouseY >= searchTop && mouseY <= searchTop + 22;
                if (searchFocused) return true;
                if (mouseX >= contentLeft + 8 && mouseX < split - 8
                        && mouseY >= panelTop + 64 && mouseY <= panelTop + 68 + Math.min(getFilteredPlayers().size(), 12) * 22) {
                    int index = (int) ((mouseY - (panelTop + 64)) / 22);
                    java.util.List<String> filtered = getFilteredPlayers();
                    if (index >= 0 && index < filtered.size() && index < 12) {
                        selectedProfile = null;
                        profileError = "Loading profile...";
                        StaffJobsUI.requestPlayerProfile(filtered.get(index));
                        return true;
                    }
                }
            }
        }
        if (selectedPage == 1) searchFocused = false;
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean charTyped(CharInput input) {
        int codepoint = input.codepoint();
        if (selectedPage == 1 && searchFocused && !Character.isISOControl(codepoint)) {
            String chr = input.codepointAsString();
            if (searchQuery.length() + chr.length() <= 24) searchQuery += chr;
            return true;
        }
        return super.charTyped(input);
    }

    @Override
    public boolean keyPressed(KeyInput input) {
        int keyCode = input.key();
        if (selectedPage == 1 && searchFocused) {
            if (keyCode == 259) {
                if (!searchQuery.isEmpty()) searchQuery = searchQuery.substring(0, searchQuery.length() - 1);
                return true;
            }
            if (keyCode == 256) {
                searchFocused = false;
                return true;
            }
        }
        return super.keyPressed(input);
    }

    private java.util.List<String> getFilteredPlayers() {
        if (searchQuery.isEmpty()) return players;
        String query = searchQuery.toLowerCase(java.util.Locale.ROOT);
        java.util.List<String> filtered = new java.util.ArrayList<>();
        for (String name : players) {
            if (name.toLowerCase(java.util.Locale.ROOT).contains(query)) filtered.add(name);
        }
        return filtered;
    }

    private static long parseLong(String[] values, int index) {
        try { return values.length > index ? Math.max(0L, Long.parseLong(values[index])) : 0L; }
        catch (RuntimeException exception) { return 0L; }
    }

    private static String formatDuration(long milliseconds) {
        long minutes = milliseconds / 60_000L;
        long days = minutes / 1_440L;
        long hours = (minutes % 1_440L) / 60L;
        long remainder = minutes % 60L;
        if (days > 0) return days + "d " + hours + "h";
        if (hours > 0) return hours + "h " + remainder + "m";
        return remainder + "m";
    }


    private static void drawOutline(DrawContext context, int x, int y, int width, int height, int color) {
        if (width <= 0 || height <= 0) return;
        context.fill(x, y, x + width, y + 1, color);
        context.fill(x, y + height - 1, x + width, y + height, color);
        context.fill(x, y, x + 1, y + height, color);
        context.fill(x + width - 1, y, x + width, y + height, color);
    }

    private void drawPreference(DrawContext context, int x, int y, String label, boolean enabled) {
        int width = 218;
        context.fill(x, y, x + width, y + 22, PANEL_ALT);
        drawOutline(context, x, y, width, 22, enabled ? GREEN : 0xFF34443A);
        context.drawTextWithShadow(textRenderer, label, x + 8, y + 7, TEXT);
        context.drawTextWithShadow(textRenderer, enabled ? "ON" : "OFF", x + width - 34, y + 7,
                enabled ? GREEN : MUTED);
    }

    private void drawNav(DrawContext context, int x, int y, String label, boolean selected) {
        if (selected) {
            context.fill(x - 6, y - 5, x + 144, y + 16, PANEL_ALT);
            context.fill(x - 6, y - 5, x - 3, y + 16, GREEN);
        }
        context.drawTextWithShadow(textRenderer, label, x, y, selected ? 0xFFFFFFFF : MUTED);
    }
    private void drawCard(DrawContext context, int x, int y, int w, int h, String heading, String value, String caption) {
        context.fill(x, y, x + w, y + h, PANEL);
        context.fill(x, y, x + 2, y + h, GREEN);
        context.drawTextWithShadow(textRenderer, heading, x + 12, y + 10, MUTED);
        context.drawTextWithShadow(textRenderer, value, x + 12, y + 27, GREEN);
        context.drawTextWithShadow(textRenderer, caption, x + 12, y + 51, MUTED);
    }
    @Override public boolean shouldPause() { return false; }
}
