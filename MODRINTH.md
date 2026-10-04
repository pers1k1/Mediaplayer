A liquid glass media island for the Minecraft HUD. It shows the track playing in Windows, its cover art, a live sound visualizer, FPS and ping, and moves like the Dynamic Island on iPhone: the springs were timed frame by frame from a screen recording of the phone.

Client-only. Not needed on the server.

## Features

- **Any Windows player.** Browsers (YouTube, SoundCloud, Spotify Web and others), Spotify, AIMP, foobar2000, Telegram, Windows Media Player: anything that publishes a Windows media session (SMTC).
- **Cover art.** Taken from the player, rounded and mipmapped so it stays clean at small sizes. On a track change the cover turns over like a card with perspective, one way for the next track and the other way for the previous one.
- **Visualizer.** Six bars follow the sound of the player itself, not the system mixer, so Discord and the game do not move them. The bars can take their colors from the cover.
- **Track card.** A new track opens the island into a card with the cover, title, artist, progress bar and timer, then it folds back into the pill.
- **Letter morph.** The title, artist and timer change letter by letter in a wave, the way labels change on iOS. The timer turns over a single digit each second.
- **FPS and ping.** Inside the island next to your name, or in their own capsule under it while music plays. The capsule can be hidden while music plays.
- **Glass.** Refraction along the rim, dispersion and rim light over the real game frame. The glass stays sharp while the island moves; only its contents blur.
- **Type.** Inter, smooth at any size, with a light glow instead of a dark outline. Switches to the vanilla font in settings.
- **Synced lyrics.** The line being sung takes the place of the title and lights up letter by letter with the voice, word by word where the lyrics carry word timings. Lines change with the same letter morph, paced by the song: short in fast verses, slow in ballads. The title moves next to the artist in the card, and comes back during the intro and long breaks. An offset setting fixes lyrics that run ahead of or behind the singing. Lyrics are on by default and can be switched off in settings.
- **Spotify bridge.** Listening in the Spotify desktop app with [Spicetify](https://spicetify.app)? The optional Glass Lyrics Bridge extension (from the [GitHub repository](https://github.com/pers1k1/Mediaplayer/tree/main/spicetify) or the release files) hands the island the lyrics Spotify itself shows, and the word-timed lyrics of Spicy Lyrics when that extension is installed. No token or cookie ever reaches the mod.

## Controls

- `K` opens the settings (also from ModMenu if installed).
- Play/pause, next and previous track have their own keys under **Controls > Media Player**, unbound by default. They go to the player session the island shows, never as a global media key, so a pause does not land in another browser tab.
- To move the island, open chat and drag it with the left mouse button. Scroll over it to resize from 0.5 to 2.5, right-click to put it back.

## Requirements

- Minecraft 1.21.11, Fabric Loader 0.19.5 or newer, [Fabric API](https://modrinth.com/mod/fabric-api).
- Music needs Windows 10 or 11. On other systems the island shows your name, FPS and ping.
- [ModMenu](https://modrinth.com/mod/modmenu) is optional.

## How the music bridge works

Minecraft has no access to Windows media sessions, so the mod talks to them through a small bridge, and it is better to say plainly what it does:

- On Windows the mod starts `powershell.exe` with the script `media-watch.ps1` that ships inside the jar. The execution policy is bypassed for that one script only, because the default Windows policy blocks unsigned scripts.
- The script compiles `media-native.cs`, also shipped inside the jar as plain source, with the C# compiler that comes with Windows (.NET Framework 4, `csc.exe`), and caches the library in `.minecraft/glassmediaplayer`.
- The bridge reads the current track, cover and playback state from the Windows media session, the audio level of that player (a loopback capture of that one process, used only for the visualizer), and the window titles of players and browsers that stop publishing a session, so the island can still name the source. It sends play/pause/next/previous to that session when you press the keys.
- The bridge writes only into `.minecraft/glassmediaplayer`: the script, the source, the compiled library (named by the hash of its source) and the current cover picture.
- Both files are readable in the jar and in the [source repository](https://github.com/pers1k1/Mediaplayer/tree/main/src/main/resources/assets/glassmediaplayer/bridge). The bridge itself never goes online.

## Network: lyrics only

The only network requests the mod makes are the lyrics lookup, and only while the Lyrics setting is on:

- They go over HTTPS to [lrclib.net](https://lrclib.net), a free public lyrics database, and to NetEase Cloud Music (music.163.com), whose word-timed lyrics know where a singer holds a word.
- It sends the title and the artist of the playing track. Nothing about you, your game or your computer.
- One request per track, only after the track has played for a moment, no more than once every 3 seconds and 10 times a minute. Found and missing lyrics are cached in `.minecraft/glassmediaplayer/lyrics`, so a song is looked up once.
- Switch Lyrics off in settings (`K`) and the mod makes no network requests at all.
- The Spotify bridge listens on `127.0.0.1:47823` only, never on the network, and accepts lyrics only from the Spotify app (origin `https://*.spotify.com`). It makes no requests of its own. Turn off **Spotify bridge** in settings to close the port.

## License and credits

AGPL-3.0-or-later with attribution terms, see [NOTICE](https://github.com/pers1k1/Mediaplayer/blob/main/NOTICE). Author: [pers1k1](https://github.com/pers1k1). The island comes from the BattleCraft mod by the same author.

The liquid glass look follows ReGlass by RedxAx. The Inter typeface is under the SIL Open Font License 1.1.

---

## На русском

Остров медиаплеера из жидкого стекла для HUD Minecraft. Показывает трек, который играет в Windows, обложку, визуализатор звука, FPS и пинг. Двигается как Dynamic Island на iPhone: ход снят покадрово с записи экрана телефона. Мод только клиентский.

- Трек из любого плеера Windows, который публикует медиасессию: браузеры, Spotify, AIMP, foobar2000, Telegram, Яндекс Музыка, VK.
- Обложка разворачивается при смене трека, сторона поворота зависит от того, листнули вперёд или назад.
- Визуализатор идёт от звука самого плеера, цвета полосок можно брать с обложки.
- Название, исполнитель и таймер меняются по буквам волной.
- FPS и пинг в острове или в своей капсуле под ним, капсулу можно скрыть на время музыки.
- Лирика: поющаяся строка стоит на месте названия и подсвечивается по буквам под голос, смена строк идёт в темпе песни.
- Мост Spotify: расширение Glass Lyrics Bridge для Spicetify отдаёт острову текст из десктопного Spotify и пословный текст Spicy Lyrics, токены в мод не попадают.
- `K` открывает настройки. Двигать остров: открыть чат и тянуть мышью, колесо меняет размер, правая кнопка возвращает на место.

Требуется Minecraft 1.21.11, Fabric Loader 0.19.5+, Fabric API, для музыки Windows 10 или 11. Мост к плееру: PowerShell запускает скрипт из jar, тот собирает штатным компилятором Windows библиотеку на C# из исходника в том же jar; мост в сеть не ходит. Единственные сетевые запросы это поиск лирики на lrclib.net и в NetEase Cloud Music по HTTPS: туда уходят название и исполнитель трека. Выключите «Лирику» в настройках, и мод не обращается к сети вовсе. Мост Spotify слушает только `127.0.0.1:47823` и сам запросов не шлёт.
