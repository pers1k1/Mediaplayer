package com.persiki84.mediaplayer;

import net.minecraft.resources.Identifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class Mediaplayer {
    public static final String MOD_ID = "mediaplayer";
    public static final Logger LOGGER = LoggerFactory.getLogger("Mediaplayer");

    private Mediaplayer() {}

    public static Identifier id(String path) {
        return Identifier.fromNamespaceAndPath(MOD_ID, path);
    }
}
