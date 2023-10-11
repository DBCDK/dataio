package dk.dbc.dataio.cli;

import dk.dbc.dataio.urlresolver.service.connector.UrlResolverServiceConnector;
import dk.dbc.dataio.urlresolver.service.connector.UrlResolverServiceConnectorException;
import dk.dbc.httpclient.HttpClient;
import jakarta.ws.rs.client.Client;
import net.sourceforge.argparse4j.helper.HelpScreenException;
import net.sourceforge.argparse4j.inf.ArgumentParserException;
import net.sourceforge.argparse4j.inf.Namespace;
import org.glassfish.jersey.client.ClientConfig;
import org.glassfish.jersey.jackson.JacksonFeature;

import java.nio.file.Paths;
import java.util.Map;
import java.util.Scanner;

public class DatafileExporter {
    private static final int DOWNLOAD_SIZE_PROMPT_THRESHOLD = 1000; // MB

    private final Namespace args;
    private final Map<String, String> endpoints;

    public static void main(String[] args) {
        try {
            final Cli cli = new Cli(args);
            new DatafileExporter(cli).run();
        } catch (HelpScreenException e) {
            System.exit(0);
        } catch (ArgumentParserException e) {
            System.exit(1);
        } catch (RuntimeException e) {
            System.err.println(String.format("Unexpected error: %s",
                    e.toString()));
            e.printStackTrace();
            System.exit(1);
        }
    }

    DatafileExporter(Cli cli) {
        args = cli.args;
        endpoints = getEndpoints();
    }

    void run() {
        final JobStoreSearcher jobStoreSearcher = new JobStoreSearcher(endpoints.get("JOBSTORE_URL"));
        final Map<String, Datafile> datafiles = jobStoreSearcher.findDatafiles(args);

        final FileStoreFetcher fileStoreFetcher = new FileStoreFetcher(endpoints.get("FILESTORE_URL"));
        System.out.println("Calculating total download size...");
        final long size = fileStoreFetcher.getDownloadSizeMB(datafiles.values());
        System.out.println("Download size: " + size + " MB");
        if (size > DOWNLOAD_SIZE_PROMPT_THRESHOLD
                && !proceedPrompt("Download size exceeds " +
                DOWNLOAD_SIZE_PROMPT_THRESHOLD + " MB, continue? [yes/no]: ")) {
            return;
        }
        for (Datafile datafile : datafiles.values()) {
            System.out.println("Downloading file: " + datafile.getFileId() + " from jobs: " + datafile.getJobs());
            fileStoreFetcher.downloadFile(datafile, Paths.get(args.getString("outdir")));
        }
    }

    private Map<String, String> getEndpoints() {
        final String target = args.getString("target");
        System.out.println("Using target: " + target);
        final Client client = HttpClient.newClient(new ClientConfig()
                .register(new JacksonFeature()));
        final UrlResolverServiceConnector urlResolverServiceConnector =
                new UrlResolverServiceConnector(client, target);
        try {
            return urlResolverServiceConnector.getUrls();
        } catch (UrlResolverServiceConnectorException e) {
            throw new CliException("Unable to retrieve target endpoint", e);
        }
    }

    private boolean proceedPrompt(String promptMessage) {
        System.out.print(promptMessage);
        final Scanner scanner = new Scanner(System.in);
        final String userInput = scanner.next();
        return userInput.equalsIgnoreCase("y") || userInput.equalsIgnoreCase("yes");
    }
}
