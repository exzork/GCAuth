package me.exzork.gcauth;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import emu.grasscutter.Grasscutter;
import static emu.grasscutter.Configuration.ACCOUNT;
import emu.grasscutter.plugin.Plugin;
import emu.grasscutter.auth.DefaultAuthentication;

import me.exzork.gcauth.utils.Authentication;
import me.exzork.gcauth.auth.GCAuthAuthenticationHandler;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;


public class GCAuth extends Plugin {
    private static Config config;
    private File configFile;
    private static final Gson gson = new GsonBuilder().setPrettyPrinting().create();

    @Override
    public void onEnable() {
        configFile = new File(getDataFolder().toPath()+ "/config.json");
        if (!configFile.exists()) {
            try {
                Files.createDirectories(configFile.toPath().getParent());
            } catch (IOException e) {
                Grasscutter.getLogger().error("[GCAuth] Failed to create config.json");
            }
        }
        try {
            Grasscutter.setAuthenticationSystem(new GCAuthAuthenticationHandler());
            loadConfig();
            Grasscutter.getLogger().info("[GCAuth] GCAuth Enabled!");
            config.jwtSecret = Authentication.generateRandomString(32);
            saveConfig();
            if(ACCOUNT.autoCreate) {
                Grasscutter.getLogger().warn("[GCAuth] GCAuth does not support automatic account creation. Please disable in the server's config.json or just ignore this warning.");
            }
        } catch (Exception e) {
            Grasscutter.getLogger().error("[GCAuth] Failed to enable GCAuth!", e);
        }
    }

    @Override
    public void onDisable() {
        Grasscutter.setAuthenticationSystem(new DefaultAuthentication());
    }

    public  void loadConfig() {
        try (FileReader file = new FileReader(configFile)) {
            config = gson.fromJson(file,Config.class);
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
            Grasscutter.getLogger().error("[GCAuth] Unable to save config file.");
        }
    }
    public static Config getConfigStatic() {return config;}
    public Config getConfig() {return config;}
}
