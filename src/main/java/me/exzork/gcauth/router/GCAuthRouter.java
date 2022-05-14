package me.exzork.gcauth.router;

import emu.grasscutter.server.http.Router;
import express.Express;
import io.javalin.Javalin;
import me.exzork.gcauth.handler.ChangePasswordHandler;
import me.exzork.gcauth.handler.LoginHandler;
import me.exzork.gcauth.handler.RegisterHandler;

public class GCAuthRouter implements Router {
    @Override
    public void applyRoutes(Express express, Javalin javalin) {
        express.post("/authentication/login", new LoginHandler());
        express.post("/authentication/register", new RegisterHandler());
        express.post("/authentication/change_password", new ChangePasswordHandler());
    }
}
