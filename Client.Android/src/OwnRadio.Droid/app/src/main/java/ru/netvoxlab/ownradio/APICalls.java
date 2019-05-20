package ru.netvoxlab.ownradio;

import android.content.Context;

import java.util.Map;
import java.util.UUID;

/**
 * Created by a.polunina on 21.10.2016.
 */

public class APICalls {
	Context mContext;

	public APICalls(Context context) {
		this.mContext = context;
	}

	//Возращает ID пользователя по DeviceID
	public String GetUserId(String deviceID) {
		//Пока не реализована привязка пользователей
		//userid=deviceid
		return deviceID;
	}

	//Возращает ID следующего трека
	public Map<String, String> GetNextTrackID(String deviceId) {
		CheckConnection checkConnection = new CheckConnection();
		boolean internetConnect = checkConnection.CheckInetConnection(mContext);
		if (!internetConnect)
			return null;

		try {
			Map<String, String> result = new GetNextTrack(mContext).execute(deviceId).get();
			if (result == null)
				return null;
			UUID.fromString(result.get("id")).toString();
			return result;
		} catch (Exception ex) {
			new Utilites().SendInformationTxt(mContext, "Error by GetNextTrackID " + ex.getLocalizedMessage());
			return null;
		}
	}

	//Пытается отправить count записей истории прослушивания треков
	public void SendHistory(String deviceId) {
		CheckConnection checkConnection = new CheckConnection();
		if (!checkConnection.CheckInetConnection(mContext)) {
			return;
		}

		try {
			Boolean result = new HistorySend(mContext).execute(deviceId).get();
		} catch (Exception ex) {
			new Utilites().SendInformationTxt(mContext, "Error by sendHistory " + ex.getLocalizedMessage());
		}

	}

	public void SendLogs(String deviceId, String path) {
		CheckConnection checkConnection = new CheckConnection();
		if (!checkConnection.CheckInetConnection(mContext)) {
			return;
		}

		try {
			Boolean result = new SendLogFile(mContext).execute(deviceId, path).get();
		} catch (Exception ex) {
			new Utilites().SendInformationTxt(mContext, "Error by sendLogs " + ex.getLocalizedMessage());
		}
	}

	public void RegisterDevice(String deviceId, String deviceName) {
		CheckConnection checkConnection = new CheckConnection();
		if (!checkConnection.CheckInetConnection(mContext)) {
			return;
		}

		try {
			Boolean result = new RegisterDevice(mContext).execute(deviceId, deviceName).get();
		} catch (Exception ex) {
			new Utilites().SendInformationTxt(mContext, "Error by registerDevice " + ex.getLocalizedMessage());
		}

	}

	public void SetIsCorrect(String deviceId, String trackId, Integer isCorrect) {
		CheckConnection checkConnection = new CheckConnection();
		if (!checkConnection.CheckInetConnection(mContext)) {
			return;
		}
		try {
			Boolean result = new SetIsCorrect(mContext).execute(deviceId, trackId, isCorrect.toString()).get();
		} catch (Exception ex) {
			new Utilites().SendInformationTxt(mContext, "Error by setIsCorrect " + ex.getLocalizedMessage());
		}
	}
}
