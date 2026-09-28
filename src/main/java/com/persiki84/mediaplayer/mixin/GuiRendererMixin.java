package com.persiki84.mediaplayer.mixin;

import com.mojang.blaze3d.buffers.GpuBufferSlice;
import com.persiki84.mediaplayer.render.GlassBackdrop;
import net.minecraft.client.gui.render.GuiRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(GuiRenderer.class)
public abstract class GuiRendererMixin {
    @Inject(method = "render(Lcom/mojang/blaze3d/buffers/GpuBufferSlice;)V", at = @At("HEAD"))
    private void mediaplayer$captureGlassBackdrop(GpuBufferSlice fog, CallbackInfo callback) {
        GlassBackdrop.capture();
    }
}
