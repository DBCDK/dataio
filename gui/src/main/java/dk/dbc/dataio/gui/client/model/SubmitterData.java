package dk.dbc.dataio.gui.client.model;

import com.google.gwt.user.client.rpc.IsSerializable;

public class SubmitterData implements IsSerializable {
    private String name;
    private String number;
    private String description;

    public void setName(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public void setNumber(String number) {
        this.number = number;
    }

    public String getNumber() {
        return number;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}
