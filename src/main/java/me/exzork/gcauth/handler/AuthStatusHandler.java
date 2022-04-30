package me.exzork.gcauth.handler;

import express.http.HttpContextHandler;
import express.http.Request;
import express.http.Response;
import me.exzork.gcauth.GCAuth;
import me.exzork.gcauth.json.AuthResponseJson;

import java.io.IOException;

public class AuthStatusHandler implements HttpContextHandler {
    @Override
    public void handle(Request request, Response response) throws IOException {
        AuthResponseJson authResponse = new AuthResponseJson();
        authResponse.success = true;
        authResponse.message = GCAuth.getConfig().Enable ? "AUTH_ENABLED" : "AUTH_DISABLED";
        authResponse.jwt = "";
        response.send(authResponse);
    }
}
