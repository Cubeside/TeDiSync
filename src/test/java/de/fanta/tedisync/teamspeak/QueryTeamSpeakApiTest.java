package de.fanta.tedisync.teamspeak;

import org.junit.Test;
import java.util.*;
import java.util.concurrent.*;
import static org.junit.Assert.*;

public class QueryTeamSpeakApiTest {
    static final class Fake implements QueryTeamSpeakApi.Backend {
        final Set<Integer> groups = new HashSet<>();
        final List<String> calls = new ArrayList<>();
        CompletableFuture<Void> blocked;
        String description;
        public CompletableFuture<Integer> databaseId(String uid) { calls.add("lookup:" + uid); return CompletableFuture.completedFuture(42); }
        public CompletableFuture<Set<Integer>> groups(int id) { assertEquals(42, id); return CompletableFuture.completedFuture(Set.copyOf(groups)); }
        public CompletableFuture<Void> group(int id, int group, boolean add) {
            assertEquals(42, id); calls.add((add ? "add:" : "remove:") + group);
            if (blocked != null) return blocked;
            if (add) groups.add(group); else groups.remove(group); return CompletableFuture.completedFuture(null);
        }
        public CompletableFuture<Void> description(String uid, int id, String text) { description = text; calls.add("description"); return CompletableFuture.completedFuture(null); }
    }
    @Test public void permanentAccountsIdempotenceAndDescription() {
        Fake backend = new Fake(); QueryTeamSpeakApi api = new QueryTeamSpeakApi(backend);
        assertTrue(api.isAvailable());
        api.addServerGroup("offline-uid", 10).join(); api.addServerGroup("offline-uid", 10).join();
        assertEquals(1, Collections.frequency(backend.calls, "add:10"));
        api.setDescription("offline-uid", "VoiceChat-User: Alice").join(); assertEquals("VoiceChat-User: Alice", backend.description);
        api.setDescription("offline-uid", "x".repeat(199) + "😀").join(); assertEquals(199, backend.description.length());
        api.removeServerGroup("offline-uid", 10).join(); api.removeServerGroup("offline-uid", 10).join();
        assertEquals(1, Collections.frequency(backend.calls, "remove:10"));
    }
    @Test public void inputFailuresAndUnavailableQueryAreFutureFailures() {
        QueryTeamSpeakApi api = new QueryTeamSpeakApi((QueryTeamSpeakApi.Backend)null);
        assertFalse(api.isAvailable());
        assertThrows(CompletionException.class, () -> api.addServerGroup("uid", 1).join());
        assertThrows(CompletionException.class, () -> api.removeServerGroup("uid", 0).join());
        assertThrows(CompletionException.class, () -> api.setDescription("uid", null).join());
        assertThrows(CompletionException.class, () -> api.setDescription(" ", "text").join());
    }
    @Test public void oneUidIsSerializedAndFailuresDoNotBlockLaterCommands() {
        Fake backend = new Fake(); backend.blocked = new CompletableFuture<>();
        QueryTeamSpeakApi api = new QueryTeamSpeakApi(backend);
        CompletableFuture<Void> add = api.addServerGroup("uid", 10);
        CompletableFuture<Void> description = api.setDescription("uid", "Alice");
        assertNull(backend.description); assertFalse(description.isDone());
        backend.blocked.completeExceptionally(new IllegalStateException("offline"));
        assertThrows(CompletionException.class, add::join); description.join(); assertEquals("Alice", backend.description);
    }
}
