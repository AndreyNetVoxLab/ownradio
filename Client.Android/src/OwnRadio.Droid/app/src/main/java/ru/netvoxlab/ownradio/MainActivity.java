package ru.netvoxlab.ownradio;

import android.app.ProgressDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.drawable.ColorDrawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.RemoteException;
import android.os.SystemClock;
import android.preference.PreferenceActivity;
import android.preference.PreferenceManager;
import android.support.annotation.RequiresApi;
import android.support.constraint.ConstraintLayout;
import android.support.design.widget.NavigationView;
import android.support.v4.content.ContextCompat;
import android.support.v4.media.session.PlaybackStateCompat;
import android.support.v4.view.GravityCompat;
import android.support.v4.view.MenuItemCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.SwitchCompat;
import android.support.v7.widget.Toolbar;
import android.text.Html;
import android.text.method.LinkMovementMethod;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.ViewStub;
import android.view.Window;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.RatingBar;
import android.widget.TextView;
import android.widget.Toast;

import com.android.vending.billing.IInAppBillingService;
import com.google.gson.Gson;

import org.apache.commons.io.FileUtils;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Serializable;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Pattern;

import ru.netvoxlab.ownradio.rdevapi.RdevApiCalls;
import ru.netvoxlab.ownradio.receivers.NetworkStateReceiver;

import static ru.netvoxlab.ownradio.Constants.ACTION_UPDATE_FILLCACHE_PROGRESS;
import static ru.netvoxlab.ownradio.Constants.OPTIMIZE_DISABLED;
import static ru.netvoxlab.ownradio.Constants.CURRENT_TRACK_ID;
import static ru.netvoxlab.ownradio.Constants.CURRENT_TRACK_URL;
import static ru.netvoxlab.ownradio.Constants.EXTRA_FILLCACHE_PROGRESS;
import static ru.netvoxlab.ownradio.Constants.OPTIMIZE_STATUS;
import static ru.netvoxlab.ownradio.Constants.IS_ALARM_WORK;
import static ru.netvoxlab.ownradio.Constants.IS_TIME_ALARM;
import static ru.netvoxlab.ownradio.Constants.OPTIMIZE_ENABLED;
import static ru.netvoxlab.ownradio.Constants.TAG;
import static ru.netvoxlab.ownradio.Constants.IS_TIMER_WORK;
import static ru.netvoxlab.ownradio.RequestAPIService.EXTRA_USERID;

public class MainActivity extends AppCompatActivity implements NavigationView.OnNavigationItemSelectedListener, View.OnTouchListener, NetworkStateReceiver.NetworkStateReceiverListener {

	private APICalls apiCalls;
	private RdevApiCalls rdevApiCalls;

	String DeviceId;
	String UserId;
	SharedPreferences sp;
	BroadcastReceiver headSetReceiver = new MusicBroadcastReceiver();
	BroadcastReceiver remoteControlReceiver = new RemoteControlReceiver();
	boolean isBound = false;
	private MediaPlayerService.MediaPlayerServiceBinder binder;
	MediaPlayerServiceConnection mediaPlayerServiceConnection;
	private Intent mediaPlayerServiceIntent;
	ImageButton btnPlayPause;
	ImageButton btnNext;
	ImageButton btnSkipTrack;
	ImageButton btnTimer;
	ImageButton btnAlarmClock;
	ImageButton buttonShare;
	//	TrackDB trackDB;
	private Handler handler = new Handler();
	private Handler handlerEvent = new Handler();
	ProgressBar progressBar;
	TextView textInfo;
	
	TextView textVersionName;
	TextView textDeviceID;
	TextView textUserID;
	TextView textTrackID;
	TextView txtTrackCount;
	TextView txtTrackCountInFolder;
	TextView txtCountPlayTracks;
	
	TextView txtMemoryUsed;
	TextView txtTrackTitle;
	TextView txtTrackArtist;
	
	TextView txtFillCacheProgress;
	
	SwitchCompat switchOptimizeStatus;
	
	Toolbar toolbar;
	ViewStub viewStubRate;
	RatingBar ratingBar;
	ImageButton btnRateOK;
	ImageButton btnRateApp;
	Button btnRateCancel;
	View rateRequestLayout;
	TextView rateRequestMessage;
	boolean isRateInflated = false;
	
	ImageButton btnMenu;
	TextView txtProgressLeft;
	TextView txtProgressRight;
	
	LinearLayout layoutDevelopersInfo;
	public static File filePath;
	boolean flagDevInfo = false;
	
	int mCountListenForRateDialog = 5;
	
	public static final String ActionProgressBarUpdate = "ru.netvoxlab.ownradio.action.PROGRESSBAR_UPDATE";
	public static final String ActionProgressBarFirstTracksLoad = "ru.netvoxlab.ownradio.action.PROGRESSBAR_FIRST_UPDATE";
	public static final String ActionTrackInfoUpdate = "ru.netvoxlab.ownradio.action.TRACK_INFO_UPDATE";
	public static final String ActionButtonImgUpdate = "ru.netvoxlab.ownradio.action.BTN_PLAYPAUSE_IMG_UPDATE";
	public static final String ActionSendInfoTxt = "ru.netvoxlab.ownradio.action.SEND_INFO_TXT";
	public static final String ActionStopPlayback = "ru.netvoxlab.ownradio.action.STOP_PLAYBACK";
	public static final String ActionCheckCountTracksAndDownloadIfNotEnought = "ru.netvoxlab.ownradio.action.CHECK_TRACKS_AND_DOWNLOAD";
	public static final String ActionShowRateRequest = "ru.netvoxlab.ownradio.SHOW_RATING_REQUEST";
	public static final String ActionNotFoundTrack = "ru.netvoxlab.ownradio.NOT_FOUND_TRACK";
	public static final String ActionAlarm = "ru.netvoxlab.ownradio.ACTION_ALARM";
	public static final String ActionCheckListensCount = "ru.netvoxlab.ownradio.CHECK_LISTENS_COUNT";
	public static final String ActionPopupClose = "ru.netvoxlab.ownradio.CLOSE_POPUP";
	public static final String ActionPopupSetForeground = "ru.netwoxlab.SET_FOREGROUND";



//	private static final Bundle mQuerySkus = new Bundle();
//	private static Bundle mSkuDetails;
//	private static final ArrayList<String> _skuList = new ArrayList<> ();
	
	public static final String NumListenedTracks = "NUM_TRACKS_LISTENED_IN_VERSION";
	public static final String version = "VERSION";
	ProgressDialog dialog;
	int numberOfTaps = 0;
	long lastTapTimeMs = 0;
	long touchDownMs = 0;
	PrefManager prefManager;
	private long lastClickTime = 0;
	
	private NetworkStateReceiver networkStateReceiver;
	
	public String GetTrackId() {
		return binder.GetMediaPlayerService().TrackID;
	}

	private IInAppBillingService mService;

	Intent intent;

