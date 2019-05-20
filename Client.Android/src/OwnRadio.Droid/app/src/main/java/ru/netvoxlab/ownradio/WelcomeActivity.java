package ru.netvoxlab.ownradio;

import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AppCompatActivity;
import android.text.Html;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;
import java.util.UUID;

import ru.netvoxlab.ownradio.rdevapi.RdevApiCalls;
import ru.netvoxlab.ownradio.receivers.NetworkStateReceiver;

import static ru.netvoxlab.ownradio.RequestAPIService.ACTION_GETNEXTTRACK;
import static ru.netvoxlab.ownradio.RequestAPIService.EXTRA_COUNT;
import static ru.netvoxlab.ownradio.RequestAPIService.EXTRA_DEVICEID;
import static ru.netvoxlab.ownradio.RequestAPIService.EXTRA_USERID;

public class WelcomeActivity extends AppCompatActivity implements NetworkStateReceiver.NetworkStateReceiverListener{
	
	private ViewPager viewPager;
	private MyViewPagerAdapter myViewPagerAdapter;
	private Button btnTryAgainCaching;
	private ProgressBar progressBar;
	private TextView textCountTryCaching;
	private LinearLayout dotsLayout;
	private TextView[] dots;
	private int[] layouts;
	private PrefManager prefManager;
//	private VideoView videoView;
	private Uri uriVideoView;
	private String deviceId;
	private String UserId;
	final Handler loadHandler = new Handler();
	private NetworkStateReceiver networkStateReceiver;
	SharedPreferences sp;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		//Меняем тему, используемую при запуске приложения, на основную
		setTheme(R.style.AppTheme);
		super.onCreate(savedInstanceState);
		this.sp = PreferenceManager.getDefaultSharedPreferences(this);
		// Checking for first time launch - before calling setContentView()
		prefManager = new PrefManager(this);
		if (!prefManager.isFirstTimeLaunch()) {
			launchHomeScreen();
			((App)getApplicationContext()).setAutoPlay(true);
			return;
		}
		networkStateReceiver = new NetworkStateReceiver();
		networkStateReceiver.addListener(this);
		this.registerReceiver(networkStateReceiver, new IntentFilter(android.net.ConnectivityManager.CONNECTIVITY_ACTION));
		
		//генерируем deviceId и регистрируем устройство
		try {
			deviceId = prefManager.getDeviceId();
			if (deviceId.isEmpty()) {
				deviceId = UUID.randomUUID().toString();
				prefManager.setDeviceId(deviceId);
				String UserName = "NewUser";
				String DeviceName = Build.BRAND + " " + Build.PRODUCT;

				//new APICalls(getApplicationContext()).RegisterDevice(deviceId, DeviceName + " " + getApplicationContext().getPackageManager().getPackageInfo(getApplicationContext().getPackageName(), PackageManager.GET_META_DATA).versionName);
				RdevApiCalls rdevApiCalls = new RdevApiCalls(getApplicationContext());
				//final Map<String, String> authMap = rdevApiCalls.GetAuthToken();
				//String token = authMap.get("token");
				rdevApiCalls.RegisterDevice(deviceId, DeviceName + " " + getApplicationContext().getPackageManager().getPackageInfo(getApplicationContext().getPackageName(), PackageManager.GET_META_DATA).versionName);

				UserId = rdevApiCalls.GetDeviceInfo(deviceId).get("OK").getResult().get("userid");
				//String UserId = new APICalls(this).GetUserId(deviceId);
				prefManager.setPrefItem("UserID", UserId);
				prefManager.setPrefItem("UserName", UserName);
				prefManager.setPrefItem("DeviceName", DeviceName);
			}
		} catch (Exception ex) {
			ex.printStackTrace();
		}
		
		//Запускаем загрузку треков
		if (new TrackDataAccess(getApplicationContext()).GetExistTracksCount() < 1) {
			Intent downloaderIntent = new Intent(this, LongRequestAPIService.class);
			downloaderIntent.setAction(ACTION_GETNEXTTRACK);
			downloaderIntent.putExtra(EXTRA_DEVICEID, deviceId);
			downloaderIntent.putExtra(EXTRA_USERID, UserId);
			downloaderIntent.putExtra(EXTRA_COUNT, 3);
			startService(downloaderIntent);
		}
		
		// Making notification bar transparent
		if (Build.VERSION.SDK_INT >= 21) {
			getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_STABLE | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN);
		}
		
		setContentView(R.layout.activity_welcome);
		
		viewPager = findViewById(R.id.view_pager);
		dotsLayout = findViewById(R.id.layoutDots);
		
