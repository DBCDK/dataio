/*
 * Copyright Dansk Bibliotekscenter a/s. Licensed under GNU GPLv3
 * See license text in LICENSE.txt
 */

package dk.dbc.dataio.cli;

import dk.dbc.dataio.urlresolver.service.connector.UrlResolverServiceConnector;
import dk.dbc.dataio.urlresolver.service.connector.UrlResolverServiceConnectorException;
import dk.dbc.httpclient.HttpClient;
import net.sourceforge.argparse4j.helper.HelpScreenException;
import net.sourceforge.argparse4j.inf.ArgumentParserException;
import net.sourceforge.argparse4j.inf.Namespace;
import org.glassfish.jersey.client.ClientConfig;
import org.glassfish.jersey.jackson.JacksonFeature;

import javax.ws.rs.client.Client;
import java.util.Map;

public class DatafileExporter {
    private final Namespace args;
    private Map<String, String> endpoints;

    public static void main(String[] args) {
        final Cli cli;
        try {
            cli = new Cli(args);
            final DatafileExporter datafileExporter = new DatafileExporter(cli);
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

    public DatafileExporter(Cli cli) {
        args = cli.args;
        endpoints = getEndpoints();
    }

    protected Map<String, String> getEndpoints() {
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
}
