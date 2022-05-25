package dk.dbc.dataio.gui.client.model;

import java.util.ArrayList;
import java.util.List;

public class ItemModel extends GenericBackendModel {

    public enum LifeCycle {PARTITIONING, PROCESSING, DELIVERING, DONE, PARTITIONING_FAILED, PARTITIONING_IGNORED, PROCESSING_FAILED, DELIVERING_FAILED, PROCESSING_IGNORED, DELIVERING_IGNORED}

    private String itemNumber;
    private String itemId;
    private String chunkId;
    private String jobId;
    private String recordId;
    private LifeCycle lifeCycle;
    private List<DiagnosticModel> diagnosticModels;
    private boolean diagnosticFatal;
    private WorkflowNoteModel workflowNoteModel;
    private String trackingId;

    public ItemModel(
            String itemNumber,
            String itemId,
            String chunkId,
            String jobId,
            String recordId,
            LifeCycle lifeCycle,
            List<DiagnosticModel> diagnosticModels,
            boolean diagnosticFatal,
            WorkflowNoteModel workflowNoteModel,
            String trackingId) {


        this.itemNumber = itemNumber;
        this.itemId = itemId;
        this.chunkId = chunkId;
        this.jobId = jobId;
        this.recordId = recordId;
        this.lifeCycle = lifeCycle;
        this.diagnosticModels = diagnosticModels;
        this.diagnosticFatal = diagnosticFatal;
        this.workflowNoteModel = workflowNoteModel;
        this.trackingId = trackingId;
    }

    public ItemModel() {
        this("1", "0", "0", "0", "0", LifeCycle.PARTITIONING, new ArrayList<DiagnosticModel>(), false, (WorkflowNoteModel) null, "0");
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

    public String getRecordId() {
        return recordId;
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

    public WorkflowNoteModel getWorkflowNoteModel() {
        return workflowNoteModel;
    }

    public void setWorkflowNoteModel(WorkflowNoteModel workflowNoteModel) {
        this.workflowNoteModel = workflowNoteModel;
    }

    public String getTrackingId() {
        return trackingId;
    }
}
