package com.persiki84.mediaplayer.mixin;

import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.textures.GpuTextureView;
import com.persiki84.mediaplayer.render.GlassPipelines;
import com.persiki84.mediaplayer.render.SoftGlyphs;
import net.minecraft.client.gui.font.TextRenderable;
import net.minecraft.client.gui.render.TextureSetup;
import net.minecraft.client.gui.render.state.GlyphRenderState;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(GlyphRenderState.class)
public abstract class GlyphRenderStateMixin {
    @Shadow
    @Final
    private TextRenderable renderable;

    @Inject(method = "textureSetup", at = @At("HEAD"), cancellable = true)
    private void mediaplayer$softSampler(CallbackInfoReturnable<TextureSetup> callback) {
        GpuTextureView view = renderable.textureView();
        if (!SoftGlyphs.soft(view)) return;

        callback.setReturnValue(TextureSetup.singleTexture(view, SoftGlyphs.sampler()));
    }

    @Inject(method = "pipeline", at = @At("HEAD"), cancellable = true)
    private void mediaplayer$softPipeline(CallbackInfoReturnable<RenderPipeline> callback) {
        if (SoftGlyphs.soft(renderable.textureView())) callback.setReturnValue(GlassPipelines.SOFT_TEXT);
    }
}
