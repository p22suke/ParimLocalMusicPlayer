# “Muusikalised P2evanimekirjad for 1 user (local-first)”;
data-driven personal music system;
always-open, side companion music system + memory tool;
bla
bla
bla
bla
bla
head readme.md failid on 9 rida pikad

## Current project brief for ChatGPT / Copilot

Important editing rule for future sessions: keep README.md lines 1-9 unchanged unless Liina explicitly says otherwise.

### Project identity
- Name: muLocalMusicList
- Type: local-first desktop music player
- Status: working MVP
- Language: Java 21
- UI stack: JavaFX 21 (`javafx-controls`, `javafx-media`)
- Persistence: local JSON files via Jackson
- Build tool: Maven
- Packaging: DMG on macOS, EXE profile for Windows

### What the app currently does
- Scans `data/music/` recursively for `.mp3` and `.wav` files.
- Builds a local library from filenames and folder structure.
- Plays audio with JavaFX `MediaPlayer`.
- Uses a day-2 dual-pane UI: global search, listView, unitView, and the existing player bar.
- listView has fixed tabs for playlists, albums, and artists.
- Clicking a listView item opens or refreshes a song context in unitView.
- unitView keeps a fixed `All Songs` tab plus closable context tabs for playlists, albums, and artists.
- Playback happens from unitView via double-click or `Enter` on a song row.
- Supports playlist creation, rename, delete, save, load, and play.
- Supports adding songs to playlists from the song-table context menu.
- Supports a pinned `Liked Music` playlist and adding the current song there from the player bar.
- Tracks song plays only when a song has been listened to for at least 30 seconds.
- Tracks playlist plays only when at least 2 songs in that playlist qualified as played.
- Supports queue navigation, shuffle, repeat, seek, previous / next, and “play next”.
- Supports day / night switching and a text display-mode toggle.

### Runtime assumptions
- The app resolves paths relative to the project root (`user.dir`).
- Expected local data files:
	- `data/music/`
	- `data/analytics/plays.json`
	- `data/playlists/playlists.json`
- The repo may also contain a committed sample bundle in `data/untitled folder/` for moving music between machines.
- The scanner still reads only `data/music/`, so sample files stored elsewhere must be copied or moved there before launch.
- Main runtime window is intentionally narrow and tall: `420 x 900` default.
- The stage is set to always-on-top.

### Current launch / build commands

Run locally:

```bash
mvn javafx:run
```

Run tests:

```bash
mvn test
```

Create macOS installer:

```bash
mvn -Pmac-installer clean package org.panteleyev:jpackage-maven-plugin:1.7.4:jpackage
```

Expected installer output:

```text
target/installer/muLocalMusicList-1.0.0.dmg
```

### Entry points and architecture
- `Launcher` forwards to `App`.
- `App` wires the application manually; there is no DI framework.
- Main layers:
	- `model/` = immutable or small stateful domain objects
	- `repository/` = JSON persistence adapters
	- `service/` = business logic and orchestration
	- `ui/` = JavaFX views / controls

### Important source files
- `src/main/java/App.java`
	- Creates services.
	- Resolves local data paths.
	- Builds `MainView`.
- `src/main/java/service/LibraryService.java`
	- Scans music files.
	- Extracts metadata from filename first.
	- Falls back to folder name for album and placeholders for missing values.
- `src/main/java/service/PlaybackService.java`
	- Wraps JavaFX `MediaPlayer`.
	- Owns queue state and playback behavior.
	- Sends qualified analytics events.
- `src/main/java/service/AnalyticsService.java`
	- Contains analytics qualification rules.
	- Aggregates song stats and top-song reporting.
- `src/main/java/service/PlaylistPersistenceService.java`
	- Maps stored playlist snapshots back to in-memory `Playlist` objects.
- `src/main/java/service/ThemeService.java`
	- Provides theme selection support for the JavaFX scene.
- `src/main/java/ui/MainView.java`
	- Main screen.
	- Coordinates global search, listView, unitView, theme toggle, and display-mode toggle.
- `src/main/java/ui/ListViewPane.java`
	- Fixed context-selector tabs and playlist actions.
- `src/main/java/ui/UnitViewPane.java`
	- Song-table surface for playback contexts.
- `src/main/java/ui/TabBar.java`
	- Shared horizontally scrollable tab component used by both panes.
- `src/main/resources/styles/day.css`
- `src/main/resources/styles/night.css`

