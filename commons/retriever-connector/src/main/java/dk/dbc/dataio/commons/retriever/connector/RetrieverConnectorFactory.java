package dk.dbc.dataio.commons.retriever.connector;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import dk.dbc.httpclient.HttpClient;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Produces;
import jakarta.inject.Inject;
import jakarta.ws.rs.client.Client;
import jakarta.ws.rs.ext.ContextResolver;
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
 *    RetrieverConnector connector = RetrieverConnectorFactory.create("https://retriever", "my-api-key");
 *
 *    // Singleton instance in CDI enabled environment
 *    {@literal @}Inject
 *    RetrieverConnectorFactory factory;
 *    ...
 *    RetrieverConnector connector = factory.getInstance();
 *
 *    // or simply
 *    {@literal @}Inject
 *    RetrieverConnector connector;
 * </pre>
 * <p>
 * The CDI case depends on the retriever service base-url and api-key being defined as
 * the value of either a system property or environment variable
 * named RETRIEVER_SERVICE_URL and RETRIEVER_SERVICE_API_KEY respectively.
 * </p>
 */
@ApplicationScoped
public class RetrieverConnectorFactory {

    private static final Logger LOGGER = LoggerFactory.getLogger(RetrieverConnectorFactory.class);

    private static class ObjectMapperProvider implements ContextResolver<ObjectMapper> {
        private final ObjectMapper objectMapper;

        public ObjectMapperProvider() {
            this.objectMapper = new ObjectMapper()
                    .registerModule(new JavaTimeModule())
                    .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
                    .enable(SerializationFeature.INDENT_OUTPUT);
        }

        @Override
        public ObjectMapper getContext(Class<?> type) {
            return objectMapper;
        }
    }

    public static RetrieverConnector create(String baseUrl, String apiKey) {
        final Client client = HttpClient.newClient(new ClientConfig()
                .register(new ObjectMapperProvider())
                .register(new JacksonFeature()));
        LOGGER.info("Creating CreatorDetectorConnector for: {}", baseUrl);
        return new RetrieverConnector(client, baseUrl, apiKey);
    }

    @Inject
    @ConfigProperty(name = "RETRIEVER_SERVICE_URL")
    private String baseUrl;

    @Inject
    @ConfigProperty(name = "RETRIEVER_SERVICE_API_KEY")
    private String apiKey;

    RetrieverConnector retrieverConnector;

    @PostConstruct
    public void initializeConnector() {
        retrieverConnector = RetrieverConnectorFactory.create(baseUrl, apiKey);
    }

    @Produces
    public RetrieverConnector getInstance() {
        return retrieverConnector;
    }

    @PreDestroy
    public void tearDownConnector() {
        retrieverConnector.close();
    }
}
