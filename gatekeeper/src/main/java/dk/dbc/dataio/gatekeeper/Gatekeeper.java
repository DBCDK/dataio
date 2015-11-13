/*
 * DataIO - Data IO
 * Copyright (C) 2015 Dansk Bibliotekscenter a/s, Tempovej 7-11, DK-2750 Ballerup,
 * Denmark. CVR: 15149043
 *
 * This file is part of DataIO.
 *
 * DataIO is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * DataIO is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with DataIO.  If not, see <http://www.gnu.org/licenses/>.
 */

package dk.dbc.dataio.gatekeeper;

import dk.dbc.dataio.gatekeeper.wal.ModificationLockedException;
import dk.dbc.dataio.gatekeeper.wal.WriteAheadLog;
import dk.dbc.dataio.gatekeeper.wal.WriteAheadLogH2;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.GnuParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.OptionBuilder;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Path;
import java.nio.file.Paths;

public class Gatekeeper {
    private static final Logger LOGGER = LoggerFactory.getLogger(Gatekeeper.class);

    private final JobDispatcher jobDispatcher;

    public static void main(String[] args) throws InterruptedException, ParseException, ModificationLockedException {
        final CommandLine commandLine = parseCommandLine(args);
        final Path dir = Paths.get(commandLine.getOptionValue("d"));
        final Path shadowDir = Paths.get(commandLine.getOptionValue("s"));
        final String jobStoreServiceUrl = commandLine.getOptionValue("j");
        final String fileStoreServiceUrl = commandLine.getOptionValue("f");
        final ShutdownManager shutdownManager = new ShutdownManager();

        registerShutdownHook(shutdownManager);

        final Gatekeeper gatekeeper = new Gatekeeper(dir, shadowDir,
                fileStoreServiceUrl, jobStoreServiceUrl, shutdownManager);
        while (true) {
            gatekeeper.standGuard();
        }
    }

    public Gatekeeper(Path dir, Path shadowDir, String fileStoreServiceUrl, String jobStoreServiceUrl,
                      ShutdownManager shutdownManager) {
        final WriteAheadLog wal = new WriteAheadLogH2();
        final ConnectorFactory connectorFactory = new ConnectorFactory(fileStoreServiceUrl, jobStoreServiceUrl);
        jobDispatcher = new JobDispatcher(dir, shadowDir, wal, connectorFactory, shutdownManager);
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

    public static CommandLine parseCommandLine(String[] args) throws ParseException {
        final CommandLineParser parser = new GnuParser();
        final HelpFormatter helpFormatter = new HelpFormatter();
        final Options options = getCommandLineOptions();
        try {
            return parser.parse(options, args);
        } catch (ParseException e) {
            helpFormatter.printHelp("java -jar <jarfile>", options);
            throw e;
        }
    }

    private static Options getCommandLineOptions() {
        final Options options = new Options();
        options.addOption(new Option("h", "help", false, "Produce help message"));

        @SuppressWarnings("static-access")
        final Option dir = OptionBuilder.withArgName("dir")
                .hasArg()
                .isRequired()
                .withDescription("Path of directory guarded by this gatekeeper instance")
                .withLongOpt("guarded-dir")
                .create("d");
        options.addOption(dir);

        @SuppressWarnings("static-access")
        final Option shadowDir = OptionBuilder.withArgName("dir")
                .hasArg()
                .isRequired()
                .withDescription("Path of shadow directory")
                .withLongOpt("shadow-dir")
                .create("s");
        options.addOption(shadowDir);

        @SuppressWarnings("static-access")
        final Option jobStoreServiceUrl = OptionBuilder.withArgName("url")
                .hasArg()
                .isRequired()
                .withDescription("Base URL of job-store service")
                .withLongOpt("job-store-service-url")
                .create("j");
        options.addOption(jobStoreServiceUrl);

        @SuppressWarnings("static-access")
        final Option fileStoreServiceUrl = OptionBuilder.withArgName("url")
                .hasArg()
                .isRequired()
                .withDescription("Base URL of file-store service")
                .withLongOpt("file-store-service-url")
                .create("f");
        options.addOption(fileStoreServiceUrl);

        return options;
    }
}
