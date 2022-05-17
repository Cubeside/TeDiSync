package de.fanta.tedisync;

import net.md_5.bungee.api.plugin.Plugin;

public final class TeDiSync extends Plugin {

    @Override
    public void onEnable() {
        new bStats(this).registerbStats();
    }

    @Override
    public void onDisable() {
        // Plugin shutdown logic
    }
}
