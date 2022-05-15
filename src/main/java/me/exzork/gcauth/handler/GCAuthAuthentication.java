package me.exzork.gcauth.handler;

import emu.grasscutter.auth.*;
import emu.grasscutter.server.http.objects.ComboTokenResJson;
import emu.grasscutter.server.http.objects.LoginResultJson;

public class GCAuthAuthentication implements AuthenticationSystem {
    private final Authenticator<LoginResultJson> gcAuthAuthenticator = new GCAuthenticators.GCAuthAuthenticator();
    private final Authenticator<LoginResultJson> tokenAuthenticator = new DefaultAuthenticators.TokenAuthenticator();
    private final Authenticator<ComboTokenResJson> sessionKeyAuthenticator = new DefaultAuthenticators.SessionKeyAuthenticator();
    private final GCAuthAuthenticationHandler externalAuthenticator = new GCAuthAuthenticationHandler();

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
        return gcAuthAuthenticator;
    }

    @Override
    public Authenticator<LoginResultJson> getTokenAuthenticator() {
        return tokenAuthenticator;
    }

    @Override
    public Authenticator<ComboTokenResJson> getSessionKeyAuthenticator() {
        return sessionKeyAuthenticator;
    }

    @Override
    public ExternalAuthenticator getExternalAuthenticator() {
        return externalAuthenticator;
    }
}
