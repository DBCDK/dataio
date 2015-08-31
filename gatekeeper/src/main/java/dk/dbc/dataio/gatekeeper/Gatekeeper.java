package dk.dbc.dataio.gatekeeper;

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

    public static void main(String[] args) throws InterruptedException, ParseException {
        final CommandLine commandLine = parseCommandLine(args);
        final Path dir = Paths.get(commandLine.getOptionValue("d"));
        final Gatekeeper gatekeeper = new Gatekeeper(dir);

        while (true) {
            gatekeeper.standGuard();
        }
    }

    public Gatekeeper(Path dir) {
        this.jobDispatcher = new JobDispatcher(dir);
    }

    public void standGuard() throws InterruptedException {
        try {
            jobDispatcher.execute();
        } catch (InterruptedException e) {
            LOGGER.warn("Job dispatcher was interrupted", e);
            throw e;
        } catch (Exception e) {
            LOGGER.error("Caught exception from job dispatcher - restarting guard operation", e);
            Thread.sleep(1000);
        }
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

        return options;
    }
}
