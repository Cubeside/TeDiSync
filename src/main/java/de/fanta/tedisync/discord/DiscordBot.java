package de.fanta.tedisync.discord;

import de.fanta.tedisync.TeDiSync;
import de.fanta.tedisync.discord.commands.DiscordCommandRegistration;
import de.fanta.tedisync.discord.listeners.DiscordEventRegistration;
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
import net.dv8tion.jda.api.interactions.components.buttons.Button;
import net.dv8tion.jda.api.requests.GatewayIntent;
import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.HoverEvent;
import net.md_5.bungee.api.chat.hover.content.Text;
import net.md_5.bungee.api.connection.ProxiedPlayer;

import java.util.HashMap;
import java.util.UUID;

public class DiscordBot extends ListenerAdapter {
    private static JDA discordAPI;
    private final TeDiSync plugin;
    private static HashMap<UUID, User> requests;

    public DiscordBot(TeDiSync plugin) {
        this.plugin = plugin;
        new DiscordCommandRegistration(plugin).registerCommands();
        new DiscordEventRegistration(plugin).registerEvents();

        plugin.getLogger().info("Login DiscordBot...");
        discordAPI = JDABuilder.createDefault(plugin.getConfig().getString("discord.login_token")).enableIntents(GatewayIntent.MESSAGE_CONTENT).build();
        plugin.getLogger().info("DiscordBot Logged in.");
        discordAPI.addEventListener(this);
        requests = new HashMap<>();
    }

    @Override
    public void onMessageReceived(MessageReceivedEvent event) {
        if (event.getAuthor().isBot()) {
            return;
        }
        User author = event.getAuthor();
        Message message = event.getMessage();
        if (event.getChannel() instanceof PrivateChannel privateChannel) {
            plugin.getLogger().info("[Private from: " + event.getChannel().getName() + "] " + author.getEffectiveName() + "(" + author.getName() + ")" + ": " + message.getContentRaw());
            String playerName = message.getContentRaw();
            ProxiedPlayer proxiedPlayer = plugin.getProxy().getPlayer(playerName);
            if (proxiedPlayer == null || !proxiedPlayer.isConnected()) {
                EmbedBuilder embedBuilder = new EmbedBuilder().setTitle("Fehler");
                embedBuilder.setColor(ChatUtil.RED.getColor());
                embedBuilder.setDescription(":stop_sign: Der Spieler " + playerName + " ist nicht Online.");
                privateChannel.sendMessageEmbeds(embedBuilder.build()).queue();
            } else {
                requests.put(proxiedPlayer.getUniqueId(), author);
                ChatUtil.sendNormalMessage(proxiedPlayer, "Möchtest du das Discord Konto " + ChatUtil.BLUE + author.getEffectiveName() + "(" + author.getName() + ")" + ChatUtil.GREEN + " mit Minecraft verbinden?");

                ClickEvent acceptClickEvent = new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/tedisync register discord accept");
                HoverEvent acceptHoverEvent = new HoverEvent(HoverEvent.Action.SHOW_TEXT, new Text("Annehmen"));

                ClickEvent denyClickEvent = new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/tedisync register discord deny");
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
                embedBuilder.setDescription("Eine anfrage zum Verbinden wurde an " + playerName + " in Minecraft geschickt.");
                privateChannel.sendMessageEmbeds(embedBuilder.build()).queue();
            }
        } else {
            plugin.getLogger().info("[" + event.getChannel().getName() + "] " + author.getEffectiveName() + "(" + author.getName() + ")" + ": " + message.getContentRaw());
            if (message.getContentRaw().equalsIgnoreCase("blub")) {
                event.getChannel().sendMessage("Klicke für ein Random Wort").addActionRow(Button.primary("test", "Klick mich")).queue();
            }
        }
    }

    @Override
    public void onButtonInteraction(ButtonInteractionEvent event) {
        if (event.getComponentId().equals("test")) {
            event.reply("Doch nicht so Random: Penis").queue();
        }
    }

    public static HashMap<UUID, User> getRequests() {
        return requests;
    }

    public static JDA getDiscordAPI() {
        return discordAPI;
    }

    public static User getDiscordUser(long id) {
        if (discordAPI != null) {
            return discordAPI.retrieveUserById(id).complete();
        }
        return null;
    }
}
