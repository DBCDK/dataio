package dk.dbc.dataio.gatekeeper;

import org.junit.Test;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

public class ShutdownManagerTest {
    @Test
    public void constructor_setsInitialState() {
        final ShutdownManager shutdownManager = new ShutdownManager();
        assertThat("isShutdownInProgress", shutdownManager.isShutdownInProgress(), is(false));
        assertThat("isReadyToExit", shutdownManager.isReadyToExit(), is(true));
    }

    @Test
    public void signalShutdownInProgress_setsState() {
        final ShutdownManager shutdownManager = new ShutdownManager();
        shutdownManager.signalShutdownInProgress();
        assertThat("isShutdownInProgress", shutdownManager.isShutdownInProgress(), is(true));
        assertThat("isReadyToExit", shutdownManager.isReadyToExit(), is(true));
    }

    @Test
    public void signalBusy_setsStateAndReturnsTrue() {
        final ShutdownManager shutdownManager = new ShutdownManager();
        assertThat("signalBusy()", shutdownManager.signalBusy(), is(true));
        assertThat("isShutdownInProgress", shutdownManager.isShutdownInProgress(), is(false));
        assertThat("isReadyToExit", shutdownManager.isReadyToExit(), is(false));
    }

    @Test
    public void signalBusy_shutdownInProgress_returnsFalse() {
        final ShutdownManager shutdownManager = new ShutdownManager();
        shutdownManager.signalShutdownInProgress();
        assertThat("signalBusy()", shutdownManager.signalBusy(), is(false));
        assertThat("isShutdownInProgress", shutdownManager.isShutdownInProgress(), is(true));
        assertThat("isReadyToExit", shutdownManager.isReadyToExit(), is(true));
    }

    @Test
    public void signalReadyToExit_setsState() {
        final ShutdownManager shutdownManager = new ShutdownManager();
        shutdownManager.signalBusy();
        shutdownManager.signalReadyToExit();
        assertThat("isShutdownInProgress", shutdownManager.isShutdownInProgress(), is(false));
        assertThat("isReadyToExit", shutdownManager.isReadyToExit(), is(true));
    }
}
