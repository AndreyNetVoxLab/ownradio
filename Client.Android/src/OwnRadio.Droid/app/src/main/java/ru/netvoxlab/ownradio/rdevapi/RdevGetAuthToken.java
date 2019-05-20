package ru.netvoxlab.ownradio.rdevapi;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.preference.PreferenceManager;
import android.util.Log;

import com.google.gson.JsonObject;

import java.util.Map;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import ru.netvoxlab.ownradio.APIService;
import ru.netvoxlab.ownradio.ServiceGenerator;
import ru.netvoxlab.ownradio.Utilites;
import ru.netvoxlab.ownradio.rdevApiObjects.LoginResponseBody;

public class RdevGetAuthToken extends AsyncTask<String, Void, LoginResponseBody> {
    private Object authResponse;
    Context mContext;
    SharedPreferences sp;

    public RdevGetAuthToken(Context context){
        this.mContext = context;
        this.sp = PreferenceManager.getDefaultSharedPreferences(mContext);
    }

    @Override
    protected LoginResponseBody doInBackground(String... strings) {
        try {
            Log.d("Auth", "Auth");
            //выполняем запрос к серверу getnexttrackid
            RdevAPIService rdevAPIService = RdevServiceGenerator.createService(RdevAPIService.class);

            JsonObject body = new JsonObject();
            body.addProperty("login", "admin");
            body.addProperty("password", "2128506");
            Log.d("ResponseStart", body.toString());
            Response<LoginResponseBody> response = RdevServiceGenerator.createService(RdevAPIService.class).getRdevAuthToken(body).execute();
            Log.d("ResponseStart1", body.toString());
            if (response.code() == 200 && !response.body().getToken().isEmpty()){
                Log.d("ResponseSuccess", String.valueOf(response.code()));
                sp.edit().putString("authToken", response.body().getToken()).commit();
                Log.d("ResponseSuccess", response.body().getToken());
                return response.body();

            }else {
                Log.d("ResponseFailure", String.valueOf(response.code()));
                Log.d("ResponseFailure", response.message());
                return null;
            }
//
//            if (response.code() == 200 && !response.body().isEmpty() && response.body().get("result").equals("true")) {
//                new Utilites().SendInformationTxt(mContext, "Auth token is recieved");
//                return response.body();
//            }else {
//                new Utilites().SendInformationTxt(mContext, "Get auth token error. Error with response code: " + response.code());
//                return null;
//            }
        } catch (Exception ex) {
            new Utilites().SendInformationTxt(mContext, "Get auth token: exception " + ex.getLocalizedMessage());
            Log.d("POST", ex.getLocalizedMessage());
            return null;
        }
    }

    protected void onPostExecute(LoginResponseBody result) {
        super.onPostExecute(result);
        this.authResponse = result;
    }
}
