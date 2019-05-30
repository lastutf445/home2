package com.lastutf445.home2.activities;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;

import com.lastutf445.home2.R;

public class SplashScreen extends AppCompatActivity {

    private Handler handler = new Handler();
    private Runnable transition = new Runnable() {

        @Override
        public void run() {
            Intent i = new Intent(SplashScreen.this, MainActivity.class);
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
        handler.postDelayed(transition, 1000);
    }

    @Override
    public void onBackPressed() {
        handler.removeCallbacks(transition);
        super.onBackPressed();
    }
}
