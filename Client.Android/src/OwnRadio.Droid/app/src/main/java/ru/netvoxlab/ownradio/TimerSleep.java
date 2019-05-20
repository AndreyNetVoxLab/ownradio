package ru.netvoxlab.ownradio;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.CountDownTimer;
import android.preference.PreferenceManager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.jesusm.holocircleseekbar.lib.HoloCircleSeekBar;


import static ru.netvoxlab.ownradio.Constants.IS_TIMER_WORK;
import static ru.netvoxlab.ownradio.Constants.TIMER_TIME;
/**
 * Created by valko on 19.09.2018
 */

public class TimerSleep extends AppCompatActivity {
	
	private int timeSeconds;
	private int currentTime;
	private final String[] HOURS = {"час", "часа", "часов"};
	private final String[] MINUTES = {"минута", "минуты", "минут"};
	
	private boolean isEnableTimer = false;
	private CountDownTimer timer;
	
	
	private ImageView btnGo;
	private TextView timeDuration;
	private TextView txtProgress;
	private Toolbar toolbar;
	private HoloCircleSeekBar picker;



	@Override
	protected void onCreate(Bundle savedInstanceState) {
		//Меняем тему, используемую при запуске приложения, на основную
		setTheme(R.style.AppTheme);
		
		super.onCreate(savedInstanceState);
		this.requestWindowFeature(Window.FEATURE_NO_TITLE);
		setContentView(R.layout.activity_timer_sleep);
		
		picker = findViewById(R.id.picker);
		picker.setMax(4 * 60);
		
		// init all components
		toolbar = findViewById(R.id.toolbar);
		btnGo = findViewById(R.id.btnGo);
		timeDuration = findViewById(R.id.timeDuration);
		txtProgress = findViewById(R.id.txtProgress);
		
		setSupportActionBar(toolbar);
		
		if (getSupportActionBar() != null) {
			getSupportActionBar().setDisplayHomeAsUpEnabled(true);
			getSupportActionBar().setDisplayShowHomeEnabled(true);
		}
		toolbar.setNavigationOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				onBackPressed();
			}
		});

		final SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
		final SharedPreferences.Editor prefEditor = prefs.edit();


		//если таймер установлен
		if(prefs.getBoolean(IS_TIMER_WORK, false)){
		    timeSeconds = prefs.getInt(TIMER_TIME, 0);
		    picker.setValue(timeSeconds / 60);
			SetTextTime(timeSeconds / 60);
            btnGo.setImageResource(R.drawable.ic_blu_bud);
            isEnableTimer = prefs.getBoolean(IS_TIMER_WORK, false);
			StartTimer(this);
        }
        else{
            btnGo.setImageResource(R.drawable.ic_grey_bud);
        }

		btnGo.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				// если таймер идет, то останавливаем его
				if (isEnableTimer) {
					timeDuration.setText("Таймер выключен");
					isEnableTimer = false;
					timer.cancel();
					Toast.makeText(getApplicationContext(), "Таймер успешно остановлен", Toast.LENGTH_SHORT).show();
					btnGo.setImageResource(R.drawable.ic_grey_bud);
					prefEditor.putBoolean(IS_TIMER_WORK, false);
				} else {
					// иначе запускаем таймер
					StartTimer(getApplicationContext());
					Toast.makeText(getApplicationContext(), "Таймер успешно запущен", Toast.LENGTH_SHORT).show();
					prefEditor.putBoolean(IS_TIMER_WORK, true);
					prefEditor.putInt(TIMER_TIME, timeSeconds);
					btnGo.setImageResource(R.drawable.ic_blu_bud);
				}
				prefEditor.apply();
			}
		});
		
		picker.setOnSeekBarChangeListener(new HoloCircleSeekBar.OnCircleSeekBarChangeListener() {
			@Override
			public void onProgressChanged(HoloCircleSeekBar holoCircleSeekBar, int progress, boolean b) {
				if(progress > 0){
					SetTextTime(progress);
				}
			}
			
			@Override
			public void onStartTrackingTouch(HoloCircleSeekBar holoCircleSeekBar) {
				// по нажатию на seekbar обновляем его значение
				int progress = holoCircleSeekBar.getValue();
				if(progress > 0){
					SetTextTime(progress);
				}
			}
			
			@Override
			public void onStopTrackingTouch(HoloCircleSeekBar holoCircleSeekBar) {
				RestartTimer(); // пытаемся перезапустить таймер
			}
		});
	}
	
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		// кнопка назад
		switch (item.getItemId()) {
			case android.R.id.home:
				onBackPressed();
				return true;
		}
		return super.onOptionsItemSelected(item);
	}
	
	private void RestartTimer() {
		// если время таймера текущее отличается от seekbar и таймер идет, то перезапускаем его
		if (currentTime != timeSeconds && isEnableTimer) {
			timer.cancel();
			StartTimer(getApplicationContext());
			Toast.makeText(getApplicationContext(), "Таймер успешно перезапущен", Toast.LENGTH_SHORT).show();
		}
	}
	
	private void SetTextTime(int progress) {
		int _hours = progress / 60;
		int _min = progress - _hours * 60;
		
		String hours = _hours > 9 ? String.valueOf(_hours) : "0" + _hours;
		String min = _min > 9 ? String.valueOf(_min) : "0" + _min;
		String time = hours + ":" + min;
		
		txtProgress.setText(time);
		timeSeconds = progress * 60;
	}
	
	public void StartTimer(final Context context) {
		currentTime = timeSeconds;
		
		// создаем таймер
		timer = new CountDownTimer(timeSeconds * 1000, 1000) {
			public void onTick(long millisUntilFinished) {
				int _seconds = (int) (millisUntilFinished / 1000); // ищем секунды
				int _hours = _seconds / 3600; // находим часы
				_seconds -= _hours * 3600; // записываем остаток секунд
				int _min = _seconds / 60; // находим минуты
				_seconds -= _min * 60; // записываем остаток секунд
				_min += _seconds > 0 ? 1 : 0; // если секунд больше 0 то добавляем минуту для отображения
				
				String hours = _hours > 9 ? String.valueOf(_hours) : "0" + _hours;
				hours += " " + GetCase(_hours, HOURS);
				String min = _min > 9 ? String.valueOf(_min) : "0" + _min;
				min += " " + GetCase(_min, MINUTES);
				String time = hours + " " + min;
				
				timeDuration.setText("Таймер выключится через " + time);
				
				btnGo.setImageResource(R.drawable.ic_blu_bud);
			}


			public void onFinish() {
				setResult(RESULT_OK, new Intent());
				final SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
				final SharedPreferences.Editor prefEditor = prefs.edit();
				prefEditor.putBoolean(IS_TIMER_WORK, false);
				prefEditor.apply();
				finish();
			}
		};
		timer.start();
		isEnableTimer = true;
	}
	
	// ставим окончание по значению
	private String GetCase(Integer value, String[] options) {
		
		value = Math.abs(value) % 100;
		
		if (value > 10 && value < 15)
			return options[2];
		
		value %= 10;
		if (value == 1) return options[0];
		if (value > 1 && value < 5) return options[1];
		return options[2];
	}
}

