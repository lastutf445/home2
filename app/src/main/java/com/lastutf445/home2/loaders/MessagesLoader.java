package com.lastutf445.home2.loaders;

import com.lastutf445.home2.R;
import com.lastutf445.home2.fragments.menu.Sync;

public class MessagesLoader {

    public static class SyncSwitch implements Runnable {
        @Override
        public void run() {
            boolean state = DataLoader.getBoolean("SyncMessages", true);
            DataLoader.set("SyncMessages", !state);

            // TODO: save optimization
            DataLoader.save();
/*
            if (state) wipeSyncing();
            else reloadSyncing();
*/
        }
    }
}
