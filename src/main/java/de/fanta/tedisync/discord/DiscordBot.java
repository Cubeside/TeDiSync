package de.fanta.tedisync.discord;

import de.fanta.tedisync.TeDiSync;
import de.fanta.tedisync.discord.commands.DiscordCommandRegistration;
import de.fanta.tedisync.utils.ChatUtil;
import de.iani.cubesideutils.ComponentUtil;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.events.interaction.ModalInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.components.ActionRow;
import net.dv8tion.jda.api.interactions.components.buttons.Button;
import net.dv8tion.jda.api.interactions.components.text.TextInput;
import net.dv8tion.jda.api.interactions.components.text.TextInputStyle;
import net.dv8tion.jda.api.interactions.modals.Modal;
import net.dv8tion.jda.api.requests.GatewayIntent;
import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.HoverEvent;
import net.md_5.bungee.api.chat.hover.content.Text;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.event.PostLoginEvent;
import net.md_5.bungee.api.plugin.Listener;
import net.md_5.bungee.config.Configuration;
import net.md_5.bungee.event.EventHandler;

import javax.annotation.Nonnull;
import java.awt.*;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;

public class DiscordBot extends ListenerAdapter implements Listener {
    private static JDA discordAPI;
    private final TeDiSync plugin;
    private static HashMap<UUID, User> requests;
    private static HashMap<String, Giveaway> giveaways;
    private static HashMap<UUID, String> userEditGiveaway;
    private static HashMap<Long, UUID> discordIdToUUID;
    private static HashMap<UUID, Long> UUIDToDiscordID;
    private static final Collection<UUID> playerNotificationList = new ArrayList<>();

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
        UUIDToDiscordID = new HashMap<>();

