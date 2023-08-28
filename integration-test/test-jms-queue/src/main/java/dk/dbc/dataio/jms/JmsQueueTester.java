package dk.dbc.dataio.jms;

import dk.dbc.dataio.commons.utils.test.jms.MockedJmsTextMessage;
import jakarta.jms.ConnectionFactory;
import jakarta.jms.JMSConsumer;
import jakarta.jms.JMSContext;
import jakarta.jms.JMSException;
import jakarta.jms.JMSProducer;
import jakarta.jms.Message;
import jakarta.jms.QueueBrowser;
import jakarta.jms.TextMessage;
import org.apache.activemq.artemis.jms.client.ActiveMQConnectionFactory;

import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.Map;

public class JmsQueueTester {
    private static final long SLEEP_INTERVAL_IN_MS = 250;
    private final ConnectionFactory connectionFactory;

    public JmsQueueTester(String host) {
        connectionFactory = new ActiveMQConnectionFactory("tcp://" + host);
    }

    public List<MockedJmsTextMessage> listQueue(Queue queue) {
        try(JMSContext context = connectionFactory.createContext()) {
            QueueBrowser browser = context.createBrowser(context.createQueue(queue.queueName));
            @SuppressWarnings("unchecked")
            Enumeration<Message> enumeration = browser.getEnumeration();
            List<MockedJmsTextMessage> messages = new ArrayList<>();
            while (enumeration.hasMoreElements()) {
                Message message = enumeration.nextElement();
                messages.add(toMockedJmsTextMessage(message));
            }
            return messages;
        } catch (JMSException e) {
            throw new RuntimeException(e);
        }
    }

    public void putOnQueue(Queue queue, MockedJmsTextMessage message) {
        try(JMSContext context = connectionFactory.createContext()) {
            JMSProducer producer = context.createProducer();
            producer.send(context.createQueue(queue.queueName), toTextMessage(message, context));
        } catch (JMSException e) {
            throw new RuntimeException(e);
        }
    }

    public int emptyQueue(Queue queue) {
        try(JMSContext context = connectionFactory.createContext();
            JMSConsumer consumer = context.createConsumer(context.createQueue(queue.queueName))) {
            Message message;
            int count = 0;
            do {
                message = consumer.receive(100);
                if(message != null) count++;
            } while (message != null);
            return count;
        }
    }

    public int getQueueSize(Queue queue) {
        try(JMSContext context = connectionFactory.createContext();
            QueueBrowser browser = context.createBrowser(context.createQueue(queue.queueName))) {
            Enumeration<?> enumeration = browser.getEnumeration();
            int count = 0;
            while (enumeration.hasMoreElements()) {
                count++;
                enumeration.nextElement();
            }
            return count;
        } catch (JMSException e) {
            throw new RuntimeException(e);
        }
    }

    public void awaitQueueSize(Queue queue, int expectedQueueSize, long maxWaitInMs) {
        long remainingWaitInMs = maxWaitInMs;
        int actualQueueSize = getQueueSize(queue);
        while (actualQueueSize != expectedQueueSize && remainingWaitInMs > 0) {
            try {
                Thread.sleep(SLEEP_INTERVAL_IN_MS);
                remainingWaitInMs -= SLEEP_INTERVAL_IN_MS;
                actualQueueSize = getQueueSize(queue);
            } catch (InterruptedException e) {
                break;
            }
        }
        if (actualQueueSize != expectedQueueSize) {
            throw new IllegalStateException(String.format("Expected size %d of queue %s differs from actual size %d",
                    expectedQueueSize, queue.getQueueName(), actualQueueSize));
        }
    }

    public List<MockedJmsTextMessage> awaitQueueSizeAndList(
            Queue queue, int expectedQueueSize, long maxWaitInMs) {
        awaitQueueSize(queue, expectedQueueSize, maxWaitInMs);
        return listQueue(queue);
    }

    private MockedJmsTextMessage toMockedJmsTextMessage(Message message) throws JMSException {
        TextMessage textMessage = (TextMessage)message;
        MockedJmsTextMessage mockedTextMessage = new MockedJmsTextMessage();
        mockedTextMessage.setJMSMessageID(textMessage.getJMSMessageID());
        mockedTextMessage.setText(textMessage.getText());
        Enumeration<?> properties = message.getPropertyNames();

        while(properties.hasMoreElements()) {
            String key = (String)properties.nextElement();
            mockedTextMessage.setStringProperty(key, message.getStringProperty(key));
        }

        return mockedTextMessage;
    }

    private TextMessage toTextMessage(MockedJmsTextMessage message, JMSContext context) throws JMSException {
        TextMessage textMessage = context.createTextMessage(message.getText());
        for (Map.Entry<String, Object> e : message.getProperties().entrySet()) {
            textMessage.setStringProperty(e.getKey(), e.getValue().toString());
        }
        return textMessage;
    }


    public enum Queue {
        PROCESSING_BUSINESS("processor::business"),
        PROCESSING_ACCTEST("processor::acctest"),
        SINK_BE_CISTERNE("sink::batch-exchange/cisterne"),
        SINK_PERIODIC_JOBS("sink::periodic-jobs");

        private final String queueName;

        Queue(String queueName) {
            this.queueName = queueName;
        }

        public String getQueueName() {
            return queueName;
        }
    }
}
