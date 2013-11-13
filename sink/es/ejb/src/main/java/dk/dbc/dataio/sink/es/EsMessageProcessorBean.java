package dk.dbc.dataio.sink.es;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ejb.MessageDriven;
import javax.jms.Message;

@MessageDriven
public class EsMessageProcessorBean {
    private static final Logger LOGGER = LoggerFactory.getLogger(EsMessageProcessorBean.class);

    public void onMessage(Message message) {
        LOGGER.trace("Message received: <{}>", message.toString());
    }
}
