package dk.dbc.dataio.commons.types;

import java.time.Duration;

public class Tools {
    public static void sleep(Duration duration) {
        sleep(duration.toMillis());
    }

    public static void sleep(long ms) {
        try {
            Thread.sleep(ms);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
