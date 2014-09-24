package dk.dbc.dataio.gui.client.pages.faileditems;

import java.io.Serializable;

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
