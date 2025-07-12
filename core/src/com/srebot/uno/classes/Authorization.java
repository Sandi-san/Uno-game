package com.srebot.uno.classes;

/** Class for authorizing user on server through register and login */
public class Authorization {
    private String name;
    private String password;

    public Authorization(String name, String password){
        this.name = name;
        this.password = password;
    }

    public class AuthorizationResponse {
        private String access_token;

        public void setAccessToken(String access_token) {
            this.access_token = access_token;
        }

        public String getAccessToken() {
            return access_token;
        }
    }
}
