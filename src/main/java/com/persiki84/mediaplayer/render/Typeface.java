package com.persiki84.mediaplayer.render;

import com.persiki84.mediaplayer.Mediaplayer;
import com.persiki84.mediaplayer.config.IslandFlag;
import com.persiki84.mediaplayer.config.IslandSettings;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FontDescription;

import java.util.IdentityHashMap;
import java.util.Map;

// WHY: шрифт тот же, что в BattleCraft (Inter, size 8.5), и хранится копиями с передискретизацией
// WHY: от 1 до 6 с шагом 0.25. Глиф чёткий, только когда его растр ложится на экран один к одному:
// WHY: копия с передискретизацией больше плотности строки ужимается и мылится, меньше - растягивается.
// WHY: Берётся ближайшая копия к настоящей плотности физических пикселей строки
public final class Typeface {
    private static final float FINEST = 1.0f;
    private static final float STEP = 0.25f;
    private static final int STEPS = 21;
    private static final int CACHE_LIMIT = 512;
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

    // WHY: атлас шрифта читается без сглаживания (NEAREST), и растр копии, чуть больше или меньше
    // WHY: плотности строки, дублирует или теряет столбцы пикселей - текст идёт зерном. Кегль строки
    // WHY: в покое поэтому подгоняется к ближайшей копии, чтобы растр лёг на экран один к одному;
    // WHY: ванильный пиксельный шрифт подгоняется к целому числу пикселей вверх
    public static float fit(float scale, float basePixels) {
        float density = scale * basePixels;
        if (basePixels <= 0.0f || density <= 0.0f) return scale;
        if (!modded()) return Math.max(1.0f, (float) Math.ceil(density - 0.05f)) / basePixels;
        if (density < FINEST || density > FINEST + STEP * (STEPS - 1)) return scale;

        return (FINEST + STEP * Math.round((density - FINEST) / STEP)) / basePixels;
    }

    public static Component measured(Component text, Weight weight) {
        return styled(text, weight, 1.0f);
    }

    private static FontDescription face(Weight weight, float pixelsPerUnit) {
        int index = Math.round((pixelsPerUnit - FINEST) / STEP);
        return faces[weight.ordinal()][Math.max(0, Math.min(STEPS - 1, index))];
    }
}
