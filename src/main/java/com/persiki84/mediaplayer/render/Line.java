package com.persiki84.mediaplayer.render;

import net.minecraft.client.gui.Font;
import net.minecraft.network.chat.Component;

public final class Line {
    private final Weight weight;
    private String raw = "";
    private Component value = Component.empty();
    private Component[] glyphs;
    private float[] offsets = new float[0];
    private float[] advances = new float[0];
    private float width = -1.0f;
    private boolean measuredModded;

    public Line(Weight weight) {
        this.weight = weight;
    }

    public void set(String next) {
        if (raw.equals(next)) return;

        raw = next;
        value = Component.literal(next);
        forget();
    }

    public void take(Line other) {
        raw = other.raw;
        value = other.value;
        forget();
    }

    private void forget() {
        width = -1.0f;
        glyphs = null;
    }

    public String raw() {
        return raw;
    }

    public Component value() {
        return value;
    }

    public Weight weight() {
        return weight;
    }

    public boolean isEmpty() {
        return raw.isEmpty();
    }

    public float width(Font font) {
        refresh();
        if (width < 0.0f) width = font.width(Typeface.measured(value, weight));
        return width;
    }

    public Component[] glyphs(Font font) {
        refresh();
        if (glyphs == null) split(font);
        return glyphs;
    }

    public float[] offsets(Font font) {
        glyphs(font);
        return offsets;
    }

    public float[] advances(Font font) {
        glyphs(font);
        return advances;
    }

    private void refresh() {
        if (measuredModded == Typeface.modded()) return;

        measuredModded = Typeface.modded();
        forget();
    }

    private void split(Font font) {
        int[] points = raw.codePoints().toArray();
        glyphs = new Component[points.length];
        offsets = new float[points.length];
        advances = new float[points.length];
        float cursor = 0.0f;
        for (int index = 0; index < points.length; index++) {
            glyphs[index] = Component.literal(new String(Character.toChars(points[index])));
            advances[index] = font.width(Typeface.measured(glyphs[index], weight));
            offsets[index] = cursor;
            cursor += advances[index];
        }
    }
}
