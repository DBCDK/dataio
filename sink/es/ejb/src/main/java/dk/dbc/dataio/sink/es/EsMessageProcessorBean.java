package dk.dbc.dataio.sink.es;

import dk.dbc.dataio.sink.InvalidMessageSinkException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Resource;
import javax.ejb.EJB;
import javax.ejb.MessageDriven;
import javax.ejb.MessageDrivenContext;
import javax.jms.Message;

@MessageDriven
public class EsMessageProcessorBean {
    private static final Logger LOGGER = LoggerFactory.getLogger(EsMessageProcessorBean.class);

    @Resource
    MessageDrivenContext messageDrivenContext;

    @EJB
    EsThrottlerBean esThrottler;

    public void onMessage(Message message) {
        LOGGER.trace("Message received: <{}>", message);
        try {
            validateMessage(message);
            processMessage(message);
        } catch (InterruptedException e) {
            // Ensure that this container-managed transaction can never commit
            // and therefore that this message subsequently will be re-delivered.
            messageDrivenContext.setRollbackOnly();
            LOGGER.error("Exception caught while processing message", e);
        } catch (InvalidMessageSinkException e) {
            LOGGER.error("Message <{}> rejected", e);
        }
    }

    /* To prevent message poisoning where invalid messages will be re-delivered
       forever, all messages must be validated */
    private void validateMessage(Message message) throws InvalidMessageSinkException {
        if (message == null) {
            throw new InvalidMessageSinkException("Message can not be null");
        }
    }

    private void processMessage(Message message) throws InterruptedException {
        // ToDo: Number of record slots must be read from message.
        esThrottler.acquireRecordSlots(1);

        LOGGER.info("Simulating ES TP push for message {}", message);
    }
}
