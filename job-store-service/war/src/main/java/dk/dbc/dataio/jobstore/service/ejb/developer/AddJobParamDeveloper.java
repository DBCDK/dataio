package dk.dbc.dataio.jobstore.service.ejb.developer;

import dk.dbc.dataio.common.utils.flowstore.FlowStoreServiceConnector;
import dk.dbc.dataio.commons.types.Diagnostic;
import dk.dbc.dataio.commons.types.Flow;
import dk.dbc.dataio.commons.types.FlowBinder;
import dk.dbc.dataio.commons.types.RecordSplitterConstants;
import dk.dbc.dataio.commons.types.Sink;
import dk.dbc.dataio.commons.types.Submitter;
import dk.dbc.dataio.jobstore.service.param.AddJobParam;
import dk.dbc.dataio.jobstore.types.FlowStoreReferences;
import dk.dbc.dataio.jobstore.types.JobInputStream;

import java.util.List;

public class AddJobParamDeveloper extends AddJobParam {

    public AddJobParamDeveloper(JobInputStream jobInputStream) {
        super(jobInputStream);
    }

    @Override
    public RecordSplitterConstants.RecordSplitter lookupTypeOfDataPartitioner() {
        return typeOfDataPartitioner;
    }

    @Override
    protected FlowBinder lookupFlowBinder() {
        return null;
    }

    @Override
    protected Submitter lookupSubmitter() {
        return null;
    }

    @Override
    protected Flow lookupFlow() {
        return null;
    }

    @Override
    protected Sink lookupSink() {
        return null;
    }

    public AddJobParamDeveloper withTypeOfDataPartitioner(RecordSplitterConstants.RecordSplitter typeOfDataPartitioner) {
        this.typeOfDataPartitioner = typeOfDataPartitioner;
        return this;
    }

    public AddJobParamDeveloper withFlowStoreServiceConnector(FlowStoreServiceConnector flowStoreServiceConnector) {
        this.flowStoreServiceConnector = flowStoreServiceConnector;
        return this;
    }

    public AddJobParamDeveloper withDiagnostics(List<Diagnostic> diagnostics) {
        this.diagnostics = diagnostics;
        return this;
    }

    public AddJobParamDeveloper withSubmitter(Submitter submitter) {
        this.submitter = submitter;
        return this;
    }

    public AddJobParamDeveloper withFlow(Flow flow) {
        this.flow = flow;
        return this;
    }

    public AddJobParamDeveloper withSink(Sink sink) {
        this.sink = sink;
        return this;
    }

    public AddJobParamDeveloper withFlowStoreReferences(FlowStoreReferences flowStoreReferences) {
        this.flowStoreReferences = flowStoreReferences;
        return this;
    }
}
