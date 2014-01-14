package dk.dbc.dataio.jobprocessor.dto;

public class ConsumedMessage {
    private final String messageId;
    private final String messagePayload;

    public ConsumedMessage(String messageId, String messagePayload) {
        this.messageId = messageId;
        this.messagePayload = messagePayload;
    }

    public String getMessageId() {
        return messageId;
    }

    public String getMessagePayload() {
        return messagePayload;
    }
}
