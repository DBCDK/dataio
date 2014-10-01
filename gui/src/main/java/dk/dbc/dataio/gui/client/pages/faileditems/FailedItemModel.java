package dk.dbc.dataio.gui.client.pages.faileditems;

import java.io.Serializable;

/**
 * Failed Items Model<br>
 * Holds data to to be used, when showing list of Failed Items
 */
public class FailedItemModel implements Serializable {
    private static final long serialVersionUID = -3264661042687015529L;
    private String jobId;
    private String chunkId;
    private String itemId;
    private String failedItem;

    public FailedItemModel(String jobId, String chunkId, String itemId, String failedItem) {
        this.jobId = jobId;
        this.chunkId = chunkId;
        this.itemId = itemId;
        this.failedItem = failedItem;
    }

    public FailedItemModel() {
        this("", "", "", "");
    }

    public String getJobId() {
        return jobId;
    }

    public void setJobId(String jobId) {
        this.jobId = jobId;
    }

    public String getChunkId() {
        return chunkId;
    }

    public void setChunkId(String chunkId) {
        this.chunkId = chunkId;
    }

    public String getItemId() {
        return itemId;
    }

    public void setItemId(String itemId) {
        this.itemId = itemId;
    }

    public String getFailedItem() {
        return failedItem;
    }

    public void setFailedItem(String failedItem) {
        this.failedItem = failedItem;
    }
}
