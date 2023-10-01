package de.fanta.tedisync.discord;

import de.fanta.tedisync.TeDiSync;
import de.fanta.tedisync.discord.commands.DiscordCommandRegistration;
import de.fanta.tedisync.utils.ChatUtil;
import de.iani.cubesideutils.ComponentUtil;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.entities.channel.concrete.PrivateChannel;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.requests.GatewayIntent;
import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.HoverEvent;
import net.md_5.bungee.api.chat.hover.content.Text;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.config.Configuration;

import java.awt.*;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;
import java.util.logging.Level;

public class DiscordBot extends ListenerAdapter {
    private static JDA discordAPI;
    private final TeDiSync plugin;
    private static HashMap<UUID, User> requests;
    private static HashMap<String, Giveaway> giveaways;
    private static HashMap<UUID, String> userEditGiveaway;
    private static HashMap<Long, UUID> discordIdToUUID;

    public DiscordBot(TeDiSync plugin) {
        this.plugin = plugin;
        new DiscordCommandRegistration(plugin).registerCommands();

        plugin.getLogger().info("Login DiscordBot...");
        discordAPI = JDABuilder.createDefault(plugin.getConfig().getString("discord.login_token")).enableIntents(GatewayIntent.MESSAGE_CONTENT).build();
        plugin.getLogger().info("DiscordBot Logged in.");
        discordAPI.addEventListener(this);
        requests = new HashMap<>();
        giveaways = new HashMap<>();
        userEditGiveaway = new HashMap<>();
        discordIdToUUID = new HashMap<>();

        Configuration config = TeDiSync.getPlugin().getConfig().getSection("discorduser");
        for (String id : config.getKeys()) {
            discordIdToUUID.put(Long.valueOf(id), UUID.fromString(config.getString(id)));
        }
        loadGiveawaysFromConfig();
    }

    public static HashMap<String, Giveaway> getGiveaways() {
        return giveaways;
    }

    public static HashMap<UUID, String> getUserEditGiveaway() {
        return userEditGiveaway;
    }

    public static HashMap<UUID, User> getRequests() {
        return requests;
    }

    public static JDA getDiscordAPI() {
        return discordAPI;
    }

    public static boolean saveUser(UUID uuid, Long id) {
        Configuration config = TeDiSync.getPlugin().getConfig();
        config.set("discorduser." + id, uuid.toString());

        try {
            if (TeDiSync.getPlugin().saveConfig()) {
                DiscordBot.getDiscordIdToUUID().put(id, uuid);
                return true;
            }
        } catch (IOException e) {
            TeDiSync.getPlugin().getLogger().log(Level.SEVERE, "User could not be saved");
            return false;
        }
        return false;
    }


    @Override
    public void onMessageReceived(MessageReceivedEvent event) {
        if (event.getAuthor().isBot()) {
            return;
        }
        User author = event.getAuthor();
        Message message = event.getMessage();
        if (event.getChannel() instanceof PrivateChannel privateChannel) {
            plugin.getLogger().info("Private from: " + author.getEffectiveName() + "(" + author.getName() + ")" + ": " + message.getContentRaw());
            String playerName = message.getContentRaw();
            ProxiedPlayer proxiedPlayer = plugin.getProxy().getPlayer(playerName);
            if (proxiedPlayer == null || !proxiedPlayer.isConnected()) {
                EmbedBuilder embedBuilder = new EmbedBuilder().setTitle("Fehler");
                embedBuilder.setColor(ChatUtil.RED.getColor());
                embedBuilder.setDescription("Der Spieler " + playerName + " ist nicht online.");
                privateChannel.sendMessageEmbeds(embedBuilder.build()).submit();
            } else {
                requests.put(proxiedPlayer.getUniqueId(), author);
                ChatUtil.sendNormalMessage(proxiedPlayer, "Möchtest du das Discord-Konto " + ChatUtil.BLUE + author.getEffectiveName() + "(" + author.getName() + ")" + ChatUtil.GREEN + " mit Minecraft verbinden?");

                ClickEvent acceptClickEvent = new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/discord register accept");
                HoverEvent acceptHoverEvent = new HoverEvent(HoverEvent.Action.SHOW_TEXT, new Text("Annehmen"));

                ClickEvent denyClickEvent = new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/discord register deny");
                HoverEvent denyHoverEvent = new HoverEvent(HoverEvent.Action.SHOW_TEXT, new Text("Ablehnen"));

                BaseComponent component = ComponentUtil.setColor("", ChatColor.BLUE);

                BaseComponent acceptComponent = ComponentUtil.setColor("Annehmen", ChatUtil.GREEN);
                acceptComponent.setHoverEvent(acceptHoverEvent);
                acceptComponent.setClickEvent(acceptClickEvent);
                component.addExtra(acceptComponent);

                component.addExtra(ComponentUtil.setColor(" | ", ChatColor.DARK_GRAY));

                BaseComponent denyComponent = ComponentUtil.setColor("Ablehnen", ChatUtil.RED);
                denyComponent.setHoverEvent(denyHoverEvent);
                denyComponent.setClickEvent(denyClickEvent);
                component.addExtra(denyComponent);
                ChatUtil.sendComponent(proxiedPlayer, component);

                EmbedBuilder embedBuilder = new EmbedBuilder().setTitle("Anfrage gesendet");
                embedBuilder.setColor(ChatUtil.GREEN.getColor());
                embedBuilder.setDescription("Eine Anfrage zum Verbinden wurde in Minecraft an " + playerName + " geschickt.");
                privateChannel.sendMessageEmbeds(embedBuilder.build()).queue();
            }
        }
    }

