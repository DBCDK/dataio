package dk.dbc.dataio.commons.utils.test.json;

import dk.dbc.dataio.commons.types.Priority;

public class SubmitterContentJsonBuilder extends JsonBuilder {
    private Long number = 42L;
    private String name = "name";
    private String description = "description";
    private Priority priority = Priority.NORMAL;

    public SubmitterContentJsonBuilder setNumber(Long number) {
        this.number = number;
        return this;
    }

    public SubmitterContentJsonBuilder setDescription(String description) {
        this.description = description;
        return this;
    }

    public SubmitterContentJsonBuilder setName(String name) {
        this.name = name;
        return this;
    }

    public SubmitterContentJsonBuilder setPriority(Priority priority) {
        this.priority = priority;
        return this;
    }

    public String build() {
        final StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append(START_OBJECT);
        stringBuilder.append(asLongMember("number", number));
        stringBuilder.append(MEMBER_DELIMITER);
        stringBuilder.append(asTextMember("name", name));
        stringBuilder.append(MEMBER_DELIMITER);
        stringBuilder.append(asTextMember("description", description));
        stringBuilder.append(MEMBER_DELIMITER);
        stringBuilder.append(asTextMember("priority", priority.name()));
        stringBuilder.append(END_OBJECT);
        return stringBuilder.toString();
    }
}
