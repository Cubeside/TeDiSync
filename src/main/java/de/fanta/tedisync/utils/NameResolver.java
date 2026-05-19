package de.fanta.tedisync.utils;

import de.fanta.tedisync.PlayerWithId;
import de.iani.playerUUIDCacheBungee.CachedPlayer;
import de.iani.playerUUIDCacheBungee.PlayerUUIDCacheBungee;
import java.util.UUID;
import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.connection.ProxiedPlayer;

public class NameResolver {
    public static PlayerWithId getPlayer(String nameOrUUID) {
        if (nameOrUUID.length() == 36) {
            try {
                UUID id = UUID.fromString(nameOrUUID);
                ProxiedPlayer player = ProxyServer.getInstance().getPlayer(id);
                if (player != null) {
                    return new PlayerWithId(player.getUniqueId(), player.getName());
                }
            } catch (IllegalArgumentException ignored) {
            }
            ProxiedPlayer player = ProxyServer.getInstance().getPlayer(nameOrUUID);
            if (player != null) {
                return new PlayerWithId(player.getUniqueId(), player.getName());
            }
        }
        if (ProxyServer.getInstance().getPluginManager().getPlugin("PlayerUUIDCacheBungee") != null) {
            PlayerWithId resolved = PlayerUUIDCacheBungeeResolver.resolve(nameOrUUID);
            if (resolved != null) {
                return resolved;
            }
        }
        return null;
    }

    private static class PlayerUUIDCacheBungeeResolver {
        public static PlayerWithId resolve(String nameOrUUID) {
            CachedPlayer cp = PlayerUUIDCacheBungee.getAPI().getPlayerFromNameOrUUID(nameOrUUID);
            return cp == null ? null : new PlayerWithId(cp.getUniqueId(), cp.getName());
        }
    }
}
