# GCAuth

Grasscutter Authentication System

### Version Compatibility
| GCAuth        | Grasscutter Development                                                                                                        | Grasscutter Stable |
|---------------|--------------------------------------------------------------------------------------------------------------------------------|--------------------|
| 2.3.1+        | 1.1.2-dev ( [141b191](https://github.com/Grasscutters/Grasscutter/commit/ce07f56f9d10cc79c9b7104b66c2e4ff19cd4f53) and after ) | -                  |
| 2.2.1 - 2.3.0 | 1.1.2-dev ( before [141b191](https://github.com/Grasscutters/Grasscutter/commit/ce07f56f9d10cc79c9b7104b66c2e4ff19cd4f53) )    | -                  |
| 2.1.4 - 2.1.6 | 1.1.1-dev                                                                                                                      | -                  |
| 2.0.0 - 2.1.3 | 1.0.3-dev                                                                                                                      | 1.1.0              |
| 1.0.0         | 1.0.2-dev                                                                                                                      | -                  |

### Usage : 
- Place jar inside plugins folder of Grasscutter.
- To change hash algorithm change `Hash` in config.json inside plugins/GCAuth (Only Bcrypt and Scrypt is supported)
- To use access control, you need set the `ACCESS_KEY` in config.json inside plugins/GCAuth. (Optional)
- All payload must be send with `application/json` and Compact JSON format ( without unnecessary spaces )
- Auth endpoint is:
  - Authentication Checking : `/authentication/type` (GET) , it'll return `GCAuthAuthenticationHandler` if GCAuth is loaded and enabled.
  - Register: `/authentication/register` (POST)
  ```
  {"username":"username","password":"password","password_confirmation":"password_confirmation"}
  ```
  - Login: `/authentication/login` (POST) 
  ```
  {"username":"username","password":"password"}
  ```
  - Change password: `/authentication/change_password` (POST)  
  ```
  {"username":"username","new_password":"new_password","new_password_confirmation":"new_password_confirmation","old_password":"old_password"}
  ```
- If you set ACCESS_KEY you must add `access_key: ACCESS_KEY` in your payload.
- Response is `JSON` with following keys:
  - `status` : `success` or `error`
  - `message` : 
    - AUTH_ENABLED : Plugin is enabled
    - AUTH_DISABLED : Plugin is disabled
    - EMPTY_BODY : No data was sent with the request
    - USERNAME_TAKEN : Username is already taken
    - PASSWORD_MISMATCH : Password does not match
    - UNKNOWN : Unknown error
    - INVALID_ACCOUNT : Username or password is invalid
    - NO_PASSWORD : Password is not set, please set password first by resetting it (change password)
    - ERROR_ACCESS_KEY : Access key is invalid (if access control is enabled)
  - `jwt` : JWT token if success with body :
    - `token` : Token used for authentication, paste it in username field of client.
    - `username` : Username of the user.
    - `uid` : UID of the user.

## Config :
- hash : Hash algorithm used for password hashing. (Only Bcrypt and Scrypt is supported)
- jwtSecret : Secret used for JWT token.
- jwtExpiration : Expiration time of JWT token.
- otpExpiration : Expiration time of OTP.
- defaultPermission : Default permission of user.
- accessKey : Access key used for access control. (Optional)
- rateLimit :
  - maxRequests : Maximum requests per timeUnit.
  - timeUnit : Time unit of rateLimit. (seconds, minutes, hours, days)
  - endPoints[] : Endpoint to rate limit. (login, register, change_password)