package de.fanta.tedisync.discord;

import java.util.UUID;

public record DiscordUserInfo(UUID uuid, long dcID, boolean notification) {
}
