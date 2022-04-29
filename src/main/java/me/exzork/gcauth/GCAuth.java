package me.exzork.gcauth;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.sun.net.httpserver.HttpServer;
import emu.grasscutter.Grasscutter;
import emu.grasscutter.plugin.Plugin;
import me.exzork.gcauth.handler.*;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;

public class GCAuth extends Plugin {
    private static Config config;
    private static final File configFile = new File(Grasscutter.getConfig().PLUGINS_FOLDER+"GCAuth/config.json");
    private static final Gson gson = new GsonBuilder().setPrettyPrinting().create();
    @Override
    public void onLoad() {
        if (!configFile.exists()) {
            try {
                Files.createDirectories(configFile.toPath().getParent());
            } catch (IOException e) {
                Grasscutter.getLogger().error("Failed to create config.json for GCAuth");
            }
        }
        loadConfig();
    }

    @Override
    public void onEnable() {
        HttpServer server = Grasscutter.getDispatchServer().getServer();
        server.createContext("/grasscutter/auth_status",new AuthStatusHandler());
        server.createContext("/grasscutter/login", new LoginHandler());
        server.createContext("/grasscutter/register", new RegisterHandler());
        server.createContext("/grasscutter/change_password", new ChangePasswordHandler());
        server.removeContext("/hk4e_global/mdk/shield/api/login");
        server.createContext("/hk4e_global/mdk/shield/api/login", new ClientLoginHandler());
    }

    @Override
    public void onDisable() {
        super.onDisable();
    }

    public static void loadConfig() {
        try (FileReader file = new FileReader(configFile)) {
            config = gson.fromJson(file,Config.class);
            saveConfig();
        } catch (Exception e) {
            config = new Config();
            saveConfig();
        }
    }

    public static void saveConfig() {
        try (FileWriter file = new FileWriter(configFile)) {
            file.write(gson.toJson(config));
        } catch (Exception e) {
            Grasscutter.getLogger().error("Unable to save config file.");
        }
    }

    public static Config getConfig() {return config;}
}
