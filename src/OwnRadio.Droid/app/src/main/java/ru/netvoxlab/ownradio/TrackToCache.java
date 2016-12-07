package ru.netvoxlab.ownradio;

import android.content.ContentValues;
import android.content.Context;
import android.os.Build;
import android.os.Environment;
import android.os.StatFs;
import android.support.v4.content.ContextCompat;
import android.util.Log;

import org.json.JSONObject;

import java.io.File;

/**
 * Created by a.polunina on 24.10.2016.
 */

public class TrackToCache {
	Context mContext;
	private final int EXTERNAL_STORAGE_NOT_AVAILABLE = -1;
	private final int DOWNLOAD_FILE_TO_CACHE = 1;
	private final int DELETE_FILE_FROM_CACHE = 2;
	final String TAG = "ownRadio";

	public TrackToCache(Context context) {
		mContext = context;
	}

	public String SaveTrackToCache(String deviceId, int trackCount) {
		TrackDataAccess trackDataAccess = new TrackDataAccess(mContext);
		String trackId;

		APICalls apiCalls = new APICalls(mContext);

		for (int i = 0; i < trackCount; i++) {
			int flag = CheckCacheDoing();
			switch (flag) {
				case EXTERNAL_STORAGE_NOT_AVAILABLE:
					return "Директория на карте памяти недоступна";

				case DOWNLOAD_FILE_TO_CACHE: {
					CheckConnection checkConnection = new CheckConnection();
					boolean internetConnect = checkConnection.CheckInetConnection(mContext);
//					boolean wifiConnect = checkConnection.CheckWifiConnection(mContext);
					if (!internetConnect)
						return "Подключение к интернету отсутствует";

					try {
						GetTrack getTrack = new GetTrack();
						JSONObject trackJSON = apiCalls.GetNextTrackID(deviceId);
						trackId = trackJSON.getString("id");
						if (trackDataAccess.CheckTrackExistInDB(trackId)) {
							Log.d(TAG, "Трек был загружен ранее. TrackID" + trackId);
							return "Трек был загружен ранее";
						}
						//Загружаем трек и сохраняем информацию о нем в БД
						getTrack.GetTrackDM(mContext, trackJSON);
						Log.d(TAG, "Кеширование начато");
					} catch (Exception ex) {
						Log.d(TAG, "Error in SaveTrackToCache at file download. Ex.mess:" + ex.getLocalizedMessage());
						return ex.getLocalizedMessage();
					}
					break;
				}

				case DELETE_FILE_FROM_CACHE: {
					ContentValues track = trackDataAccess.GetTrackForDel();
					if(DeleteTrackFromCache(track))
						i--;
					break;
				}
			}
		}
		return "Кеширование треков";
	}

	public static long FolderSize(File directory) {
		long length = 0;
		try {
			if (directory.listFiles() != null) {
				for (File file : directory.listFiles()) {
					if (file.isFile())
						length += file.length();
					else
						length += FolderSize(file);
				}
			}
		} catch (Exception ex) {
			return ex.hashCode();
		}
		return length;
	}

	public void ScanTrackToCache() {
		try {
			File directory = mContext.getExternalFilesDir(Environment.DIRECTORY_MUSIC);
			if (directory.listFiles() != null) {
				ContentValues track = new ContentValues();
				for (File file : directory.listFiles()) {
					if (file.isFile()) {
						track.put("id", file.getName().substring(0, 36));
						track.put("trackurl", file.getAbsolutePath());
						track.put("datetimelastlisten", "");
						track.put("islisten", "0");
						track.put("isexist", "1");
						new TrackDataAccess(mContext).SaveTrack(track);
					}
				}
			}
		} catch (Exception ex) {
			ex.getLocalizedMessage();
		}
	}

	public int CheckCacheDoing(){
			File[] externalStoragesPaths = ContextCompat.getExternalFilesDirs(mContext, null);
			File externalStoragePath;
			if (externalStoragesPaths == null)
				return EXTERNAL_STORAGE_NOT_AVAILABLE;

			externalStoragePath = externalStoragesPaths[0];
			long availableSpace = 0;
			long cacheSize = FolderSize(mContext.getExternalFilesDir(Environment.DIRECTORY_MUSIC));
			if (Build.VERSION.SDK_INT <= 17) {
				StatFs stat = new StatFs(Environment.getExternalStorageDirectory().getPath());
				availableSpace = (long) stat.getFreeBlocks() * (long) stat.getBlockSize();
				Log.d(TAG, "availableSpace :" + availableSpace / 1048576);
			}
			if (Build.VERSION.SDK_INT >= 18) {
				availableSpace = new StatFs(externalStoragePath.getPath()).getAvailableBytes();
				Log.d(TAG, "availableSpace :" + availableSpace / 1048576);
			}

			if (cacheSize < (cacheSize + availableSpace) / 2.0)
				return DOWNLOAD_FILE_TO_CACHE;
			else
				return DELETE_FILE_FROM_CACHE;
	}

	public boolean DeleteTrackFromCache(ContentValues track){
		TrackDataAccess trackDataAccess = new TrackDataAccess(mContext);
		try {
			if (track != null) {
				File file1 = new File(track.getAsString("trackurl"));
				File file = new File(mContext.getExternalFilesDir(Environment.DIRECTORY_MUSIC) + "/" + file1.getName());
				if (file.exists()) {
					if (file.delete()) {
						trackDataAccess.DeleteTrackFromCache(track);
						Log.d(TAG, "File is deleted");
						return true;
					}
					Log.d(TAG, "File not deleted. Something error");
					return false;
				} else {
					trackDataAccess.DeleteTrackFromCache(track);
					Log.d(TAG, "File for delete is not exist. Rec about track deleted from DB");
					return false;
				}
			} else {
				Log.d(TAG, "Отсутствует файл для удаления.");
				return false;
			}
		} catch (Exception ex) {
			Log.d(TAG, "Error in SaveTrackToCache at file delete. Ex.mess:" + ex.getLocalizedMessage());
			return false;
		}
	}
}
