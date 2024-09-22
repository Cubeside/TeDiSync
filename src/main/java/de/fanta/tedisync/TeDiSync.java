package de.fanta.tedisync;

import de.fanta.tedisync.discord.DiscordBot;
import de.fanta.tedisync.teamspeak.TeamSpeakBot;
import de.fanta.tedisync.utils.ChatUtil;
import net.luckperms.api.LuckPerms;
import net.md_5.bungee.api.plugin.Plugin;
import net.md_5.bungee.config.Configuration;
import net.md_5.bungee.config.ConfigurationProvider;
import net.md_5.bungee.config.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.util.Collection;
import java.util.UUID;
import java.util.concurrent.ConcurrentSkipListSet;
import java.util.logging.Level;

public final class TeDiSync extends Plugin {
    private static TeDiSync plugin;
    public static final String PREFIX = ChatUtil.BLUE + "[" + ChatUtil.GREEN + "TeDiSync" + ChatUtil.BLUE + "]";

    private Configuration config;

    private static ConcurrentSkipListSet<UUID> updatePlayers;
    private TeamSpeakBot teamSpeakBot;
    private DiscordBot discordBot;

    @Override
    public void onEnable() {
        plugin = this;
        updatePlayers = new ConcurrentSkipListSet<>();

        try {
            Class.forName(LuckPerms.class.getName());
        } catch (NoClassDefFoundError | ClassNotFoundException ex) {
            plugin.getLogger().log(Level.SEVERE, "Without LuckPerms this plugin will not work. Please install LuckPerms and restart the server.");
            plugin.getLogger().log(Level.SEVERE, "Shutting down.....");
            plugin.getProxy().stop();
        }

        try {
            saveDefaultConfig();
            this.config = ConfigurationProvider.getProvider(YamlConfiguration.class).load(new File(getDataFolder(), "config.yml"));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        if (config.getBoolean("teamspeak.enabled")) {
            teamSpeakBot = new TeamSpeakBot(this);
        }

        if (config.getBoolean("discord.enabled")) {
            discordBot = new DiscordBot(this);
            discordBot.registerCommands();
        }

        new LuckPermsListener(this, teamSpeakBot, discordBot).createEventHandler();
    }

    @Override
    public void onDisable() {
        if (config.getBoolean("teamspeak.enabled")) {
            teamSpeakBot.stopTeamSpeakBot();
        }

        if (config.getBoolean("discord.enabled")) {
            discordBot.stopDiscordBot();
        }
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

    public Collection<UUID> getUpdatePlayers() {
        return updatePlayers;
    }

    public DiscordBot getDiscordBot() {
        return discordBot;
    }

    public TeamSpeakBot getTeamSpeakBot() {
        return teamSpeakBot;
    }
}
