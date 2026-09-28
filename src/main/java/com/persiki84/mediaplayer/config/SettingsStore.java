package com.persiki84.mediaplayer.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.persiki84.mediaplayer.Mediaplayer;
import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

public final class SettingsStore {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final long SAVE_DELAY_MS = 600L;

    private static boolean dirty;
    private static long changedAt;

    private SettingsStore() {}

    private static Path file() {
        return FabricLoader.getInstance().getConfigDir().resolve(Mediaplayer.MOD_ID + ".json");
    }

    static void changed() {
        dirty = true;
        changedAt = System.currentTimeMillis();
    }

    public static void tick() {
        if (dirty && System.currentTimeMillis() - changedAt >= SAVE_DELAY_MS) flush();
    }

    public static void flush() {
        if (!dirty) return;

        dirty = false;
        save();
    }

    public static void load() {
        IslandSettings.reset();
        Path path = file();
        if (!Files.isRegularFile(path)) return;

        try {
            JsonObject root = JsonParser.parseString(Files.readString(path, StandardCharsets.UTF_8)).getAsJsonObject();
            readFlags(section(root, "flags"));
            readDials(section(root, "dials"));
            readPlacement(section(root, "placement"));
        } catch (Exception error) {
            Mediaplayer.LOGGER.warn("Settings file unreadable, defaults kept: {}", error.toString());
        }
    }

    private static JsonObject section(JsonObject root, String name) {
        JsonElement element = root.get(name);
        return element != null && element.isJsonObject() ? element.getAsJsonObject() : new JsonObject();
    }

    private static void readFlags(JsonObject flags) {
        for (IslandFlag flag : IslandFlag.values()) {
            JsonElement value = flags.get(flag.key());
            if (value != null && value.isJsonPrimitive() && value.getAsJsonPrimitive().isBoolean()) {
                IslandSettings.load(flag, value.getAsBoolean());
            }
        }
    }

    private static void readDials(JsonObject dials) {
        for (IslandDial dial : IslandDial.values()) {
            JsonElement value = dials.get(dial.key());
            if (value != null && value.isJsonPrimitive() && value.getAsJsonPrimitive().isNumber()) {
                IslandSettings.load(dial, value.getAsFloat());
            }
        }
    }

    private static void readPlacement(JsonObject placement) {
        IslandPlacement fallback = IslandPlacement.DEFAULT;
        IslandSettings.load(new IslandPlacement(number(placement, "center", fallback.centerShare()),
                number(placement, "top", fallback.topShare()), number(placement, "scale", fallback.scale())));
    }

    private static float number(JsonObject section, String key, float fallback) {
        JsonElement value = section.get(key);
        boolean numeric = value != null && value.isJsonPrimitive() && value.getAsJsonPrimitive().isNumber();
        return numeric ? value.getAsFloat() : fallback;
    }

    private static void save() {
        Path path = file();
        Path temporary = path.resolveSibling(path.getFileName() + ".tmp");
        try {
            Files.createDirectories(path.getParent());
            Files.writeString(temporary, GSON.toJson(snapshot()), StandardCharsets.UTF_8);
            move(temporary, path);
        } catch (IOException error) {
            Mediaplayer.LOGGER.warn("Settings not saved: {}", error.toString());
        }
    }

    private static void move(Path from, Path to) throws IOException {
        try {
            Files.move(from, to, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
        } catch (AtomicMoveNotSupportedException unsupported) {
            Files.move(from, to, StandardCopyOption.REPLACE_EXISTING);
        }
    }

    private static JsonObject snapshot() {
        JsonObject flags = new JsonObject();
        for (IslandFlag flag : IslandFlag.values()) flags.addProperty(flag.key(), IslandSettings.on(flag));
        JsonObject dials = new JsonObject();
        for (IslandDial dial : IslandDial.values()) dials.addProperty(dial.key(), IslandSettings.dial(dial));
        IslandPlacement placement = IslandSettings.placement();
        JsonObject place = new JsonObject();
        place.addProperty("center", placement.centerShare());
        place.addProperty("top", placement.topShare());
        place.addProperty("scale", placement.scale());
        JsonObject root = new JsonObject();
        root.add("flags", flags);
        root.add("dials", dials);
        root.add("placement", place);
        return root;
    }
}
