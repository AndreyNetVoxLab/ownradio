package ru.netvoxlab.ownradio.rdevApiObjects;

import com.google.gson.annotations.SerializedName;

import java.util.ArrayList;

/**
 * Тело результата выполнения хранимки на rdev
 */
public class StoredProcedureResponse {
    @SerializedName("AdditionalData")
    String additionalData;

    @SerializedName("Success")
    Boolean success;

    @SerializedName("Message")
    String message;

    @SerializedName("Data")
    ArrayList<AttachDeviceStoredProcData> data = new ArrayList<>();

    StoredProcedureResponse(String additionalData, Boolean success, String message, ArrayList<AttachDeviceStoredProcData> data){
        this.additionalData = additionalData;
        this.success = success;
        this.message = message;
        this.data = data;
    }

    public void addData(AttachDeviceStoredProcData dataItem){
        this.data.add(dataItem);
    }

    public Boolean getSuccess() {
        return success;
    }

    public String getMessage() {
        return message;
    }

    public String getAdditionalData() {
        return additionalData;
    }

    public ArrayList<AttachDeviceStoredProcData> getData() {
        return data;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public void setSuccess(Boolean success) {
        this.success = success;
    }

    public void setAdditionalData(String additionalData) {
        this.additionalData = additionalData;
    }

    public void setData(ArrayList<AttachDeviceStoredProcData> data) {
        this.data = data;
    }
}
