package me.exzork.gcauth.handler;

import com.google.gson.Gson;
import emu.grasscutter.Grasscutter;
import emu.grasscutter.game.Account;
import emu.grasscutter.server.http.Router;
import express.Express;
import express.http.Request;
import express.http.Response;
import io.javalin.Javalin;
import me.exzork.gcauth.GCAuth;
import me.exzork.gcauth.json.AuthResponseJson;
import me.exzork.gcauth.json.LoginGenerateToken;
import me.exzork.gcauth.utils.Authentication;


public class LoginHandler implements Router {

    @Override public void applyRoutes(Express express, Javalin handle) {
        express.post("/authentication/login", LoginHandler::handle);
    }

    public static void handle(Request request, Response response) {
        AuthResponseJson authResponse = new AuthResponseJson();


        try {
            String requestBody = request.ctx().body();
            if (requestBody.isEmpty()) {
                authResponse.success = false;
                authResponse.message = "EMPTY_BODY"; // ENG = "No data was sent with the request"
                authResponse.jwt = "";
            } else {
                LoginGenerateToken loginGenerateToken = new Gson().fromJson(requestBody, LoginGenerateToken.class);
                if (!GCAuth.getConfigStatic().ACCESS_KEY.isEmpty() && !GCAuth.getConfigStatic().ACCESS_KEY.equals(loginGenerateToken.access_key)){
                    authResponse.success = false;
                    authResponse.message = "ERROR_ACCESS_KEY"; // ENG = "Error access key was sent with the request"
                    authResponse.jwt = "";
                } else {
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
            }
        } catch (Exception e) {
            authResponse.success = false;
            authResponse.message = "UNKNOWN"; // ENG = "An unknown error has occurred..."
            authResponse.jwt = "";
            Grasscutter.getLogger().error("[Dispatch] An error occurred while a user was logging in.");
            e.printStackTrace();
        }

        response.send(authResponse);
    }
}
