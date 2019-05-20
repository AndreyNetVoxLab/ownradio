package ru.netvoxlab.ownradio;

import android.annotation.TargetApi;
import android.content.Context;
import android.database.DatabaseUtils;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.os.Build;
import android.os.Environment;

import java.io.File;

/**
 * Created by a.polunina on 25.10.2016.
 */

public class TrackDB extends SQLiteOpenHelper {
	public final static int DB_VER = 9;
	public static final String TABLE_NAME_TRACK = "track";
	static final String CREATE_TABLE_TRACK = "CREATE TABLE " + TABLE_NAME_TRACK + "(id TEXT NOT NULL UNIQUE, trackurl TEXT," +
			" title TEXT, artist TEXT, length INTEGER NOT NULL DEFAULT 1000, " +
			" datetimelastlisten TEXT NOT NULL, isexist INTEGER NOT NULL DEFAULT 0, countplay INTEGER NOT NULL DEFAULT 0, methodnumber INTEGER)";

	public static final String TABLE_NAME_HISTORY = "history";
	static final String CREATE_TABLE_HISTORY = "CREATE TABLE " + TABLE_NAME_HISTORY + "(id TEXT NOT NULL UNIQUE, trackid TEXT NOT NULL, userid TEXT NOT NULL," +
			"datetimelisten TEXT NOT NULL, islisten INTEGER NOT NULL DEFAULT 0)";

	public static final String DB_NAME = "ownradiodb34.db3";
	private static Context mContext;
	private static TrackDB instance;
	private SQLiteDatabase database;
	private int openConnections = 0;
	
	public synchronized static TrackDB getInstance(Context context){
		if(instance == null) {
			instance = new TrackDB(context.getApplicationContext(), DB_VER);
		}
		return instance;
	}
	
	public TrackDB(Context context, int dbVer) {
		super(context, DB_NAME, null, dbVer);
		mContext = context;
		
		if(Build.VERSION.SDK_INT < Build.VERSION_CODES.JELLY_BEAN) {
			SQLiteDatabase db = getWritableDatabase();
			db.enableWriteAheadLogging();
//			db.execSQL();
			//TODO
		}
	}

	@Override
	public void onCreate(SQLiteDatabase db) {
		db.execSQL(CREATE_TABLE_TRACK);
		db.execSQL(CREATE_TABLE_HISTORY);
	}

	@Override
	public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
		//проверяем какая версия сейчас и делаете апдейт
		db.execSQL("DROP TABLE IF EXISTS " + TABLE_NAME_TRACK);
		db.execSQL("DROP TABLE IF EXISTS " + TABLE_NAME_HISTORY);
		onCreate(db);


		//Чистим все треки
		try {
			File directory = ((App)mContext).getMusicDirectory();//mContext.getFilesDir();
//			File directory = mContext.getExternalFilesDir(Environment.DIRECTORY_MUSIC);
			if (directory.listFiles() != null) {
				for (File file : directory.listFiles()) {
					file.delete();
				}
			}

			File directory2 = mContext.getExternalFilesDir(Environment.DIRECTORY_MUSIC);
			if (directory2.listFiles() != null) {
				for (File file : directory2.listFiles()) {
					file.delete();
				}
			}
		} catch (Exception ex) {
			ex.getLocalizedMessage();
		}
	}
	
	/**
	 * Must be called from the same thread as the original openDatabase call.
	 */
	@Override
	public synchronized void close() {
		if(database == null || openConnections == 0){
			throw new IllegalStateException("Database already closed or has never been opened.");
		}
		openConnections--;
		if(openConnections != 0){
			return;
		}
		database = null;
		super.close();
	}
	
	/**
	 * Do not manually call this method! Use openDatabase(), database() and close()!
	 *
	 * Opens the SQLiteDatabase if not already opened.
	 * This implementation does the exact same thing as getWritableDatabase and thus will return a writable database.
	 *
	 * @return the newly opened database or the existing database.
	 */
	@Override
	public synchronized SQLiteDatabase getReadableDatabase() {
		return getWritableDatabase();
	}
	
	/**
	 *
	 * Do not manually call this method! Use openDatabase(), database() and close()!
	 *
	 * Opens the SQLiteDatabase if not already opened.
	 *
	 * @return the newly opened database or the existing database.
	 */
	@Override
	public synchronized SQLiteDatabase getWritableDatabase() {
		if(database == null){
			database = super.getWritableDatabase();
		}
		openConnections++;
		return database;
	}
	
	/**
	 * Open the database. Always pair this call with close() and use database() to get the opened database!
	 */
	public synchronized void openDatabase(){
		getWritableDatabase();
	}
	
	/**
	 * Returns the opened database. Throws an exception if the database has not been opened yet!
	 * @return the database.
	 */
	public synchronized SQLiteDatabase database(){
		if(database == null){
			throw new IllegalStateException("Database has not been opened yet!");
		}
		return database;
	}
	@Override
	public synchronized void onOpen(SQLiteDatabase db) {
		setForeignKeyConstraintsEnabled(db);
	}
	
	@TargetApi(Build.VERSION_CODES.JELLY_BEAN)
	@Override
	public synchronized void onConfigure(SQLiteDatabase db) {
		db.setForeignKeyConstraintsEnabled(true);
	}
	
	private void setForeignKeyConstraintsEnabled(SQLiteDatabase db){
		//Skip for Android 4.1 and newer as this is already handled in onConfigure
		if(Build.VERSION.SDK_INT < Build.VERSION_CODES.JELLY_BEAN && !db.isReadOnly()) {
			db.execSQL("PRAGMA foreign_keys=ON;");
		}
	}
	
	/* I often have some utility methods in this class too. */
	public long getCount(String table){
		return DatabaseUtils.queryNumEntries(database(), table);
	}
}