package dk.dbc.dataio.commons.utils.test.json;

public class GatekeeperDestinationJsonBuilder extends JsonBuilder {

    private long id = 42;
    private String submitterNumber = "123456";
    private String destination = "destination";
    private String packaging = "packaging";
    private String format = "format";

    public GatekeeperDestinationJsonBuilder setSubmitterNumber(String submitterNumber) {
        this.submitterNumber = submitterNumber;
        return this;
    }

    public GatekeeperDestinationJsonBuilder setDestination(String destination) {
        this.destination = destination;
        return this;
    }

    public GatekeeperDestinationJsonBuilder setPackaging(String packaging) {
        this.packaging = packaging;
        return this;
    }

    public GatekeeperDestinationJsonBuilder setFormat(String format) {
        this.format = format;
        return this;
    }

    public String build() {
        final StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append(START_OBJECT);
        stringBuilder.append(asLongMember("id", id));
        stringBuilder.append(MEMBER_DELIMITER);
        stringBuilder.append(asTextMember("submitterNumber", submitterNumber));
        stringBuilder.append(MEMBER_DELIMITER);
        stringBuilder.append(asTextMember("destination", destination));
        stringBuilder.append(MEMBER_DELIMITER);
        stringBuilder.append(asTextMember("packaging", packaging));
        stringBuilder.append(MEMBER_DELIMITER);
        stringBuilder.append(asTextMember("format", format));
        stringBuilder.append(END_OBJECT);
        return stringBuilder.toString();
    }
}
