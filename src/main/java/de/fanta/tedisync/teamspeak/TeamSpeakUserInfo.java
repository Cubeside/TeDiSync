package de.fanta.tedisync.teamspeak;

import java.util.UUID;

public record TeamSpeakUserInfo(UUID uuid, String tsID, String latestName) {
}
