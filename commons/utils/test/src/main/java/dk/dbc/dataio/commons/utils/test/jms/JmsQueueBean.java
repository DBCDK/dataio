package dk.dbc.dataio.commons.utils.test.jms;

import dk.dbc.commons.jsonb.JSONBContext;
import dk.dbc.commons.jsonb.JSONBException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ejb.EJBException;
import javax.ejb.Stateless;
import javax.inject.Inject;
import javax.jms.JMSConnectionFactory;
import javax.jms.JMSConsumer;
import javax.jms.JMSContext;
import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.Queue;
import javax.jms.QueueBrowser;
import javax.jms.TextMessage;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.Map;

@Stateless
@Path("queue")
public class JmsQueueBean {
    private static final Logger LOGGER = LoggerFactory.getLogger(JmsQueueBean.class);

    JSONBContext jsonbContext = new JSONBContext();
    @Inject
    @JMSConnectionFactory("jms/artemisConnectionFactory")
    JMSContext context;

    @GET
    @Path("{queueName}")
    @Produces({MediaType.APPLICATION_JSON})
    public Response listQueue(@PathParam("queueName") String queueName) {
        LOGGER.info("Listing messages on queue {}", queueName);

        String listOfMessagesAsJson;
        try {
            try (QueueBrowser browser = context.createBrowser(getQueueResource(queueName))) {
                final List<MockedJmsTextMessage> messages = new ArrayList<>();
                final Enumeration<?> queue = browser.getEnumeration();
                while (queue.hasMoreElements()) {
                    messages.add(toMockedJmsTextMessage((Message) queue.nextElement()));
                }
                listOfMessagesAsJson = jsonbContext.marshall(messages);
                LOGGER.info("Content of queue {} <{}>", queueName, listOfMessagesAsJson);
            }

        } catch (JMSException | JSONBException | NamingException e) {
            throw new EJBException(e);
        }
        return Response.ok().entity(listOfMessagesAsJson).build();
    }

    @POST
    @Path("{queueName}")
    @Consumes({MediaType.APPLICATION_JSON})
    public Response putOnQueue(@PathParam("queueName") String queueName, String message) {
        LOGGER.info("Putting message on queue {} <{}>", queueName, message);

        try {
            MockedJmsTextMessage mockedJmsTextMessage = jsonbContext.unmarshall(message, MockedJmsTextMessage.class);
            context.createProducer().send(getQueueResource(queueName), toTextMessage(mockedJmsTextMessage, context));
        } catch (JMSException | JSONBException | NamingException e) {
            throw new EJBException(e);
        }
        return Response.ok().build();
    }

    @DELETE
    @Path("{queueName}")
    public Response emptyQueue(@PathParam("queueName") String queueName) {
        LOGGER.info("Emptying queue {}", queueName);

        int numDeleted = 0;
        try {
            numDeleted = emptyQueue(getQueueResource(queueName));
        } catch (NamingException e) {
            e.printStackTrace();
        }
        return Response.ok().entity(numDeleted).build();
    }

    public int emptyQueue(Queue queue) {
        int numDeleted = 0;
        try (JMSConsumer consumer = context.createConsumer(queue)) {
            Message message;
            do {
                message = consumer.receive(1000);
                if (message != null) {
                    numDeleted++;
                }
            } while (message != null);
        }
        return numDeleted;
    }

    @GET
    @Path("{queueName}/size")
    @Produces({MediaType.TEXT_PLAIN})
    public Response getQueueSize(@PathParam("queueName") String queueName) {
        LOGGER.info("Getting size of queue {}", queueName);

        int queueSize = 0;
        try {
            try (QueueBrowser browser = context.createBrowser(getQueueResource(queueName))) {
                final Enumeration<?> messages = browser.getEnumeration();
                while (messages.hasMoreElements()) {
                    queueSize++;
                    messages.nextElement();
                }
            }
        } catch (JMSException | NamingException e) {
            throw new EJBException(e);
        }
        return Response.ok().entity(queueSize).build();
    }

    private MockedJmsTextMessage toMockedJmsTextMessage(Message message) throws JMSException {
        final TextMessage textMessage = (TextMessage) message;
        final MockedJmsTextMessage mockedTextMessage = new MockedJmsTextMessage();
        mockedTextMessage.setJMSMessageID(textMessage.getJMSMessageID());
        mockedTextMessage.setText(textMessage.getText());
        final Enumeration<?> properties = message.getPropertyNames();
        while (properties.hasMoreElements()) {
            final String key = (String) properties.nextElement();
            mockedTextMessage.setStringProperty(key, message.getStringProperty(key));
        }
        return mockedTextMessage;
    }

    private TextMessage toTextMessage(MockedJmsTextMessage message, JMSContext context) throws JMSException {
        final TextMessage textMessage = context.createTextMessage(message.getText());
        for (Map.Entry<String, Object> entry : message.getProperties().entrySet()) {
            textMessage.setStringProperty(entry.getKey(), entry.getValue().toString());
        }
        return textMessage;
    }

    @SuppressWarnings("PMD.UnusedPrivateMethod")
    private Queue getQueueResource(String resourceName) throws NamingException {
        if(resourceName.contains("::")) {
            return context.createQueue(resourceName);
        }
        InitialContext initialContext = null;
        try {
            initialContext = new InitialContext();
            return  (Queue) initialContext.lookup(resourceName);
        } finally {
            closeInitialContext(initialContext);
        }
    }

    private static void closeInitialContext(InitialContext initialContext) {
        if (initialContext != null) {
            try {
                initialContext.close();
            } catch (NamingException e) {
                LOGGER.warn("Unable to close initial context", e);
            }
        }
    }
}
