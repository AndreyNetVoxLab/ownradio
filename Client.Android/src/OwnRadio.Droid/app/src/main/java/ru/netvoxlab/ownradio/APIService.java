package ru.netvoxlab.ownradio;

import java.util.Map;

import okhttp3.MultipartBody;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.Field;
import retrofit2.http.GET;
import retrofit2.http.Header;
import retrofit2.http.Headers;
import retrofit2.http.Multipart;
import retrofit2.http.POST;
import retrofit2.http.Part;
import retrofit2.http.Path;
import retrofit2.http.Streaming;
import ru.netvoxlab.ownradio.models.DeviceModel;
import ru.netvoxlab.ownradio.models.HistoryModel;
import ru.netvoxlab.ownradio.models.TrackModel;
import ru.netvoxlab.ownradio.rdevApiObjects.NextTrackBody;

/**
 * Created by a.polunina on 15.11.2016.
 */

public interface APIService {

	@GET("v4/tracks/{deviceid}/next")
	@Headers("Content-Type: application/json")
	Call<Map<String, String>> getNextTrackID(@Path("deviceid") String deviceId);

	@Streaming
	@GET("v5/tracks/{trackid}/{deviceid}")
	Call<ResponseBody> getTrackById(@Path("trackid") String trackId, @Path("deviceid") String deviceId);
	
	@Streaming
	@GET("v5/tracks/{id}")
	Call<ResponseBody> getTrack(@Path("id") String trackId);
	
	@POST("v5/histories/{deviceid}/{trackid}")
	@Headers("Content-Type: application/json")
	Call<Void> sendHistory(@Path("deviceid") String deviceId, @Path("trackid") String trackId, @Body HistoryModel data);
	
	@POST("v5/devices")
	@Headers("Content-Type: application/json")
	Call<Void> registerDevice(@Body DeviceModel deviceModel);
	
	@POST("v5/tracks/{trackid}/{deviceid}")
	@Headers("Content-Type: application/json")
	Call<Void> setIsCorrect(@Path("trackid") String trackId, @Path("deviceid") String deviceId, @Body TrackModel data);
		
	@Multipart
	@POST("v4/logs/{deviceid}")
	Call<Map<String, String>> sendLogFile(@Path("deviceid") String deviceId, @Part MultipartBody.Part file);
}
