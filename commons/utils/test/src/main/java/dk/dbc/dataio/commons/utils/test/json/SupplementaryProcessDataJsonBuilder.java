package dk.dbc.dataio.commons.utils.test.json;

public class SupplementaryProcessDataJsonBuilder extends JsonBuilder {

    private long submitter = 987654L;
    private String format = "latin-1";

    public SupplementaryProcessDataJsonBuilder setSubmitter(long submitter) {
        this.submitter = submitter;
        return this;
    }

    public SupplementaryProcessDataJsonBuilder setFormat(String format) {
        this.format = format;
        return this;
    }

    public String build() {
        final StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append(START_OBJECT);
        stringBuilder.append(asLongMember("submitter", submitter));
        stringBuilder.append(MEMBER_DELIMITER);
        stringBuilder.append(asTextMember("format", format));
        stringBuilder.append(END_OBJECT);
        return stringBuilder.toString();
    }

}
