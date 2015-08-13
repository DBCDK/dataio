package dk.dbc.dataio.gui.client.modelBuilders;

import dk.dbc.dataio.gui.client.model.DiagnosticModel;
import dk.dbc.dataio.gui.client.model.ItemModel;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class ItemModelBuilder {
    private String itemNumber = "1";
    private String itemId = "0";
    private String chunkId = "0";
    private String jobId = "1";
    private ItemModel.LifeCycle lifeCycle = ItemModel.LifeCycle.PARTITIONING;
    private List<DiagnosticModel> diagnosticModels = new ArrayList<DiagnosticModel>(Collections.singletonList(
            new DiagnosticModelBuilder().build()));
    private boolean diagnosticFatal = false;

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

    public ItemModelBuilder setLifeCycle(ItemModel.LifeCycle lifeCycle) {
        this.lifeCycle = lifeCycle;
        return this;
    }

    public ItemModelBuilder setDiagnosticModels(List<DiagnosticModel> diagnosticModels) {
        this.diagnosticModels = new ArrayList<DiagnosticModel>(diagnosticModels);
        return this;
    }

    public ItemModelBuilder setHasDiagnosticFatal(boolean diagnosticFatal) {
        this.diagnosticFatal = diagnosticFatal;
        return this;
    }

    public ItemModel build() {
        return new ItemModel(itemNumber, itemId, chunkId, jobId, lifeCycle, diagnosticModels, diagnosticFatal);
    }

}
