package com.persiki84.mediaplayer.render;

import com.persiki84.mediaplayer.Mediaplayer;
import com.persiki84.mediaplayer.config.IslandFlag;
import com.persiki84.mediaplayer.config.IslandSettings;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FontDescription;

import java.util.IdentityHashMap;
import java.util.Map;

// WHY: шрифт тот же, что в BattleCraft (Inter, size 8.5), и так же хранится несколькими копиями с
// WHY: разной передискретизацией: строка острова рисуется в масштабе от трети до десятка физических
// WHY: пикселей на единицу, и одна копия либо мылит мелкий текст, либо рвёт крупный. Берётся
// WHY: ближайшая копия не меньше настоящей плотности пикселей строки
public final class Typeface {
    private static final int[] OVERSAMPLES = {1, 2, 3, 4, 6};
    private static final int CACHE_LIMIT = 512;
    private static final Map<FontDescription, Map<Component, Component>> styled = new IdentityHashMap<>();
    private static final FontDescription[][] faces = new FontDescription[Weight.values().length][OVERSAMPLES.length];

    static {
        for (Weight weight : Weight.values()) {
            for (int index = 0; index < OVERSAMPLES.length; index++) {
                faces[weight.ordinal()][index] = new FontDescription.Resource(
                        Mediaplayer.id(weight.face() + OVERSAMPLES[index]));
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

    public static Component measured(Component text, Weight weight) {
        return styled(text, weight, 1.0f);
    }

    private static FontDescription face(Weight weight, float pixelsPerUnit) {
        FontDescription[] row = faces[weight.ordinal()];
        for (int index = 0; index < OVERSAMPLES.length; index++) {
            if (OVERSAMPLES[index] >= pixelsPerUnit) return row[index];
        }
        return row[OVERSAMPLES.length - 1];
    }
}
