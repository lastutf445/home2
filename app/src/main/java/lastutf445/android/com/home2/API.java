package lastutf445.android.com.home2;

public final class API {

    /**
     *
     * @param login - login
     * @param password - password
     * @return int
     *
     * 0 - all done
     * 1 - master server is undefined
     * 2 - network error
     * 3 - master server isn't responding
     * 3 - incorrect login
     * 4 - incorrect password
     *
     */

    public static int login(String login, String password) {
        if (!Data.getBoolean("masterServer", false) || !Data.getBoolean("MasterServerIP", false)) {
            return 1;
        }

        return 0;
    }

    public static boolean loginAsGuest(String name) {
        Data.set("GuestMode", true);
        Data.set("AccountName", name);
        Data.recordOptions();
        return true;
    }

    public static boolean restoreAccount(String login) {

        return false;
    }

    public static boolean logout() {
        Data.set("GuestMode", false);
        Data.set("AccountName", null);
        Data.set("Session", null);
        Data.recordOptions();
        return true;
    }

    public static boolean isAuthorized() {
        return (Data.getString("Session", null) != null || Data.getBoolean("GuestMode", false));
    }

    public static boolean rename(String name) {
        if (Data.getBoolean("GuestMode", true)) {
            Data.set("AccountName", name);
            Data.recordOptions();
            return true;
        }

        return false;
    }
}
