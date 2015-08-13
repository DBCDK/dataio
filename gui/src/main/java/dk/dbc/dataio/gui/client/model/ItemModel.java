package dk.dbc.dataio.gui.client.model;

import java.util.ArrayList;
import java.util.List;

public class ItemModel extends GenericBackendModel {

    public enum LifeCycle { PARTITIONING, PROCESSING, DELIVERING, DONE }

    private String itemNumber;
    private String itemId;
    private String chunkId;
    private String jobId;
    private LifeCycle lifeCycle;
    private List<DiagnosticModel> diagnosticModels;
    private boolean diagnosticFatal;


    public ItemModel(
            String itemNumber,
            String itemId,
            String chunkId,
            String jobId,
            LifeCycle lifeCycle,
            List<DiagnosticModel> diagnosticModels,
            boolean diagnosticFatal) {


        this.itemNumber = itemNumber;
        this.itemId = itemId;
        this.chunkId = chunkId;
        this.jobId = jobId;
        this.lifeCycle = lifeCycle;
        this.diagnosticModels = diagnosticModels;
        this.diagnosticFatal = diagnosticFatal;
    }

    public ItemModel() {
        this("1", "0", "0", "0", LifeCycle.PARTITIONING, new ArrayList<DiagnosticModel>(), false);
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

    public List<DiagnosticModel> getDiagnosticModels() {
        return diagnosticModels;
    }

    public boolean isDiagnosticFatal() {
        return diagnosticFatal;
    }
}
