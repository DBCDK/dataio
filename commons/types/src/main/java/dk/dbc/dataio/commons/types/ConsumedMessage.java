package dk.dbc.dataio.commons.types;

public class ConsumedMessage {
    private final String messageId;
    private final String payloadType;
    private final String messagePayload;

    public ConsumedMessage(String messageId, String payloadType, String messagePayload) {
        this.messageId = messageId;
        this.payloadType = payloadType;
        this.messagePayload = messagePayload;
    }

    public String getMessageId() {
        return messageId;
    }

    public String getPayloadType() {
        return payloadType;
    }

    public String getMessagePayload() {
        return messagePayload;
    }
}
