package dk.dbc.dataio.sink.dummy;

import dk.dbc.dataio.commons.types.ChunkItem;
import dk.dbc.dataio.commons.types.ChunkResult;
import dk.dbc.dataio.commons.types.SinkChunkResult;
import dk.dbc.dataio.commons.types.jms.JmsConstants;
import dk.dbc.dataio.commons.types.json.mixins.MixIns;
import dk.dbc.dataio.commons.utils.json.JsonException;
import dk.dbc.dataio.commons.utils.json.JsonUtil;
import java.util.ArrayList;
import java.util.List;
import javax.annotation.Resource;
import javax.ejb.EJB;
import javax.ejb.MessageDriven;
import javax.ejb.MessageDrivenContext;
import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.TextMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@MessageDriven
public class DummyMessageProcessorBean {

    private static final Logger LOGGER = LoggerFactory.getLogger(DummyMessageProcessorBean.class);

    private static final String SINK_CHUNK_RESULT_MESSAGE_PROPERTY_NAME = "chunkResultSource";
    private static final String SINK_CHUNK_RESULT_MESSAGE_PROPERTY_VALUE = "sink";

    @Resource
    MessageDrivenContext messageDrivenContext;

    @EJB
    TextMessageSenderBean textMessageSender;

    public void onMessage(Message message) {
        try {
            if (message instanceof TextMessage) {
                String messagePayload = ((TextMessage) message).getText();
                if (messagePayload == null) {
                    LOGGER.error("Message content was null");
                    return;
                }
                if (messagePayload.isEmpty()) {
                    LOGGER.error("Message conent was empty");
                    return;
                }
                ChunkResult chunkResult = JsonUtil.fromJson(messagePayload, ChunkResult.class, MixIns.getMixIns());
                List<ChunkItem> sinkItems = new ArrayList<>(chunkResult.getItems().size());
                for (ChunkItem item : chunkResult.getItems()) {
                    // Set new-item-status to success if chunkResult-item was success - else set new-item-status to ignore:
                    ChunkItem.Status status = item.getStatus() == ChunkItem.Status.SUCCESS ? ChunkItem.Status.SUCCESS : ChunkItem.Status.IGNORE;
                    sinkItems.add(new ChunkItem(item.getId(), "Set by DummySink", status));

                }
                SinkChunkResult sinkChunkResult = new SinkChunkResult(chunkResult.getJobId(), chunkResult.getChunkId(), chunkResult.getEncoding(), sinkItems);

                List<TextMessageSenderBean.StringProperty> properties = new ArrayList<>();
                properties.add(new TextMessageSenderBean.StringProperty(JmsConstants.PAYLOAD_PROPERTY_NAME, JmsConstants.SINK_RESULT_PAYLOAD_TYPE));
                properties.add(new TextMessageSenderBean.StringProperty(SINK_CHUNK_RESULT_MESSAGE_PROPERTY_NAME, SINK_CHUNK_RESULT_MESSAGE_PROPERTY_VALUE));
                String jsonMessage = JsonUtil.toJson(sinkChunkResult);
                LOGGER.info("Adding dummy message to queue: {}", jsonMessage);
                textMessageSender.send(jsonMessage, properties);
            } else {
                LOGGER.error("Invalid message type: {}", message);
            }
        } catch (JMSException | JsonException ex) {
            LOGGER.error("Exception caught while processing message.", ex);
        }
    }
}
