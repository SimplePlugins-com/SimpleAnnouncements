package com.simpleplugins.simpleannouncements.broadcast;

import com.simpleplugins.simpleannouncements.SimpleAnnouncements;
import com.simpleplugins.simpleannouncements.config.ConfigurationManager;
import com.simpleplugins.simpleannouncements.util.Reflection;
import com.simpleplugins.simpleannouncements.util.Version;
import net.md_5.bungee.api.chat.*;
import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Constructor;
import java.util.List;
import java.util.Optional;
import java.util.logging.Level;

public class BroadcastManager {
    private static final String ERROR_CONFIGURATION_SECTION_MESSAGE = "Could not get Configuration Section '{SECTION}'";
    private static final String ERROR_PACKET_SENDING_MESSAGE = "Could not send Title Packet to UUID '{UUID}'";

    private final ConfigurationManager configManager;

    public BroadcastManager(@NotNull ConfigurationManager configManager) {
        this.configManager = configManager;

        createSchedulers();
    }

    private void createSchedulers() {
        ConfigurationSection section = configManager.getConfig().getConfigurationSection("auto-broadcasts");

        if (section == null) {
            SimpleAnnouncements.getInstance().getLogger().log(Level.SEVERE, ERROR_CONFIGURATION_SECTION_MESSAGE.replace("{SECTION}", "auto-broadcasts"));
            SimpleAnnouncements.getInstance().getPluginLoader().disablePlugin(SimpleAnnouncements.getInstance());
            return;
        }

        int allDelay = 0;
        for (String name : section.getKeys(false)) {
            int delay = section.getInt(name + ".delay");
            if (delay != -1) allDelay += delay;
        }

        int startDelay = 0;
        for (String name : section.getKeys(false)) {
            ConfigurationSection target = section.getConfigurationSection(name);
            if (target != null) {
                int delay = target.getInt("delay");
                if (delay != -1) {
                    startDelay += delay;
                    SimpleAnnouncements.getInstance().getServer().getScheduler().runTaskTimer(
                            SimpleAnnouncements.getInstance(),
                            () -> sendBroadcast(target, target.getString("permission")),
                            startDelay * 20L,
                            allDelay * 20L
                    );
                }
            }
        }
    }

    private void sendBroadcast(ConfigurationSection target, String permission) {
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (permission == null || player.hasPermission(permission)) {
                sendMessages(target, player);
                playSound(target, player);
                sendTitle(target, player);
                executePlayerCommands(target, player);
            }
        }

        for (String command : target.getStringList("commands.broadcast")) {
            Bukkit.dispatchCommand(Bukkit.getConsoleSender(), command);
        }
    }

    private void sendMessages(ConfigurationSection target, Player player) {
        Optional<List<String>> messages = configManager.getTextList(target, "message.message");
        messages.ifPresent(msgList -> {
            for (String message : msgList) {
                TextComponent component = new TextComponent(TextComponent.fromLegacyText(configManager.translateText(player, message)));
                setHoverText(target, player, component);
                setClickEvent(target, component);
                player.spigot().sendMessage(component);
            }
        });
    }

    private void setHoverText(ConfigurationSection target, Player player, TextComponent component) {
        Optional<List<String>> hoverMessages = configManager.getTextList(target, "message.hover");
        hoverMessages.ifPresent(messages -> {
            ComponentBuilder builder = new ComponentBuilder("");

            for (String message : messages)
                builder.append(SimpleAnnouncements.getInstance().getConfigurationManager().translateText(player, message)).append("\n");

            component.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, builder.create()));
        });
    }

    private void setClickEvent(ConfigurationSection target, TextComponent component) {
        String openLink = target.getString("message.onClick.open-link");
        String executeCommand = target.getString("message.onClick.execute-command");
        String suggestCommand = target.getString("message.onClick.suggest-command");

        if (openLink != null)
            component.setClickEvent(new ClickEvent(ClickEvent.Action.OPEN_URL, openLink));

        if (executeCommand != null)
            component.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, executeCommand));

        if (suggestCommand != null)
            component.setClickEvent(new ClickEvent(ClickEvent.Action.SUGGEST_COMMAND, suggestCommand));
    }

    private void playSound(ConfigurationSection target, Player player) {
        String sound = target.getString("sound.name");
        if (sound != null) {
            player.playSound(player.getLocation(), getSoundByName(sound), target.getInt("sound.volume", 1), target.getInt("sound.pitch", 1));
        }
    }

    private void sendTitle(ConfigurationSection target, Player player) {
        Optional<String> title = configManager.getText(target, "title.title");
        title.ifPresent(t -> {
            String subtitle = configManager.getText(target, "title.subtitle").orElse(null);

            int fadeIn = target.getInt("title.fadeIn", 1);
            int stay = target.getInt("title.stay", 20);
            int fadeOut = target.getInt("title.fadeOut", 1);

            sendTitle(player, configManager.translateText(player, t), subtitle, fadeIn, stay, fadeOut);
        });
    }

    private void executePlayerCommands(ConfigurationSection target, Player player) {
        for (String command : target.getStringList("commands.player"))
            player.performCommand(configManager.translateText(player, command));
    }

    private Sound getSoundByName(String name) {
        return name.equals("ENTITY_EXPERIENCE_ORB_PICKUP") && Version.v1_9.isLess() ? Sound.valueOf("ORB_PICKUP") : Sound.valueOf(name);
    }

    private void sendTitle(@NotNull Player player, @NotNull String title, @Nullable String subTitle, int fadeIn, int stay, int fadeOut) {
        if (Version.v1_12.isLess())
            sendNMSTitle(player, title, subTitle, fadeIn, stay, fadeOut);
        else
            player.sendTitle(title, subTitle, fadeIn, stay, fadeOut);
    }

    private void sendNMSTitle(Player player, String title, String subTitle, int fadeIn, int stay, int fadeOut) {
        try {
            Constructor<?> titleConstructor = Reflection.getNMSClass("PacketPlayOutTitle").getConstructor(
                    Reflection.getNMSClass("PacketPlayOutTitle").getDeclaredClasses()[0], Reflection.getNMSClass("IChatBaseComponent"), int.class, int.class, int.class);
            Object enumTitle = Reflection.getNMSClass("PacketPlayOutTitle").getDeclaredClasses()[0].getField("TITLE").get(null);
            Object titleChat = Reflection.getNMSClass("IChatBaseComponent").getDeclaredClasses()[0].getMethod("a", String.class).invoke(null, "{\"text\":\"" + title + "\"}");
            Object packet = titleConstructor.newInstance(enumTitle, titleChat, fadeIn, stay, fadeOut);
            Reflection.sendPacket(player, packet);

            if (subTitle != null) {
                Object enumSubtitle = Reflection.getNMSClass("PacketPlayOutTitle").getDeclaredClasses()[0].getField("SUBTITLE").get(null);
                Object subtitleChat = Reflection.getNMSClass("IChatBaseComponent").getDeclaredClasses()[0].getMethod("a", String.class).invoke(null, "{\"text\":\"" + subTitle + "\"}");
                Object subtitlePacket = titleConstructor.newInstance(enumSubtitle, subtitleChat, fadeIn, stay, fadeOut);
                Reflection.sendPacket(player, subtitlePacket);
            }
        } catch (Exception e) {
            SimpleAnnouncements.getInstance().getLogger().log(Level.SEVERE, ERROR_PACKET_SENDING_MESSAGE.replace("{UUID}", player.getUniqueId().toString()));
        }
    }
}