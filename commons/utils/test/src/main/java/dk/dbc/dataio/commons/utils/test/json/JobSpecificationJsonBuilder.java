package dk.dbc.dataio.commons.utils.test.json;

public class JobSpecificationJsonBuilder extends JsonBuilder {
    private String packaging = "packaging";
    private String format = "format";
    private String charset = "charset";
    private String destination = "destination";
    private String dataFile = "dataFile";
    private long submitterId = 42L;

    public JobSpecificationJsonBuilder setCharset(String charset) {
        this.charset = charset;
        return this;
    }

    public JobSpecificationJsonBuilder setDataFile(String dataFile) {
        this.dataFile = dataFile;
        return this;
    }

    public JobSpecificationJsonBuilder setDestination(String destination) {
        this.destination = destination;
        return this;
    }

    public JobSpecificationJsonBuilder setFormat(String format) {
        this.format = format;
        return this;
    }

    public JobSpecificationJsonBuilder setPackaging(String packaging) {
        this.packaging = packaging;
        return this;
    }

    public JobSpecificationJsonBuilder setSubmitterId(long submitterId) {
        this.submitterId = submitterId;
        return this;
    }

    public String build() {
        final StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append(START_OBJECT);
        stringBuilder.append(asTextMember("packaging", packaging)); stringBuilder.append(MEMBER_DELIMITER);
        stringBuilder.append(asTextMember("format", format)); stringBuilder.append(MEMBER_DELIMITER);
        stringBuilder.append(asTextMember("charset", charset)); stringBuilder.append(MEMBER_DELIMITER);
        stringBuilder.append(asTextMember("destination", destination)); stringBuilder.append(MEMBER_DELIMITER);
        stringBuilder.append(asTextMember("dataFile", dataFile)); stringBuilder.append(MEMBER_DELIMITER);
        stringBuilder.append(asTextMember("mailForNotificationAboutVerification", "ab@cd.ef")); stringBuilder.append(MEMBER_DELIMITER);
        stringBuilder.append(asTextMember("mailForNotificationAboutProcessing", "ab@cd.ef")); stringBuilder.append(MEMBER_DELIMITER);
        stringBuilder.append(asTextMember("resultmailInitials", "abc")); stringBuilder.append(MEMBER_DELIMITER);
        stringBuilder.append(asLongMember("submitterId", submitterId));
        stringBuilder.append(END_OBJECT);
        return stringBuilder.toString();
    }
}
