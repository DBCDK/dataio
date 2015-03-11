package dk.dbc.dataio.gui.client.model;

public class ItemModel extends GenericBackendModel {

    public enum LifeCycle { PARTITIONING, PROCESSING, DELIVERING, DONE }

    private String itemNumber;
    private String itemId;
    private String chunkId;
    private String jobId;
    LifeCycle lifeCycle;


    public ItemModel(
            String itemNumber,
            String itemId,
            String chunkId,
            String jobId,
            LifeCycle lifeCycle) {

        this.itemNumber = itemNumber;
        this.itemId = itemId;
        this.chunkId = chunkId;
        this.jobId = jobId;
        this.lifeCycle = lifeCycle;
    }

    public ItemModel() {
        this("1", "0", "0", "0", LifeCycle.PARTITIONING);
    }

    public String getItemNumber() {
        return itemNumber;
    }

    public String getItemId() {
        return itemId;
    }

    public String getChunkId() {
        return chunkId;
    }

    public String getJobId() {
        return jobId;
    }

    public LifeCycle getStatus() {
        return lifeCycle;
    }
}
