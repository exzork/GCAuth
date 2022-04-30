package me.exzork.gcauth.handler;

import emu.grasscutter.Grasscutter;
import emu.grasscutter.database.DatabaseHelper;
import emu.grasscutter.game.Account;
import emu.grasscutter.server.dispatch.json.LoginAccountRequestJson;
import emu.grasscutter.server.dispatch.json.LoginResultJson;
import express.http.HttpContextHandler;
import express.http.Request;
import express.http.Response;
import me.exzork.gcauth.GCAuth;
import me.exzork.gcauth.utils.Authentication;

import java.io.IOException;

public class ClientLoginHandler implements HttpContextHandler {

    @Override
    public void handle(Request request, Response response) throws IOException {
        LoginAccountRequestJson requestData = null;
        try {
            String body = request.ctx().body();
            Grasscutter.getLogger().info("Received login request: " + body);
            requestData = Grasscutter.getGsonFactory().fromJson(body, LoginAccountRequestJson.class);
        } catch (Exception ignored) {
        }

        // Create response json
        if (requestData == null) {
            return;
        }
        LoginResultJson responseData = new LoginResultJson();

        Grasscutter.getLogger()
                .info(String.format("[Dispatch] Client %s is trying to log in", request.ip()));

        // Login
        Account account = null;
        if(GCAuth.getConfig().Enable){
            account = Authentication.getAccountByOneTimeToken(requestData.account);
            if(account == null) {
                Grasscutter.getLogger().info("[Dispatch] Client " + request.ip() + " failed to log in");
                responseData.retcode = -201;
                responseData.message = "Token is invalid";
                response.send(responseData);
            }
        }else{
            account = DatabaseHelper.getAccountByName(requestData.account);
        }

        // Check if account exists, else create a new one.
        if (account == null) {
            // Account doesnt exist, so we can either auto create it if the config value is
            // set
            if (Grasscutter.getConfig().getDispatchOptions().AutomaticallyCreateAccounts) {
                // This account has been created AUTOMATICALLY. There will be no permissions
                // added.
                account = DatabaseHelper.createAccountWithId(requestData.account, 0);

                for (String permission : Grasscutter.getConfig().getDispatchOptions().defaultPermissions) {
                    account.addPermission(permission);
                }

                if (account != null) {
                    responseData.message = "OK";
                    responseData.data.account.uid = account.getId();
                    responseData.data.account.token = account.generateSessionKey();
                    responseData.data.account.email = account.getEmail();

                    Grasscutter.getLogger()
                            .info(String.format("[Dispatch] Client %s failed to log in: Account %s created",
                                    request.ip(), responseData.data.account.uid));
                } else {
                    responseData.retcode = -201;
                    responseData.message = "Username not found, create failed.";

                    Grasscutter.getLogger().info(String.format(
                            "[Dispatch] Client %s failed to log in: Account create failed", request.ip()));
                }
            } else {
                responseData.retcode = -201;
                responseData.message = "Username not found.";

                Grasscutter.getLogger().info(String
                        .format("[Dispatch] Client %s failed to log in: Account no found", request.ip()));
            }
        } else {
            // Account was found, log the player in
            responseData.message = "OK";
            responseData.data.account.uid = account.getId();
            responseData.data.account.token = account.generateSessionKey();
            responseData.data.account.email = account.getEmail();

            Grasscutter.getLogger().info(String.format("[Dispatch] Client %s logged in as %s", request.ip(),
                    responseData.data.account.uid));
        }

        response.send(responseData);
    }
}