	ServiceConnection mServiceConn = new ServiceConnection() {
		@Override
		public void onServiceConnected(ComponentName name, IBinder service) {
			mService = IInAppBillingService.Stub.asInterface(service);
			SubscribeManager.CheckSubscribeStatus(getApplicationContext(), mService);

//			mService = IInAppBillingService.Stub.asInterface(service);
//			try{
//				Bundle ownedItems = mService.getPurchases(3, getPackageName(), "subs", null);
//				int response = ownedItems.getInt("RESPONSE_CODE");
//				if (response == 0) {
//					ArrayList<String> purchase_item_list = ownedItems.getStringArrayList("INAPP_PURCHASE_ITEM_LIST");
//					int purchaseIndex = purchase_item_list.indexOf("test_subscribe_1");
//					if(purchaseIndex != -1){
//						prefManager.setPrefItemBool("is_subscribed", true);
//					}
//					else{
//						prefManager.setPrefItemBool("is_subscribed", false);
//					}
//				}
//
//			}catch (RemoteException e){
//				Log.d(TAG, " " + e.getLocalizedMessage());
//			}
		}

		@Override
		public void onServiceDisconnected(ComponentName name) {
			mService = null;
		}
	};


	@Override
	protected void onCreate(Bundle savedInstanceState) {
		//Меняем тему, используемую при запуске приложения, на основную
		setTheme(R.style.AppTheme);
		super.onCreate(savedInstanceState);
		this.requestWindowFeature(Window.FEATURE_NO_TITLE);
		setContentView(R.layout.activity_main_new);



//		toolbar = findViewById(R.id.toolbar);
//		setSupportActionBar(toolbar);
//		getSupportActionBar().setDisplayShowTitleEnabled(false);

//		_skuList.add("subscribe");
//		mQuerySkus.putStringArrayList("ITEM_ID_LIST", _skuList);


		prefManager = new PrefManager(getApplicationContext());

		Intent serviceIntent = new Intent("com.android.vending.billing.InAppBillingService.BIND");
		serviceIntent.setPackage("com.android.vending");
		bindService(serviceIntent, mServiceConn, Context.BIND_AUTO_CREATE);

//		if (prefManager.isFirstTimeLaunch()) {
//			startActivity(new Intent(MainActivity.this, WelcomeActivity.class));
//			finish();
//		}

		final DrawerLayout drawer = findViewById(R.id.drawer_layout);
		ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
				this, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close) {
			@Override
			public void onDrawerStateChanged(int newState) {
				if (newState == DrawerLayout.STATE_SETTLING) {
					if (!drawer.isDrawerOpen(GravityCompat.START)) {
						String optimizeStatus = prefManager.getPrefItem(OPTIMIZE_STATUS, OPTIMIZE_DISABLED); //получаем настройки подключения
						if (optimizeStatus.equals(OPTIMIZE_ENABLED))
							switchOptimizeStatus.setChecked(true);
						else
							switchOptimizeStatus.setChecked(false);
					}
					invalidateOptionsMenu();
				}
			}
		};
		drawer.addDrawerListener(toggle);
		toggle.syncState();
		NavigationView navigationView = findViewById(R.id.nav_view);
		navigationView.setNavigationItemSelectedListener(this);
		navigationView.bringToFront();
		navigationView.requestLayout();
		try {
			//получаем настройки приложения по умолчанию
			PreferenceManager.setDefaultValues(this, R.xml.pref_general, false);
		} catch (Exception ex) {
			
		}
		Menu menu = navigationView.getMenu();


		switchOptimizeStatus = MenuItemCompat.getActionView(menu.findItem(R.id.app_bar_switch_only_wifi)).findViewById(R.id.switchWidget);
		switchOptimizeStatus.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
			@Override
			public void onCheckedChanged(CompoundButton compoundButton, boolean state) {
				if (state){
					prefManager.setPrefItem(OPTIMIZE_STATUS, OPTIMIZE_ENABLED);
					Log.d("State", String.valueOf(state));
				}
				else {
					prefManager.setPrefItem(OPTIMIZE_STATUS, OPTIMIZE_DISABLED);
					Log.d("State", String.valueOf(state));
				}
			}
		});
		//Z
		filePath = ((App) getApplicationContext()).getMusicDirectory();
		textUserID = findViewById(R.id.userID);
		textVersionName = findViewById(R.id.versionName);
		textDeviceID = findViewById(R.id.deviceID);
		textUserID = findViewById(R.id.userID);
		txtTrackCount = findViewById(R.id.txtTrackCount);
		txtTrackCountInFolder = findViewById(R.id.txtCountTrackInFolder);
		txtMemoryUsed = findViewById(R.id.txtMemoryUsed);
		txtCountPlayTracks = findViewById(R.id.txtCountPlayTracks);
		txtCountPlayTracks.setMovementMethod(new android.text.method.ScrollingMovementMethod());
		
		textInfo = findViewById(R.id.textViewInfo);
		textInfo.setMovementMethod(new android.text.method.ScrollingMovementMethod());
		
		textTrackID = findViewById(R.id.trackID);
		
		txtTrackTitle = findViewById(R.id.trackTitle);
		txtTrackArtist = findViewById(R.id.trackArtist);
		
		txtProgressLeft = findViewById(R.id.tProgressLeft);
		txtProgressRight = findViewById(R.id.tProgressRight);
		
		IntentFilter filter = new IntentFilter();
		filter.addAction(ActionProgressBarUpdate);
		filter.addAction(ActionTrackInfoUpdate);
		filter.addAction(ActionButtonImgUpdate);
		filter.addAction(ActionSendInfoTxt);
		filter.addAction(ActionProgressBarFirstTracksLoad);
		filter.addAction(ActionStopPlayback);
		filter.addAction(ActionCheckCountTracksAndDownloadIfNotEnought);
		filter.addAction(ActionShowRateRequest);
		filter.addAction(ACTION_UPDATE_FILLCACHE_PROGRESS);
		filter.addAction(ActionNotFoundTrack);
		filter.addAction(ActionAlarm);
		filter.addAction(ActionCheckListensCount);
		filter.addAction(ActionPopupClose);
		filter.addAction(ActionPopupSetForeground);
		registerReceiver(myReceiver, filter);

		IntentFilter headsetFilters = new IntentFilter();
		headsetFilters.addAction(Intent.ACTION_HEADSET_PLUG);
		headsetFilters.addAction(BluetoothAdapter.ACTION_CONNECTION_STATE_CHANGED);
		headsetFilters.addAction(BluetoothDevice.ACTION_ACL_CONNECTED);
		headsetFilters.addAction(BluetoothDevice.ACTION_ACL_DISCONNECTED);

//		this.registerReceiver(headSetReceiver, new IntentFilter(Intent.ACTION_HEADSET_PLUG));
		this.registerReceiver(headSetReceiver, headsetFilters);

		this.registerReceiver(remoteControlReceiver, new IntentFilter(Intent.ACTION_MEDIA_BUTTON));
		
		networkStateReceiver = new NetworkStateReceiver();
		networkStateReceiver.addListener(this);
		this.registerReceiver(networkStateReceiver, new IntentFilter(android.net.ConnectivityManager.CONNECTIVITY_ACTION));
		
		if (mediaPlayerServiceConnection == null)
			InitilizeMedia();

