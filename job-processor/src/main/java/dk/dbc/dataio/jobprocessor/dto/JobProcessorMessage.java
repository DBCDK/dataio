package dk.dbc.dataio.jobprocessor.dto;

public class JobProcessorMessage {
    private final String messageId;
    private final String messagePayload;

    public JobProcessorMessage(String messageId, String messagePayload) {
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
