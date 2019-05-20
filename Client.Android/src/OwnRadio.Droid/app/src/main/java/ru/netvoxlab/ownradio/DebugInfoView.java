package ru.netvoxlab.ownradio;

import android.content.ComponentName;
import android.content.ContentValues;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceCategory;
import android.preference.PreferenceFragment;
import android.preference.PreferenceManager;
import android.preference.PreferenceScreen;
import android.support.annotation.Nullable;
import android.support.v4.media.session.PlaybackStateCompat;
import android.support.v7.widget.Toolbar;
import android.util.TypedValue;
import android.view.View;
import android.view.ViewGroup;

import java.math.BigDecimal;
import java.util.Map;

import ru.netvoxlab.ownradio.rdevApiObjects.DeviceInfoResponse;
import ru.netvoxlab.ownradio.rdevapi.RdevApiCalls;

public class DebugInfoView extends AppCompatPreferenceActivity {
    static TrackDataAccess trackDataAccess;
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {

        setTheme(R.style.AppTheme);
        super.onCreate(savedInstanceState);
        trackDataAccess = new TrackDataAccess(getBaseContext());
        getLayoutInflater().inflate(R.layout.app_bar, (ViewGroup) findViewById(android.R.id.content));
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        //добавляем стрелку "назад" в тулбар
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setDisplayShowHomeEnabled(true);
//		setupActionBar();
        int horizontalMargin = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 2, getResources().getDisplayMetrics());
        int verticalMargin = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 2, getResources().getDisplayMetrics());
        int topMargin = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, (int) getResources().getDimension(R.dimen.activity_vertical_margin) + 30, getResources().getDisplayMetrics());
        getListView().setPadding(horizontalMargin, topMargin, horizontalMargin, verticalMargin);
        toolbar.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onBackPressed();
            }
        });

