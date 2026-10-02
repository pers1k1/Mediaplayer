package com.persiki84.mediaplayer.screen;

import com.persiki84.mediaplayer.anim.Anim;
import com.persiki84.mediaplayer.anim.FrameClock;
import com.persiki84.mediaplayer.anim.Spring;
import com.persiki84.mediaplayer.color.Colors;
import com.persiki84.mediaplayer.color.Palette;
import com.persiki84.mediaplayer.config.IslandDial;
import com.persiki84.mediaplayer.config.IslandFlag;
import com.persiki84.mediaplayer.config.IslandPlacement;
import com.persiki84.mediaplayer.config.IslandSettings;
import com.persiki84.mediaplayer.config.SettingsStore;
import com.persiki84.mediaplayer.render.Ink;
import com.persiki84.mediaplayer.render.Paint;
import com.persiki84.mediaplayer.render.Weight;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

public final class SettingsScreen extends Screen {
    private static final int COLUMN = 150;
    private static final int GUTTER = 18;
    private static final int PAD = 12;
    private static final int HEADER = 30;
    private static final int FOOTER = 40;
    private static final int ROW_MAX = 14;
    private static final int ROW_MIN = 11;
    private static final int BUTTON_WIDTH = 110;
    private static final int BUTTON_HEIGHT = 16;
    private static final float TITLE_SCALE = 1.0f;
    private static final float SECTION_SCALE = 0.7f;
    private static final float NOTE_SCALE = 0.6f;
    private static final float PANEL_RADIUS = 11.0f;
    private static final float ENTER_FROM = 0.94f;

    private final @Nullable Screen parent;
    private final List<DialRow> dials = new ArrayList<>();
    private final Spring entrance = new Spring(0.45f, 0.8f, 0.0f);
    private int panelX;
    private int panelY;
    private int panelWidth;
    private int panelHeight;
    private int row;

    public SettingsScreen(@Nullable Screen parent) {
        super(Component.translatable("mediaplayer.settings.title"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        int flags = IslandFlag.values().length;
        row = (int) Anim.clamp((height - HEADER - FOOTER - PAD) / (float) flags, ROW_MIN, ROW_MAX);
        panelWidth = PAD * 2 + COLUMN * 2 + GUTTER;
        panelHeight = HEADER + row * flags + FOOTER;
        panelX = (width - panelWidth) / 2;
        panelY = Math.max(4, (height - panelHeight) / 2);
        addToggles();
        addDials();
        addButtons();
    }

    private void addToggles() {
        int top = panelY + HEADER;
        IslandFlag[] flags = IslandFlag.values();
        for (int index = 0; index < flags.length; index++) {
            addRenderableWidget(new GlassToggle(panelX + PAD, top + index * row, COLUMN, row, flags[index]));
        }
    }

    private void addDials() {
        dials.clear();
        int left = panelX + PAD + COLUMN + GUTTER;
        int top = panelY + HEADER;
        IslandDial[] all = IslandDial.values();
        for (int index = 0; index < all.length; index++) {
            DialRow dial = new DialRow(font, all[index], left, top + index * (row + 2), COLUMN, row);
            dials.add(dial);
            addRenderableWidget(dial.field());
        }
    }

    private void addButtons() {
        int y = panelY + panelHeight - PAD - BUTTON_HEIGHT;
        int center = panelX + panelWidth / 2;
        addRenderableWidget(new GlassButton(center - BUTTON_WIDTH - 4, y, BUTTON_WIDTH, BUTTON_HEIGHT,
                Component.translatable("mediaplayer.settings.reset_place"),
                () -> IslandSettings.place(IslandPlacement.DEFAULT)));
        addRenderableWidget(new GlassButton(center + 4, y, BUTTON_WIDTH, BUTTON_HEIGHT, CommonComponents.GUI_DONE,
                this::onClose));
    }

    @Override
    public void renderBackground(GuiGraphics graphics, int mouseX, int mouseY, float partial) {
        if (minecraft != null && minecraft.level == null) {
            super.renderBackground(graphics, mouseX, mouseY, partial);
        } else {
            renderTransparentBackground(graphics);
        }
        float shown = entrance.to(1.0f, FrameClock.delta());
        float scale = Anim.lerp(ENTER_FROM, 1.0f, shown);
        float drawnWidth = panelWidth * scale;
        float drawnHeight = panelHeight * scale;
        Paint.glass(graphics, panelX + (panelWidth - drawnWidth) / 2.0f, panelY + (panelHeight - drawnHeight) / 2.0f,
                drawnWidth, drawnHeight, PANEL_RADIUS, Anim.clamp01(shown));
        for (DialRow dial : dials) dial.render(graphics, font);
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partial) {
        super.render(graphics, mouseX, mouseY, partial);
        float titleWidth = Ink.width(font, title, Weight.SEMIBOLD, Ink.fit(TITLE_SCALE));
        Ink.label(graphics, font, title, Weight.SEMIBOLD, panelX + (panelWidth - titleWidth) / 2.0f, panelY + 9,
                Ink.fit(TITLE_SCALE),
                Palette.INK, 0.0f);
        section(graphics, "mediaplayer.settings.section.show", panelX + PAD);
        section(graphics, "mediaplayer.settings.section.tune", panelX + PAD + COLUMN + GUTTER);
        Component credit = Component.translatable("mediaplayer.settings.credit");
        float creditWidth = Ink.width(font, credit, Weight.REGULAR, Ink.fit(NOTE_SCALE));
        Ink.label(graphics, font, credit, Weight.REGULAR, panelX + (panelWidth - creditWidth) / 2.0f,
                panelY + panelHeight - PAD - BUTTON_HEIGHT - 9, Ink.fit(NOTE_SCALE),
                Colors.alpha(Palette.INK_DIM, 0.6f), 0.0f);
        Component note = Component.translatable("mediaplayer.settings.drag_note");
        Ink.label(graphics, font, note, Weight.REGULAR, panelX + PAD + COLUMN + GUTTER,
                panelY + HEADER + IslandDial.values().length * (row + 2) + 6, Ink.fit(NOTE_SCALE),
                Colors.alpha(Palette.INK_DIM, 0.8f), 0.0f);
    }

    private void section(GuiGraphics graphics, String key, int x) {
        Ink.label(graphics, font, Component.translatable(key), Weight.REGULAR, x, panelY + HEADER - 9,
                Ink.fit(SECTION_SCALE),
                Colors.alpha(Palette.INK_DIM, 0.9f), 0.0f);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public void onClose() {
        SettingsStore.flush();
        if (minecraft != null) minecraft.setScreen(parent);
    }
}
