package com.simpleplugins.simpleannouncements.util;

import com.simpleplugins.simpleannouncements.SimpleAnnouncements;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.logging.Level;

public class Reflection {
    private static final String CLASS_NOT_FOUND_MESSAGE = "Class: '{CLASS}' not found";
    private static final String COULD_NOT_SEND_PACKET_MESSAGE = "Could not send Packet";

    public static Class<?> getNMSClass(String name) {
        String c = "net.minecraft.server." + Bukkit.getServer().getClass().getPackage()
                .getName().split("\\.")[3] + "." + name;
        try {
            return Class.forName(c);
        } catch (ClassNotFoundException e) {
            SimpleAnnouncements.getInstance().getLogger().log(
                    Level.SEVERE,
                    CLASS_NOT_FOUND_MESSAGE.replace("{CLASS}", c)
            );
            return null;
        }
    }

    public static void sendPacket(Player player, Object packet) {
        try {
            Object handle = player.getClass().getMethod("getHandle").invoke(player);
            Object connection = handle.getClass().getField("playerConnection").get(handle);
            connection.getClass().getMethod("sendPacket", getNMSClass("Packet")).invoke(connection, packet);
        } catch (Exception e) {
            SimpleAnnouncements.getInstance().getLogger().log(Level.SEVERE, COULD_NOT_SEND_PACKET_MESSAGE, e);
        }
    }
}
