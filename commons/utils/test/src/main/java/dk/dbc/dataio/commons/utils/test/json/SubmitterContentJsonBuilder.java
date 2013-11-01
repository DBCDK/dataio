package dk.dbc.dataio.commons.utils.test.json;

public class SubmitterContentJsonBuilder extends JsonBuilder {
    private String name = "name";
    private String description = "description";
    private Long number = 42L;

    public SubmitterContentJsonBuilder setDescription(String description) {
        this.description = description;
        return this;
    }

    public SubmitterContentJsonBuilder setName(String name) {
        this.name = name;
        return this;
    }

    public SubmitterContentJsonBuilder setNumber(Long number) {
        this.number = number;
        return this;
    }

    public String build() {
        final StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append(START_OBJECT);
        stringBuilder.append(asTextMember("name", name)); stringBuilder.append(MEMBER_DELIMITER);
        stringBuilder.append(asTextMember("description", description)); stringBuilder.append(MEMBER_DELIMITER);
        stringBuilder.append(asLongMember("number", number));
        stringBuilder.append(END_OBJECT);
        return stringBuilder.toString();
    }
}
