package com.persiki84.mediaplayer.render;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.FilterMode;
import com.mojang.blaze3d.textures.GpuSampler;
import com.mojang.blaze3d.textures.GpuTextureView;
import com.persiki84.mediaplayer.Mediaplayer;

import java.util.Collections;
import java.util.Set;
import java.util.WeakHashMap;

// WHY: игра читает атлас любого шрифта без сглаживания (NEAREST), это правильно для пиксельного
// WHY: ванильного шрифта, но у Inter буквы выходили жёсткими и зернистыми на любом дробном размере
// WHY: и положении. Атласы шрифтов мода читаются со сглаживанием: буквы мягкие, как в системных
// WHY: интерфейсах, а ванильный шрифт остаётся пиксельным
public final class SoftGlyphs {
    private static final String PREFIX = Mediaplayer.MOD_ID + ":";
    private static final Set<GpuTextureView> views = Collections.newSetFromMap(new WeakHashMap<>());

    private SoftGlyphs() {}

    public static boolean claims(String atlasName) {
        return atlasName.startsWith(PREFIX);
    }

    public static void remember(GpuTextureView view) {
        views.add(view);
    }

    public static boolean soft(GpuTextureView view) {
        return views.contains(view);
    }

    public static GpuSampler sampler() {
        return RenderSystem.getSamplerCache().getClampToEdge(FilterMode.LINEAR);
    }
}
