package com.persiki84.mediaplayer.render;

import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.FilterMode;
import com.mojang.blaze3d.textures.GpuTexture;
import com.mojang.blaze3d.textures.GpuTextureView;
import com.mojang.blaze3d.textures.TextureFormat;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.render.TextureSetup;
import org.jspecify.annotations.Nullable;

// WHY: GUI в 1.21.11 рисуется отложенно одним проходом в конце кадра, и стеклу нечего читать под
// WHY: собой: снимок кадра под стекло снимается в начале этого прохода, когда в кадре лежит мир.
// WHY: Копия идёт только в кадрах, где стекло действительно заявлено
public final class GlassBackdrop {
    private static final int USAGE = GpuTexture.USAGE_COPY_DST | GpuTexture.USAGE_TEXTURE_BINDING;

    private static @Nullable GpuTexture texture;
    private static @Nullable GpuTextureView view;
    private static boolean wanted;

    private GlassBackdrop() {}

    public static @Nullable TextureSetup claim() {
        RenderTarget main = Minecraft.getInstance().getMainRenderTarget();
        if (main.getColorTexture() == null) return null;

        fit(main.width, main.height);
        wanted = true;
        return TextureSetup.singleTexture(view, RenderSystem.getSamplerCache().getClampToEdge(FilterMode.LINEAR));
    }

    public static void capture() {
        if (!wanted || texture == null) return;

        wanted = false;
        GpuTexture source = Minecraft.getInstance().getMainRenderTarget().getColorTexture();
        if (source == null || !sameSize(source, texture)) return;
        RenderSystem.getDevice().createCommandEncoder()
                .copyTextureToTexture(source, texture, 0, 0, 0, 0, 0, texture.getWidth(0), texture.getHeight(0));
    }

    private static boolean sameSize(GpuTexture source, GpuTexture copy) {
        return source.getWidth(0) == copy.getWidth(0) && source.getHeight(0) == copy.getHeight(0);
    }

    private static void fit(int width, int height) {
        if (texture != null && texture.getWidth(0) == width && texture.getHeight(0) == height) return;

        release();
        texture = RenderSystem.getDevice().createTexture("Mediaplayer glass backdrop", USAGE, TextureFormat.RGBA8,
                width, height, 1, 1);
        view = RenderSystem.getDevice().createTextureView(texture);
    }

    public static void release() {
        if (view != null) view.close();
        if (texture != null) texture.close();
        view = null;
        texture = null;
    }
}
