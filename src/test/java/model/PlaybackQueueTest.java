package model;

import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PlaybackQueueTest {
    @Test
    void nextWrapsWhenRepeatAllIsEnabled() {
        Song first = new Song("1", "first", "artist", "album", "2024", Path.of("first.mp3"));
        Song second = new Song("2", "second", "artist", "album", "2024", Path.of("second.mp3"));

        PlaybackQueue queue = new PlaybackQueue();
        queue.setQueue(List.of(first, second), "All Songs", first);
        queue.setRepeatMode(RepeatMode.ALL);

        assertEquals(second, queue.next());
        assertEquals(first, queue.next());
    }

    @Test
    void shuffleKeepsCurrentSongPlayable() {
        Song first = new Song("1", "first", "artist", "album", "2024", Path.of("first.mp3"));
        Song second = new Song("2", "second", "artist", "album", "2024", Path.of("second.mp3"));
        Song third = new Song("3", "third", "artist", "album", "2024", Path.of("third.mp3"));

        PlaybackQueue queue = new PlaybackQueue();
        queue.setQueue(List.of(first, second, third), "All Songs", second);

        Song current = queue.toggleShuffle();

        assertNotNull(current);
        assertEquals(second, current);
        assertTrue(queue.getActiveSongs().containsAll(List.of(first, second, third)));
    }

    @Test
    void nextReturnsNullAtEndWhenRepeatOff() {
        Song only = new Song("1", "only", "artist", "album", "2024", Path.of("only.mp3"));
        PlaybackQueue queue = new PlaybackQueue();
        queue.setQueue(List.of(only), "All Songs", only);

        assertNull(queue.next());
    }

    @Test
    void addAsNextPlacesSongDirectlyAfterCurrent() {
        Song first = new Song("1", "first", "artist", "album", "2024", Path.of("first.mp3"));
        Song second = new Song("2", "second", "artist", "album", "2024", Path.of("second.mp3"));
        Song third = new Song("3", "third", "artist", "album", "2024", Path.of("third.mp3"));

        PlaybackQueue queue = new PlaybackQueue();
        queue.setQueue(List.of(first, second, third), "All Songs", first);

        queue.addAsNext(third);

        assertEquals(List.of(first, third, second), queue.getActiveSongs());
        assertEquals(third, queue.next());
    }
}
