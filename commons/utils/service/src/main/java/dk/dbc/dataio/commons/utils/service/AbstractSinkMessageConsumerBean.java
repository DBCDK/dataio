package dk.dbc.dataio.commons.utils.service;

import dk.dbc.dataio.commons.types.ConsumedMessage;
import dk.dbc.dataio.commons.types.ExternalChunk;
import dk.dbc.dataio.commons.types.exceptions.InvalidMessageException;
import dk.dbc.dataio.commons.types.jms.JmsConstants;
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
     * if message payload can not be unmarshalled, or if resulting chunk contains no items.
     */
    protected ExternalChunk unmarshallPayload(ConsumedMessage consumedMessage)
            throws NullPointerException, InvalidMessageException {
        if (!JmsConstants.CHUNK_PAYLOAD_TYPE.equals(consumedMessage.getPayloadType())) {
            throw new InvalidMessageException(String.format("Message<%s> payload type %s != %s",
                    consumedMessage.getMessageId(), consumedMessage.getPayloadType(), PAYLOAD_TYPE));
        }
        ExternalChunk processedChunk;
        try {
            processedChunk= JsonUtil.fromJson(consumedMessage.getMessagePayload(), ExternalChunk.class);
        } catch (JsonException e) {
            throw new InvalidMessageException(String.format("Message<%s> payload was not valid ExternalChunk type",
                    consumedMessage.getMessageId()), e);
        }
        if (processedChunk.isEmpty()) {
            throw new InvalidMessageException(String.format("Message<%s> processed chunk payload contains no results",
                    consumedMessage.getMessageId()));
        }
        confirmLegalChunkTypeOrThrow(processedChunk, ExternalChunk.Type.PROCESSED);
        return processedChunk;
    }
}
