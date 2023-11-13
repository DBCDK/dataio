package dk.dbc.dataio.gatekeeper;

import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.GnuParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.OptionBuilder;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.Properties;

public class Util {
    private static final Logger LOGGER = LoggerFactory.getLogger(Util.class);
    protected static Properties commandLineProps = new Properties();


    public enum CommandLineOption {
        GUARDED_DIR("d"),
        JOBSTORE_SERVICE("j"),
        FILESTORE_SERVICE("f"),
        FLOWSTORE_SERVICE("c"),
        CC_MAIL_ADDRESS("m");

        private final String option;

        CommandLineOption(String option) {
            this.option = option;
        }

        public String get() {
            return (String) commandLineProps.get(option);
        }

    }
    public static void parseCommandLine(String[] args) throws ParseException {
        CommandLineParser parser = new GnuParser();
        HelpFormatter helpFormatter = new HelpFormatter();
        Options options = getCommandLineOptions();
        try {
            parser.parse(options, args);
            Arrays.stream(parser.parse(options, args).getOptions())
                    .forEach(option -> commandLineProps.put(option.getOpt(), option.getValue()));
            LOGGER.info("Command line properties: {}", commandLineProps);
        } catch (ParseException e) {
            helpFormatter.printHelp("java -jar <jarfile>", options);
            throw e;
        }

    }
    private static Options getCommandLineOptions() {
        Options options = new Options();
        options.addOption(new Option("h", "help", false, "Produce help message"));

        @SuppressWarnings("static-access") Option dir = OptionBuilder.withArgName("dir")
                .hasArg()
                .isRequired()
                .withDescription("Path of directory guarded by this gatekeeper instance")
                .withLongOpt("guarded-dir")
                .create("d");
        options.addOption(dir);

        @SuppressWarnings("static-access") Option jobStoreServiceUrl = OptionBuilder.withArgName("url")
                .hasArg()
                .isRequired()
                .withDescription("Base URL of job-store service")
                .withLongOpt("job-store-service-url")
                .create("j");
        options.addOption(jobStoreServiceUrl);

        @SuppressWarnings("static-access") Option fileStoreServiceUrl = OptionBuilder.withArgName("url")
                .hasArg()
                .isRequired()
                .withDescription("Base URL of file-stopre service")
                .withLongOpt("file-store-service-url")
                .create("f");
        options.addOption(fileStoreServiceUrl);

        @SuppressWarnings("static-access") Option flowStoreServiceUrl = OptionBuilder.withArgName("url")
                .hasArg()
                .isRequired()
                .withDescription("Base URL of flow-store service")
                .withLongOpt("flow-store-service-url")
                .create("c");
        options.addOption(flowStoreServiceUrl);

        @SuppressWarnings("static-access") Option dbcMailAddress = OptionBuilder.withArgName("address")
                .hasArg()
                .withDescription("Additional DBC mail address")
                .withLongOpt("mailaddress")
                .create("m");
        options.addOption(dbcMailAddress);

        return options;
    }

}
