package dk.dbc.dataio.jobstore.service.param;

import dk.dbc.dataio.common.utils.flowstore.FlowStoreServiceConnector;
import dk.dbc.dataio.common.utils.flowstore.FlowStoreServiceConnectorException;
import dk.dbc.dataio.common.utils.flowstore.FlowStoreServiceConnectorUnexpectedStatusCodeException;
import dk.dbc.dataio.commons.types.Constants;
import dk.dbc.dataio.commons.types.Diagnostic;
import dk.dbc.dataio.commons.types.Flow;
import dk.dbc.dataio.commons.types.FlowBinder;
import dk.dbc.dataio.commons.types.FlowStoreError;
import dk.dbc.dataio.commons.types.JobSpecification;
import dk.dbc.dataio.commons.types.Priority;
import dk.dbc.dataio.commons.types.Sink;
import dk.dbc.dataio.commons.types.Submitter;
import dk.dbc.dataio.commons.utils.test.model.FlowBinderBuilder;
import dk.dbc.dataio.commons.utils.test.model.FlowBinderContentBuilder;
import dk.dbc.dataio.commons.utils.test.model.FlowBuilder;
import dk.dbc.dataio.commons.utils.test.model.SinkBuilder;
import dk.dbc.dataio.commons.utils.test.model.SubmitterBuilder;
import dk.dbc.dataio.commons.utils.test.model.SubmitterContentBuilder;
import dk.dbc.dataio.jobstore.types.FlowStoreReference;
import dk.dbc.dataio.jobstore.types.FlowStoreReferences;
import dk.dbc.dataio.jobstore.types.JobInputStream;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.not;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

public class AddJobParamTest extends ParamBaseTest {

    private final FlowStoreServiceConnector mockedFlowStoreServiceConnector = mock(FlowStoreServiceConnector.class);

    @Test
    public void constructor_inputStreamArgIsNull_throws() {
        assertThrows(NullPointerException.class, () -> new AddJobParam(null, mockedFlowStoreServiceConnector));
    }

    @Test
    public void constructor_flowStoreServiceConnectorArgIsNull_throws() {
        JobInputStream jobInputStream = new JobInputStream(jobSpecification, true, 2);
        assertThrows(NullPointerException.class, () -> new AddJobParam(jobInputStream, null));
    }

    @Test
    public void constructor_allArgsAreValid_returnsAddJobParam() {
        JobInputStream jobInputStream = new JobInputStream(jobSpecification, true, 2);
        AddJobParam addJobParam = new AddJobParam(jobInputStream, mockedFlowStoreServiceConnector);
        assertThat(addJobParam, is(notNullValue()));

        assertThat(addJobParam.getDiagnostics(), is(notNullValue()));
        assertThat(addJobParam.getDiagnostics().size(), is(0));

        assertThat(addJobParam.getFlowStoreReferences(), is(notNullValue()));
        assertThat(addJobParam.getFlowStoreReferences(), is(new FlowStoreReferences()));
    }

    @Test
    public void isDatafileValid_ancestryHasDataFileWithMissingValue_diagnosticLevelFatalAdded() {
        JobSpecification jobSpecification = new JobSpecification()
                .withAncestry(new JobSpecification.Ancestry().withDatafile(Constants.MISSING_FIELD_VALUE));

        AddJobParam addJobParam = constructAddJobParam(jobSpecification);
        assertIsDatafileValidForInvalidDatafile(addJobParam);
    }

    @Test
    public void isDatafileValid_dataFileHasMissingValue_diagnosticLevelFatalAdded() {
        JobSpecification jobSpecification = new JobSpecification()
                .withAncestry(new JobSpecification.Ancestry())
                .withDataFile(Constants.MISSING_FIELD_VALUE);

        AddJobParam addJobParam = constructAddJobParam(jobSpecification);
        assertIsDatafileValidForInvalidDatafile(addJobParam);
    }

    @Test
    public void isDatafileValid_ancestryIsNullAndDataFileHasMissingValue_diagnosticLevelFatalAdded() {
        JobSpecification jobSpecification = new JobSpecification()
                .withDataFile(Constants.MISSING_FIELD_VALUE);

        AddJobParam addJobParam = constructAddJobParam(jobSpecification);
        assertIsDatafileValidForInvalidDatafile(addJobParam);
    }

