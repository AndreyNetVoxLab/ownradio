package ru.netvoxlab.ownradio.rdevapi;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.preference.PreferenceManager;
import android.util.Log;

import java.util.Map;

import retrofit2.Response;
import ru.netvoxlab.ownradio.Utilites;
import ru.netvoxlab.ownradio.rdevApiObjects.RegisterUserBody;

public class RdevRegisterDevice extends AsyncTask<String, Void, Boolean> {
    Context mContext;
    SharedPreferences sp;
    public RdevRegisterDevice(Context context){
        this.mContext = context;
        this.sp = PreferenceManager.getDefaultSharedPreferences(mContext);
    }


    @Override
    protected Boolean doInBackground(String... strings) {
        try {
            RegisterUserBody body = new RegisterUserBody(strings[0], strings[1]);
            String token = sp.getString("authToken", "");
            Response<Map<String, Map<String, String>[]>> response = RdevServiceGenerator.createService(RdevAPIService.class).rdevRegisterNewDevice("Bearer " + token, body).execute();
            if (response.code() == 200){
                Log.d("Success", String.valueOf(response.code()));
                return true;
            }else if (response.code() == 401){
                Log.d("Unauth", String.valueOf(response.code()));
                new Utilites().SendInformationTxt(mContext, "No authorized -  " + String.valueOf(response.code()));

                return null;
            }
            else {
                return false;
            }
        } catch (Exception ex) {
            ex.printStackTrace();
            new Utilites().SendInformationTxt(mContext, "Device is not register -  " + ex.getLocalizedMessage());
            return false;
        }
    }

    protected void onPostExecute(Boolean result) {
        super.onPostExecute(result);
    }
}
