package com.lastutf445.home2.activities;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.lastutf445.home2.R;
import com.lastutf445.home2.loaders.DataLoader;

public class SplashScreen extends AppCompatActivity {

    @NonNull
    private Handler handler;

    private Runnable transition = new Runnable() {
        @Override
        public void run() {
            Intent i = new Intent(
                    SplashScreen.this,
                    DataLoader.getBoolean("FirstStart", true)
                            ? IntroduceScreen.class : MainActivity.class
            );

            SplashScreen.this.startActivity(i);
            SplashScreen.this.overridePendingTransition(R.anim.fragment_add, R.anim.fragment_hide);
            SplashScreen.this.finish();
        }
    };

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splashscreen);
    }

    @Override
    protected void onStart() {
        super.onStart();

        DataLoader.init(
                getApplicationContext(),
                getResources()
        );

        handler = new Handler();
        handler.postDelayed(transition, 1000);
    }

    @Override
    public void onBackPressed() {
        handler.removeCallbacks(transition);
        super.onBackPressed();
    }
}
