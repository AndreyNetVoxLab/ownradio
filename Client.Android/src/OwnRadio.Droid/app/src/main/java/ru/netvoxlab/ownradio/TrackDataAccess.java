package ru.netvoxlab.ownradio;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import java.io.File;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * Created by a.polunina on 26.10.2016.
 */

public class TrackDataAccess {
	Context mContext;
	SQLiteDatabase db;
	TrackDB trackDB;
	private final String TrackTableName = "track";
	
	public TrackDataAccess(Context context) {
		mContext = context;
		trackDB = TrackDB.getInstance(mContext);
//		trackDB = new TrackDB(mContext, DB_VER);
	}
	
	public void SaveTrack(ContentValues trackInstance) {
		trackDB.openDatabase();
		db = trackDB.database();
		//Получаем текущее время - 10 лет
		Cursor userCursor = db.rawQuery("SELECT strftime('%Y-%m-%dT%H:%M:%S', 'now','-10 year')", null);
		if (userCursor.moveToFirst())
			trackInstance.put("datetimelastlisten", userCursor.getString(0));
		trackInstance.put("countplay", 0);
		
		if (trackInstance.getAsInteger("length") == null) {
			trackInstance.put("length", 1000);
		}
		
		db.insertOrThrow(TrackTableName, null, trackInstance);
		trackDB.close();
		//db.close();
		
	}
	
	public boolean CheckTrackExistInDB(String trackId) {
		trackDB.openDatabase();
		db = trackDB.database();
		Cursor userCursor = db.rawQuery("SELECT * FROM track WHERE id = ?", new String[]{String.valueOf(trackId)});
		boolean result = true;
		//db.close();
		if (userCursor.moveToFirst())
			result = true;
		else
			result = false;
		userCursor.close();
		trackDB.close();
		return result;
	}
	
	public ContentValues GetMostOldTrack() {
		ContentValues result = new ContentValues();
		trackDB.openDatabase();
		db = trackDB.database();
		Cursor userCursor = db.rawQuery("SELECT id, trackurl, title, artist, length, methodnumber FROM track WHERE isexist = ? ORDER BY datetimelastlisten", new String[]{String.valueOf(1)});
		if (userCursor.moveToFirst()) {
			result.put("id", userCursor.getString(0));
			result.put("trackurl", userCursor.getString(1));
			result.put("title", userCursor.getString(2));
			result.put("artist", userCursor.getString(3));
			result.put("length", userCursor.getInt(4));
			result.put("methodnumber", userCursor.getInt(5));
		} else {
			result = null;
		}
		//db.close();
		userCursor.close();
		trackDB.close();
		return result;
	}
	public ContentValues GetMostNewNotListenTrack() {
		ContentValues result = new ContentValues();
		trackDB.openDatabase();
		db = trackDB.database();
		Cursor userCursor = db.rawQuery("SELECT id, trackurl, title, artist, length, methodnumber FROM track WHERE isexist = ? AND countplay = 0 ORDER BY datetimelastlisten DESC", new String[]{String.valueOf(1)});
		if (userCursor.moveToFirst()) {

			result.put("trackurl", userCursor.getString(1));result.put("id", userCursor.getString(0));
			result.put("title", userCursor.getString(2));
			result.put("artist", userCursor.getString(3));
			result.put("length", userCursor.getInt(4));
			result.put("methodnumber", userCursor.getInt(5));
		} else {
			result = null;
		}
		//db.close();
		userCursor.close();
		trackDB.close();
		return result;
	}
	public ContentValues GetOldListenedTrack() {
		ContentValues result = new ContentValues();
		trackDB.openDatabase();
		db = trackDB.database();
		Cursor userCursor = db.rawQuery("SELECT id, trackurl, title, artist, length, methodnumber FROM track WHERE isexist = ? AND countplay > 0 ORDER BY datetimelastlisten", new String[]{String.valueOf(1)});
		if (userCursor.moveToFirst()) {
			result.put("id", userCursor.getString(0));
			result.put("trackurl", userCursor.getString(1));
			result.put("title", userCursor.getString(2));
			result.put("artist", userCursor.getString(3));
			result.put("length", userCursor.getInt(4));
			result.put("methodnumber", userCursor.getInt(5));
		} else {
			result = null;
		}
		//db.close();
		userCursor.close();
		trackDB.close();
		return result;
	}
	
