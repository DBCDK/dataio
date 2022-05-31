package dk.dbc.dataio.gatekeeper;

/**
 * Simple class used to coordinate VM shutdown sequence between two threads
 */
public class ShutdownManager {
    private boolean shutdownInProgress = false;
    private boolean readyToExit = true;

    /**
     * Sets internal shutdown-in-progress state to true
     */
    public synchronized void signalShutdownInProgress() {
        shutdownInProgress = true;
    }

    /**
     * Sets internal ready-to-exit state to false unless
     * shutdown-in-progress is already true
     *
     * @return true if ready-to-exit state was changed, otherwise false
     */
    public synchronized boolean signalBusy() {
        if (!shutdownInProgress) {
            readyToExit = false;
        }
        return !shutdownInProgress;
    }

    /**
     * Sets internal ready-to-exit state to true
     */
    public synchronized void signalReadyToExit() {
        readyToExit = true;
    }

    /**
     * @return true if internal shutdown-in-progress state is true, otherwise false
     */
    public synchronized boolean isShutdownInProgress() {
        return shutdownInProgress;
    }

    /**
     * @return true if internal ready-to-exit state is true, otherwise false
     */
    public synchronized boolean isReadyToExit() {
        return readyToExit;
    }
}
