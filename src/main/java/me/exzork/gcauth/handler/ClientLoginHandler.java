package me.exzork.gcauth.handler;

import com.sun.net.httpserver.HttpExchange;
import emu.grasscutter.Grasscutter;
import emu.grasscutter.database.DatabaseHelper;
import emu.grasscutter.game.Account;
import emu.grasscutter.server.dispatch.json.LoginAccountRequestJson;
import emu.grasscutter.server.dispatch.json.LoginResultJson;
import emu.grasscutter.utils.Utils;
import me.exzork.gcauth.GCAuth;
import me.exzork.gcauth.utils.Authentication;

import java.io.IOException;

public class ClientLoginHandler extends AbstractHandler{

    @Override
    public void handle(HttpExchange t) throws IOException {
        LoginAccountRequestJson requestData = null;
        try {
            String body = Utils.toString(t.getRequestBody());
            requestData = Grasscutter.getGsonFactory().fromJson(body, LoginAccountRequestJson.class);
        } catch (Exception ignored) {
        }

        // Create response json
        if (requestData == null) {
            return;
        }
        LoginResultJson responseData = new LoginResultJson();

        Grasscutter.getLogger()
                .info(String.format("[Dispatch] Client %s is trying to log in", t.getRemoteAddress()));

        // Login
        Account account = null;
        if(GCAuth.getConfig().Enable){
            account = Authentication.getAccountByOneTimeToken(requestData.account);
            if(account == null) {
                responseData.retcode = -201;
                responseData.message = "Token is invalid";
                responseJSON(t, responseData);
            }
            ;
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

                if (account != null) {
                    responseData.message = "OK";
                    responseData.data.account.uid = account.getId();
                    responseData.data.account.token = account.generateSessionKey();
                    responseData.data.account.email = account.getEmail();

                    Grasscutter.getLogger()
                            .info(String.format("[Dispatch] Client %s failed to log in: Account %s created",
                                    t.getRemoteAddress(), responseData.data.account.uid));
                } else {
                    responseData.retcode = -201;
                    responseData.message = "Username not found, create failed.";

                    Grasscutter.getLogger().info(String.format(
                            "[Dispatch] Client %s failed to log in: Account create failed", t.getRemoteAddress()));
                }
            } else {
                responseData.retcode = -201;
                responseData.message = "Username not found.";

                Grasscutter.getLogger().info(String
                        .format("[Dispatch] Client %s failed to log in: Account no found", t.getRemoteAddress()));
            }
        } else {
            // Account was found, log the player in
            responseData.message = "OK";
            responseData.data.account.uid = account.getId();
            responseData.data.account.token = account.generateSessionKey();
            responseData.data.account.email = account.getEmail();

            Grasscutter.getLogger().info(String.format("[Dispatch] Client %s logged in as %s", t.getRemoteAddress(),
                    responseData.data.account.uid));
        }

        responseJSON(t, responseData);
    }
}
