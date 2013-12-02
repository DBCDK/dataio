package dk.dbc.dataio.sink.es.queue.ejb;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Resource;
import javax.ejb.EJBException;
import javax.ejb.Stateless;
import javax.jms.ConnectionFactory;
import javax.jms.JMSContext;
import javax.jms.JMSException;
import javax.jms.Queue;
import javax.jms.QueueBrowser;
import javax.jms.TextMessage;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.Enumeration;

@Stateless
@Path("queue")
public class QueueBean {
    private static final Logger LOGGER = LoggerFactory.getLogger(QueueBean.class);

    private static final String QUEUE_JNDI_NAME = "jms/dataio/sinks";
    private static final String RESOURCE_TYPE = "jdbc/dataio/es";

    @Resource
    private ConnectionFactory messageQueueConnectionFactory;

    @Resource(mappedName = QUEUE_JNDI_NAME)
    private Queue messageQueue;

    @POST
    @Consumes({ MediaType.APPLICATION_JSON })
    public Response putOnQueue(String content) {
        LOGGER.info("Putting on queue: '{}'", content);

        try (JMSContext context = messageQueueConnectionFactory.createContext()) {
            final TextMessage message = context.createTextMessage(content);
            message.setStringProperty("resource", RESOURCE_TYPE);
            context.createProducer().send(messageQueue, message);
        } catch (JMSException e) {
            throw new EJBException(e);
        }

        return Response.ok().build();
    }

    @GET
    @Produces({ MediaType.TEXT_PLAIN })
    public Response getQueueSize() {
        LOGGER.info("Getting queue size");
        int queueSize = 0;

        try (JMSContext context = messageQueueConnectionFactory.createContext()) {
            final QueueBrowser browser = context.createBrowser(messageQueue);
            final Enumeration messages = browser.getEnumeration();
            while (messages.hasMoreElements()) {
                queueSize++;
                messages.nextElement();
            }
        } catch (JMSException e) {
            throw new EJBException(e);
        }

        return Response.ok().entity(queueSize).build();
    }

}
