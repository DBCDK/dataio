package dk.dbc.dataio.gui.client.model;

import com.google.gwt.user.client.rpc.IsSerializable;

public class SubmitterData implements IsSerializable {

    public String submittername;
    public String submitternumber;
    public String description;

    public void setSubmitterName(String name) {
        this.submittername = name;
    }

    public String getSubmitterName() {
        return submittername;
    }

    public void setSubmitterNumber(String number) {
        this.submitternumber = number;
    }

    public String getSubmitterNumber() {
        return submitternumber;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}
