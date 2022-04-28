package me.exzork.gcauth.handler;

import com.google.gson.Gson;
import com.sun.net.httpserver.HttpExchange;
import emu.grasscutter.Grasscutter;
import emu.grasscutter.game.Account;
import emu.grasscutter.utils.Utils;
import me.exzork.gcauth.GCAuth;
import me.exzork.gcauth.json.AuthResponseJson;
import me.exzork.gcauth.json.LoginGenerateToken;
import me.exzork.gcauth.utils.Authentication;

import java.io.IOException;

public class LoginHandler extends AbstractHandler{
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
                    LoginGenerateToken loginGenerateToken = new Gson().fromJson(requestBody, LoginGenerateToken.class);
                    Account account = Authentication.getAccountByUsernameAndPassword(loginGenerateToken.username, loginGenerateToken.password);
                    if (account == null) {
                        authResponse.success = false;
                        authResponse.message = "INVALID_ACCOUNT"; // ENG = "Invalid username or password"
                        authResponse.jwt = "";
                    } else {
                        if (account.getPassword() != null && !account.getPassword().isEmpty()) {
                            authResponse.success = true;
                            authResponse.message = "";
                            authResponse.jwt = Authentication.generateJwt(account);
                        } else {
                            authResponse.success = false;
                            authResponse.message = "NO_PASSWORD"; // ENG = "There is no account password set. Please create a password by resetting it."
                            authResponse.jwt = "";
                        }
                    }
                }
            } catch (Exception e) {
                authResponse.success = false;
                authResponse.message = "UNKNOWN"; // ENG = "An unknown error has occurred..."
                authResponse.jwt = "";
                Grasscutter.getLogger().error("[Dispatch] An error occurred while a user was logging in.");
                e.printStackTrace();
            }
        } else {
            authResponse.success = false;
            authResponse.message = "AUTH_DISABLED"; // ENG = "Authentication is not required for this server..."
            authResponse.jwt = "";
        }

        responseJSON(t, authResponse);
    }
}
