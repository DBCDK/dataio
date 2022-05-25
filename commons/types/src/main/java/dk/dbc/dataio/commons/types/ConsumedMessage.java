package dk.dbc.dataio.commons.types;

import java.util.Map;

public class ConsumedMessage {
    private final String messageId;
    private final Map<String, Object> headers;
    private final String messagePayload;
    private final Priority priority;

    public ConsumedMessage(String messageId, Map<String, Object> headers, String messagePayload) {
        this(messageId, headers, messagePayload, Priority.NORMAL);
    }

    public ConsumedMessage(String messageId, Map<String, Object> headers,
                           String messagePayload, Priority priority) {
        this.messageId = messageId;
        this.headers = headers;
        this.messagePayload = messagePayload;
        this.priority = priority;
    }

    public String getMessageId() {
        return messageId;
    }

    @SuppressWarnings("unchecked")
    public <T> T getHeaderValue(String headerName, Class<T> returnTypeClass) {
        return returnTypeClass.cast(headers.get(headerName));
    }

    public String getMessagePayload() {
        return messagePayload;
    }

    public Priority getPriority() {
        return priority;
    }
}
