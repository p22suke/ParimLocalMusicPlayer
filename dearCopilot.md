You are building a minimal desktop music player in Java using JavaFX, designed to be a **narrow vertical sidebar app (~20% screen width)** with a **stacked floor layout**.

GOAL:
Create an MVP local music player that works with **MP3, WAV, and FLAC files**, displays metadata, supports playlists and a queue, logs plays for analytics (hidden), includes keyboard navigation, search, and terminal-like UI with optional day/night themes that can auto-switch with local sunset/sunrise.

---

FEATURES:

1️⃣ Window & Layout:
- Stacked “floors” layout in the narrow vertical window:
  - **Top floor (20%)**: Navigation links for All Songs, Albums, Artists, Playlists, Queue, hidden Memory (no buttons, links only)
  - **Middle floor (70%)**: Scrollable main view (table-like / Excel-style)
  - **Bottom floor (10%)**: Player bar + context info
- Pixel borders separate floors
- Terminal-like, minimal, clean
- Window always visible, narrow, scrollable main content

---

2️⃣ Metadata / List Design:
- Formatting rules:
  - song title → lowercase
  - artist → uppercase
  - album → CamelCase
  - year → numerical
- Lists in main view:
  - single-line: `song title -- ARTIST NAME -- Album Name -- 2007`
  - wrapping enabled
- Search bar above main view (filters current list)
- Table-like appearance with columns: song, artist, album, year
- Configurable display styles via settings

---

3️⃣ Player Bar:
- Always visible at bottom (10% height)
- Displays:
  - song title
  - album name -- year
  - progress bar
  - context line: “Playing from: Playlist / Album / All Songs”
- Controls: play/pause, next, previous, shuffle, repeat (ONE / ALL)
- **Do not display artist**
- Pixel borders separate from main view

---

4️⃣ Playlists & Queue:
- User can create, rename, add/remove songs
- Selecting any list (All Songs, Album, Artist, Playlist) → becomes current queue
- Shuffle/repeat only apply to queue

---

5️⃣ Analytics / Time-Based Memory (Hidden):
- Logs plays ≥30 seconds into JSON file
- Stores: songId, timestamp
- Supports future monthly/yearly memory playlists
- Architecture ready for migration to database later

---

6️⃣ Playback & Library:
- **LibraryService**: loads MP3/WAV/FLAC files + metadata (title, artist, album, year)
- **PlaybackService**: handles queue, MediaPlayer, shuffle/repeat
- **AnalyticsService**: logs plays to JSON
- Playback errors handled gracefully

---

7️⃣ Styling / Themes:
- Configurable text styles (artist/song/album)
- Day/night themes:
  - user-selectable
  - optional auto-switch with local sunset/sunrise
- Terminal-like, minimal UI

---

8️⃣ Keyboard Navigation:
- Up/Down → move selection in main view
- Enter → play selected song
- Optional hotkeys: next, previous, shuffle, repeat

---

9️⃣ Error Handling:
- Missing metadata → fallback: “Unknown Artist / Album / Year”
- Unsupported files ignored with warning
- Playback errors reported

---

10️⃣ Output Requirements:
- JavaFX application skeleton with packages: model, service, repository, ui
- Implement stacked floor layout
- Scrollable, table-like main view with wrapping
- Playlist creation + queue logic
- Player bar with context info and controls
- Search bar filtering
- Keyboard navigation
- Audio playback (MP3/WAV/FLAC)
- Hidden JSON analytics logging
- Day/night theme switching

Generate fully functional, clean, readable code ready to run as MVP.


ParimLocalMusicPlayer/
│
├─ src/
│   ├─ main/
│   │   ├─ java/
│   │   │   ├─ model/
│   │   │   │   ├─ Song.java
│   │   │   │   ├─ Album.java
│   │   │   │   ├─ Artist.java
│   │   │   │   ├─ Playlist.java
│   │   │   │   ├─ PlaybackQueue.java
│   │   │   │   └─ RepeatMode.java
│   │   │   │
│   │   │   ├─ service/
│   │   │   │   ├─ LibraryService.java
│   │   │   │   ├─ PlaybackService.java
│   │   │   │   └─ AnalyticsService.java
│   │   │   │
│   │   │   ├─ repository/
│   │   │   │   ├─ AnalyticsRepository.java
│   │   │   │   └─ JsonAnalyticsRepository.java
│   │   │   │
│   │   │   ├─ ui/
│   │   │   │   ├─ MainView.java
│   │   │   │   ├─ NavigationBar.java
│   │   │   │   ├─ PlayerBar.java
│   │   │   │   └─ TableCellRenderer.java (for formatting rows)
│   │   │   │
│   │   │   └─ App.java (main entry point)
│   │   │
│   │   └─ resources/
│   │       ├─ styles/
│   │       │   ├─ day.css
│   │       │   └─ night.css
│   │       └─ icons/ (optional, maybe for play/pause)
│   │
│   └─ test/ (unit tests for services and models)
│
└─ data/
    ├─ music/ (user MP3/WAV/FLAC files)
    └─ analytics/plays.json

    COMMENT CLEARLY! I WANT TO UNDERST±ND
    