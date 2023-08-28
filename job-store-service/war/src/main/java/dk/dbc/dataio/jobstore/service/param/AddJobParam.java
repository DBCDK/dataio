package dk.dbc.dataio.jobstore.service.param;

import dk.dbc.dataio.common.utils.flowstore.FlowStoreServiceConnector;
import dk.dbc.dataio.common.utils.flowstore.FlowStoreServiceConnectorException;
import dk.dbc.dataio.common.utils.flowstore.FlowStoreServiceConnectorUnexpectedStatusCodeException;
import dk.dbc.dataio.commons.types.Constants;
import dk.dbc.dataio.commons.types.Diagnostic;
import dk.dbc.dataio.commons.types.Flow;
import dk.dbc.dataio.commons.types.FlowBinder;
import dk.dbc.dataio.commons.types.JobSpecification;
import dk.dbc.dataio.commons.types.ObjectFactory;
import dk.dbc.dataio.commons.types.Priority;
import dk.dbc.dataio.commons.types.RecordSplitterConstants;
import dk.dbc.dataio.commons.types.Sink;
import dk.dbc.dataio.commons.types.Submitter;
import dk.dbc.dataio.jobstore.types.FlowStoreReference;
import dk.dbc.dataio.jobstore.types.FlowStoreReferences;
import dk.dbc.dataio.jobstore.types.JobInputStream;
import dk.dbc.invariant.InvariantUtil;
import jakarta.ws.rs.ProcessingException;

import java.util.ArrayList;
import java.util.List;

/**
 * This class is a parameter abstraction for the PgJobStore.addJob() method.
 * <p>
 * Parameter initialization failures will result in fatal diagnostics being added
 * to the internal diagnostics list, and the corresponding parameter field being
 * given a null value.
 * </p>
 */
public class AddJobParam {
    protected FlowStoreServiceConnector flowStoreServiceConnector;
    protected JobInputStream jobInputStream;
    protected List<Diagnostic> diagnostics;
    protected Submitter submitter;
    protected FlowBinder flowBinder;
    protected RecordSplitterConstants.RecordSplitter typeOfDataPartitioner;
    protected Flow flow;
    protected Sink sink;
    protected FlowStoreReferences flowStoreReferences;

    public AddJobParam(JobInputStream jobInputStream) {
        this.jobInputStream = jobInputStream;

    }

    public AddJobParam(JobInputStream jobInputStream, FlowStoreServiceConnector flowStoreServiceConnector) throws NullPointerException {
        this.jobInputStream = InvariantUtil.checkNotNullOrThrow(jobInputStream, "jobInputStream");
        this.flowStoreServiceConnector = InvariantUtil.checkNotNullOrThrow(flowStoreServiceConnector, "flowStoreServiceConnector");
        this.diagnostics = new ArrayList<>();
        if (isDatafileValid()) {
            this.flowBinder = lookupFlowBinder();
            this.submitter = lookupSubmitter();
            this.flow = lookupFlow();
            this.sink = lookupSink();
            this.typeOfDataPartitioner = lookupTypeOfDataPartitioner();
        }
        this.flowStoreReferences = newFlowStoreReferences();
    }

    public JobInputStream getJobInputStream() {
        return jobInputStream;
    }

    public List<Diagnostic> getDiagnostics() {
        return diagnostics;
    }

    public Submitter getSubmitter() {
        return submitter;
    }

    public Flow getFlow() {
        return flow;
    }

    public Sink getSink() {
        return sink;
    }

    public RecordSplitterConstants.RecordSplitter getTypeOfDataPartitioner() {
        return typeOfDataPartitioner;
    }

    public FlowStoreReferences getFlowStoreReferences() {
        return flowStoreReferences;
    }

    public Priority getPriority() {
        Priority priority = Priority.NORMAL;
        if (flowBinder != null) {
            priority = flowBinder.getContent().getPriority();
            if (submitter.getContent().getPriority() != null) {
                // submitter override priority
                priority = submitter.getContent().getPriority();
            }
        }
        return priority;
    }

    private boolean isDatafileValid() {
        JobSpecification jobSpecification = jobInputStream.getJobSpecification();
        JobSpecification.Ancestry ancestry = jobSpecification.getAncestry();
        boolean isJobSpecificationValid = true;
        String message = null;
        if (ancestry != null) {
            if (Constants.MISSING_FIELD_VALUE.equals(ancestry.getDatafile())) {
                message = String.format("Datafil angivelse mangler i transfilen: %s", ancestry.getTransfile());
                isJobSpecificationValid = false;
            } else if (jobSpecification.getDataFile().equals(Constants.MISSING_FIELD_VALUE)) {
                message = String.format("Kan ikke finde datafilen. I transfilen: %s var den forventede datafil angivet som: %s", ancestry.getTransfile(), ancestry.getDatafile());
                isJobSpecificationValid = false;
            }

        } else if (jobSpecification.getDataFile().equals(Constants.MISSING_FIELD_VALUE)) {
            message = "Kan ikke finde datafilen";
            isJobSpecificationValid = false;
        }

        if (!isJobSpecificationValid) {
            diagnostics.add(ObjectFactory.buildFatalDiagnostic(message));
        }
        return isJobSpecificationValid;
    }

