package dk.dbc.dataio.commons.utils.test.json;

public class FlowContentJsonBuilder extends JsonBuilder {
    private String name = "name";
    private String description = "description";

    public FlowContentJsonBuilder setDescription(String description) {
        this.description = description;
        return this;
    }

    public FlowContentJsonBuilder setName(String name) {
        this.name = name;
        return this;
    }

    public String build() {
        final StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append(START_OBJECT);
        stringBuilder.append(asTextMember("name", name));
        stringBuilder.append(MEMBER_DELIMITER);
        stringBuilder.append(asTextMember("description", description));
        stringBuilder.append(END_OBJECT);
        return stringBuilder.toString();
    }
}
