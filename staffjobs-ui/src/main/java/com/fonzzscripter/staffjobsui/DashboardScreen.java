package com.fonzzscripter.staffjobsui;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;

/** Black-green dashboard with live read-only server overview cards. */
public final class DashboardScreen extends Screen {
    private static final int BACKGROUND = 0xF0101412, PANEL = 0xFF18211C,
            PANEL_ALT = 0xFF202B24, GREEN = 0xFF00E676, MUTED = 0xFF9AA79F;
    private static volatile int online = -1, maxPlayers = -1, staffOnline = -1;
    private static volatile java.util.List<String> players = java.util.Collections.emptyList();
    private static volatile boolean playersLoaded;
    private static volatile String[] selectedProfile = null;
    private static volatile String profileError = null;
    private int selectedPage = 0;
    private String searchQuery = "";
    private boolean searchFocused = false;

    public DashboardScreen() { super(Text.literal("StaffJobs Dashboard")); }

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
            context.drawBorder(searchLeft, searchTop, split - searchLeft - 10, 22, searchFocused ? GREEN : 0xFF34443A);
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
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0) {
            int left = Math.max(18, (width - 760) / 2);
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
                    }
                    return true;
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
    public boolean charTyped(char chr, int modifiers) {
        if (selectedPage == 1 && searchFocused && !Character.isISOControl(chr)) {
            if (searchQuery.length() < 24) searchQuery += chr;
            return true;
        }
        return super.charTyped(chr, modifiers);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
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
        return super.keyPressed(keyCode, scanCode, modifiers);
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
