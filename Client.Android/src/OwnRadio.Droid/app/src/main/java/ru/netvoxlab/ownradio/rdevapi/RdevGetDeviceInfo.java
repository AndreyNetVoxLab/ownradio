package ru.netvoxlab.ownradio.rdevapi;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.preference.PreferenceManager;
import android.util.Log;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import retrofit2.Response;
import ru.netvoxlab.ownradio.rdevApiObjects.DeviceInfoBody;
import ru.netvoxlab.ownradio.rdevApiObjects.DeviceInfoResponse;

public class RdevGetDeviceInfo extends AsyncTask<String, Void, Map<String, DeviceInfoResponse>> {
    Context mContext;
    SharedPreferences sp;

    public RdevGetDeviceInfo(Context context){
        this.mContext = context;
        this.sp = PreferenceManager.getDefaultSharedPreferences(mContext);
    }

    @Override
    protected Map<String, DeviceInfoResponse> doInBackground(String... strings) {
        DeviceInfoBody body = new DeviceInfoBody(strings[0]);
        try {
            String token = sp.getString("authToken", "");
            Map<String, DeviceInfoResponse> returnMap = new HashMap<>();
            Response<DeviceInfoResponse> response = RdevServiceGenerator.createService(RdevAPIService.class).getRdevDeviceInfo("Bearer " + token, body).execute();
            if (response.code() == 200){
                Map<String, String> result = response.body().getResult();
                if (result != null){
                    returnMap.put("OK", response.body());
                    return returnMap;
                }
                else {
                    return null;
                }
            } else if (response.code() == 401){
                Log.d("Unauthorized", String.valueOf(response.code()));
                returnMap.put(String.valueOf(response.code()), response.body());
                return returnMap;
            }
            else {
                Log.d("??", String.valueOf(response.code()));
                returnMap.put(String.valueOf(response.code()), response.body());
                return returnMap;
            }
        } catch (Exception e) {
            Log.d("RdevGetNextTrack", e.getLocalizedMessage());
            return null;
        }
    }

    protected void onPostExecute(Map<String, DeviceInfoResponse> result) {
        super.onPostExecute(result);
        //trackModel = result;
    }
}
