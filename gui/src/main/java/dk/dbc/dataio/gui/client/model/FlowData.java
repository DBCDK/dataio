package dk.dbc.dataio.gui.client.model;

import com.google.gwt.user.client.rpc.IsSerializable;

public class FlowData implements IsSerializable {
    private String name;
    private String description;

    public void setName(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}
