package dk.dbc.dataio.gui.client.model;

import com.google.gwt.user.client.rpc.IsSerializable;

public class ItemListCriteriaModel implements IsSerializable {

    public enum ItemSearchType { ALL, FAILED, IGNORED}

    private String itemId;
    private String chunkId;
    private String jobId;
    private ItemSearchType itemSearchType;
    private int limit;
    private int offset;

    public ItemListCriteriaModel() {
        this("0", "0", "0", ItemSearchType.FAILED, 0, 0);
    }

    private ItemListCriteriaModel(String itemId, String chunkId, String jobId, ItemSearchType itemSearchType, int offset, int limit) {
        this.itemId = itemId;
        this.chunkId = chunkId;
        this.jobId = jobId;
        this.itemSearchType = itemSearchType;
        this.offset = offset;
        this.limit = limit;
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

    public int getLimit() {
        return limit;
    }

    public void setLimit(int limit) {
        this.limit = limit;
    }

    public int getOffset() {
        return offset;
    }

    public void setOffset(int offset) {
        this.offset = offset;
    }
}
