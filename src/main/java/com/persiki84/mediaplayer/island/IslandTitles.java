package com.persiki84.mediaplayer.island;

import com.persiki84.mediaplayer.media.MediaTrack;
import com.persiki84.mediaplayer.render.MorphText;
import com.persiki84.mediaplayer.render.Weight;

public final class IslandTitles {
    final MorphText title = new MorphText(Weight.SEMIBOLD);
    final MorphText artist = new MorphText(Weight.SEMIBOLD);
    final MorphText timing = new MorphText(Weight.SEMIBOLD);
    final MorphText pillRow = new MorphText(Weight.SEMIBOLD);

    private MediaTrack shown = MediaTrack.NONE;
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
        stamp(Math.max(0L, IslandClock.elapsedMs(track, now) / 1000L), Math.max(0L, track.durationMs() / 1000L));
        return announced;
    }

    // WHY: у слепого трека название есть, только если плеер пишет его в заголовок окна, иначе в
    // WHY: строке стоит площадка или плеер, с которого шёл звук
    private void retitle(MediaTrack track) {
        shown = track;
        shownPlayed = -1L;
        title.set(track.blind() && track.title().isEmpty() ? track.origin() : track.title());
        artist.set(track.artist().isEmpty() ? track.origin() : track.artist());
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
        String artistName = artist.line().raw();
        pillRow.set(artistName.isEmpty() ? clock(played) : artistName + " · " + clock(played));
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
