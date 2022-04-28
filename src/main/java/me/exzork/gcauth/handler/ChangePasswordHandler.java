package me.exzork.gcauth.handler;

import com.google.gson.Gson;
import com.sun.net.httpserver.HttpExchange;
import emu.grasscutter.Grasscutter;
import emu.grasscutter.game.Account;
import emu.grasscutter.utils.Utils;
import me.exzork.gcauth.GCAuth;
import me.exzork.gcauth.json.AuthResponseJson;
import me.exzork.gcauth.json.ChangePasswordAccount;
import me.exzork.gcauth.utils.Authentication;

import java.io.IOException;

public class ChangePasswordHandler extends AbstractHandler{
    @Override
    public void handle(HttpExchange t) throws IOException {
        AuthResponseJson authResponse = new AuthResponseJson();

        if (GCAuth.getConfig().Enable) {
            try {
                String requestBody = Utils.toString(t.getRequestBody());
                if (requestBody.isEmpty()) {
                    authResponse.success = false;
                    authResponse.message = "EMPTY_BODY"; // ENG = "No data was sent with the request"
                    authResponse.jwt = "";
                } else {
                    ChangePasswordAccount changePasswordAccount = new Gson().fromJson(requestBody, ChangePasswordAccount.class);
                    if (changePasswordAccount.new_password.equals(changePasswordAccount.new_password_confirmation)) {
                        Account account = Authentication.getAccountByUsernameAndPassword(changePasswordAccount.username, changePasswordAccount.old_password);
                        if (account == null) {
                            authResponse.success = false;
                            authResponse.message = "INVALID_ACCOUNT"; // ENG = "Invalid username or password"
                            authResponse.jwt = "";
                        }
                        String newPassword = Authentication.generateHash(changePasswordAccount.new_password);
                        account.setPassword(newPassword);
                        account.save();
                        authResponse.success = true;
                        authResponse.message = "";
                        authResponse.jwt = "";
                    } else {
                        authResponse.success = false;
                        authResponse.message = "PASSWORD_MISMATCH"; // ENG = "Passwords do not match."
                        authResponse.jwt = "";
                    }
                }
            } catch (Exception e) {
                authResponse.success = false;
                authResponse.message = "UNKNOWN"; // ENG = "An unknown error has occurred..."
                authResponse.jwt = "";
                Grasscutter.getLogger().error("[Dispatch] Error while changing user password.");
                e.printStackTrace();
                responseJSON(t, authResponse);
            }
        } else {
            authResponse.success = false;
            authResponse.message = "AUTH_DISABLED"; // ENG = "Authentication is not required for this server..."
            authResponse.jwt = "";
        }

        responseJSON(t, authResponse);
    }
}