//		trackDB = TrackDB.getInstance(getApplicationContext());
//		trackDB = new TrackDB(MainActivity.this, 1);
		sp = PreferenceManager.getDefaultSharedPreferences(this);
		try {
			int currentVersion = getApplicationContext().getPackageManager().getPackageInfo(getApplicationContext().getPackageName(), PackageManager.GET_META_DATA).versionCode;
			//Если приложение было обновлено - обнуляем счетчик положительных прослушиваний для версии приложения
			if (sp.getInt("lastVersion", -1) < currentVersion) {
				prefManager.setPrefItemInt("lastVersion", currentVersion);
				prefManager.setPrefItemInt("listenTracksCountInLastVersion", 0);
				prefManager.setPrefItemBool("isRateRequestAlreadyShown", false);
			}
			
			if (sp.getInt("lastVersion", -1) < 34) {
				sp.edit().putInt("lastVersion", currentVersion).commit();
				FileUtils.cleanDirectory(this.getExternalFilesDir(Environment.DIRECTORY_MUSIC));
				this.deleteDatabase("ownradiodb.db3");
			}
		} catch (Exception ex) {
			new Utilites().SendInformationTxt(getApplicationContext(), "Error by get app version " + ex.getLocalizedMessage());
		}

//         полная очистка настроек
//         sp.edit().clear().commit();
		layoutDevelopersInfo = findViewById(R.id.devInfo);//linearLayoutDevelopersInfo

		final SharedPreferences.Editor editor = sp.edit();
		btnPlayPause = findViewById(R.id.btnPlayPause);
		btnPlayPause.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View view) {
				if (binder.GetMediaPlayerService().player != null && binder.GetMediaPlayerService().GetMediaPlayerState() == PlaybackStateCompat.STATE_PLAYING) {
					binder.GetMediaPlayerService().Pause();

				} else {
					binder.GetMediaPlayerService().Play();
					MediaPlayerService.playbackWithHSisInterrupted = false;
				}
				textTrackID.setText("Track ID: " + binder.GetMediaPlayerService().TrackID);
				
				//todo вернуть условия проверки
//				if (prefManager.getPrefItemInt("listenTracksCountInLastVersion", 0) >= mCountListenForRateDialog
//						&& !prefManager.getPrefItemBool("isRateRequestAlreadyShown", false)
//						&& prefManager.getPrefItemBool("isAllowShowRateRequestAgain", true)
//						) {
//					if (!isRateInflated) {
//						viewStubRate.setLayoutResource(R.layout.rate_request_cardview);
//						viewStubRate.inflate();
//						isRateInflated = true;
//					} else {
//						//todo отладить и согласовать макет
//						// rateRequestLayout.setVisibility(View.VISIBLE);
//					}
//				}
			}
		});
		
		btnNext = findViewById(R.id.btnNext);
		btnNext.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View view) {
				if (SystemClock.elapsedRealtime() - lastClickTime < 1000) {
					Log.e(TAG, "Next return");
					return;
				}
				
				lastClickTime = SystemClock.elapsedRealtime();
				if (binder.GetMediaPlayerService().player != null && binder.GetMediaPlayerService().GetMediaPlayerState() != PlaybackStateCompat.STATE_SKIPPING_TO_NEXT && binder.GetMediaPlayerService().GetMediaPlayerState() != PlaybackStateCompat.STATE_BUFFERING) {
					binder.GetMediaPlayerService().Next();
					textTrackID.setText("Track ID: " + binder.GetMediaPlayerService().TrackID);
					Log.e(TAG, "Next");
				}
				SubscribeManager.CheckSubscribeStatus(getApplicationContext(), mService);
			}
		});
		
		btnSkipTrack = findViewById(R.id.btnSkipTrack);
		btnSkipTrack.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View view) {
				if (binder.GetMediaPlayerService().player != null)
					binder.GetMediaPlayerService().onCompletion(binder.GetMediaPlayerService().player);
			}
		});

		buttonShare = findViewById(R.id.buttonShareTrack);
		buttonShare.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				onCopyLink(v);
			}
		});

		Button btnTest = findViewById(R.id.btnTest);
		btnTest.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View view) {
				new Utilites().SendLogs(getApplicationContext(), DeviceId);
			}
		});
		
		viewStubRate = findViewById(R.id.viewStubRate);
		viewStubRate.setOnInflateListener(new ViewStub.OnInflateListener() {
			@Override
			public void onInflate(ViewStub stub, View inflated) {
				rateRequestMessage = findViewById(R.id.rateRequestMessage);//feedback_request_message_1
				rateRequestMessage.setMovementMethod(LinkMovementMethod.getInstance());
				rateRequestLayout = findViewById(R.id.rateRequestLayout);//(LinearLayout) findViewById(R.id.rateRequestLayout);
				
				btnRateCancel = findViewById(R.id.btnRateCancel);
				btnRateCancel.setOnClickListener(new View.OnClickListener() {
					@Override
					public void onClick(View v) {
						rateRequestLayout.setVisibility(View.GONE);
						prefManager.setPrefItemBool("isRateRequestAlreadyShown", true);
					}
				});
			}
		});

		//Кнопка оценки приложения(активна, когда прослушиваний больше 100)
		btnRateApp = findViewById(R.id.rateAppBtn);
		btnRateApp.setOnClickListener(new View.OnClickListener() {

			@Override
			public void onClick(View v) {
				ConstraintLayout layout = (ConstraintLayout) findViewById(R.id.main_layout);
				if(android.os.Build.VERSION.SDK_INT >= 23) {
					layout.setForeground(new ColorDrawable(ContextCompat.getColor(getApplicationContext(), R.color.rate_popup_color)));
				}
				startActivity(new Intent(MainActivity.this,RatePopup.class));



				btnRateApp.setEnabled(false);
				btnRateApp.setVisibility(View.GONE);
			}
		});
		btnRateApp.setEnabled(false);
		btnRateApp.setVisibility(View.GONE);
		
		txtFillCacheProgress = findViewById(R.id.fill_cache_progress);
		
		btnTimer = findViewById(R.id.btnTimer);
		
		btnTimer.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View view) {
				Intent timerSleepActivity = new Intent(getBaseContext(), TimerSleep.class);
				startActivityForResult(timerSleepActivity, 0);
			}
			
		});
		
		btnAlarmClock = findViewById(R.id.btnAlarmClock);
		
		btnAlarmClock.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View view) {
				Intent alarmClockActivity = new Intent(getBaseContext(), AlarmClock.class);
				startActivity(alarmClockActivity);
			}
		});
		
		btnMenu = findViewById(R.id.btnMenu);
		btnMenu.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View view) {
				Intent settingsActivity = new Intent(getBaseContext(), SettingsActivity.class);
				settingsActivity.putExtra(PreferenceActivity.EXTRA_SHOW_FRAGMENT, SettingsActivity.GeneralPreferenceFragment.class.getName());
				settingsActivity.putExtra(PreferenceActivity.EXTRA_NO_HEADERS, true);
				startActivity(settingsActivity);
			}
		});

		String[] fileName = filePath.list();
		for (String file: fileName){
			Log.d("File", file);
		}

		
	}
	
	
	@Override
	public void onActivityResult(int requestCode, int resultCode, Intent data) {
		if (resultCode == RESULT_OK) {
			finish();
		} else {
		
		}
	}
	
	@Override
	public void onBackPressed() {
		DrawerLayout drawer = findViewById(R.id.drawer_layout);
		if (drawer.isDrawerOpen(GravityCompat.START)) {
			drawer.closeDrawer(GravityCompat.START);
		} else {
//			super.onBackPressed();
			Intent startMain = new Intent(Intent.ACTION_MAIN);
			startMain.addCategory(Intent.CATEGORY_HOME);
			startMain.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
			startActivity(startMain);
		}
	}

