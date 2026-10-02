package com.persiki84.mediaplayer.island;

import com.persiki84.mediaplayer.anim.Anim;
import com.persiki84.mediaplayer.media.MediaTrack;
import com.persiki84.mediaplayer.render.Line;
import com.persiki84.mediaplayer.render.Weight;

public final class IslandTitles {
    private static final float SWAP_SECONDS = 0.45f;

    final Line title = new Line(Weight.SEMIBOLD);
    final Line artist = new Line(Weight.REGULAR);
    final Line timing = new Line(Weight.REGULAR);
    final Line pillRow = new Line(Weight.REGULAR);
    private final Line leavingTitle = new Line(Weight.SEMIBOLD);
    private final Line leavingArtist = new Line(Weight.REGULAR);

    private MediaTrack shown = MediaTrack.NONE;
    private long shownPlayed = -1L;
    private long shownWhole = -1L;
    private float swap = 1.0f;

    interface Stroke {
        void draw(Line line, float alpha, float blur);
    }

    public boolean refresh(MediaTrack track, boolean visible, float delta) {
        swap = Math.min(1.0f, swap + delta / SWAP_SECONDS);
        boolean changed = !track.sameTrack(shown);
        boolean announced = changed && visible && shown.present();
        if (changed) retitle(track, announced);
        long now = System.currentTimeMillis();
        stamp(Math.max(0L, IslandClock.elapsedMs(track, now) / 1000L), Math.max(0L, track.durationMs() / 1000L));
        return announced;
    }

    // WHY: у слепого трека название есть, только если плеер пишет его в заголовок окна, иначе в
    // WHY: строке стоит площадка или плеер, с которого шёл звук
    private void retitle(MediaTrack track, boolean announced) {
        if (announced) {
            leavingTitle.take(title);
            leavingArtist.take(artist);
            swap = 0.0f;
        }
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
            pillRow.set(timing.raw());
            return;
        }
        timing.set(clock(played));
        pillRow.set(artist.isEmpty() ? clock(played) : artist.raw() + " · " + clock(played));
    }

    public boolean untimed() {
        return shown.durationMs() <= 0L;
    }

    // WHY: новое значение не уезжает и не подменяется, а перетекает на месте одной долей: старое
    // WHY: гаснет ровно настолько, насколько проявилось новое, так что строка не пустеет ни в один
    // WHY: кадр, а размытие обеих растёт к середине перехода и к концу снимается
    void swapped(Line now, boolean titleRow, float alpha, Stroke stroke) {
        if (swap >= 1.0f) {
            stroke.draw(now, alpha, 0.0f);
            return;
        }
        float share = Anim.smoothstep(0.0f, 1.0f, swap);
        float haze = 4.0f * share * (1.0f - share);
        stroke.draw(titleRow ? leavingTitle : leavingArtist, alpha * (1.0f - share), haze);
        stroke.draw(now, alpha * share, haze);
    }

    public boolean swapping() {
        return swap < 1.0f;
    }

    private static String clock(long seconds) {
        return seconds / 60L + (seconds % 60L < 10L ? ":0" : ":") + seconds % 60L;
    }
}
