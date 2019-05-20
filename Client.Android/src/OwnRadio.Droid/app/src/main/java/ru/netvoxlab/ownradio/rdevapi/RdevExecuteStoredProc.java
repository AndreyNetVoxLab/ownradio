package ru.netvoxlab.ownradio.rdevapi;

import android.os.AsyncTask;

import java.util.Map;

import retrofit2.Response;
import ru.netvoxlab.ownradio.rdevApiObjects.DeviceInfoResponse;
import ru.netvoxlab.ownradio.rdevApiObjects.ExecuteProcedureObject;
import ru.netvoxlab.ownradio.rdevApiObjects.StoredProcedureResponse;

public class RdevExecuteStoredProc extends AsyncTask<ExecuteProcedureObject, Void, StoredProcedureResponse> {
    public RdevExecuteStoredProc(){

    }

    @Override
    protected StoredProcedureResponse doInBackground(ExecuteProcedureObject... procedureObjects) {
        try{
            Response<StoredProcedureResponse> response = RdevServiceGenerator.createService(RdevAPIService.class).executeStoredProcedure(procedureObjects[0]).execute();
            Integer responseCode = response.code();
            if(responseCode == 200){
                return response.body();
            }
            else {
                return null;
            }
        }catch (Exception e){
            return null;
        }
    }

    protected void onPostExecute(StoredProcedureResponse result) {
        super.onPostExecute(result);
    }
}
