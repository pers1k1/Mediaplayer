(function GlassLyricsBridge() {
    const ENDPOINT = "http://127.0.0.1:47823/lyrics";
    const SPOTIFY_LYRICS = "https://spclient.wg.spotify.com/color-lyrics/v2/track/";
    const SPICY_CACHE = "SpicyLyrics_LyricsStore_g1";
    const SPICY_CACHE_VERSION = 6;
    const SPICY_POLL_MS = [0, 800, 1600, 3200, 6400];
    const RESEND_MS = 10000;
    const PARCEL_VERSION = 1;
    const SUNG = /[\p{L}\p{N}]/u;

    if (!window.Spicetify?.Player?.addEventListener || !window.Spicetify.CosmosAsync) {
        setTimeout(GlassLyricsBridge, 300);
        return;
    }

    let turn = 0;
    let unsent = null;

    const millis = (seconds) => Math.max(0, Math.round(Number(seconds) * 1000));
    const flat = (text) => String(text ?? "").replace(/[\r\n]+/g, " ");
    const pause = (ms) => new Promise((resolve) => setTimeout(resolve, ms));

    function trackOf(item) {
        const uri = item?.uri ?? "";
        if (!uri.startsWith("spotify:track:")) return null;
        const artists = item.artists?.map((artist) => artist.name).filter(Boolean).join(", ");
        return {
            id: uri.split(":")[2],
            title: item.name ?? item.metadata?.title ?? "",
            artist: artists || item.metadata?.artist_name || "",
            durationMs: Number(item.duration?.milliseconds ?? item.metadata?.duration ?? Spicetify.Player.getDuration()),
        };
    }

    function stamp(ms) {
        const minutes = Math.floor(ms / 60000);
        const seconds = Math.floor((ms % 60000) / 1000);
        const rest = ms % 1000;
        return `[${minutes}:${String(seconds).padStart(2, "0")}.${String(rest).padStart(3, "0")}]`;
    }

    function yrcLine(start, end, words) {
        return `[${start},${Math.max(1, end - start)}]${words}`;
    }

    function yrcWord(start, end, text) {
        return `(${start},${Math.max(1, end - start)},0)${text}`;
    }

    function syllableYrc(content) {
        return content.filter((line) => line?.Lead?.Syllables?.length).map((line) => {
            const words = line.Lead.Syllables.map((syllable) => yrcWord(millis(syllable.StartTime),
                millis(syllable.EndTime), flat(syllable.Text) + (syllable.IsPartOfWord ? "" : " "))).join("");
            return yrcLine(millis(line.Lead.StartTime), millis(line.Lead.EndTime), words.trimEnd());
        }).join("\n");
    }

    function lineYrc(content) {
        return content.filter((line) => flat(line?.Text).trim()).map((line) => {
            const start = millis(line.StartTime);
            const end = millis(line.EndTime);
            return yrcLine(start, end, yrcWord(start, end, flat(line.Text).trim()));
        }).join("\n");
    }

    function fromSpicyData(data) {
        if (!Array.isArray(data?.Content)) return null;
        if (data.Type === "Syllable") return { source: "spicy", format: "yrc", text: syllableYrc(data.Content) };
        if (data.Type === "Line") return { source: "spicy", format: "yrc", text: lineYrc(data.Content) };
        return null;
    }

    // WHY: Spicy Lyrics сам тянет текст на каждую смену трека и кладёт его в Cache Storage клиента.
    // WHY: Мост только читает этот кеш и не шлёт своих запросов на сервер Spicy Lyrics
    async function fromSpicy(id) {
        if (!("caches" in window) || !(await caches.has(SPICY_CACHE))) return null;
        const response = await (await caches.open(SPICY_CACHE)).match(`/${id}`);
        if (!response) return null;
        const stored = await response.json();
        if (stored?.CacheVersion !== SPICY_CACHE_VERSION || stored.ExpiresAt < Date.now()) return null;
        return fromSpicyData(stored.Content);
    }

    async function waitSpicy(id, mine) {
        for (const delay of SPICY_POLL_MS) {
            await pause(delay);
            if (mine !== turn) return null;
            const found = await fromSpicy(id).catch(() => null);
            if (found?.text) return found;
        }
        return null;
    }

    async function fromSpotify(id) {
        const url = `${SPOTIFY_LYRICS}${id}?format=json&vocalRemoval=false&market=from_token`;
        const body = await Spicetify.CosmosAsync.get(url).catch(() => null);
        const lyrics = body?.lyrics;
        if (lyrics?.syncType !== "LINE_SYNCED" || !Array.isArray(lyrics.lines)) return null;
        const text = lyrics.lines.filter((line) => SUNG.test(flat(line.words)))
            .map((line) => stamp(Number(line.startTimeMs)) + flat(line.words).trim()).join("\n");
        return text ? { source: "spotify", format: "lrc", text } : null;
    }

    async function deliver(parcel) {
        unsent = parcel;
        const reply = await fetch(ENDPOINT, {
            method: "POST",
            headers: { "Content-Type": "application/json" },
            body: JSON.stringify(parcel),
        }).catch(() => null);
        if (reply && unsent === parcel) unsent = null;
    }

    function parcelOf(track, lyrics) {
        return { v: PARCEL_VERSION, title: track.title, artist: track.artist, durationMs: track.durationMs, ...lyrics };
    }

    // WHY: сначала уходит построчный текст Spotify, чтобы остров не ждал, а пословный Spicy Lyrics
    // WHY: догоняет его, когда Spicy докачает свой: мод заменяет строчный текст пословным на ходу
    async function relay() {
        const track = trackOf(Spicetify.Player.data?.item);
        const mine = ++turn;
        unsent = null;
        if (!track) return;
        const spicy = waitSpicy(track.id, mine);
        const spotify = await fromSpotify(track.id);
        if (spotify && mine === turn) await deliver(parcelOf(track, spotify));
        const worded = await spicy;
        if (worded && mine === turn) await deliver(parcelOf(track, worded));
    }

    // WHY: игру запускают и посреди песни: неотправленная посылка повторяется, пока порт не ответит
    setInterval(() => {
        if (unsent) deliver(unsent);
    }, RESEND_MS);

    Spicetify.Player.addEventListener("songchange", relay);
    relay();
})();
