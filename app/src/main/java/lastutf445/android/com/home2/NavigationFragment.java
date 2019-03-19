package lastutf445.android.com.home2;

import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.support.v4.app.Fragment;
import android.view.View;

import org.json.JSONObject;

public abstract class NavigationFragment extends Fragment {

    protected String id;

    public String getFragmentId() {
        return id;
    }

    public void navigationFragmentSetup() {

    }

    public void universalViewerSetup(UniversalViewer uv, View view, int layout) {

    }

    public void onActivityResult(int resultCode, Intent data) {

    }
}
