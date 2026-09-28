package com.persiki84.mediaplayer.mixin;

import com.persiki84.mediaplayer.anim.FrameClock;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.renderer.GameRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(GameRenderer.class)
public abstract class GameRendererMixin {
    @Inject(method = "render(Lnet/minecraft/client/DeltaTracker;Z)V", at = @At("HEAD"))
    private void mediaplayer$tickFrameClock(DeltaTracker tracker, boolean advance, CallbackInfo callback) {
        FrameClock.advance();
    }
}
