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
- Name: ParimLocalMusicPlayer
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
- Shows songs, albums, artists, years, playlists, queue, and a small “memory” / analytics area.
- Supports playlist creation, rename, delete, save, load, and play.
- Tracks song plays only when a song has been listened to for at least 30 seconds.
- Tracks playlist plays only when at least 2 songs in that playlist qualified as played.
- Supports queue navigation, shuffle, repeat, seek, previous / next, and “play next”.
- Supports day / night / auto theme switching.

### Runtime assumptions
- The app resolves paths relative to the project root (`user.dir`).
- Expected local data files:
	- `data/music/`
	- `data/analytics/plays.json`
	- `data/playlists/playlists.json`
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
target/installer/ParimLocalMusicPlayer-1.0.0.dmg
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
	- Resolves day / night / auto theme.
	- Auto mode can use `-Dparim.latitude=... -Dparim.longitude=...`.
- `src/main/java/ui/MainView.java`
	- Main screen.
	- Holds navigation logic and view switching.
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
- Display formatting intentionally transforms values:
	- title -> lowercase
	- artist -> uppercase
	- album -> simple CamelCase

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
- Narrow vertical layout with 3 stacked zones:
	- navigation
	- content
	- player bar
- Search filters the current view.
- Main aesthetic goal: terminal-like / minimal / clean, not flashy.
- No cover art system yet.
- Onboarding includes:
	- filename pattern hint
	- open music folder
	- rescan library

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

### Good prompt context to give ChatGPT in a future session
Use something close to this:

```text
I am working on ParimLocalMusicPlayer, a Java 21 + JavaFX 21 local-first desktop music player built with Maven. The app scans data/music for mp3 and wav files, infers metadata mostly from filenames, stores analytics in data/analytics/plays.json and playlists in data/playlists/playlists.json using Jackson, and uses a layered structure with model/, repository/, service/, and ui/. Playback is handled by JavaFX MediaPlayer through PlaybackService. Analytics are only recorded after 30 seconds of listening, and playlist plays are only counted after 2 qualified songs. Main entry point is App via Launcher. Please preserve README.md lines 1-9 if editing that file.
```

### Short roadmap candidates already implied by the codebase
- Add artist / album analytics aggregation.
- Add monthly / yearly “memory” views.
- Add skip tracking.
- Replace or complement JSON with SQLite.
- Improve metadata parsing beyond filename conventions.
- Reduce portability issues caused by absolute-path song IDs.
