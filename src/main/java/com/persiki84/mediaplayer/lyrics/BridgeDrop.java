package com.persiki84.mediaplayer.lyrics;

record BridgeDrop(String title, String artist, long durationMs, LyricsOutcome outcome) {
    boolean fits(TrackQuery query) {
        return TrackMatch.fits(query, title, artist, durationMs / 1000.0);
    }
}
