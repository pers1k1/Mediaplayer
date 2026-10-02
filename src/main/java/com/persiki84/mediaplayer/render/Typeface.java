package com.persiki84.mediaplayer.render;

import com.persiki84.mediaplayer.Mediaplayer;
import com.persiki84.mediaplayer.config.IslandFlag;
import com.persiki84.mediaplayer.config.IslandSettings;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FontDescription;

import java.util.IdentityHashMap;
import java.util.Map;

// WHY: шрифт тот же, что в BattleCraft (Inter, size 8.5), и хранится копиями с передискретизацией
// WHY: от 1 до 6 с шагом 0.25. Атлас читается со сглаживанием (SoftGlyphs), и берётся ближайшая
// WHY: копия не меньше плотности строки: растр только уменьшается, а уменьшение со сглаживанием
// WHY: даёт мягкие буквы без зерна на любом дробном размере
public final class Typeface {
    private static final float FINEST = 1.0f;
    private static final float STEP = 0.25f;
    private static final int STEPS = 21;
    private static final int CACHE_LIMIT = 512;
    private static final Object VANILLA = new Object();
    private static final Map<FontDescription, Map<Component, Component>> styled = new IdentityHashMap<>();
    private static final FontDescription[][] faces = new FontDescription[Weight.values().length][STEPS];

    static {
        for (Weight weight : Weight.values()) {
            for (int index = 0; index < STEPS; index++) {
                int hundredths = Math.round((FINEST + STEP * index) * 100.0f);
                faces[weight.ordinal()][index] = new FontDescription.Resource(
                        Mediaplayer.id(weight.face() + "_" + hundredths));
            }
        }
    }

    private Typeface() {}

    public static boolean modded() {
        return IslandSettings.on(IslandFlag.MOD_FONT);
    }

    public static Component styled(Component text, Weight weight, float pixelsPerUnit) {
        if (!modded()) return text;

        FontDescription face = face(weight, pixelsPerUnit);
        Map<Component, Component> cache = styled.computeIfAbsent(face, unused -> new IdentityHashMap<>());
        if (cache.size() > CACHE_LIMIT) cache.clear();
        return cache.computeIfAbsent(text, plain -> plain.copy().withStyle(style -> style.withFont(face)));
    }

    // WHY: ванильный шрифт пиксельный и читается без сглаживания, поэтому его кегль подгоняется к
    // WHY: целому числу пикселей вверх; шрифт мода читается со сглаживанием и кегль не трогает
    public static float fit(float scale, float basePixels) {
        float density = scale * basePixels;
        if (modded() || basePixels <= 0.0f || density <= 0.0f) return scale;

        return Math.max(1.0f, (float) Math.ceil(density - 0.05f)) / basePixels;
    }

    // WHY: игра округляет ширину буквы до пикселя растра той копии, которой строка рисуется, поэтому
    // WHY: мерить строку можно только той же копией: копия 1.0 давала другую ширину, и на длинном
    // WHY: названии бегущая строка и раскладка острова съезжали на несколько единиц
    public static Object faceKey(Weight weight, float pixelsPerUnit) {
        return modded() ? face(weight, pixelsPerUnit) : VANILLA;
    }

    private static FontDescription face(Weight weight, float pixelsPerUnit) {
        int index = (int) Math.ceil((pixelsPerUnit - FINEST) / STEP - 0.01f);
        return faces[weight.ordinal()][Math.max(0, Math.min(STEPS - 1, index))];
    }
}
