package me.exzork.gcauth;

import me.exzork.gcauth.utils.Authentication;

public final class Config {
    public String Hash = "BCRYPT";
    public String jwtSecret = Authentication.generateRandomString(32);
    public String[] defaultPermissions = new String[]{""};
}
