package de.fanta.tedisync;

import de.fanta.tedisync.data.Database;
import de.fanta.tedisync.discord.DiscordBot;
import de.fanta.tedisync.listener.MainJoinListener;
import de.fanta.tedisync.teamspeak.TeamSpeakTest;
import de.fanta.tedisync.utils.ChatUtil;
import de.iani.cubesideutils.bungee.sql.SQLConfigBungee;
import net.luckperms.api.LuckPerms;
import net.md_5.bungee.api.plugin.Plugin;
import net.md_5.bungee.api.plugin.PluginManager;
import net.md_5.bungee.config.Configuration;
import net.md_5.bungee.config.ConfigurationProvider;
import net.md_5.bungee.config.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.UUID;
import java.util.logging.Level;

public final class TeDiSync extends Plugin {
    private static TeDiSync plugin;
    public static final String PREFIX = ChatUtil.BLUE + "[" + ChatUtil.GREEN + "TeDiSync" + ChatUtil.BLUE + "]";

    private Configuration config;
    private Database database;

    private HashMap<UUID, TeDiPlayer> teDiPlayerMap;

    @Override
    public void onEnable() {
        plugin = this;
        teDiPlayerMap  = new HashMap<>();

        try {
            Class.forName(LuckPerms.class.getName());
        } catch (NoClassDefFoundError | ClassNotFoundException ex) {
            plugin.getLogger().log(Level.SEVERE, "Without Luckperms this plugin will not work. Please install Luckperms and restart the server.");
            plugin.getLogger().log(Level.SEVERE, "Shutdown.....");
            plugin.getProxy().stop();
        }

        try {
            saveDefaultConfig();
            this.config = ConfigurationProvider.getProvider(YamlConfiguration.class).load(new File(getDataFolder(), "config.yml"));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        database = new Database(new SQLConfigBungee(config.getSection("database")), this);

        new bStats(this).registerbStats();

        PluginManager pluginManager = this.getProxy().getPluginManager();
        pluginManager.registerListener(this, new MainJoinListener(this));

        if (config.getBoolean("teamspeak.enabled")) {
            new TeamSpeakTest(this).initTeamSpeakBot();
        }

        if (config.getBoolean("discord.enabled")) {
            new DiscordBot(this);
        }

    }

    @Override
    public void onDisable() {
        // Plugin shutdown logic
    }

    public static TeDiSync getPlugin() {
        return plugin;
    }

    private void saveDefaultConfig() throws IOException {
        File config = new File(getDataFolder(), "config.yml");
        if (config.exists()) {
            return;
        }
        InputStream defaultConfig = getClass().getClassLoader().getResourceAsStream("config.yml");
        getDataFolder().mkdirs();
        Files.copy(defaultConfig, config.toPath());
    }

    public boolean saveConfig() throws IOException {
        ConfigurationProvider.getProvider(YamlConfiguration.class).save(getConfig(), new File(getDataFolder(), "config.yml"));
        return true;
    }

    public Configuration getConfig() {
        return config;
    }

    public Database getDatabase() {
        return database;
    }

    public HashMap<UUID, TeDiPlayer> getTeDiPlayerMap() {
        return teDiPlayerMap;
    }
}
