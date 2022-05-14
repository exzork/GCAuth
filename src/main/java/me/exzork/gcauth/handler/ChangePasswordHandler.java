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
import me.exzork.gcauth.json.ChangePasswordAccount;
import me.exzork.gcauth.utils.Authentication;


public class ChangePasswordHandler implements Router {

    @Override public void applyRoutes(Express express, Javalin handle) {
        express.post("/authentication/change_password", ChangePasswordHandler::handle);
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
                ChangePasswordAccount changePasswordAccount = new Gson().fromJson(requestBody, ChangePasswordAccount.class);
                if (!GCAuth.getConfigStatic().ACCESS_KEY.isEmpty() && !GCAuth.getConfigStatic().ACCESS_KEY.equals(changePasswordAccount.access_key)){
                    authResponse.success = false;
                    authResponse.message = "ERROR_ACCESS_KEY"; // ENG = "Error access key was sent with the request"
                    authResponse.jwt = "";
                } else {
                    if (changePasswordAccount.new_password.equals(changePasswordAccount.new_password_confirmation)) {
                        Account account = Authentication.getAccountByUsernameAndPassword(changePasswordAccount.username, changePasswordAccount.old_password);
                        if (account == null) {
                            authResponse.success = false;
                            authResponse.message = "INVALID_ACCOUNT"; // ENG = "Invalid username or password"
                            authResponse.jwt = "";
                        } else {
                            if (changePasswordAccount.new_password.length() >= 8) {
                                String newPassword = Authentication.generateHash(changePasswordAccount.new_password);
                                account.setPassword(newPassword);
                                account.save();
                                authResponse.success = true;
                                authResponse.message = "";
                                authResponse.jwt = "";
                            } else {
                                authResponse.success = false;
                                authResponse.message = "PASSWORD_INVALID"; // ENG = "Password must be at least 8 characters long"
                                authResponse.jwt = "";
                            }
                        }
                    } else {
                        authResponse.success = false;
                        authResponse.message = "PASSWORD_MISMATCH"; // ENG = "Passwords do not match."
                        authResponse.jwt = "";
                    }
                }
            }
        } catch (Exception e) {
            authResponse.success = false;
            authResponse.message = "UNKNOWN"; // ENG = "An unknown error has occurred..."
            authResponse.jwt = "";
            Grasscutter.getLogger().error("[Dispatch] Error while changing user password.");
            e.printStackTrace();
        }

        response.send(authResponse);
    }
}
