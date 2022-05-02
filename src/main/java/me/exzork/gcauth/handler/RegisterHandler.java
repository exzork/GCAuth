package me.exzork.gcauth.handler;

import com.google.gson.Gson;
import emu.grasscutter.Grasscutter;
import emu.grasscutter.database.DatabaseHelper;
import emu.grasscutter.game.Account;
import express.http.HttpContextHandler;
import express.http.Request;
import express.http.Response;
import me.exzork.gcauth.GCAuth;
import me.exzork.gcauth.json.AuthResponseJson;
import me.exzork.gcauth.json.RegisterAccount;
import me.exzork.gcauth.utils.Authentication;

import java.io.IOException;

public class RegisterHandler implements HttpContextHandler {
    @Override
    public void handle(Request request, Response response) throws IOException {
        AuthResponseJson authResponse = new AuthResponseJson();

        try {
            String requestBody = request.ctx().body();
            if (requestBody.isEmpty()) {
                authResponse.success = false;
                authResponse.message = "EMPTY_BODY"; // ENG = "No data was sent with the request"
                authResponse.jwt = "";
            } else {
                RegisterAccount registerAccount = new Gson().fromJson(requestBody, RegisterAccount.class);
                if (registerAccount.password.equals(registerAccount.password_confirmation)) {
                    String password = Authentication.generateHash(registerAccount.password);
                    Account account = DatabaseHelper.createAccountWithPassword(registerAccount.username, password);
                    if (account == null) {
                        authResponse.success = false;
                        authResponse.message = "USERNAME_TAKEN"; // ENG = "Username has already been taken by another user."
                        authResponse.jwt = "";
                    } else {
                        authResponse.success = true;
                        authResponse.message = "";
                        authResponse.jwt = "";
                    }
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
            Grasscutter.getLogger().error("[Dispatch] An error occurred while creating an account.");
            e.printStackTrace();
        }

        response.send(authResponse);
    }
}
