package ru.netvoxlab.ownradio.rdevapi;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.preference.PreferenceManager;

import java.net.HttpURLConnection;

import retrofit2.Response;
import ru.netvoxlab.ownradio.APIService;
import ru.netvoxlab.ownradio.ServiceGenerator;
import ru.netvoxlab.ownradio.SetIsCorrect;
import ru.netvoxlab.ownradio.Utilites;
import ru.netvoxlab.ownradio.models.TrackModel;
import ru.netvoxlab.ownradio.rdevApiObjects.SetIsCorrectBody;

public class RdevSetIsCorrect extends AsyncTask<String, Void, Boolean> {
    Context mContext;
    SharedPreferences sp;
    public RdevSetIsCorrect(Context context){
        this.mContext = context;
        this.sp = PreferenceManager.getDefaultSharedPreferences(mContext);
    }

    @Override
    protected Boolean doInBackground(String... strings) {
        try {
            SetIsCorrectBody body = new SetIsCorrectBody(Boolean.parseBoolean(strings[1]));
            String token = sp.getString("authToken", "");
            Response<Void> response = RdevServiceGenerator.createService(RdevAPIService.class).setIsCorrect("Bearer " + token, strings[0], body).execute();
            if(response.isSuccessful()){
                if (response.code() == 200) {
                    new Utilites().SendInformationTxt(mContext, "Трек " + strings[1] + "был помечен некорректным");
                    return true;
                }
                else if (response.code() == 401){
                    new Utilites().SendInformationTxt(mContext, "Ошибка в функции setIsCorrect. Сервер вернул код: " + response.code());
                    return null;
                }
                else {
                    new Utilites().SendInformationTxt(mContext, "Ошибка в функции setIsCorrect. Сервер вернул код: " + response.code());
                    return false;
                }

            }else {
                new Utilites().SendInformationTxt(mContext, "Ошибка в функции setIsCorrect. Сервер вернул код: " + response.code());
                return false;
            }
        }catch (Exception ex){
            new Utilites().SendInformationTxt(mContext, "Ошибка в функции setIsCorrect");
            return false;
        }
    }

    protected void onPostExecute(Boolean result) {
        super.onPostExecute(result);
    }
}
