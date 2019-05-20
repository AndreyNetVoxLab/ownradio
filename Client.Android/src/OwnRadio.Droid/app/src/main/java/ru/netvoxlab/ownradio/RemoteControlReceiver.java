package ru.netvoxlab.ownradio;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothProfile;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.view.KeyEvent;

import java.util.List;
import java.util.Set;

import static ru.netvoxlab.ownradio.MediaPlayerService.playbackWithHSisInterrupted;

public class RemoteControlReceiver extends BroadcastReceiver {
	static final long CLICK_DELAY = 500;
	static long lastClick = 0; // oldValue
	static long currentClick = System.currentTimeMillis();


	public RemoteControlReceiver() {
	}

	public String ComponentName() {
		return getClass().getName();
	}

	@Override
	public void onReceive(Context context, Intent intent) {
		String action = MediaPlayerService.ActionPlay;


		if (intent.getAction() != Intent.ACTION_MEDIA_BUTTON){

			return;
		}
		else {

			// Событие будет срабатывать дважды, при нажатии (down) и отпускании (up).
			// Необходимо обработать только нажатие.
			KeyEvent key = intent.getParcelableExtra(Intent.EXTRA_KEY_EVENT);
			if (key.getAction() != KeyEvent.ACTION_DOWN)
				return;

			switch (key.getKeyCode()) {
				case KeyEvent.KEYCODE_HEADSETHOOK:
					lastClick = currentClick;
					currentClick = System.currentTimeMillis();
					if (currentClick - lastClick < CLICK_DELAY) //проверяем сколько раз подряд была нажата кнопка на гарнитуре
						action = MediaPlayerService.ActionNext;
					else
						action = MediaPlayerService.ActionTogglePlayback;
					break;
				case KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE:
					action = MediaPlayerService.ActionTogglePlayback;
					break;
				case KeyEvent.KEYCODE_MEDIA_PLAY:
					action = MediaPlayerService.ActionPlay;
					break;
				case KeyEvent.KEYCODE_MEDIA_PAUSE:
					action = MediaPlayerService.ActionPause;
					break;
				case KeyEvent.KEYCODE_MEDIA_STOP:
					action = MediaPlayerService.ActionStop;
					break;
				case KeyEvent.KEYCODE_MEDIA_NEXT:
					action = MediaPlayerService.ActionNext;
					break;
				default: {
					return;
				}
			}

		}


		playbackWithHSisInterrupted = false;
		Intent remoteIntent = new Intent(context, MediaPlayerService.class);
		remoteIntent.setAction(action);
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
			context.startForegroundService(remoteIntent);
		}else{
			context.startService(remoteIntent);
		}
	}
}
