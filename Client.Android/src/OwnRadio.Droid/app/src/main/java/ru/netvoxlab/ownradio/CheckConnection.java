package ru.netvoxlab.ownradio;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;



/**
 * Created by a.polunina on 31.10.2016.
 */

public class CheckConnection {

	/**
	 * Get the network info
	 * @param context
	 * @return
	 */
	public static NetworkInfo getNetworkInfo(Context context){
		ConnectivityManager cm = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
		return cm.getActiveNetworkInfo();
	}

	/**
	 * Check if there is any connectivity
	 * @param context
	 * @return
	 */
	public static boolean isConnected(Context context){
		NetworkInfo info = CheckConnection.getNetworkInfo(context);
		return (info != null && info.isConnected());
	}


	/**
	 * Check if there is any connectivity to a Wifi network
	 * @param context
	 * @return
	 */
	public static boolean isConnectedWifi(Context context){
		NetworkInfo info = CheckConnection.getNetworkInfo(context);
		return (info != null && info.isConnected() && info.getType() == ConnectivityManager.TYPE_WIFI);
	}

	/**
	 * Check if there is any connectivity to a mobile network
	 * @param context
	 * @return
	 */
	public static boolean isConnectedMobile(Context context){
		NetworkInfo info = CheckConnection.getNetworkInfo(context);
		return (info != null && info.isConnected() && info.getType() == ConnectivityManager.TYPE_MOBILE);
	}


	public boolean CheckInetConnection(Context mCcontext) {
		ConnectivityManager connectivityManager = (ConnectivityManager) mCcontext.getSystemService(mCcontext.CONNECTIVITY_SERVICE);
		NetworkInfo inetInfo = connectivityManager.getActiveNetworkInfo();
		//Если никакого интернет подключения нет - возвращаем false
		if(inetInfo == null || !inetInfo.isConnected()){
			new Utilites().SendInformationTxt(mCcontext, "Internet is disconnected");
			return false;
		}
		return true;
	}


}
