package me.exzork.gcauth;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import emu.grasscutter.Grasscutter;
import emu.grasscutter.database.DatabaseHelper;
import emu.grasscutter.game.Account;
import emu.grasscutter.net.proto.QueryCurrRegionHttpRspOuterClass;
import emu.grasscutter.plugin.Plugin;
import emu.grasscutter.server.dispatch.DispatchHttpJsonHandler;
import emu.grasscutter.server.dispatch.json.ComboTokenReqJson;
import emu.grasscutter.server.dispatch.json.ComboTokenResJson;
import emu.grasscutter.server.dispatch.json.LoginResultJson;
import emu.grasscutter.server.dispatch.json.LoginTokenRequestJson;
import emu.grasscutter.server.event.dispatch.QueryAllRegionsEvent;
import emu.grasscutter.server.event.dispatch.QueryCurrentRegionEvent;
import express.Express;
import me.exzork.gcauth.handler.*;
import org.eclipse.jetty.server.Connector;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.util.ssl.SslContextFactory;

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
                Grasscutter.getLogger().error("[GCAuth] Failed to create config.json");
            }
        }
        loadConfig();
    }

    @Override
    public void onEnable() {
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
            Grasscutter.getLogger().error("[GCAuth] Unable to save config file.");
        }
    }


    public static class RegionData {
        QueryCurrRegionHttpRspOuterClass.QueryCurrRegionHttpRsp parsedRegionQuery;
        String Base64;

        public RegionData(QueryCurrRegionHttpRspOuterClass.QueryCurrRegionHttpRsp prq, String b64) {
            this.parsedRegionQuery = prq;
            this.Base64 = b64;
        }
    }
    public static Config getConfig() {return config;}
}
