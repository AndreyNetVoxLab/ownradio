package ru.netvoxlab.ownradio;

import android.app.IntentService;
import android.app.Notification;
import android.content.Intent;
import android.support.v4.content.ContextCompat;
import android.util.Log;

import static ru.netvoxlab.ownradio.Constants.ACTION_FILLCACHE;
import static ru.netvoxlab.ownradio.Constants.ACTION_GETNEXTTRACK;
import static ru.netvoxlab.ownradio.Constants.EXTRA_COUNT;
import static ru.netvoxlab.ownradio.Constants.EXTRA_DEVICEID;
import static ru.netvoxlab.ownradio.Constants.OPTIMIZE_ENABLED;
import static ru.netvoxlab.ownradio.Constants.OPTIMIZE_STATUS;
import static ru.netvoxlab.ownradio.Constants.TAG;

/**
 * Created by a.polunina on 20.07.2017.
 */

public class LongRequestAPIService extends IntentService {
	public int waitingIntentCount;
	
	public LongRequestAPIService() {
		super("LongRequestAPIService");

	}
	
	public void onCreate() {
		super.onCreate();
		this.startForeground(2, new Notification());
	}
	
	@Override
	public int onStartCommand(Intent intent, int flags, int startId){
		waitingIntentCount++;
		Log.i(TAG, "onStartCommand, waitingIntentCount = " + waitingIntentCount);
		return super.onStartCommand(intent, flags, startId);
	}
	
	@Override
	protected void onHandleIntent(Intent intent) {
		waitingIntentCount--;
		Log.i(TAG, "onHandleIntent, waitingIntentCount = " + waitingIntentCount);
		
		if (intent != null) {
			
			if(!new CheckConnection().CheckInetConnection(getApplicationContext()))
				return;
			
			final String action = intent.getAction();
			if (ACTION_GETNEXTTRACK.equals(action)) {
				//Получение информации о следующем треке и его загрузка
				final String deviceId = intent.getStringExtra(EXTRA_DEVICEID);
				Integer countTracks = intent.getIntExtra(EXTRA_COUNT, 3);

				TrackDataAccess trackInfo = new TrackDataAccess(getApplicationContext());
				PrefManager prefManager = new PrefManager(getApplicationContext());
				String optimizeStatus = prefManager.getPrefItem(OPTIMIZE_STATUS, OPTIMIZE_ENABLED);

				int cachedTrackCount = trackInfo.GetExistTracksCount();
				if(optimizeStatus.equals(OPTIMIZE_ENABLED) && cachedTrackCount <= 10 && CheckConnection.isConnectedMobile(getApplicationContext()) ){
					countTracks = 2;
				}
				else if(optimizeStatus.equals(OPTIMIZE_ENABLED) && cachedTrackCount <= 50 && CheckConnection.isConnectedMobile(getApplicationContext())){
					countTracks = 1;
				}
				else if(optimizeStatus.equals(OPTIMIZE_ENABLED) && cachedTrackCount > 50 && CheckConnection.isConnectedMobile(getApplicationContext())){
					countTracks = 0;
				}

				new TrackToCache(getApplicationContext()).SaveTrackToCache(deviceId, countTracks);
			} else if(ACTION_FILLCACHE.equals(action)) {
				new TrackToCache(getApplicationContext()).FillCache();
			}
		}
		Log.i(TAG, "onHandleIntent end");
		
	}
	
	public void onDestroy() {
		super.onDestroy();
	}
	
	public int getWaitingIntentCount(){
		Log.e(TAG, "waitingIntentCount = " + waitingIntentCount);
		return waitingIntentCount;
	}
}