//		videoView = findViewById(R.id.videoView);
//		videoView.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
//			@Override
//			public void onPrepared(MediaPlayer mp) {
//				mp.setLooping(true);
//				videoView.requestFocus();
//				videoView.start();
//			}
//		});
		try {
			uriVideoView = Uri.parse("android.resource://" + getPackageName() + "/" + R.raw.video_portrait);
//			videoView.setVideoURI(uriVideoView);
			
//			setDimension();
		}catch (Exception ex){
			new Utilites().SendInformationTxt(getApplicationContext(), "Ошибка при подключении фонового видео " + ex.getLocalizedMessage());
		}
		
		btnTryAgainCaching = findViewById(R.id.tryCachingAgain);
		btnTryAgainCaching.setVisibility(View.GONE);
		btnTryAgainCaching.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View view) {
				if(!new CheckConnection().CheckInetConnection(getApplicationContext())) {
					if (textCountTryCaching != null)
						textCountTryCaching.setText("Проверьте подключение к интернету");
					return;
				}
				((App)getApplicationContext()).setCountDownloadTrying(0);
				if(progressBar != null)
					progressBar.setVisibility(View.VISIBLE);
				//Запускаем загрузку треков
				Intent downloaderIntent = new Intent(getApplicationContext(), LongRequestAPIService.class);
				downloaderIntent.setAction(ACTION_GETNEXTTRACK);
				downloaderIntent.putExtra(EXTRA_DEVICEID, deviceId);
				downloaderIntent.putExtra(EXTRA_COUNT, 3);
				startService(downloaderIntent);
				btnTryAgainCaching.setVisibility(View.GONE);
			}
		});
				
		// layouts of all welcome sliders
		// add few more layouts if you want
		layouts = new int[]{
				R.layout.launch_screen,
				R.layout.slide1,
				R.layout.slide2,
				R.layout.slide3
//				,
//				R.layout.welcome_slide1,
//				R.layout.welcome_slide2,
//				R.layout.welcome_slide3,
//				R.layout.welcome_slide4,
//				R.layout.welcome_slide5
		};
		
		// adding bottom dots
		addBottomDots(0);
		
		// making notification bar transparent
		changeStatusBarColor();
		
		myViewPagerAdapter = new MyViewPagerAdapter();
		viewPager.setAdapter(myViewPagerAdapter);
		viewPager.addOnPageChangeListener(viewPagerPageChangeListener);

		final Timer timer = new Timer();
		final Handler handler = new Handler();
		final Runnable Update = new Runnable(){
			public void run(){
				int current = viewPager.getCurrentItem();
				if(current >= layouts.length-1){
//					timer.cancel();
					return;
				}
				current++;
				viewPager.setCurrentItem(current);
			}
		};
		 // This will create a new Thread
		timer .schedule(new TimerTask() { // task to be scheduled
			@Override
			public void run() {
				handler.post(Update);
			}
		}, 5000, 5000);
		}
	
	@Override
	public void onResume(){
		super.onResume();
		networkStateReceiver.addListener(this);
		this.registerReceiver(networkStateReceiver, new IntentFilter(android.net.ConnectivityManager.CONNECTIVITY_ACTION));
	}
	
	@Override
	public void onPause() {
		networkStateReceiver.removeListener(this);
		this.unregisterReceiver(networkStateReceiver);
		super.onPause();
	}
	
	@Override
	public void onDestroy(){
		try{
			//при разрушении активности отписываемся от событий переподключения интернета
			networkStateReceiver.removeListener(this);
			this.unregisterReceiver(networkStateReceiver);
		}catch(Exception e)
		{
			
		}
		super.onDestroy();
	}
	
	@Override
	public void networkAvailable() {
	}
	
	@Override
	public void networkUnavailable() {
		//Если интернет соединение разорвалось и показывается слайд с прогрессбаром предзагрузки
		// - показываем сообщение об отсутсвии интернета и кнопку "попробовать еще раз"
		if(btnTryAgainCaching != null) {
			btnTryAgainCaching.setVisibility(View.VISIBLE);
			if(textCountTryCaching != null)
				textCountTryCaching.setText(getResources().getString(R.string.slide_5_check_connection));
		}
	}
	private void addBottomDots(int currentPage) {
		dots = new TextView[layouts.length];
		
		int[] colorsActive = getResources().getIntArray(R.array.array_dot_active);
		int[] colorsInactive = getResources().getIntArray(R.array.array_dot_inactive);
		dotsLayout.removeAllViews();
		for (int i = 0; i < dots.length; i++) {
			dots[i] = new TextView(this);
			dots[i].setText(Html.fromHtml("&#8226;"));
			dots[i].setTextSize(35);
			dots[i].setTextColor(colorsInactive[currentPage]);
			dotsLayout.addView(dots[i]);
		}
		
		if (dots.length > 0)
			dots[currentPage].setTextColor(colorsActive[currentPage]);
	}
	
	private int getItem(int i) {
		return viewPager.getCurrentItem() + i;
	}
	
	private void launchHomeScreen() {
		prefManager.setFirstTimeLaunch(false);
		startActivity(new Intent(WelcomeActivity.this, MainActivity.class));
		finish();
	}
	
	//  viewpager change listener
	ViewPager.OnPageChangeListener viewPagerPageChangeListener = new ViewPager.OnPageChangeListener() {
		
		@Override
		public void onPageSelected(int position) {
			addBottomDots(position);
			
			if (position == layouts.length - 1) {
				final Timer loadTimer = new Timer();
				final Runnable LoadTrack = new Runnable(){
					public void run(){
						//Если поле для вывода доступно - выводим номер попытки кеширования и количество загруженных треков
						if(textCountTryCaching != null)
							textCountTryCaching.setText(((App)getApplicationContext()).getCountDownloadTrying() + " попытка кеширования \n (загружено " + new TrackDataAccess(getApplicationContext()).GetExistTracksCount() + ")");
						//Если хотя бы один трек загрузился - запускаем основную активность
						//Иначе - после 10 неудачных попыток загрузки показываем кнопку "попробовать еще раз",
						//При отсутсвии интернета также выводим сообщение об этом
						if(new TrackDataAccess(getApplicationContext()).GetExistTracksCount() >= 1){
							launchHomeScreen();
							loadTimer.cancel();
							return;
						} else if (((App)getApplicationContext()).getCountDownloadTrying() >= 10 || !new CheckConnection().CheckInetConnection(getApplicationContext())){
							if(!new CheckConnection().CheckInetConnection(getApplicationContext()))
								if(textCountTryCaching != null)
									textCountTryCaching.setText(getResources().getString(R.string.slide_5_check_connection));
							btnTryAgainCaching.setVisibility(View.VISIBLE);
							if(progressBar != null)
								progressBar.setVisibility(View.GONE);
						} //else
//						if(new CheckConnection().CheckInetConnection(getApplicationContext())
							//Если треков нет - запускаем загрузку трех треков
//							if (new TrackDataAccess(getApplicationContext()).GetExistTracksCount() < 1) {
//								btnTryAgainCaching.setVisibility(View.GONE);
////								((App)getApplicationContext()).setCountDownloadTrying((((App)getApplicationContext()).getCountDownloadTrying()+1));
////								Log.e("OWNRADIO", "Загрузка номер " + ((App)getApplicationContext()).getCountDownloadTrying() );
//								Intent downloaderIntent = new Intent(getApplicationContext(), LongRequestAPIServiceRequestAPIService.class);
//								downloaderIntent.setAction(ACTION_GETNEXTTRACK);
//								downloaderIntent.putExtra(EXTRA_DEVICEID, deviceId);
//								downloaderIntent.putExtra(EXTRA_COUNT, 1);
//								startService(downloaderIntent);
//							}
						}
				};
				loadTimer .schedule(new TimerTask() { // task to be scheduled
					@Override
					public void run() {
						loadHandler.post(LoadTrack);
					}
				}, 500, 3000);
			} else {
				// still pages are left
				btnTryAgainCaching.setVisibility(View.GONE);
			}
		}
		
		@Override
		public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {

		}
		
		@Override
		public void onPageScrollStateChanged(int state) {
			
		}
	};
	
	/**
	 * Making notification bar transparent
	 */
	private void changeStatusBarColor() {
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
			Window window = getWindow();
			window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
			window.setStatusBarColor(Color.TRANSPARENT);
		}
	}
	
	/**
	 * View pager adapter
	 */
	public class MyViewPagerAdapter extends PagerAdapter {
		private LayoutInflater layoutInflater;
		
		public MyViewPagerAdapter() {
		}
		
		@Override
		public Object instantiateItem(ViewGroup container, int position) {
			layoutInflater = (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);
			
			View view = layoutInflater.inflate(layouts[position], container, false);
			container.addView(view);
			
			if(position == layouts.length - 1) {
				progressBar = findViewById(R.id.progressBar);
				if(progressBar != null)
					progressBar.setVisibility(View.VISIBLE);
				textCountTryCaching = findViewById(R.id.countTryCaching);
			}
			return view;
		}
		
		@Override
		public int getCount() {
			return layouts.length;
		}
		
		@Override
		public boolean isViewFromObject(View view, Object obj) {
			return view == obj;
		}
		
		
		@Override
		public void destroyItem(ViewGroup container, int position, Object object) {
			View view = (View) object;
			container.removeView(view);
		}
	}
	
	
	// This method set dimension for video view
//	private void setDimension() {
//		// Adjust the size of the video
//		// so it fits on the screen
//		float videoProportion = getVideoProportion();
//		int screenWidth = getResources().getDisplayMetrics().widthPixels;
//		int screenHeight = getResources().getDisplayMetrics().heightPixels;
//		float screenProportion = (float) screenHeight / (float) screenWidth;
//		android.view.ViewGroup.LayoutParams lp = videoView.getLayoutParams();
//
//		if (videoProportion < screenProportion) {
//			lp.height= screenHeight;
//			lp.width = (int) ((float) screenHeight / videoProportion);
//		} else {
//			lp.width = screenWidth;
//			lp.height = (int) ((float) screenWidth * videoProportion);
//		}
//		videoView.setLayoutParams(lp);
//	}
	
	// This method gets the proportion of the video that you want to display.
	private float getVideoProportion(){
			return 1.8f; //height/width video
		}
}
