package com.persiki84.mediaplayer.render;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import org.joml.Matrix3x2f;
import org.jspecify.annotations.Nullable;

public record QuadArea(float left, float top, float right, float bottom) {
    public static QuadArea of(float x, float y, float width, float height, float margin) {
        return new QuadArea(x - margin, y - margin, x + width + margin, y + height + margin);
    }

    public float centerX() {
        return (left + right) / 2.0f;
    }

    public float centerY() {
        return (top + bottom) / 2.0f;
    }

    public @Nullable ScreenRectangle bounds(Matrix3x2f pose, @Nullable ScreenRectangle scissor) {
        int x = (int) Math.floor(left);
        int y = (int) Math.floor(top);
        int width = (int) Math.ceil(right) - x;
        int height = (int) Math.ceil(bottom) - y;
        ScreenRectangle drawn = new ScreenRectangle(x, y, width, height).transformMaxBounds(pose);
        return scissor != null ? scissor.intersection(drawn) : drawn;
    }

    public static float pixels(Matrix3x2f pose) {
        float scale = (float) Math.hypot(pose.m00(), pose.m01());
        return scale * Minecraft.getInstance().getWindow().getGuiScale();
    }

    public static short quarter(float value) {
        return (short) Math.max(Short.MIN_VALUE, Math.min(Short.MAX_VALUE, Math.round(value * 4.0f)));
    }
}
