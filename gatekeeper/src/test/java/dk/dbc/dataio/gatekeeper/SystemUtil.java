package dk.dbc.dataio.gatekeeper;

public final class SystemUtil {
    private SystemUtil() {
    }

    public static boolean isOsX() {
        return System.getProperty("os.name").toLowerCase().startsWith("mac");
    }
}
