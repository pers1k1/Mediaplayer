package com.persiki84.mediaplayer.render;

import net.minecraft.client.gui.Font;
import net.minecraft.network.chat.Component;

public final class Line {
    private String raw = "";
    private Component value = Component.empty();
    private Component[] glyphs = new Component[0];
    private float[] offsets = new float[0];
    private float[] advances = new float[0];
    private float width = -1.0f;

    public void set(String next) {
        if (raw.equals(next)) return;

        raw = next;
        value = Component.literal(next);
        width = -1.0f;
        glyphs = null;
    }

    public void take(Line other) {
        raw = other.raw;
        value = other.value;
        glyphs = other.glyphs;
        offsets = other.offsets;
        advances = other.advances;
        width = other.width;
    }

    public String raw() {
        return raw;
    }

    public Component value() {
        return value;
    }

    public boolean isEmpty() {
        return raw.isEmpty();
    }

    public float width(Font font) {
        if (width < 0.0f) width = font.width(value);
        return width;
    }

    public Component[] glyphs(Font font) {
        if (glyphs == null) split(font);
        return glyphs;
    }

    public float[] offsets(Font font) {
        if (glyphs == null) split(font);
        return offsets;
    }

    public float[] advances(Font font) {
        if (glyphs == null) split(font);
        return advances;
    }

    private void split(Font font) {
        int[] points = raw.codePoints().toArray();
        glyphs = new Component[points.length];
        offsets = new float[points.length];
        advances = new float[points.length];
        float cursor = 0.0f;
        for (int index = 0; index < points.length; index++) {
            glyphs[index] = Component.literal(new String(Character.toChars(points[index])));
            advances[index] = font.width(glyphs[index]);
            offsets[index] = cursor;
            cursor += advances[index];
        }
    }
}
