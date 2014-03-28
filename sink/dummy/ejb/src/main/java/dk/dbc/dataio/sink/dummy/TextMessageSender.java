package dk.dbc.dataio.sink.dummy;

import java.util.List;
import javax.annotation.Resource;
import javax.ejb.EJBException;
import javax.ejb.Stateless;
import javax.jms.ConnectionFactory;
import javax.jms.JMSContext;
import javax.jms.JMSException;
import javax.jms.Queue;
import javax.jms.TextMessage;

@Stateless
public class TextMessageSender {

    @Resource
    ConnectionFactory connectionFactory;

    @Resource(name = "jobProcessorJmsQueue") // this resource gets its jndi name mapping from xml-deploy-descriptors
    Queue queue;

    public void send(String messagePayload, List<StringProperty> properties) {
        try (final JMSContext context = connectionFactory.createContext()) {
            final TextMessage message = context.createTextMessage(messagePayload);
            for (StringProperty property : properties) {
                message.setStringProperty(property.name, property.value);
            }
            context.createProducer().send(queue, message);
        } catch (JMSException ex) {
            throw new EJBException(ex);
        }
    }

    public static class StringProperty {

        public final String name;
        public final String value;

        public StringProperty(String name, String value) {
            this.name = name;
            this.value = value;
        }
    }
}
