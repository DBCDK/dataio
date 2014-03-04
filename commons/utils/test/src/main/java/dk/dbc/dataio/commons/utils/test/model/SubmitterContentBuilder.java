package dk.dbc.dataio.commons.utils.test.model;

import dk.dbc.dataio.commons.types.SubmitterContent;

public class SubmitterContentBuilder {
    private Long number = 63L;
    private String name = "name";
    private String description = "description";

    public SubmitterContentBuilder setNumber(Long number) {
        this.number = number;
        return this;
    }

    public SubmitterContentBuilder setName(String name) {
        this.name = name;
        return this;
    }

    public SubmitterContentBuilder setDescription(String description) {
        this.description = description;
        return this;
    }

    public SubmitterContent build() {
        return new SubmitterContent(number, name, description);
    }
}