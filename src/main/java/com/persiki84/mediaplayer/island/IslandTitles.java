package com.persiki84.mediaplayer.island;

import com.persiki84.mediaplayer.anim.Anim;
import com.persiki84.mediaplayer.media.MediaTrack;
import com.persiki84.mediaplayer.render.Line;
import net.minecraft.client.gui.GuiGraphics;

public final class IslandTitles {
    private static final float SWAP_SECONDS = 0.3f;
    private static final float SWAP_LIFT = 4.0f;
    private static final float LEAVE_UNTIL = 0.6f;
    private static final float ENTER_FROM = 0.3f;

    final Line title = new Line();
    final Line artist = new Line();
    final Line timing = new Line();
    final Line pillRow = new Line();
    private final Line leavingTitle = new Line();
    private final Line leavingArtist = new Line();

    private MediaTrack shown = MediaTrack.NONE;
    private long shownPlayed = -1L;
    private long shownWhole = -1L;
    private float swap = 1.0f;

    interface Stroke {
        void draw(Line line, float alpha);
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

    void swapped(GuiGraphics graphics, Line now, boolean titleRow, float alpha, Stroke stroke) {
        if (swap >= 1.0f) {
            stroke.draw(now, alpha);
            return;
        }
        float leave = Anim.smoothstep(0.0f, LEAVE_UNTIL, swap);
        float enter = Anim.smoothstep(ENTER_FROM, 1.0f, swap);
        lifted(graphics, -SWAP_LIFT * leave, () -> stroke.draw(titleRow ? leavingTitle : leavingArtist,
                alpha * (1.0f - leave)));
        lifted(graphics, SWAP_LIFT * (1.0f - enter), () -> stroke.draw(now, alpha * enter));
    }

    private static void lifted(GuiGraphics graphics, float lift, Runnable body) {
        graphics.pose().pushMatrix();
        graphics.pose().translate(0.0f, lift);
        try {
            body.run();
        } finally {
            graphics.pose().popMatrix();
        }
    }

    private static String clock(long seconds) {
        return seconds / 60L + (seconds % 60L < 10L ? ":0" : ":") + seconds % 60L;
    }
}
