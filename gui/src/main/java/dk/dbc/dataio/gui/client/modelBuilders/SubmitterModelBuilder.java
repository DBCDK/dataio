package dk.dbc.dataio.gui.client.modelBuilders;

import dk.dbc.dataio.commons.types.Priority;
import dk.dbc.dataio.gui.client.model.SubmitterModel;

public class SubmitterModelBuilder {
    private long id = 53L;
    private long version = 1L;
    private String number = "123445";
    private String name = "name";
    private String description = "description";
    private Priority priority = Priority.NORMAL;
    private Boolean enabled = true;

    public SubmitterModelBuilder setId(long id) {
        this.id = id;
        return this;
    }

    public SubmitterModelBuilder setVersion(long version) {
        this.version = version;
        return this;
    }

    public SubmitterModelBuilder setNumber(String number) {
        this.number = number;
        return this;
    }

    public SubmitterModelBuilder setName(String name) {
        this.name = name;
        return this;
    }

    public SubmitterModelBuilder setDescription(String description) {
        this.description = description;
        return this;
    }

    public SubmitterModelBuilder setPriority(Priority priority) {
        this.priority = priority;
        return this;
    }

    public SubmitterModelBuilder setEnabled(Boolean enabled) {
        this.enabled = enabled;
        return this;
    }

    public SubmitterModel build() {
        return new SubmitterModel(id, version, number, name, description, priority.getValue(), enabled);
    }

}
