package service;

import org.jaudiotagger.audio.AudioFile;
import org.jaudiotagger.audio.AudioFileIO;
import org.jaudiotagger.tag.FieldKey;
import org.jaudiotagger.tag.Tag;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Writes ID3 / audio-tag metadata directly into music files.
 *
 * Supported formats: MP3 (ID3v2) and WAV (ID3 chunk).
 * After writing, the file is read back immediately to verify the tags actually
 * persisted — this surfaces silent failures (permissions, unsupported format,
 * read-only filesystem) as a clear IOException instead of losing the edit.
 */
public final class MetadataService {

    static {
        Logger.getLogger("org.jaudiotagger").setLevel(Level.SEVERE);
    }

    /**
     * Writes the supplied fields into the file's embedded tags and saves the file.
     *
     * @throws IOException if the file is not writable, the write fails, or
     *                     the post-write verification detects the change was not
     *                     persisted
     */
    public void writeMetadata(Path filePath, String title, String artist, String album, String year)
            throws IOException {
        if (!Files.isWritable(filePath)) {
            throw new IOException(filePath.getFileName()
                    + " is not writable — check file permissions");
        }

        // Write
        try {
            AudioFile audioFile = AudioFileIO.read(filePath.toFile());
            Tag tag = audioFile.getTagOrCreateAndSetDefault();
            setField(tag, FieldKey.TITLE, title);
            setField(tag, FieldKey.ARTIST, artist);
            setField(tag, FieldKey.ALBUM, album);
            setField(tag, FieldKey.YEAR, year);
            audioFile.commit();
        } catch (IOException e) {
            throw e;
        } catch (Exception e) {
            throw new IOException(
                    "Could not write metadata to " + filePath.getFileName() + ": " + e.getMessage(), e);
        }

        // Verify — read the file back and confirm the title tag is present
        try {
            AudioFile check = AudioFileIO.read(filePath.toFile());
            Tag written = check.getTag();
            if (written == null) {
                throw new IOException(
                        "Write appeared to succeed but no tag was found afterwards in "
                                + filePath.getFileName()
                                + " — the file format may not support embedded tags");
            }
            String savedTitle = written.getFirst(FieldKey.TITLE);
            if (title != null && !title.isBlank() && !title.trim().equals(savedTitle)) {
                throw new IOException(
                        "Write appeared to succeed but the value was not persisted in "
                                + filePath.getFileName()
                                + " — this file may be a macOS resource-fork sidecar (._) or on a read-only volume");
            }
        } catch (IOException e) {
            throw e;
        } catch (Exception e) {
            throw new IOException(
                    "Could not verify write to " + filePath.getFileName() + ": " + e.getMessage(), e);
        }
    }

    private static void setField(Tag tag, FieldKey key, String value) {
        if (value == null) {
            return;
        }
        try {
            tag.setField(key, value.trim());
        } catch (Exception ignored) {
            // some tag implementations do not support every FieldKey — skip silently
        }
    }
}
