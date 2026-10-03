package com.persiki84.mediaplayer.island;

import com.persiki84.mediaplayer.config.IslandDial;
import com.persiki84.mediaplayer.config.IslandFlag;
import com.persiki84.mediaplayer.config.IslandSettings;
import com.persiki84.mediaplayer.lyrics.LyricLine;
import com.persiki84.mediaplayer.lyrics.Lyrics;
import com.persiki84.mediaplayer.lyrics.LyricsService;
import com.persiki84.mediaplayer.media.MediaTrack;
import com.persiki84.mediaplayer.render.Ink;
import com.persiki84.mediaplayer.render.Weight;
import net.minecraft.client.gui.Font;
import net.minecraft.network.chat.Component;

import java.util.Arrays;

// WHY: во вступлении, в проигрыше дольше REST_MS и после последней строки в слоте стоит название:
// WHY: погасшая строка висела бы над тишиной, а пустой слот выглядел бы поломкой
final class IslandLyrics {
    private static final long LINGER_MS = 1500L;
    private static final long REST_MS = 4000L;
    private static final float MORPH_SHARE = 0.3f;
    private static final float MORPH_MIN_SECONDS = 0.14f;
    private static final float MORPH_MAX_SECONDS = 0.6f;

    private final LineSweep sweep = new LineSweep();
    private Lyrics lyrics = Lyrics.NONE;
    private LyricLine shown;
    private final float[] measuredScales = {Float.NaN, Float.NaN, Float.NaN};
    private final float[] widths = new float[3];
    private Lyrics measured;
    private float measuredBase;
    private int nextSlot;

    LyricLine line(MediaTrack track, long now) {
        Lyrics fresh = IslandSettings.on(IslandFlag.LYRICS) && track.present()
                ? LyricsService.lyrics(track) : Lyrics.NONE;
        if (fresh != lyrics) {
            lyrics = fresh;
            shown = null;
        }
        if (!lyrics.present()) return null;

        long at = track.elapsedMs(now) - Math.round(IslandSettings.dial(IslandDial.LYRICS_OFFSET) * 1000.0f);
        int index = lyrics.lineAt(at);
        if (index < 0 || resting(index, at)) return null;
        LyricLine line = lyrics.lines().get(index);
        if (line != shown) {
            shown = line;
            sweep.load(line);
        }
        sweep.time(at);
        return line;
    }

    private boolean resting(int index, long at) {
        LyricLine line = lyrics.lines().get(index);
        long quietFrom = line.endMs() + LINGER_MS;
        if (at <= quietFrom) return false;
        return index + 1 >= lyrics.lines().size() || lyrics.lines().get(index + 1).startMs() - quietFrom > REST_MS;
    }

    boolean engaged() {
        return lyrics.present();
    }

    LineSweep sweep() {
        return sweep;
    }

    static float pace(LyricLine line) {
        float seconds = line.spanMs() / 1000.0f * MORPH_SHARE;
        return Math.max(MORPH_MIN_SECONDS, Math.min(MORPH_MAX_SECONDS, seconds));
    }

    // WHY: ширина острова держится по самой длинной строке песни: подгонка под каждую строку
    // WHY: дёргала бы стекло каждые две-три секунды. Замер один раз на текст и кегль
    float widest(Font font, float scale) {
        float base = Ink.base();
        if (measured != lyrics || measuredBase != base) {
            measured = lyrics;
            measuredBase = base;
            Arrays.fill(measuredScales, Float.NaN);
        }
        for (int slot = 0; slot < measuredScales.length; slot++) {
            if (measuredScales[slot] == scale) return widths[slot];
        }
        int slot = nextSlot;
        nextSlot = (nextSlot + 1) % measuredScales.length;
        measuredScales[slot] = scale;
        widths[slot] = 0.0f;
        for (LyricLine line : lyrics.lines()) {
            widths[slot] = Math.max(widths[slot], Ink.width(font, Component.literal(line.text()), Weight.SEMIBOLD, scale));
        }
        return widths[slot];
    }
}
