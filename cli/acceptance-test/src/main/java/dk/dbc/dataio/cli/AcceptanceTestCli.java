package dk.dbc.dataio.cli;

import com.beust.jcommander.JCommander;
import com.beust.jcommander.ParameterException;
import dk.dbc.dataio.cli.command.CommitCommand;
import dk.dbc.dataio.cli.command.CreateCommand;
import dk.dbc.dataio.cli.options.CommitOptions;
import dk.dbc.dataio.cli.options.CreateOptions;
import dk.dbc.dataio.cli.options.Options;

/**
 * dataIO acceptance test command line interface
 * <p>
 *     To show usage run
 *
 *     %&gt; java -jar dataio-cli-acctest.jar
 * </p>
 */
public class AcceptanceTestCli {
    private final Options options;
    private final CreateOptions createOptions;
    private final CommitOptions commitOptions;

    public static void main(String[] args) {
        final AcceptanceTestCli cli = new AcceptanceTestCli();
        cli.run(args);
    }

    private AcceptanceTestCli() {
        options = new Options();
        createOptions = new CreateOptions();
        commitOptions = new CommitOptions();
    }

    private void run(String[] args) {
        final JCommander argParser = new JCommander(options);
        try {
            argParser.setProgramName("dataio-cli-acctest");
            argParser.addCommand("create", createOptions);
            argParser.addCommand("commit", commitOptions);
            argParser.parse(args);

            if (args.length == 0 || options.help || createOptions.help || commitOptions.help) {
                argParser.usage();
                System.exit(0);
            }

            switch (argParser.getParsedCommand()) {
                case "create": new CreateCommand(createOptions).execute();
                    break;
                case "commit": new CommitCommand(commitOptions).execute();
                    break;
                default:
                    argParser.usage();
            }
        } catch (ParameterException e) {
            System.out.println(e.getMessage());
            argParser.usage();
            System.exit(-1);
        } catch (Exception e) {
            e.printStackTrace();
            System.exit(-1);
        }

        System.exit(0);
    }
}
