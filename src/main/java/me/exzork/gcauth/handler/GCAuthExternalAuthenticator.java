package me.exzork.gcauth.handler;

import com.google.gson.Gson;
import com.mchange.v1.util.ArrayUtils;
import emu.grasscutter.Grasscutter;
import emu.grasscutter.auth.AuthenticationSystem;
import emu.grasscutter.auth.ExternalAuthenticator;
import emu.grasscutter.database.DatabaseHelper;
import emu.grasscutter.game.Account;
import express.http.Response;
import io.javalin.http.Context;
import io.javalin.http.util.RateLimit;
import me.exzork.gcauth.GCAuth;
import me.exzork.gcauth.json.AuthResponseJson;
import me.exzork.gcauth.json.ChangePasswordAccount;
import me.exzork.gcauth.json.LoginGenerateToken;
import me.exzork.gcauth.json.RegisterAccount;
import me.exzork.gcauth.utils.Authentication;

import java.util.Arrays;
import java.util.concurrent.TimeUnit;

public class GCAuthExternalAuthenticator implements ExternalAuthenticator {
    private String[] endPoints = GCAuth.getInstance().getConfig().rateLimit.endPoints;
    private int maxRequests = GCAuth.getInstance().getConfig().rateLimit.maxRequests;
    private String timeUnit = GCAuth.getInstance().getConfig().rateLimit.timeUnit;
    @Override
    public void handleLogin(AuthenticationSystem.AuthenticationRequest authenticationRequest) {
        AuthResponseJson authResponse = new AuthResponseJson();
        Response response = authenticationRequest.getResponse();
        assert response != null; // This should never be null.
        if (Arrays.asList(endPoints).contains("login"))
            new RateLimit(response.ctx()).requestPerTimeUnit(maxRequests, Authentication.getTimeUnit(timeUnit));
        try {
            String requestBody = response.ctx().body();
            if (requestBody.isEmpty()) {
                authResponse.success = false;
                authResponse.message = "EMPTY_BODY"; // ENG = "No data was sent with the request"
                authResponse.jwt = "";
            } else {
                LoginGenerateToken loginGenerateToken = new Gson().fromJson(requestBody, LoginGenerateToken.class);
                if (!GCAuth.getInstance().getConfig().accessKey.isEmpty() && !GCAuth.getInstance().getConfig().accessKey.equals(loginGenerateToken.access_key)){
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
            GCAuth.getInstance().getLogger().error("[Dispatch] An error occurred while a user was logging in.");
            e.printStackTrace();
        }
        response.send(authResponse);
    }

    @Override
    public void handleAccountCreation(AuthenticationSystem.AuthenticationRequest authenticationRequest) {
        AuthResponseJson authResponse = new AuthResponseJson();
        Response response = authenticationRequest.getResponse();
        assert response != null; // This should never be null.
        if (Arrays.asList(endPoints).contains("register"))
            new RateLimit(response.ctx()).requestPerTimeUnit(maxRequests, Authentication.getTimeUnit(timeUnit));
        Account account = null;
        try {
            String requestBody = response.ctx().body();
            if (requestBody.isEmpty()) {
                authResponse.success = false;
                authResponse.message = "EMPTY_BODY"; // ENG = "No data was sent with the request"
                authResponse.jwt = "";
            } else {
                RegisterAccount registerAccount = new Gson().fromJson(requestBody, RegisterAccount.class);
                if (!GCAuth.getInstance().getConfig().accessKey.isEmpty() && !GCAuth.getInstance().getConfig().accessKey.equals(registerAccount.access_key)){
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
            GCAuth.getInstance().getLogger().error("[Dispatch] An error occurred while creating an account.");
            e.printStackTrace();
        }
        if (authResponse.success) {
            if (GCAuth.getInstance().getConfig().defaultPermissions.length > 0) {
                for (String permission : GCAuth.getInstance().getConfig().defaultPermissions) {
                    account.addPermission(permission);
                }
                account.save();
            }
        }
        response.send(authResponse);
    }

    @Override
    public void handlePasswordReset(AuthenticationSystem.AuthenticationRequest authenticationRequest) {
        AuthResponseJson authResponse = new AuthResponseJson();
        Response response = authenticationRequest.getResponse();
        assert response != null; // This should never be null.
        if (Arrays.asList(endPoints).contains("change_password"))
            new RateLimit(response.ctx()).requestPerTimeUnit(maxRequests, Authentication.getTimeUnit(timeUnit));
        try {
            String requestBody = response.ctx().body();
            if (requestBody.isEmpty()) {
                authResponse.success = false;
                authResponse.message = "EMPTY_BODY"; // ENG = "No data was sent with the request"
                authResponse.jwt = "";
            } else {
                ChangePasswordAccount changePasswordAccount = new Gson().fromJson(requestBody, ChangePasswordAccount.class);
                if (!GCAuth.getInstance().getConfig().accessKey.isEmpty() && !GCAuth.getInstance().getConfig().accessKey.equals(changePasswordAccount.access_key)){
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
                            } else {
                                authResponse.success = false;
                                authResponse.message = "PASSWORD_INVALID"; // ENG = "Password must be at least 8 characters long"
                            }
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
            GCAuth.getInstance().getLogger().error("[Dispatch] Error while changing user password.");
            e.printStackTrace();
        }
        response.send(authResponse);
    }
}