        Configuration config = TeDiSync.getPlugin().getConfig().getSection("discorduser");
        for (String id : config.getKeys()) {
            discordIdToUUID.put(Long.valueOf(id), UUID.fromString(config.getString(id)));
            UUIDToDiscordID.put(UUID.fromString(config.getString(id)), Long.valueOf(id));
        }
        loadGiveawaysFromConfig();
        startNotificationTask();
        ProxyServer.getInstance().getPluginManager().registerListener(plugin, this);
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
                DiscordBot.getUUIDToDiscordID().put(uuid, id);
                return true;
            }
        } catch (IOException e) {
            TeDiSync.getPlugin().getLogger().log(Level.SEVERE, "User could not be saved");
            return false;
        }
        return false;
    }

    @Override
    public void onButtonInteraction(ButtonInteractionEvent event) {
        if (event.getComponentId().equals("TeDiSync-Register")) {
            TextInput nameTextField = TextInput.create("TeDiSync-MinecraftName", "Minecraft-Name", TextInputStyle.SHORT).setPlaceholder("Minecraft Name").setRequiredRange(1, 16).setRequired(true).build();
            Modal modal = Modal.create("TeDiSync-Register", "Registrieren").addComponents(ActionRow.of(nameTextField)).build();
            event.replyModal(modal).queue();
            return;
        }
        for (Giveaway giveaway : giveaways.values()) {
            if (giveaway.getName().equals(event.getComponentId())) {
                if (!giveaway.isOpen()) {
                    privateReplay(event, "Das Gewinnspiel ist momentan nicht geöffnet.", ChatUtil.ORANGE.getColor());
                    return;
                }

                if (giveaway.isEnterMultiple()) {
                    if (giveaway.getLastEntry().containsKey(event.getUser().getIdLong()) && isCurrentDay(giveaway.getLastEntry().get(event.getUser().getIdLong()))) {
                        privateReplay(event, "Du hast dich heute schon für dieses Gewinnspiel eingetragen. Du bist " + giveaway.getEntryCount(event.getUser().getIdLong()) + "x für das Gewinnspiel eingetragen.", ChatUtil.RED.getColor());
                        return;
                    }
                } else {
                    if (giveaway.getEntryList().containsKey(event.getUser().getIdLong())) {
                        privateReplay(event, "Du kannst dich für dieses Gewinnspiel nur ein Mal eintragen.", ChatUtil.RED.getColor());
                        return;
                    }
                }

                if (giveaway.countUp(event.getUser().getIdLong())) {
                    privateReplay(event, "Du hast dich für das Gewinnspiel eingetragen." + (giveaway.isEnterMultiple() ? " Du bist jetzt " + giveaway.getEntryCount(event.getUser().getIdLong()) + "x für das Gewinnspiel eingetragen." : ""), ChatUtil.GREEN.getColor());
                } else {
                    privateReplay(event, "Es ist ein Fehler aufgetreten.", ChatUtil.RED.getColor());
                }
            }

            if (giveaway.getNotificationButton().equals(event.getComponentId())) {
                UUID playerUUID = DiscordBot.discordIdToUUID.get(event.getUser().getIdLong());
                if (playerUUID == null) {
                    long channelID = plugin.getConfig().getLong("discord.registerchannel", -1);
                    if (channelID == -1) {
                        return;
                    }
                    TextChannel channel = DiscordBot.getDiscordAPI().getTextChannelById(channelID);
                    privateReplay(event, "Dafür musst du dich im Discord Channel " + (channel != null ? channel.getName() : "NULL") + " Registrieren.", ChatUtil.RED.getColor());
                    return;
                }
                if (toggleNotification(playerUUID)) {
                    privateReplay(event, "Du wirst jetzt in Minecraft benachrichtigt, falls du dich an einem Tag noch nicht für aktive Gewinnspiele eingetragen hast.", ChatUtil.GREEN.getColor());
                } else {
                    privateReplay(event, "Du wirst jetzt nicht mehr in Minecraft benachrichtigt, falls du dich an einem Tag noch nicht für aktive Gewinnspiele eingetragen hast.", ChatUtil.GREEN.getColor());
                }
            }
        }
    }

    @Override
    public void onModalInteraction(@Nonnull ModalInteractionEvent event) {
        if (event.getModalId().equals("TeDiSync-Register")) {
            User author = event.getUser();
            String playerName = event.getValue("TeDiSync-MinecraftName").getAsString();

            ProxiedPlayer proxiedPlayer = plugin.getProxy().getPlayer(playerName);
            if (proxiedPlayer == null || !proxiedPlayer.isConnected()) {
                EmbedBuilder embedBuilder = new EmbedBuilder().setTitle("Fehler");
                embedBuilder.setColor(ChatUtil.RED.getColor());
                embedBuilder.setDescription("Der Spieler " + playerName + " ist nicht online.");
                event.reply("").setEmbeds(embedBuilder.build()).setEphemeral(true).queue();
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
                event.reply("").setEmbeds(embedBuilder.build()).setEphemeral(true).queue();
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
            List<String> notificationStringList = giveawayConfig.getStringList(giveawayName + ".notifications");
            List<UUID> notificationList = new ArrayList<>();
            for (String UUIDString : notificationStringList) {
                notificationList.add(UUID.fromString(UUIDString));
            }
            Giveaway giveaway = new Giveaway(name, message, title, buttonText, chatColor, enterMultiple);
            giveaway.setMessageID(messageID);
            giveaway.setEntryList(entries);
            giveaway.setLastEntry(times);
            giveaway.setOpen(open);
            giveaways.put(giveawayName, giveaway);
            playerNotificationList.addAll(notificationList);
        }

        List<String> notificationStringList = giveawayConfig.getStringList("notifications");
        List<UUID> notificationList = new ArrayList<>();
        for (String UUIDString : notificationStringList) {
            notificationList.add(UUID.fromString(UUIDString));
        }
        playerNotificationList.addAll(notificationList);
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

    public static boolean isCurrentDay(long timeInMs) {
        Calendar lastEntry = Calendar.getInstance();
        lastEntry.setTimeInMillis(timeInMs);

        Calendar currentDay = Calendar.getInstance();
        boolean sameDay = currentDay.get(Calendar.DAY_OF_MONTH) == lastEntry.get(Calendar.DAY_OF_MONTH);
        boolean sameMonth = currentDay.get(Calendar.MONTH) == lastEntry.get(Calendar.MONTH);
        boolean sameYear = currentDay.get(Calendar.YEAR) == lastEntry.get(Calendar.YEAR);

        return sameDay && sameMonth && sameYear;
    }

    public static HashMap<Long, UUID> getDiscordIdToUUID() {
        return discordIdToUUID;
    }

    public static HashMap<UUID, Long> getUUIDToDiscordID() {
        return UUIDToDiscordID;
    }

    public static void sendRegisterMessage(TextChannel channel) {
        EmbedBuilder embedBuilder = new EmbedBuilder().setTitle("Registrieren");
        embedBuilder.setColor(ChatUtil.GREEN.getColor());
        embedBuilder.setDescription("Du kannst deinen Account mit deinem Minecraft-Account verknüpfen, damit dein Rang im Discord automatisch gesetzt werden kann und auch andere zusätzliche Funktionen für dich nutzbar sind.");
        channel.sendMessageEmbeds(embedBuilder.build()).addActionRow(Button.success("TeDiSync-Register", "Registrieren")).submit();
    }

    @EventHandler
    public void onPlayerJoin(PostLoginEvent event) {
        ProxiedPlayer player = event.getPlayer();
        ProxyServer.getInstance().getScheduler().schedule(plugin, () -> {
            for (Giveaway giveaway : giveaways.values()) {
                if (!giveaway.isOpen()) {
                    continue;
                }
                sendNotificationToPlayer(giveaway, player.getUniqueId());
            }
        }, 3, TimeUnit.SECONDS);
    }

    private void startNotificationTask() {
        ProxyServer.getInstance().getScheduler().schedule(plugin, () -> {
            for (Giveaway giveaway : giveaways.values()) {
                if (!giveaway.isOpen()) {
                    continue;
                }
                for (UUID uuid : getPlayerNotificationList()) {
                    sendNotificationToPlayer(giveaway, uuid);
                }
            }
        }, 0, 30, TimeUnit.MINUTES);
    }

    private void sendNotificationToPlayer(Giveaway giveaway, UUID uuid) {
        if (!getPlayerNotificationList().contains(uuid)) {
            return;
        }
        User discordUser;
        try {
            discordUser = discordAPI.retrieveUserById(UUIDToDiscordID.get(uuid)).submit().get();
        } catch (InterruptedException | ExecutionException e) {
            throw new RuntimeException(e);
        }
        if (discordUser == null || !giveaway.getLastEntry().containsKey(discordUser.getIdLong())) {
            return;
        }
        Long lastEntry = giveaway.getLastEntry().get(discordUser.getIdLong());
        if (!isCurrentDay(lastEntry)) {
            ProxiedPlayer player = ProxyServer.getInstance().getPlayer(uuid);
            if (player != null && player.isConnected()) {
                ChatUtil.sendNormalMessage(player, "Hey, du hast dich heute noch nicht für das Gewinnspiel " + ChatUtil.BLUE + giveaway.getName() + ChatUtil.GREEN + " im Discord eingetragen!");
            }
        }
    }

    private void setPlayerNotificationList(Collection<UUID> playerNotifications) {
        playerNotificationList.clear();
        playerNotificationList.addAll(playerNotifications);
    }

    private static Collection<UUID> getPlayerNotificationList() {
        return playerNotificationList;
    }

    public boolean toggleNotification(UUID uuid) {
        if (playerNotificationList.contains(uuid)) {
            playerNotificationList.remove(uuid);
        } else {
            playerNotificationList.add(uuid);
        }

        Configuration config = TeDiSync.getPlugin().getConfig();
        List<String> UUIDStrings = new ArrayList<>();
        for (UUID uuidFromList : playerNotificationList) {
            UUIDStrings.add(uuidFromList.toString());
        }

        config.set("giveaways.notifications", UUIDStrings);

        try {
            if (TeDiSync.getPlugin().saveConfig()) {
                return playerNotificationList.contains(uuid);
            }
        } catch (IOException e) {
            TeDiSync.getPlugin().getLogger().log(Level.SEVERE, "Giveaway notifications could not be saved");
        }
        return playerNotificationList.contains(uuid);
    }
}
