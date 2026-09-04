package com.comrst;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;

public class LauncherActivity extends AppCompatActivity {
    private static final long LOGO_ICON_ANIMATION = 900;
    private static final long LOGO_ANIMATION = 600;
    private static final long APP_NAME_DELAY = 700;
    private static final long TAGLINE_DELAY = 700;
    private static final long SPLASH_DURATION = 1200;

    FirebaseAuth myAuth;
    private ImageView logo;
    private TextView appName , tagName;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_launcher);
        myAuth = FirebaseAuth.getInstance();
        logo = findViewById(R.id.logo_id);
        appName = findViewById(R.id.app_name_id);
        tagName = findViewById(R.id.tag_name_id);

        logo.setAlpha(0f);
        logo.setScaleX(0.85f);
        logo.setScaleY(0.85f);

        appName.setAlpha(0f);
        tagName.setAlpha(0f);

        logo.animate()
                .alpha(1f)
                .scaleX(1f)
                .scaleY(1f)
                .setDuration(LOGO_ICON_ANIMATION)
                .withEndAction(()->{
                    logo.animate()
                            .translationY(-30f)
                            .setDuration(LOGO_ANIMATION)
                            .withEndAction(()->{
                                appName.animate()
                                        .alpha(1f)
                                        .setDuration(APP_NAME_DELAY)
                                        .withEndAction(()->{
                                            tagName.animate()
                                                    .alpha(1f)
                                                    .setDuration(TAGLINE_DELAY)
                                                    .withEndAction(()->{
                                                        tagName.postDelayed(()->{
                                                            if (myAuth.getCurrentUser()!=null){
                                                                goTOMatch();
                                                            }else {
                                                                signInAnonymously();
                                                            }

                                                        },SPLASH_DURATION);


                                                    });
                                        });

                            });
                });


    }

    private void signInAnonymously() {
        myAuth.signInAnonymously().addOnCompleteListener(this,task -> {
            if (task.isSuccessful()){
                goTOMatch();
            }else {
                Log.e("USER","Login Failed: "+task.getException());
            }
        });
    }

    private void goTOMatch() {
        startActivity(new Intent(this,MatchmakingActivity.class));
        finish();
    }
}