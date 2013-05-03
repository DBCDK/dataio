package dk.dbc.dataio.gui.client.model;

import com.google.gwt.user.client.rpc.IsSerializable;

public class FlowData implements IsSerializable {
    public String flowname;
    public String description;

    public void setFlowname(String flowname) {
        this.flowname = flowname;
    }

    public String getFlowname() {
        return flowname;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}
