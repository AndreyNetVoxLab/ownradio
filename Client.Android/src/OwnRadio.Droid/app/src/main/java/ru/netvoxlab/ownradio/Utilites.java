package ru.netvoxlab.ownradio;

import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.util.Log;
import android.widget.Toast;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;

import static ru.netvoxlab.ownradio.Constants.TAG;
import static ru.netvoxlab.ownradio.MainActivity.ActionCheckCountTracksAndDownloadIfNotEnought;
import static ru.netvoxlab.ownradio.MainActivity.ActionProgressBarFirstTracksLoad;
import static ru.netvoxlab.ownradio.MainActivity.ActionSendInfoTxt;
import static ru.netvoxlab.ownradio.RequestAPIService.ACTION_GETNEXTTRACK;
import static ru.netvoxlab.ownradio.RequestAPIService.ACTION_SENDLOGS;
import static ru.netvoxlab.ownradio.RequestAPIService.EXTRA_COUNT;
import static ru.netvoxlab.ownradio.RequestAPIService.EXTRA_DEVICEID;
import static ru.netvoxlab.ownradio.RequestAPIService.EXTRA_LOGFILEPATH;

/**
 * Created by a.polunina on 11.01.2017.
 */

public class Utilites {

	public void SendInformationTxt(Context mContext, String message){
		try {
			Log.d(TAG, message);
			Intent i = new Intent(ActionSendInfoTxt);
			i.putExtra("TEXTINFO", new SimpleDateFormat("dd/MM/yyyy HH:mm:ss").format(new Date()) + " " + message);
			mContext.sendBroadcast(i);
		}catch (Exception ex){
			ex.getLocalizedMessage();
		}
	}

	public boolean CheckCountTracksAndDownloadIfNotEnought(Context mContext, String DeviceId) {
		try {
			if (new TrackDataAccess(mContext).GetExistTracksCount() < 1) {
				if (new CheckConnection().CheckInetConnection(mContext)) {
					new Utilites().SendInformationTxt(mContext, "Подождите пока наполнится кеш");
					((App)mContext.getApplicationContext()).setCountDownloadTrying(0);
					Intent i = new Intent(ActionProgressBarFirstTracksLoad);
					i.putExtra("ProgressOn", true);
					mContext.sendBroadcast(i);
					//		Запускаем кеширование треков - 3 шт
					Intent downloaderIntent = new Intent(mContext.getApplicationContext(), LongRequestAPIService.class);
					downloaderIntent.setAction(ACTION_GETNEXTTRACK);
					downloaderIntent.putExtra(EXTRA_DEVICEID, DeviceId);
					downloaderIntent.putExtra(EXTRA_COUNT, 3);

					if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
						mContext.startForegroundService(downloaderIntent);
					}else{
						mContext.startService(downloaderIntent);
					}

				} else {
					mContext.sendBroadcast(new Intent(ActionCheckCountTracksAndDownloadIfNotEnought));
				}
				return false;
			} else return true;
		}catch (Exception ex){
			new Utilites().SendInformationTxt(mContext, "Ошибка показа диалога " + ex.getLocalizedMessage());
			return false;
		}
	}
	
	public boolean SendLogs(Context mContext, String deviceId){
		try {
			File appDirectory = mContext.getFilesDir();
			File logDirectory = new File( appDirectory + File.separator + "log" );
			// create app folder
			if ( !appDirectory.exists() ) {
				appDirectory.mkdir();
			}
			// create log folder
			if ( !logDirectory.exists() ) {
				logDirectory.mkdir();
			}
			File logFile = new File( logDirectory, "logcat" + System.currentTimeMillis() + ".txt" );
			Runtime.getRuntime().exec("logcat -d -f " + logFile.getAbsolutePath());
			Intent logSenderIntent = new Intent(mContext, RequestAPIService.class);
			logSenderIntent.setAction(ACTION_SENDLOGS);
			logSenderIntent.putExtra(EXTRA_DEVICEID, deviceId); //getApplicationContext()).execute(sp.getString("DeviceID", "")
			logSenderIntent.putExtra(EXTRA_LOGFILEPATH, logFile.getAbsolutePath());


			if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
				mContext.startForegroundService(logSenderIntent);
			}else{
				mContext.startService(logSenderIntent);
			}

			Toast.makeText(mContext, mContext.getResources().getString(R.string.log_is_send), Toast.LENGTH_SHORT).show();
			return true;
		}catch (Exception ex){
			new Utilites().SendInformationTxt(mContext, "Error in sendLogFile(): " + ex.getLocalizedMessage());
			return false;
		}
	}

}