	// получить последний прослушанный трек
	public ContentValues GetMostNewTrack() {
		ContentValues result = new ContentValues();
		trackDB.openDatabase();
		db = trackDB.database();
		Cursor userCursor = db.rawQuery("SELECT id, trackurl, title, artist, length, methodnumber FROM track WHERE isexist = ? ORDER BY datetimelastlisten DESC", new String[]{String.valueOf(1)});
		if (userCursor.moveToFirst()) {
			result.put("id", userCursor.getString(0));
			result.put("trackurl", userCursor.getString(1));
			result.put("title", userCursor.getString(2));
			result.put("artist", userCursor.getString(3));
			result.put("length", userCursor.getInt(4));
			result.put("length", userCursor.getInt(5));
		} else {
			result = null;
		}
		//db.close();
		userCursor.close();
		trackDB.close();
		return result;
	}


	
	public ContentValues GetTrackWithMaxCountPlay() {
		ContentValues result = new ContentValues();
		trackDB.openDatabase();
		db = trackDB.database();
		Cursor userCursor = db.rawQuery("SELECT id, trackurl FROM track WHERE countplay != ? AND isexist = ? AND id != ? ORDER BY countplay DESC LIMIT 1", new String[]{String.valueOf(0), String.valueOf(1), String.valueOf(new PrefManager(mContext).getPrefItem("LastTrackID", ""))});
		if (userCursor.moveToFirst()) {
			result.put("id", userCursor.getString(0));
			result.put("trackurl", userCursor.getString(1));
		} else {
			result = null;
		}
		//db.close();
		userCursor.close();
		trackDB.close();
		return result;
	}
	
	public int GetExistTracksCount() {
		int result;
		trackDB.openDatabase();
		db = trackDB.database();
		Cursor userCursor = db.rawQuery("SELECT COUNT(*) FROM track WHERE isexist = ? ORDER BY datetimelastlisten", new String[]{String.valueOf(1)});
		if (userCursor.moveToFirst()) {
			result = userCursor.getInt(0);
		} else {
			result = 0;
		}
		//db.close();
		userCursor.close();
		trackDB.close();
		return result;
	}
	
	//Функция сохраняет время начала попытки проигрывания трека и увеличивает счетчик попыток проигрывания на 1
	public void SetTimeAndCountStartPlayback(ContentValues trackInstance) {
		int countPlay = 1;
		String currentDatetime = "2000-01-01T12:00:00";
		
		trackDB.openDatabase();
		db = trackDB.database();
		
		//Получаем из базы количетво попыток прослушивания трека
		Cursor userCursorCount = db.rawQuery("SELECT countplay FROM track WHERE id = ?", new String[]{String.valueOf(trackInstance.get("id"))});
		if (userCursorCount.moveToFirst()) {
			countPlay += userCursorCount.getInt(0);
		}
		trackInstance.put("countplay", countPlay);
		userCursorCount.close();
		//Получаем текущее время
		Cursor userCursorTime = db.rawQuery("SELECT strftime('%Y-%m-%dT%H:%M:%S', 'now')", null);
		if (userCursorTime.moveToFirst())
			currentDatetime = userCursorTime.getString(0);
		userCursorTime.close();
		trackInstance.put("datetimelastlisten", currentDatetime);
//		db.rawQuery("UPDATE track SET datetimelastlisten=?, countplay=? WHERE id = ?", new String[]{String.valueOf(currentDatetime), String.valueOf(trackInstance.get("id")), String.valueOf(countPlay)});
		int row = db.update(TrackTableName, trackInstance, "id = ?", new String[]{String.valueOf(trackInstance.get("id"))});
		trackDB.close();
//db.close();
	}
	
	public String GetCountPlayTracksTable() {
		trackDB.openDatabase();
		db = trackDB.database();
		Cursor userCursor = db.rawQuery("SELECT countplay, COUNT(*) AS tracks FROM track GROUP BY countplay ORDER BY countplay DESC", null);
		
		String tableString = "";
		if (userCursor.moveToFirst()) {
			String[] columnNames = userCursor.getColumnNames();
//			tableString = columnNames[0] + " | " + columnNames[1] + "\n";
			do {
				tableString += userCursor.getInt(userCursor.getColumnIndex(columnNames[0])) + " : \t" +
						userCursor.getInt(userCursor.getColumnIndex(columnNames[1]));
				tableString += "\n";
				
			} while (userCursor.moveToNext());
		} else
			tableString = mContext.getResources().getString(R.string.count_tracks_listened_table);
		userCursor.close();
		//db.close();
		trackDB.close();
		return tableString;
	}
	
	//возвращает количество прослушанных треков
	public long GetCountPlayTracks() {
		trackDB.openDatabase();
		db = trackDB.database();
		Cursor userCursor = db.rawQuery("SELECT COUNT(*) FROM track WHERE countplay > ?", new String[]{String.valueOf(0)});
		long listeningTracksCount = 0;
		if (userCursor.moveToFirst()) {
			listeningTracksCount = userCursor.getLong(0);
		} else
			listeningTracksCount = 0;
		userCursor.close();
		//db.close();
		trackDB.close();
		return listeningTracksCount;
	}
	
