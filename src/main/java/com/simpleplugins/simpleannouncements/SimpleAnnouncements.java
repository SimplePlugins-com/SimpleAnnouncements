package com.simpleplugins.simpleannouncements;

import com.simpleplugins.simpleannouncements.broadcast.BroadcastManager;
import com.simpleplugins.simpleannouncements.config.ConfigurationManager;
import lombok.Getter;
import org.bukkit.plugin.java.JavaPlugin;

@Getter
public class SimpleAnnouncements extends JavaPlugin {
    @Getter
    private static SimpleAnnouncements instance;

    private ConfigurationManager configurationManager;

    @Override
    public void onEnable() {
        instance = this;

        configurationManager = new ConfigurationManager(this);
        new BroadcastManager(configurationManager);
    }

    @Override
    public void onDisable() {

    }
}