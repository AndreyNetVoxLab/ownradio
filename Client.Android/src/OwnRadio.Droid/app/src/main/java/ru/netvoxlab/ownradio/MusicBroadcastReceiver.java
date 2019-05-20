package ru.netvoxlab.ownradio;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothProfile;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.media.AudioManager;
import android.os.Build;

import java.util.Set;

import static ru.netvoxlab.ownradio.MediaPlayerService.isHSConnected;


import static ru.netvoxlab.ownradio.MediaPlayerService.playbackWithHSisInterrupted;
import static ru.netvoxlab.ownradio.MediaPlayerService.player;


public class MusicBroadcastReceiver extends BroadcastReceiver {
	public MusicBroadcastReceiver() {
	}
	@Override
	public void onReceive(Context context, Intent intent) {

		if (intent.getAction() != AudioManager.ACTION_AUDIO_BECOMING_NOISY &&
				intent.getAction() != Intent.ACTION_HEADSET_PLUG &&
				intent.getAction() != BluetoothAdapter.ACTION_CONNECTION_STATE_CHANGED){
			return;
		}


		String action = MediaPlayerService.ActionPause;

		if (intent.getAction() == Intent.ACTION_HEADSET_PLUG) {
			if (intent.getAction().equals(Intent.ACTION_HEADSET_PLUG)) {
				int state = intent.getIntExtra("state", -1);
				switch (state) {
					case 0:
						//отключение наушников
						if (isHSConnected) {
							isHSConnected = false;
							action = MediaPlayerService.ActionPause;
							if(player != null && player.isPlaying())
								playbackWithHSisInterrupted = true;
						}
						break;
					case 1:
						//подключение наушников
						isHSConnected = true;
						if(!playbackWithHSisInterrupted)
							return;
						action = MediaPlayerService.ActionPlay;
						playbackWithHSisInterrupted = false;
						break;
				}
			}
		}

		if(BluetoothAdapter.ACTION_CONNECTION_STATE_CHANGED.equals(intent.getAction())) {
        BluetoothAdapter adapter = BluetoothAdapter.getDefaultAdapter();
			switch (adapter.getProfileConnectionState(BluetoothProfile.HEADSET)) {
				case BluetoothProfile.STATE_DISCONNECTED: {
					isHSConnected = false;
					action = MediaPlayerService.ActionPause;
//					playbackWithHSisInterrupted = true;
					if (player != null && player.isPlaying())
						playbackWithHSisInterrupted = true;

					break;
				}
				case BluetoothProfile.STATE_CONNECTED: {
					isHSConnected = true;
					if (!playbackWithHSisInterrupted)
						return;
					action = MediaPlayerService.ActionPlay;
					playbackWithHSisInterrupted = false;
					break;
				}
				default:{
						return;
				}
			}
		}



//
		if (intent.getAction() == AudioManager.ACTION_AUDIO_BECOMING_NOISY) {
			//сигнал остановки сервиса
			action = MediaPlayerService.ActionPause;
			if(player.isPlaying())
				playbackWithHSisInterrupted = true;
		}


		Intent remoteIntent = new Intent(context, MediaPlayerService.class);
		remoteIntent.setAction(action);
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
			context.startForegroundService(remoteIntent);
		}
		else{
			context.startService(remoteIntent);
		}
	}
}