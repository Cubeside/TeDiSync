package de.fanta.tedisync.discord;

import de.fanta.tedisync.TeDiSync;
import de.fanta.tedisync.discord.commands.DiscordCommandRegistration;
import de.fanta.tedisync.utils.ChatUtil;
import de.iani.cubesideutils.ComponentUtil;

import java.awt.Color;
import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;

import de.iani.cubesideutils.bungee.sql.SQLConfigBungee;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.ISnowflake;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Role;
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
import net.luckperms.api.LuckPermsProvider;
import net.luckperms.api.model.group.Group;
import net.luckperms.api.query.Flag;
import net.luckperms.api.query.QueryMode;
import net.luckperms.api.query.QueryOptions;
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
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class DiscordBot extends ListenerAdapter implements Listener {
    private static JDA discordAPI;
    private final TeDiSync plugin;
    private static DiscordDatabase database;
    private static Map<UUID, User> requests;
    private static Map<String, Giveaway> giveaways;
    private static Map<UUID, String> userEditGiveaway;
    private static ConcurrentHashMap<String, Long> groupIDs;


    public DiscordBot(TeDiSync plugin) {
        this.plugin = plugin;
        database = new DiscordDatabase(new SQLConfigBungee(plugin.getConfig().getSection("discord.database")));

        plugin.getLogger().info("Login DiscordBot...");
        discordAPI = JDABuilder.createDefault(plugin.getConfig().getString("discord.login_token")).enableIntents(GatewayIntent.MESSAGE_CONTENT).build();
        plugin.getLogger().info("DiscordBot Logged in as " + discordAPI.getSelfUser().getName());
        discordAPI.addEventListener(this);
        requests = new ConcurrentHashMap<>();
        giveaways = new ConcurrentHashMap<>();
        userEditGiveaway = new ConcurrentHashMap<>();
        groupIDs = new ConcurrentHashMap<>();
        Configuration rankConfig = plugin.getConfig().getSection("discord.rankIDs");
        rankConfig.getKeys().forEach(s -> groupIDs.put(s, rankConfig.getLong(s)));

        if (!plugin.getConfig().getBoolean("discord.convert")) {
            convertUser();
        }

        loadGiveawaysFromConfig();
        startNotificationTask();
        ProxyServer.getInstance().getPluginManager().registerListener(plugin, this);
    }

    public void registerCommands() {
        new DiscordCommandRegistration(plugin).registerCommands();
    }

    private void convertUser() {
        Map<Long, UUID> discordIdToUUID = new ConcurrentHashMap<>();

        Configuration config = TeDiSync.getPlugin().getConfig().getSection("discorduser");
        for (String id : config.getKeys()) {
            discordIdToUUID.put(Long.valueOf(id), UUID.fromString(config.getString(id)));
        }

        List<String> notificationStringList = TeDiSync.getPlugin().getConfig().getStringList("notifications");
        List<UUID> notificationList = new ArrayList<>();
        for (String UUIDString : notificationStringList) {
            notificationList.add(UUID.fromString(UUIDString));
        }

        discordIdToUUID.forEach((id, uuid) -> {
            boolean notification = notificationList.contains(uuid);
            try {
                database.insertUser(uuid, id, notification);
            } catch (SQLException e) {
                plugin.getLogger().log(Level.SEVERE, "Error by add convert user to database!", e);
            }
        });

        plugin.getConfig().set("discord.convert", true);
        try {
            plugin.saveConfig();
        } catch (IOException e) {
            plugin.getLogger().log(Level.SEVERE, "Error while save Config");
        }
    }

    public static Map<String, Giveaway> getGiveaways() {
        return giveaways;
    }

    public static Map<UUID, String> getUserEditGiveaway() {
        return userEditGiveaway;
    }

    public static Map<UUID, User> getRequests() {
        return requests;
    }

    public static JDA getDiscordAPI() {
        return discordAPI;
    }

    public static boolean saveUser(UUID uuid, Long id) {
        try {
            database.insertUser(uuid, id, false);
            return true;
        } catch (SQLException e) {
            TeDiSync.getPlugin().getLogger().log(Level.SEVERE, "Error while saving user in Database", e);
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
                DiscordUserInfo discordUserInfo = null;
                try {
                    discordUserInfo = database.getUserByDCID(event.getUser().getIdLong());
                } catch (SQLException ignore) {
                }
                UUID playerUUID = discordUserInfo == null ? null : discordUserInfo.uuid();
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
    public void onModalInteraction(@NotNull ModalInteractionEvent event) {
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

    public static boolean isCurrentDay(long timeInMs) {
        Calendar lastEntry = Calendar.getInstance();
        lastEntry.setTimeInMillis(timeInMs);

        Calendar currentDay = Calendar.getInstance();
        boolean sameDay = currentDay.get(Calendar.DAY_OF_MONTH) == lastEntry.get(Calendar.DAY_OF_MONTH);
        boolean sameMonth = currentDay.get(Calendar.MONTH) == lastEntry.get(Calendar.MONTH);
        boolean sameYear = currentDay.get(Calendar.YEAR) == lastEntry.get(Calendar.YEAR);

        return sameDay && sameMonth && sameYear;
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
                try {
                    database.getGivewayNotificationUser().forEach(discordUserInfo -> sendNotificationToPlayer(giveaway, discordUserInfo.uuid()));
                } catch (SQLException e) {
                    plugin.getLogger().log(Level.SEVERE, "Error while loading Notification User", e);
                }
            }
        }, 0, 30, TimeUnit.MINUTES);
    }

    private void sendNotificationToPlayer(Giveaway giveaway, UUID uuid) {
        DiscordUserInfo userInfo;
        try {
            userInfo = database.getUsersByUUID(uuid);
        } catch (SQLException e) {
            return;
        }

        if (userInfo == null || !userInfo.notification()) {
            return;
        }

        User discordUser;
        try {
            discordUser = discordAPI.retrieveUserById(userInfo.dcID()).submit().get();
        } catch (InterruptedException | ExecutionException e) {
            throw new RuntimeException(e);
        }
        if (discordUser == null) {
            return;
        }
        Long lastEntry = giveaway.getLastEntry().get(discordUser.getIdLong());
        if (!isCurrentDay(lastEntry != null ? lastEntry : 0)) {
            ProxiedPlayer player = ProxyServer.getInstance().getPlayer(uuid);
            if (player != null && player.isConnected()) {
                ChatUtil.sendNormalMessage(player, "Hey, du hast dich heute noch nicht für das Gewinnspiel " + ChatUtil.BLUE + giveaway.getName() + ChatUtil.GREEN + " im Discord eingetragen!");
            }
        }
    }

    public void updateDiscordGroup(UUID uuid, DiscordUserInfo discordUserInfo, @Nullable net.luckperms.api.model.user.User luckpermsUser, boolean register) {
        if (discordUserInfo == null) {
            return;
        }

        plugin.getProxy().getScheduler().runAsync(plugin, () -> {
            net.luckperms.api.model.user.User lpUser = luckpermsUser;
            try {
                Guild guild = discordAPI.getGuildById(plugin.getConfig().getLong("discord.serverID"));
                if (guild == null) {
                    throw new NullPointerException("discord guild is null");
                }

                Member member = guild.retrieveMemberById(discordUserInfo.dcID()).complete();
                if (member == null) {
                    throw new NullPointerException("user is null");
                }

                if (lpUser == null) {
                    if (LuckPermsProvider.get().getUserManager().isLoaded(uuid)) {
                        lpUser = LuckPermsProvider.get().getUserManager().getUser(uuid);
                    } else {
                        lpUser = LuckPermsProvider.get().getUserManager().loadUser(uuid).get();
                    }
                }

                ArrayList<Group> userGroups = new ArrayList<>(lpUser.getInheritedGroups(QueryOptions.builder(QueryMode.NON_CONTEXTUAL).flag(Flag.RESOLVE_INHERITANCE, true).build()));
                userGroups.sort((a, b) -> Integer.compare(b.getWeight().orElse(0), a.getWeight().orElse(0)));
                String group = "default";
                for (Group g : userGroups) {
                    if (groupIDs.containsKey(g.getName())) {
                        group = g.getName();
                        break;
                    }
                }
                long groupID = groupIDs.get(group);
                long[] ranks = member.getRoles().stream().mapToLong(ISnowflake::getIdLong).toArray();
                List<Long> dcUserRanks = Arrays.stream(ranks).boxed().toList();

                for (Long userRank : dcUserRanks) {
                    if (groupIDs.containsValue(userRank) && userRank != groupID) {
                        Role role = guild.getRoleById(userRank);
                        if (role == null) {
                            return;
                        }
                        if (!groupIDs.get("default").equals(userRank)) {
                            guild.removeRoleFromMember(member, role).queue();
                        }
                    }
                }

                if (!dcUserRanks.contains(groupID)) {
                    Role role = guild.getRoleById(groupID);
                    if (role == null) {
                        return;
                    }
                    guild.addRoleToMember(member, role).queue();
                }

                if (register) {
                    Role role = guild.getRoleById(groupIDs.get("default"));
                    if (role != null) {
                        guild.addRoleToMember(member, role).queue();
                    }
                }
            } catch (InterruptedException | ExecutionException e) {
                throw new RuntimeException(e);
            }
        });
    }

    public boolean toggleNotification(UUID uuid) {
        try {
            DiscordUserInfo discordUserInfo = database.getUsersByUUID(uuid);
            boolean notification = !discordUserInfo.notification();
            database.updateNotificationUser(uuid, notification);
            return notification;
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "Error while change notification option", e);
        }
        return false;
    }

    public static DiscordDatabase getDatabase() {
        return database;
    }

    public void stopDiscordBot() {
        discordAPI.shutdownNow();
        database.disconnect();
    }
}
