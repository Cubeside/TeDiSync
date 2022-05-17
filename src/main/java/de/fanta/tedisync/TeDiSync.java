package de.fanta.tedisync;

import de.fanta.tedisync.utils.ChatUtil;
import net.luckperms.api.LuckPerms;
import net.md_5.bungee.api.plugin.Plugin;

import java.util.logging.Level;

public final class TeDiSync extends Plugin {

    public static final String PREFIX = ChatUtil.BLUE + "[" + ChatUtil.GREEN + "TeDiSync" + ChatUtil.BLUE + "]";
    private static TeDiSync plugin;

    @Override
    public void onEnable() {
        plugin = this;

        try {
            Class.forName(LuckPerms.class.getName());
        } catch (NoClassDefFoundError | ClassNotFoundException ex) {
            plugin.getLogger().log(Level.SEVERE, "Without Luckperms this plugin will not work. Please install Luckperms and restart the server.");
            plugin.getLogger().log(Level.SEVERE, "Shutdown.....");
            plugin.getProxy().stop();
        }

        new bStats(this).registerbStats();
        new CommandRegistration(this).registerCommands();
        new EventRegistration(this).registerEvents();

        new TeamSpeakTest(this).initTeamSpeakBot();
    }

    @Override
    public void onDisable() {
        // Plugin shutdown logic
    }

    public static TeDiSync getPlugin() {
        return plugin;
    }
}
