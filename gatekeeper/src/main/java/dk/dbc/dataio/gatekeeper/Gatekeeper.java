package dk.dbc.dataio.gatekeeper;

import dk.dbc.dataio.gatekeeper.wal.ModificationLockedException;
import dk.dbc.dataio.gatekeeper.wal.WriteAheadLog;
import dk.dbc.dataio.gatekeeper.wal.WriteAheadLogH2;
import org.apache.commons.cli.ParseException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Objects;

public class Gatekeeper {
    private static final Logger LOGGER = LoggerFactory.getLogger(Gatekeeper.class);

    private final JobDispatcher jobDispatcher;

    public static void main(String[] args) throws InterruptedException, ParseException, ModificationLockedException {
        Util.parseCommandLine(args);
        Path dir = Paths.get(Objects.requireNonNull(Util.CommandLineOption.GUARDED_DIR.get()));
        String jobStoreServiceUrl = Util.CommandLineOption.JOBSTORE_SERVICE.get();
        String fileStoreServiceUrl = Util.CommandLineOption.FILESTORE_SERVICE.get();
        ShutdownManager shutdownManager = new ShutdownManager();

        registerShutdownHook(shutdownManager);

        Gatekeeper gatekeeper = new Gatekeeper(dir, fileStoreServiceUrl, jobStoreServiceUrl, shutdownManager);
        while (true) {
            gatekeeper.standGuard();
        }
    }

    public Gatekeeper(Path dir, String fileStoreServiceUrl, String jobStoreServiceUrl, ShutdownManager shutdownManager) {
        final WriteAheadLog wal = new WriteAheadLogH2();
        final ConnectorFactory connectorFactory = new ConnectorFactory(fileStoreServiceUrl, jobStoreServiceUrl);
        jobDispatcher = new JobDispatcher(dir, wal, connectorFactory, shutdownManager);
    }

    public void standGuard() throws InterruptedException, ModificationLockedException {
        try {
            jobDispatcher.execute();
        } catch (InterruptedException e) {
            LOGGER.warn("Job dispatcher was interrupted", e);
            throw e;
        } catch (ModificationLockedException e) {
            LOGGER.error("Job dispatcher caught WAL exception", e);
            throw e;
        } catch (Exception e) {
            LOGGER.error("Caught exception from job dispatcher - restarting guard operation", e);
            Thread.sleep(1000);
        }
    }

    public static void registerShutdownHook(final ShutdownManager shutdownManager) {
        Runtime.getRuntime().addShutdownHook(new Thread() {
            @Override
            public void run() {
                shutdownManager.signalShutdownInProgress();
                // Wait up to 30 seconds giving the job dispatcher a chance to finish.
                for (int i = 0; i < 30; i++) {
                    if (shutdownManager.isReadyToExit())
                        break;
                    try {
                        wait(1000);
                    } catch (InterruptedException e) {
                        LOGGER.warn("Interrupted in shutdown hook", e);
                    }
                    if (!shutdownManager.isReadyToExit()) {
                        LOGGER.error("Shutdown while job dispatcher in busy state - system corruption possible!");
                    }
                }
            }
        });
    }
}
