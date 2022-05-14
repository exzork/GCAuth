package me.exzork.gcauth.handler;

import emu.grasscutter.Grasscutter;
import emu.grasscutter.auth.AuthenticationSystem;
import emu.grasscutter.auth.Authenticator;
import emu.grasscutter.database.DatabaseHelper;
import emu.grasscutter.game.Account;
import emu.grasscutter.server.http.objects.LoginResultJson;
import me.exzork.gcauth.utils.Authentication;

public class GCAuthenticators {

    public static class GCAuthAuthenticator implements Authenticator<LoginResultJson> {

        @Override
        public LoginResultJson authenticate(AuthenticationSystem.AuthenticationRequest authenticationRequest) {
            var response = new LoginResultJson();

            var requestData = authenticationRequest.getPasswordRequest();
            assert requestData != null;
            Account account = Authentication.getAccountByOneTimeToken(requestData.account);
            if(account == null) {
                Grasscutter.getLogger().info("[GCAuth] Client " + requestData.account + " tried to login with invalid one time token.");
                response.retcode = -201;
                response.message = "Token is invalid";
                return response;
            }

            // Account was found, log the player in
            response.message = "OK";
            response.data.account.uid = account.getId();
            response.data.account.token = account.generateSessionKey();
            response.data.account.email = account.getEmail();

            Grasscutter.getLogger().info("[GCAuth] Client " + requestData.account + " logged in");
            return response;
        }
    }
}
