package dk.dbc.dataio.sink.testutil;

import dk.dbc.commons.jsonb.JSONBContext;
import dk.dbc.commons.jsonb.JSONBException;
import dk.dbc.dataio.commons.types.Chunk;
import dk.dbc.dataio.commons.types.ConsumedMessage;
import dk.dbc.dataio.commons.types.Priority;
import dk.dbc.dataio.commons.types.jms.JmsConstants;

import java.util.HashMap;
import java.util.Map;

public class ObjectFactory {
    private static final JSONBContext JSONB_CONTEXT = new JSONBContext();

    private ObjectFactory() {}

    public static ConsumedMessage createConsumedMessage(Chunk chunk) {
        try {
            final Map<String, Object> headers = new HashMap<>();
            headers.put(JmsConstants.PAYLOAD_PROPERTY_NAME, JmsConstants.CHUNK_PAYLOAD_TYPE);
            return new ConsumedMessage("messageId", headers, JSONB_CONTEXT.marshall(chunk));
        } catch (JSONBException e) {
            throw new IllegalStateException(e);
        }
    }

    public static ConsumedMessage createConsumedMessage(Chunk chunk, Priority priority) {
        try {
            final Map<String, Object> headers = new HashMap<>();
            headers.put(JmsConstants.PAYLOAD_PROPERTY_NAME, JmsConstants.CHUNK_PAYLOAD_TYPE);
            return new ConsumedMessage("messageId", headers,
                    JSONB_CONTEXT.marshall(chunk), priority);
        } catch (JSONBException e) {
            throw new IllegalStateException(e);
        }
    }
}
