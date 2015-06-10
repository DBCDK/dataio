package dk.dbc.dataio.commons.utils.test.json;

public class SinkContentJsonBuilder extends JsonBuilder {
    private String name = "defaultSinkName";
    private String resource = "defaultResource";
    private String description = "defaultDescription";

    public SinkContentJsonBuilder setName(String name) {
        this.name = name;
        return this;
    }

    public SinkContentJsonBuilder setResource(String resource) {
        this.resource = resource;
        return this;
    }

    public SinkContentJsonBuilder setDescription(String description) {
        this.description = description;
        return this;
    }

    public String build() {
        final StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append(START_OBJECT);
        stringBuilder.append(asTextMember("name", name)); stringBuilder.append(MEMBER_DELIMITER);
        stringBuilder.append(asTextMember("resource", resource)); stringBuilder.append(MEMBER_DELIMITER);
        stringBuilder.append(asTextMember("description", description));
        stringBuilder.append(END_OBJECT);
        return stringBuilder.toString();
    }
}