    @Override
    public void onButtonInteraction(ButtonInteractionEvent event) {
        for (Giveaway giveaway : giveaways.values()) {
            if (giveaway.getName().equals(event.getComponentId())) {
                if (!giveaway.isOpen()) {
                    privateReplay(event, "Das Gewinnspiel ist momentan nicht geöffnet.", ChatUtil.ORANGE.getColor());
                    return;
                }

                if (giveaway.isEnterMultiple()) {
                    if (giveaway.getLastEntry().containsKey(event.getUser().getIdLong()) && isCurrentDay(giveaway.getLastEntry().get(event.getUser().getIdLong()))) {
                        privateReplay(event, "Du hast dich heute schon für dieses Gewinnspiel eingetragen.", ChatUtil.RED.getColor());
                        return;
                    }
                } else {
                    if (giveaway.getEntryList().containsKey(event.getUser().getIdLong())) {
                        privateReplay(event, "Du kannst dich für dieses Gewinnspiel nur ein Mal eintragen.", ChatUtil.RED.getColor());
                        return;
                    }
                }

                if (giveaway.countUp(event.getUser().getIdLong())) {
                    privateReplay(event, "Du hast dich für das Gewinnspiel eingetragen.", ChatUtil.GREEN.getColor());
                } else {
                    privateReplay(event, "Es ist ein Fehler aufgetreten.", ChatUtil.RED.getColor());
                }
            }
        }
    }

    private void privateReplay(ButtonInteractionEvent event, String message, Color color) {
        EmbedBuilder embedBuilder = new EmbedBuilder();
        embedBuilder.setColor(color);
        embedBuilder.setDescription(message);
        event.reply("").setEmbeds(embedBuilder.build()).setEphemeral(true).queue();
    }

    private void loadGiveawaysFromConfig() {
        Configuration giveawayConfig = TeDiSync.getPlugin().getConfig().getSection("giveaways");
        for (String giveawayName : giveawayConfig.getKeys()) {
            String name = giveawayConfig.getString(giveawayName + ".name");
            String message = giveawayConfig.getString(giveawayName + ".message");
            String title = giveawayConfig.getString(giveawayName + ".title");
            String buttonText = giveawayConfig.getString(giveawayName + ".buttontext");
            String chatColor = giveawayConfig.getString(giveawayName + ".chatcolor");
            boolean enterMultiple = giveawayConfig.getBoolean(giveawayName + ".enterMultiple");
            boolean open = giveawayConfig.getBoolean(giveawayName + ".open");
            long messageID = giveawayConfig.getLong(giveawayName + ".messageID");
            HashMap<Long, Integer> entries = entryListToMap(giveawayConfig.getStringList(giveawayName + ".entries"));
            HashMap<Long, Long> times = timeListToMap(giveawayConfig.getStringList(giveawayName + ".times"));
            Giveaway giveaway = new Giveaway(name, message, title, buttonText, chatColor, enterMultiple);
            giveaway.setMessageID(messageID);
            giveaway.setEntryList(entries);
            giveaway.setLastEntry(times);
            giveaway.setOpen(open);
            giveaways.put(giveawayName, giveaway);
        }
    }

    public HashMap<Long, Long> timeListToMap(List<String> timeList) {
        HashMap<Long, Long> listData = new HashMap<>();
        for (String rawData : timeList) {
            String[] raw = rawData.split(":");
            listData.put(Long.valueOf(raw[0]), Long.valueOf(raw[1]));
        }
        return listData;
    }

    public static List<String> timeMapToList(HashMap<Long, Long> timeMap) {
        List<String> hashmapData = new ArrayList<>();
        for (Long id : timeMap.keySet()) {
            String data = id.toString() + ":" + timeMap.get(id);
            hashmapData.add(data);
        }
        return hashmapData;
    }

    public static List<String> entryMapToList(HashMap<Long, Integer> entryMap) {
        List<String> hashmapData = new ArrayList<>();
        for (Long id : entryMap.keySet()) {
            String data = id.toString() + ":" + entryMap.get(id);
            hashmapData.add(data);
        }
        return hashmapData;
    }

    public static HashMap<Long, Integer> entryListToMap(List<String> entryList) {
        HashMap<Long, Integer> listData = new HashMap<>();
        for (String rawData : entryList) {
            String[] raw = rawData.split(":");
            listData.put(Long.valueOf(raw[0]), Integer.valueOf(raw[1]));
        }
        return listData;
    }

    public static boolean isCurrentDay(long zeitInMs) {
        Calendar lastEntry = Calendar.getInstance();
        lastEntry.setTimeInMillis(zeitInMs);

        Calendar currentDay = Calendar.getInstance();
        boolean sameDay = currentDay.get(Calendar.DAY_OF_MONTH) == lastEntry.get(Calendar.DAY_OF_MONTH);
        boolean sameMonth = currentDay.get(Calendar.MONTH) == lastEntry.get(Calendar.MONTH);
        boolean sameYear = currentDay.get(Calendar.YEAR) == lastEntry.get(Calendar.YEAR);

        return sameDay && sameMonth && sameYear;
    }


    public static HashMap<Long, UUID> getDiscordIdToUUID() {
        return discordIdToUUID;
    }
}
