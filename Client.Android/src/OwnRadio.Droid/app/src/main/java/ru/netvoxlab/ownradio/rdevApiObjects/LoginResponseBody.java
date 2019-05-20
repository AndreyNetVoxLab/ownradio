package ru.netvoxlab.ownradio.rdevApiObjects;

import com.google.gson.annotations.SerializedName;

public class LoginResponseBody {
    @SerializedName("token")
    private String token;

    @SerializedName("username")
    private String username;

    @SerializedName("groups")
    private String[] groups;

    LoginResponseBody(String token, String username, String[] groups){
        this.token = token;
        this.username = username;
        this.groups = groups;
    }

    public String getUsername() {
        return username;
    }

    public String getToken() {
        return token;
    }
}
