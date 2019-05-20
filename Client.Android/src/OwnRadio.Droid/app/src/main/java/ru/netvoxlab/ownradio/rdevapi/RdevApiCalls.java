package ru.netvoxlab.ownradio.rdevapi;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

import java.util.ArrayList;
import java.util.Map;

import ru.netvoxlab.ownradio.CheckConnection;

import ru.netvoxlab.ownradio.Utilites;
import ru.netvoxlab.ownradio.rdevApiObjects.AttachDeviceStoredProcData;
import ru.netvoxlab.ownradio.rdevApiObjects.DeviceInfoResponse;
import ru.netvoxlab.ownradio.rdevApiObjects.ExecuteProcedureObject;
import ru.netvoxlab.ownradio.rdevApiObjects.LoginResponseBody;
import ru.netvoxlab.ownradio.rdevApiObjects.StoredProcParameter;
import ru.netvoxlab.ownradio.rdevApiObjects.StoredProcedureResponse;

public class RdevApiCalls {

    Context mContext;
    SharedPreferences sp;
    public RdevApiCalls(Context context){
        this.mContext = context;
        this.sp = PreferenceManager.getDefaultSharedPreferences(context);
    }

    public LoginResponseBody GetAuthToken(){
        CheckConnection checkConnection = new CheckConnection();
        boolean internetConnect = checkConnection.CheckInetConnection(mContext);
        if (!internetConnect)
            return null;
        try {
            LoginResponseBody result = new RdevGetAuthToken(mContext).execute().get();
            if (result == null)
                return null;
            return result;
        } catch (Exception ex) {
            new Utilites().SendInformationTxt(mContext, "Error by GetAuthToken " + ex.getLocalizedMessage());
            return null;
        }
    }

    public Map<String, Map<String, String>[]> GetNextTrack(String deviceId){
        CheckConnection checkConnection = new CheckConnection();
        boolean internetConnect = checkConnection.CheckInetConnection(mContext);
        if (!internetConnect)
            return null;
        try {
            Map<String, Map<String, String>[]> result = new RdevGetNextTrack(mContext).execute(deviceId).get();
            if (result == null)
                return null;
            else if (result.get("unauth") != null){
                GetAuthToken();
                GetNextTrack(deviceId);
            }
            return result;
        } catch (Exception ex) {
            new Utilites().SendInformationTxt(mContext, "Error by GetAuthToken " + ex.getLocalizedMessage());
            return null;
        }
    }

    public void RegisterDevice(String deviceId, String deviceName) {
        CheckConnection checkConnection = new CheckConnection();
        if (!checkConnection.CheckInetConnection(mContext)) {
            return;
        }

        try {
            Boolean result = new RdevRegisterDevice(mContext).execute(deviceId, deviceName).get();
            if(result == null){
                GetAuthToken();
                RegisterDevice(deviceId, deviceName);
            }
        } catch (Exception ex) {
            new Utilites().SendInformationTxt(mContext, "Error by registerDevice " + ex.getLocalizedMessage());
        }

    }

    public Map<String, DeviceInfoResponse> GetDeviceInfo(String recid){
        String result;
        CheckConnection checkConnection = new CheckConnection();
        if (!checkConnection.CheckInetConnection(mContext)) {
            return null;
        }

        try {

            Map<String, DeviceInfoResponse> responseMap = new RdevGetDeviceInfo(mContext).execute(recid).get();
            if (responseMap == null) {
                return null;
            }else if(responseMap.get("401") != null){
                GetAuthToken();
                responseMap = GetDeviceInfo(recid);

            }
            return responseMap;
        } catch (Exception ex) {
            new Utilites().SendInformationTxt(mContext, "Error by GetDeviceInfo " + ex.getLocalizedMessage());
            return null;
        }
    }

    public Boolean SendHistoryInfo(String userid, String deviceid){
        CheckConnection checkConnection = new CheckConnection();
        if (!checkConnection.CheckInetConnection(mContext)) {
            return null;
        }

        try {
            Boolean result = new RdevSaveHistoryInfo(mContext).execute(deviceid, userid).get();
            if(result != null && result){
                return true;
            }else if(result == null){
                GetAuthToken();
                SendHistoryInfo(userid, deviceid);
            }
            return false;

        } catch (Exception ex) {
            new Utilites().SendInformationTxt(mContext, "Error by SendHistoryInfo " + ex.getLocalizedMessage());
            return null;
        }
    }

    public void SetIsCorrect(String trackId, Boolean isCorrect){
        CheckConnection checkConnection = new CheckConnection();
        if (!checkConnection.CheckInetConnection(mContext)) {
            return;
        }

        try{
            Boolean result = new RdevSetIsCorrect(mContext).execute(trackId, String.valueOf(isCorrect)).get();
            if(result != null && result){
                return;
            }else if(result == null){
                GetAuthToken();
                SetIsCorrect(trackId, isCorrect);
            }

        }catch (Exception ex) {
            new Utilites().SendInformationTxt(mContext, "Error by SetIsCorrect " + ex.getLocalizedMessage());
                return;
        }
    }
    //Запрос на привязку устройства к пользователю
    public Boolean rdevAttachDeviceToUser(String deviceId, String email, String googletoken){
        ArrayList<StoredProcParameter> parameters = new ArrayList<>();
        parameters.add(new StoredProcParameter("device_id", deviceId, "SysString"));
        parameters.add(new StoredProcParameter("googleemail", email, "SysString"));
        parameters.add(new StoredProcParameter("googleidtoken", googletoken, "SysString"));

        ExecuteProcedureObject procedureObject = new ExecuteProcedureObject("attachdevicetouser", parameters);

        try {
            StoredProcedureResponse response = new RdevExecuteStoredProc().execute(procedureObject).get();
            if(response.getSuccess() && response != null) {
                ArrayList<AttachDeviceStoredProcData> responseData = response.getData();
                if(responseData.size() == 1 && responseData.get(0).getSuccess()){
                    return true;
                }
                else {
                    return false;
                }
            }else {
                return false;
            }
        }catch (Exception e){
            new Utilites().SendInformationTxt(mContext, "Error by rdevAttachDeviceToUser " + e.getLocalizedMessage());
            return false;
        }
    }
    //Запрос на отвязку устройства от пользователя
    public Boolean rdevDetachDeviceFromUser(String deviceId){
        ArrayList<StoredProcParameter> parameters = new ArrayList<>();
        parameters.add(new StoredProcParameter("device_id", deviceId, "SysString"));

        ExecuteProcedureObject procedureObject = new ExecuteProcedureObject("detachdevicefromuser", parameters);

        try {
            StoredProcedureResponse response = new RdevExecuteStoredProc().execute(procedureObject).get();
            if(response.getSuccess() && response != null){
                ArrayList<AttachDeviceStoredProcData> responseData = response.getData();
                if(responseData.size() == 1 && responseData.get(0).getSuccess()){
                    return true;
                }
                else {
                    return false;
                }
            }
            else {
                return false;
            }
        }catch (Exception e){
            new Utilites().SendInformationTxt(mContext, "Error by rdevAttachDeviceToUser " + e.getLocalizedMessage());
            return false;
        }
    }


}