    @Test
    public void lookupSubmitter_flowBinderExistsAndSubmitterNotFound_diagnosticLevelFatalAddedAndReferenceIsNull() throws FlowStoreServiceConnectorException {

        FlowBinder flowBinder = new FlowBinderBuilder().build();

        when(mockedFlowStoreServiceConnector.getFlowBinder(
                eq(jobSpecification.getPackaging()),
                eq(jobSpecification.getFormat()),
                eq(jobSpecification.getCharset()),
                eq(jobSpecification.getSubmitterId()),
                eq(jobSpecification.getDestination()))).thenReturn(flowBinder);

        when(mockedFlowStoreServiceConnector.getSubmitterBySubmitterNumber(
                eq(jobSpecification.getSubmitterId()))).thenThrow(new FlowStoreServiceConnectorException(ERROR_MESSAGE));

        AddJobParam addJobParam = constructAddJobParam();

        List<Diagnostic> diagnostics = addJobParam.getDiagnostics();
        assertThat(diagnostics.size(), is(1));
        assertThat(diagnostics.get(0).getLevel(), is(Diagnostic.Level.FATAL));
        assertThat(diagnostics.get(0).getMessage().contains(Long.valueOf(jobSpecification.getSubmitterId()).toString()), is(true));
        assertThat(addJobParam.getSubmitter(), is(nullValue()));
        assertThat(addJobParam.getFlowStoreReferences().getReference(FlowStoreReferences.Elements.FLOW_BINDER), not(nullValue()));
        assertThat(addJobParam.getFlowStoreReferences().getReference(FlowStoreReferences.Elements.SUBMITTER), is(nullValue()));
    }

    @Test
    public void lookupSubmitter_flowBinderDoesNotExistAndSubmitterNotFound_diagnosticLevelFatalNotAddedAndReferenceIsNull() throws FlowStoreServiceConnectorException {

        when(mockedFlowStoreServiceConnector.getFlowBinder(
                eq(jobSpecification.getPackaging()),
                eq(jobSpecification.getFormat()),
                eq(jobSpecification.getCharset()),
                eq(jobSpecification.getSubmitterId()),
                eq(jobSpecification.getDestination()))).thenThrow(new FlowStoreServiceConnectorException(ERROR_MESSAGE));

        when(mockedFlowStoreServiceConnector.getSubmitterBySubmitterNumber(
                eq(jobSpecification.getSubmitterId()))).thenThrow(new FlowStoreServiceConnectorException(ERROR_MESSAGE));

        AddJobParam addJobParam = constructAddJobParam();

        List<Diagnostic> diagnostics = addJobParam.getDiagnostics();
        assertThat(diagnostics.size(), is(1));
        assertThat(diagnostics.get(0).getLevel(), is(Diagnostic.Level.FATAL));
        assertThat(diagnostics.get(0).getMessage().contains(jobSpecification.toString()), is(true));
        assertThat(addJobParam.getSubmitter(), is(nullValue()));

        assertThat(addJobParam.getFlowStoreReferences(), is(new FlowStoreReferences()));
    }

    @Test
    public void lookupSubmitter_submitterFound_submitterReferenceExists() throws FlowStoreServiceConnectorException {
        Submitter submitter = new SubmitterBuilder().build();

        when(mockedFlowStoreServiceConnector.getSubmitterBySubmitterNumber(eq(jobSpecification.getSubmitterId()))).thenReturn(submitter);


        AddJobParam addJobParam = constructAddJobParam();

        assertThat(addJobParam.getDiagnostics().size(), is(0));
        assertThat(addJobParam.getSubmitter(), is(notNullValue()));
        FlowStoreReferences flowStoreReferences = new FlowStoreReferences();
        FlowStoreReference submitterReference = new FlowStoreReference(submitter.getId(), submitter.getVersion(), submitter.getContent().getName());
        flowStoreReferences.setReference(FlowStoreReferences.Elements.SUBMITTER, submitterReference);
        assertThat(addJobParam.getFlowStoreReferences(), is(flowStoreReferences));
    }

