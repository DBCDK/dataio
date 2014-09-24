package dk.dbc.dataio.gui.client.pages.faileditems;


import com.google.gwt.user.client.rpc.IsSerializable;
import com.google.gwt.view.client.ProvidesKey;
import com.sun.corba.se.pept.transport.ContactInfo;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

/**
 * Failed Items Model<br>
 * Holds data to to be used, when showing list of Failed Items
 */
public class FailedItemModel implements Serializable {

    private String id;
    private String failedItem;

    public FailedItemModel(String id, String failedItem) {
        this.id = id;
        this.failedItem = failedItem;
    }

    public FailedItemModel() {
        this.id = "";
        this.failedItem = "";
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getFailedItem() {
        return failedItem;
    }

    public void setFailedItem(String failedItem) {
        this.failedItem = failedItem;
    }

}
