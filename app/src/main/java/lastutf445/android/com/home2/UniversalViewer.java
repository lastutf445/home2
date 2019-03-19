package lastutf445.android.com.home2;

import android.content.Intent;
import android.os.Handler;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

public class UniversalViewer extends AppCompatActivity {

    private View content;
    private int returnCode;
    private int layout;
    private boolean needReload;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_universal_viewer);

        content = findViewById(R.id.universalViewerContent);

        Intent i = getIntent();
        layout = i.getIntExtra("layout", R.layout.menu_about);
        returnCode = i.getIntExtra("returnCode", 0);
        needReload = i.getBooleanExtra("needReload", false);

        getLayoutInflater().inflate(layout, (ViewGroup) content);
        setupUniversalViewer();
    }

    private void setupUniversalViewer() {
        MainActivity.universalViewerSetupBridge(this, content, layout);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        //super.onActivityResult(requestCode, resultCode, data);

        if (data.getBooleanExtra("needReload", false)) {
            setupUniversalViewer();
        }
    }

    public void kill() {
        Intent i = new Intent();
        i.putExtra("needReload", needReload);
        setResult(returnCode, i);
        finish();
    }

    @Override
    public void onBackPressed() {
        kill();
    }
}
