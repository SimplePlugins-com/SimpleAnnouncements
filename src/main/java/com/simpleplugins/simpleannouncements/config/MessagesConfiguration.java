package com.simpleplugins.simpleannouncements.config;

import com.google.gson.Gson;
import com.simpleplugins.simpleannouncements.SimpleAnnouncements;
import lombok.Getter;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

public class MessagesConfiguration {
    @Getter
    private final Map<String, Object> values = new HashMap<>();
    private final Path path;

    public MessagesConfiguration(File dataFolder) {
        this.path = dataFolder.toPath().resolve("messages.json");
    }

    public void reload() throws IOException {
        if (Files.notExists(path)) {
            SimpleAnnouncements.getInstance().saveResource(path.getFileName().toString(), false);
            Files.move(path.getParent().resolve(path.getFileName()), path);
        }

        try (BufferedReader bufferedReader = Files.newBufferedReader(path)) {
            values.putAll(new Gson().fromJson(bufferedReader, values.getClass()));
        }
    }
}