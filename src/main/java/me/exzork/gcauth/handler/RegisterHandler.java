package me.exzork.gcauth.handler;

import com.google.gson.Gson;
import emu.grasscutter.Grasscutter;
import emu.grasscutter.database.DatabaseHelper;
import emu.grasscutter.game.Account;
import emu.grasscutter.server.http.Router;
import express.Express;
import express.http.Request;
import express.http.Response;
import io.javalin.Javalin;
import me.exzork.gcauth.GCAuth;
import me.exzork.gcauth.json.AuthResponseJson;
import me.exzork.gcauth.json.RegisterAccount;
import me.exzork.gcauth.utils.Authentication;


public class RegisterHandler implements Router {

    @Override public void applyRoutes(Express express, Javalin handle) {
        express.post("/authentication/register", RegisterHandler::handle);
    }

    public static void handle(Request request, Response response) {
        AuthResponseJson authResponse = new AuthResponseJson();
        Account account = null;
        try {
            String requestBody = request.ctx().body();
            if (requestBody.isEmpty()) {
                authResponse.success = false;
                authResponse.message = "EMPTY_BODY"; // ENG = "No data was sent with the request"
                authResponse.jwt = "";
            } else {
                RegisterAccount registerAccount = new Gson().fromJson(requestBody, RegisterAccount.class);
                if (!GCAuth.getConfigStatic().ACCESS_KEY.isEmpty() && !GCAuth.getConfigStatic().ACCESS_KEY.equals(registerAccount.access_key)){
                    authResponse.success = false;
                    authResponse.message = "ERROR_ACCESS_KEY"; // ENG = "Error access key was sent with the request"
                    authResponse.jwt = "";
                } else {
                    if (registerAccount.password.equals(registerAccount.password_confirmation)) {
                        if (registerAccount.password.length() >= 8) {
                            String password = Authentication.generateHash(registerAccount.password);
                            try{
                                account = Authentication.getAccountByUsernameAndPassword(registerAccount.username, "");
                                if (account != null) {
                                    account.setPassword(password);
                                    account.save();
                                    authResponse.success = true;
                                    authResponse.message = "";
                                    authResponse.jwt = "";
                                } else {
                                    account = DatabaseHelper.createAccountWithPassword(registerAccount.username, password);
                                    if (account == null) {
                                        authResponse.success = false;
                                        authResponse.message = "USERNAME_TAKEN"; // ENG = "Username has already been taken by another user."
                                        authResponse.jwt = "";
                                    } else {
                                        authResponse.success = true;
                                        authResponse.message = "";
                                        authResponse.jwt = "";
                                    }
                                }
                            }catch (Exception ignored){
                                authResponse.success = false;
                                authResponse.message = "UNKNOWN"; // ENG = "Username has already been taken by another user."
                                authResponse.jwt = "";
                            }
                        } else {
                            authResponse.success = false;
                            authResponse.message = "PASSWORD_INVALID"; // ENG = "Password must be at least 8 characters long"
                            authResponse.jwt = "";
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
            Grasscutter.getLogger().error("[Dispatch] An error occurred while creating an account.");
            e.printStackTrace();
        }
        if (authResponse.success) {
            if (GCAuth.getConfigStatic().defaultPermissions.length > 0) {
                for (String permission : GCAuth.getConfigStatic().defaultPermissions) {
                    account.addPermission(permission);
                }
            }
        }
        response.send(authResponse);
    }
}
