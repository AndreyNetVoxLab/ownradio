package ru.netvoxlab.ownradio.rdevapi;

import android.content.ContentValues;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.preference.PreferenceManager;
import android.util.Log;

import com.google.gson.Gson;

import java.net.HttpURLConnection;

import okhttp3.ResponseBody;
import retrofit2.Response;
import ru.netvoxlab.ownradio.APIService;
import ru.netvoxlab.ownradio.HistoryDataAccess;
import ru.netvoxlab.ownradio.ServiceGenerator;
import ru.netvoxlab.ownradio.Utilites;
import ru.netvoxlab.ownradio.models.HistoryModel;
import ru.netvoxlab.ownradio.rdevApiObjects.SendHistoryInfoBody;

public class RdevSaveHistoryInfo extends AsyncTask<String, Void, Boolean> {
    Context mContext;
    SharedPreferences sp;

    public RdevSaveHistoryInfo(Context context){
        this.mContext = context;
        this.sp = PreferenceManager.getDefaultSharedPreferences(mContext);
    }

    @Override
    protected Boolean doInBackground(String... strings) {
        final HistoryDataAccess historyDataAccess = new HistoryDataAccess(mContext);
//		final ContentValues historyRecs = historyDataAccess.GetHistoryRec();
//
//		if (historyRecs == null)
//			return true;
//
        final ContentValues historyRec = historyDataAccess.GetHistoryRec();

        try {
//				final ContentValues historyRec = historyDataAccess.GetHistoryRec();
            if (historyRec == null) //Если неотправленной статистики нет - выходим
                return true;

            if (historyRec.getAsString("trackid").equals("")) {
                historyDataAccess.DeleteHistoryRec(historyRec.getAsString("id"));
                return true;
            }

//				HistoryModel data = new HistoryModel("2016-11-16T13:15:15",1,1);
            SendHistoryInfoBody body = new SendHistoryInfoBody(historyRec.getAsString("trackid"), strings[0], historyRec.getAsString("id"), historyRec.getAsString("isListen"), historyRec.getAsString("lastListen"), strings[1]);
//            HistoryModel historyData = new HistoryModel();
//            historyData.setRecId(historyRec.getAsString("id"));
//            historyData.setLastListen(historyRec.getAsString("lastListen"));
//            historyData.setIsListen(historyRec.getAsInteger("isListen"));

            String gsonBody = new Gson().toJson(body);
            Log.d("Json", gsonBody);
            //Response<Void> response = ServiceGenerator.createService(APIService.class).sendHistory(data[0], historyRec.getAsString("trackid"), historyData).execute();
            String token = sp.getString("authToken", "");
            Response<ResponseBody> response = RdevServiceGenerator.createService(RdevAPIService.class).sendListensHistory("Bearer " + token, body).execute();
            //Log.d("resp", response.body().string());

            if (response.code() == 200) {
                historyDataAccess.DeleteHistoryRec(historyRec.getAsString("id"));
                new Utilites().SendInformationTxt(mContext, "History by trackId " + historyRec.getAsString("trackid") + " is sending with response code=" + response.code());
                Log.d("SendHistorySuccess", "OK");
                return true;
            }else if( response.code() == 401){
                new Utilites().SendInformationTxt(mContext, "Unauthorized");
                Log.d("Errornauthorized", String.valueOf(response.code()));
                return null;
            }
            else {
                new Utilites().SendInformationTxt(mContext, "Error: History by trackId " + historyRec.getAsString("trackid")+ " not send with response code=" + response.code());
                historyDataAccess.DeleteHistoryRec(historyRec.getAsString("id"));
                Log.d("Error", String.valueOf(response.code()));
                return false;
            }
        } catch (Exception ex) {
            ex.printStackTrace();
            Log.d("Exception", ex.getLocalizedMessage());
            new Utilites().SendInformationTxt(mContext,"Error: History by trackId " + historyRec.getAsString("trackid")+ " not send with exception="  + ex.getLocalizedMessage());
            return false;
        }
    }

    protected void onPostExecute(Boolean result) {
        super.onPostExecute(result);
    }
}
