package de.fanta.tedisync.teamspeak;

import com.github.theholywaffle.teamspeak3.TS3ApiAsync;
import com.github.theholywaffle.teamspeak3.api.ClientProperty;
import com.github.theholywaffle.teamspeak3.api.CommandFuture;
import de.fanta.tedisync.api.TeamSpeakApi;
import java.util.*;
import java.util.concurrent.*;
import java.util.function.*;

/** Keeps the shaded query library behind the public Java API. Calls for one UID are ordered. */
public final class QueryTeamSpeakApi implements TeamSpeakApi {
    interface Backend {
        CompletableFuture<Integer> databaseId(String uid);
        CompletableFuture<Set<Integer>> groups(int id);
        CompletableFuture<Void> group(int id, int group, boolean add);
        CompletableFuture<Void> description(String uid, int id, String text);
    }
    private final Supplier<Backend> backend;
    private final Map<String, CompletableFuture<Void>> tails = new HashMap<>();

    public QueryTeamSpeakApi(Supplier<TS3ApiAsync> query) {
        this.backend = () -> { TS3ApiAsync api = query.get(); return api == null ? null : new Backend() {
            public CompletableFuture<Integer> databaseId(String uid) { return future(api.getDatabaseClientByUId(uid)).thenApply(c -> c.getDatabaseId()); }
            public CompletableFuture<Set<Integer>> groups(int id) {
                return future(api.getServerGroupsByClientId(id)).thenApply(groups -> {
                    Set<Integer> ids = new HashSet<>(); groups.forEach(g -> ids.add(g.getId())); return ids;
                });
            }
            public CompletableFuture<Void> group(int id, int group, boolean add) {
                return future(add ? api.addClientToServerGroup(group, id) : api.removeClientFromServerGroup(group, id));
            }
            public CompletableFuture<Void> description(String uid, int id, String text) {
                return future(api.isClientOnline(uid)).thenCompose(online -> online
                        ? future(api.getClientByUId(uid)).thenCompose(c -> future(api.editClient(c.getId(), ClientProperty.CLIENT_DESCRIPTION, text)))
                        : future(api.editDatabaseClient(id, Map.of(ClientProperty.CLIENT_DESCRIPTION, text))));
            }
        }; };
    }
    QueryTeamSpeakApi(Backend backend) { this.backend = () -> backend; }
    private static <T> CompletableFuture<T> future(CommandFuture<T> command) {
        CompletableFuture<T> result = new CompletableFuture<>();
        command.onSuccess(result::complete).onFailure(result::completeExceptionally); return result;
    }
    @Override public boolean isAvailable() { return backend.get() != null; }
    @Override public CompletableFuture<Void> addServerGroup(String uid, int group) { return group(uid, group, true); }
    @Override public CompletableFuture<Void> removeServerGroup(String uid, int group) { return group(uid, group, false); }
    private CompletableFuture<Void> group(String uid, int group, boolean add) {
        if (group <= 0) return CompletableFuture.failedFuture(new IllegalArgumentException("Server group ID must be positive"));
        return submit(uid, api -> api.databaseId(uid).thenCompose(id -> api.groups(id).thenCompose(groups ->
                groups.contains(group) == add ? CompletableFuture.completedFuture(null) : api.group(id, group, add))));
    }
    @Override public CompletableFuture<Void> setDescription(String uid, String description) {
        if (description == null) return CompletableFuture.failedFuture(new IllegalArgumentException("Description is required"));
        int end = Math.min(200, description.length());
        if (end < description.length() && end > 0 && Character.isHighSurrogate(description.charAt(end - 1))) end--;
        String text = description.substring(0, end);
        return submit(uid, api -> api.databaseId(uid).thenCompose(id -> api.description(uid, id, text)));
    }
    private synchronized CompletableFuture<Void> submit(String uid, Function<Backend, CompletableFuture<Void>> action) {
        if (uid == null || uid.isBlank() || uid.length() > 128) return CompletableFuture.failedFuture(new IllegalArgumentException("Invalid TeamSpeak UID"));
        CompletableFuture<Void> previous = tails.getOrDefault(uid, CompletableFuture.completedFuture(null));
        // Install the tail before starting, including when the backend completes synchronously.
        CompletableFuture<Void> result = new CompletableFuture<>(); tails.put(uid, result);
        previous.handle((v, error) -> null).thenCompose(v -> {
            Backend api = backend.get();
            return api == null ? CompletableFuture.failedFuture(new IllegalStateException("TeDiSync TeamSpeak query unavailable")) : action.apply(api);
        }).whenComplete((v, error) -> {
            if (error == null) result.complete(null); else result.completeExceptionally(error);
            synchronized (QueryTeamSpeakApi.this) { tails.remove(uid, result); }
        });
        return result;
    }
}
