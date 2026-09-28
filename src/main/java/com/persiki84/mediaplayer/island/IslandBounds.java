package com.persiki84.mediaplayer.island;

public final class IslandBounds {
    private static boolean shown;
    private static float left;
    private static float top;
    private static float width;
    private static float height;

    private IslandBounds() {}

    static void set(float x, float y, float spanX, float spanY) {
        shown = true;
        left = x;
        top = y;
        width = spanX;
        height = spanY;
    }

    static void hide() {
        shown = false;
    }

    public static boolean contains(double x, double y) {
        return shown && x >= left && x <= left + width && y >= top && y <= top + height;
    }

    public static boolean shown() {
        return shown;
    }

    public static float left() {
        return left;
    }

    public static float top() {
        return top;
    }

    public static float width() {
        return width;
    }

    public static float height() {
        return height;
    }
}
