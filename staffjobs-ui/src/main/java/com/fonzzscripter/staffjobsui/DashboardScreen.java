package com.fonzzscripter.staffjobsui;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;

/**
 * First visual shell for the StaffJobs staff dashboard.
 * Data panels are placeholders until the server-side dashboard data API is added.
 */
public final class DashboardScreen extends Screen {
    private static final int BACKGROUND = 0xF0101412;
    private static final int PANEL = 0xFF18211C;
    private static final int PANEL_ALT = 0xFF202B24;
    private static final int GREEN = 0xFF00E676;
    private static final int MUTED = 0xFF9AA79F;

    public DashboardScreen() {
        super(Text.literal("StaffJobs Dashboard"));
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        renderBackground(context);
        int left = Math.max(18, (width - 760) / 2);
        int top = Math.max(18, (height - 440) / 2);
        int right = Math.min(width - 18, left + 760);
        int bottom = Math.min(height - 18, top + 440);

        context.fill(left, top, right, bottom, BACKGROUND);
        context.fill(left, top, left + 172, bottom, PANEL);
        context.fill(left, top, right, top + 3, GREEN);

        context.drawTextWithShadow(textRenderer, "STAFFJOBS", left + 18, top + 18, GREEN);
        context.drawTextWithShadow(textRenderer, "UNIVERSAL 2.0", left + 18, top + 34, MUTED);

        drawNav(context, left + 14, top + 82, "OVERVIEW", true);
        drawNav(context, left + 14, top + 112, "PLAYERS", false);
        drawNav(context, left + 14, top + 142, "REPORT CENTER", false);
        drawNav(context, left + 14, top + 172, "STAFF ANALYTICS", false);
        drawNav(context, left + 14, top + 202, "ACTIVITY HISTORY", false);
        drawNav(context, left + 14, top + 232, "DEATH LOGS", false);
        drawNav(context, left + 14, top + 262, "PUNISHMENTS", false);
        drawNav(context, left + 14, top + 292, "SETTINGS", false);

        int contentLeft = left + 194;
        context.drawTextWithShadow(textRenderer, "Staff Dashboard", contentLeft, top + 22, 0xFFFFFFFF);
        context.drawTextWithShadow(textRenderer, "Server overview and moderation workspace", contentLeft, top + 40, MUTED);

        int gap = 12;
        int cardWidth = Math.max(100, (right - contentLeft - 28 - gap * 2) / 3);
        drawCard(context, contentLeft, top + 76, cardWidth, 72, "ONLINE PLAYERS", "--", "Waiting for server data");
        drawCard(context, contentLeft + cardWidth + gap, top + 76, cardWidth, 72, "OPEN REPORTS", "--", "Waiting for permission");
        drawCard(context, contentLeft + (cardWidth + gap) * 2, top + 76, cardWidth, 72, "STAFF ONLINE", "--", "Waiting for server data");

        int lowerTop = top + 164;
        context.fill(contentLeft, lowerTop, right - 18, bottom - 22, PANEL);
        context.drawTextWithShadow(textRenderer, "RECENT ACTIVITY", contentLeft + 16, lowerTop + 16, GREEN);
        context.drawTextWithShadow(textRenderer, "The secure server data endpoint is the next integration milestone.", contentLeft + 16, lowerTop + 42, MUTED);
        context.drawTextWithShadow(textRenderer, "No player information is requested by this UI shell yet.", contentLeft + 16, lowerTop + 58, MUTED);

        super.render(context, mouseX, mouseY, delta);
    }

    private void drawNav(DrawContext context, int x, int y, String label, boolean selected) {
        if (selected) {
            context.fill(x - 6, y - 5, x + 144, y + 16, PANEL_ALT);
            context.fill(x - 6, y - 5, x - 3, y + 16, GREEN);
        }
        context.drawTextWithShadow(textRenderer, label, x, y, selected ? 0xFFFFFFFF : MUTED);
    }

    private void drawCard(DrawContext context, int x, int y, int w, int h,
                          String heading, String value, String caption) {
        context.fill(x, y, x + w, y + h, PANEL);
        context.fill(x, y, x + 2, y + h, GREEN);
        context.drawTextWithShadow(textRenderer, heading, x + 12, y + 10, MUTED);
        context.drawTextWithShadow(textRenderer, value, x + 12, y + 27, GREEN);
        context.drawTextWithShadow(textRenderer, caption, x + 12, y + 51, MUTED);
    }

    @Override
    public boolean shouldPause() {
        return false;
    }
}
