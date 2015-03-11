package dk.dbc.dataio.gui.client.model;

import com.google.gwt.user.client.rpc.IsSerializable;

public class ItemListCriteriaModel implements IsSerializable {

    public enum ItemSearchType { ALL, FAILED, IGNORED}

    private String itemId;
    private String chunkId;
    private String jobId;
    private ItemSearchType itemSearchType;

    public ItemListCriteriaModel() {
        this.itemId = "0";
        this.chunkId = "0";
        this.jobId = "0";
        this.itemSearchType = ItemSearchType.FAILED;
    }

    public ItemListCriteriaModel(String itemId, String chunkId, String jobId, ItemSearchType itemSearchType) {
        this.itemId = itemId;
        this.chunkId = chunkId;
        this.jobId = jobId;
        this.itemSearchType = itemSearchType;
    }

    public String getItemId() {
        return itemId;
    }

    public void setItemId(String itemId) {
        this.itemId = itemId;
    }

    public String getChunkId() {
        return chunkId;
    }

    public void setChunkId(String chunkId) {
        this.chunkId = chunkId;
    }

    public String getJobId() {
        return jobId;
    }

    public void setJobId(String jobId) {
        this.jobId = jobId;
    }

    public ItemSearchType getItemSearchType() {
        return itemSearchType;
    }

    public void setItemSearchType(ItemSearchType itemSearchType) {
        this.itemSearchType = itemSearchType;
    }
}
