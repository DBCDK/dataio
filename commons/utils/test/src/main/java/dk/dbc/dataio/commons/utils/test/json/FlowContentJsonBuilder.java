package dk.dbc.dataio.commons.utils.test.json;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class FlowContentJsonBuilder extends JsonBuilder {
    private String name = "name";
    private String description = "description";
    private List<String> components = new ArrayList<>(Collections.singletonList(
            new FlowComponentJsonBuilder().build()));

    public FlowContentJsonBuilder setDescription(String description) {
        this.description = description;
        return this;
    }

    public FlowContentJsonBuilder setName(String name) {
        this.name = name;
        return this;
    }

    public FlowContentJsonBuilder setComponents(List<String> components) {
        this.components = new ArrayList<>(components);
        return this;
    }

    public String build() {
        final StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append(START_OBJECT);
        stringBuilder.append(asTextMember("name", name));
        stringBuilder.append(MEMBER_DELIMITER);
        stringBuilder.append(asTextMember("description", description));
        stringBuilder.append(MEMBER_DELIMITER);
        stringBuilder.append(asObjectArray("components", components));
        stringBuilder.append(END_OBJECT);
        return stringBuilder.toString();
    }
}
