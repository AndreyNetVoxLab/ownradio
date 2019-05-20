package ru.netvoxlab.ownradio.rdevApiObjects;

import com.google.gson.annotations.SerializedName;

import java.util.Map;

public class DeviceInfoResponse {
    @SerializedName("result")
    private Map<String, String> result;
    @SerializedName("log")
    private Map<String, String>[] log;

    public Map<String, String> getResult(){
        return this.result;
    }

    public Map<String, String>[] getLog(){
        return this.log;
    }
}
