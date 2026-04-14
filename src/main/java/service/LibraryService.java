package service;

import mudelid.Album;
import mudelid.Artist;
import mudelid.Song;

import org.jaudiotagger.audio.AudioFile;
import org.jaudiotagger.audio.AudioFileIO;
import org.jaudiotagger.tag.FieldKey;
import org.jaudiotagger.tag.Tag;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Scans the local music folder and extracts metadata for MP3 and WAV files.
 *
 * Metadata is read from embedded audio tags (ID3 for MP3, ID3 chunks for WAV)
 * first. Filename-based parsing is used as a fallback for any fields that are
 * missing or empty in the actual tags.
 */
public final class LibraryService {
    private static final Set<String> SUPPORTED_EXTENSIONS = Set.of("mp3", "wav");

    static {
        // jaudiotagger is very chatty; silence it
        Logger.getLogger("org.jaudiotagger").setLevel(Level.SEVERE);
    }

    private final Path musicDirectory;
    private final List<String> warnings = new ArrayList<>();

    public LibraryService(Path musicDirectory) {
        this.musicDirectory = Objects.requireNonNull(musicDirectory, "musicDirectory cannot be null");
    }

    public List<Song> loadLibrary() {
        warnings.clear();
        try {
            Files.createDirectories(musicDirectory);
        } catch (IOException exception) {
            throw new IllegalStateException("Could not create music directory: " + musicDirectory, exception);
        }

        try (Stream<Path> pathStream = Files.walk(musicDirectory)) {
            return pathStream
                    .filter(Files::isRegularFile)
                    .filter(p -> p.getFileName() != null && !p.getFileName().toString().startsWith("._"))
                    .map(this::loadSong)
                    .filter(Objects::nonNull)
                    .sorted(Comparator.comparing(Song::getDisplayArtist)
                            .thenComparing(Song::getDisplayAlbum)
                            .thenComparing(Song::getDisplayTitle))
                    .toList();
        } catch (IOException exception) {
            throw new IllegalStateException("Could not scan music directory: " + musicDirectory, exception);
        }
    }

    public List<Album> getAlbums(List<Song> songs) {
        return songs.stream()
                .collect(Collectors.groupingBy(song -> song.getDisplayAlbum() + "@@" + song.getDisplayArtist()))
                .entrySet().stream()
                .map(entry -> {
                    List<Song> albumSongs = entry.getValue();
                    Song first = albumSongs.getFirst();
                    return new Album(first.getDisplayAlbum(), first.getDisplayArtist(), albumSongs);
                })
                .sorted(Comparator.comparing(Album::name).thenComparing(Album::artistName))
                .toList();
    }

    public List<Artist> getArtists(List<Song> songs) {
        return songs.stream()
                .collect(Collectors.groupingBy(Song::getDisplayArtist))
                .entrySet().stream()
                .map(entry -> new Artist(entry.getKey(), entry.getValue()))
                .sorted(Comparator.comparing(Artist::name))
                .toList();
    }

    public List<String> getWarnings() {
        return List.copyOf(warnings);
    }

    public Path getMusicDirectory() {
        return musicDirectory;
    }

    private Song loadSong(Path filePath) {
        String extension = extensionOf(filePath);
        if (!SUPPORTED_EXTENSIONS.contains(extension)) {
            warnings.add("Ignored unsupported file: " + filePath.getFileName());
            return null;
        }

        ParsedMetadata tagMeta = readTagsFromFile(filePath);
        ParsedMetadata parsed = tagMeta != null ? tagMeta : parseMetadataFromPath(filePath);
        return new Song(
                filePath.toAbsolutePath().normalize().toString(),
                parsed.title(),
                parsed.artist(),
                parsed.album(),
                parsed.year(),
                filePath.toAbsolutePath().normalize());
    }

    private String extensionOf(Path filePath) {
        String filename = filePath.getFileName() == null ? "" : filePath.getFileName().toString();
        int dotIndex = filename.lastIndexOf('.');
        return dotIndex >= 0 ? filename.substring(dotIndex + 1).toLowerCase(Locale.ROOT) : "";
    }

    private ParsedMetadata parseMetadataFromPath(Path filePath) {
        String baseName = stripExtension(filePath.getFileName() == null ? "" : filePath.getFileName().toString());
        String parentAlbum = fallbackAlbumFromFolder(filePath);
        String[] parts = baseName.split("\\s+-\\s+");

        if (parts.length >= 4) {
            // expected format: ARTIST - ALBUM - YEAR - TITLE
            return new ParsedMetadata(parts[3], parts[0], parts[1], parts[2]);
        }
        if (parts.length == 3) {
            return new ParsedMetadata(parts[2], parts[0], parts[1], "");
        }
        if (parts.length == 2) {
            return new ParsedMetadata(parts[1], parts[0], parentAlbum, "");
        }

        warnings.add("Metadata fallback used for: " + filePath.getFileName());
        return new ParsedMetadata(baseName, Song.UNKNOWN_ARTIST, parentAlbum, "");
    }

    private String fallbackAlbumFromFolder(Path filePath) {
        Path parent = filePath.getParent();
        if (parent == null || parent.getFileName() == null) {
            return Song.UNKNOWN_ALBUM;
        }

        Path normalizedMusicDirectory = musicDirectory.toAbsolutePath().normalize();
        Path normalizedParent = parent.toAbsolutePath().normalize();
        if (normalizedParent.equals(normalizedMusicDirectory)) {
            return Song.UNKNOWN_ALBUM;
        }

        String parentName = parent.getFileName().toString().trim();
        return parentName.isEmpty() ? Song.UNKNOWN_ALBUM : parentName;
    }

    private String stripExtension(String fileName) {
        int dotIndex = fileName.lastIndexOf('.');
        return dotIndex > 0 ? fileName.substring(0, dotIndex) : fileName;
    }

    private ParsedMetadata readTagsFromFile(Path filePath) {
        try {
            AudioFile audioFile = AudioFileIO.read(filePath.toFile());
            Tag tag = audioFile.getTag();
            if (tag == null) {
                return null;
            }
            String title = tag.getFirst(FieldKey.TITLE);
            String artist = tag.getFirst(FieldKey.ARTIST);
            String album = tag.getFirst(FieldKey.ALBUM);
            String year = tag.getFirst(FieldKey.YEAR);
            // only use tag data if at least one real field is present
            if (isBlank(title) && isBlank(artist) && isBlank(album)) {
                return null;
            }
            ParsedMetadata fromFilename = parseMetadataFromPath(filePath);
            return new ParsedMetadata(
                    isBlank(title) ? fromFilename.title() : title.trim(),
                    isBlank(artist) ? fromFilename.artist() : artist.trim(),
                    isBlank(album) ? fromFilename.album() : album.trim(),
                    isBlank(year) ? fromFilename.year() : year.trim());
        } catch (Exception e) {
            return null; // fall back to filename parsing
        }
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private record ParsedMetadata(String title, String artist, String album, String year) {
    }
}