    @Test
    public void lookupFlowBinder_flowBinderNotFound_diagnosticLevelFatalAddedAndReferenceIsNull() throws FlowStoreServiceConnectorException {

        when(mockedFlowStoreServiceConnector.getFlowBinder(
                eq(jobSpecification.getPackaging()),
                eq(jobSpecification.getFormat()),
                eq(jobSpecification.getCharset()),
                eq(jobSpecification.getSubmitterId()),
                eq(jobSpecification.getDestination()))).thenThrow(new FlowStoreServiceConnectorException(ERROR_MESSAGE));

        AddJobParam addJobParam = constructAddJobParam();

        List<Diagnostic> diagnostics = addJobParam.getDiagnostics();
        assertThat(diagnostics.size(), is(1));
        assertThat(diagnostics.get(0).getLevel(), is(Diagnostic.Level.FATAL));
        assertThat(diagnostics.get(0).getMessage().contains(jobSpecification.toString()), is(true));
        assertThat(addJobParam.getTypeOfDataPartitioner(), is(nullValue()));
        assertThat(addJobParam.getFlowStoreReferences(), is(new FlowStoreReferences()));
    }

    @Test
    public void lookupFlowBinder_flowStoreError_diagnosticLevelFatalAddedAndReferenceIsNull() throws FlowStoreServiceConnectorException {
        final String FLOW_STORE_ERROR_DESCRIPTION = "FlowStoreErrorToString";
        FlowStoreServiceConnectorUnexpectedStatusCodeException mockedFlowStoreServiceConnectorUnexpectedStatusCodeException = mock(FlowStoreServiceConnectorUnexpectedStatusCodeException.class);
        FlowStoreError mockedFlowStoreError = mock(FlowStoreError.class);

        when(mockedFlowStoreServiceConnectorUnexpectedStatusCodeException.getFlowStoreError()).thenReturn(mockedFlowStoreError);
        when(mockedFlowStoreError.getDescription()).thenReturn(FLOW_STORE_ERROR_DESCRIPTION);
        when(mockedFlowStoreServiceConnector.getFlowBinder(
                eq(jobSpecification.getPackaging()),
                eq(jobSpecification.getFormat()),
                eq(jobSpecification.getCharset()),
                eq(jobSpecification.getSubmitterId()),
                eq(jobSpecification.getDestination()))).thenThrow(mockedFlowStoreServiceConnectorUnexpectedStatusCodeException);

        AddJobParam addJobParam = constructAddJobParam();

        List<Diagnostic> diagnostics = addJobParam.getDiagnostics();
        assertThat(diagnostics.size(), is(1));
        assertThat(diagnostics.get(0).getLevel(), is(Diagnostic.Level.FATAL));
        assertThat(diagnostics.get(0).getMessage(), is(FLOW_STORE_ERROR_DESCRIPTION));
        assertThat(addJobParam.getTypeOfDataPartitioner(), is(nullValue()));
        assertThat(addJobParam.getFlowStoreReferences(), is(new FlowStoreReferences()));
    }

    @Test
    public void lookupFlowBinder_flowBinderFound_flowBinderReferenceExists() throws FlowStoreServiceConnectorException {
        FlowBinder flowBinder = new FlowBinderBuilder().build();

        when(mockedFlowStoreServiceConnector.getFlowBinder(
                eq(jobSpecification.getPackaging()),
                eq(jobSpecification.getFormat()),
                eq(jobSpecification.getCharset()),
                eq(jobSpecification.getSubmitterId()),
                eq(jobSpecification.getDestination()))).thenReturn(flowBinder);

        AddJobParam addJobParam = constructAddJobParam();

        assertThat(addJobParam.getDiagnostics().size(), is(0));
        assertThat(addJobParam.getTypeOfDataPartitioner(), is(notNullValue()));

        FlowStoreReferences flowStoreReferences = new FlowStoreReferences();
        FlowStoreReference flowBinderReference = new FlowStoreReference(flowBinder.getId(), flowBinder.getVersion(), flowBinder.getContent().getName());
        flowStoreReferences.setReference(FlowStoreReferences.Elements.FLOW_BINDER, flowBinderReference);
        assertThat(addJobParam.getFlowStoreReferences(), is(flowStoreReferences));
    }

