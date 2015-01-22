package dk.dbc.dataio.commons.utils.service;

import dk.dbc.dataio.commons.types.ConsumedMessage;
import dk.dbc.dataio.commons.types.ExternalChunk;
import dk.dbc.dataio.commons.types.exceptions.InvalidMessageException;
import dk.dbc.dataio.commons.types.json.mixins.MixIns;
import dk.dbc.dataio.commons.utils.json.JsonException;
import dk.dbc.dataio.commons.utils.json.JsonUtil;

public abstract class AbstractSinkMessageConsumerBean extends AbstractMessageConsumerBean {
    private static final String PAYLOAD_TYPE = "ChunkResult";

    /**
     * Unmarshalls payload from given consumed message into ChunkResult
     * @param consumedMessage consumed message
     * @return consumed message payload as ChunkResult
     * @throws NullPointerException if given null-valued consumedMessage
     * @throws InvalidMessageException if message payload type differs from {@value #PAYLOAD_TYPE},
     * if message payload can not be unmarshalled to ChunkResult, or if resulting ChunkResult contains no items.
     */
    protected ExternalChunk unmarshallPayload(ConsumedMessage consumedMessage)
            throws NullPointerException, InvalidMessageException {
        if (!PAYLOAD_TYPE.equals(consumedMessage.getPayloadType())) {
            throw new InvalidMessageException(String.format("Message<%s> payload type %s != %s",
                    consumedMessage.getMessageId(), consumedMessage.getPayloadType(), PAYLOAD_TYPE));
        }
        try {
            ExternalChunk processedChunk= JsonUtil.fromJson(consumedMessage.getMessagePayload(), ExternalChunk.class, MixIns.getMixIns());
            if (processedChunk.isEmpty()) {
                throw new InvalidMessageException(String.format("Message<%s> ChunkResult payload contains no results",
                        consumedMessage.getMessageId()));
            }
            return processedChunk;
        } catch (JsonException e) {
            throw new InvalidMessageException(String.format("Message<%s> payload was not valid ChunkResult type",
                    consumedMessage.getMessageId()), e);
        }
    }
}
