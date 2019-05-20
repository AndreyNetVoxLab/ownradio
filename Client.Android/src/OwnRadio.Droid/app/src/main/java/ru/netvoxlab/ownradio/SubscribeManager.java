package ru.netvoxlab.ownradio;

import android.content.ComponentName;
import android.content.Context;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.os.RemoteException;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.util.Log;
import android.widget.Toast;

import com.android.vending.billing.IInAppBillingService;

import java.util.ArrayList;

import static ru.netvoxlab.ownradio.Constants.TAG;

public class SubscribeManager {

    /*
    * Проверка статуса подписки
    * Статус подписки сохраняется в prefitem "is_subscribed"*/


    public static boolean CheckSubscribeStatus(Context context, IInAppBillingService mService) {
        boolean status = false;
        try {
            Bundle ownedItems = mService.getPurchases(3, context.getPackageName(), "subs", null);
            int response = ownedItems.getInt("RESPONSE_CODE");
            if (response == 0) {
                ArrayList<String> purchase_item_list = ownedItems.getStringArrayList("INAPP_PURCHASE_ITEM_LIST");
                int purchaseIndex = purchase_item_list.indexOf("test_subscribe_1");
                PrefManager prefManager = new PrefManager(context);
                if (purchaseIndex != -1) {
                    prefManager.setPrefItemBool("is_subscribed", true);
                    status = true;
                } else {
                    prefManager.setPrefItemBool("is_subscribed", false);
                }
            }

        } catch (RemoteException e) {
            Log.d(TAG, " " + e.getLocalizedMessage());
        }
        finally {
            return status;
        }
    }
    public static boolean CheckSubscribeStatus(Context context, IInAppBillingService mService, boolean displayToast) {
        boolean status = false;
        try {
            Bundle ownedItems = mService.getPurchases(3, context.getPackageName(), "subs", null);
            int response = ownedItems.getInt("RESPONSE_CODE");
            if (response == 0) {
                ArrayList<String> purchase_item_list = ownedItems.getStringArrayList("INAPP_PURCHASE_ITEM_LIST");
                int purchaseIndex = purchase_item_list.indexOf("test_subscribe_1");
                PrefManager prefManager = new PrefManager(context);
                if (purchaseIndex != -1) {
                    prefManager.setPrefItemBool("is_subscribed", true);
                    status = true;
                } else {
                    prefManager.setPrefItemBool("is_subscribed", false);
                    //if(displayToast){
                      //  Toast.makeText(context, "Подписка не оплачена, максимальный объем кэша 1Gb, заполнение кэша недоступно", Toast.LENGTH_LONG).show();
                    //}
                }
            }

        } catch (RemoteException e) {
            Log.d(TAG, " " + e.getLocalizedMessage());
        }
        finally {
            return status;
        }
    }
}

