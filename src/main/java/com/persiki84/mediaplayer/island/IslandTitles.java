package com.persiki84.mediaplayer.island;

import com.persiki84.mediaplayer.lyrics.LyricLine;
import com.persiki84.mediaplayer.lyrics.LyricText;
import com.persiki84.mediaplayer.media.MediaTrack;
import com.persiki84.mediaplayer.render.Line;
import com.persiki84.mediaplayer.render.MorphText;
import com.persiki84.mediaplayer.render.Sweep;
import com.persiki84.mediaplayer.render.Weight;
import net.minecraft.client.gui.Font;

public final class IslandTitles {
    private static final int FIELD_LIMIT = 200;

    final MorphText title = new MorphText(Weight.SEMIBOLD);
    final MorphText artist = new MorphText(Weight.SEMIBOLD);
    final MorphText timing = new MorphText(Weight.SEMIBOLD);
    final MorphText pillRow = new MorphText(Weight.SEMIBOLD);

    private final IslandLyrics lyrics = new IslandLyrics();
    private final Line titleLine = new Line(Weight.SEMIBOLD);
    private MediaTrack shown = MediaTrack.NONE;
    private String titleText = "";
    private String artistText = "";
    private String pairedArtist = "";
    private LyricLine sung;
    private long shownPlayed = -1L;
    private long shownWhole = -1L;

    public boolean refresh(MediaTrack track, boolean visible, float delta) {
        title.advance(delta);
        artist.advance(delta);
        timing.advance(delta);
        pillRow.advance(delta);
        boolean changed = !track.sameTrack(shown);
        boolean announced = changed && visible && shown.present();
        if (changed) retitle(track);
        long now = System.currentTimeMillis();
        sing(lyrics.line(track, now));
        stamp(Math.max(0L, IslandClock.elapsedMs(track, now) / 1000L), Math.max(0L, track.durationMs() / 1000L));
        return announced;
    }

    // WHY: название и исполнитель приходят со страницы или из плеера: чистятся от управляющих и
    // WHY: форматных символов, знака параграфа и залго и режутся по длине, иначе строка из тысяч
    // WHY: невидимых знаков рисовалась бы десятками тысяч вызовов за кадр. У слепого трека название
    // WHY: есть, только если плеер пишет его в заголовок окна, иначе в строке площадка или плеер
    private void retitle(MediaTrack track) {
        shown = track;
        shownPlayed = -1L;
        String cleanTitle = LyricText.clean(track.title(), FIELD_LIMIT);
        String cleanArtist = LyricText.clean(track.artist(), FIELD_LIMIT);
        titleText = track.blind() && cleanTitle.isEmpty() ? track.origin() : cleanTitle;
        artistText = cleanArtist.isEmpty() ? track.origin() : cleanArtist;
        titleLine.set(titleText);
        pairedArtist = titleText.isEmpty() ? artistText : artistText + " · " + titleText;
    }

    // WHY: пока поётся строка, она стоит на месте названия, а название уходит к исполнителю в
    // WHY: карточке: так видно и что поют, и что играет
    private void sing(LyricLine line) {
        sung = line;
        if (line == null) {
            title.set(titleText);
            artist.set(artistText);
            return;
        }
        title.set(line.text(), IslandLyrics.pace(line));
        artist.set(pairedArtist);
    }

    Sweep sweep() {
        return sung == null ? Sweep.NONE : lyrics.sweep();
    }

    float mainWidth(Font font, float scale) {
        if (!lyrics.engaged()) return title.line().width(font, scale);
        return Math.max(lyrics.widest(font, scale), titleLine.width(font, scale));
    }

    // WHY: у трека без длины полосе нечего показывать, и строка таблетки отдаётся исполнителю
    // WHY: с отсчётом: в таблетке больше негде увидеть, кто играет
    private void stamp(long played, long whole) {
        if (played == shownPlayed && whole == shownWhole) return;

        shownPlayed = played;
        shownWhole = whole;
        if (whole > 0L) {
            timing.set(clock(played) + " / " + clock(whole));
            pillRow.set(timing.line().raw());
            return;
        }
        timing.set(clock(played));
        pillRow.set(artistText.isEmpty() ? clock(played) : artistText + " · " + clock(played));
    }

    public boolean untimed() {
        return shown.durationMs() <= 0L;
    }

    public boolean morphing() {
        return title.morphing() || artist.morphing() || timing.morphing() || pillRow.morphing();
    }

    private static String clock(long seconds) {
        return seconds / 60L + (seconds % 60L < 10L ? ":0" : ":") + seconds % 60L;
    }
}
