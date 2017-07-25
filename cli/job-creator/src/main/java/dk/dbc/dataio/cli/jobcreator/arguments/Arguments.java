package dk.dbc.dataio.cli.jobcreator.arguments;

import net.sourceforge.argparse4j.ArgumentParsers;
import net.sourceforge.argparse4j.impl.action.AppendArgumentAction;
import net.sourceforge.argparse4j.inf.ArgumentParser;
import net.sourceforge.argparse4j.inf.ArgumentParserException;
import net.sourceforge.argparse4j.inf.Namespace;
import net.sourceforge.argparse4j.internal.HelpScreenException;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Arguments {
    public long jobId;
    public String source, target;
    public Map<String, String> overriddenSourceEndpoints,
        overriddenTargetEndpoints;

    public void parseArgs(String[] args) throws ArgParseException {
        ArgumentParser parser = ArgumentParsers.newArgumentParser(
            "dataio-cli-job-creator");
        parser.addArgument("jobId").help("id of job to re-create")
            .type(Long.class);
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

        Namespace ns;
        try {
            ns = parser.parseArgs(args);
        } catch(ArgumentParserException e) {
            parser.handleError(e);
            if(e instanceof HelpScreenException) System.exit(1);
            throw new ArgParseException(String.format(
                "error parsing arguments: %s", e.toString()), e);
        }

        jobId = ns.getLong("jobId");
        source = ns.getString("source");
        target = ns.getString("target");
        List<String> overriddenSourceEndpointsList = ns.getList(
            "override_source_endpoint");
        List<String> overriddenTargetEndpointsList = ns.getList(
            "override_target_endpoint");
        overriddenSourceEndpoints = new HashMap<>();
        overriddenTargetEndpoints = new HashMap<>();
        for(String endpoint : overriddenSourceEndpointsList) {
            ArgPair argPair = ArgPair.fromString(endpoint);
            overriddenSourceEndpoints.put(argPair.getKey(),
                argPair.getValue());
        }
        for(String endpoint : overriddenTargetEndpointsList) {
            ArgPair argPair = ArgPair.fromString(endpoint);
            overriddenTargetEndpoints.put(argPair.getKey(),
                argPair.getValue());
        }
    }
}
