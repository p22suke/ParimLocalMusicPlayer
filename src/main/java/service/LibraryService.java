package service;

import model.Album;
import model.Artist;
import model.Song;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Scans the local music folder and extracts metadata for MP3 and WAV files.
 *
 * For the MVP this service stays pure Java and does not depend on any native or
 * third-party tagging library. Metadata is inferred from the filename first and
 * then from the folder structure as a fallback. This keeps the project easy to
 * export on both macOS and Windows while leaving room for richer tag parsing in
 * v2.
 */
public final class LibraryService {
    private static final Set<String> SUPPORTED_EXTENSIONS = Set.of("mp3", "wav");

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

        ParsedMetadata parsed = parseMetadataFromPath(filePath);
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
            return new ParsedMetadata(parts[3], parts[0], parts[1], parts[2]);
        }
        if (parts.length == 3) {
            return new ParsedMetadata(parts[2], parts[0], parts[1], Song.UNKNOWN_YEAR);
        }
        if (parts.length == 2) {
            return new ParsedMetadata(parts[1], parts[0], parentAlbum, Song.UNKNOWN_YEAR);
        }

        warnings.add("Metadata fallback used for: " + filePath.getFileName());
        return new ParsedMetadata(baseName, Song.UNKNOWN_ARTIST, parentAlbum, Song.UNKNOWN_YEAR);
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

    private record ParsedMetadata(String title, String artist, String album, String year) {
    }
}