### Metadata rules right now
- Supported audio extensions: `mp3`, `wav`.
- Preferred filename pattern:

```text
ARTIST - ALBUM - YEAR - TITLE.mp3
```

- Parsing fallbacks:
	- 4 parts: artist / album / year / title
	- 3 parts: artist / album / title, year becomes `00000`
	- 2 parts: artist / title, album falls back to parent folder or `Album`
	- otherwise title falls back to filename, artist = `Unknown Artist`, album = `Album`, year = `00000`
- Display formatting is now user-switchable from the main view:
	- `CAPS`
	- `lower`
	- `Karju`
	- `Lause`
- Default display mode can also be set with `-Dparim.displayMode=...`.

### Persistence format right now

Analytics JSON shape:

```json
{
	"plays": [
		{
			"songId": "/absolute/path/to/song.mp3",
			"timestamp": "2026-03-19T15:15:42.275919Z",
			"secondsPlayed": 160
		}
	],
	"playlistPlayCounts": {
		"Focus": 3
	}
}
```

Playlist JSON shape:

```json
{
	"playlists": [
		{
			"name": "myPlaylist3",
			"songIds": [
				"/absolute/path/to/song.mp3"
			]
		}
	]
}
```

Important detail: song IDs are currently absolute file paths, not generated UUIDs.

### Current UX / design specifics
- Minimal JavaFX UI with CSS styling.
- Narrow vertical layout with 4 stacked zones:
	- global search and controls
	- listView (tabs + content)
	- unitView (tabs + song table)
	- player bar
- One search field filters both listView content and the active unitView song list.
- unitView tabs are horizontally scrollable and behave like a lightweight browser tab strip.
- The `All Songs` tab is always present.
- Main aesthetic goal: terminal-like / minimal / clean, not flashy.
- No cover art system yet.
- The player bar includes seek, previous / next, play-pause, shuffle, repeat, and a like button.

### Current analytics rules
- Qualified song play threshold: `30` seconds.
- Qualified playlist play threshold: `2` qualified songs from the playlist session.
- Available analytics outputs in code today:
	- per-song play count
	- last played timestamp
	- total play time per song
	- playlist play count
	- top songs between dates
- Not implemented yet:
	- skip count
	- artist-level aggregate analytics
	- album-level aggregate analytics
	- SQLite backend

### Test coverage that already exists
- `PlaybackQueueTest`
	- repeat-all wrapping
	- shuffle keeps current song playable
	- queue end behavior
	- play-next insertion
- `AnalyticsServiceTest`
	- 30-second qualification rule
	- top songs aggregation
	- playlist play qualification rule
	- default playlist count
- `LibraryServiceTest`
	- fallback album behavior for root-level files
	- missing year placeholder behavior
- `PlaylistPersistenceServiceTest`
	- loading playlists from stored song IDs
	- saving playlists back to snapshots
- `JsonPlaylistRepositoryTest` also exists in the repo.

### Known current constraints / design choices
- No external metadata tagging library is used yet.
- Metadata is inferred from filenames and folders, not ID3 tags.
- JSON is the MVP persistence mechanism.
- There is no dependency injection container.
- App is local-first and assumes one user.
- Song paths are machine-local, so playlist / analytics portability is limited.
- Even though a sample bundle can be committed under `data/untitled folder/`, the runtime library source remains `data/music/`.

### Good prompt context to give ChatGPT in a future session
Use something close to this:

```text
I am working on muLocalMusicList, a Java 21 + JavaFX 21 local-first desktop music player built with Maven. The app scans data/music for mp3 and wav files, infers metadata mostly from filenames, stores analytics in data/analytics/plays.json and playlists in data/playlists/playlists.json using Jackson, and uses a dual-pane JavaFX UI with a global search row, a listView for playlists/albums/artists, a unitView for playable song contexts, and a player bar. Playback is handled by JavaFX MediaPlayer through PlaybackService. Analytics are only recorded after 30 seconds of listening, and playlist plays are only counted after 2 qualified songs. Main entry point is App via Launcher. Please preserve README.md lines 1-9 if editing that file.
```

### Short roadmap candidates already implied by the codebase
- Add artist / album analytics aggregation.
- Add monthly / yearly “memory” views.
- Add skip tracking.
- Replace or complement JSON with SQLite.
- Improve metadata parsing beyond filename conventions.
- Reduce portability issues caused by absolute-path song IDs.
