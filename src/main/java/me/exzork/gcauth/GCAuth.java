package me.exzork.gcauth;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import emu.grasscutter.Grasscutter;
import emu.grasscutter.auth.DefaultAuthentication;
import emu.grasscutter.plugin.Plugin;

import me.exzork.gcauth.handler.*;
import me.exzork.gcauth.utils.Authentication;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;

import static emu.grasscutter.config.Configuration.ACCOUNT;


public class GCAuth extends Plugin {
    private static Config config;
    private File configFile;
    private static final Gson gson = new GsonBuilder().setPrettyPrinting().create();
    public static GCAuth getInstance() {
        return (GCAuth) Grasscutter.getPluginManager().getPlugin("GCAuth");
    }

    @Override
    public void onEnable() {
        configFile = new File(getDataFolder().toPath() + "/config.json");
        if (!configFile.exists()) {
            try {
                Files.createDirectories(configFile.toPath().getParent());
            } catch (IOException e) {
                getLogger().error("Failed to create config.json");
            }
        }
        loadConfig();
        Grasscutter.setAuthenticationSystem(new GCAuthAuthenticationHandler());
        getLogger().info("GCAuth Enabled!");
        config.jwtSecret = Authentication.generateRandomString(32);
        saveConfig();
        if (ACCOUNT.autoCreate) {
            getLogger().warn("GCAuth does not support automatic account creation. Please disable in the server's config.json or just ignore this warning.");
        }
    }

    @Override
    public void onDisable() {
        Grasscutter.setAuthenticationSystem(new DefaultAuthentication());
    }

    public void loadConfig() {
        try (FileReader file = new FileReader(configFile)) {
            config = gson.fromJson(file, Config.class);
            saveConfig();
        } catch (Exception e) {
            config = new Config();
            saveConfig();
        }
    }

    public void saveConfig() {
        try (FileWriter file = new FileWriter(configFile)) {
            file.write(gson.toJson(config));
        } catch (Exception e) {
            getLogger().error("Unable to save config file.");
        }
    }

    public Config getConfig() {
        return config;
    }
}