//        getFragmentManager().beginTransaction().replace(android.R.id.content, new DebugInfoPreferenceFragment()).commit();
    }

    protected boolean isValidFragment(String fragmentName) {
        return DebugInfoView.DebugInfoPreferenceFragment.class.getName().equals(fragmentName);
    }


    public static class DebugInfoPreferenceFragment extends PreferenceFragment{
        SharedPreferences sp;
        Thread fetchRdevInfoThread;
        final static double bytesInGB = 1073741824.0d;
        @Override
        public void onCreate(@Nullable Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            addPreferencesFromResource(R.xml.dev_info);



            //Получение настроек
            Preference deviceFreeMemory = findPreference("total_free_mamory_pref");
            Preference deviceUsedMemory = findPreference("used_memory_pref");
            Preference listenedTracksCount = findPreference("listened_tracks_count_pref");
            Preference cachedTracksCount = findPreference("cached_tracks_count_pref");
            Preference deviceAvailableMemory = findPreference("available_memory_pref");
            final Preference usernameFromServer = findPreference("username_pref");
            final Preference useridFromServer = findPreference("userid_pref");
            final Preference deviceIdFromServer = findPreference("deviceid_pref");
            Preference googleAuth = findPreference("google_auth");


            this.sp = PreferenceManager.getDefaultSharedPreferences(getActivity());

            //Установка значений
            final TrackToCache memoryUtil = new TrackToCache(getActivity().getApplicationContext());
            final TrackDataAccess trackInfo = new TrackDataAccess(getActivity().getApplicationContext());

            final double freeSpace = memoryUtil.FreeSpace();
            final double tracksSpace = memoryUtil.FolderSize(((App) getActivity().getApplicationContext()).getMusicDirectory());

            deviceFreeMemory.setTitle(getResources()
                    .getString(R.string.pref_free_memory_size) + " " + BigDecimal.valueOf(freeSpace / bytesInGB)
                    .setScale(2, BigDecimal.ROUND_DOWN) + "Gb");

            deviceUsedMemory.setSummary(BigDecimal.valueOf(tracksSpace / bytesInGB).setScale(2, BigDecimal.ROUND_DOWN) + "Gb");
            listenedTracksCount.setSummary(trackInfo.GetCountPlayTracks() + " " +  getResources().getString(R.string.tracks));
            cachedTracksCount.setSummary(trackInfo.GetExistTracksCount() + " " +  getResources().getString(R.string.tracks));

            Double percentage = Double.valueOf(sp.getInt("key_number", 0)) / 10;
            Double availableMemory = freeSpace * percentage;
            deviceAvailableMemory.setSummary(BigDecimal.valueOf(availableMemory / bytesInGB
            ).setScale(2, BigDecimal.ROUND_DOWN) + "Gb");



            Boolean wasLogged = sp.getBoolean("wasLogged", false);
            if(wasLogged){
                String userFullName = sp.getString("googleUserFullName", "");
                String email = sp.getString("googleEmail", "");
                googleAuth.setTitle(userFullName);
                googleAuth.setSummary(email);
            }else {
                googleAuth.setTitle("Авторизация Google");
                googleAuth.setSummary("Не авторизовано");
            }
            //Получение данных с сервера
            final RdevApiCalls rdevApiCalls = new RdevApiCalls(getActivity());

            final String DeviceId = sp.getString("DeviceID", "");
            final Handler handler = new Handler(Looper.getMainLooper());
            CheckConnection checkConnection = new CheckConnection();
            Boolean networkConnected = checkConnection.CheckInetConnection(getActivity());
            if(networkConnected) {
                this.fetchRdevInfoThread = new Thread(new Runnable() {
                    public void run() {
                        handler.post(new Runnable() {
                            @Override
                            public void run() {
                                usernameFromServer.setSummary("Получение данных с сервера..");
                                useridFromServer.setSummary("Получение данных с сервера..");
                                deviceIdFromServer.setSummary("Получение данных с сервера..");
                            }
                        });
                        if (!DeviceId.isEmpty()) {
                            Map<String, DeviceInfoResponse> result = rdevApiCalls.GetDeviceInfo(DeviceId);
                            if (result != null) {
                                DeviceInfoResponse deviceInfoResponse = result.get("OK");
                                if (deviceInfoResponse.getResult() != null) {
                                    final String deviceName = deviceInfoResponse.getResult().get("recname");
                                    final String userid = deviceInfoResponse.getResult().get("userid");
                                    final String serverDeviceId = deviceInfoResponse.getResult().get("recid");
                                    handler.post(new Runnable() {
                                        @Override
                                        public void run() {
                                            usernameFromServer.setSummary(deviceName);
                                            useridFromServer.setSummary(userid);
                                            deviceIdFromServer.setSummary(serverDeviceId);
                                        }
                                    });
                                } else {
                                    handler.post(new Runnable() {
                                        @Override
                                        public void run() {
                                            usernameFromServer.setSummary("Ошибка, сервер вернул пустое значение");
                                            useridFromServer.setSummary("Ошибка, сервер вернул пустое значение");
                                            deviceIdFromServer.setSummary("Ошибка, сервер вернул пустое значение");
                                        }
                                    });
                                }
                            } else {
                                handler.post(new Runnable() {
                                    @Override
                                    public void run() {
                                        usernameFromServer.setSummary("Ошибка, сервер вернул пустое значение");
                                        useridFromServer.setSummary("Ошибка, сервер вернул пустое значение");
                                        deviceIdFromServer.setSummary("Ошибка, сервер вернул пустое значение");
                                    }
                                });
                            }

                        } else {
                            handler.post(new Runnable() {
                                @Override
                                public void run() {
                                    usernameFromServer.setSummary("Ошибка получения данных с сервера");
                                    useridFromServer.setSummary("Ошибка получения данных с сервера");
                                    deviceIdFromServer.setSummary("Ошибка получения данных с сервера");
                                }
                            });
                        }
                    }
                });

                this.fetchRdevInfoThread.start();
            }else {
                usernameFromServer.setSummary("Подключение к интернету отсутствует");
                useridFromServer.setSummary("Подключение к интернету отсутствует");
                deviceIdFromServer.setSummary("Подключение к интернету отсутствует");
            }


            String trackid = sp.getString("current_playing_track_id", "");
            ContentValues track = trackDataAccess.GetTrackByIdWithoutExist(trackid);
            Boolean exist = trackDataAccess.CheckTrackExistInDB(trackid);

            PreferenceCategory devInfoCat = (PreferenceCategory)findPreference("debug_info_pref_category");
            Preference preference = new Preference(devInfoCat.getContext());

            if(!trackid.isEmpty()){
                if(exist &&  track != null){

                    Integer getTrackMethod = track.getAsInteger("methodnumber");
                    String artist = track.getAsString("artist");
                    String title = track.getAsString("title");

                    preference.setTitle("Сейчас играет " + artist + " - " + title);

                    switch (getTrackMethod){
                        case 1:
                            preference.setSummary("Трек выдан как случайный");
                            break;
                        case 2:
                            preference.setSummary("Трек выдан как свой");
                            break;
                        case 7:
                            preference.setSummary("Трек выдан как рекомендуемый");
                            break;
                        case 8:
                            preference.setSummary("Выдан как популярный трек для нового пользователя");
                            break;

                            default:
                                break;
                    }
                    devInfoCat.addPreference(preference);
                }else {
                    preference.setTitle("Текущий трек");
                    preference.setSummary("Текущий трек не удалось найти в БД");
                    devInfoCat.addPreference(preference);
                }
            }
        }

        @Override
        public void onDestroy() {
            super.onDestroy();
            if(this.fetchRdevInfoThread != null){
                if(this.fetchRdevInfoThread.isAlive()){
                    this.fetchRdevInfoThread.interrupt();
                }
            }
        }

        @Override
        public void onDestroyView() {
            super.onDestroyView();
            if(this.fetchRdevInfoThread != null){
                if(this.fetchRdevInfoThread.isAlive()){
                    this.fetchRdevInfoThread.interrupt();
                }
            }
        }
    }
}
