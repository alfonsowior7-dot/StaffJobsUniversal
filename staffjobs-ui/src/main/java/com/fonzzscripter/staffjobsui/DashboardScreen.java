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
    private int selectedPage = 0;

    public DashboardScreen() { super(Text.literal("StaffJobs Dashboard")); }

    public static void updateOverview(int currentOnline, int maximum, int currentStaff) {
        online = currentOnline; maxPlayers = maximum; staffOnline = currentStaff;
    }
    public static void clearOverview() { online = -1; maxPlayers = -1; staffOnline = -1; players = java.util.Collections.emptyList(); playersLoaded = false; }
    public static void updatePlayers(java.util.List<String> names) { players = java.util.Collections.unmodifiableList(new java.util.ArrayList<>(names)); playersLoaded = true; }

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
            context.fill(contentLeft, panelTop, right - 18, bottom - 22, PANEL);
            context.drawTextWithShadow(textRenderer, playersLoaded ? "ONLINE (" + players.size() + ")" : "LOADING PLAYERS...", contentLeft + 16, panelTop + 16, GREEN);
            if (playersLoaded && players.isEmpty()) {
                context.drawTextWithShadow(textRenderer, "No players are currently online.", contentLeft + 16, panelTop + 42, MUTED);
            } else {
                for (int i = 0; i < players.size() && i < 14; i++) {
                    int rowY = panelTop + 42 + i * 22;
                    context.fill(contentLeft + 12, rowY - 4, right - 30, rowY + 15, (i % 2 == 0) ? PANEL_ALT : PANEL);
                    context.drawTextWithShadow(textRenderer, players.get(i), contentLeft + 22, rowY, 0xFFFFFFFF);
                }
                if (players.size() > 14) context.drawTextWithShadow(textRenderer, "Showing first 14 of " + players.size() + " online players.", contentLeft + 16, bottom - 38, MUTED);
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
                        StaffJobsUI.requestPlayers();
                    }
                    return true;
                }
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
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
