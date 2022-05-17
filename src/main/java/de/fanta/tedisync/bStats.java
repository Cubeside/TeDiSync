package de.fanta.tedisync;

import org.bstats.bungeecord.Metrics;

public record bStats(TeDiSync plugin) {
    public void registerbStats() {
        int pluginId = 15219;
        Metrics metrics = new Metrics(plugin, pluginId);
    }
}
