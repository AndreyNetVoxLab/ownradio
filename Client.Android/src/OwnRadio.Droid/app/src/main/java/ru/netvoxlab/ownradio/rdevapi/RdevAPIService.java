package ru.netvoxlab.ownradio.rdevapi;

import com.google.gson.JsonObject;

import java.util.Map;

import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.Header;
import retrofit2.http.Headers;
import retrofit2.http.PATCH;
import retrofit2.http.POST;
import retrofit2.http.Path;
import retrofit2.http.Streaming;
import ru.netvoxlab.ownradio.rdevApiObjects.DeviceInfoBody;
import ru.netvoxlab.ownradio.rdevApiObjects.DeviceInfoResponse;
import ru.netvoxlab.ownradio.rdevApiObjects.ExecuteProcedureObject;
import ru.netvoxlab.ownradio.rdevApiObjects.GetTrackFilestream;
import ru.netvoxlab.ownradio.rdevApiObjects.LoginResponseBody;
import ru.netvoxlab.ownradio.rdevApiObjects.NextTrackBody;
import ru.netvoxlab.ownradio.rdevApiObjects.RegisterUserBody;
import ru.netvoxlab.ownradio.rdevApiObjects.SendHistoryInfoBody;
import ru.netvoxlab.ownradio.rdevApiObjects.SetIsCorrectBody;
import ru.netvoxlab.ownradio.rdevApiObjects.StoredProcedureResponse;

public interface RdevAPIService {
    //Запрос на получение auth-токена
    @POST("auth/login")
    @Headers("Content-Type: application/json")
    Call<LoginResponseBody> getRdevAuthToken (@Body JsonObject body);

    //Запрос на получение информации о следующем загружаемом треке
    @POST("api/executejs")
    @Headers("Content-Type: application/json")
    Call<Map<String, Map<String, String>[]>> getRdevNextTrack (@Header("Authorization") String token, @Body NextTrackBody body);

    //Запрос на регистрацию устройства
    @POST("api/executejs")
    @Headers("Content-Type: application/json")
    Call<Map<String, Map<String, String>[]>> rdevRegisterNewDevice (@Header("Authorization") String token, @Body RegisterUserBody body);

    //Запрос на получение записи об устройстве
    @POST("api/executejs")
    @Headers("Content-Type: application/json")
    Call<DeviceInfoResponse> getRdevDeviceInfo (@Header("Authorization") String token, @Body DeviceInfoBody body);

    //Запрос на получение потока с треком
    @Streaming
    @POST("api/executejs")
    @Headers("Content-Type: application/json")
    Call<ResponseBody> getTrackFilestream(@Header("Authorization") String token, @Body GetTrackFilestream body);

    //Запрос на передачу истории серверу
    @POST("api/executejs")
    @Headers("Content-Type: application/json")
    Call<ResponseBody> sendListensHistory(@Header("Authorization") String token, @Body SendHistoryInfoBody body);

    //Запрос на обновление записи в таблице tracks
    @PATCH("odata/tracks({trackid})")
    @Headers("Content-Type: application/json")
    Call<Void> setIsCorrect(@Header("Authorization") String token, @Path("trackid")String trackid, @Body SetIsCorrectBody body);
    //Выполнение хранимки на сервере
    @POST("sqlstoredprocedure/execute")
    @Headers("Content-Type: application/json")
    Call<StoredProcedureResponse> executeStoredProcedure(@Body ExecuteProcedureObject body);
}
