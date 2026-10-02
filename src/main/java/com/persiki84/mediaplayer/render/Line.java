package com.persiki84.mediaplayer.render;

import net.minecraft.client.gui.Font;
import net.minecraft.network.chat.Component;

public final class Line {
    private final Weight weight;
    private String raw = "";
    private Component value = Component.empty();
    private Component[] glyphs = new Component[0];
    private float[] offsets = new float[0];
    private float width;
    private Object measuredFace;

    public Line(Weight weight) {
        this.weight = weight;
    }

    public void set(String next) {
        if (raw.equals(next)) return;

        raw = next;
        value = Component.literal(next);
        glyphs = split(next);
        measuredFace = null;
    }

    public void take(Line other) {
        raw = other.raw;
        value = other.value;
        glyphs = other.glyphs;
        measuredFace = null;
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

    public float width(Font font, float scale) {
        measure(font, scale);
        return width * scale;
    }

    public Component[] glyphs() {
        return glyphs;
    }

    public float[] offsets(Font font, float scale) {
        measure(font, scale);
        return offsets;
    }

    // WHY: Font.width округляет ширину вверх до целой единицы, и буква, мерянная отдельно, получала
    // WHY: до единицы лишнего: в строке по буквам промежутки разъезжались на пиксели. Ширина берётся
    // WHY: дробной, прямо из раскладчика строк
    private void measure(Font font, float scale) {
        float pixels = scale * Ink.base();
        Object face = Typeface.faceKey(weight, pixels);
        if (face == measuredFace) return;

        measuredFace = face;
        offsets = new float[glyphs.length];
        float cursor = 0.0f;
        for (int index = 0; index < glyphs.length; index++) {
            offsets[index] = cursor;
            cursor += font.getSplitter().stringWidth(Typeface.styled(glyphs[index], weight, pixels));
        }
        width = cursor;
    }

    private static Component[] split(String text) {
        int[] points = text.codePoints().toArray();
        Component[] parts = new Component[points.length];
        for (int index = 0; index < points.length; index++) {
            parts[index] = Component.literal(new String(Character.toChars(points[index])));
        }
        return parts;
    }
}
