/*
 * DataIO - Data IO
 * Copyright (C) 2015 Dansk Bibliotekscenter a/s, Tempovej 7-11, DK-2750 Ballerup,
 * Denmark. CVR: 15149043
 *
 * This file is part of DataIO.
 *
 * DataIO is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * DataIO is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with DataIO.  If not, see <http://www.gnu.org/licenses/>.
 */

package dk.dbc.dataio.test.jms.queue.ejb;

import dk.dbc.dataio.commons.utils.json.JsonException;
import dk.dbc.dataio.commons.utils.json.JsonUtil;
import dk.dbc.dataio.commons.utils.test.jms.MockedJmsTextMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Resource;
import javax.ejb.EJBException;
import javax.ejb.Stateless;
import javax.jms.ConnectionFactory;
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

    @Resource
    private ConnectionFactory messageQueueConnectionFactory;

    @GET
    @Path("{queueName}")
    @Produces({ MediaType.APPLICATION_JSON })
    public Response listQueue(@PathParam("queueName") String queueName) {
        LOGGER.info("Listing messages on queue {}", queueName);

        String listOfMessagesAsJson;
        try (JMSContext context = messageQueueConnectionFactory.createContext()) {
            try (final QueueBrowser browser = context.createBrowser(getQueueResource(queueName))) {
                final List<MockedJmsTextMessage> messages = new ArrayList<>();
                final Enumeration queue = browser.getEnumeration();
                while (queue.hasMoreElements()) {
                    messages.add(toMockedJmsTextMessage((Message) queue.nextElement()));
                }
                listOfMessagesAsJson = JsonUtil.toJson(messages);
                LOGGER.info("Content of queue {} <{}>", queueName, listOfMessagesAsJson);
            }

        } catch (JMSException | JsonException | NamingException e) {
            throw new EJBException(e);
        }
        return Response.ok().entity(listOfMessagesAsJson).build();
    }

    @POST
    @Path("{queueName}")
    @Consumes({ MediaType.APPLICATION_JSON })
    public Response putOnQueue(@PathParam("queueName") String queueName, String message) {
        LOGGER.info("Putting message on queue {} <{}>", queueName, message);

        try (JMSContext context = messageQueueConnectionFactory.createContext()) {
            MockedJmsTextMessage mockedJmsTextMessage = JsonUtil.fromJson(message, MockedJmsTextMessage.class);
            context.createProducer().send(getQueueResource(queueName), toTextMessage(mockedJmsTextMessage, context));
        } catch (JMSException | JsonException | NamingException e) {
            throw new EJBException(e);
        }
        return Response.ok().build();
    }

    @DELETE
    @Path("{queueName}")
    public Response emptyQueue(@PathParam("queueName") String queueName) {
        LOGGER.info("Emptying queue {}", queueName);

        int numDeleted = 0;
        try (JMSContext context = messageQueueConnectionFactory.createContext()) {
            try (final JMSConsumer consumer = context.createConsumer(getQueueResource(queueName))) {
                Message message;
                do {
                    message = consumer.receiveNoWait(); // todo: we should probably add an option to receive with timeout as well
                    if (message != null) {
                        message.acknowledge();
                        numDeleted++;
                    }
                } while (message != null);
            }
        } catch (JMSException | NamingException e) {
            throw new EJBException(e);
        }
        return Response.ok().entity(numDeleted).build();
    }

    @GET
    @Path("{queueName}/size")
    @Produces({ MediaType.TEXT_PLAIN })
    public Response getQueueSize(@PathParam("queueName") String queueName) {
        LOGGER.info("Getting size of queue {}", queueName);

        int queueSize = 0;
        try (JMSContext context = messageQueueConnectionFactory.createContext()) {
            try (final QueueBrowser browser = context.createBrowser(getQueueResource(queueName))) {
                final Enumeration messages = browser.getEnumeration();
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
        final Enumeration properties = message.getPropertyNames();
        while (properties.hasMoreElements()) {
            final String key = (String) properties.nextElement();
            mockedTextMessage.setStringProperty(key, message.getStringProperty(key));
        }
        return mockedTextMessage;
    }

    private TextMessage toTextMessage(MockedJmsTextMessage message, JMSContext context) throws JMSException {
        final TextMessage textMessage = context.createTextMessage(message.getText());
        for (Map.Entry<String, String> entry : message.getProperties().entrySet()) {
            textMessage.setStringProperty(entry.getKey(), entry.getValue());
        }
        return textMessage;
    }

    @SuppressWarnings("PMD.UnusedPrivateMethod")
    private static Queue getQueueResource(String resourceName) throws NamingException {
        Queue resourceValue;
        InitialContext initialContext = null;
        try {
            initialContext = new InitialContext();
            resourceValue = (Queue) initialContext.lookup(resourceName);
        } finally {
            closeInitialContext(initialContext);
        }
        return resourceValue;
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
