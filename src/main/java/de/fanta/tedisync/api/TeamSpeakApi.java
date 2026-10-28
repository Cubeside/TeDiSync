package de.fanta.tedisync.api;

import java.util.concurrent.CompletableFuture;

/** Account operations use the permanent TeamSpeak UID, including for offline accounts. */
public interface TeamSpeakApi {
    boolean isAvailable();
    /** Ensures membership; an existing membership is successful. */
    CompletableFuture<Void> addServerGroup(String tsUid, int groupId);
    /** Ensures absence; an absent membership is successful. */
    CompletableFuture<Void> removeServerGroup(String tsUid, int groupId);
    /** Replaces the description, truncated to 200 UTF-16 characters without splitting a surrogate pair. */
    CompletableFuture<Void> setDescription(String tsUid, String description);
}
