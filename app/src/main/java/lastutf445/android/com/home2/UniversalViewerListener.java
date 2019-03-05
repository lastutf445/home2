package lastutf445.android.com.home2;

import android.app.Activity;
import android.view.View;

public abstract class UniversalViewerListener implements View.OnClickListener {

    UniversalViewer uv;
    View view;

    UniversalViewerListener(UniversalViewer uv, View view) {
        this.uv = uv;
        this.view = view;
    }

    @Override
    public void onClick(View v) {

    }
}
