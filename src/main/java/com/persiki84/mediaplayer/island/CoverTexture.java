package com.persiki84.mediaplayer.island;

import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.systems.CommandEncoder;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.FilterMode;
import com.mojang.blaze3d.textures.GpuTexture;
import com.mojang.blaze3d.textures.TextureFormat;
import net.minecraft.client.renderer.texture.AbstractTexture;

// WHY: обложка кладётся цепочкой уровней, ужатых усреднением по площади, и читается трилинейно:
// WHY: без мипмапов картинка в десятки пикселей из сотен шла рябью, а размытие содержимого
// WHY: на ходу острова берёт как раз верхние уровни цепочки
public final class CoverTexture extends AbstractTexture {
    private static final int USAGE = GpuTexture.USAGE_COPY_DST | GpuTexture.USAGE_TEXTURE_BINDING;

    private final int edge;

    public CoverTexture(NativeImage[] levels) {
        RenderSystem.assertOnRenderThread();
        edge = levels[0].getWidth();
        texture = RenderSystem.getDevice().createTexture("Mediaplayer cover", USAGE, TextureFormat.RGBA8, edge, edge,
                1, levels.length);
        CommandEncoder encoder = RenderSystem.getDevice().createCommandEncoder();
        for (int level = 0; level < levels.length; level++) {
            NativeImage image = levels[level];
            encoder.writeToTexture(texture, image, level, 0, 0, 0, image.getWidth(), image.getHeight(), 0, 0);
        }
        textureView = RenderSystem.getDevice().createTextureView(texture);
        sampler = RenderSystem.getSamplerCache().getClampToEdge(FilterMode.LINEAR, true);
    }

    public int edge() {
        return edge;
    }

    public static void discard(NativeImage[] levels) {
        if (levels == null) return;

        for (NativeImage level : levels) {
            if (level != null) level.close();
        }
    }
}
