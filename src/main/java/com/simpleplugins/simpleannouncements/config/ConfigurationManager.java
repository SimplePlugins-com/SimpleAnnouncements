package com.simpleplugins.simpleannouncements.config;

import com.simpleplugins.simpleannouncements.SimpleAnnouncements;
import com.simpleplugins.simpleannouncements.util.Version;
import lombok.Getter;
import me.clip.placeholderapi.PlaceholderAPI;
import net.md_5.bungee.api.ChatColor;
import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.logging.Level;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ConfigurationManager {
    private static final String ERROR_CREATING_DATAFOLDER = "Could not create DataFolder";
    private static final String ERROR_RELOADING_MESSAGES_CONFIGURATION = "Could not reload Messages Configuration";

    private MessagesConfiguration messagesConfig;

    @Getter
    private YamlConfiguration config;

    private boolean placeholderAPIEnabled;

    public ConfigurationManager(@NotNull SimpleAnnouncements plugin) {
        setupConfig(plugin);
        setupMessages(plugin);
        setupPluginSupport();
    }

    private void setupConfig(@NotNull SimpleAnnouncements plugin) {
        File dataFolder = plugin.getDataFolder();
        if (!dataFolder.exists() && !dataFolder.mkdir()) {
            plugin.getLogger().log(Level.SEVERE, ERROR_CREATING_DATAFOLDER);
            plugin.getPluginLoader().disablePlugin(plugin);
            return;
        }

        plugin.saveResource("config.yml", false);
        this.config = YamlConfiguration.loadConfiguration(new File(dataFolder, "config.yml"));
    }

    private void setupMessages(@NotNull SimpleAnnouncements plugin) {
        messagesConfig = new MessagesConfiguration(plugin.getDataFolder());
        try {
            messagesConfig.reload();
        } catch (IOException e) {
            plugin.getLogger().log(Level.SEVERE, ERROR_RELOADING_MESSAGES_CONFIGURATION, e);
            plugin.getPluginLoader().disablePlugin(plugin);
        }
    }

    private void setupPluginSupport() {
        placeholderAPIEnabled = config.getBoolean("placeholders.PlaceholderAPI") && Bukkit.getPluginManager().getPlugin("PlaceholderAPI") != null;
    }

    public String translateText(@NotNull Player player, @NotNull String text) {
        if (placeholderAPIEnabled) {
            text = PlaceholderAPI.setPlaceholders(player, text);
        }

        if (Version.v1_15.isGreater()) {
            return replaceHexColors(text);
        }

        return text;
    }

    private String replaceHexColors(String text) {
        Matcher matcher = Pattern.compile("#[a-fA-F0-9]{6}").matcher(text);
        while (matcher.find()) {
            String color = text.substring(matcher.start(), matcher.end());
            text = text.replace(color, ChatColor.of(color) + "");
        }
        return text;
    }

    public Optional<String> getText(@NotNull ConfigurationSection config, @NotNull String path) {
        String message = config.getString(path);
        if (message != null) {
            Map<String, Object> values = messagesConfig.getValues();
            return Optional.ofNullable(values.getOrDefault(message, message).toString());
        }
        return Optional.empty();
    }

    public Optional<List<String>> getTextList(@NotNull ConfigurationSection config, @NotNull String path) {
        String message = config.getString(path);
        if (message != null) {
            Map<String, Object> values = messagesConfig.getValues();
            return Optional.ofNullable((List<String>) values.getOrDefault(message, Collections.singletonList(message)));
        }
        return Optional.empty();
    }
}
