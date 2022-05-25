package dk.dbc.dataio.commons.time;

public class StopWatch {
    /* Stores the start time when an object of the StopWatch class is initialized. */
    private long startTime;

    /**
     * Custom constructor which initializes the {@link #startTime} parameter.
     */
    public StopWatch() {
        startTime = System.currentTimeMillis();
    }

    /**
     * Gets the elapsed time (in milliseconds) since the time the object of StopWatch was initialized.
     *
     * @return Elapsed time in milliseconds.
     */
    public long getElapsedTime() {
        return System.currentTimeMillis() - startTime;
    }
}
