/*
 * Copyright Dansk Bibliotekscenter a/s. Licensed under GNU GPLv3
 * See license text in LICENSE.txt
 */

package dk.dbc.dataio.cli;

import net.sourceforge.argparse4j.ArgumentParsers;
import net.sourceforge.argparse4j.inf.Argument;
import net.sourceforge.argparse4j.inf.ArgumentAction;
import net.sourceforge.argparse4j.inf.ArgumentParser;
import net.sourceforge.argparse4j.inf.ArgumentParserException;
import net.sourceforge.argparse4j.inf.Namespace;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Map;

public class Cli {
    public Namespace args;

    public Cli(String[] args) throws CliException {
        final ArgumentParser parser = ArgumentParsers.newFor("datafile-exporter").build()
                .defaultHelp(true)
                .description("Exports datafiles from jobs found based on filters");
        parser.addArgument("-t", "--target")
                .setDefault("http://dataio.dbc.dk")
                .help("URL of dataIO target system, eg. http://dataio.dbc.dk, http://dataio-staging.dbc.dk");
        parser.addArgument("-a", "--agency")
                .required(true)
                .help("Agency ID filter, eg. 870970");
        parser.addArgument("-p", "--packaging")
                .help("File transfer format filter, in danish (rammeformat), eg. addi-xml, iso,...");
        parser.addArgument("-f", "--format")
                .help("File content format filter, in danish (indholdsformat), eg. basis, worldcat,...");
        parser.addArgument("-e", "--encoding")
                .help("File encoding filter, eg. latin-1, utf8,...");
        parser.addArgument("-d", "--destination")
                .help("File destination filter, eg. ticklerepo, oclc,...");
        parser.addArgument("-cf", "--created-from")
                .action(new TimestampAction())
                .help("Job time-of-creation from filter in localtime format yyyy-MM-dd HH:mm, eg. 2020-01-22 13:22");
        parser.addArgument("-ct", "--created-to")
                .action(new TimestampAction())
                .help("Job time-of-creation to filter in localtime format yyyy-MM-dd HH:mm, eg. 2020-01-22 13:42");
        try {
            this.args = parser.parseArgs(args);
        } catch (ArgumentParserException e) {
            parser.handleError(e);
            throw new CliException(e);
        }
    }

    private static class TimestampAction implements ArgumentAction {
        private final static String PATTERN = "yyyy-MM-dd HH:mm";

        private final SimpleDateFormat format = new SimpleDateFormat(PATTERN);

        @Override
        public void run(ArgumentParser parser, Argument arg,
                        Map<String, Object> attrs, String flag, Object value)
                throws ArgumentParserException {
            try {
                format.parse((String) value);
                attrs.put(arg.getDest(), value);
            } catch (ParseException e) {
                throw new ArgumentParserException(e.getMessage(), parser);
            }
        }

        @Override
        public void onAttach(Argument arg) {}

        @Override
        public boolean consumeArgument() {
            return true;
        }
    }
}