    protected FlowBinder lookupFlowBinder() {
        final JobSpecification jobSpec = jobInputStream.getJobSpecification();
        try {
            return flowStoreServiceConnector.getFlowBinder(
                    jobSpec.getPackaging(),
                    jobSpec.getFormat(),
                    jobSpec.getCharset(),
                    jobSpec.getSubmitterId(),
                    jobSpec.getDestination());
        } catch (FlowStoreServiceConnectorException | ProcessingException e) {
            String message = String.format("Could not retrieve FlowBinder for specification: %s", jobSpec); // Default msg
            if (e instanceof FlowStoreServiceConnectorUnexpectedStatusCodeException) {
                FlowStoreServiceConnectorUnexpectedStatusCodeException exception = (FlowStoreServiceConnectorUnexpectedStatusCodeException) e;
                if (exception.getFlowStoreError() != null) {
                    message = exception.getFlowStoreError().getDescription();
                }
            }
            diagnostics.add(ObjectFactory.buildFatalDiagnostic(message, e));
        }
        return null;
    }

    protected RecordSplitterConstants.RecordSplitter lookupTypeOfDataPartitioner() {
        if (flowBinder != null) {
            return flowBinder.getContent().getRecordSplitter();
        }
        return null;
    }

    protected Submitter lookupSubmitter() {
        final long submitterNumber = jobInputStream.getJobSpecification().getSubmitterId();
        try {
            return flowStoreServiceConnector.getSubmitterBySubmitterNumber(submitterNumber);
        } catch (FlowStoreServiceConnectorException | ProcessingException e) {
            if (flowBinder != null) { // No diagnostic created when retrieving flow binder
                final String message = String.format("Could not retrieve Submitter with submitter number: %d", submitterNumber);
                diagnostics.add(ObjectFactory.buildFatalDiagnostic(message, e));
            }
        }
        return null;
    }

    protected Flow lookupFlow() {
        if (flowBinder != null) {
            final long flowId = flowBinder.getContent().getFlowId();
            try {
                return flowStoreServiceConnector.getFlow(flowId);
            } catch (FlowStoreServiceConnectorException | ProcessingException e) {
                final String message = String.format("Could not retrieve Flow with ID: %d", flowId);
                diagnostics.add(ObjectFactory.buildFatalDiagnostic(message, e));
            }
        }
        return null;
    }

    protected Sink lookupSink() {
        if (jobInputStream.getJobSpecification().getType() == JobSpecification.Type.ACCTEST) {
            return Sink.DIFF;
        } else if (flowBinder != null) {
            final long sinkId = flowBinder.getContent().getSinkId();
            try {
                return flowStoreServiceConnector.getSink(sinkId);
            } catch (FlowStoreServiceConnectorException | ProcessingException e) {
                final String message = String.format("Could not retrieve Sink with ID: %d", sinkId);
                diagnostics.add(ObjectFactory.buildFatalDiagnostic(message, e));
            }
        }
        return null;
    }

    private FlowStoreReferences newFlowStoreReferences() {
        final FlowStoreReferences flowStoreReferences = new FlowStoreReferences();
        if (flowBinder != null) {
            flowStoreReferences.setReference(FlowStoreReferences.Elements.FLOW_BINDER,
                    new FlowStoreReference(flowBinder.getId(), flowBinder.getVersion(), flowBinder.getContent().getName()));
        }
        if (flow != null) {
            flowStoreReferences.setReference(FlowStoreReferences.Elements.FLOW,
                    new FlowStoreReference(flow.getId(), flow.getVersion(), flow.getContent().getName()));
        }
        if (sink != null) {
            flowStoreReferences.setReference(FlowStoreReferences.Elements.SINK,
                    new FlowStoreReference(sink.getId(), sink.getVersion(), sink.getContent().getName()));
        }
        if (submitter != null) {
            flowStoreReferences.setReference(FlowStoreReferences.Elements.SUBMITTER,
                    new FlowStoreReference(submitter.getId(), submitter.getVersion(), submitter.getContent().getName()));
        }
        return flowStoreReferences;
    }
}
