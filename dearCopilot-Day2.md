I am working on ParimLocalMusicPlayer, a Java 21 + JavaFX 21 local-first desktop music player. The app currently has a MainView with a simple vertical layout (navigation, content, player bar). I want to refactor the UI into a dual-pane system.

IMPORTANT:
Preserve README.md lines 1–9. Do not change backend services unless explicitly required for UI interaction.

---

## GOAL

Refactor the UI into two coordinated panes:

1. listView (top)
2. unitView (below it)

Both should look visually similar (same design language), but have different roles:

- listView = “things you click to open”
- unitView = “things you play”

---

## LAYOUT (top to bottom)

1. Global Search Bar
2. listViewTabs
3. listViewContent
4. unitViewTabs
5. unitViewContent
6. existing PlayerBar (unchanged for now)

The window remains narrow and vertical (~420px width).

---

## listView (context selector)

### Tabs (fixed):
- Playlists
- Albums
- Artists

### Behavior:
- Shows lists of items (e.g. playlist names, album names, artist names)
- Clicking ANY item:
  → opens a NEW TAB in unitViewTabs
  → displays the corresponding songs in unitViewContent

Examples:
- Clicking a playlist → shows its songs
- Clicking an album → shows songs from that album
- Clicking an artist → shows songs by that artist

listView NEVER plays music directly.

---

## unitView (playback surface)

### Tabs:
- All Songs (fixed, always first, cannot be closed)
- + dynamically opened tabs (from listView clicks)

### Tab behavior:
- Tabs behave like a browser:
  - unlimited tabs
  - horizontally scrollable if needed
- Each tab represents a “song list context”

### Content:
- Always displays a list of songs
- This is where playback actions happen

---

## GLOBAL SEARCH

- One search bar at the top
- Filters BOTH:
  - listViewContent
  - unitViewContent
- Should be reactive / live filtering

---

## DESIGN REQUIREMENTS

- Minimal, clean, terminal-like aesthetic
- Very compact (optimized for narrow width)
- listView and unitView should look visually consistent
  (same row styles, spacing, typography)

---

## ARCHITECTURE CHANGES

Refactor MainView into smaller components:

- ListViewPane
  - contains:
    - ListViewTabs
    - ListViewContent

- UnitViewPane
  - contains:
    - UnitViewTabs
    - UnitViewContent

- Reusable TabBar component for both panes

---

## DATA FLOW (IMPORTANT)

- listView interaction drives unitView
- unitView does NOT affect listView

Introduce a simple controller/state mechanism:

- selecting a list item creates a “SongListContext”
- unitViewTabs manages multiple SongListContexts

Each context should minimally contain:
- name (tab title)
- list of song IDs

---

## DO NOT

- Do not redesign PlaybackService yet
- Do not finalize queue behavior yet
- Do not introduce external frameworks
- Do not break existing playback

---

## OPTIONAL (if easy)

- Make unitViewTabs horizontally scrollable
- Highlight active tab clearly
- Keep All Songs always visible

---

Focus on clean structure and maintainability, not visual perfection.