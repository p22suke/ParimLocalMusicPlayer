package mudelid;

import java.nio.file.Path;
import java.util.Locale;
import java.util.Objects;

/**
 * Immutable song model used throughout the MVP.
 *
 * The formatting helpers intentionally live here so the UI can reuse the same
 * rules everywhere: titles are lower-case, artists are upper-case and albums
 * use a simple CamelCase conversion.
 */
public final class Song {
    public static final String UNKNOWN_ARTIST = "Unknown Kunstnik";
    public static final String UNKNOWN_ALBUM = "Album";

    private final String id;
    private final String title;
    private final String artist;
    private final String album;
    private final String year;
    private final Path filePath;

    public Song(String id, String title, String artist, String album, String year, Path filePath) {
        this.id = Objects.requireNonNull(id, "id cannot be null");
        this.title = sanitise(title, deriveFallbackTitle(filePath));
        this.artist = sanitise(artist, UNKNOWN_ARTIST);
        this.album = sanitise(album, UNKNOWN_ALBUM);
        this.year = sanitise(year, "");
        this.filePath = Objects.requireNonNull(filePath, "filePath cannot be null");
    }

    // LIINA!!!!! backwards-compat constructor used by tests and old call sites
    public Song(String id, String title, String artist, String album, Path filePath) {
        this(id, title, artist, album, "", filePath);
    }

    public String getId() {
        return id;
    }

    public String getTitle() {
        return title;
    }

    public String getArtist() {
        return artist;
    }

    public String getAlbum() {
        return album;
    }

    public String getYear() {
        return year;
    }

    public Path getFilePath() {
        return filePath;
    }

    public String getDisplayTitle() {
        return title.toLowerCase(Locale.ROOT);
    }

    public String getDisplayArtist() {
        return artist.toUpperCase(Locale.ROOT);
    }

    public String getDisplayAlbum() {
        return toCamelCase(album);
    }

    public String getDisplayLine() {
        return "%s -- %s -- %s".formatted(
                getDisplayTitle(),
                getDisplayArtist(),
                getDisplayAlbum());
    }

    public String toSearchableText() {
        String y = year != null && !year.isBlank() ? " " + year : "";
        return (title + " " + artist + " " + album + y).toLowerCase(Locale.ROOT);
    }

    private static String sanitise(String value, String fallback) {
        if (value == null) {
            return fallback;
        }
        String cleaned = value.trim();
        return cleaned.isEmpty() ? fallback : cleaned;
    }

    private static String deriveFallbackTitle(Path filePath) {
        if (filePath == null || filePath.getFileName() == null) {
            return "unknown track";
        }
        String fileName = filePath.getFileName().toString();
        int dotIndex = fileName.lastIndexOf('.');
        return dotIndex > 0 ? fileName.substring(0, dotIndex) : fileName;
    }

    private static String toCamelCase(String value) {
        String[] parts = value.trim().split("\\s+");
        StringBuilder builder = new StringBuilder();
        for (String part : parts) {
            if (part.isBlank()) {
                continue;
            }
            if (!builder.isEmpty()) {
                builder.append(' ');
            }
            builder.append(part.substring(0, 1).toUpperCase(Locale.ROOT));
            if (part.length() > 1) {
                builder.append(part.substring(1).toLowerCase(Locale.ROOT));
            }
        }
        return builder.isEmpty() ? UNKNOWN_ALBUM : builder.toString();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof Song song)) {
            return false;
        }
        return id.equals(song.id);
    }

    @Override
    public int hashCode() {
        return id.hashCode();
    }

    @Override
    public String toString() {
        return getDisplayLine();
    }
}