//
//	@Override
//	public boolean onCreateOptionsMenu(Menu menu) {
//		// Inflate the menu; this adds items to the action bar if it is present.
//		getMenuInflater().inflate(R.menu.main, menu);
//		return true;
//	}
	
	//	@Override
//	public boolean onOptionsItemSelected(MenuItem item) {
//		// Handle action bar item clicks here. The action bar will
//		// automatically handle clicks on the Home/Up button, so long
//		// as you specify a parent activity in AndroidManifest.xml.
//		int id = item.getItemId();
//
//		//noinspection SimplifiableIfStatement
////		if (id == R.id.action_settings) {
////			Intent settingsActivity = new Intent(getBaseContext(),
////					SettingsActivity.class);
////			startActivity(settingsActivity);
////			return true;
////		}
//
//		return super.onOptionsItemSelected(item);
//	}
	
	@SuppressWarnings("StatementWithEmptyBody")
	@Override
	public boolean onNavigationItemSelected(MenuItem item) {
		// Handle navigation view item clicks here.
		int id = item.getItemId();
//		PrefManager prefManager = new PrefManager(getApplicationContext());
		DrawerLayout drawer = findViewById(R.id.drawer_layout);
		
		switch (id) {
			case R.id.app_bar_switch_only_wifi:
				if (switchOptimizeStatus.isChecked())
					switchOptimizeStatus.setChecked(false);
				else
					switchOptimizeStatus.setChecked(true);
				break;
			case R.id.app_bar_write_to_developers:
				startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("https://vk.me/ownradio")));
				break;
			case R.id.app_bar_rate_app:
				final String appPackageName = getPackageName(); // getPackageName() from Context or Activity object