	//возвращает список id прослушанных треков
	public List<File> GetUuidsListeningTracks() {
		trackDB.openDatabase();
		db = trackDB.database();
		Cursor userCursor = db.rawQuery("SELECT id FROM track WHERE countplay > ?", new String[]{String.valueOf(0)});
		List<File> listListeningTracks = new ArrayList<>();
		if (userCursor.moveToFirst()) {
			do {
				listListeningTracks.add(new File(((App) mContext).getMusicDirectory() + "/" + userCursor.getString(0) + ".mp3"));
			} while (userCursor.moveToNext());
		} else {
			trackDB.close();
			return null;
		}
		trackDB.close();
		return listListeningTracks;
	}
	
	//Функция удаляет запись о треке из БД
	public int DeleteTrackFromCache(ContentValues trackInstance) {
		trackDB.openDatabase();
		db = trackDB.database();
		int rows = db.delete(TrackTableName, "id = ?", new String[]{String.valueOf(trackInstance.get("id"))});
		trackDB.close();
		return rows;
		//db.close();
	}


	
	//Функция удаляет записи обо всех треках
	public int DeleteAllTracksFromCache() {
		trackDB.openDatabase();
		db = trackDB.database();
		int rows = db.delete(TrackTableName, null, null);
		trackDB.close();
		return rows;
		//db.close();
	}
	
	//Функция удаляет записи о прослушанных треках
	public int DeleteListenedTracksFromCache() {
		trackDB.openDatabase();
		db = trackDB.database();
		int rows = db.delete(TrackTableName, "countplay > ?", new String[]{String.valueOf(0)});
		trackDB.close();
		return rows;
		//db.close();
	}
	
	//функция возвращает время начала последнего воспроизведения трека
	public Boolean CheckEnoughTimeFromStartPlaying(String trackId) {
		trackDB.openDatabase();
		db = trackDB.database();
		String currentDatetime = "2000-01-01T12:00:00";
		SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");
		
		//Получаем текущее время
		Cursor userCursorTime = db.rawQuery("SELECT strftime('%Y-%m-%dT%H:%M:%S', 'now', '-3 second')", null);
		
		try {
			if (userCursorTime.moveToFirst()) {
				currentDatetime = userCursorTime.getString(0);
				userCursorTime.close();
				Date currentDate = format.parse(currentDatetime);
				Cursor userCursor = db.rawQuery("SELECT datetimelastlisten FROM track WHERE id = ?", new String[]{String.valueOf(trackId)});
				if (userCursor.moveToFirst()) {
					String dtStart = userCursor.getString(0);
					Date date = format.parse(dtStart);
					if (currentDate.after(date)) {
						trackDB.close();
						return true;
					} else {
						trackDB.close();
						return false;
					}
				}
			} else {
				trackDB.close();
				return false;
			}
			
		} catch (ParseException e) {
			e.printStackTrace();
			trackDB.close();
			return false;
		}
		trackDB.close();
		return true;
	}
	
	//Возвращает трек по id если он не удалён
	public ContentValues GetTrackById(String trackId) {
		ContentValues result = new ContentValues();
		trackDB.openDatabase();
		db = trackDB.database();
		Cursor userCursor = db.rawQuery("SELECT id, trackurl, title, artist, length, methodnumber FROM track WHERE id = ? and isexist = ? ORDER BY datetimelastlisten", new String[]{String.valueOf(trackId), String.valueOf(1)});
		if (userCursor.moveToFirst()) {
			result.put("id", userCursor.getString(0));
			result.put("trackurl", userCursor.getString(1));
			result.put("title", userCursor.getString(2));
			result.put("artist", userCursor.getString(3));
			result.put("length", userCursor.getInt(4));
			result.put("methodnumber", userCursor.getInt(5));
		} else {
			result = null;
		}
		userCursor.close();
		trackDB.close();
		return result;
	}

	//Возвращает трек по id если он не удалён
	public ContentValues GetTrackByIdWithoutExist(String trackId) {
		ContentValues result = new ContentValues();
		trackDB.openDatabase();
		db = trackDB.database();
		Cursor userCursor = db.rawQuery("SELECT" +
				" id, title, artist, methodnumber FROM track WHERE id = ? ORDER BY datetimelastlisten LIMIT 1", new String[]{trackId});
		if (userCursor.moveToFirst()) {
			result.put("id", userCursor.getString(0));
			result.put("title", userCursor.getString(1));
			result.put("artist", userCursor.getString(2));
			result.put("methodnumber", userCursor.getInt(3));
		} else {
			result = null;
		}
		userCursor.close();
		trackDB.close();
		return result;
	}
	
	public void UpdateTrack(ContentValues track)
	{
		trackDB.openDatabase();
		db = trackDB.database();
		db.update(TrackTableName, track, "id = ?", new String[]{track.getAsString("id")});
		trackDB.close();
	}
}
