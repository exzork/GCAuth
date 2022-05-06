package me.exzork.gcauth;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import emu.grasscutter.Grasscutter;
import emu.grasscutter.net.proto.QueryCurrRegionHttpRspOuterClass;
import emu.grasscutter.plugin.Plugin;
import me.exzork.gcauth.handler.*;

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
        loadConfig();
        if(Grasscutter.getDispatchServer().registerAuthHandler(new GCAuthAuthenticationHandler())) {
            Grasscutter.getLogger().info("[GCAuth] GCAuth Enabled!");

            if(Grasscutter.getConfig().getDispatchOptions().AutomaticallyCreateAccounts) {
                Grasscutter.getLogger().warn("[GCAuth] GCAuth does not support automatic account creation. Please disable in the server's config.json or just ignore this warning.");
            }
        } else {
            Grasscutter.getLogger().error("[GCAuth] GCAuth could not be enabled");
        }
    }

    @Override
    public void onDisable() {
        if(Grasscutter.getDispatchServer().getAuthHandler().getClass().equals(GCAuthAuthenticationHandler.class)) {
            Grasscutter.getDispatchServer().resetAuthHandler();
        }
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
    public static Config getConfig() {return config;}
}
