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
- The bridge reads the current track, cover and playback state from the Windows media session and the audio level of that player, and sends play/pause/next/previous to that session when you press the keys. Nothing is downloaded, nothing is sent over the network.
- Both files are readable in the jar and in the [source repository](https://github.com/pers1k1/Mediaplayer/tree/main/src/main/resources/assets/glassmediaplayer/bridge).

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
- `K` открывает настройки. Двигать остров: открыть чат и тянуть мышью, колесо меняет размер, правая кнопка возвращает на место.

Требуется Minecraft 1.21.11, Fabric Loader 0.19.5+, Fabric API, для музыки Windows 10 или 11. Мост к плееру: PowerShell запускает скрипт из jar, тот собирает штатным компилятором Windows библиотеку на C# из исходника в том же jar. Мод ничего не скачивает и ничего не отправляет в сеть.
