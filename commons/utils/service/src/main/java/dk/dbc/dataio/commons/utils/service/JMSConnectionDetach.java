package dk.dbc.dataio.commons.utils.service;

import dk.dbc.jms.artemis.AdminClient;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.PostConstruct;
import javax.ejb.Stateless;
import javax.inject.Inject;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Response;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.stream.Collectors;

@Stateless
@Path("/")
public class JMSConnectionDetach {
    private static final Logger LOGGER = LoggerFactory.getLogger(JMSConnectionDetach.class);
    @Inject
    @ConfigProperty(name = "ARTEMIS_MQ_HOST")
    private String artemisHost;

    @Inject
    @ConfigProperty(name = "ARTEMIS_ADMIN_PORT")
    private Integer artemisPort;

    @Inject
    @ConfigProperty(name = "ARTEMIS_USER")
    private String artemisUser;
    @Inject
    @ConfigProperty(name = "ARTEMIS_PASSWORD")
    private String artemisPassword;
    private AdminClient adminClient;

    @PostConstruct
    public void init() {
        adminClient = artemisPort == null ? null : new AdminClient("http://" + artemisHost + ":" + artemisPort, artemisUser, artemisPassword);
    }

    @POST
    @Path("maintenance/JMS/connection/reset/{queueName}")
    public Response resetJMSConnections(@PathParam("queueName") String queueName, @QueryParam("sinkname") String sinkName) {
        LOGGER.info("Cleaning up JMS connections from queue:{}", queueName);
        LOGGER.info("Filter:{}", sinkName == null ? "(No filter)" : String.format("resource = '%s'", sinkName));
        Instant nowMinus15 = Instant.now().minus(Duration.ofMinutes(15));
        List<AdminClient.Consumer> consumers = adminClient.listConsumers(
                c -> c.getQueueName().equals(queueName) && nowMinus15.isAfter(c.getLastAcknowledgedTime()));
        List<AdminClient.Consumer> filteredConsumers = sinkName == null ? consumers : consumers.stream()
                .filter(consumer -> String.format("resource = '%s'", sinkName).equals(consumer.getFilter())).collect(Collectors.toList());
        LOGGER.info("Disconnecting: {} consumers", filteredConsumers.size());
        for (AdminClient.Consumer c : filteredConsumers) {
            adminClient.closeConsumerConnection(c.getConnectionID());
            LOGGER.info("Disconnecting consumer with id:{} from queue: {}", c.getConnectionID(), c.getQueueName());
        }
        return Response.ok().build();
    }
}
