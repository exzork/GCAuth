# GCAuth

Grasscutter Authentication System

### Usage : 
- Place jar inside plugins folder of Grasscutter.
- To change hash algorithm change `Hash` in config.json inside plugins/GCAuth (Only Bcrypt and Scrypt is supported)
- To use access control, you need set the `ACCESS_KEY` in config.json inside plugins/GCAuth. (Optional)
- All payload must be send with `application/json` and Compact JSON format ( without unnecessary spaces )
- Auth endpoint is:
  - Authentication Checking : `/authentication/type` (GET) , it'll return `me.exzork.gcauth.handler.GCAuthAuthenticationHandler` if GCAuth is loaded and enabled.
  - Register: `/authentication/register` (POST)
  ```
  {"username":"username","password":"password","password_confirmation":"password_confirmation","access_key":"access_key"}
  ```
  - Login: `/authentication/login` (POST) 
  ```
  {"username":"username","password":"password","access_key":"access_key"}
  ```
  - Change password: `/authentication/change_password` (POST)  
  ```
  {"username":"username","new_password":"new_password","new_password_confirmation":"new_password_confirmation","old_password":"old_password","access_key":"access_key"}
  ```
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