package me.exzork.gcauth.handler;

import emu.grasscutter.auth.AuthenticationSystem;
import emu.grasscutter.auth.Authenticator;
import emu.grasscutter.auth.DefaultAuthenticators;
import emu.grasscutter.server.http.objects.ComboTokenResJson;
import emu.grasscutter.server.http.objects.LoginResultJson;

public class GCAuthAuthenticationHandler implements AuthenticationSystem {
    private final Authenticator<LoginResultJson> gcAuthAuthenticator = new GCAuthenticators.GCAuthAuthenticator();
    private final Authenticator<LoginResultJson> tokenAuthenticator = new DefaultAuthenticators.TokenAuthenticator();
    private final Authenticator<ComboTokenResJson> sessionKeyAuthenticator = new DefaultAuthenticators.SessionKeyAuthenticator();

    @Override
    public void createAccount(String username, String password) {

    }

    @Override
    public void resetPassword(String s) {

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
}