    @Test
    public void lookupFlow_flowNotFound_diagnosticLevelFatalAddedAndReferenceIsNull() throws FlowStoreServiceConnectorException {
        FlowBinder flowBinder = new FlowBinderBuilder().build();

        when(mockedFlowStoreServiceConnector.getFlowBinder(
                eq(jobSpecification.getPackaging()),
                eq(jobSpecification.getFormat()),
                eq(jobSpecification.getCharset()),
                eq(jobSpecification.getSubmitterId()),
                eq(jobSpecification.getDestination()))).thenReturn(flowBinder);

        when(mockedFlowStoreServiceConnector.getFlow(eq(flowBinder.getContent().getFlowId()))).thenThrow(new FlowStoreServiceConnectorException(ERROR_MESSAGE));

        AddJobParam addJobParam = constructAddJobParam();

        List<Diagnostic> diagnostics = addJobParam.getDiagnostics();
        assertThat(diagnostics.size(), is(1));
        assertThat(diagnostics.get(0).getLevel(), is(Diagnostic.Level.FATAL));
        assertThat(diagnostics.get(0).getMessage().contains(Long.valueOf(flowBinder.getContent().getFlowId()).toString()), is(true));

        assertThat(addJobParam.getTypeOfDataPartitioner(), is(notNullValue()));
        assertThat(addJobParam.getFlow(), is(nullValue()));

        FlowStoreReferences flowStoreReferences = new FlowStoreReferences();
        FlowStoreReference flowBinderReference = new FlowStoreReference(flowBinder.getId(), flowBinder.getVersion(), flowBinder.getContent().getName());
        flowStoreReferences.setReference(FlowStoreReferences.Elements.FLOW_BINDER, flowBinderReference);
        assertThat(addJobParam.getFlowStoreReferences(), is(flowStoreReferences));
    }

    @Test
    public void lookupFlow_flowFound_flowBinderAndFlowReferenceExists() throws FlowStoreServiceConnectorException {
        FlowBinder flowBinder = new FlowBinderBuilder().build();
        Flow flow = new FlowBuilder().build();

        when(mockedFlowStoreServiceConnector.getFlowBinder(
                eq(jobSpecification.getPackaging()),
                eq(jobSpecification.getFormat()),
                eq(jobSpecification.getCharset()),
                eq(jobSpecification.getSubmitterId()),
                eq(jobSpecification.getDestination()))).thenReturn(flowBinder);

        when(mockedFlowStoreServiceConnector.getFlow(eq(flowBinder.getContent().getFlowId()))).thenReturn(flow);

        AddJobParam addJobParam = constructAddJobParam();

        assertThat(addJobParam.getDiagnostics().size(), is(0));
        assertThat(addJobParam.getFlowStoreReferences(), is(notNullValue()));
        assertThat(addJobParam.getFlow(), is(notNullValue()));

        FlowStoreReferences flowStoreReferences = new FlowStoreReferences();
        FlowStoreReference flowBinderReference = new FlowStoreReference(flowBinder.getId(), flowBinder.getVersion(), flowBinder.getContent().getName());
        FlowStoreReference flowReference = new FlowStoreReference(flow.getId(), flow.getVersion(), flow.getContent().getName());
        flowStoreReferences.setReference(FlowStoreReferences.Elements.FLOW_BINDER, flowBinderReference);
        flowStoreReferences.setReference(FlowStoreReferences.Elements.FLOW, flowReference);
        assertThat(addJobParam.getFlowStoreReferences(), is(flowStoreReferences));
    }

