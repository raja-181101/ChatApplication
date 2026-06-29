package com.example.chatapplication;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.firebase.auth.FirebaseAuth;

public class LauncherActivity extends AppCompatActivity {

    FirebaseAuth myAuth;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        myAuth = FirebaseAuth.getInstance();

        if (myAuth.getCurrentUser()!=null){
            goTOMatch();
        }else {
            signInAnonymously();
        }

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