package me.exzork.gcauth.auth;

import emu.grasscutter.auth.*;
import emu.grasscutter.server.http.objects.ComboTokenResJson;
import emu.grasscutter.server.http.objects.LoginResultJson;

import me.exzork.gcauth.auth.GCAuthAuthenticators.*;


public final class GCAuthAuthenticationHandler implements AuthenticationSystem {
    private final Authenticator<LoginResultJson> passwordAuthenticator = new PasswordAuthenticator();
    private final Authenticator<LoginResultJson> tokenAuthenticator = new DefaultAuthenticators.TokenAuthenticator();
    private final Authenticator<ComboTokenResJson> sessionKeyAuthenticator = new DefaultAuthenticators.SessionKeyAuthenticator();
    private final ExternalAuthenticator externalAuthenticator = new ExternalAuthentication();

    @Override
    public void createAccount(String username, String password) {
        // Unhandled.
    }

    @Override
    public void resetPassword(String username) {
        // Unhandled.
    }

    @Override
    public boolean verifyUser(String s) {
        return false;
    }

    @Override
    public Authenticator<LoginResultJson> getPasswordAuthenticator() {
        return this.passwordAuthenticator;
    }

    @Override
    public Authenticator<LoginResultJson> getTokenAuthenticator() {
        return this.tokenAuthenticator;
    }

    @Override
    public Authenticator<ComboTokenResJson> getSessionKeyAuthenticator() {
        return this.sessionKeyAuthenticator;
    }

    @Override
    public ExternalAuthenticator getExternalAuthenticator() {
        return this.externalAuthenticator;
    }
}
