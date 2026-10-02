package com.persiki84.mediaplayer.mixin;

import com.persiki84.mediaplayer.render.SoftGlyphs;
import net.minecraft.client.gui.font.FontTexture;
import net.minecraft.client.gui.font.GlyphRenderTypes;
import net.minecraft.client.renderer.texture.AbstractTexture;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.function.Supplier;

@Mixin(FontTexture.class)
public abstract class FontTextureMixin extends AbstractTexture {
    @Inject(method = "<init>", at = @At("TAIL"))
    private void mediaplayer$softenModAtlas(Supplier<String> label, GlyphRenderTypes types, boolean colored,
                                            CallbackInfo callback) {
        if (colored || !SoftGlyphs.claims(label.get())) return;

        sampler = SoftGlyphs.sampler();
        SoftGlyphs.remember(getTextureView());
    }
}
