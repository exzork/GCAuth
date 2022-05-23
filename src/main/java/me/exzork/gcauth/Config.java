package me.exzork.gcauth;

import me.exzork.gcauth.utils.Authentication;

public final class Config {
    public String Hash = "BCRYPT";
    public String jwtSecret = Authentication.generateRandomString(32);
    public long jwtExpiration = 86400;
    public long otpExpiration = 300;
    public String[] defaultPermissions = new String[]{""};
    public String accessKey = "";
}
