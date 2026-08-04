package com.example.chatapplication;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.firebase.auth.FirebaseAuth;

public class LauncherActivity extends AppCompatActivity {

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
                .setDuration(900)
                .withEndAction(()->{
                    logo.animate()
                            .translationY(-30f)
                            .setDuration(600)
                            .withEndAction(()->{
                                appName.animate()
                                        .alpha(1f)
                                        .setDuration(700)
                                        .withEndAction(()->{
                                            tagName.animate()
                                                    .alpha(1f)
                                                    .setDuration(700)
                                                    .withEndAction(()->{
                                                        tagName.postDelayed(()->{
                                                            if (myAuth.getCurrentUser()!=null){
                                                                goTOMatch();
                                                            }else {
                                                                signInAnonymously();
                                                            }

                                                        },1200);


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