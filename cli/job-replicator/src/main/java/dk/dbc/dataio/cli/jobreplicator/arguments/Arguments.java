package dk.dbc.dataio.cli.jobreplicator.arguments;

import net.sourceforge.argparse4j.ArgumentParsers;
import net.sourceforge.argparse4j.helper.HelpScreenException;
import net.sourceforge.argparse4j.impl.action.AppendArgumentAction;
import net.sourceforge.argparse4j.inf.ArgumentParser;
import net.sourceforge.argparse4j.inf.ArgumentParserException;
import net.sourceforge.argparse4j.inf.Namespace;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Arguments {
    public long jobId;
    public String source, target, targetSinkName;
    public Map<String, String> overriddenSourceEndpoints,
            overriddenTargetEndpoints;
    public String mailAddressProcessing, mailAddressVerification;

    public void parseArgs(String[] args) throws ArgParseException {
        ArgumentParser parser = ArgumentParsers.newArgumentParser(
                        "dataio-cli-job-creator")
                .epilog("examples:\n\tjava -jar dataio-cli-job-replicator.jar" +
                        "2973273 --target-sink-name \"Hive sink\" -s" +
                        "\"http://dataio.dbc.dk\" -t \"http://dataio-staging.dbc.dk\"");
        parser.addArgument("jobId").help("id of job to re-create")
                .type(Long.class);
        parser.addArgument("--target-sink-name").help("name of target sink");
        parser.addArgument("-s", "--source");
        parser.addArgument("-t", "--target");
        parser.addArgument("--override-source-endpoint")
                .action(new AppendArgumentAction())
                .setDefault(new ArrayList<>())
                .metavar("key=value");
        parser.addArgument("--override-target-endpoint")
                .action(new AppendArgumentAction())
                .setDefault(new ArrayList<>())
                .metavar("key=value");
        parser.addArgument("--mail-address-processing")
                .help("mail address for notification about processing")
                .setDefault("");
        parser.addArgument("--mail-address-verification")
                .help("mail address for notification about verification")
                .setDefault("");

        Namespace ns;
        try {
            ns = parser.parseArgs(args);
        } catch (ArgumentParserException e) {
            parser.handleError(e);
            if (e instanceof HelpScreenException) System.exit(1);
            throw new ArgParseException(String.format(
                    "error parsing arguments: %s", e.toString()), e);
        }

        jobId = ns.getLong("jobId");
        source = ns.getString("source");
        target = ns.getString("target");
        targetSinkName = ns.getString("target_sink_name");
        mailAddressProcessing = ns.getString("mail_address_processing");
        mailAddressVerification = ns.getString("mail_address_verification");
        List<String> overriddenSourceEndpointsList = ns.getList(
                "override_source_endpoint");
        List<String> overriddenTargetEndpointsList = ns.getList(
                "override_target_endpoint");
        overriddenSourceEndpoints = new HashMap<>();
        overriddenTargetEndpoints = new HashMap<>();
        for (String endpoint : overriddenSourceEndpointsList) {
            ArgPair argPair = ArgPair.fromString(endpoint);
            overriddenSourceEndpoints.put(argPair.getKey(),
                    argPair.getValue());
        }
        for (String endpoint : overriddenTargetEndpointsList) {
            ArgPair argPair = ArgPair.fromString(endpoint);
            overriddenTargetEndpoints.put(argPair.getKey(),
                    argPair.getValue());
        }
    }
}
