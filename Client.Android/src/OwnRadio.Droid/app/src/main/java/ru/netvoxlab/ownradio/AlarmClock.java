package ru.netvoxlab.ownradio;

import android.app.AlarmManager;
import android.content.ContentValues;
import android.content.SharedPreferences;
import android.media.AudioManager;
import android.os.CountDownTimer;
import android.preference.PreferenceManager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.view.Window;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.NumberPicker;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;


import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Dictionary;
import java.util.Hashtable;

import static ru.netvoxlab.ownradio.Constants.ALARM_TIME;
import static ru.netvoxlab.ownradio.Constants.CURRENT_TRACK_ARTIST;
import static ru.netvoxlab.ownradio.Constants.CURRENT_TRACK_ID;
import static ru.netvoxlab.ownradio.Constants.CURRENT_TRACK_TITLE;
import static ru.netvoxlab.ownradio.Constants.CURRENT_TRACK_URL;
import static ru.netvoxlab.ownradio.Constants.CURRENT_VOLUME;
import static ru.netvoxlab.ownradio.Constants.CURRENT_VOLUME_PERCENT;
import static ru.netvoxlab.ownradio.Constants.FRIDAY_DAY;
import static ru.netvoxlab.ownradio.Constants.IS_ALARM_WORK;
import static ru.netvoxlab.ownradio.Constants.IS_CHANGE_VOLUME;
import static ru.netvoxlab.ownradio.Constants.IS_ONCE;
import static ru.netvoxlab.ownradio.Constants.IS_TIME_ALARM;
import static ru.netvoxlab.ownradio.Constants.MONDAY_DAY;
import static ru.netvoxlab.ownradio.Constants.SATURDAY_DAY;
import static ru.netvoxlab.ownradio.Constants.SUNDAY_DAY;
import static ru.netvoxlab.ownradio.Constants.THURSDAY_DAY;
import static ru.netvoxlab.ownradio.Constants.TUESDAY_DAY;
import static ru.netvoxlab.ownradio.Constants.WEDNESDAY_DAY;

public class AlarmClock extends AppCompatActivity {
	
	private final String[] DAYS_NAMES = {"Воскресенье", "Понедельник", "Вторник", "Среду", "Четверг", "Пятницу", "Субботу"};
	private final String[] HOURS = {"час", "часа", "часов"};
	private final String[] MINUTES = {"минуту", "минуты", "минут"};
	private final String[] DAYS = {"день", "дня", "дней"};
	
	private ImageView imageView;
	
	private TextView txtProgress;
	/*Days of week*/
	private TextView txtMonday;
	private TextView txtTuesday;
	private TextView txtWednesday;
	private TextView txtThursday;
	private TextView txtFriday;
	private TextView txtSaturday;
	private TextView txtSunday;
	
	private TextView txtCurrentTrack;
	private TextView txtCurrentPlayTrack;
	
	private SharedPreferences prefs;
	
	private CountDownTimer timer;
	private CountDownTimer musicTimer;
	
	private NumberPicker numberHours;
	private NumberPicker numberMinutes;
	
	private Toolbar toolbar;
	
	private TrackDataAccess db;
	
	//пн=2, вт=3, ср=4, чт=5, пт=6, сб=7, вс=1
	
	private CheckBox checkVolume;
	private SeekBar seekBarVolume;
	private TextView txtVolumePercent;
	
