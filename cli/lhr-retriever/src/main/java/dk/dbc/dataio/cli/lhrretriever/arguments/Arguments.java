package dk.dbc.dataio.cli.lhrretriever.arguments;

import net.sourceforge.argparse4j.ArgumentParsers;
import net.sourceforge.argparse4j.helper.HelpScreenException;
import net.sourceforge.argparse4j.inf.ArgumentParser;
import net.sourceforge.argparse4j.inf.ArgumentParserException;
import net.sourceforge.argparse4j.inf.Namespace;

public class Arguments {
    public String configPath;
    public String flowName;
    public String outputPath;

    public static Arguments parseArgs(String[] args) throws ArgParseException {
        ArgumentParser parser = ArgumentParsers.newArgumentParser("lhr-retriever");
        parser.addArgument("config-path")
                .help("path to config file with values for open agency and " +
                        "rawrepo connections");
        parser.addArgument("flow-name").help(
                "name of flow containing javascript for lhr conversion");
        parser.addArgument("output").help("file to write iso2709 output to");

        Namespace ns;
        try {
            ns = parser.parseArgs(args);
        } catch (ArgumentParserException e) {
            parser.handleError(e);
            if (e instanceof HelpScreenException) System.exit(1);
            throw new ArgParseException(String.format(
                    "error parsing arguments: %s", e.toString()), e);
        }

        Arguments arguments = new Arguments();
        arguments.configPath = ns.getString("config_path");
        arguments.flowName = ns.getString("flow_name");
        arguments.outputPath = ns.getString("output");
        return arguments;
    }
}
