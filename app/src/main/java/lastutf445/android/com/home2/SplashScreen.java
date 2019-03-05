package lastutf445.android.com.home2;

import android.content.Intent;
import android.os.Handler;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;

public class SplashScreen extends AppCompatActivity {

    private Handler handler = new Handler();
    private Runnable transition = new Runnable() {

        @Override
        public void run() {
            Intent mainIntent = new Intent(SplashScreen.this, MainActivity.class);
            SplashScreen.this.startActivity(mainIntent);
            SplashScreen.this.overridePendingTransition(0, 0);
            SplashScreen.this.finish();
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash_screen);
        this.handler.postDelayed(this.transition, 1000);;
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        this.handler.removeCallbacks(this.transition);
    }
}
