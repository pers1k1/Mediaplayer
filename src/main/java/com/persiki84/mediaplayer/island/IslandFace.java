package com.persiki84.mediaplayer.island;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.FilterMode;
import com.mojang.blaze3d.textures.GpuSampler;
import com.persiki84.mediaplayer.color.Colors;
import com.persiki84.mediaplayer.color.Palette;
import com.persiki84.mediaplayer.config.IslandFlag;
import com.persiki84.mediaplayer.config.IslandSettings;
import com.persiki84.mediaplayer.render.ImageState;
import com.persiki84.mediaplayer.render.Paint;
import com.persiki84.mediaplayer.render.QuadArea;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.client.renderer.texture.AbstractTexture;
import net.minecraft.resources.Identifier;
import org.jspecify.annotations.Nullable;

public final class IslandFace {
    private static final float SKIN = 64.0f;
    private static final ImageState.UvArea WHOLE = new ImageState.UvArea(0.0f, 0.0f, 1.0f, 1.0f);
    private static final ImageState.UvArea HEAD = new ImageState.UvArea(8.0f / SKIN, 8.0f / SKIN, 16.0f / SKIN,
            16.0f / SKIN);
    private static final ImageState.UvArea HAT = new ImageState.UvArea(40.0f / SKIN, 8.0f / SKIN, 48.0f / SKIN,
            16.0f / SKIN);
    private static final float FLIP_DRIFT = 0.08f;

    private IslandFace() {}

    static void draw(GuiGraphics graphics, float centerX, float centerY, float size, float alpha, float blur,
                     boolean withArt) {
        if (alpha <= 0.01f || size <= 1.0f) return;

        if (withArt && IslandArt.ready()) {
            cover(graphics, centerX, centerY, size, alpha, blur);
            return;
        }
        if (awaited()) {
            placeholder(graphics, centerX, centerY, size, alpha, blur);
            return;
        }
        if (IslandSettings.on(IslandFlag.AVATAR)) head(graphics, centerX, centerY, size, alpha, blur);
    }

    // WHY: разворот идёт сжатием по ширине: до середины хода видна уходящая обложка, после неё
    // WHY: пришедшая, а сторона сдвига показывает, листнул игрок вперёд или назад
    private static void cover(GuiGraphics graphics, float centerX, float centerY, float size, float alpha,
                              float blur) {
        boolean turning = IslandFlip.turning() && IslandArt.carries();
        Identifier id = !turning || IslandFlip.fresh() ? IslandArt.texture() : IslandArt.carriedTexture();
        AbstractTexture texture = Minecraft.getInstance().getTextureManager().getTexture(id);
        float across = turning ? Math.max(0.02f, IslandFlip.squeeze()) : 1.0f;
        float drift = turning ? IslandFlip.way() * (1.0f - across) * size * FLIP_DRIFT : 0.0f;
        float half = size / 2.0f;
        QuadArea quad = new QuadArea(centerX + drift - half * across, centerY - half, centerX + drift + half * across,
                centerY + half);
        Paint.image(graphics, texture.getTextureView(), texture.getSampler(), quad, WHOLE,
                size * IslandImage.CORNER_SHARE * across, alpha, blur);
    }

    private static void head(GuiGraphics graphics, float centerX, float centerY, float size, float alpha,
                             float blur) {
        Identifier skin = ownSkin();
        if (skin == null) return;

        AbstractTexture texture = Minecraft.getInstance().getTextureManager().getTexture(skin);
        float half = size / 2.0f;
        QuadArea quad = new QuadArea(centerX - half, centerY - half, centerX + half, centerY + half);
        GpuSampler sampler = RenderSystem.getSamplerCache().getClampToEdge(FilterMode.NEAREST);
        float radius = size * IslandImage.CORNER_SHARE;
        Paint.image(graphics, texture.getTextureView(), sampler, quad, HEAD, radius, alpha, blur);
        Paint.image(graphics, texture.getTextureView(), sampler, quad, HAT, radius, alpha, blur);
    }

    private static void placeholder(GuiGraphics graphics, float centerX, float centerY, float size, float alpha,
                                    float blur) {
        int shade = Colors.alpha(Palette.SHADE, alpha);
        Paint.shape(graphics, centerX - size / 2.0f, centerY - size / 2.0f, size, size,
                size * IslandImage.CORNER_SHARE, shade, shade, blur);
    }

    // WHY: обложка догружается мостом, и подставлять на это время голову игрока нельзя: в карточке
    // WHY: трека мелькнула бы чужая картинка. Место держится пустой площадкой, но только пока
    // WHY: обложка действительно ожидается, иначе у трека без обложки площадка висела бы вечно
    private static boolean awaited() {
        return IslandSettings.on(IslandFlag.COVER) && IslandModel.media() > 0.02f && IslandModel.blind() < 0.5f
                && !IslandArt.ready() && IslandArt.pending();
    }

    private static @Nullable Identifier ownSkin() {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null || minecraft.getConnection() == null) return null;

        PlayerInfo info = minecraft.getConnection().getPlayerInfo(minecraft.player.getUUID());
        return info == null ? null : info.getSkin().body().texturePath();
    }
}
