package com.persiki84.mediaplayer.screen;

import com.persiki84.mediaplayer.color.Colors;
import com.persiki84.mediaplayer.color.Palette;
import com.persiki84.mediaplayer.config.IslandDial;
import com.persiki84.mediaplayer.config.IslandSettings;
import com.persiki84.mediaplayer.render.Ink;
import com.persiki84.mediaplayer.render.Paint;
import com.persiki84.mediaplayer.render.Weight;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.network.chat.Component;

import java.util.Locale;

final class DialRow {
    static final int FIELD_WIDTH = 34;
    private static final float LABEL_SCALE = 0.8f;
    private static final int MAX_LENGTH = 5;
    private static final int WELL_PAD = 3;
    private static final float FOCUS_TINT = 0.25f;

    private final IslandDial dial;
    private final Component label;
    private final EditBox field;
    private final int x;
    private final int y;
    private final int width;
    private final int height;

    DialRow(Font font, IslandDial dial, int x, int y, int width, int height) {
        this.dial = dial;
        this.label = Component.translatable(dial.translationKey());
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
        this.field = new EditBox(font, x + width - FIELD_WIDTH + WELL_PAD, y + (height - 8) / 2, FIELD_WIDTH - WELL_PAD,
                height, label);
        field.setBordered(false);
        field.setMaxLength(MAX_LENGTH);
        field.setFilter(DialRow::typeable);
        field.setValue(format(IslandSettings.dial(dial)));
        field.setResponder(this::accept);
        field.setTooltip(Tooltip.create(Component.translatable(dial.hintKey(), format(dial.min()),
                format(dial.max()))));
    }

    EditBox field() {
        return field;
    }

    private static boolean typeable(String text) {
        return text.chars().allMatch(symbol -> Character.isDigit(symbol) || symbol == '.' || symbol == ',');
    }

    private void accept(String text) {
        Float value = parse(text);
        boolean valid = value != null && value >= dial.min() && value <= dial.max();
        field.setTextColor(valid ? Palette.INK : Palette.PING_BAD);
        if (valid) IslandSettings.set(dial, value);
    }

    private static Float parse(String text) {
        try {
            return text.isEmpty() ? null : Float.parseFloat(text.replace(',', '.'));
        } catch (NumberFormatException malformed) {
            return null;
        }
    }

    static String format(float value) {
        return value == Math.rint(value) ? String.valueOf((int) value) : String.format(Locale.ROOT, "%.2f", value)
                .replaceAll("0+$", "");
    }

    void render(GuiGraphics graphics, Font font) {
        Ink.label(graphics, font, label, Weight.REGULAR, x, Ink.centerY(y, height, LABEL_SCALE), LABEL_SCALE,
                Palette.INK_DIM, 0.0f);
        int well = field.isFocused() ? Colors.mix(Palette.WELL_TOP, Palette.ACCENT, FOCUS_TINT) : Palette.WELL_TOP;
        Paint.shape(graphics, x + width - FIELD_WIDTH, y + 1, FIELD_WIDTH, height - 2, (height - 2) / 2.0f,
                well, Palette.WELL_BOTTOM, 0.0f);
    }
}
