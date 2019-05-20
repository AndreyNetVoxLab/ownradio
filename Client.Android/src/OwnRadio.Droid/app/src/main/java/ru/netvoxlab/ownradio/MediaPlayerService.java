package ru.netvoxlab.ownradio;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.ComponentName;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.AssetFileDescriptor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.net.wifi.WifiManager;
import android.os.Binder;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.PowerManager;
import android.os.SystemClock;
import android.preference.PreferenceManager;
import android.support.v4.app.NotificationManagerCompat;
import android.support.v4.media.MediaMetadataCompat;
import android.support.v4.media.session.MediaButtonReceiver;
import android.support.v4.media.session.MediaControllerCompat;
import android.support.v4.media.session.MediaSessionCompat;
import android.support.v4.media.session.PlaybackStateCompat;
import android.support.v7.app.NotificationCompat;
import android.util.Log;
import android.view.KeyEvent;
import android.widget.RemoteViews;
import android.widget.Toast;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.TimeZone;
import java.util.concurrent.ExecutionException;

import ru.netvoxlab.ownradio.rdevapi.RdevApiCalls;
import ru.netvoxlab.ownradio.rdevapi.RdevGetTrack;
import ru.netvoxlab.ownradio.utils.ResourceHelper;

import static android.app.PendingIntent.getActivity;
import static ru.netvoxlab.ownradio.Constants.CURRENT_TRACK_ARTIST;
import static ru.netvoxlab.ownradio.Constants.CURRENT_TRACK_TITLE;
import static ru.netvoxlab.ownradio.Constants.OPTIMIZE_DISABLED;
import static ru.netvoxlab.ownradio.Constants.OPTIMIZE_ENABLED;
import static ru.netvoxlab.ownradio.Constants.OPTIMIZE_STATUS;
import static ru.netvoxlab.ownradio.MainActivity.ActionButtonImgUpdate;
import static ru.netvoxlab.ownradio.MainActivity.ActionNotFoundTrack;
import static ru.netvoxlab.ownradio.MainActivity.ActionProgressBarFirstTracksLoad;
import static ru.netvoxlab.ownradio.MainActivity.ActionProgressBarUpdate;
import static ru.netvoxlab.ownradio.MainActivity.ActionTrackInfoUpdate;
import static ru.netvoxlab.ownradio.MainActivity.ActionTrackInfoUpdate;
import static ru.netvoxlab.ownradio.MainActivity.ActionCheckListensCount;
import static ru.netvoxlab.ownradio.RequestAPIService.ACTION_GETNEXTTRACK;
import static ru.netvoxlab.ownradio.RequestAPIService.ACTION_SENDHISTORY;
import static ru.netvoxlab.ownradio.RequestAPIService.EXTRA_COUNT;
import static ru.netvoxlab.ownradio.RequestAPIService.EXTRA_DEVICEID;

public class MediaPlayerService extends Service implements MediaPlayer.OnCompletionListener, MediaPlayer.OnPreparedListener, AudioManager.OnAudioFocusChangeListener {
	public static final String ActionPlay = "ru.netvoxlab.ownradio.action.PLAY";
	public static final String ActionPause = "ru.netvoxlab.ownradio.action.PAUSE";
	public static final String ActionNext = "ru.netvoxlab.ownradio.action.NEXT";
	public static final String ActionStop = "ru.netvoxlab.ownradio.action.STOP";
	public static final String ActionTogglePlayback = "ru.netvoxlab.ownradio.action.TOGGLEPLAYBACK";
	public static final String ActionUpdateNotification = "ru.netvoxlab.ownradio.action.UPDATE_NOTIFICATION";
	public static MediaPlayer player = null;
	private AudioManager audioManager;
	private WifiManager wifiManager;
	private WifiManager.WifiLock wifiLock;
	private static final int REQUEST_CODE = 111;
	private static final int NOTIFICATION_ID = 502;
	private static final int minTrackDuration = 60;
	private static final int DELAY_DOUBLE_CLICK = 1000;

//	private MediaNotificationManager mMediaNotificationManager;
	
	private PowerManager pm;
	private PowerManager.WakeLock wl;
	
	private ComponentName remoteComponentName;
	private int NotificationId = 1;
	
	public MediaSessionCompat mediaSessionCompat;
	public MediaControllerCompat mediaControllerCompat;
	
	private Handler handler = new Handler();
	public static boolean playbackWithHSisInterrupted = false; //флаг было ли прервано проигрывание при подключенной гарнитуре
	public static boolean isAutoplay = false; //флаг начинать ли проигрывание автоматически
	public static boolean isHSConnected = false;
	String trackURL;
	TrackDataAccess trackDataAccess;
	Utilites utilites;
	String DeviceID;
	String UserID;
	String TrackID = "";
	Boolean FlagDownloadTrack = true;
	final String TAG = "ownRadio";

	SharedPreferences sp;
	ContentValues track;
	int startPosition = 0;
	String startTrackID = "";
	public boolean isPreparing = false;

//	private NotificationManagerCompat mNotificationManager ;
	
	int duration = 0;
	float volume = 1;
	float speed = 0.05f;
	
	PrefManager prefManager;
	
	public int GetMediaPlayerState() {
		return (mediaControllerCompat.getPlaybackState() != null ? mediaControllerCompat.getPlaybackState().getState() : PlaybackStateCompat.STATE_NONE);
	}
	
	@Override
	public void onCreate() {
		super.onCreate();
		this.sp = PreferenceManager.getDefaultSharedPreferences(this);
		startForeground(3, new Notification());
		prefManager = new PrefManager(this);
		trackDataAccess = new TrackDataAccess(getApplicationContext());
		utilites = new Utilites();
		DeviceID = prefManager.getDeviceId();
//		PreferenceManager.getDefaultSharedPreferences(this).getString("DeviceID", "");
		UserID = DeviceID;//PreferenceManager.getDefaultSharedPreferences(this).getString("UserID", "");
		audioManager = (AudioManager) getSystemService(AUDIO_SERVICE);
		wifiManager = (WifiManager) getApplicationContext().getSystemService(WIFI_SERVICE);
		remoteComponentName = new ComponentName(getPackageName(), new RemoteControlReceiver().ComponentName());


//		audioManager.getDevices(AudioManager.GET_DEVICES_ALL);
//		IntentFilter intentFilter = new IntentFilter(AudioManager.ACTION_AUDIO_BECOMING_NOISY);
//		registerReceiver(MusicBroadcastReceiver.class, intentFilter);
		pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
//		try {
//			mMediaNotificationManager = new MediaNotificationManager(this, getApplicationContext());
//		} catch (RemoteException e) {
//			throw new IllegalStateException("Could not create a MediaNotificationManager", e);
//		}
	}
	
