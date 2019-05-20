package ru.netvoxlab.ownradio.rdevApiObjects;

import com.google.gson.annotations.SerializedName;

public class AttachDeviceStoredProcData {
    @SerializedName("userid")
    String userid;

    @SerializedName("success")
    Boolean success;

    @SerializedName("request_info")
    String request_info;

    AttachDeviceStoredProcData(String userid, Boolean success, String request_info){
        this.userid = userid;
        this.success = success;
        this.request_info = request_info;
    }

    public Boolean getSuccess() {
        return success;
    }

    public String getRequest_info() {
        return request_info;
    }

    public String getUserid() {
        return userid;
    }

    public void setSuccess(Boolean success) {
        this.success = success;
    }

    public void setRequest_info(String request_info) {
        this.request_info = request_info;
    }

    public void setUserid(String userid) {
        this.userid = userid;
    }
}