//				try {
//					startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=" + appPackageName)));
//				} catch (android.content.ActivityNotFoundException anfe) {
//					startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/apps/details?id=" + appPackageName)));
//				}
				AlertDialog.Builder rateRequest = new AlertDialog.Builder(MainActivity.this);
				rateRequest.setTitle("Оцените приложение")
						.setMessage("Вы можете оставить комментарий или пожелание в нашей группе ВКонтакте. Если Вам понравилось приложение, вы можете поддержать нас оставив положительный отзыв в Google Play.")
						.setCancelable(true)
						.setPositiveButton(R.string.btn_vk_rate, new DialogInterface.OnClickListener() {
							@Override
							public void onClick(DialogInterface dialog, int which) {
								startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("https://vk.com/ownradio")));
							}
						})
						.setNegativeButton(R.string.btn_google_play_rate,
								new DialogInterface.OnClickListener() {
									@Override
									public void onClick(DialogInterface dialog, int which) {
										try {
											startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=" + appPackageName)));
										} catch (android.content.ActivityNotFoundException anfe) {
											startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/apps/details?id=" + appPackageName)));
										}
										dialog.dismiss();
									}
								})
						.setNeutralButton(R.string.button_cancel,
								new DialogInterface.OnClickListener() {
									@Override
									public void onClick(DialogInterface dialog, int i) {
										dialog.dismiss();
									}
								});
				AlertDialog alert = rateRequest.create();
				alert.show();
				break;
			case R.id.app_bar_settings:
				Intent settingsActivity = new Intent(getBaseContext(),
						SettingsActivity.class);
				settingsActivity.putExtra(PreferenceActivity.EXTRA_SHOW_FRAGMENT, SettingsActivity.GeneralPreferenceFragment.class.getName());
				settingsActivity.putExtra(PreferenceActivity.EXTRA_NO_HEADERS, true);
				startActivity(settingsActivity);
				drawer.closeDrawer(GravityCompat.START);
				break;
			case R.id.app_bar_subscribe:
				Intent iabillingActivity = new Intent(getBaseContext(), IABillingActivity.class);
				startActivity(iabillingActivity);
				break;
			case R.id.app_bar_switch_sleeping_mode:
				Intent timerSleepActivity = new Intent(getBaseContext(), TimerSleep.class);
				timerSleepActivity.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
				startActivity(timerSleepActivity);
				break;
			case R.id.app_bar_switch_alarm_clock:
				Intent alarmClockActivity = new Intent(getBaseContext(), AlarmClock.class);
				startActivity(alarmClockActivity);
				break;
		}
		return true;
	}
	
	
	@Override
	public boolean onTouch(View v, MotionEvent event) {
		switch (event.getAction()) {
			case MotionEvent.ACTION_DOWN: // нажатие
				touchDownMs = System.currentTimeMillis();
				break;
			case MotionEvent.ACTION_MOVE: // движение
				break;
			case MotionEvent.ACTION_UP: // отпускание
				handlerEvent.removeCallbacksAndMessages(null);
				if ((System.currentTimeMillis() - touchDownMs) > ViewConfiguration.getTapTimeout()) {
					//it was not a tap
					numberOfTaps = 0;
					lastTapTimeMs = 0;
					break;
				}
				
				if (numberOfTaps > 0
						&& (System.currentTimeMillis() - lastTapTimeMs) < 2 * ViewConfiguration.getDoubleTapTimeout()) {
					numberOfTaps += 1;
				} else {
					numberOfTaps = 1;
				}
				
				lastTapTimeMs = System.currentTimeMillis();
				
				//3 тапа - отобразить/скрыть инфрмацию для разработчиков
				if (numberOfTaps == 3) {
					flagDevInfo = !flagDevInfo;
					SetDevelopersInfo();
				}
				break;
			case MotionEvent.ACTION_CANCEL:
				break;
		}
		return true;
	}
	
	public void SetDevelopersInfo() {
		if (!flagDevInfo)
			layoutDevelopersInfo.setVisibility(View.GONE);
		else
			layoutDevelopersInfo.setVisibility(View.VISIBLE);
	}
	
	private float curVolume = 0.0f;
	private BroadcastReceiver myReceiver = new BroadcastReceiver() {
		@Override
		public void onReceive(Context context, final Intent intent) {
			if (intent.getAction() == ActionCheckCountTracksAndDownloadIfNotEnought) {
				AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
				builder.setTitle("Ошибка")
						.setMessage("Невозможно кешировать треки. Проверьте интернет подключение.")
						.setCancelable(false)
						.setNegativeButton("OK",
								new DialogInterface.OnClickListener() {
									@Override
									public void onClick(DialogInterface dialogInterface, int i) {
										dialogInterface.cancel();
									}
								});
				AlertDialog alert = builder.create();
				alert.show();
			}
			
			if (intent.getAction() == ActionStopPlayback) {
				binder.GetMediaPlayerService().Stop();
				binder.GetMediaPlayerService().StopNotification();
			}
			
			if (intent.getAction() == ActionTrackInfoUpdate)
				SetTrackInfoText();
			
			if (intent.getAction() == ActionButtonImgUpdate) {
				String title;
				String artist;
				try {
					if (binder == null || binder.GetMediaPlayerService().player == null) {
						txtTrackTitle.setText("");
						txtTrackArtist.setText("");
						btnPlayPause.setImageResource(R.drawable.btn_ic_play);
						return;
					}
					
					if (binder.GetMediaPlayerService().GetMediaPlayerState() == PlaybackStateCompat.STATE_STOPPED) {
						btnPlayPause.setImageResource(R.drawable.btn_ic_play);
						return;
					}
					
					if (binder.GetMediaPlayerService().player != null && binder.GetMediaPlayerService().GetMediaPlayerState() == PlaybackStateCompat.STATE_PLAYING) {
						btnPlayPause.setImageResource(R.drawable.btn_ic_pause);
					} else {
						btnPlayPause.setImageResource(R.drawable.btn_ic_play);
					}
					
					if (binder.GetMediaPlayerService().player != null) {
						title = binder.GetMediaPlayerService().track.getAsString("title");
						artist = binder.GetMediaPlayerService().track.getAsString("artist");
						if (title == null || title.isEmpty() || title.equals("null"))
							title = "Track";
						if (artist == null || artist.isEmpty() || artist.equals("null"))
							artist = "Artist";
						txtTrackTitle.setText(title);
						txtTrackArtist.setText(artist);
						textTrackID = findViewById(R.id.trackID);
						textTrackID.setText("Track ID: " + binder.GetMediaPlayerService().TrackID);
					}
				} catch (Exception ex) {
					new Utilites().SendInformationTxt(MainActivity.this, "ActionButtonImgUpdate error:" + ex.getLocalizedMessage());
					title = "Track";
					artist = "Artist";
					txtTrackTitle.setText(title);
					txtTrackArtist.setText(artist);
				}
			}
			
			if (intent.getAction() == ActionSendInfoTxt) {
				if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
					textInfo.append(Html.fromHtml("<p><b>Info:</b> " + intent.getStringExtra("TEXTINFO") + "<br/></p>", Html.FROM_HTML_MODE_LEGACY));
				} else {
					textInfo.append(Html.fromHtml("<p><b>Info:</b> " + intent.getStringExtra("TEXTINFO") + "<br/></p>"));
				}
			}
			
			if (intent.getAction() == ActionProgressBarUpdate) {
				curVolume = 0.1f;
				if (progressBar == null)
					progressBar = findViewById(R.id.progressBar);
				new Thread(new Runnable() {
					@Override
					public void run() {
						if (binder.GetMediaPlayerService().player == null) {
							progressBar.setMax(10000);
							progressBar.setProgress(0);
							return;
						}
						int duration = 0;
						try {
							duration = binder.GetMediaPlayerService().GetDuration();
//							duration = binder.GetMediaPlayerService().track.getAsInteger("length");
						} catch (Exception ex) {
							duration = binder.GetMediaPlayerService().track.getAsInteger("length") * 1000;

//							duration = binder.GetMediaPlayerService().GetDuration() / 1000;
						}
						int currentPosition = 0;
						if (duration > 0)
							progressBar.setMax(duration);
						progressBar.setSecondaryProgress(0);
						
						while (currentPosition < duration) {
//							if(currentPosition <= 5000)
//								binder.GetMediaPlayerService().FadeIn(.2f);
//							else if (currentPosition >= duration - 5000)
//								binder.GetMediaPlayerService().FadeOut(.2f);
//							else
//								binder.GetMediaPlayerService().pla
							try {
								Thread.sleep(1000);
								currentPosition = binder.GetMediaPlayerService().GetPosition();
								binder.GetMediaPlayerService().SaveLastPosition();
//								currentPosition = binder.GetMediaPlayerService().GetPosition() / 1000;
//								try {
//									duration = binder.GetMediaPlayerService().GetDuration();
////									duration = binder.GetMediaPlayerService().track.getAsInteger("length");
//								} catch (Exception ex) {
//									duration = binder.GetMediaPlayerService().track.getAsInteger("length") * 1000;
////									duration = binder.GetMediaPlayerService().GetDuration() / 1000;
//								}
							} catch (InterruptedException e) {
								e.printStackTrace();
							} catch (Exception e) {
								e.getLocalizedMessage();
							}
							
							// Update the progress bar
							handler.post(new Runnable() {
								@Override
								public void run() {
									int duration;
									try {
										duration = binder.GetMediaPlayerService().GetDuration();
//										duration = binder.GetMediaPlayerService().track.getAsInteger("length");
									} catch (Exception ex) {
										duration = binder.GetMediaPlayerService().track.getAsInteger("length") * 1000;
//										duration = binder.GetMediaPlayerService().GetDuration() / 1000;
									}
									progressBar.setMax(duration);
									progressBar.setProgress(binder.GetMediaPlayerService().GetPosition());
									//todo:
									int curSeconds = binder.GetMediaPlayerService().GetPosition() / 1000;
									
									if (curSeconds < 10 && curVolume <= 1.0f) {
										curVolume += 0.1f;
										binder.GetMediaPlayerService().SetVolume(curVolume);
										Log.d(TAG, "curVolume = " + curVolume + " seconds = " + curSeconds);
									}
									
//									int curHours = curSeconds / 3600;
//									curSeconds -= curHours * 3600;
//									int curMin = curSeconds / 60;
//									curSeconds -= curMin * 60;
									//String curTime = (curHours != 0 ? (curHours + ":") : "") + (curMin != 0 ? (curMin + ":") : "") + (curSeconds != 0 ? (curSeconds) : "");

                                    SimpleDateFormat df = new SimpleDateFormat("m:ss");
                                    String curTime = df.format(curSeconds * 1000);

									int nextSeconds = (duration - binder.GetMediaPlayerService().GetPosition()) / 1000;
									
									if (nextSeconds < 10 && curVolume >= 0f) {
										curVolume -= 0.1f;
										binder.GetMediaPlayerService().SetVolume(curVolume);
										Log.d(TAG, "curVolume = " + curVolume + " seconds = " + nextSeconds);
									}
									
//									int nextHours = nextSeconds / 3600;
//									nextSeconds -= nextHours * 3600;
//									int nextMin = nextSeconds / 60;
//									nextSeconds -= nextMin * 60;
									//String nextTime = "-" + (nextHours != 0 ? (nextHours + ":") : "") + (nextMin != 0 ? (nextMin + ":") : "") + (nextSeconds != 0 ? (nextSeconds) : "");

                                    String nextTime = df.format(nextSeconds * 1000);

									txtProgressLeft.setText(curTime);
									txtProgressRight.setText("-" + nextTime);
								}
							});
						}
					}
				}).start();
			}
			
			if (intent.getAction() == ActionProgressBarFirstTracksLoad) {
				if (intent.getBooleanExtra("ProgressOn", false)) {
					btnPlayPause.setClickable(false);
					btnNext.setClickable(false);
					btnNext.setImageResource(R.drawable.btn_ic_grey_next);
//					btnNext.setColorFilter(ContextCompat.getColor(getApplicationContext(), R.color.ColorPrimaryBtnDisable));
					if (dialog == null)
						dialog = new ProgressDialog(MainActivity.this);
					dialog.setTitle(getResources().getString(R.string.is_caching));
					dialog.setMessage(getResources().getString(R.string.wait_caching));
					dialog.setIndeterminate(true);
					new Thread(new Runnable() {
						@Override
						public void run() {
							while (new TrackDataAccess(getApplicationContext()).GetExistTracksCount() < 1) {
								// Обновляем счетчик количества попыток
								handler.post(new Runnable() {
									@Override
									public void run() {
										dialog.setMessage(getResources().getString(R.string.wait_caching) + " \n" + ((App) getApplicationContext()).getCountDownloadTrying() + 1 + " попытка загрузки");
									}
								});
							}
						}
					}).start();
					dialog.setCancelable(true);
					dialog.show();
				} else {
					if (dialog != null)
						dialog.dismiss();
					btnPlayPause.setClickable(true);
//					if(binder.GetMediaPlayerService().isAutoplay)
//						binder.GetMediaPlayerService().Play();
					if (new TrackDataAccess(getApplicationContext()).GetExistTracksCount() < 1) {
						btnNext.setClickable(false);
						btnNext.setImageResource(R.drawable.btn_ic_grey_next);
//						btnNext.setColorFilter(ContextCompat.getColor(getApplicationContext(), R.color.ColorPrimaryBtnDisable));
					} else {
						btnNext.setClickable(true);
						btnNext.setImageResource(R.drawable.btn_ic_next);
//						btnNext.setColorFilter(ContextCompat.getColor(getApplicationContext(), R.color.ColorTransparent));
					}
				}
			}
			
			if (intent.getAction().equals(ACTION_UPDATE_FILLCACHE_PROGRESS)) {
				int count = intent.getExtras().getInt(EXTRA_FILLCACHE_PROGRESS, 0);
				if (count == 0)
					txtFillCacheProgress.setVisibility(View.GONE);
			}
			
			if (intent.getAction().equals(ActionNotFoundTrack)) {
				AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
				builder.setTitle("Ошибка загрузки")
						.setMessage("Данный трек отсутствует")
						.setCancelable(false)
						.setNegativeButton(getResources().getString(R.string.button_ok),
								new DialogInterface.OnClickListener() {
									@Override
									public void onClick(DialogInterface dialogInterface, int i) {
										dialogInterface.cancel();
									}
								});
				AlertDialog alert = builder.create();
				alert.show();
			}
			
			if (intent.getAction().equals(ActionAlarm)) {
				//startPlayAlarm();
			}

			if (intent.getAction() == ActionCheckListensCount) {
				int listensCount = sp.getInt("rateListensCount", 0);
				Boolean isRated = sp.getBoolean("appIsRated", false);
				if (listensCount >= 100 && !isRated) {
					btnRateApp.setEnabled(true);
					btnRateApp.setVisibility(View.VISIBLE);
				}
			}
			if (intent.getAction() == ActionPopupClose){
				ConstraintLayout layout = (ConstraintLayout) findViewById(R.id.main_layout);
				if(android.os.Build.VERSION.SDK_INT >= 23) {
					layout.setForeground(new ColorDrawable(ContextCompat.getColor(getApplicationContext(), R.color.transparent)));
				}
			}
			if (intent.getAction() == ActionPopupSetForeground){
                ConstraintLayout layout = (ConstraintLayout) findViewById(R.id.main_layout);
                if(android.os.Build.VERSION.SDK_INT >= 23) {
                    layout.setForeground(new ColorDrawable(ContextCompat.getColor(getApplicationContext(), R.color.rate_popup_color)));
                }
            }
		}
	};
	
	private void startPlayAlarm() {
		if (binder != null) {
			SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(MainActivity.this);
			
			String trackId = prefs.getString(CURRENT_TRACK_ID, "");
			String trackUrl = prefs.getString(CURRENT_TRACK_URL, "");
			
			binder.GetMediaPlayerService().PlayAlarmTrack(trackId, trackUrl);
		}
	}
	
	private void InitilizeMedia() {
		mediaPlayerServiceIntent = new Intent(this, MediaPlayerService.class);
		mediaPlayerServiceConnection = new MediaPlayerServiceConnection(this);
		bindService(mediaPlayerServiceIntent, mediaPlayerServiceConnection, BIND_AUTO_CREATE);
	}
	
	@Override
	public void onStart() {
		super.onStart();
		TrackToCache trackToCache = new TrackToCache(MainActivity.this);
		
		if (mediaPlayerServiceConnection == null)
			InitilizeMedia();

		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
		if (prefs.getBoolean(IS_ALARM_WORK, false)) {
			btnAlarmClock.setImageResource(R.drawable.ic_bud_b);
		}
		else{
			btnAlarmClock.setImageResource(R.drawable.ic_bud);
		}
		if(prefs.getBoolean(IS_TIMER_WORK, false)){
			btnTimer.setImageResource(R.drawable.ic_set_tim);
		}
		else{
			btnTimer.setImageResource(R.drawable.ic_tim);
		}

		txtTrackTitle.setOnTouchListener(this);
		txtTrackArtist.setOnTouchListener(this);
//		toolbar.setOnTouchListener(this);
		
		
		try {
			//запускаем загрузку треков, если кеш пуст
			if (new TrackDataAccess(getApplicationContext()).GetExistTracksCount() < 1) {
				if (trackToCache.FreeSpace() + TrackToCache.FolderSize(filePath) < 104857600) {
					AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
					builder.setTitle(getResources().getString(R.string.not_enough_free_space))
							.setMessage(getResources().getString(R.string.requires_free_space))
							.setCancelable(false)
							.setNegativeButton(getResources().getString(R.string.button_ok),
									new DialogInterface.OnClickListener() {
										@Override
										public void onClick(DialogInterface dialogInterface, int i) {
											dialogInterface.cancel();
										}
									});
					AlertDialog alert = builder.create();
					alert.show();
					
					btnPlayPause.setClickable(false);
					btnNext.setClickable(false);
					btnNext.setColorFilter(ContextCompat.getColor(getApplicationContext(), R.color.ColorPrimaryBtnDisable));
				}
			}
			
			SetDevelopersInfo();
			
			try {
				String info = getApplicationContext().getPackageManager().getPackageInfo(getApplicationContext().getPackageName(), PackageManager.GET_META_DATA).versionName;
				if (!info.equals(sp.getString("version", ""))) {
					sp.edit().putString("version", info).commit();
					sp.edit().putInt("rateListensCount", 0).commit();
					sp.edit().putBoolean("appIsRated", false).commit();

				}
				textVersionName.setText("Version name: " + info);
			} catch (PackageManager.NameNotFoundException e) {
				e.printStackTrace();
			}
			
			apiCalls = new APICalls(MainActivity.this);
			rdevApiCalls = new RdevApiCalls(MainActivity.this);
			try {
				DeviceId = sp.getString("DeviceID", "");
//				final Map<String, String> authMap = rdevApiCalls.GetAuthToken();
//				String token = authMap.get("token");
				Intent downloaderIntent = new Intent(this, LongRequestAPIService.class);
				if (DeviceId.isEmpty()) {
					DeviceId = UUID.randomUUID().toString();
					String UserName = "NewUser";
					String DeviceName = Build.BRAND + " " + Build.PRODUCT;
					//new APICalls(getApplicationContext()).RegisterDevice(DeviceId, DeviceName + " " + getApplicationContext().getPackageManager().getPackageInfo(getApplicationContext().getPackageName(), PackageManager.GET_META_DATA).versionName);


					new RdevApiCalls(getApplicationContext()).RegisterDevice(DeviceId, DeviceName + " " + getApplicationContext().getPackageManager().getPackageInfo(getApplicationContext().getPackageName(), PackageManager.GET_META_DATA).versionName);
					//UserId = apiCalls.GetUserId(DeviceId);


					UserId = rdevApiCalls.GetDeviceInfo(DeviceId).get("OK").getResult().get("userid");
					sp.edit().putString("DeviceID", DeviceId).commit();
					sp.edit().putString("UserID", UserId);
					sp.edit().putString("UserName", UserName);
					sp.edit().putString("DeviceName", DeviceName);
					sp.edit().commit();

					downloaderIntent.putExtra(EXTRA_USERID, UserId);
				} else {
					UserId = sp.getString("UserID", "");
					if (UserId.isEmpty()) {
						UserId = rdevApiCalls.GetDeviceInfo(DeviceId).get("OK").getResult().get("userid");
						sp.edit().putString("UserID", UserId).commit();

						downloaderIntent.putExtra(EXTRA_USERID, UserId);
					}
				}
			} catch (Exception ex) {
				ex.printStackTrace();
				ex.getLocalizedMessage();
			}
			textDeviceID.setText("Device ID: " + DeviceId);
			textUserID.setText("User ID: " + UserId);
			
			trackToCache.ScanTrackToCache();
			
			SetTrackInfoText();
			
			
			new Utilites().CheckCountTracksAndDownloadIfNotEnought(MainActivity.this, DeviceId);
			
			if (((App) getApplicationContext()).getFillingCacheActive()) {
				final Handler h = new Handler();
				txtFillCacheProgress.setVisibility(View.VISIBLE);
				txtFillCacheProgress.setText("Кешировано треков: " + ((App) getApplicationContext()).getCountDownloadTrying());
				new Thread(new Runnable() {
					@Override
					public void run() {
						while (txtFillCacheProgress.getVisibility() == View.VISIBLE) {
							try {
								Thread.sleep(5000);
							} catch (Exception ex) {
								
							}
							h.post(new Runnable() {
								@Override
								public void run() {
									txtFillCacheProgress.setText("Кешировано треков: " + ((App) getApplicationContext()).getCountDownloadTrying());
									
								}
							});
							
						}
					}
				}).start();
			} else {
				txtFillCacheProgress.setVisibility(View.GONE);
			}
			
			if (new TrackDataAccess(getApplicationContext()).GetExistTracksCount() < 1) {
				txtTrackTitle.setText("");
				txtTrackArtist.setText("");
			}

			return;

		} catch (Exception ex) {
			Log.d(TAG, " " + ex.getLocalizedMessage());
			return;
		}
	}
	
	@Override
	protected void onNewIntent(Intent intent) {
		if (intent != null)
			setIntent(intent);
	}
	
	public String TrackId;
	public String TrackTitle;
	public String TrackArtist;
	
	@Override
	public void onResume() {
		
		Intent intent = getIntent();
		
		Uri uri = intent.getData();
		if (uri != null) {
			try {
				TrackId = uri.getQueryParameter("trackId");
				TrackTitle = uri.getQueryParameter("title").replace('_', ' ');
				TrackArtist = uri.getQueryParameter("artist").replace('_', ' ');
				if (binder != null) {
					if (binder.GetMediaPlayerService().player != null) {
						binder.GetMediaPlayerService().Pause();
					}/*else {
						binder.GetMediaPlayerService().Play();
						MediaPlayerService.playbackWithHSisInterrupted = false;
					}*/
					
					
					if (dialog == null) {
						dialog = new ProgressDialog(MainActivity.this);
					}
					dialog.setTitle(R.string.download_title);
					dialog.setMessage(getResources().getString(R.string.download_message));
					dialog.setIndeterminate(true);
					dialog.setCancelable(true);
					dialog.show();
					
					PlayThread thread = new PlayThread();
					thread.start();
				}
			} catch (Exception ex) {
				Toast.makeText(this, ex.getMessage(), Toast.LENGTH_SHORT).show();
			}
		}
		
		intent.setData(null); // clear data
		super.onResume();
		networkStateReceiver.addListener(this);
		this.registerReceiver(networkStateReceiver, new IntentFilter(android.net.ConnectivityManager.CONNECTIVITY_ACTION));
		
		
		String action = intent.getAction();
		if (action != null && action.equals(ActionAlarm)) {
			intent.setAction(null);
			SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(MainActivity.this);
			
			if (prefs.getBoolean(IS_TIME_ALARM, false)) {
				startPlayAlarm();
				Log.d(TAG, "STARTBRAZIL");
			}
		}
		
	}
	
	
	volatile boolean isEnd = false;
	
	public void onCopyLink(View view) {
		ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
		String trackId = binder.GetMediaPlayerService().TrackID;
		
		if (trackId != "") {
			Intent intent = new Intent(Intent.ACTION_SEND);
			intent.setType("text/plain");
			intent.putExtra(Intent.EXTRA_SUBJECT, "OwnRadio");
			String title = txtTrackTitle.getText().toString().replace(' ', '_');
			String artist = txtTrackArtist.getText().toString().replace(' ', '_');
			intent.putExtra(Intent.EXTRA_TEXT, "http://ownradio.ru/?trackId=" + trackId + "&title=" + title + "&artist=" + artist);
			startActivity(Intent.createChooser(intent, "Подделиться"));
			/*ClipData clip = ClipData.newPlainText("Link", "ownradio.ru/?trackId=" + trackId);
			clipboard.setPrimaryClip(clip);
			Toast.makeText(this, "Ссылка на трек успешно скопирована", Toast.LENGTH_SHORT).show();*/
		}
	}
	
	@Override
	public void onPause() {
		networkStateReceiver.removeListener(this);
		this.unregisterReceiver(networkStateReceiver);
		super.onPause();
	}
	
	public void SetTrackInfoText() {
		TrackDataAccess trackDataAccess = new TrackDataAccess(this);
		TrackToCache trackToCache = new TrackToCache(this);
		txtTrackCount.setText("Track count: " + trackDataAccess.GetExistTracksCount() + ".");
		txtTrackCountInFolder.setText("Count files in folder: " + trackToCache.TrackCountInFolder(filePath));
		txtCountPlayTracks.setText(trackDataAccess.GetCountPlayTracksTable());
		txtMemoryUsed.setText("Cache size: " + TrackToCache.FolderSize(filePath) / 1048576 + " MB.");
	}
	
	@Override
	public void onSaveInstanceState(Bundle savedInstanceState) {
		super.onSaveInstanceState(savedInstanceState);
	}
	
	@Override
	public void onDestroy() {
		try {
			unregisterReceiver(myReceiver);
		} catch (Exception ex) {
			Log.e(TAG, " " + ex.getLocalizedMessage());
		}
		try {
			unregisterReceiver(headSetReceiver);
		} catch (Exception ex) {
			Log.e(TAG, " " + ex.getLocalizedMessage());
		}
		try {
			unregisterReceiver(remoteControlReceiver);
		} catch (Exception ex) {
			Log.e(TAG, " " + ex.getLocalizedMessage());
		}
		binder.GetMediaPlayerService().SaveLastPosition();
		binder.GetMediaPlayerService().StopNotification();
//				unbindService(mediaPlayerServiceConnection);
		stopService(new Intent(this, MediaPlayerService.class));
//                Intent intent = new Intent(MainActivity.this, MediaPlayerService.class);
//                intent.setAction("ru.netvoxlab.ownradio.action.SAVE_CURRENT_POSITION");
//                startService(intent);
		android.os.Process.killProcess(android.os.Process.myPid());
		if(mService != null){
			unbindService(mServiceConn);
		}
		final SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
		final SharedPreferences.Editor prefEditor = prefs.edit();
		prefEditor.putBoolean(IS_TIMER_WORK, false);
		prefEditor.apply();

		super.onDestroy();
	}
	
	public void LogToTextView() {
		try {
			Process process = Runtime.getRuntime().exec("logcat -d");
			BufferedReader bufferedReader = new BufferedReader(
					new InputStreamReader(process.getInputStream()));
			
			StringBuilder log = new StringBuilder();
			String line;
			while ((line = bufferedReader.readLine()) != null) {
				if (line.contains(TAG))
					log.append(line + "\n");
			}
			TextView textInfo = findViewById(R.id.textViewInfo);
			textInfo.setMovementMethod(new android.text.method.ScrollingMovementMethod());
			textInfo.setText(log.toString());
		} catch (IOException e) {
		}
	}
	
	@Override
	public void networkAvailable() {
		new Utilites().SendInformationTxt(getApplicationContext(), "Интернет подключен");
		//Если треков в кеше мало - при подключении  интернета запускаем запуск треков
//		if(new TrackDataAccess(getApplicationContext()).GetExistTracksCount() < 3) {
//			//		Запускаем кеширование треков - 3 шт
//			Intent downloaderIntent = new Intent(getApplicationContext(), LongRequestAPIService.class);
//			downloaderIntent.setAction(ACTION_GETNEXTTRACK);
//			downloaderIntent.putExtra(EXTRA_DEVICEID, DeviceId);
//			downloaderIntent.putExtra(EXTRA_COUNT, 1);
//			startService(downloaderIntent);
//		}
		/* TODO: Your connection-oriented stuff here */
	}
	
	@Override
	public void networkUnavailable() {
		new Utilites().SendInformationTxt(getApplicationContext(), "Интернет отключен");
		/* TODO: Your disconnection-oriented stuff here */
	}
	
	class MediaPlayerServiceConnection extends java.lang.Object implements ServiceConnection {
		MainActivity instance;
		
		public MediaPlayerServiceConnection(MainActivity player) {
			this.instance = player;
		}
		
		@Override
		public void onServiceConnected(ComponentName componentName, IBinder service) {
			MediaPlayerService.MediaPlayerServiceBinder mediaPlayerServiceBinder = (MediaPlayerService.MediaPlayerServiceBinder) service;
			if (mediaPlayerServiceBinder != null) {
				new Utilites().SendInformationTxt(getApplicationContext(), "onServiceConnected");
				MediaPlayerService.MediaPlayerServiceBinder binder = (MediaPlayerService.MediaPlayerServiceBinder) service;
				instance.binder = binder;
				instance.isBound = true;
				
				SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(MainActivity.this);
				
				if (prefs.getBoolean(IS_TIME_ALARM, false)) {
					String trackId = prefs.getString(CURRENT_TRACK_ID, "");
					String trackUrl = prefs.getString(CURRENT_TRACK_URL, "");
					
					
					if (binder.GetMediaPlayerService().player != null) {
						binder.GetMediaPlayerService().Pause();
					} else {
						binder.GetMediaPlayerService().InitiMedia();
						MediaPlayerService.playbackWithHSisInterrupted = false;
					}
					binder.GetMediaPlayerService().PlayAlarmTrack(trackId, trackUrl);
					SharedPreferences.Editor editor = prefs.edit();
					editor.putBoolean(IS_TIME_ALARM, false);
					editor.apply();
					
				} else if (TrackId != null) {
					if (binder.GetMediaPlayerService().player != null) {
						binder.GetMediaPlayerService().Pause();
					} else {
						binder.GetMediaPlayerService().InitiMedia();
						MediaPlayerService.playbackWithHSisInterrupted = false;
					}
					
					
					if (dialog == null) {
						dialog = new ProgressDialog(MainActivity.this);
					}
					dialog.setTitle(R.string.download_title);
					dialog.setMessage(getResources().getString(R.string.download_message));
					dialog.setIndeterminate(true);
					dialog.setCancelable(true);
					dialog.show();
					
					PlayThread thread = new PlayThread();
					thread.start();
					
				} else {
					//предзагрузка трека, на котором остановилось воспроизведение
					Log.d(TAG, "ELSE");
					instance.binder.GetMediaPlayerService().PreloadTrack();
					if (binder.GetMediaPlayerService().track != null) {
						txtTrackTitle.setText(binder.GetMediaPlayerService().track.getAsString("title"));
						txtTrackArtist.setText(binder.GetMediaPlayerService().track.getAsString("artist"));
						if (progressBar == null)
							progressBar = findViewById(R.id.progressBar);
						int trackLength = binder.GetMediaPlayerService().track.getAsInteger("length") * 1000;
						progressBar.setMax(trackLength);

						int currentPosition = prefManager.getPrefItemInt("LastPosition", 0);

						progressBar.setProgress(currentPosition);
						SimpleDateFormat df = new SimpleDateFormat("m:ss");
						String curTime = df.format(currentPosition);
						txtProgressLeft.setText(curTime);

						int nextSeconds = (trackLength - currentPosition);
						String nextTime = df.format(nextSeconds);
						txtProgressRight.setText("-" + nextTime);
					}
				}
			}
		}
		
		@Override
		public void onServiceDisconnected(ComponentName componentName) {
			instance.isBound = false;
		}
	}
	
	private class PlayThread extends Thread {
		
		@Override
		public void run() {
			//final Map<String, String> authMap = rdevApiCalls.GetAuthToken();
			//String token = authMap.get("token");
			if (binder.GetMediaPlayerService().PlayNewTrack(TrackId, TrackTitle, TrackArtist)) {
				handler.sendEmptyMessage(0);
			} else {
				dialog.dismiss();
			}
		}
		
		private Handler handler = new Handler() {
			
			@Override
			public void handleMessage(Message msg) {
				binder.GetMediaPlayerService().Play();
				textTrackID.setText("Track ID: " + binder.GetMediaPlayerService().TrackID);
				dialog.dismiss();
			}
		};
	}
	
}
