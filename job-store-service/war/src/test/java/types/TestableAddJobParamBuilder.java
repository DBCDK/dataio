package types;


import dk.dbc.dataio.common.utils.flowstore.FlowStoreServiceConnector;
import dk.dbc.dataio.commons.types.Diagnostic;
import dk.dbc.dataio.commons.types.Flow;
import dk.dbc.dataio.commons.types.FlowBinder;
import dk.dbc.dataio.commons.types.JobSpecification;
import dk.dbc.dataio.commons.types.Sink;
import dk.dbc.dataio.commons.types.Submitter;
import dk.dbc.dataio.commons.utils.test.model.FlowBinderBuilder;
import dk.dbc.dataio.commons.utils.test.model.FlowBuilder;
import dk.dbc.dataio.commons.utils.test.model.SinkBuilder;
import dk.dbc.dataio.commons.utils.test.model.SubmitterBuilder;
import dk.dbc.dataio.jobstore.test.types.FlowStoreReferencesBuilder;
import dk.dbc.dataio.jobstore.types.FlowStoreReferences;
import dk.dbc.dataio.jobstore.types.JobInputStream;

import java.util.ArrayList;
import java.util.List;

import static org.mockito.Mockito.mock;

public class TestableAddJobParamBuilder {
    private List<Diagnostic> diagnostics = new ArrayList<>();
    private JobSpecification jobSpecification = new JobSpecification();
    private boolean isEndOfJob = true;
    private int partNumber = 0;
    private Submitter submitter = new SubmitterBuilder().build();
    private Flow flow = new FlowBuilder().build();
    private FlowBinder flowBinder = new FlowBinderBuilder().build();
    private Sink sink = new SinkBuilder().build();
    private FlowStoreReferences flowStoreReferences = new FlowStoreReferencesBuilder().build();
    private FlowStoreServiceConnector flowStoreServiceConnector = mock(FlowStoreServiceConnector.class);
    private byte[] records = (
            "<records>"
                    + "<record>first</record>"
                    + "<record>second</record>"
                    + "<record>third</record>"
                    + "<record>fourth</record>"
                    + "<record>fifth</record>"
                    + "<record>sixth</record>"
                    + "<record>seventh</record>"
                    + "<record>eighth</record>"
                    + "<record>ninth</record>"
                    + "<record>tenth</record>"
                    + "<record>eleventh</record>"
                    + "</records>").getBytes();

    public TestableAddJobParamBuilder setDiagnostics(List<Diagnostic> diagnostics) {
        this.diagnostics = diagnostics;
        return this;
    }

    public TestableAddJobParamBuilder setJobSpecification(JobSpecification jobSpecification) {
        this.jobSpecification = jobSpecification;
        return this;
    }

    public TestableAddJobParamBuilder setIsEndIfJob(boolean isEndOfJob) {
        this.isEndOfJob = isEndOfJob;
        return this;
    }

    public TestableAddJobParamBuilder setPartNumber(int partNumber) {
        this.partNumber = partNumber;
        return this;
    }

    public TestableAddJobParamBuilder setSubmitter(Submitter submitter) {
        this.submitter = submitter;
        return this;
    }

    public TestableAddJobParamBuilder setFlow(Flow flow) {
        this.flow = flow;
        return this;
    }

    public TestableAddJobParamBuilder setFlowBinder(FlowBinder flowBinder) {
        this.flowBinder = flowBinder;
        return this;
    }

    public TestableAddJobParamBuilder setSink(Sink sink) {
        this.sink = sink;
        return this;
    }

    public TestableAddJobParamBuilder setFlowStoreReferences(FlowStoreReferences flowStoreReferences) {
        this.flowStoreReferences = flowStoreReferences;
        return this;
    }

    public TestableAddJobParamBuilder setRecords(byte[] records) {
        this.records = records;
        return this;
    }

    public TestableAddJobParam build() {
        return new TestableAddJobParam(records, buildJobInputStream(), flowStoreServiceConnector, submitter, flow, sink, flowBinder, flowStoreReferences, diagnostics);
    }

    private JobInputStream buildJobInputStream() {
        return new JobInputStream(jobSpecification, isEndOfJob, partNumber);
    }
}
