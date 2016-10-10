package types;

import dk.dbc.dataio.common.utils.flowstore.FlowStoreServiceConnector;
import dk.dbc.dataio.commons.types.Diagnostic;
import dk.dbc.dataio.commons.types.Flow;
import dk.dbc.dataio.commons.types.FlowBinder;
import dk.dbc.dataio.commons.types.Sink;
import dk.dbc.dataio.commons.types.Submitter;
import dk.dbc.dataio.jobstore.service.param.AddJobParam;
import dk.dbc.dataio.jobstore.types.FlowStoreReferences;
import dk.dbc.dataio.jobstore.types.JobInputStream;

import java.util.List;

public class TestableAddJobParam extends AddJobParam {
    private String records;
    private FlowBinder flowBinder;

    public TestableAddJobParam(String records,
                               JobInputStream jobInputStream,
                               FlowStoreServiceConnector flowStoreServiceConnector,
                               Submitter submitter,
                               Flow flow,
                               Sink sink,
                               FlowBinder flowBinder,
                               FlowStoreReferences flowStoreReferences,
                               List<Diagnostic> diagnostics) {

        super(jobInputStream, flowStoreServiceConnector);

        this.submitter = submitter;
        this.flow = flow;
        this.sink = sink;
        this.flowBinder = flowBinder;
        this.typeOfDataPartitioner = flowBinder.getContent().getRecordSplitter();
        this.flowStoreReferences = flowStoreReferences;
        this.diagnostics = diagnostics;
        this.records = records;
    }

    public String getRecords() {
        return records;
    }

    public FlowBinder getFlowBinder() {
        return flowBinder;
    }
}
