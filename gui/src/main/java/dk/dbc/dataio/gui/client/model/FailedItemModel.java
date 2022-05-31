package dk.dbc.dataio.gui.client.model;

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
    private String chunkifyState;
    private String processingState;
    private String deliveryState;

    public FailedItemModel(String jobId, String chunkId, String itemId, String chunkifyState, String processingState, String deliveryState) {
        this.jobId = jobId;
        this.chunkId = chunkId;
        this.itemId = itemId;
        this.chunkifyState = chunkifyState;
        this.processingState = processingState;
        this.deliveryState = deliveryState;
    }

    public FailedItemModel() {
        this("", "", "", "", "", "");
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

    public String getChunkifyState() {
        return chunkifyState;
    }

    public void setChunkifyState(String chunkifyState) {
        this.chunkifyState = chunkifyState;
    }

    public String getProcessingState() {
        return processingState;
    }

    public void setProcessingState(String processingState) {
        this.processingState = processingState;
    }

    public String getDeliveryState() {
        return deliveryState;
    }

    public void setDeliveryState(String deliveryState) {
        this.deliveryState = deliveryState;
    }
}
