package ru.netvoxlab.ownradio;

import android.app.Activity;
import android.content.Intent;
import android.graphics.drawable.ColorDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.constraint.ConstraintLayout;
import android.support.v4.content.ContextCompat;
import android.util.DisplayMetrics;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ViewAnimator;

import static ru.netvoxlab.ownradio.MainActivity.ActionPopupClose;

public class RateThanksPopup extends Activity {

    ImageButton closeBtn;
    ImageButton rateGooglePlay;
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.thanks_for_rate_popup);

        DisplayMetrics dm = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(dm);

        int width = dm.widthPixels;
        int height = dm.heightPixels;
        getWindow().setLayout(width - 180, 600);

        closeBtn = findViewById(R.id.closePopupLike);
        closeBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
                Intent mainIntent = new Intent(ActionPopupClose);
                getApplicationContext().sendBroadcast(mainIntent);
            }
        });

        rateGooglePlay = findViewById(R.id.gplayRedirrect);
        rateGooglePlay.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
                Intent mainIntent = new Intent(ActionPopupClose);
                getApplicationContext().sendBroadcast(mainIntent);
                Uri vkUri = Uri.parse("https://play.google.com/store/apps/details?id=ru.netvoxlab.ownradio");
                Intent intent = new Intent(Intent.ACTION_VIEW, vkUri);
                startActivity(intent);
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
