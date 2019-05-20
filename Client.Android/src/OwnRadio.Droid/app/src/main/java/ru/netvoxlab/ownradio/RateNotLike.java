package ru.netvoxlab.ownradio;

import android.app.Activity;
import android.content.Intent;
import android.graphics.drawable.ColorDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.annotation.Nullable;
import android.support.constraint.ConstraintLayout;
import android.support.v4.content.ContextCompat;
import android.util.DisplayMetrics;
import android.view.View;
import android.widget.ImageButton;

import static ru.netvoxlab.ownradio.MainActivity.ActionPopupClose;
import static ru.netvoxlab.ownradio.MainActivity.ActionPopupSetForeground;

public class RateNotLike extends Activity {
    ImageButton closeBtn;
    ImageButton openVk;


    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Intent mainIntent = new Intent(ActionPopupSetForeground);
        getApplicationContext().sendBroadcast(mainIntent);

        setContentView(R.layout.app_not_like_popup);

        DisplayMetrics dm = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(dm);

        int width = dm.widthPixels;
        int height = dm.heightPixels;
        getWindow().setLayout(width - 200, 480);

        closeBtn = findViewById(R.id.closePopupNotLike);
        closeBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
                Intent mainIntent = new Intent(ActionPopupClose);
                getApplicationContext().sendBroadcast(mainIntent);
            }
        });

        openVk = findViewById(R.id.rateAppVk);
        openVk.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
                Uri vkUri = Uri.parse("https://vk.com/ownradio");
                Intent intent = new Intent(Intent.ACTION_VIEW, vkUri);
                startActivity(intent);
                Intent mainIntent = new Intent(ActionPopupClose);
                getApplicationContext().sendBroadcast(mainIntent);
            }
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        Intent mainIntent = new Intent(ActionPopupClose);
        getApplicationContext().sendBroadcast(mainIntent);
    }
}
