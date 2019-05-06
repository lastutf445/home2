package com.lastutf445.home2.loaders;

import android.content.res.Resources;

import com.lastutf445.home2.R;
import java.util.UUID;

public final class UserLoader {

    public static boolean isAuthenticated() {
        return DataLoader.getString("Session", null) != null;
    }

    public static String getUsername() {
        Resources res = DataLoader.getAppResources();
        String name = DataLoader.getString("Username", res.getString(R.string.usernameError));
        return isAuthenticated() ? name : res.getString(R.string.notAuthenticated);
    }

    public static int auth(String login, String password) {
        createSession();

        // TODO: TRUE AUTHENTICATION

        DataLoader.set(
                "Username",
                "just rofl"
        );

        DataLoader.save();
        return 1;
    }

    public static void authBasic() {
        createSession();

        DataLoader.set(
                "Username",
                DataLoader.getAppResources().getString(R.string.usernameBasic)
        );

        DataLoader.save();
    }

    public static void logout() {
        DataLoader.set("Session", null);
        DataLoader.set("Username", null);
        DataLoader.save();
    }

    private static void createSession() {
        DataLoader.set("Session", UUID.randomUUID().toString().replace("-", "").substring(0, 19));
    }
}