    @Test
    public void lookupSink_sinkNotFound_diagnosticLevelFatalAddedAndReferenceIsNull() throws FlowStoreServiceConnectorException {
        FlowBinder flowBinder = new FlowBinderBuilder().build();

        when(mockedFlowStoreServiceConnector.getFlowBinder(
                eq(jobSpecification.getPackaging()),
                eq(jobSpecification.getFormat()),
                eq(jobSpecification.getCharset()),
                eq(jobSpecification.getSubmitterId()),
                eq(jobSpecification.getDestination()))).thenReturn(flowBinder);

        when(mockedFlowStoreServiceConnector.getSink(eq(flowBinder.getContent().getSinkId()))).thenThrow(new FlowStoreServiceConnectorException(ERROR_MESSAGE));

        AddJobParam addJobParam = constructAddJobParam();
        List<Diagnostic> diagnostics = addJobParam.getDiagnostics();
        assertThat(diagnostics.size(), is(1));
        assertThat(diagnostics.get(0).getLevel(), is(Diagnostic.Level.FATAL));
        assertThat(diagnostics.get(0).getMessage().contains(Long.valueOf(flowBinder.getContent().getSinkId()).toString()), is(true));

        assertThat(addJobParam.getTypeOfDataPartitioner(), is(notNullValue()));
        assertThat(addJobParam.getSink(), is(nullValue()));
        FlowStoreReferences flowStoreReferences = new FlowStoreReferences();
        FlowStoreReference flowStoreReference = new FlowStoreReference(flowBinder.getId(), flowBinder.getVersion(), flowBinder.getContent().getName());
        flowStoreReferences.setReference(FlowStoreReferences.Elements.FLOW_BINDER, flowStoreReference);
        assertThat(addJobParam.getFlowStoreReferences(), is(flowStoreReferences));
    }

    @Test
    public void lookupSink_sinkFound_flowBinderAndSinkReferenceExists() throws FlowStoreServiceConnectorException {
        FlowBinder flowBinder = new FlowBinderBuilder().build();
        Sink sink = new SinkBuilder().build();

        when(mockedFlowStoreServiceConnector.getFlowBinder(
                eq(jobSpecification.getPackaging()),
                eq(jobSpecification.getFormat()),
                eq(jobSpecification.getCharset()),
                eq(jobSpecification.getSubmitterId()),
                eq(jobSpecification.getDestination()))).thenReturn(flowBinder);

        when(mockedFlowStoreServiceConnector.getSink(eq(flowBinder.getContent().getSinkId()))).thenReturn(sink);

        AddJobParam addJobParam = constructAddJobParam();
        assertThat(addJobParam.getDiagnostics().size(), is(0));

        assertThat(addJobParam.getTypeOfDataPartitioner(), is(notNullValue()));
        assertThat(addJobParam.getSink(), is(notNullValue()));

        FlowStoreReferences flowStoreReferences = new FlowStoreReferences();
        FlowStoreReference flowBinderReference = new FlowStoreReference(flowBinder.getId(), flowBinder.getVersion(), flowBinder.getContent().getName());
        FlowStoreReference sinkReference = new FlowStoreReference(sink.getId(), sink.getVersion(), sink.getContent().getName());
        flowStoreReferences.setReference(FlowStoreReferences.Elements.FLOW_BINDER, flowBinderReference);
        flowStoreReferences.setReference(FlowStoreReferences.Elements.SINK, sinkReference);
        assertThat(addJobParam.getFlowStoreReferences(), is(flowStoreReferences));
    }

    @Test
    public void addJobParam_allReachableParametersSet_expectedValuesReturnedThroughGetters() throws FlowStoreServiceConnectorException {
        Submitter submitter = new SubmitterBuilder().build();
        FlowBinder flowBinder = new FlowBinderBuilder().build();
        Flow flow = new FlowBuilder().build();
        Sink sink = new SinkBuilder().build();
        AddJobParam addJobParam = getAddJobParamWithAllParametersSet(submitter, flow, sink, flowBinder);

        assertThat(addJobParam.getDiagnostics().size(), is(0));
        assertThat(addJobParam.getFlowStoreReferences(), is(getFlowStoreReferencesWithAllReferencesSet(submitter, flow, sink, flowBinder)));

        assertThat(addJobParam.getSubmitter(), is(submitter));
        assertThat(addJobParam.getTypeOfDataPartitioner(), is(flowBinder.getContent().getRecordSplitter()));
        assertThat(addJobParam.getFlow(), is(flow));
        assertThat(addJobParam.getSink(), is(sink));
    }

    @Test
    public void getPriority_noFlowBinderFound() {
        AddJobParam addJobParam = constructAddJobParam();
        assertThat(addJobParam.getPriority(), is(Priority.NORMAL));
    }

