package de.fanta.tedisync.discord;

import de.fanta.tedisync.TeDiSync;
import de.fanta.tedisync.utils.RandomCollection;
import de.iani.cubesideutils.StringUtil;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.channel.middleman.MessageChannel;
import net.dv8tion.jda.api.entities.emoji.Emoji;
import net.dv8tion.jda.api.interactions.components.buttons.Button;
import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.config.Configuration;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ExecutionException;
import java.util.logging.Level;

public class Giveaway {
    private final String name;
    private final String notificationButton;
    private String message;
    private String title;
    private String buttonText;
    private String chatColor;
    private boolean enterMultiple = false;
    private boolean open = false;
    private HashMap<Long, Integer> entryList = new HashMap<>();
    private HashMap<Long, Long> lastEntry = new HashMap<>();
    private long messageID;

    public Giveaway(String name, String message, String title, String buttonText, String chatColor, boolean enterMultiple, HashMap<Long, Integer> entryList, HashMap<Long, Long> lastEntry) {
        this.name = name;
        this.message = message;
        this.title = title;
        this.buttonText = buttonText;
        this.chatColor = chatColor;
        this.enterMultiple = enterMultiple;
        this.entryList = entryList;
        this.lastEntry = lastEntry;
        this.notificationButton = name + "_Notification";
    }

    public Giveaway(String name, String message, String title, String buttonText, String chatColor, boolean enterMultiple) {
        this.name = name;
        this.message = message;
        this.title = title;
        this.buttonText = buttonText;
        this.chatColor = chatColor;
        this.enterMultiple = enterMultiple;
        this.notificationButton = name + "_Notification";
    }

    public Giveaway(String name) {
        this.name = name;
        this.notificationButton = name + "_Notification";
    }

    public String getName() {
        return name;
    }

    public String getMessage() {
        return message;
    }

    public String getTitle() {
        return title;
    }

    public String getButtonText() {
        return buttonText;
    }

    public String getChatColor() {
        return chatColor;
    }

