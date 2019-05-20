package ru.netvoxlab.ownradio;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.drawable.ColorDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.annotation.Nullable;
import android.support.constraint.ConstraintLayout;
import android.support.v4.content.ContextCompat;
import android.util.DisplayMetrics;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;

import static ru.netvoxlab.ownradio.MainActivity.ActionPopupClose;


public class RatePopup extends Activity {

    ImageButton vkRate;
    Button yesBtn;
    Button noBtn;
    ImageButton closeBtn;
    Boolean rated = false;

    SharedPreferences sp;
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        sp = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        setContentView(R.layout.rate_app_popup);

        DisplayMetrics dm = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(dm);

        int width = dm.widthPixels;
        int height = dm.heightPixels;
        getWindow().setLayout(width - 25, 750);


        vkRate = findViewById(R.id.sayVk);
        vkRate.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                rated = true;
                sp.edit().putBoolean("appIsRated", true).commit();
                Uri vkUri = Uri.parse("https://vk.com/ownradio");
                Intent intent = new Intent(Intent.ACTION_VIEW, vkUri);
                startActivity(intent);
                Intent mainIntent = new Intent(ActionPopupClose);
                getApplicationContext().sendBroadcast(mainIntent);
                finish();
            }
        });

        yesBtn = findViewById(R.id.yesBtn);
        yesBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                rated = true;
                finish();
                sp.edit().putBoolean("appIsRated", true).commit();
                startActivity(new Intent(RatePopup.this,RateThanksPopup.class));
            }
        });

        noBtn = findViewById(R.id.noBtn);
        noBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                rated = true;
                finish();
                sp.edit().putBoolean("appIsRated", true).commit();
                startActivity(new Intent(RatePopup.this,RateNotLike.class));
            }
        });

        closeBtn = findViewById(R.id.closePopup);
        closeBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                rated = true;
                sp.edit().putBoolean("appIsRated", false).commit();
                Intent mainIntent = new Intent(ActionPopupClose);
                getApplicationContext().sendBroadcast(mainIntent);
                finish();
            }
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if(!rated){
            Intent mainIntent = new Intent(ActionPopupClose);
            getApplicationContext().sendBroadcast(mainIntent);
        }
    }
}
