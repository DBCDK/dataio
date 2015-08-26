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

    /**
     * Merges the current model with the one supplied as a parameter in the call, using AND logic
     * @param model The model to merge with the current one using AND logic
     * @return The resulting model (also stored in this instance)
     */
    public ItemListCriteriaModel and(ItemListCriteriaModel model) {
        if (model != null) {
            setItemSearchType(model.getItemSearchType());  // The old SearchType is discarded, and the new SearchType is used instead
            setJobId(model.getJobId());
            setChunkId(model.getChunkId());
            setItemId(model.getItemId());
        }
        return this;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ItemListCriteriaModel)) return false;

        ItemListCriteriaModel that = (ItemListCriteriaModel) o;

        if (limit != that.limit) return false;
        if (offset != that.offset) return false;
        if (itemId != null ? !itemId.equals(that.itemId) : that.itemId != null) return false;
        if (chunkId != null ? !chunkId.equals(that.chunkId) : that.chunkId != null) return false;
        if (jobId != null ? !jobId.equals(that.jobId) : that.jobId != null) return false;
        return itemSearchType == that.itemSearchType;

    }

    @Override
    public int hashCode() {
        int result = itemId != null ? itemId.hashCode() : 0;
        result = 31 * result + (chunkId != null ? chunkId.hashCode() : 0);
        result = 31 * result + (jobId != null ? jobId.hashCode() : 0);
        result = 31 * result + (itemSearchType != null ? itemSearchType.hashCode() : 0);
        result = 31 * result + limit;
        result = 31 * result + offset;
        return result;
    }
}
