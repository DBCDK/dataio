package dk.dbc.dataio.commons.creatordetector.connector;

import dk.dbc.commons.useragent.UserAgent;
import dk.dbc.httpclient.HttpClient;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Produces;
import jakarta.inject.Inject;
import jakarta.ws.rs.client.Client;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.glassfish.jersey.client.ClientConfig;
import org.glassfish.jersey.jackson.JacksonFeature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * CreatorDetectorConnectorFactory
 * <p>
 * Synopsis:
 * </p>
 * <pre>
 *    // New instance
 *    CreatorDetectorConnector connector = CreatorDetectorConnectorFactory.create("http://creator-detector");
 *
 *    // Singleton instance in CDI enabled environment
 *    {@literal @}Inject
 *    CreatorDetectorConnectorFactory factory;
 *    ...
 *    CreatorDetectorConnector connector = factory.getInstance();
 *
 *    // or simply
 *    {@literal @}Inject
 *    CreatorDetectorConnector connector;
 * </pre>
 * <p>
 * The CDI case depends on the creator-detector service base-url being defined as
 * the value of either a system property or environment variable
 * named CREATOR_DETECTOR_SERVICE_URL.
 * </p>
 */
@ApplicationScoped
public class CreatorDetectorConnectorFactory {

    private static final Logger LOGGER = LoggerFactory.getLogger(CreatorDetectorConnectorFactory.class);

    public static CreatorDetectorConnector create(String baseUrl) {
        final Client client = HttpClient.newClient(new ClientConfig().register(new JacksonFeature()));
        LOGGER.info("Creating CreatorDetectorConnector for: {}", baseUrl);
        return new CreatorDetectorConnector(client, UserAgent.forInternalRequests(), baseUrl);
    }

    @Inject
    @ConfigProperty(name = "CREATOR_DETECTOR_SERVICE_URL")
    private String baseUrl;

    CreatorDetectorConnector creatorDetectorConnector;

    @PostConstruct
    public void initializeConnector() {
        creatorDetectorConnector = CreatorDetectorConnectorFactory.create(baseUrl);
    }

    @Produces
    public CreatorDetectorConnector getInstance() {
        return creatorDetectorConnector;
    }

    @PreDestroy
    public void tearDownConnector() {
        creatorDetectorConnector.close();
    }
}
