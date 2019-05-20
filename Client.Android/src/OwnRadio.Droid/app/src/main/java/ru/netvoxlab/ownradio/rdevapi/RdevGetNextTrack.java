package ru.netvoxlab.ownradio.rdevapi;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.preference.PreferenceManager;
import android.util.ArrayMap;
import android.util.Log;

import com.google.gson.Gson;
import com.google.gson.JsonObject;

import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import retrofit2.Response;
import ru.netvoxlab.ownradio.Utilites;
import ru.netvoxlab.ownradio.rdevApiObjects.NextTrackBody;
import ru.netvoxlab.ownradio.rdevApiObjects.NexttrackFields;

public class RdevGetNextTrack extends AsyncTask<String, Void, Map<String, Map<String, String>[]>> {
    Context mContext;
    SharedPreferences sp;

    public RdevGetNextTrack(Context context){
        this.mContext = context;
        this.sp = PreferenceManager.getDefaultSharedPreferences(mContext);
    }


    @Override
    protected Map<String, Map<String, String>[]> doInBackground(String... strings) {
        try{
            Integer ratio = sp.getInt("tracksRatio", 100);
            NexttrackFields bodyFields = new NexttrackFields("", strings[0], ratio);
            NextTrackBody body = new NextTrackBody(bodyFields);

            String bodyJson = new Gson().toJson(body);
            Log.d("Body", bodyJson);
            String token = sp.getString("authToken", "");
            Response<Map<String, Map<String, String>[]>> response = RdevServiceGenerator.createService(RdevAPIService.class).getRdevNextTrack("Bearer " + token, body).execute();
            if (response.code() == 200) {
                new Utilites().SendInformationTxt(mContext, "RdevGetNextTrack: Information about next track is received.");
                Log.d("GetTrackSuccess", "SUCCESS");
                return response.body();
            } else if(response.code() == 401){
                new Utilites().SendInformationTxt(mContext, "Unauthorized");
                Log.d("Unauthorized", token);

                Map<String, Map<String, String>[]> res = new HashMap<String, Map<String, String>[]>();
                Map<String, String>[] resArr = new Map[1];
                Map<String, String> resMap = new HashMap<>();
                resMap.put("unauthorized", String.valueOf(response.code()));
                resArr[0] = resMap;
                res.put("unauth", resArr);
                return res;
            }
            else {
                new Utilites().SendInformationTxt(mContext, "RdevGetNextTrack: Error with response code: " + response.code());
                int code = response.code();
                Log.d("GetTrackFail", String.valueOf(response.code()));
                Log.d("GetTrackFail", response.message());
                return null;
            }
        }catch (Exception ex){
            Log.d("RdevGetNextTrack", ex.getLocalizedMessage());
            new Utilites().SendInformationTxt(mContext, "RdevGetNextTrack: " + ex.getLocalizedMessage());
            return null;
        }
    }

    protected void onPostExecute(Map<String, Map<String, String>[]> result) {
        super.onPostExecute(result);
        //trackModel = result;
    }
}
