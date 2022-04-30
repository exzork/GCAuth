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
                Grasscutter.getLogger().error("Failed to create config.json for GCAuth");
            }
        }
        loadConfig();
    }

    @Override
    public void onEnable() {

        Express httpServer = new Express(config -> {
            config.server(() -> {
                Server server = new Server();
                ServerConnector serverConnector;

                if(Grasscutter.getConfig().getDispatchOptions().UseSSL) {
                    SslContextFactory.Server sslContextFactory = new SslContextFactory.Server();
                    File keystoreFile = new File(Grasscutter.getConfig().getDispatchOptions().KeystorePath);

                    if(keystoreFile.exists()) {
                        try {
                            sslContextFactory.setKeyStorePath(keystoreFile.getPath());
                            sslContextFactory.setKeyStorePassword(Grasscutter.getConfig().getDispatchOptions().KeystorePassword);
                        } catch (Exception e) {
                            e.printStackTrace();
                            Grasscutter.getLogger().warn("[Dispatch] Unable to load keystore. Trying default keystore password...");

                            try {
                                sslContextFactory.setKeyStorePath(keystoreFile.getPath());
                                sslContextFactory.setKeyStorePassword("123456");
                                Grasscutter.getLogger().warn("[Dispatch] The default keystore password was loaded successfully. Please consider setting the password to 123456 in config.json.");
                            } catch (Exception e2) {
                                Grasscutter.getLogger().warn("[Dispatch] Error while loading keystore!");
                                e2.printStackTrace();
                            }
                        }

                        serverConnector = new ServerConnector(server, sslContextFactory);
                    } else {
                        Grasscutter.getLogger().warn("[Dispatch] No SSL cert found! Falling back to HTTP server.");
                        Grasscutter.getConfig().getDispatchOptions().UseSSL = false;

                        serverConnector = new ServerConnector(server);
                    }
                } else {
                    serverConnector = new ServerConnector(server);
                }

                serverConnector.setPort(Grasscutter.getConfig().getDispatchOptions().Port);
                server.setConnectors(new Connector[]{serverConnector});
                return server;
            });

            config.enforceSsl = Grasscutter.getConfig().getDispatchOptions().UseSSL;
            if(Grasscutter.getConfig().DebugMode.equalsIgnoreCase("ALL")) {
                config.enableDevLogging();
            }
        });

        httpServer.get("/grasscutter/auth_status",new AuthStatusHandler());
        httpServer.post("/grasscutter/login", new LoginHandler());
        httpServer.post("/grasscutter/register", new RegisterHandler());
        httpServer.post("/grasscutter/change_password", new ChangePasswordHandler());
        httpServer.post("/hk4e_global/mdk/shield/api/login", new ClientLoginHandler());

        httpServer.get("/", (req, res) -> res.send("Welcome to Grasscutter"));

        httpServer.raw().error(404, ctx -> {
            if(Grasscutter.getConfig().DebugMode.equalsIgnoreCase("MISSING")) {
                Grasscutter.getLogger().info(String.format("[Dispatch] Potential unhandled %s request: %s", ctx.method(), ctx.url()));
            }
            ctx.contentType("text/html");
            ctx.result("<!doctype html><html lang=\"en\"><body><img src=\"https://http.cat/404\" /></body></html>"); // I'm like 70% sure this won't break anything.
        });

        // Dispatch
        httpServer.get("/query_region_list", (req, res) -> {
            // Log
            Grasscutter.getLogger().info(String.format("[Dispatch] Client %s request: query_region_list", req.ip()));

            // Invoke event.
            QueryAllRegionsEvent event = new QueryAllRegionsEvent(Grasscutter.getDispatchServer().regionListBase64); event.call();
            // Respond with event result.
            res.send(event.getRegionList());
        });

        httpServer.get("/query_cur_region/:id", (req, res) -> {
            String regionName = req.params("id");
            // Log
            Grasscutter.getLogger().info(
                    String.format("Client %s request: query_cur_region/%s", req.ip(), regionName));
            // Create a response form the request query parameters
            String response = "CAESGE5vdCBGb3VuZCB2ZXJzaW9uIGNvbmZpZw==";
            if (req.query().values().size() > 0) {
                response = Grasscutter.getDispatchServer().regions.get(regionName).getBase64();
            }

            // Invoke event.
            QueryCurrentRegionEvent event = new QueryCurrentRegionEvent(response); event.call();
            // Respond with event result.
            res.send(event.getRegionInfo());
        });


        // Login via token
        httpServer.post("/hk4e_global/mdk/shield/api/verify", (req, res) -> {
            // Get post data
            LoginTokenRequestJson requestData = null;
            try {
                String body = req.ctx().body();
                requestData = Grasscutter.getGsonFactory().fromJson(body, LoginTokenRequestJson.class);
            } catch (Exception ignored) {
            }

            // Create response json
            if (requestData == null) {
                return;
            }
            LoginResultJson responseData = new LoginResultJson();
            Grasscutter.getLogger().info(String.format("[Dispatch] Client %s is trying to log in via token", req.ip()));

            // Login
            Account account = DatabaseHelper.getAccountById(requestData.uid);

            // Test
            if (account == null || !account.getSessionKey().equals(requestData.token)) {
                responseData.retcode = -111;
                responseData.message = "Game account cache information error";

                Grasscutter.getLogger()
                        .info(String.format("[Dispatch] Client %s failed to log in via token", req.ip()));
            } else {
                responseData.message = "OK";
                responseData.data.account.uid = requestData.uid;
                responseData.data.account.token = requestData.token;
                responseData.data.account.email = account.getEmail();

                Grasscutter.getLogger().info(String.format("[Dispatch] Client %s logged in via token as %s",
                        req.ip(), responseData.data.account.uid));
            }

            res.send(responseData);
        });

        // Exchange for combo token
        httpServer.post("/hk4e_global/combo/granter/login/v2/login", (req, res) -> {
            // Get post data
            ComboTokenReqJson requestData = null;
            try {
                String body = req.ctx().body();
                requestData = Grasscutter.getGsonFactory().fromJson(body, ComboTokenReqJson.class);
            } catch (Exception ignored) {
            }

            // Create response json
            if (requestData == null || requestData.data == null) {
                return;
            }
            ComboTokenReqJson.LoginTokenData loginData = Grasscutter.getGsonFactory().fromJson(requestData.data, ComboTokenReqJson.LoginTokenData.class); // Get login
            // data
            ComboTokenResJson responseData = new ComboTokenResJson();

            // Login
            Account account = DatabaseHelper.getAccountById(loginData.uid);

            // Test
            if (account == null || !account.getSessionKey().equals(loginData.token)) {
                responseData.retcode = -201;
                responseData.message = "Wrong session key.";

                Grasscutter.getLogger().info(
                        String.format("[Dispatch] Client %s failed to exchange combo token", req.ip()));
            } else {
                responseData.message = "OK";
                responseData.data.open_id = loginData.uid;
                responseData.data.combo_id = "157795300";
                responseData.data.combo_token = account.generateLoginToken();

                Grasscutter.getLogger().info(
                        String.format("[Dispatch] Client %s succeed to exchange combo token", req.ip()));
            }

            res.send(responseData);
        });

        // TODO: There are some missing route request types here (You can tell if they are missing if they are .all and not anything else)
        //  When http requests for theses routes are found please remove it from the list in DispatchHttpJsonHandler and update the route request types here

        // Agreement and Protocol
        // hk4e-sdk-os.hoyoverse.com
        httpServer.get("/hk4e_global/mdk/agreement/api/getAgreementInfos", new DispatchHttpJsonHandler("{\"retcode\":0,\"message\":\"OK\",\"data\":{\"marketing_agreements\":[]}}"));
        // hk4e-sdk-os.hoyoverse.com
        httpServer.post("/hk4e_global/combo/granter/api/compareProtocolVersion", new DispatchHttpJsonHandler("{\"retcode\":0,\"message\":\"OK\",\"data\":{\"modified\":true,\"protocol\":{\"id\":0,\"app_id\":4,\"language\":\"en\",\"user_proto\":\"\",\"priv_proto\":\"\",\"major\":7,\"minimum\":0,\"create_time\":\"0\",\"teenager_proto\":\"\",\"third_proto\":\"\"}}}"));

        // Game data
        // hk4e-api-os.hoyoverse.com
        httpServer.all("/common/hk4e_global/announcement/api/getAlertPic", new DispatchHttpJsonHandler("{\"retcode\":0,\"message\":\"OK\",\"data\":{\"total\":0,\"list\":[]}}"));
        // hk4e-api-os.hoyoverse.com
        httpServer.all("/common/hk4e_global/announcement/api/getAlertAnn", new DispatchHttpJsonHandler("{\"retcode\":0,\"message\":\"OK\",\"data\":{\"alert\":false,\"alert_id\":0,\"remind\":true}}"));
        // hk4e-api-os.hoyoverse.com
        httpServer.all("/common/hk4e_global/announcement/api/getAnnList", new DispatchHttpJsonHandler("{\"retcode\":0,\"message\":\"OK\",\"data\":{\"list\":[],\"total\":0,\"type_list\":[],\"alert\":false,\"alert_id\":0,\"timezone\":0,\"t\":\"" + System.currentTimeMillis() + "\"}}"));
        // hk4e-api-os-static.hoyoverse.com
        httpServer.all("/common/hk4e_global/announcement/api/getAnnContent", new DispatchHttpJsonHandler("{\"retcode\":0,\"message\":\"OK\",\"data\":{\"list\":[],\"total\":0}}"));
        // hk4e-sdk-os.hoyoverse.com
        httpServer.all("/hk4e_global/mdk/shopwindow/shopwindow/listPriceTier", new DispatchHttpJsonHandler("{\"retcode\":0,\"message\":\"OK\",\"data\":{\"suggest_currency\":\"USD\",\"tiers\":[]}}"));

        // Captcha
        // api-account-os.hoyoverse.com
        httpServer.post("/account/risky/api/check", new DispatchHttpJsonHandler("{\"retcode\":0,\"message\":\"OK\",\"data\":{\"id\":\"none\",\"action\":\"ACTION_NONE\",\"geetest\":null}}"));

        // Config
        // sdk-os-static.hoyoverse.com
        httpServer.get("/combo/box/api/config/sdk/combo", new DispatchHttpJsonHandler("{\"retcode\":0,\"message\":\"OK\",\"data\":{\"vals\":{\"disable_email_bind_skip\":\"false\",\"email_bind_remind_interval\":\"7\",\"email_bind_remind\":\"true\"}}}"));
        // hk4e-sdk-os-static.hoyoverse.com
        httpServer.get("/hk4e_global/combo/granter/api/getConfig", new DispatchHttpJsonHandler("{\"retcode\":0,\"message\":\"OK\",\"data\":{\"protocol\":true,\"qr_enabled\":false,\"log_level\":\"INFO\",\"announce_url\":\"https://webstatic-sea.hoyoverse.com/hk4e/announcement/index.html?sdk_presentation_style=fullscreen\\u0026sdk_screen_transparent=true\\u0026game_biz=hk4e_global\\u0026auth_appid=announcement\\u0026game=hk4e#/\",\"push_alias_type\":2,\"disable_ysdk_guard\":false,\"enable_announce_pic_popup\":true}}"));
        // hk4e-sdk-os-static.hoyoverse.com
        httpServer.get("/hk4e_global/mdk/shield/api/loadConfig", new DispatchHttpJsonHandler("{\"retcode\":0,\"message\":\"OK\",\"data\":{\"id\":6,\"game_key\":\"hk4e_global\",\"client\":\"PC\",\"identity\":\"I_IDENTITY\",\"guest\":false,\"ignore_versions\":\"\",\"scene\":\"S_NORMAL\",\"name\":\"原神海外\",\"disable_regist\":false,\"enable_email_captcha\":false,\"thirdparty\":[\"fb\",\"tw\"],\"disable_mmt\":false,\"server_guest\":false,\"thirdparty_ignore\":{\"tw\":\"\",\"fb\":\"\"},\"enable_ps_bind_account\":false,\"thirdparty_login_configs\":{\"tw\":{\"token_type\":\"TK_GAME_TOKEN\",\"game_token_expires_in\":604800},\"fb\":{\"token_type\":\"TK_GAME_TOKEN\",\"game_token_expires_in\":604800}}}}"));
        // Test api?
        // abtest-api-data-sg.hoyoverse.com
        httpServer.post("/data_abtest_api/config/experiment/list", new DispatchHttpJsonHandler("{\"retcode\":0,\"success\":true,\"message\":\"\",\"data\":[{\"code\":1000,\"type\":2,\"config_id\":\"14\",\"period_id\":\"6036_99\",\"version\":\"1\",\"configs\":{\"cardType\":\"old\"}}]}"));

        // log-upload-os.mihoyo.com
        httpServer.all("/log/sdk/upload", new DispatchHttpJsonHandler("{\"code\":0}"));
        httpServer.all("/sdk/upload", new DispatchHttpJsonHandler("{\"code\":0}"));
        httpServer.post("/sdk/dataUpload", new DispatchHttpJsonHandler("{\"code\":0}"));
        // /perf/config/verify?device_id=xxx&platform=x&name=xxx
        httpServer.all("/perf/config/verify", new DispatchHttpJsonHandler("{\"code\":0}"));

        // Logging servers
        // overseauspider.yuanshen.com
        httpServer.all("/log", new DispatchHttpJsonHandler("{\"code\":0}"));
        // log-upload-os.mihoyo.com
        httpServer.all("/crash/dataUpload", new DispatchHttpJsonHandler("{\"code\":0}"));

        httpServer.get("/gacha", (req, res) -> res.send("<!doctype html><html lang=\"en\"><head><title>Gacha</title></head><body></body></html>"));

        Grasscutter.getDispatchServer().setHttpServer(httpServer);
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
