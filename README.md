# ParimLocalMusicPlayer
-  for 1 user (local-first);
- always-open, side companion music system
## RETIRED FIRST SKETCHES that should make eventually a comeback:
1. shuffle
2. capslock/LOWERCASE formatting instead of colors>Display formatting 
	- `CAPS`
	- `lower`
	- `Karju`
	- `Lause`
- ideed, mida praegu EI TEE:
- volume control;
### head README.md failid on 9 rida pikad;

###DEAR AGENTS!! Important editing rules for future sessions: 
1. keep README.md lines 1-18 unchanged unless Liina explicitly says otherwise.
2. A human used to reading Dostojevski, Jelinek and scientific writing in five+ different human languages has to read this code=> 2.1 be precise in commenting. 2.2 always mark the code with the comment "LIINA!!!!!" where a root-level variable, class, method has been declared that Liina could change from nerd dude English to her wonderful kolkaplika estonian without breaking the dependencies.

### PLAYER identity
- Name: ParimLocalMusicPlayer
- ANNO 2026
- Type: local-first desktop music player
- Status: working javafx java21 project
- Language: Java 21
- UI stack: JavaFX 21 (`javafx-controls`, `javafx-media`)
- Persistence: local JSON files via Jackson
- Build tool: Maven
- Packaging: DMG on macOS, EXE profile for Windows

## HOW TO RUN
if you have all the code downloaded, java21, javafx21, maven installed: in the directory of the app, run "mvn javafx:run"

### HOW THE APP CURRENTLY RUNS
1. Opens your finder(win opens /explorer?) to choose the folder with music. standard: `data/music/.
2. the program scans the chosen 1 folder recursively for `.mp3` and `.wav` files.
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
	- `andmed/music/`
	- `andmed/analytics/plays.json`
	- `andmed/playlists/playlists.json`
- The repo may also contain a committed sample bundle in `andmed/untitled folder/` for moving music between machines.
- The scanner reads only the folder chosen at startup; sample files stored elsewhere must be copied there first.
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
	- `mudelid/` = immutable or small stateful domain objects
	- `repository/` = JSON persistence adapters
	- `service/` = business logic and orchestration
	- `meik/` = JavaFX views / controls

### Important source files
- `src/main/java/App.java`
	- Creates services.
	- Resolves local data paths.
	- Builds `MainView`.
- `src/main/java/service/LibraryService.java`
	- Scans music files; skips macOS `._` sidecar files.
	- Reads embedded audio tags (jaudiotagger) for title, artist, album, year.
	- Falls back to filename parsing and folder name when tags are absent.
- `src/main/java/service/MetadataService.java`
	- Writes title, artist, album, year tags directly into audio files.
	- Verifies the write succeeded by reading the file back; surfaces errors immediately.
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
- `src/main/java/meik/MainView.java`
	- Main screen.
	- Coordinates global search, listView, unitView, theme toggle, and metadata editor.
- `src/main/java/meik/ListViewPane.java`
	- Fixed context-selector tabs and playlist actions.
	- Album entries include year; artist entries include album count.
- `src/main/java/meik/UnitViewPane.java`
	- Song-table surface for playback contexts.
- `src/main/java/meik/TabBar.java`
	- Shared horizontally scrollable tab component used by both panes.
- `src/main/resources/meigid/day.css`
- `src/main/resources/meigid/night.css`

### Metadata rules right now
- Supported audio extensions: `mp3`, `wav`.
- macOS `._` AppleDouble sidecar files are skipped at scan time.
- **Primary source:** embedded audio tags read via jaudiotagger (ID3v2 for MP3, ID3 chunk for WAV).
  - Fields read: title, artist, album, year.
  - If at least one tag field is present, tags win over filename parsing.
  - Missing tag fields are filled in from filename parsing.
- **Fallback filename pattern:**

```text
ARTIST - ALBUM - YEAR - TITLE.mp3
```

- Filename fallback rules:
	- 4 parts: artist / album / year / title
	- 3 parts: artist / album / title (year empty)
	- 2 parts: artist / title (album from parent folder or `Album`)
	- otherwise: title from filename, artist = `Unknown Kunstnik`, album = `Album`

- **In-app metadata editing:** right-click any song → "edit metadata" to write title / artist / album / year directly into the audio file. The write is verified immediately; errors are shown as a dialog.

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
- The player bar includes seek, previous / next, play-pause, repeat, and a like button.

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
- Metadata is read from embedded audio tags via jaudiotagger; filename parsing is the fallback.
- JSON is the MVP persistence mechanism.
- There is no dependency injection container.
- App is local-first and assumes one user.
- Song paths are machine-local, so playlist / analytics portability is limited.
- Even though a sample bundle can be committed under `data/untitled folder/`, the runtime library source remains `data/music/`.

### Good prompt context to give ChatGPT in a future session
Use something close to this:

```text
I am working on ParimLocalMusicPlayer, a Java 21 + JavaFX 21 local-first desktop music player built with Maven. The app scans a chosen music folder for mp3 and wav files, reads embedded audio tags via jaudiotagger (falling back to filename parsing), stores analytics in andmed/analytics/plays.json and playlists in andmed/playlists/playlists.json using Jackson, and uses a dual-pane JavaFX UI (meik/ package) with a global search row, a listView for playlists/albums/artists, a unitView with a song table (laul/kunstnik/album/aasta columns), and a player bar. Playback is handled by JavaFX MediaPlayer through PlaybackService. In-app metadata editing writes tags directly to audio files via MetadataService. Analytics are only recorded after 30 seconds of listening, and playlist plays are only counted after 2 qualified songs. Shuffle has been removed. Main entry point is App via Launcher. Please preserve README.md lines 1-18 if editing that file.
```

### Short roadmap candidates already implied by the codebase
- Add artist / album analytics aggregation.
- Add monthly / yearly “memory” views.
- Add skip tracking.
- Replace or complement JSON with SQLite.
- Reduce portability issues caused by absolute-path song IDs.
- Bring back shuffle (was removed; noted in RETIRED FIRST SKETCHES).
- Bring back text display-mode toggle (CAPS / lower / Karju / Lause — noted in RETIRED FIRST SKETCHES).