	@Override
	public void onAudioFocusChange(int focusChange) {
		if (player == null)
			InitializePlayer();
		switch (focusChange) {
			case AudioManager.AUDIOFOCUS_GAIN:
				if (playbackWithHSisInterrupted && !isHSConnected)
					return;

//				if (player == null)
//					InitializePlayer();
				
				//проверить возобновление проигрывания при переключении фокуса
				if (!player.isPlaying() && GetMediaPlayerState() == PlaybackStateCompat.STATE_PLAYING) {
					player.start();
				}
				
				player.setVolume(1.0f, 1.0f);
				playbackWithHSisInterrupted = false;
				break;
			case AudioManager.AUDIOFOCUS_LOSS:
				Pause();
				if (isHSConnected && player.isPlaying())
					playbackWithHSisInterrupted = true;
//				Stop();
				break;
			case AudioManager.AUDIOFOCUS_LOSS_TRANSIENT:
				//We have lost focus for a short time, but likely to resume so pause
				Pause();
				if (isHSConnected && player.isPlaying())
					playbackWithHSisInterrupted = true;
				break;
			case AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK:
				//We have lost focus but should till play at a muted 10% volume
				if (player.isPlaying())
					player.setVolume(.1f, .1f);
				break;
		}
	}
	
	public MediaPlayerService() {
	}
	
	private void InitMediaSession() {
		try {
			if (mediaSessionCompat == null) {
				Intent nIntent = new Intent(getApplicationContext(), MainActivity.class);
				PendingIntent pIntent = getActivity(getApplicationContext(), 0, nIntent, 0);
				
				remoteComponentName = new ComponentName(getApplication().getPackageName(), new RemoteControlReceiver().ComponentName());
				
				mediaSessionCompat = new MediaSessionCompat(getApplicationContext(), "StreamingAudio", remoteComponentName, null);
				mediaControllerCompat = new MediaControllerCompat(getApplicationContext(), mediaSessionCompat.getSessionToken());
			}
			
			mediaSessionCompat.setActive(true);
			mediaSessionCompat.setCallback(new MediaSessionCallback((MediaPlayerServiceBinder) binder));
			mediaSessionCompat.setFlags(MediaSessionCompat.FLAG_HANDLES_MEDIA_BUTTONS | MediaSessionCompat.FLAG_HANDLES_TRANSPORT_CONTROLS);
		} catch (Exception ex) {
		}
	}
	
	
	IBinder binder;
	
	@Override
	public IBinder onBind(Intent intent) {
		binder = new MediaPlayerServiceBinder(this);
		return binder;
	}
	
	@Override
	public boolean onUnbind(Intent intent) {
		StopNotification();
		stopForeground(true);
		ReleaseWifiLock();
		if (wl != null && wl.isHeld())
			wl.release();
		UnregisterMediaSessionCompat();
		return super.onUnbind(intent);
	}
	
	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		MediaButtonReceiver.handleIntent(mediaSessionCompat, intent);
		trackDataAccess = new TrackDataAccess(getApplicationContext());
		DeviceID = prefManager.getDeviceId(); // PreferenceManager.getDefaultSharedPreferences(this).getString("DeviceID", "");
		UserID = DeviceID;//PreferenceManager.getDefaultSharedPreferences(this).getString("UserID", "");
//        if((flags&START_FLAG_RETRY)==0){
//        }
//        else{
//        }
		String action = intent.getAction();
		
