package me.exzork.gcauth.handler;

import com.sun.net.httpserver.HttpExchange;
import me.exzork.gcauth.GCAuth;
import me.exzork.gcauth.json.AuthResponseJson;

import java.io.IOException;

public class AuthStatusHandler extends AbstractHandler{
    @Override
    public void handle(HttpExchange t) throws IOException {
        AuthResponseJson authResponse = new AuthResponseJson();
        authResponse.success = true;
        authResponse.message = GCAuth.getConfig().Enable ? "AUTH_ENABLED" : "AUTH_DISABLED";
        authResponse.jwt = "";
        responseJSON(t, authResponse);
    }
}
