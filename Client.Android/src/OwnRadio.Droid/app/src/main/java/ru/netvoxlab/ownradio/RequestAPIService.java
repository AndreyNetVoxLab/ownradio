package ru.netvoxlab.ownradio;

import android.app.IntentService;
import android.app.Notification;
import android.content.Intent;
import android.content.SharedPreferences;

import java.util.Map;

import ru.netvoxlab.ownradio.rdevapi.RdevApiCalls;

/**
 * An {@link IntentService} subclass for handling asynchronous task requests in
 * a service on a separate handler thread.
 * <p> Сервис, последовательно обрабатывающий поступающие к нему запросы в отдельном потоке.
 * helper methods.
 */
public class RequestAPIService extends IntentService {
	public static final String ACTION_SENDHISTORY = "ru.netvoxlab.ownradio.action.SENDHISTORY";
	public static final String ACTION_GETNEXTTRACK = "ru.netvoxlab.ownradio.action.GETNEXTTRACK";
	public static final String ACTION_SENDLOGS = "ru.netvoxlab.ownradio.action.SENDLOGS";
	public static final String EXTRA_USERID = "ru.netvoxlab.ownradio.action.USERID";
	public static final String EXTRA_DEVICEID = "ru.netvoxlab.ownradio.extra.EXTRA_DEVICEID";
	public static final String EXTRA_COUNT = "ru.netvoxlab.ownradio.extra.COUNT";
	public static final String EXTRA_LOGFILEPATH = "ru.netvoxlab.ownradio.extra.LOGFILEPATH";

	//SharedPreferences sp;

	public RequestAPIService() {
		super("RequestAPIService");
	}
	
	public void onCreate() {
		super.onCreate();
		startForeground(4, new Notification());
	}
	
	@Override
	protected void onHandleIntent(Intent intent) {
		if (intent != null) {
			
			if(!new CheckConnection().CheckInetConnection(getApplicationContext()))
				return;
			
			final String action = intent.getAction();
			if (ACTION_SENDHISTORY.equals(action)) {
				//Отправка на сервер накопленной истории прослушивания треков
				final String deviceId = intent.getStringExtra(EXTRA_DEVICEID);
				RdevApiCalls rdevApiCalls = new RdevApiCalls(getApplicationContext());
				//
				// final Map<String, String> authMap = rdevApiCalls.GetAuthToken();
				//String token = authMap.get("token");
				String userid = rdevApiCalls.GetDeviceInfo(deviceId).get("OK").getResult().get("userid");
				for (int i = 0; i < 3; i++) {
//					new APICalls(getApplicationContext()).SendHistory(deviceId);
					new RdevApiCalls(getApplicationContext()).SendHistoryInfo(userid, deviceId);
				}
			} else if(ACTION_SENDLOGS.equals(action)){
				final String deviceId = intent.getStringExtra(EXTRA_DEVICEID);
				final String logFilePath = intent.getStringExtra(EXTRA_LOGFILEPATH);
				try {
//					if (new File(logFilePath).length() <= 0)
						Thread.sleep(1000);
					//new APICalls(getApplicationContext()).SendLogs(deviceId, logFilePath);
				}catch (Exception ex){
					new Utilites().SendInformationTxt(getApplicationContext(), "Ошибка при отправке логов: " + ex.getLocalizedMessage());
				}
			}
		}
	}
	
	public void onDestroy() {
		super.onDestroy();
	}
}