		switch (action) {
			case ActionPlay:
				Play();
				break;
			case ActionPause:
				Pause();
				break;
			case ActionNext:
				Next();
				break;
			case ActionStop:
				Stop();
				break;
			case ActionTogglePlayback:
				if (player == null) {
					Play();
					break;
				}
				//	return Service.START_REDELIVER_INTENT;
				
				if (player.isPlaying())
					Pause();
				else
					Play();
				break;
			case ActionUpdateNotification:
//				{
////				mMediaNotificationManager.startNotification(track.getAsString("title"), track.getAsString("artist"));
//
//				Notification notification = createNotification();
//				if (notification != null) {
//					mNotificationManager.notify(NOTIFICATION_ID, notification);
//				}
//			}
				StartNotification();
				break;
//            case ActionSaveCurrentPosition: SaveLastPosition();
//                android.os.Process.killProcess(android.os.Process.myPid());
//                break;
		}
		return Service.START_NOT_STICKY;
//		return Service.START_REDELIVER_INTENT;
	}
	
	private void InitializePlayer() {
		utilites.SendInformationTxt(getApplicationContext(), "Play(): InitializePlayer");
		player = new MediaPlayer();
		
		//Tell our player to stream music
		player.setAudioStreamType(AudioManager.STREAM_MUSIC);
		//Wake mode will be partial to keep the CPU still running under lock screen
		player.setWakeMode(getApplicationContext(), 1);// WakeLockFlags.Partial=1
		player.setOnBufferingUpdateListener(new MediaPlayer.OnBufferingUpdateListener() {
			@Override
			public void onBufferingUpdate(MediaPlayer mediaPlayer, int i) {
			}
		});
		
		player.setOnCompletionListener(this);
		player.setOnErrorListener(new MediaPlayer.OnErrorListener() {
			@Override
			public boolean onError(MediaPlayer mediaPlayer, int what, int extra) {
				switch (what) {
					case MediaPlayer.MEDIA_ERROR_UNKNOWN:
						if (player != null) {
							if (player.isPlaying())
								player.stop();
							isPreparing = false;
							player.reset();
							player.release();
							player = null;
						}
						//new APICalls(getApplicationContext()).SetIsCorrect(DeviceID, track.getAsString("id"), 0);
						new RdevApiCalls(getApplicationContext()).SetIsCorrect(track.getAsString("id"), false);
						new TrackToCache(getApplicationContext()).DeleteTrackFromCache(track);
						utilites.SendInformationTxt(getApplicationContext(), "Битый трек удален и помечен");
						PlayNext();
						sendBroadcast(new Intent(ActionButtonImgUpdate));
						Toast.makeText(getApplicationContext(), "MediaPlayer error " + what + " (" + extra + ")", Toast.LENGTH_SHORT).show();
						Log.e(TAG, "unknown media error what=[" + what + "] extra=[" + extra + "]");
						break;
					case MediaPlayer.MEDIA_ERROR_SERVER_DIED:
						Log.e(TAG, "Streaming source Server died what=[" + what + "] extra=[" + extra + "]");
						break;
					default:
						Log.e(TAG, "Default Problems what=[" + what + "] extra=[" + extra + "]");
				}
				return false;
			}
		});
		player.setOnPreparedListener(this);
		
		//Блокирует отключение 3G когда экран не активен
		wl = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK | PowerManager.ACQUIRE_CAUSES_WAKEUP, "NVXOwnRadio:NVXOwnRadioPlayback");
		if (!wl.isHeld())
			wl.acquire();

	}
	
	public void onCompletion(MediaPlayer mediaPlayer) {
		//Событие возникает при дослушивании трека до конца, плеер останавливается, отправляется статистика прослушивания
		//затем вызвается функция Play(), запускающая проигрывание следующего трека
		//Сохраняем информацию о прослушивании в локальную БД.
//		if(GetDuration() < minTrackDuration){
		if (track != null && track.get("id").equals("zero_track")) {
			if (trackDataAccess.GetExistTracksCount() > 0)
				PlayNext();
			else {
				if (player != null) {
					if (player.isPlaying())
						player.stop();
					isPreparing = false;
					player.reset();
					player.release();
					player = null;
				}
				UpdatePlaybackState(PlaybackStateCompat.STATE_STOPPED);
				StopNotification();
			}
			return;
		}
		if (!trackDataAccess.CheckEnoughTimeFromStartPlaying(track.getAsString("id"))) {
			new TrackToCache(getApplicationContext()).DeleteTrackFromCache(track);
			if (GetDuration() < minTrackDuration) {
//				new APICalls(getApplicationContext()).SetIsCorrect(DeviceID, track.getAsString("id"), 0);
			}
			else {
//				new APICalls(getApplicationContext()).SetIsCorrect(DeviceID, track.getAsString("id"), 2);
			}
			try {
				utilites.SendInformationTxt(getApplicationContext(), "track id: " + track.get("id") + ", \n track duration (server): " + track.get("length") + ", \n track duration (player): " + GetDuration());
			} catch (Exception ex) {
				
			}
			utilites.SendInformationTxt(getApplicationContext(), "Битый трек удален и помечен");
			PlayNext();
		} else {
			int listedTillTheEnd = 1;
			SaveHistory(listedTillTheEnd, track);
			prefManager.setPrefItemInt("listenTracksCountInLastVersion", prefManager.getPrefItemInt("listenTracksCountInLastVersion", 0) + 1);
			
			PlayNext();
			utilites.SendInformationTxt(getApplicationContext(), "Completion playback");
			Intent historySenderIntent = new Intent(this, RequestAPIService.class);
			historySenderIntent.setAction(ACTION_SENDHISTORY);
			historySenderIntent.putExtra(EXTRA_DEVICEID, DeviceID);

			if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
				startForegroundService(historySenderIntent);
			}else{
				startService(historySenderIntent);
			}


			SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
			int currentListens = sp.getInt("rateListensCount", 0);
			sp.edit().putInt("rateListensCount", currentListens + 1).apply();
			Intent mainIntent = new Intent(ActionCheckListensCount);
			getApplicationContext().sendBroadcast(mainIntent);
		}
	}
	
	public void InitiMedia() {
		if (player == null)
			InitializePlayer();
		
		if (mediaSessionCompat == null)
			InitMediaSession();
	}
	
	public void Play() {

		AudioManager mAudioManager = (AudioManager) this.getSystemService(Context.AUDIO_SERVICE);

		if (mAudioManager.isMusicActive()) {
			int focusResult = audioManager.requestAudioFocus(this, AudioManager.STREAM_MUSIC, AudioManager.AUDIOFOCUS_GAIN_TRANSIENT);
			Intent i = new Intent("com.android.music.musicservicecommand");

			i.putExtra("command", "pause");
			getApplicationContext().sendBroadcast(i);
		}

		Log.d(TAG, "Play(): ");
		playbackWithHSisInterrupted = false;


		
		//todo:add code
		
		if (player != null && GetMediaPlayerState() == PlaybackStateCompat.STATE_PAUSED) {
			int focusResult = audioManager.requestAudioFocus(this, AudioManager.STREAM_MUSIC, AudioManager.AUDIOFOCUS_GAIN_TRANSIENT);
			if (focusResult == AudioManager.AUDIOFOCUS_REQUEST_FAILED) {
				utilites.SendInformationTxt(getApplicationContext(), "Не удалось получить аудиофокус");
				return;
			}
			
			player.start();

			utilites.SendInformationTxt(getApplicationContext(), "Play(): start");

//			UpdatePlaybackState(PlaybackStateCompat.STATE_PLAYING);
//			StartNotification();
//			UpdateButtonPlayPauseImg();
			UpdateMediaMetadataCompat();
			sp.edit().putString("current_playing_track_id", TrackID).commit();
		}
		
		if (player == null) {
			InitializePlayer();
			player.setVolume(0.1f, 0.1f);
			sp.edit().putString("current_playing_track_id", TrackID).commit();
		}
		if (mediaSessionCompat == null)
			InitMediaSession();
			sp.edit().putString("current_playing_track_id", TrackID).commit();
		
		if (player.isPlaying()) {
			UpdatePlaybackState(PlaybackStateCompat.STATE_PLAYING);
			sp.edit().putString("current_playing_track_id", TrackID).commit();
			return;
		}

		
		try {

//			if (!new Utilites().CheckCountTracksAndDownloadIfNotEnought(getApplicationContext(), DeviceID))
//				return;
//			SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(this);
//			startPosition = settings.getInt("LastPosition", 0);
//			startTrackID = settings.getString("LastTrackID", "");
			if (getTrackForPlayFromDB()) {
//				player.setDataSource(getApplicationContext(), Uri.parse(trackURL));
				player.setDataSource(trackURL);
				Intent updateInfoIntent = new Intent(ActionTrackInfoUpdate);
				Intent updateProgressIntent = new Intent(ActionProgressBarUpdate);


				getApplicationContext().sendBroadcast(updateInfoIntent);
				getApplicationContext().sendBroadcast(updateProgressIntent);
				sp.edit().putString("current_playing_track_id", TrackID).commit();
			} else {
				AssetFileDescriptor afd = getApplicationContext().getResources().openRawResourceFd(R.raw.zero_track);
				player.setDataSource(afd.getFileDescriptor(), afd.getStartOffset(), afd.getDeclaredLength());
				track = new ContentValues();
				track.put("id", "zero_track");
				track.put("title", " ");
				track.put("artist", " ");
				player.prepareAsync();
				utilites.CheckCountTracksAndDownloadIfNotEnought(getApplicationContext(), DeviceID);
				return;
			}
			
			int focusResult = audioManager.requestAudioFocus(this, AudioManager.STREAM_MUSIC, AudioManager.AUDIOFOCUS_GAIN_TRANSIENT);
			if (focusResult != AudioManager.AUDIOFOCUS_REQUEST_GRANTED) {
				audioManager.requestAudioFocus(this, AudioManager.STREAM_MUSIC, AudioManager.AUDIOFOCUS_GAIN_TRANSIENT);
//				new Utilites().SendInformationTxt(getApplicationContext(), "Не удалось получить аудиофокус");
//				return;
			}
			
			UpdatePlaybackState(PlaybackStateCompat.STATE_BUFFERING);
			Log.d(TAG, "Play(): UpdatePlaybackState");
			
			player.prepareAsync();
			Log.d(TAG, "Play(): prepareAsync");

//			if(GetDuration()<minTrackDuration) {
//				new Utilites().SendInformationTxt(getApplicationContext(), "track id: " + track.get("id") + ", \n track duration (server): " + track.get("length") + ", \n track duration (player): " + player.getDuration());
//				new TrackToCache(getApplicationContext()).DeleteTrackFromCache(track);
//				Play();
//				return;
//			}

//			if(GetDuration()> minTrackDuration && player != null)
			UpdatePlaybackState(PlaybackStateCompat.STATE_PLAYING);
			
			//записываем время начала проигрывания трека
			new TrackDataAccess(getApplicationContext()).SetTimeAndCountStartPlayback(track);
			
			utilites.SendInformationTxt(getApplicationContext(), "SetTimeAndCountStartPlayback for track " + track.getAsString("id"));
			
			wl = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK | PowerManager.ACQUIRE_CAUSES_WAKEUP, "NVXOwnRadio:NVXOwnRadioPlayback");
			if (!wl.isHeld())
				wl.acquire();

//			AcquireWifiLock();
			UpdateMediaMetadataCompat();
//			if (GetMediaPlayerState() != PlaybackStateCompat.STATE_BUFFERING) {
//				StartNotification();
//				UpdateButtonPlayPauseImg();
//			}
			
			Intent progressIntent = new Intent(ActionProgressBarUpdate);
			sendBroadcast(progressIntent);
			Log.d(TAG, "Play(): sendBroadcast");
		} catch (Exception ex) {
			utilites.SendInformationTxt(getApplicationContext(), "Error in Play(): " + ex.getLocalizedMessage());
			//player.reset();
//			new Utilites().SendInformationTxt(getApplicationContext(), "track id: " + track.get("id") + ", \n track duration (server): " + track.get("length") + ", \n track duration (player): " + player.getDuration());
//			if(player.getDuration() < minTrackDuration){
//				new TrackToCache(getApplicationContext()).DeleteTrackFromCache(track);
//				Play();
//			}
			if (player != null) {
				if (player.isPlaying())
					player.stop();
				isPreparing = false;
				player.reset();
				player.release();
				player = null;
			}
			UpdatePlaybackState(PlaybackStateCompat.STATE_STOPPED);
			if (new File(trackURL).length() <= 0) {
				new TrackToCache(getApplicationContext()).DeleteTrackFromCache(track);
				Play();
			}
			track = null;
			return;
		}
		
		Intent downloaderIntent = new Intent(this, LongRequestAPIService.class);

		downloaderIntent.setAction(ACTION_GETNEXTTRACK);
		downloaderIntent.putExtra(EXTRA_DEVICEID, DeviceID);
		downloaderIntent.putExtra(EXTRA_COUNT, 10);//Увеличила количество треков для загрузки с 3 до 10 - задача 12850
		try{
			if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
				this.startForegroundService(downloaderIntent);
			}else{
				this.startService(downloaderIntent);
			}
		}catch (Exception ex){
			Log.d("DownloaderException", ex.getLocalizedMessage());
		}

	}
	
	
	public void SetVolume(float volume) {
		if (player != null)
			player.setVolume(volume, volume);
	}
	
	
	public void Pause() {
		if (player == null)
			return;
		
		if (player.isPlaying()) {
			player.pause();
			sp.edit().putString("current_playing_track_id", "").commit();

		}
		UpdatePlaybackState(PlaybackStateCompat.STATE_PAUSED);
	}
	
	public void Stop() {
		
		
		if (player == null)
			return;
		
		SaveLastPosition();
		
		if (player.isPlaying()) {
			sp.edit().putString("current_playing_track_id", "").commit();
			player.stop();
			
		}
//		NotificationManager notificationManager = (NotificationManager) getApplicationContext().getSystemService(Context.NOTIFICATION_SERVICE);
//		notificationManager.cancel( 1 );
//		Intent intent = new Intent( getApplicationContext(), MediaPlayerService.class );
//		stopService( intent );

//		try{
//			BroadcastReceiver headSetReceiver = new MusicBroadcastReceiver();
//			BroadcastReceiver remoteControlReceiver = new RemoteControlReceiver();
//			unregisterReceiver(headSetReceiver);
//			unregisterReceiver(remoteControlReceiver);
//		}catch (Exception ex){
//			Log.d(TAG, " " + ex.getLocalizedMessage());
//		}
		
		try {
			UpdatePlaybackState(PlaybackStateCompat.STATE_STOPPED);
//			UpdateButtonPlayPauseImg();
//			player.reset();
			isPreparing = false;
			player.release();
			player = null;
//			StartNotification();
//			super.stopForeground(true);
			ReleaseWifiLock();
			if (wl != null && wl.isHeld())
				wl.release();
//			UnregisterMediaSessionCompat();
		} catch (Exception ex) {
			Log.d(TAG, " " + ex.getLocalizedMessage());
		}
	}
	
	public void Next() {


//		if(new TrackDataAccess(getApplicationContext()).GetExistTracksCount() <3){
//			Intent progressIntent = new Intent(ActionProgressBarFirstTracksLoad);
//			getApplicationContext().sendBroadcast(progressIntent);
//			return;
//		}
		if (track != null && track.get("id").equals("zero_track")) {
			if (trackDataAccess.GetExistTracksCount() > 0)
				PlayNext();
			else {
				isAutoplay = true;
				if (player != null) {
					if (player.isPlaying())
						player.stop();
					isPreparing = false;
					player.reset();
					player.release();
					player = null;
				}
				UpdatePlaybackState(PlaybackStateCompat.STATE_STOPPED);
				StopNotification();
			}
			return;
		}
		
		if (new TrackDataAccess(getApplicationContext()).GetExistTracksCount() <= 0) {
			Intent progressIntent = new Intent(ActionProgressBarFirstTracksLoad);
			getApplicationContext().sendBroadcast(progressIntent);
			return;
		}

//		if(new TrackDataAccess(getApplicationContext()).GetExistTracksCount() <=3){
//			Intent progressIntent = new Intent(ActionProgressBarFirstTracksLoad);
//			getApplicationContext().sendBroadcast(progressIntent);
//		}
		//Сохраняем информацию о прослушивании в локальную БД.
		if (track != null) {
			int listedTillTheEnd = -1;
			SaveHistory(listedTillTheEnd, track);
			// Удаление пропущенного трека
			new TrackToCache(getApplicationContext()).DeleteTrackFromCache(track);
			utilites.SendInformationTxt(getApplicationContext(), "Skip track " + track.getAsString("id") + " to next");
			PlayNext();
		}
		Intent historySenderIntent = new Intent(this, RequestAPIService.class);
		historySenderIntent.setAction(ACTION_SENDHISTORY);
		historySenderIntent.putExtra(EXTRA_DEVICEID, DeviceID);
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
			startForegroundService(historySenderIntent);
		}else{
			startService(historySenderIntent);
		}
	}
	
	private void SaveHistory(int listedTillTheEnd, ContentValues trackInstance) {
		try {
			//история прослушивания
			//Добавление истории прослушивания в локальную БД
			if (trackInstance.getAsString("id") != null && !trackInstance.getAsString("id").isEmpty()) {
				SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd\'T\'HH:mm:ss");
				dateFormat.setTimeZone(TimeZone.getTimeZone("UTC"));//Time format UTC+0
				String currentDateTime = dateFormat.format(new Date()); // Find todays date
				ContentValues history = new ContentValues();
				history.put("trackid", trackInstance.getAsString("id"));
				history.put("userid", UserID);
				history.put("datetimelisten", currentDateTime);
				history.put("islisten", listedTillTheEnd);
				HistoryDataAccess historyDataAccess = new HistoryDataAccess(getApplicationContext());
				historyDataAccess.SaveHistoryRec(history);//сохраняет информацию в history для последующей отправки на сервер
			}
		} catch (Exception ex) {
			ex.getLocalizedMessage();
		}
	}
	
	private Map<String, String> createMap(String id) {
		Map<String, String> obj = new HashMap<String, String>();
		obj.put("id", id);
		obj.put("deviceid", null);
		
		return obj;
	}
	
	
	public boolean DownloadFile(String id) {
		Boolean isDownload = false;
		TrackDataAccess trackInfo = new TrackDataAccess(getApplicationContext());
		try {
			Map<String, String> obj = createMap(id);
			String optimizeStatus = prefManager.getPrefItem(OPTIMIZE_STATUS, OPTIMIZE_ENABLED);
			int cachedTrackCount = trackInfo.GetExistTracksCount();
			if(optimizeStatus.equals(OPTIMIZE_ENABLED) && cachedTrackCount > 50){
				return isDownload;
			}else{
				isDownload = new RdevGetTrack(getApplicationContext()).execute(obj).get();
				if(isDownload == null){
					RdevApiCalls rdevApiCalls = new RdevApiCalls(getApplicationContext());
					rdevApiCalls.GetAuthToken();
					DownloadFile(id);
				}
			}
		} catch (InterruptedException e) {
			e.printStackTrace();
		} catch (ExecutionException e) {
			e.printStackTrace();
		}
		
		return isDownload;
	}
	
	
	public void PlayAlarmTrack(String id, String url) {
		if (player != null && player.isPlaying()) {
			player.stop();
		}
		player = null;
		
		track = trackDataAccess.GetTrackById(id);
		if (track == null) {
			SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
			
			track = new ContentValues();
			track.put("id", id);
			track.put("title", prefs.getString(CURRENT_TRACK_TITLE, "Track"));
			track.put("artist", prefs.getString(CURRENT_TRACK_ARTIST, "Artist"));
		}
		
		TrackID = id;
		trackURL = url;
		
		Play();
	}
	
	public boolean PlayNewTrack(String id, String title, String artist) {
		if (player != null && player.isPlaying()) {
			player.stop();
			player = null;
		}
		
		track = trackDataAccess.GetTrackById(id);
		
		if (track == null) {
			
			boolean isDownload = DownloadFile(id);
			
			if (isDownload) {
				track = trackDataAccess.GetTrackById(id);
				
				track.put("title", title);
				track.put("artist", artist);
				
				trackDataAccess.UpdateTrack(track);
				
			} else {
				Intent intent = new Intent(ActionNotFoundTrack);
				sendBroadcast(intent);
			}
		}
		
		if (track != null) {
			startPosition = 0;
			TrackID = track.getAsString("id");
			trackURL = track.getAsString("trackurl");
			
			if (track.getAsString("title").equals("Track")) {
				track.put("title", title);
				track.put("artist", artist);
				trackDataAccess.UpdateTrack(track);
			}
			
			File file = new File(trackURL);
			if (!file.exists()) {
				boolean isDownload = DownloadFile(id);
				if (!isDownload) {
					return false;
				}
			}
			return true;
		}
		
		return false;
	}
	
	
	private boolean getTrackForPlayFromDB() {
		boolean flagFindTrack = false;
		if (track != null)
			return true;
		do {
			track = trackDataAccess.GetMostNewNotListenTrack();
			if (track == null){
				track = trackDataAccess.GetOldListenedTrack();
//			Intent i = new Intent(ActionTrackInfoUpdate)
			};
//			getApplicationContext().sendBroadcast(i);
			if (track != null) {
				startPosition = 0;
				utilites.SendInformationTxt(getApplicationContext(), "getTrackForPlayFromDB, id=" + track.getAsString("id"));
				FlagDownloadTrack = false;
				TrackID = track.getAsString("id");
				trackURL = track.getAsString("trackurl");
				if (!new File(trackURL).exists()) {
					trackDataAccess.DeleteTrackFromCache(track);
				} else
					flagFindTrack = true;
			} else
				return false;
		} while (!flagFindTrack);
		return true;
//			return true;
//		}else
//			return false;
	}
	
	private void AcquireWifiLock() {
		if (wifiLock == null) {
			wifiLock = wifiManager.createWifiLock(WifiManager.WIFI_MODE_FULL, "OwnRadioWiFiLock");
		}
		wifiLock.acquire();
	}
	
	private void ReleaseWifiLock() {
		if (wifiLock == null)
			return;
		
		wifiLock.release();
		wifiLock = null;
	}
	
	public void onPrepared(MediaPlayer mp) {
		//Mediaplayer is prepared start track playback
//		if (startTrackID.equals(TrackID)) {
//			player.seekTo(startPosition);
//			startPosition = 0;
//		}
		mp.start();
		duration = player.getDuration();
		//Попытка вызова некоторых функций (таких как getCurrentPosition(), getDuration() и др)
		//до срабатывания onPrepared вызывает исключение и событие onError().
		//Для избежания этого исплоьзуется флаг isPreparing
		isPreparing = true;
		utilites.SendInformationTxt(getApplicationContext(), "Start playback " + track.getAsString("id"));
		UpdatePlaybackState(PlaybackStateCompat.STATE_PLAYING);
		isAutoplay = false;
		player.seekTo(startPosition);
		Log.i(TAG, "startposition=" + startPosition + " ,currP=" + GetPosition());
	}
	
	public int GetPosition() {
		if (!isPreparing)
			return -1;
		if (player == null
				|| (GetMediaPlayerState() != PlaybackStateCompat.STATE_PLAYING
				&& GetMediaPlayerState() != PlaybackStateCompat.STATE_PAUSED))
			return -1;
		else
			return player.getCurrentPosition();
	}
	
	public int GetDuration() {
		return duration;
//			if (player == null
//					|| (GetMediaPlayerState() != PlaybackStateCompat.STATE_PLAYING
//					&& GetMediaPlayerState() != PlaybackStateCompat.STATE_PAUSED))
//				return 0;
//			else
//				return player.getDuration();
	}
	
	private void UpdatePlaybackState(int state) {
		if (mediaSessionCompat == null || player == null) {
			UpdateButtonPlayPauseImg();
			return;
		}
		try {
			PlaybackStateCompat.Builder stateBuilder = new PlaybackStateCompat.Builder()
					.setActions(
							PlaybackStateCompat.ACTION_PAUSE |
									PlaybackStateCompat.ACTION_PLAY |
									PlaybackStateCompat.ACTION_PLAY_PAUSE |
									PlaybackStateCompat.ACTION_SKIP_TO_NEXT |
									PlaybackStateCompat.ACTION_STOP
					)
					.setState(state, GetPosition(), 1.0f, SystemClock.elapsedRealtime());
			
			mediaSessionCompat.setPlaybackState(stateBuilder.build());
			
			if (state != PlaybackStateCompat.STATE_BUFFERING) {
				StartNotification();

//					Notification notification = createNotification();
//					if (notification != null) {
//						mNotificationManager.notify(NOTIFICATION_ID, notification);
//					}

//				mMediaNotificationManager.startNotification(track.getAsString("title"), track.getAsString("artist"));
				UpdateButtonPlayPauseImg();
			}
		} catch (Exception ex) {
			ex.printStackTrace();
			Log.d(TAG, " " + ex.getLocalizedMessage());
		}
	}
	
	private void UpdateButtonPlayPauseImg() {
		Intent intent = new Intent(ActionButtonImgUpdate);
		sendBroadcast(intent);
	}
	
	//кастомное уведомление
	private void StartNotification() {
		android.support.v7.app.NotificationCompat.MediaStyle style = new android.support.v7.app.NotificationCompat.MediaStyle();
		style.setMediaSession(mediaSessionCompat.getSessionToken());
		
		PendingIntent pendingIntent = PendingIntent.getActivity(getApplicationContext(), 0, new Intent(this, MainActivity.class), PendingIntent.FLAG_UPDATE_CURRENT);
		PendingIntent pendingCancelIntent = PendingIntent.getService(getApplicationContext(), 1, new Intent(this, MediaPlayerService.class), PendingIntent.FLAG_CANCEL_CURRENT);
		style.setShowCancelButton(true);
		style.setCancelButtonIntent(pendingCancelIntent);
		String trackTitle;
		String trackArtist;
		try {
			trackTitle = (track.getAsString("title") == null) ? "Track" : track.getAsString("title");
			trackArtist = (track.getAsString("artist") == null) ? "Artist" : track.getAsString("artist");
			
		} catch (Exception ex) {
			trackTitle = "Track";
			trackArtist = "Artist";
			Log.d(TAG, " " + ex.getLocalizedMessage());
		}
		
		Intent intent = new Intent(getApplicationContext(), MediaPlayerService.class);
		intent.setAction(ActionPlay);
		RemoteViews contentView = new RemoteViews(getApplicationContext().getPackageName(), R.layout.notification);
		RemoteViews contentViewBig = new RemoteViews(getApplicationContext().getPackageName(), R.layout.notification2);

//		contentView.setViewVisibility(R.id.viewsIcon, View.VISIBLE);
		
		Intent mainIntent = new Intent(this, MainActivity.class);
		mainIntent.setAction(Intent.ACTION_MAIN);
		mainIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
		PendingIntent pmainIntent = PendingIntent.getActivity(getApplicationContext(), 0,
				mainIntent, 0);
		
		contentView.setTextViewText(R.id.viewsTitle, trackTitle);
		contentView.setTextViewText(R.id.viewsArtist, trackArtist);
		Intent playIntent = new Intent(this, MediaPlayerService.class);
		if (GetMediaPlayerState() == PlaybackStateCompat.STATE_PLAYING) {
			playIntent.setAction(ActionPause);
			contentView.setImageViewResource(R.id.viewsPlayPause, R.drawable.btn_pause);
			contentViewBig.setImageViewResource(R.id.viewsPlayPause, R.drawable.btn_pause);
			
		} else {
			playIntent.setAction(ActionPlay);
			contentView.setImageViewResource(R.id.viewsPlayPause, R.drawable.btn_play);
			contentViewBig.setImageViewResource(R.id.viewsPlayPause, R.drawable.btn_play);
		}
		PendingIntent pplayIntent = PendingIntent.getService(this, 0, playIntent, 0);
		
		Intent nextIntent = new Intent(this, MediaPlayerService.class);
		nextIntent.setAction(ActionNext);
		PendingIntent pnextIntent = PendingIntent.getService(this, 0, nextIntent, 0);
		
		contentView.setOnClickPendingIntent(R.id.viewsNext, pnextIntent);
		contentViewBig.setOnClickPendingIntent(R.id.viewsNext, pnextIntent);
		contentView.setOnClickPendingIntent(R.id.viewsPlayPause, pplayIntent);
		contentViewBig.setOnClickPendingIntent(R.id.viewsPlayPause, pplayIntent);
		
		
		contentViewBig.setTextViewText(R.id.viewsTitle, trackTitle);
		contentViewBig.setTextViewText(R.id.viewsArtist, trackArtist);
		
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
			createChannels();
			Notification.Builder builder = new Notification.Builder(this, "ownradio_channel")
					.setSmallIcon(R.drawable.ic_notification)
					.setContentIntent(pendingIntent)
					.setCustomContentView(contentView)
					.setCustomBigContentView(contentViewBig)
					.setContentTitle(trackTitle)
					.setContentText(trackArtist)
					.setShowWhen(false)
					.setVisibility(Notification.VISIBILITY_PUBLIC)
					.setAutoCancel(false);
//					.setChannelId("ownradio_channel");
			if (GetMediaPlayerState() == PlaybackStateCompat.STATE_PLAYING)
				builder.setOngoing(true);
			else
				builder.setOngoing(false);
//        style.setShowActionsInCompactView(0,1,2);
			NotificationManagerCompat.from(this).notify(NotificationId, builder.build());
		} else {
			
			android.support.v4.app.NotificationCompat.Builder builder = new NotificationCompat.Builder(this)
					.setSmallIcon(R.drawable.ic_notification)
					.setContentIntent(pendingIntent)
					.setCustomContentView(contentView)
					.setCustomBigContentView(contentViewBig)
					.setContentTitle(trackTitle)
					.setContentText(trackArtist)
					.setShowWhen(false)
					.setPriority(NotificationCompat.PRIORITY_HIGH)
					.setVisibility(NotificationCompat.VISIBILITY_PUBLIC);
			if (GetMediaPlayerState() == PlaybackStateCompat.STATE_PLAYING)
				builder.setOngoing(true);
			else
				builder.setOngoing(false);
//        style.setShowActionsInCompactView(0,1,2);
			NotificationManagerCompat.from(this).notify(NotificationId, builder.build());
			
			
			if (GetMediaPlayerState() == PlaybackStateCompat.STATE_PLAYING)
				builder.setOngoing(true);
			else
				builder.setOngoing(false);
//        style.setShowActionsInCompactView(0,1,2);
			NotificationManagerCompat.from(this).notify(NotificationId, builder.build());
		}
	}
	
	public void createChannels() {
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
			
			// create android channel
			NotificationChannel androidChannel = new NotificationChannel("ownradio_channel",
					"Воспроизведение", NotificationManager.IMPORTANCE_LOW);
			// Sets whether notifications posted to this channel should display notification lights
			androidChannel.enableLights(false);
			// Sets whether notification posted to this channel should vibrate.
			androidChannel.enableVibration(false);
			// Sets the notification light color for notifications posted to this channel
			androidChannel.setLightColor(Color.GREEN);
			// Sets whether notifications posted to this channel appear on the lockscreen or not
			androidChannel.setLockscreenVisibility(Notification.VISIBILITY_PUBLIC);

			androidChannel.setSound(null, null);

			androidChannel.setVibrationPattern(new long[]{ 0 });


			NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
			notificationManager.createNotificationChannel(androidChannel);
			
		}
	}
	
	
	//стандартное уведомление
	private Notification createNotification() {
//		mNotificationManager = NotificationManagerCompat.from(this);
		int mNotificationColor = ResourceHelper.getThemeColor(this, R.attr.colorPrimary,
				Color.DKGRAY);
		String pkg = getApplicationContext().getPackageName();
		
		android.support.v7.app.NotificationCompat.MediaStyle style = new android.support.v7.app.NotificationCompat.MediaStyle();
		style.setMediaSession(mediaSessionCompat.getSessionToken());
		style.setShowCancelButton(true);
		PendingIntent pendingIntent = PendingIntent.getActivity(getApplicationContext(), 0, new Intent(this, MainActivity.class), PendingIntent.FLAG_UPDATE_CURRENT);
		
		PendingIntent pendingCancelIntent = PendingIntent.getService(getApplicationContext(), 1, new Intent(this, MediaPlayerService.class), PendingIntent.FLAG_CANCEL_CURRENT);
		style.setCancelButtonIntent(pendingCancelIntent);
		
		
		Intent nextIntent = new Intent(this, MediaPlayerService.class);
		nextIntent.setAction(ActionNext);
		PendingIntent mNextIntent = PendingIntent.getService(this, 0, nextIntent, 0);


//		PendingIntent mNextIntent = PendingIntent.getBroadcast(this, REQUEST_CODE,
//				new Intent(ActionNext).setPackage(pkg), PendingIntent.FLAG_CANCEL_CURRENT);
		
		String trackTitle;
		String trackArtist;
		try {
			trackTitle = (track.getAsString("title") == null) ? "Track" : track.getAsString("title");
			trackArtist = (track.getAsString("artist") == null) ? "Artist" : track.getAsString("artist");
			
		} catch (Exception ex) {
			trackTitle = "Track";
			trackArtist = "Artist";
			Log.d(TAG, " " + ex.getLocalizedMessage());
		}
		
		NotificationCompat.Builder notificationBuilder = new NotificationCompat.Builder(this);
		addPlayPauseAction(notificationBuilder);
		
		
		Bitmap art = BitmapFactory.decodeResource(this.getResources(),
				R.drawable.ic_default_art);
		
		
		// If skip to next action is enabled
		notificationBuilder.addAction(R.drawable.btn_ic_next,
				this.getString(R.string.label_next), mNextIntent);
		
		
		notificationBuilder
				.setStyle(new NotificationCompat.MediaStyle()
						.setShowActionsInCompactView(
								new int[]{0, 1})  // show only play/pause snd next buttons in compact view
						.setMediaSession(mediaSessionCompat.getSessionToken()))
				.setSmallIcon(R.drawable.ic_notification)
				.setColor(mNotificationColor)
				.setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
				.setUsesChronometer(true)
				.setContentIntent(pendingIntent)
				.setContentTitle(trackTitle)
				.setContentText(trackArtist)
				.setLargeIcon(art)
				.setVibrate(new long[]{ 0 })
				.setSound(null);


//		if (GetMediaPlayerState() == PlaybackStateCompat.STATE_PLAYING
//				) {
//			notificationBuilder
////					.setWhen(System.currentTimeMillis() - mPlaybackState.getPosition())
//					.setShowWhen(true)
//					.setUsesChronometer(true);
//		} else {
//			LogHelper.d(TAG, "updateNotificationPlaybackState. hiding playback position");
		notificationBuilder
				.setWhen(0)
				.setShowWhen(false)
				.setUsesChronometer(false)
//					.setPriority(NotificationCompat.PRIORITY_HIGH)
				.setCategory(NotificationCompat.CATEGORY_TRANSPORT);
//		}
		
		// Make sure that the notification can be dismissed by the user when we are not playing:
		notificationBuilder.setOngoing(GetMediaPlayerState() == PlaybackStateCompat.STATE_PLAYING);
		return notificationBuilder.build();
		
	}
	
	private void addPlayPauseAction(NotificationCompat.Builder builder) {
//		String pkg = getApplicationContext().getPackageName();
//		PendingIntent mPauseIntent = PendingIntent.getBroadcast(this, REQUEST_CODE,
//				new Intent(ActionPause).setPackage(pkg), PendingIntent.FLAG_CANCEL_CURRENT);
//		PendingIntent mPlayIntent = PendingIntent.getBroadcast(this, REQUEST_CODE,
//				new Intent(ActionPlay).setPackage(pkg), PendingIntent.FLAG_CANCEL_CURRENT);
		int icon;
		String label;
//		PendingIntent intent;
		
		Intent playIntent = new Intent(this, MediaPlayerService.class);
		if (GetMediaPlayerState() == PlaybackStateCompat.STATE_PLAYING) {
			label = getString(R.string.label_pause);
			icon = R.drawable.ic_pause;
//			intent = mPauseIntent;
			playIntent.setAction(ActionPause);
		} else {
			label = getString(R.string.label_play);
			icon = R.drawable.ic_play;
//			intent = mPlayIntent;
			playIntent.setAction(ActionPlay);
		}
		PendingIntent pplayIntent = PendingIntent.getService(this, 0, playIntent, 0);
		
		builder.addAction(new NotificationCompat.Action(icon, label, pplayIntent));
	}
	
	private int getNotificationIcon() {
		boolean useWhiteIcon = (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP);
		return useWhiteIcon ? R.drawable.logo_white : R.drawable.logo;
	}
	
	public void StopNotification() {
		NotificationManagerCompat nm = NotificationManagerCompat.from(getApplicationContext());
		nm.cancelAll();
	}
	
	public void PlayNext() {
		if (player != null) {
			if (player.isPlaying())
				player.stop();
			isPreparing = false;
			player.reset();
			player.release();
			player = null;
		}
		track = null;
		UpdatePlaybackState(PlaybackStateCompat.STATE_SKIPPING_TO_NEXT);

		Play();
	}
	
	private void UpdateMediaMetadataCompat() {
		if (mediaSessionCompat == null)
			return;
		
		MediaMetadataCompat.Builder builder = new MediaMetadataCompat.Builder();
		
		builder
				.putLong(MediaMetadataCompat.METADATA_KEY_DURATION, GetDuration())//track.getAsLong("length"))
				.putBitmap(MediaMetadataCompat.METADATA_KEY_DISPLAY_ICON, BitmapFactory.decodeResource(getResources(), R.drawable.icon))
//				.putBitmap(MediaMetadataCompat.METADATA_KEY_ALBUM_ART, BitmapFactory.decodeResource(getResources(), R.drawable.icon))
//				.putBitmap(MediaMetadataCompat.METADATA_KEY_ART, BitmapFactory.decodeResource(getResources(), R.drawable.icon))
				.putString(MediaMetadataCompat.METADATA_KEY_TITLE, track.getAsString("title"))
				.putString(MediaMetadataCompat.METADATA_KEY_DISPLAY_TITLE, track.getAsString("name"))
				.putString(MediaMetadataCompat.METADATA_KEY_DISPLAY_SUBTITLE, track.getAsString("artist"))
				.putString(MediaMetadataCompat.METADATA_KEY_ARTIST, track.getAsString("artist"))
				.putString(MediaMetadataCompat.METADATA_KEY_ALBUM_ARTIST, track.getAsString("artist"))
//				.putString(MediaMetadataCompat.METADATA_KEY_AUTHOR,  track.getAsString("artist"))
				.putString(MediaMetadataCompat.METADATA_KEY_ALBUM, "ownRadio");
		
		mediaSessionCompat.setMetadata(builder.build());
	}
	
	public class MediaSessionCallback extends MediaSessionCompat.Callback {
		private long mHeadsetDownTime = 0;
		private long mHeadsetUpTime = 0;
		
		private MediaPlayerServiceBinder mediaPlayerService;
		
		public MediaSessionCallback(MediaPlayerServiceBinder service) {
			mediaPlayerService = service;
		}
		
		@Override
		public boolean onMediaButtonEvent(Intent mediaButtonEvent) {
			KeyEvent event = mediaButtonEvent.getParcelableExtra(Intent.EXTRA_KEY_EVENT);
			if (event != null) {// && !isPlaying()) {
				int keyCode = event.getKeyCode();
				if (keyCode == KeyEvent.KEYCODE_MEDIA_PLAY || keyCode == KeyEvent.KEYCODE_MEDIA_PAUSE
						|| keyCode == KeyEvent.KEYCODE_HEADSETHOOK || keyCode == KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE) {
					long time = SystemClock.uptimeMillis();
					switch (event.getAction()) {
						case KeyEvent.ACTION_DOWN:
							if (event.getRepeatCount() <= 0)
								mHeadsetDownTime = time;
							break;
						case KeyEvent.ACTION_UP:
							if (time - mHeadsetUpTime <= DELAY_DOUBLE_CLICK) { // double click
								mHeadsetUpTime = time;
//									if(player.isPlaying())
								Next();
								return true;
							} else {
								mHeadsetUpTime = time;
								return false;
							}
					}
					return false;
				} else if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
					switch (keyCode) {
						case KeyEvent.KEYCODE_MEDIA_NEXT:
							mediaPlayerService.GetMediaPlayerService().Next();
							return true;
					}
				}
			}
			return false;
		}
		
		@Override
		public void onPause() {
			mediaPlayerService.GetMediaPlayerService().Pause();
			super.onPause();
		}
		
		@Override
		public void onPlay() {
			mediaPlayerService.GetMediaPlayerService().Play();
			super.onPlay();
		}
		
		@Override
		public void onSkipToNext() {
			mediaPlayerService.GetMediaPlayerService().Next();
			super.onSkipToNext();
		}
		
		@Override
		public void onStop() {
			mediaPlayerService.GetMediaPlayerService().Stop();
			super.onStop();
		}
	}
	
	public class MediaPlayerServiceBinder extends Binder {
		private MediaPlayerService service;
		
		public MediaPlayerServiceBinder(MediaPlayerService service) {
			this.service = service;
		}
		
		public MediaPlayerService GetMediaPlayerService() {
			return service;
		}
	}
	
	private void UnregisterMediaSessionCompat() {
		try {
			if (mediaSessionCompat != null) {
				mediaSessionCompat.release();
				mediaSessionCompat = null;
			}
		} catch (Exception ex) {
		}
	}
	
	@Override
	public void onDestroy() {
		super.onDestroy();
		
		if (player != null) {
			SaveLastPosition();
			if (player.isPlaying())
				player.stop();
			isPreparing = false;
			player.reset();
			player.release();
			player = null;
			
			Intent intent = new Intent(ActionButtonImgUpdate);
			sendBroadcast(intent);
			StopNotification();
			stopForeground(true);
			ReleaseWifiLock();
			if (wl != null && wl.isHeld())
				wl.release();
			UnregisterMediaSessionCompat();
		}
	}
	
	public void SaveLastPosition() {
		if (player != null) {
			prefManager.setPrefItem("LastTrackID", TrackID);
			prefManager.setPrefItemInt("LastPosition", GetPosition());
		}
	}
	
	public void FadeOut(float deltaTime) {
		player.setVolume(volume, volume);
		volume -= speed * deltaTime;
	}
	
	public void FadeIn(float deltaTime) {
		player.setVolume(volume, volume);
		volume += speed * deltaTime;
	}
	
	public void PreloadTrack() {
		utilites.SendInformationTxt(getApplicationContext(), "Вызов функции PreloadTrack");
		String tid = prefManager.getPrefItem("LastTrackID", "");
		utilites.SendInformationTxt(getApplicationContext(), "PreloadTrack: Получен сохраненный id трека: " + tid);
		if (tid != "") {
			int currentPosition = prefManager.getPrefItemInt("LastPosition", -1);
			if (currentPosition < 0)
				currentPosition = 0;
			utilites.SendInformationTxt(getApplicationContext(), "PreloadTrack: Получена сохраненная позиция проигрывания: " + currentPosition);
			track = trackDataAccess.GetTrackById(tid);
			
			if (track != null) {
				utilites.SendInformationTxt(getApplicationContext(), "PreloadTrack: Получена информация о треке из БД");
				utilites.SendInformationTxt(getApplicationContext(), "getPreloadTrack, id=" + track.getAsString("id"));
				FlagDownloadTrack = false;
				TrackID = track.getAsString("id");
				trackURL = track.getAsString("trackurl");
//				track.put("position", currentPosition);
				startPosition = currentPosition;
				
				if (!new File(trackURL).exists()) {
					utilites.SendInformationTxt(getApplicationContext(), "PreloadTrack: Ошибка! Файл не найден");
					trackDataAccess.DeleteTrackFromCache(track);
					track = null;
					return;
				}
			} else {
				utilites.SendInformationTxt(getApplicationContext(), "PreloadTrack: Ошибка! Информация о треке не найдена в БД");
			}
		}
	}

//	@Override
//	public void onTaskRemoved(Intent rootIntent){
//		StopNotification();
//		stopSelf();
//		super.onTaskRemoved(rootIntent);
//	}
}