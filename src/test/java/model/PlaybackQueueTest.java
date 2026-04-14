package model;

import mudelid.PlaybackQueue;
import mudelid.RepeatMode;
import mudelid.Song;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class PlaybackQueueTest {
    @Test
    void nextWrapsWhenRepeatAllIsEnabled() {
        Song first = new Song("1", "first", "artist", "album", Path.of("first.mp3"));
        Song second = new Song("2", "second", "artist", "album", Path.of("second.mp3"));

        PlaybackQueue queue = new PlaybackQueue();
        queue.setQueue(List.of(first, second), "All Songs", first);
        queue.setRepeatMode(RepeatMode.ALL);

        assertEquals(second, queue.next());
        assertEquals(first, queue.next());
    }

    @Test
    void nextReturnsNullAtEndWhenRepeatOff() {
        Song only = new Song("1", "only", "artist", "album", Path.of("only.mp3"));
        PlaybackQueue queue = new PlaybackQueue();
        queue.setQueue(List.of(only), "All Songs", only);

        assertNull(queue.next());
    }

    @Test
    void addAsNextPlacesSongDirectlyAfterCurrent() {
        Song first = new Song("1", "first", "artist", "album", Path.of("first.mp3"));
        Song second = new Song("2", "second", "artist", "album", Path.of("second.mp3"));
        Song third = new Song("3", "third", "artist", "album", Path.of("third.mp3"));

        PlaybackQueue queue = new PlaybackQueue();
        queue.setQueue(List.of(first, second, third), "All Songs", first);

        queue.addAsNext(third);

        assertEquals(List.of(first, third, second), queue.getActiveSongs());
        assertEquals(third, queue.next());
    }
}
