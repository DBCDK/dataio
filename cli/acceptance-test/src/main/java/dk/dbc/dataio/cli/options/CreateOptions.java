package dk.dbc.dataio.cli.options;

import com.beust.jcommander.Parameter;
import com.beust.jcommander.Parameters;

/**
 * Options for the 'create' sub command
 */
@Parameters(separators = "=", commandDescription = "Create dataIO acceptance test")
public class CreateOptions extends Options {

    @Parameter(names = {"-f", "--flow-name"}, description = "Name of dataIO flow", required = true)
    public String flowName;

    @Parameter(names = {"-r", "--revision"}, description = "Revision of NEXT component of dataIO flow", required = true)
    public Long revision;

    @Parameter(names = {"-t", "--testsuite"}, description = "Name of testsuite")
    public String testsuite;
}