	private int maxVolume = 0;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		//Меняем тему, используемую при запуске приложения, на основную
		setTheme(R.style.AppTheme);
		super.onCreate(savedInstanceState);
		this.requestWindowFeature(Window.FEATURE_NO_TITLE);
		setContentView(R.layout.activity_alarm_clock);
		
		
		toolbar = findViewById(R.id.toolbar);
		setSupportActionBar(toolbar);
		getSupportActionBar().setDisplayHomeAsUpEnabled(true);
		getSupportActionBar().setDisplayShowHomeEnabled(true);
		toolbar.setNavigationOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				onBackPressed();
			}
		});
		
		// инициализируем все компоненты
		imageView = findViewById(R.id.imageView);
		imageView.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				btnStartClock(v);
			}
		});

		txtProgress = findViewById(R.id.txtProgress);
		
		txtMonday = findViewById(R.id.txtMonday);
		txtTuesday = findViewById(R.id.txtTuesday);
		txtWednesday = findViewById(R.id.txtWednesday);
		txtThursday = findViewById(R.id.txtThursday);
		txtFriday = findViewById(R.id.txtFriday);
		txtSaturday = findViewById(R.id.txtSaturday);
		txtSunday = findViewById(R.id.txtSunday);
		
		numberHours = findViewById(R.id.numberHours);
		numberMinutes = findViewById(R.id.numberMinutes);
		
		txtCurrentTrack = findViewById(R.id.txtCurrentTrack);
		txtCurrentPlayTrack = findViewById(R.id.txtCurrentPlayTrack);
		
		db = new TrackDataAccess(getApplicationContext());
		
		prefs = PreferenceManager.getDefaultSharedPreferences(this);
		
		
		checkVolume = findViewById(R.id.checkVolume);
		seekBarVolume = findViewById(R.id.seekBarVolume);
		txtVolumePercent = findViewById(R.id.txtVolumePercent);
		checkVolume.setChecked(prefs.getBoolean(IS_CHANGE_VOLUME, false));
		
		AudioManager mAudioManager = (AudioManager) getSystemService(AUDIO_SERVICE);
		maxVolume = mAudioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC);
		
		int volumePercent = prefs.getInt(CURRENT_VOLUME_PERCENT, 0);
		
		seekBarVolume.setMax(maxVolume);
		seekBarVolume.setProgress(prefs.getInt(CURRENT_VOLUME, 0));
		seekBarVolume.setEnabled(checkVolume.isChecked());
		
		txtVolumePercent.setText(volumePercent + " %");


		initialNumbers(); // загружаем цифры для часов, минут.
		
		initialComponents(); // загружаем все настройки
		
		NumberPicker.OnValueChangeListener timeVCListener = new NumberPicker.OnValueChangeListener() {
			@Override
			public void onValueChange(NumberPicker numberPicker, int i, int i1) {
				String time = "";
				
				String addMinutes = numberMinutes.getValue() < 10 ? "0" + String.valueOf(numberMinutes.getValue()) : String.valueOf(numberMinutes.getValue());
				String addHours = numberHours.getValue() < 10 ? "0" + String.valueOf(numberHours.getValue()) : String.valueOf(numberHours.getValue());
				time = addHours + ":" + addMinutes;
				
				
				if (prefs.getBoolean(IS_ALARM_WORK, false)) {
					if (prefs.getString(ALARM_TIME, "") != time) // время отличается, необходимо все таймеры перезапустить
					{
						if (prefs.getBoolean(IS_ONCE, false)) {
							setNewDay(time);
						}
						//restartAlarm(time); // перезапускаем таймеры
						SharedPreferences.Editor prefEditor = prefs.edit();
						prefEditor.putString(ALARM_TIME, time); // сохраняем новое время
						prefEditor.apply();
						
						startTimer(prefEditor);
						prefEditor.apply();
						
						Toast.makeText(AlarmClock.this, "Будильник успешно перезапущен", Toast.LENGTH_SHORT).show();
					}
				}
			}
		};
		
		numberHours.setOnValueChangedListener(timeVCListener);

		numberMinutes.setOnValueChangedListener(timeVCListener);
		
		musicTimer = new CountDownTimer(Long.MAX_VALUE, 1000) {
			@Override
			public void onTick(long l) {
				ContentValues trackInfo = db.GetMostNewTrack();
				
				if (trackInfo == null)
					return;
				
				String title = trackInfo.getAsString("title");
				
				if (title == "")
					return;
				
				if (txtCurrentPlayTrack.getText() != title) {
					txtCurrentPlayTrack.setText(title);
				}
			}

			@Override
			public void onFinish() {
			
			}
		};
		
		musicTimer.start();
		
		// change SeekBar
		seekBarVolume.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
			@Override
			public void onProgressChanged(SeekBar seekBar, int i, boolean b) {
				int percent = i * 100 / maxVolume;
				txtVolumePercent.setText(percent + " %");
				SharedPreferences.Editor editor = prefs.edit();
				editor.putInt(CURRENT_VOLUME_PERCENT, percent);
				editor.putInt(CURRENT_VOLUME, i);
				editor.apply();
			}
			
			@Override
			public void onStartTrackingTouch(SeekBar seekBar) {
			
			}
			
			@Override
			public void onStopTrackingTouch(SeekBar seekBar) {
				// TODO:ADD LOGICAL TO TEXT PERCENT
			}
		});
		
		
	}
	
	public void SetTrack(View view) {
		// получаем сведения о текущем треке
		ContentValues trackInfo = db.GetMostNewTrack();
		
		String title = trackInfo.getAsString("title");
		String artist = trackInfo.getAsString("artist");
		String id = trackInfo.getAsString("id");
		String url = trackInfo.getAsString("trackurl");
		
		//String directory = url.substring(0, url.indexOf("music/")) + "AlarmTrack/";
		
		String path = getPathAlarmTrack(url);
		
		copyFile(url, path); // копируем трек в папку
		
		txtCurrentTrack.setText(title); // устанавливаем его как трек для будильника
		
		SharedPreferences.Editor editor = prefs.edit();
		
		editor.putString(CURRENT_TRACK_TITLE, title);
		editor.putString(CURRENT_TRACK_ARTIST, artist);
		editor.putString(CURRENT_TRACK_ID, id);
		editor.putString(CURRENT_TRACK_URL, path);
		
		
		editor.apply(); // сохраняем текущий трек
	}
	
	public void OnCheckVolume(View view) {
		
		boolean isChecked = checkVolume.isChecked();
		
		seekBarVolume.setEnabled(isChecked);
		
		SharedPreferences.Editor editor = prefs.edit();
		editor.putBoolean(IS_CHANGE_VOLUME, isChecked);
		editor.apply();
	}
	
	private void copyFile(String src, String dest) {
		File srcFile = new File(src);
		File destFile = new File(dest);
		
		if (destFile.exists()) {
			destFile.delete();
		}
		
		try {
			FileUtils.copyFile(srcFile, destFile);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	private String getPathAlarmTrack(String url) {
		String directory = url.substring(0, url.indexOf("music/")) + "AlarmTrack/";
		String path = directory + "alarm.mp3";
		
		return path;
	}
	
	
	private void setNewDay(String time) {
		int hours = TimePreference.getHour(time);
		int mins = TimePreference.getMinute(time);

		String timesCurrent = "";
		if(android.os.Build.VERSION.SDK_INT >= 24){
			timesCurrent = new SimpleDateFormat("u:HH:mm").format(Calendar.getInstance().getTime());
		}
		else{
			Date d = new Date();
			Calendar cal = Calendar.getInstance();
			cal.setTime(d);
			int dow = cal.get(Calendar.DAY_OF_WEEK);
			switch (dow) {
				case Calendar.SUNDAY:
					dow = 7;
					break;
				case Calendar.MONDAY:
					dow = 1;
					break;
				case Calendar.TUESDAY:
					dow = 2;
					break;
				case Calendar.WEDNESDAY:
					dow = 3;
					break;
				case Calendar.THURSDAY:
					dow = 4;
					break;
				case Calendar.FRIDAY:
					dow = 5;
					break;
				case Calendar.SATURDAY:
					dow = 6;
					break;
			}
			timesCurrent = new SimpleDateFormat("HH:mm").format(Calendar.getInstance().getTime());
			timesCurrent = String.valueOf(dow) + ":" + timesCurrent;
		}


		String[] curTimes = timesCurrent.split(":");
		
		int day = Integer.valueOf(curTimes[0]); // день недели
		if (day == 7) {
			day = 1;
		} else {
			day += 1;
		}
		
		int currentHours = Integer.valueOf(curTimes[1]);
		int currentMins = Integer.valueOf(curTimes[2]);
		
		boolean isNewDay = false;
		
		if (currentHours > hours) {
			isNewDay = true;
		} else if (currentHours == hours) {
			if (currentMins > mins) {
				isNewDay = true;
			}
		}
		
		SharedPreferences.Editor prefEditor = prefs.edit();
		int resHours, resMins;
		if (isNewDay) {
			setPreference(day, false);
			selectTextView(day, false);
			if (++day > 7)
				day = 1;
			setPreference(day, true);
			selectTextView(day, true);
		} else {
			setPreference(day, true);
			selectTextView(day, true);
			if (++day > 7)
				day = 1;
			setPreference(day, false);
			selectTextView(day, false);
		}
		
		resMins = mins - currentMins;
		
		if (resMins < 0) {
			resMins += 60;
			hours -= 1;
		}
		
		resHours = hours - currentHours;
		
		if (resHours < 0) {
			resHours += 24;
		}
		
		String strTime = "Будильник зазвонит через";
		
		if (resHours > 0) {
			
			if (resHours > 24) {
				int resDays = resHours / 24;
				resHours -= resDays * 24;
				
				strTime += " " + resDays + " " + getCase(resDays, DAYS);
			}
			
			strTime += " " + resHours + " " + getCase(resHours, HOURS);
		}
		
		strTime += " " + resMins + " " + getCase(resMins, MINUTES);
		
		txtProgress.setText(strTime);
		
	}
	
	private void selectTextView(int dayWeek, boolean value) {
		SharedPreferences.Editor editor = prefs.edit();
		editor.putBoolean(IS_ONCE, false);
		editor.apply();
		
		switch (dayWeek) {
			case 1:
				txtSunday.setBackgroundResource(value ? R.drawable.bottom_selected : 0);
				break;
			case 2:
				txtMonday.setBackgroundResource(value ? R.drawable.bottom_selected : 0);
				break;
			case 3:
				txtTuesday.setBackgroundResource(value ? R.drawable.bottom_selected : 0);
				break;
			case 4:
				txtWednesday.setBackgroundResource(value ? R.drawable.bottom_selected : 0);
				break;
			case 5:
				txtThursday.setBackgroundResource(value ? R.drawable.bottom_selected : 0);
				break;
			case 6:
				txtFriday.setBackgroundResource(value ? R.drawable.bottom_selected : 0);
				break;
			case 7:
				txtSaturday.setBackgroundResource(value ? R.drawable.bottom_selected : 0);
				break;
		}
	}
	
	public void OnSelected(View view) {
		TextView textView = (TextView) view;
		boolean isSelected = false;
		
		String time = numberHours.getValue() + ":" + numberMinutes.getValue();
		
		if (textView.getBackground() == null) {
			textView.setBackgroundResource(R.drawable.bottom_selected);
			isSelected = true;
		} else {
			textView.setBackgroundResource(0);
		}
		
		int dayWeek = Integer.valueOf(textView.getTag().toString());
		
		// сохраняем дни в настройках дни выбранные
		setPreference(dayWeek, isSelected);
		
		
		if (!prefs.getBoolean(IS_ALARM_WORK, false))
			return;
		
		if (textView.getBackground() != null) {
			AlarmManager am = (AlarmManager) getSystemService(ALARM_SERVICE);
			AlarmReceiver.setTime(getApplicationContext(), time, dayWeek);
			Toast.makeText(this, "Будильник успешно добавлен на " + DAYS_NAMES[dayWeek - 1], Toast.LENGTH_SHORT).show();
		} else {
			AlarmReceiver.stopAlarm(this, dayWeek);
			
			// если не выбран ни 1 день недели, останавливаем будильник
			if (isNotSelected()) {
				stopAlarm();
				imageView.setImageResource(R.drawable.ic_grey_bud);
				
				if (timer != null)
					timer.cancel();
				
				txtProgress.setText("Будильник остановлен");
				
				SharedPreferences.Editor prefEditor = prefs.edit();
				prefEditor.putBoolean(IS_ALARM_WORK, false);
				prefEditor.apply();
			}
			
		}
	}
	
	private void initialComponents() {
		// получаем последний играющий трек ( текущий )
		ContentValues trackInfo = db.GetMostNewTrack();
		
		if (trackInfo != null) {
			
			String id = trackInfo.getAsString("id");
			String title = trackInfo.getAsString("title");
			String url = trackInfo.getAsString("trackurl");
			
			txtCurrentPlayTrack.setText(title);
			
			String currentTrack = prefs.getString(CURRENT_TRACK_TITLE, "");
			
			if (currentTrack == "") // если текущий трек не установлен, то устанавливаем, который играет
			{
				String path = getPathAlarmTrack(url);
				copyFile(url, path); // копируем трек в папку
				
				SharedPreferences.Editor editor = prefs.edit();
				editor.putString(CURRENT_TRACK_ID, id);
				editor.putString(CURRENT_TRACK_TITLE, title);
				editor.putString(CURRENT_TRACK_URL, path);
				editor.apply(); // сохраняем текущий трек
				
			}
			
			txtCurrentTrack.setText(prefs.getString(CURRENT_TRACK_TITLE, ""));
		}
		
		String time = prefs.getString(ALARM_TIME, "8:00");
		
		numberHours.setValue(TimePreference.getHour(time));
		numberMinutes.setValue(TimePreference.getMinute(time));
		
		if (prefs.getBoolean(IS_ONCE, false)) {
			for (int i = 1; i <= 7; i++)
				setPreference(i, false);
			stopAlarm();
			imageView.setImageResource(R.drawable.ic_grey_bud);
			
			return;
		}
		
		reloadDays();
		
		if (prefs.getBoolean(IS_ALARM_WORK, false)) {
			setTime();
			startTimer(prefs.edit());
			imageView.setImageResource(R.drawable.ic_blu_bud);
		} else {
			imageView.setImageResource(R.drawable.ic_grey_bud);
		}
	}
	
	
	private void reloadDays() {
		if (prefs.getBoolean(MONDAY_DAY, false)) {
			txtMonday.setBackgroundResource(R.drawable.bottom_selected);
		} else {
			txtMonday.setBackgroundResource(0);
		}
		
		if (prefs.getBoolean(TUESDAY_DAY, false)) {
			txtTuesday.setBackgroundResource(R.drawable.bottom_selected);
		} else {
			txtTuesday.setBackgroundResource(0);
		}
		
		if (prefs.getBoolean(WEDNESDAY_DAY, false)) {
			txtWednesday.setBackgroundResource(R.drawable.bottom_selected);
		} else {
			txtWednesday.setBackgroundResource(0);
		}
		
		if (prefs.getBoolean(THURSDAY_DAY, false)) {
			txtThursday.setBackgroundResource(R.drawable.bottom_selected);
		} else {
			txtThursday.setBackgroundResource(0);
		}
		
		if (prefs.getBoolean(FRIDAY_DAY, false)) {
			txtFriday.setBackgroundResource(R.drawable.bottom_selected);
		} else {
			txtFriday.setBackgroundResource(0);
		}
		
		if (prefs.getBoolean(SATURDAY_DAY, false)) {
			txtSaturday.setBackgroundResource(R.drawable.bottom_selected);
		} else {
			txtSaturday.setBackgroundResource(0);
		}
		
		if (prefs.getBoolean(SUNDAY_DAY, false)) {
			txtSunday.setBackgroundResource(R.drawable.bottom_selected);
		} else {
			txtSunday.setBackgroundResource(0);
		}
	}
	
	
	private void initialNumbers() {
		String[] hourValues = new String[25];
		
		for (int i = 0; i < 25; i++) {
			String zero = "";
			if (i < 10)
				zero = "0";
			hourValues[i] = zero + String.valueOf(i);
		}
		
		numberHours.setMinValue(0);
		numberHours.setMaxValue(23);
		numberHours.setDisplayedValues(hourValues);
		
		
		String[] minValues = new String[61];
		
		for (int i = 0; i < 61; i++) {
			String zero = "";
			if (i < 10)
				zero = "0";
			minValues[i] = zero + String.valueOf(i);
		}
		
		numberMinutes.setMinValue(0);
		numberMinutes.setMaxValue(60);
		numberMinutes.setDisplayedValues(minValues);
	}
	
	private void setAlarmDay(String timesCurrent, String time) {
		AlarmManager am = (AlarmManager) getSystemService(ALARM_SERVICE);
		
		int hours = TimePreference.getHour(time);
		int mins = TimePreference.getMinute(time);
		
		String[] curTimes = timesCurrent.split(":");
		
		int day = Integer.valueOf(curTimes[0]); // день недели
		if (day == 7) {
			day = 1;
		} else {
			day += 1;
		}
		
		int currentHours = Integer.valueOf(curTimes[1]);
		int currentMins = Integer.valueOf(curTimes[2]);
		
		int dayWeek = day;
		
		if (currentHours > hours) {
			if (dayWeek == 7) {
				dayWeek = 1;
			} else {
				dayWeek++;
			}
		} else if (hours == currentHours) {
			if (currentMins > mins) {
				if (dayWeek == 7) {
					dayWeek = 1;
				} else {
					dayWeek++;
				}
			}
		}
		
		setPreference(dayWeek, true); // сохраняем, то что выбран день
		
		// устанавливаем TextView подчеркнутым
		switch (dayWeek) {
			case 1:
				txtSunday.setBackgroundResource(R.drawable.bottom_selected);
				break;
			case 2:
				txtMonday.setBackgroundResource(R.drawable.bottom_selected);
				break;
			case 3:
				txtTuesday.setBackgroundResource(R.drawable.bottom_selected);
				break;
			case 4:
				txtWednesday.setBackgroundResource(R.drawable.bottom_selected);
				
				break;
			case 5:
				txtThursday.setBackgroundResource(R.drawable.bottom_selected);
				break;
			case 6:
				txtFriday.setBackgroundResource(R.drawable.bottom_selected);
				break;
			case 7:
				txtSaturday.setBackgroundResource(R.drawable.bottom_selected);
				break;
		}
		
		SharedPreferences.Editor prefEditor = prefs.edit();
		prefEditor.putBoolean(IS_ONCE, true);
		prefEditor.apply();
		AlarmReceiver.setTime(getApplicationContext(), time, dayWeek);
	}
	
	private void startTimer(final SharedPreferences.Editor prefEditor) {
		String time = numberHours.getValue() + ":" + numberMinutes.getValue();
		boolean isRestart = restartAlarm(time);

		String timesCurrent = "";
		if(android.os.Build.VERSION.SDK_INT >= 24){
			timesCurrent = new SimpleDateFormat("u:HH:mm").format(Calendar.getInstance().getTime());
		}
		else{
			Date d = new Date();
			Calendar cal = Calendar.getInstance();
			cal.setTime(d);
			int dow = cal.get(Calendar.DAY_OF_WEEK);
			switch (dow) {
				case Calendar.SUNDAY:
					dow = 7;
					break;
				case Calendar.MONDAY:
					dow = 1;
					break;
				case Calendar.TUESDAY:
					dow = 2;
					break;
				case Calendar.WEDNESDAY:
					dow = 3;
					break;
				case Calendar.THURSDAY:
					dow = 4;
					break;
				case Calendar.FRIDAY:
					dow = 5;
					break;
				case Calendar.SATURDAY:
					dow = 6;
					break;
			}
			timesCurrent = new SimpleDateFormat("HH:mm").format(Calendar.getInstance().getTime());
			timesCurrent = String.valueOf(dow) + ":" + timesCurrent;
		}
		
		String[] curTimes = timesCurrent.split(":");
		
		int day = Integer.valueOf(curTimes[0]); // день недели
		if (day == 7) {
			day = 1;
		} else {
			day += 1;
		}
		
		int currentHours = Integer.valueOf(curTimes[1]);
		int currentMins = Integer.valueOf(curTimes[2]);
		
		// если не удалось перезапустить, то есть ни 1 день не выбран
		if (!isRestart) {
			setAlarmDay(timesCurrent, time); // устанавливаем ближайший день
		}
		
		if (!prefs.getBoolean(IS_ALARM_WORK, false)) {
			Toast.makeText(this, "Будильник успешно запущен", Toast.LENGTH_SHORT).show();
		}
		
		imageView.setImageResource(R.drawable.ic_blu_bud);
		prefEditor.putBoolean(IS_ALARM_WORK, true); // устанавливаем флаг, то что будильник запущен
		prefEditor.putBoolean(IS_TIME_ALARM, false);
		prefEditor.apply();
		int hours = TimePreference.getHour(time);
		int mins = TimePreference.getMinute(time);
		
		boolean isNewDay = false;
		
		if (currentHours > hours)
			isNewDay = true;
		else if (hours == currentHours) {
			if (currentMins > mins) {
				isNewDay = true;
			}
		}
		
		hours += getHoursToDay(day, isNewDay);
		
		mins -= currentMins;
		
		if (mins < 0) {
			mins += 60;
			hours -= 1;
		}
		
		hours -= currentHours;
		
		if (hours == 0 && mins == 0) {
			hours += 24 * 7;
		} else if (hours < 0) {
			hours += 24 * 7;
		}
		
		long mlsec = (hours * 3600 + mins * 60) * 1000;
		
		timer = new CountDownTimer(mlsec, 1000) {
			@Override
			public void onTick(long l) {
				
				if (prefs.getBoolean(IS_TIME_ALARM, false)) {
					timer.onFinish();
				} else if (prefs.getBoolean(IS_ALARM_WORK, false)) {
					setTime(); // обновляем надпись, количество времени до будильника
				}
			}
			
			@Override
			public void onFinish() {
				stopAlarm();
				timer.cancel();
			}
		};
		
		timer.start();
	}




	public void btnStartClock(View view) {
		SharedPreferences.Editor prefEditor = prefs.edit();
		
		// если будильник работает и мы нажали на кнопку, останавливаем его
		if (prefs.getBoolean(IS_ALARM_WORK, false)) {
			stopAlarm();
			imageView.setImageResource(R.drawable.ic_grey_bud);
			prefEditor.putBoolean(IS_ALARM_WORK, false);
			Toast.makeText(this, "Будильник успешно остановлен", Toast.LENGTH_SHORT).show();
			if (timer != null) {
				timer.cancel();
			}
			txtProgress.setText("Будильник остановлен");
		} else {
			// запускаем будильник
			startTimer(prefEditor);
		}
		
		prefEditor.apply();
	}
	
	boolean isNotSelected() {
		boolean isNotSelected = true;
		
		if (prefs.getBoolean(SUNDAY_DAY, false)) {
			isNotSelected = false;
		} else if (prefs.getBoolean(MONDAY_DAY, false)) {
			isNotSelected = false;
		} else if (prefs.getBoolean(TUESDAY_DAY, false)) {
			isNotSelected = false;
		} else if (prefs.getBoolean(WEDNESDAY_DAY, false)) {
			isNotSelected = false;
		} else if (prefs.getBoolean(THURSDAY_DAY, false)) {
			isNotSelected = false;
		} else if (prefs.getBoolean(FRIDAY_DAY, false)) {
			isNotSelected = false;
		} else if (prefs.getBoolean(SATURDAY_DAY, false)) {
			isNotSelected = false;
		}
		
		// возращаем true , если ни 1 день недели не выбран
		return isNotSelected;
	}
	
	private void stopAlarm() {
		boolean isTimeAlarm = prefs.getBoolean(IS_TIME_ALARM, false);
		if(isTimeAlarm)
		{
			txtProgress.setText("Будильник остановлен");
			imageView.setImageResource(R.drawable.ic_grey_bud);
		}
		
		boolean isOnce = prefs.getBoolean(IS_ONCE, false);
		
		// если работает Alarm, то останавливаем
		if (prefs.getBoolean(IS_ALARM_WORK, false)) {
			for (int i = 1; i <= 7; i++) {
				if (isOnce && isTimeAlarm) {
					setPreference(i, false);
				}
				AlarmReceiver.stopAlarm(getApplicationContext(), i);
			}
		}
		
		reloadDays();
		
		SharedPreferences.Editor editor = prefs.edit();
		editor.putBoolean(IS_ONCE, false);
		editor.putBoolean(IS_ALARM_WORK, false);
		editor.apply();
	}
	
	private boolean restartAlarm(String time) {
		boolean isRestart = false;
		
		stopAlarm(); // останавливаем все будильники
		
		if (prefs.getBoolean(SUNDAY_DAY, false)) {
			AlarmReceiver.setTime(this, time, 1);
			isRestart = true;
		}
		
		if (prefs.getBoolean(MONDAY_DAY, false)) {
			AlarmReceiver.setTime(this, time, 2);
			isRestart = true;
		}
		
		if (prefs.getBoolean(TUESDAY_DAY, false)) {
			AlarmReceiver.setTime(this, time, 3);
			isRestart = true;
		}
		
		if (prefs.getBoolean(WEDNESDAY_DAY, false)) {
			AlarmReceiver.setTime(this, time, 4);
			isRestart = true;
		}
		
		if (prefs.getBoolean(THURSDAY_DAY, false)) {
			AlarmReceiver.setTime(this, time, 5);
			isRestart = true;
		}
		
		if (prefs.getBoolean(FRIDAY_DAY, false)) {
			AlarmReceiver.setTime(this, time, 6);
			isRestart = true;
		}
		
		if (prefs.getBoolean(SATURDAY_DAY, false)) {
			AlarmReceiver.setTime(this, time, 7);
			isRestart = true;
		}
		
		if (isRestart)
			imageView.setImageResource(R.drawable.ic_blu_bud);
		
		
		// возращаем true если будильник удалось перезапустить ( выбран хотя бы 1 день )
		return isRestart;
	}
	
	private void setPreference(int tag, boolean isChecked) {
		// сохраняем выбранные дни
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
		SharedPreferences.Editor prefEditor = prefs.edit();
		
		switch (tag) {
			case 1:
				prefEditor.putBoolean(SUNDAY_DAY, isChecked);
				
				break;
			case 2:
				prefEditor.putBoolean(MONDAY_DAY, isChecked);
				break;
			case 3:
				prefEditor.putBoolean(TUESDAY_DAY, isChecked);
				break;
			case 4:
				prefEditor.putBoolean(WEDNESDAY_DAY, isChecked);
				break;
			case 5:
				prefEditor.putBoolean(THURSDAY_DAY, isChecked);
				break;
			case 6:
				prefEditor.putBoolean(FRIDAY_DAY, isChecked);
				break;
			case 7:
				prefEditor.putBoolean(SATURDAY_DAY, isChecked);
				break;
		}
		
		prefEditor.apply();
		
	}
	
	private void setTime() {
		String time = numberHours.getValue() + ":" + numberMinutes.getValue(); // получаем установленное время
		
		int hours = TimePreference.getHour(time); // получаем часы
		int mins = TimePreference.getMinute(time); // получаем минуты

		String timesCurrent = "";
		if(android.os.Build.VERSION.SDK_INT >= 24){
			timesCurrent = new SimpleDateFormat("u:HH:mm").format(Calendar.getInstance().getTime());
		}
		else{
			Date d = new Date();
			Calendar cal = Calendar.getInstance();
			cal.setTime(d);
			int dow = cal.get(Calendar.DAY_OF_WEEK);
			switch (dow) {
				case Calendar.SUNDAY:
					dow = 7;
					break;
				case Calendar.MONDAY:
					dow = 1;
					break;
				case Calendar.TUESDAY:
					dow = 2;
					break;
				case Calendar.WEDNESDAY:
					dow = 3;
					break;
				case Calendar.THURSDAY:
					dow = 4;
					break;
				case Calendar.FRIDAY:
					dow = 5;
					break;
				case Calendar.SATURDAY:
					dow = 6;
					break;
			}
			timesCurrent = new SimpleDateFormat("HH:mm").format(Calendar.getInstance().getTime());
			timesCurrent = String.valueOf(dow) + ":" + timesCurrent;
		}
		
		String[] curTimes = timesCurrent.split(":");
		
		int day = Integer.valueOf(curTimes[0]); // день недели
		if (day == 7) {
			day = 1;
		} else {
			day += 1;
		}
		
		int currentHours = Integer.valueOf(curTimes[1]);
		int currentMins = Integer.valueOf(curTimes[2]);
		
		
		boolean isNewDay = false;
		
		// если текущее время больше установленного, то это новый день
		if (currentHours > hours)
			isNewDay = true;
		else if (hours == currentHours) {
			if (mins < currentMins) {
				isNewDay = true;
			}
		}
		
		int hoursToDay = getHoursToDay(day, isNewDay); // получаем количество часов до дня
		
		hours += hoursToDay;
		
		int resHours, resMins;
		
		resMins = mins - currentMins; // вычитаем минуты
		
		// если получилось отрицательное число, то берем 1 час
		if (resMins < 0) {
			resMins += 60;
			hours -= 1;
		}
		
		resHours = hours - currentHours;
		
		if (resHours == 0 && resMins == 0) {
			resHours += 24 * 7;
		}
		
		// если часы получились отрицательные, то будильник зазвончит через неделю
		if (resHours < 0) {
			resHours += 24 * 7;
		}
		
		String strTime = "Будильник зазвонит через";
		
		if (resHours > 0) {
			if (resHours > 24) {
				int resDays = resHours / 24;
				resHours -= resDays * 24;
				strTime += " " + resDays + " " + getCase(resDays, DAYS);
			}
			
			strTime += " " + resHours + " " + getCase(resHours, HOURS);
		}
		
		strTime += " " + resMins + " " + getCase(resMins, MINUTES);
		
		txtProgress.setText(strTime);
	}
	
	private int getHoursToDay(int dayWeek, boolean isNewDay) {
		Dictionary<Integer, String> dist = new Hashtable<Integer, String>();
		
		dist.put(1, SUNDAY_DAY);
		dist.put(2, MONDAY_DAY);
		dist.put(3, TUESDAY_DAY);
		dist.put(4, WEDNESDAY_DAY);
		dist.put(5, THURSDAY_DAY);
		dist.put(6, FRIDAY_DAY);
		dist.put(7, SATURDAY_DAY);
		
		// инициализируем дни
		
		int count = 0;
		
		int curDay = dayWeek;
		
		while (count != 7) {
			
			if (prefs.getBoolean(dist.get(curDay), false)) {
				
				// если день выбранный и текущий равны и время текущее меньше установленного, то это новый день
				if (curDay == dayWeek && isNewDay) {
					curDay += 1;
					continue;
				}
				
				// если дни равны и время текущее меньше установлеенного значит будильник должен сработать сегодня
				if (curDay == dayWeek && !isNewDay)
					return 0;
				
				int add = 0;
				if (dayWeek > curDay) {
					add = 7;
				}
				
				// высчитываем количество часов до дня
				return (curDay + add - dayWeek) * 24;
			}
			
			curDay++;
			
			if (curDay > 7) {
				curDay = 1;
			}
			
			count++;
		}
		
		return 0;
	}
	
	private String getCase(Integer value, String[] options) {
		
		value = Math.abs(value) % 100;
		
		if (value > 10 && value < 15)
			return options[2];
		
		value %= 10;
		if (value == 1) return options[0];
		if (value > 1 && value < 5) return options[1];
		return options[2];
	}
}
