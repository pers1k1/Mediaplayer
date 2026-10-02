package com.persiki84.mediaplayer.render;

import net.minecraft.client.gui.Font;
import net.minecraft.network.chat.Component;

public final class Line {
    private static final String DIGITS = "0123456789";
    private static final Component[] DIGIT_GLYPHS = split(DIGITS);

    private final Weight weight;
    private String raw = "";
    private Component value = Component.empty();
    private Component[] glyphs = new Component[0];
    private float[] offsets = new float[0];
    private float[] insets = new float[0];
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

    public float[] insets(Font font, float scale) {
        measure(font, scale);
        return insets;
    }

    // WHY: цифры ставятся в клетку ширины самой широкой цифры, как табличные цифры шрифта: иначе
    // WHY: таймер на каждой секунде менял ширину, и всё, что стоит после изменившейся цифры, ехало
    private void measure(Font font, float scale) {
        float pixels = scale * Ink.base();
        Object face = Typeface.faceKey(weight, pixels);
        if (face == measuredFace) return;

        measuredFace = face;
        float cell = digitCell(font, pixels);
        offsets = new float[glyphs.length];
        insets = new float[glyphs.length];
        float cursor = 0.0f;
        for (int index = 0; index < glyphs.length; index++) {
            float advance = font.width(Typeface.styled(glyphs[index], weight, pixels));
            boolean digit = DIGITS.contains(glyphs[index].getString());
            insets[index] = digit ? wholePixels((cell - advance) / 2.0f, pixels) : 0.0f;
            offsets[index] = cursor;
            cursor += digit ? cell : advance;
        }
        width = cursor;
    }

    private float digitCell(Font font, float pixels) {
        float widest = 0.0f;
        for (Component digit : DIGIT_GLYPHS) {
            widest = Math.max(widest, font.width(Typeface.styled(digit, weight, pixels)));
        }
        return widest;
    }

    private static float wholePixels(float units, float pixels) {
        return pixels <= 0.0f ? units : Math.round(units * pixels) / pixels;
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
