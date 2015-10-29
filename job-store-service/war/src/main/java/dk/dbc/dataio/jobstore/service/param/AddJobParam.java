/*
 * DataIO - Data IO
 * Copyright (C) 2015 Dansk Bibliotekscenter a/s, Tempovej 7-11, DK-2750 Ballerup,
 * Denmark. CVR: 15149043
 *
 * This file is part of DataIO.
 *
 * DataIO is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * DataIO is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with DataIO.  If not, see <http://www.gnu.org/licenses/>.
 */

package dk.dbc.dataio.jobstore.service.param;

import dk.dbc.dataio.common.utils.flowstore.FlowStoreServiceConnector;
import dk.dbc.dataio.common.utils.flowstore.FlowStoreServiceConnectorException;
import dk.dbc.dataio.common.utils.flowstore.FlowStoreServiceConnectorUnexpectedStatusCodeException;
import dk.dbc.dataio.commons.types.Flow;
import dk.dbc.dataio.commons.types.FlowBinder;
import dk.dbc.dataio.commons.types.JobSpecification;
import dk.dbc.dataio.commons.types.Sink;
import dk.dbc.dataio.commons.types.Submitter;
import dk.dbc.dataio.commons.utils.invariant.InvariantUtil;
import dk.dbc.dataio.jobstore.types.Diagnostic;
import dk.dbc.dataio.jobstore.types.FlowStoreReference;
import dk.dbc.dataio.jobstore.types.FlowStoreReferences;
import dk.dbc.dataio.jobstore.types.JobInputStream;

import javax.ws.rs.ProcessingException;
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

    protected final FlowStoreServiceConnector flowStoreServiceConnector;
    protected final JobInputStream jobInputStream;
    protected List<Diagnostic> diagnostics;
    protected Submitter submitter;
    protected FlowBinder flowBinder;
    protected Flow flow;
    protected Sink sink;
    protected FlowStoreReferences flowStoreReferences;

    public AddJobParam(JobInputStream jobInputStream, FlowStoreServiceConnector flowStoreServiceConnector) throws NullPointerException {

        this.jobInputStream = InvariantUtil.checkNotNullOrThrow(jobInputStream, "jobInputStream");
        this.flowStoreServiceConnector = InvariantUtil.checkNotNullOrThrow(flowStoreServiceConnector, "flowStoreServiceConnector");
        this.diagnostics = new ArrayList<>();
        this.submitter = lookupSubmitter();
        this.flowBinder = lookupFlowBinder();
        this.flow = lookupFlow();
        this.sink = lookupSink();
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
    public FlowBinder getFlowBinder() {
        return flowBinder;
    }
    public Flow getFlow() {
        return flow;
    }
    public Sink getSink() {
        return sink;
    }
    public FlowStoreReferences getFlowStoreReferences() {
        return flowStoreReferences;
    }

    private Submitter lookupSubmitter() {
        final long submitterNumber = jobInputStream.getJobSpecification().getSubmitterId();
        try {
            return flowStoreServiceConnector.getSubmitterBySubmitterNumber(submitterNumber);
        } catch(FlowStoreServiceConnectorException | ProcessingException e) {
            final String message = String.format("Could not retrieve Submitter with submitter number: %d", submitterNumber);
            diagnostics.add(new Diagnostic(Diagnostic.Level.FATAL, message, e));
        }
        return null;
    }

    private FlowBinder lookupFlowBinder() {
        final JobSpecification jobSpec = jobInputStream.getJobSpecification();
        try {
            return flowStoreServiceConnector.getFlowBinder(
                    jobSpec.getPackaging(),
                    jobSpec.getFormat(),
                    jobSpec.getCharset(),
                    jobSpec.getSubmitterId(),
                    jobSpec.getDestination());
        } catch (FlowStoreServiceConnectorException | ProcessingException e) {
            if (e instanceof FlowStoreServiceConnectorUnexpectedStatusCodeException) {
                FlowStoreServiceConnectorUnexpectedStatusCodeException exception = (FlowStoreServiceConnectorUnexpectedStatusCodeException) e;
                final String message = String.format("Error in job description: %s", exception.getFlowStoreError().getDescription());
                diagnostics.add(new Diagnostic(Diagnostic.Level.FATAL, message, e));
            } else {
                final String message = String.format("Could not retrieve FlowBinder for specification: %s", jobSpec);
                diagnostics.add(new Diagnostic(Diagnostic.Level.FATAL, message, e));
            }
        }
        return null;
    }

    private Flow lookupFlow() {
        if (flowBinder != null) {
            final long flowId = flowBinder.getContent().getFlowId();
            try {
                return flowStoreServiceConnector.getFlow(flowId);
            } catch(FlowStoreServiceConnectorException | ProcessingException e) {
                final String message = String.format("Could not retrieve Flow with ID: %d", flowId);
                diagnostics.add(new Diagnostic(Diagnostic.Level.FATAL, message, e));
            }
        }
        return null;
    }

    private Sink lookupSink() {
        if (flowBinder != null) {
            final long sinkId = flowBinder.getContent().getSinkId();
            try {
                return flowStoreServiceConnector.getSink(sinkId);
            } catch(FlowStoreServiceConnectorException | ProcessingException e) {
                final String message = String.format("Could not retrieve Sink with ID: %d", sinkId);
                diagnostics.add(new Diagnostic(Diagnostic.Level.FATAL, message, e));
            }
        }
        return null;
    }

    private FlowStoreReferences newFlowStoreReferences() {
        final FlowStoreReferences flowStoreReferences = new FlowStoreReferences();
        if (flowBinder != null) {
            flowStoreReferences.setReference(FlowStoreReferences.Elements.FLOW_BINDER,
                    new FlowStoreReference(flowBinder.getId(), flowBinder.getVersion(),flowBinder.getContent().getName()));
        }
        if (flow != null) {
            flowStoreReferences.setReference(FlowStoreReferences.Elements.FLOW,
                    new FlowStoreReference(flow.getId(), flow.getVersion(),flow.getContent().getName()));
        }
        if (sink != null) {
            flowStoreReferences.setReference(FlowStoreReferences.Elements.SINK,
                    new FlowStoreReference(sink.getId(), sink.getVersion(),sink.getContent().getName()));
        }
        if (submitter != null) {
            flowStoreReferences.setReference(FlowStoreReferences.Elements.SUBMITTER,
                    new FlowStoreReference(submitter.getId(), submitter.getVersion(),submitter.getContent().getName()));
        }
        return flowStoreReferences;
    }
}