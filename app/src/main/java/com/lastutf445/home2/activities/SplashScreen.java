package com.lastutf445.home2.activities;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;

import com.lastutf445.home2.R;
import com.lastutf445.home2.loaders.CryptoLoader;
import com.lastutf445.home2.loaders.DataLoader;
import com.lastutf445.home2.loaders.FragmentsLoader;
import com.lastutf445.home2.loaders.ModulesLoader;
import com.lastutf445.home2.loaders.NodesLoader;
import com.lastutf445.home2.network.Sync;

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
        handler.postDelayed(transition, 1500);

        FragmentsLoader.init(
                getSupportFragmentManager(),
                R.id.content
        );

        DataLoader.init(
                getApplicationContext(),
                getResources()
        );

        CryptoLoader.init();
        NodesLoader.init();
        ModulesLoader.init();
        Sync.init();
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        handler.removeCallbacks(transition);
    }
}