    public boolean isEnterMultiple() {
        return enterMultiple;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public void setButtonText(String buttonText) {
        this.buttonText = buttonText;
    }

    public void setChatColor(String chatColor) {
        this.chatColor = chatColor;
    }

    public void setEnterMultiple(boolean enterMultiple) {
        this.enterMultiple = enterMultiple;
    }

    public String getNotificationButton() {
        return notificationButton;
    }

    public boolean isOpen() {
        return open;
    }

    public boolean canOpen() {
        return name != null && message != null && title != null && buttonText != null && chatColor != null;
    }

    public boolean setOpen(boolean open) {
        if (open && name != null && message != null && title != null && buttonText != null && chatColor != null) {
            this.open = true;
        } else if (!open) {
            this.open = false;
        }
        return this.open;
    }

    public void setMessageID(long messageID) {
        this.messageID = messageID;
    }

    public HashMap<Long, Integer> getEntryList() {
        return entryList;
    }

    public HashMap<Long, Long> getLastEntry() {
        return lastEntry;
    }

    public void setEntryList(HashMap<Long, Integer> entryList) {
        this.entryList = entryList;
    }

    public void setLastEntry(HashMap<Long, Long> lastEntry) {
        this.lastEntry = lastEntry;
    }

    public boolean countUp(Long id) {
        entryList.put(id, entryList.getOrDefault(id, 0) + 1);
        lastEntry.put(id, System.currentTimeMillis());

        Configuration config = TeDiSync.getPlugin().getConfig();
        config.set("giveaways." + name.toLowerCase() + ".entries", DiscordBot.entryMapToList(entryList));
        config.set("giveaways." + name.toLowerCase() + ".times", DiscordBot.timeMapToList(lastEntry));

        try {
            if (TeDiSync.getPlugin().saveConfig()) {
                return true;
            }
        } catch (IOException e) {
            TeDiSync.getPlugin().getLogger().log(Level.SEVERE, "Giveaway entries could not be saved");
        }
        entryList.put(id, entryList.get(id) - 1);
        return false;
    }

    public int getEntryCount(Long id) {
        return entryList.getOrDefault(id, 0);
    }

    public Long drawRandom() {
        if (!entryList.isEmpty()) {
            RandomCollection<Long> users = new RandomCollection<>();
            entryList.forEach((aLong, integer) -> users.add(integer, aLong));
            return users.next();
        }
        return null;
    }

    public boolean saveToConfig() {
        Configuration config = TeDiSync.getPlugin().getConfig();
        config.set("giveaways." + name.toLowerCase() + ".name", name);
        config.set("giveaways." + name.toLowerCase() + ".message", message);
        config.set("giveaways." + name.toLowerCase() + ".title", title);
        config.set("giveaways." + name.toLowerCase() + ".buttontext", buttonText);
        config.set("giveaways." + name.toLowerCase() + ".chatcolor", chatColor);
        config.set("giveaways." + name.toLowerCase() + ".enterMultiple", enterMultiple);
        config.set("giveaways." + name.toLowerCase() + ".open", open);
        config.set("giveaways." + name.toLowerCase() + ".messageID", messageID);

        try {
            if (TeDiSync.getPlugin().saveConfig()) {
                return true;
            }
        } catch (IOException e) {
            TeDiSync.getPlugin().getLogger().log(Level.SEVERE, "Giveaway could not be saved.");
            return false;
        }
        return false;
    }

    public boolean delete() {
        Configuration config = TeDiSync.getPlugin().getConfig();
        config.set("giveaways." + name.toLowerCase(), null);

        try {
            if (TeDiSync.getPlugin().saveConfig()) {
                DiscordBot.getGiveaways().remove(name.toLowerCase());
                return true;
            }
        } catch (IOException e) {
            TeDiSync.getPlugin().getLogger().log(Level.SEVERE, "Giveaway could not be deleted.");
            return false;
        }
        return false;
    }

    public void sendGiveawayToDiscordChannel() {
        MessageChannel channel = DiscordBot.getDiscordAPI().getTextChannelById(TeDiSync.getPlugin().getConfig().getLong("discord.giveawaychannel"));
        if (channel != null) {
            EmbedBuilder embedBuilder = new EmbedBuilder().setTitle(title);
            embedBuilder.setColor(ChatColor.of(chatColor).getColor());
            embedBuilder.setDescription(message);
            embedBuilder.setFooter("Gewinnspiel: " + StringUtil.capitalizeFirstLetter(name, true));
            try {
                Message gm;
                if (isEnterMultiple()) {
                    gm = channel.sendMessageEmbeds(embedBuilder.build()).addActionRow(Button.primary(name, buttonText), Button.success(name + "_Notification", Emoji.fromUnicode("U+1F4E7"))).submit().get();
                } else {
                    gm = channel.sendMessageEmbeds(embedBuilder.build()).addActionRow(Button.primary(name, buttonText)).submit().get();
                }

                this.setMessageID(gm.getIdLong());
                if (this.saveToConfig()) {
                    return;
                }
                gm.delete().queue();
            } catch (InterruptedException | ExecutionException e) {
                TeDiSync.getPlugin().getLogger().log(Level.SEVERE, "Message could not be sent.", e);
            }
        }
    }

    public void deleteGiveawayFromDiscordChannel() {
        MessageChannel channel = DiscordBot.getDiscordAPI().getTextChannelById(TeDiSync.getPlugin().getConfig().getLong("discord.giveawaychannel"));
        if (channel != null) {
            Message gm = channel.retrieveMessageById(messageID).complete();
            TeDiSync.getPlugin().getLogger().info(String.valueOf(messageID));
            if (gm != null) {
                gm.delete().queue();
            }
        }
    }

    public int countEntries() {
        int count = 0;
        for (Integer entries : entryList.values()) {
            count += entries;
        }
        return count;
    }
}
