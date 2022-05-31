package dk.dbc.dataio.gui.client.modelBuilders;

import dk.dbc.dataio.gui.client.model.DiagnosticModel;
import dk.dbc.dataio.gui.client.model.ItemModel;
import dk.dbc.dataio.gui.client.model.WorkflowNoteModel;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class ItemModelBuilder {
    private String itemNumber = "1";
    private String itemId = "0";
    private String chunkId = "0";
    private String jobId = "1";
    private String recordId = "1";
    private ItemModel.LifeCycle lifeCycle = ItemModel.LifeCycle.PARTITIONING;
    private List<DiagnosticModel> diagnosticModels = new ArrayList<>(Collections.singletonList(
            new DiagnosticModelBuilder().build()));
    private boolean diagnosticFatal = false;
    private WorkflowNoteModel workflowNoteModel = new WorkflowNoteModelBuilder().build();
    private String trackingId;

    public ItemModelBuilder setItemNumber(String itemNumber) {
        this.itemNumber = itemNumber;
        return this;
    }

    public ItemModelBuilder setItemId(String itemId) {
        this.itemId = itemId;
        return this;
    }

    public ItemModelBuilder setChunkId(String chunkId) {
        this.chunkId = chunkId;
        return this;
    }

    public ItemModelBuilder setJobId(String jobId) {
        this.jobId = jobId;
        return this;
    }

    public ItemModelBuilder setRecordId(String recordId) {
        this.recordId = recordId;
        return this;
    }

    public ItemModelBuilder setLifeCycle(ItemModel.LifeCycle lifeCycle) {
        this.lifeCycle = lifeCycle;
        return this;
    }

    public ItemModelBuilder setDiagnosticModels(List<DiagnosticModel> diagnosticModels) {
        this.diagnosticModels = new ArrayList<>(diagnosticModels);
        return this;
    }

    public ItemModelBuilder setHasDiagnosticFatal(boolean diagnosticFatal) {
        this.diagnosticFatal = diagnosticFatal;
        return this;
    }

    public ItemModelBuilder setWorkflowNoteModel(WorkflowNoteModel workflowNoteModel) {
        this.workflowNoteModel = workflowNoteModel;
        return this;
    }

    public ItemModelBuilder setTrackingId(String trackingId) {
        this.trackingId = trackingId;
        return this;
    }

    public ItemModel build() {
        return new ItemModel(itemNumber, itemId, chunkId, jobId, recordId, lifeCycle, diagnosticModels, diagnosticFatal, workflowNoteModel, trackingId);
    }

}
