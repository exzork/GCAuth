package me.exzork.gcauth.auth;

import emu.grasscutter.Grasscutter;
import emu.grasscutter.auth.Authenticator;
import emu.grasscutter.auth.AuthenticationSystem.AuthenticationRequest;
import emu.grasscutter.auth.ExternalAuthenticator;
import emu.grasscutter.game.Account;
import emu.grasscutter.server.http.objects.*;

import me.exzork.gcauth.handler.*;
import me.exzork.gcauth.utils.Authentication;

import static emu.grasscutter.utils.Language.translate;


public final class GCAuthAuthenticators {

    /**
     * Handles the authentication request from the username & password form.
     */
    public static class PasswordAuthenticator implements Authenticator<LoginResultJson> {
        @Override public LoginResultJson authenticate(AuthenticationRequest request) {
            var response = new LoginResultJson();

            var requestData = request.getPasswordRequest();
            assert requestData != null; // This should never be null.

            boolean successfulLogin = false;
            String address = request.getRequest().ip();
            String responseMessage = translate("messages.dispatch.account.username_error");

            // Get account By OneTimeToken.
            Account account = Authentication.getAccountByOneTimeToken(requestData.account);

            // Check if account exists.
            if(account == null) {
                responseMessage = "Token is invalid";
            } else {
                successfulLogin = true;
            }

            // Set response data.
            if(successfulLogin) {
                response.message = "OK";
                response.data.account.uid = account.getId();
                response.data.account.token = account.generateSessionKey();
                response.data.account.email = account.getEmail();
                response.data.account.twitter_name = account.getUsername();

                // Log the login.
                Grasscutter.getLogger().info(translate("messages.dispatch.account.login_success", address, account.getId()));
            } else {
                response.retcode = -201;
                response.message = responseMessage;

                // Log the failure.
                Grasscutter.getLogger().info(translate("messages.dispatch.account.account_login_exist_error", address));
            }

            return response;
        }
    }

    /**
     * Handles authentication requests from external sources.
     */
    public static class ExternalAuthentication implements ExternalAuthenticator {
        @Override public void handleLogin(AuthenticationRequest request) {
            assert request.getResponse() != null;
            LoginHandler.handle(request.getRequest(), request.getResponse());
        }

        @Override public void handleAccountCreation(AuthenticationRequest request) {
            assert request.getResponse() != null;
            RegisterHandler.handle(request.getRequest(), request.getResponse());
        }

        @Override public void handlePasswordReset(AuthenticationRequest request) {
            assert request.getResponse() != null;
            ChangePasswordHandler.handle(request.getRequest(), request.getResponse());
        }
    }
}