    @Test
    public void getPriority_fromFlowBinder() throws FlowStoreServiceConnectorException {
        Submitter submitter = new SubmitterBuilder()
                .setContent(new SubmitterContentBuilder()
                        .setPriority(null)
                        .build())
                .build();
        FlowBinder flowBinder = new FlowBinderBuilder()
                .setContent(new FlowBinderContentBuilder()
                        .setPriority(Priority.HIGH)
                        .build())
                .build();
        Flow flow = new FlowBuilder().build();
        Sink sink = new SinkBuilder().build();
        AddJobParam addJobParam = getAddJobParamWithAllParametersSet(submitter, flow, sink, flowBinder);
        assertThat(addJobParam.getPriority(), is(Priority.HIGH));
    }

    @Test
    public void getPriority_submitterOverride() throws FlowStoreServiceConnectorException {
        Submitter submitter = new SubmitterBuilder()
                .setContent(new SubmitterContentBuilder()
                        .setPriority(Priority.LOW)
                        .build())
                .build();
        FlowBinder flowBinder = new FlowBinderBuilder()
                .setContent(new FlowBinderContentBuilder()
                        .setPriority(Priority.HIGH)
                        .build())
                .build();
        Flow flow = new FlowBuilder().build();
        Sink sink = new SinkBuilder().build();
        AddJobParam addJobParam = getAddJobParamWithAllParametersSet(submitter, flow, sink, flowBinder);
        assertThat(addJobParam.getPriority(), is(Priority.LOW));
    }

    private AddJobParam constructAddJobParam() {
        JobInputStream jobInputStream = new JobInputStream(jobSpecification, true, 2);
        return new AddJobParam(jobInputStream, mockedFlowStoreServiceConnector);
    }

    private AddJobParam constructAddJobParam(JobSpecification jobSpecification) {
        JobInputStream jobInputStream = new JobInputStream(jobSpecification, true, 2);
        return new AddJobParam(jobInputStream, mockedFlowStoreServiceConnector);
    }

    private void assertIsDatafileValidForInvalidDatafile(AddJobParam addJobParam) {
        List<Diagnostic> diagnostics = addJobParam.getDiagnostics();
        assertThat(diagnostics.size(), is(1));
        assertThat(diagnostics.get(0).getLevel(), is(Diagnostic.Level.FATAL));
        verifyNoInteractions(mockedFlowStoreServiceConnector);
        assertThat(addJobParam.getFlowStoreReferences(), is(new FlowStoreReferences()));
    }

    private FlowStoreReferences getFlowStoreReferencesWithAllReferencesSet(Submitter submitter, Flow flow, Sink sink, FlowBinder flowBinder) {
        FlowStoreReferences flowStoreReferences = new FlowStoreReferences();

        flowStoreReferences.setReference(
                FlowStoreReferences.Elements.SUBMITTER,
                new FlowStoreReference(submitter.getId(), submitter.getVersion(), submitter.getContent().getName()));

        flowStoreReferences.setReference(
                FlowStoreReferences.Elements.FLOW_BINDER,
                new FlowStoreReference(flowBinder.getId(), flowBinder.getVersion(), flowBinder.getContent().getName()));

        flowStoreReferences.setReference(
                FlowStoreReferences.Elements.FLOW,
                new FlowStoreReference(flow.getId(), flow.getVersion(), flow.getContent().getName()));

        flowStoreReferences.setReference(
                FlowStoreReferences.Elements.SINK,
                new FlowStoreReference(sink.getId(), sink.getVersion(), sink.getContent().getName()));

        return flowStoreReferences;
    }

    private AddJobParam getAddJobParamWithAllParametersSet(Submitter submitter, Flow flow, Sink sink, FlowBinder flowBinder) throws FlowStoreServiceConnectorException {

        when(mockedFlowStoreServiceConnector.getSubmitterBySubmitterNumber(eq(jobSpecification.getSubmitterId()))).thenReturn(submitter);

        when(mockedFlowStoreServiceConnector.getFlowBinder(
                eq(jobSpecification.getPackaging()),
                eq(jobSpecification.getFormat()),
                eq(jobSpecification.getCharset()),
                eq(jobSpecification.getSubmitterId()),
                eq(jobSpecification.getDestination()))).thenReturn(flowBinder);

        when(mockedFlowStoreServiceConnector.getFlow(eq(flowBinder.getContent().getFlowId()))).thenReturn(flow);
        when(mockedFlowStoreServiceConnector.getSink(eq(flowBinder.getContent().getSinkId()))).thenReturn(sink);

        return constructAddJobParam();
    }
}
