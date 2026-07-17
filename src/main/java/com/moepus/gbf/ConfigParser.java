package com.moepus.gbf;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonIOException;
import com.google.gson.JsonSyntaxException;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.function.Supplier;

public final class ConfigParser {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static Config config;
    static Supplier<Path> configPath = () -> Path.of("config", "gbf.json");

    private ConfigParser() {}

    public static Config getConfig() {
        if (config == null) {
            loadConfig();
        }

        return config;
    }

    public static void loadConfig() {
        Path path = configPath.get();

        if (!Files.exists(path)) {
            config = new Config();
            saveConfig();
            return;
        }

        try (Reader reader = Files.newBufferedReader(path)) {
            config = GSON.fromJson(reader, Config.class);
            if (config == null) {
                config = new Config();
            }
            saveConfig();
        } catch (JsonIOException | JsonSyntaxException | IOException exception) {
            reportFailure("load", path, exception);
            config = new Config();
        }
    }

    public static void saveConfig() {
        Path path = configPath.get();

        try {
            Path parent = path.getParent();
            if (parent != null) {
                Files.createDirectories(parent);
            }
            try (Writer writer = Files.newBufferedWriter(path)) {
                GSON.toJson(config, writer);
            }
        } catch (IOException exception) {
            reportFailure("save", path, exception);
        }
    }

    private static void reportFailure(String operation, Path path, Exception exception) {
        System.err.println("[GeckoBetterFPS] Failed to " + operation + " config " + path);
        exception.printStackTrace();
    }
}
