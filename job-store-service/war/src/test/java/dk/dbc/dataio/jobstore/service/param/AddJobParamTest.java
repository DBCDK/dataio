package dk.dbc.dataio.jobstore.service.param;

import dk.dbc.dataio.common.utils.flowstore.FlowStoreServiceConnector;
import dk.dbc.dataio.common.utils.flowstore.FlowStoreServiceConnectorException;
import dk.dbc.dataio.commons.types.FileStoreUrn;
import dk.dbc.dataio.commons.types.Flow;
import dk.dbc.dataio.commons.types.FlowBinder;
import dk.dbc.dataio.commons.types.FlowBinderContent;
import dk.dbc.dataio.commons.types.JobSpecification;
import dk.dbc.dataio.commons.types.RecordSplitterConstants;
import dk.dbc.dataio.commons.types.Sink;
import dk.dbc.dataio.commons.types.Submitter;
import dk.dbc.dataio.commons.utils.test.model.FlowBinderBuilder;
import dk.dbc.dataio.commons.utils.test.model.FlowBinderContentBuilder;
import dk.dbc.dataio.commons.utils.test.model.FlowBuilder;
import dk.dbc.dataio.commons.utils.test.model.JobSpecificationBuilder;
import dk.dbc.dataio.commons.utils.test.model.SinkBuilder;
import dk.dbc.dataio.commons.utils.test.model.SubmitterBuilder;
import dk.dbc.dataio.filestore.service.connector.FileStoreServiceConnector;
import dk.dbc.dataio.filestore.service.connector.FileStoreServiceConnectorException;
import dk.dbc.dataio.jobstore.service.partitioner.DefaultXmlDataPartitionerFactory;
import dk.dbc.dataio.jobstore.service.partitioner.Iso2709DataPartitionerFactory;
import dk.dbc.dataio.jobstore.types.Diagnostic;
import dk.dbc.dataio.jobstore.types.FlowStoreReference;
import dk.dbc.dataio.jobstore.types.FlowStoreReferences;
import dk.dbc.dataio.jobstore.types.JobInputStream;
import org.junit.Test;

import java.io.InputStream;
import java.net.URISyntaxException;
import java.util.List;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class AddJobParamTest {
    private static final String ERROR_MESSAGE = "Error Message";
    private static final String DATA_FILE_ID = "42";

    private static final FileStoreUrn FILE_STORE_URN;
    private static final JobSpecification jobSpecification;

    private final FlowStoreServiceConnector mockedFlowStoreServiceConnector = mock(FlowStoreServiceConnector.class);
    private final FileStoreServiceConnector mockedFileStoreServiceConnector = mock(FileStoreServiceConnector.class);

    static {
        try {
            FILE_STORE_URN = FileStoreUrn.create(DATA_FILE_ID);
            jobSpecification = new JobSpecificationBuilder().setDataFile(FILE_STORE_URN.toString()).build();
        } catch (URISyntaxException e) {
            throw new IllegalStateException(e);
        }
    }

    @Test
    public void constructor_inputStreamArgIsNull_throws() {
        try {
            new AddJobParam(null, mockedFlowStoreServiceConnector, mockedFileStoreServiceConnector);
            fail("No exception thrown");
        } catch (NullPointerException e) {
        }
    }

    @Test
    public void constructor_flowStoreServiceConnectorArgIsNull_throws() {
        final JobInputStream jobInputStream = new JobInputStream(jobSpecification, true, 2);
        try {
            new AddJobParam(jobInputStream, null, mockedFileStoreServiceConnector);
            fail("No exception thrown");
        } catch (NullPointerException e) {
        }
    }

    @Test
    public void constructor_fileStoreServiceConnectorArgIsNull_throws() {
        final JobInputStream jobInputStream = new JobInputStream(jobSpecification, true, 2);
        try {
            new AddJobParam(jobInputStream, mockedFlowStoreServiceConnector, null);
            fail("No exception thrown");
        } catch (NullPointerException e) {
        }
    }

    @Test
    public void constructor_allArgsAreValid_returnsAddJobParam() {
        final JobInputStream jobInputStream = new JobInputStream(jobSpecification, true, 2);
        final AddJobParam addJobParam = new AddJobParam(jobInputStream, mockedFlowStoreServiceConnector, mockedFileStoreServiceConnector);
        assertThat(addJobParam, is(notNullValue()));

        assertThat(addJobParam.diagnostics, is(notNullValue()));
        assertThat(addJobParam.diagnostics.size(), is(0));

        assertThat(addJobParam.flowStoreReferences, is(notNullValue()));
        assertThat(addJobParam.flowStoreReferences, is(new FlowStoreReferences()));
    }

    @Test
    public void extractDataFileIdFromURN_invalidUrn_diagnosticLevelFatalAddedForUrnAndDataFileInputStream() throws FlowStoreServiceConnectorException, FileStoreServiceConnectorException {
        when(mockedFileStoreServiceConnector.getFile(null)).thenThrow(new NullPointerException());

        final AddJobParam addJobParam = constructAddJobParam(false);

        final List<Diagnostic> diagnostics = addJobParam.getDiagnostics();

        assertThat(diagnostics.size(), is(1));
        assertThat(diagnostics.get(0).getLevel(), is(Diagnostic.Level.FATAL));
        assertThat(diagnostics.get(0).getMessage().contains(addJobParam.jobInputStream.getJobSpecification().getDataFile()), is(true));

        assertThat(addJobParam.dataFileId, is(nullValue()));
        assertThat(addJobParam.dataFileInputStream, is(nullValue()));
        assertThat(addJobParam.flowStoreReferences, is(new FlowStoreReferences()));
    }

    @Test
    public void extractDataFileIdFromURN_validUrnAndFileFound_dataFileIdAndDataFileInputStreamAndDataPartitionerSet() throws FlowStoreServiceConnectorException, FileStoreServiceConnectorException {

        InputStream mockedInputStream = mock(InputStream.class);
        when(mockedFileStoreServiceConnector.getFile(anyString())).thenReturn(mockedInputStream);

        final FlowBinder flowBinder = new FlowBinderBuilder().build();
        when(mockedFlowStoreServiceConnector.getFlowBinder(
                eq(jobSpecification.getPackaging()),
                eq(jobSpecification.getFormat()),
                eq(jobSpecification.getCharset()),
                eq(jobSpecification.getSubmitterId()),
                eq(jobSpecification.getDestination()))).thenReturn(flowBinder);

        final AddJobParam addJobParam = constructAddJobParam(true);

        final List<Diagnostic> diagnostics = addJobParam.diagnostics;
        assertThat(diagnostics.size(), is(0));
        assertThat(addJobParam.dataFileId, is(notNullValue()));
        assertThat(addJobParam.dataFileInputStream, is(notNullValue()));
        assertThat(addJobParam.dataPartitioner, is(notNullValue()));
    }

    @Test
    public void newDataFileInputStream_fileNotFound_diagnosticLevelFatalAddedForInputStream() throws FlowStoreServiceConnectorException, FileStoreServiceConnectorException {

        when(mockedFileStoreServiceConnector.getFile(anyString())).thenThrow(new FileStoreServiceConnectorException(ERROR_MESSAGE));

        final AddJobParam addJobParam = constructAddJobParam(true);

        final List<Diagnostic> diagnostics = addJobParam.diagnostics;
        assertThat(diagnostics.size(), is(1));
        assertThat(diagnostics.get(0).getLevel(), is(Diagnostic.Level.FATAL));
        assertThat(diagnostics.get(0).getMessage().contains(jobSpecification.getDataFile()), is(true));
    }

    @Test
    public void lookupSubmitter_submitterNotFound_diagnosticLevelFatalAddedAndReferenceIsNull() throws FlowStoreServiceConnectorException, FileStoreServiceConnectorException {

        when(mockedFlowStoreServiceConnector.getSubmitterBySubmitterNumber(
                eq(jobSpecification.getSubmitterId()))).thenThrow(new FlowStoreServiceConnectorException(ERROR_MESSAGE));

        final AddJobParam addJobParam = constructAddJobParam(true);

        final List<Diagnostic> diagnostics = addJobParam.diagnostics;
        assertThat(diagnostics.size(), is(1));
        assertThat(diagnostics.get(0).getLevel(), is(Diagnostic.Level.FATAL));
        assertThat(diagnostics.get(0).getMessage().contains(Long.valueOf(jobSpecification.getSubmitterId()).toString()), is(true));
        assertThat(addJobParam.submitter, is(nullValue()));
        assertThat(addJobParam.flowStoreReferences, is(new FlowStoreReferences()));
    }

    @Test
    public void lookupSubmitter_submitterFound_submitterReferenceExists() throws FlowStoreServiceConnectorException, FileStoreServiceConnectorException {
        final Submitter submitter = new SubmitterBuilder().build();

        when(mockedFlowStoreServiceConnector.getSubmitterBySubmitterNumber(
                eq(jobSpecification.getSubmitterId()))).thenReturn(submitter);

        final AddJobParam addJobParam = constructAddJobParam(true);

        assertThat(addJobParam.diagnostics.size(), is(0));
        assertThat(addJobParam.submitter, is(notNullValue()));
        final FlowStoreReferences flowStoreReferences = new FlowStoreReferences();
        final FlowStoreReference submitterReference = new FlowStoreReference(submitter.getId(), submitter.getVersion(), submitter.getContent().getName());
        flowStoreReferences.setReference(FlowStoreReferences.Elements.SUBMITTER, submitterReference);
        assertThat(addJobParam.flowStoreReferences, is(flowStoreReferences));
    }

    @Test
    public void lookupFlowBinder_flowBinderNotFound_diagnosticLevelFatalAddedAndReferenceIsNull() throws FlowStoreServiceConnectorException, FileStoreServiceConnectorException {

        when(mockedFlowStoreServiceConnector.getFlowBinder(
                eq(jobSpecification.getPackaging()),
                eq(jobSpecification.getFormat()),
                eq(jobSpecification.getCharset()),
                eq(jobSpecification.getSubmitterId()),
                eq(jobSpecification.getDestination()))).thenThrow(new FlowStoreServiceConnectorException(ERROR_MESSAGE));

        final AddJobParam addJobParam = constructAddJobParam(true);

        final List<Diagnostic> diagnostics = addJobParam.diagnostics;
        assertThat(diagnostics.size(), is(1));
        assertThat(diagnostics.get(0).getLevel(), is(Diagnostic.Level.FATAL));
        assertThat(diagnostics.get(0).getMessage().contains(jobSpecification.toString()), is(true));
        assertThat(addJobParam.flowBinder, is(nullValue()));
        assertThat(addJobParam.flowStoreReferences, is(new FlowStoreReferences()));
    }

    @Test
    public void lookupFlowBinder_flowBinderFound_flowBinderReferenceExists() throws FlowStoreServiceConnectorException, FileStoreServiceConnectorException {
        final FlowBinder flowBinder = new FlowBinderBuilder().build();

        when(mockedFlowStoreServiceConnector.getFlowBinder(
                eq(jobSpecification.getPackaging()),
                eq(jobSpecification.getFormat()),
                eq(jobSpecification.getCharset()),
                eq(jobSpecification.getSubmitterId()),
                eq(jobSpecification.getDestination()))).thenReturn(flowBinder);

        final AddJobParam addJobParam = constructAddJobParam(true);

        assertThat(addJobParam.diagnostics.size(), is(0));
        assertThat(addJobParam.flowBinder, is(notNullValue()));

        final FlowStoreReferences flowStoreReferences = new FlowStoreReferences();
        final FlowStoreReference flowBinderReference = new FlowStoreReference(flowBinder.getId(), flowBinder.getVersion(), flowBinder.getContent().getName());
        flowStoreReferences.setReference(FlowStoreReferences.Elements.FLOW_BINDER, flowBinderReference);
        assertThat(addJobParam.flowStoreReferences, is(flowStoreReferences));
    }

    @Test
    public void lookupFlow_flowNotFound_diagnosticLevelFatalAddedAndReferenceIsNull() throws FlowStoreServiceConnectorException, FileStoreServiceConnectorException {
        final FlowBinder flowBinder = new FlowBinderBuilder().build();

        when(mockedFlowStoreServiceConnector.getFlowBinder(
                eq(jobSpecification.getPackaging()),
                eq(jobSpecification.getFormat()),
                eq(jobSpecification.getCharset()),
                eq(jobSpecification.getSubmitterId()),
                eq(jobSpecification.getDestination()))).thenReturn(flowBinder);

        when(mockedFlowStoreServiceConnector.getFlow(eq(flowBinder.getContent().getFlowId()))).thenThrow(new FlowStoreServiceConnectorException(ERROR_MESSAGE));

        final AddJobParam addJobParam = constructAddJobParam(true);

        final List<Diagnostic> diagnostics = addJobParam.getDiagnostics();
        assertThat(diagnostics.size(), is(1));
        assertThat(diagnostics.get(0).getLevel(), is(Diagnostic.Level.FATAL));
        assertThat(diagnostics.get(0).getMessage().contains(Long.valueOf(flowBinder.getContent().getFlowId()).toString()), is(true));

        assertThat(addJobParam.flowBinder, is(notNullValue()));
        assertThat(addJobParam.flow, is(nullValue()));

        final FlowStoreReferences flowStoreReferences = new FlowStoreReferences();
        final FlowStoreReference flowBinderReference = new FlowStoreReference(flowBinder.getId(), flowBinder.getVersion(), flowBinder.getContent().getName());
        flowStoreReferences.setReference(FlowStoreReferences.Elements.FLOW_BINDER, flowBinderReference);
        assertThat(addJobParam.flowStoreReferences, is(flowStoreReferences));
    }

    @Test
    public void lookupFlow_flowFound_flowBinderAndFlowReferenceExists() throws FlowStoreServiceConnectorException, FileStoreServiceConnectorException {
        final FlowBinder flowBinder = new FlowBinderBuilder().build();
        final Flow flow = new FlowBuilder().build();

        when(mockedFlowStoreServiceConnector.getFlowBinder(
                eq(jobSpecification.getPackaging()),
                eq(jobSpecification.getFormat()),
                eq(jobSpecification.getCharset()),
                eq(jobSpecification.getSubmitterId()),
                eq(jobSpecification.getDestination()))).thenReturn(flowBinder);

        when(mockedFlowStoreServiceConnector.getFlow(eq(flowBinder.getContent().getFlowId()))).thenReturn(flow);

        final AddJobParam addJobParam = constructAddJobParam(true);

        assertThat(addJobParam.diagnostics.size(), is(0));
        assertThat(addJobParam.flowStoreReferences, is(notNullValue()));
        assertThat(addJobParam.flow, is(notNullValue()));

        final FlowStoreReferences flowStoreReferences = new FlowStoreReferences();
        final FlowStoreReference flowBinderReference = new FlowStoreReference(flowBinder.getId(), flowBinder.getVersion(), flowBinder.getContent().getName());
        final FlowStoreReference flowReference = new FlowStoreReference(flow.getId(), flow.getVersion(), flow.getContent().getName());
        flowStoreReferences.setReference(FlowStoreReferences.Elements.FLOW_BINDER, flowBinderReference);
        flowStoreReferences.setReference(FlowStoreReferences.Elements.FLOW, flowReference);
        assertThat(addJobParam.flowStoreReferences, is(flowStoreReferences));
    }

    @Test
    public void lookupSink_sinkNotFound_diagnosticLevelFatalAddedAndReferenceIsNull() throws FlowStoreServiceConnectorException, FileStoreServiceConnectorException {
        final FlowBinder flowBinder = new FlowBinderBuilder().build();

        when(mockedFlowStoreServiceConnector.getFlowBinder(
                eq(jobSpecification.getPackaging()),
                eq(jobSpecification.getFormat()),
                eq(jobSpecification.getCharset()),
                eq(jobSpecification.getSubmitterId()),
                eq(jobSpecification.getDestination()))).thenReturn(flowBinder);

        when(mockedFlowStoreServiceConnector.getSink(eq(flowBinder.getContent().getSinkId()))).thenThrow(new FlowStoreServiceConnectorException(ERROR_MESSAGE));

        final AddJobParam addJobParam = constructAddJobParam(true);
        final List<Diagnostic> diagnostics = addJobParam.getDiagnostics();
        assertThat(diagnostics.size(), is(1));
        assertThat(diagnostics.get(0).getLevel(), is(Diagnostic.Level.FATAL));
        assertThat(diagnostics.get(0).getMessage().contains(Long.valueOf(flowBinder.getContent().getSinkId()).toString()), is(true));

        assertThat(addJobParam.flowBinder, is(notNullValue()));
        assertThat(addJobParam.sink, is(nullValue()));
        final FlowStoreReferences flowStoreReferences = new FlowStoreReferences();
        final FlowStoreReference flowStoreReference = new FlowStoreReference(flowBinder.getId(), flowBinder.getVersion(), flowBinder.getContent().getName());
        flowStoreReferences.setReference(FlowStoreReferences.Elements.FLOW_BINDER, flowStoreReference);
        assertThat(addJobParam.flowStoreReferences, is(flowStoreReferences));
    }

    @Test
    public void lookupSink_sinkFound_flowBinderAndSinkReferenceExists() throws FlowStoreServiceConnectorException, FileStoreServiceConnectorException {
        final FlowBinder flowBinder = new FlowBinderBuilder().build();
        final Sink sink = new SinkBuilder().build();

        when(mockedFlowStoreServiceConnector.getFlowBinder(
                eq(jobSpecification.getPackaging()),
                eq(jobSpecification.getFormat()),
                eq(jobSpecification.getCharset()),
                eq(jobSpecification.getSubmitterId()),
                eq(jobSpecification.getDestination()))).thenReturn(flowBinder);

        when(mockedFlowStoreServiceConnector.getSink(eq(flowBinder.getContent().getSinkId()))).thenReturn(sink);

        final AddJobParam addJobParam = constructAddJobParam(true);
        assertThat(addJobParam.diagnostics.size(), is(0));

        assertThat(addJobParam.flowBinder, is(notNullValue()));
        assertThat(addJobParam.sink, is(notNullValue()));

        final FlowStoreReferences flowStoreReferences = new FlowStoreReferences();
        final FlowStoreReference flowBinderReference = new FlowStoreReference(flowBinder.getId(), flowBinder.getVersion(), flowBinder.getContent().getName());
        final FlowStoreReference sinkReference = new FlowStoreReference(sink.getId(), sink.getVersion(), sink.getContent().getName());
        flowStoreReferences.setReference(FlowStoreReferences.Elements.FLOW_BINDER, flowBinderReference);
        flowStoreReferences.setReference(FlowStoreReferences.Elements.SINK, sinkReference);
        assertThat(addJobParam.flowStoreReferences, is(flowStoreReferences));
    }

    @Test
    public void addJobParam_allReachableParametersSet_expectedValuesReturnedThroughGetters() throws FlowStoreServiceConnectorException, FileStoreServiceConnectorException {
        final Submitter submitter = new SubmitterBuilder().build();
        final FlowBinder flowBinder = new FlowBinderBuilder().build();
        final Flow flow = new FlowBuilder().build();
        final Sink sink = new SinkBuilder().build();
        final AddJobParam addJobParam = getAddJobParamWithAllParametersSet(submitter, flow, sink, flowBinder);

        assertThat(addJobParam.getSequenceAnalyserKeyGenerator(), is(notNullValue()));
        assertThat(addJobParam.getDataPartitioner(), is(notNullValue()));
        assertThat(addJobParam.getDiagnostics().size(), is(0));
        assertThat(addJobParam.flowStoreReferences, is(getFlowStoreReferencesWithAllReferencesSet(submitter, flow, sink, flowBinder)));

        assertThat(addJobParam.getDataFileId(), is(DATA_FILE_ID));
        assertThat(addJobParam.getSubmitter(), is(submitter));
        assertThat(addJobParam.getFlowBinder(), is(flowBinder));
        assertThat(addJobParam.getFlow(), is(flow));
        assertThat(addJobParam.getSink(), is(sink));
    }

    @Test
    public void addJobParam_defaultXmlDataPartitionerSetAsParameter_ok() throws FlowStoreServiceConnectorException, FileStoreServiceConnectorException {
        final FlowBinderContent flowBinderContent = new FlowBinderContentBuilder().setRecordSplitter(RecordSplitterConstants.RecordSplitter.XML).build();
        final FlowBinder flowBinder = new FlowBinderBuilder().setContent(flowBinderContent).build();
        final AddJobParam addJobParam = getAddJobParamWithAllParametersSet(
                new SubmitterBuilder().build(),
                new FlowBuilder().build(),
                new SinkBuilder().build(),
                flowBinder);

        assertThat(addJobParam.getDataPartitioner(), is(notNullValue()));
        assertThat(addJobParam.getDataPartitioner().toString().contains(DefaultXmlDataPartitionerFactory.class.getName()), is(true));
    }

    @Test
    public void addJobParam_iso2709DataPartitionerSetAsParameter_ok() throws FlowStoreServiceConnectorException, FileStoreServiceConnectorException {
        final FlowBinderContent flowBinderContent = new FlowBinderContentBuilder().setRecordSplitter(RecordSplitterConstants.RecordSplitter.ISO2709).build();
        final FlowBinder flowBinder = new FlowBinderBuilder().setContent(flowBinderContent).build();
        final AddJobParam addJobParam = getAddJobParamWithAllParametersSet(
                new SubmitterBuilder().build(),
                new FlowBuilder().build(),
                new SinkBuilder().build(),
                flowBinder);

        assertThat(addJobParam.getDataPartitioner(), is(notNullValue()));
        assertThat(addJobParam.getDataPartitioner().toString().contains(Iso2709DataPartitionerFactory.class.getName()), is(true));
    }

    /*
     * private methods
     */

    private AddJobParam constructAddJobParam(boolean isDataFileInputStreamMocked){
        final AddJobParam addJobParam;
        final JobInputStream jobInputStream;

        if(isDataFileInputStreamMocked) {
            jobInputStream = new JobInputStream(jobSpecification, true, 2);
            addJobParam = new AddJobParam(jobInputStream, mockedFlowStoreServiceConnector, mockedFileStoreServiceConnector);
            addJobParam.dataFileInputStream = mock(InputStream.class);
        }
        else {
            JobSpecification jobSpecificationWithInvalidDataFile = new JobSpecificationBuilder().setDataFile(DATA_FILE_ID).build();
            jobInputStream = new JobInputStream(jobSpecificationWithInvalidDataFile, true, 2);
            addJobParam = new AddJobParam(jobInputStream, mockedFlowStoreServiceConnector, mockedFileStoreServiceConnector);
        }
        return addJobParam;
    }

    private FlowStoreReferences getFlowStoreReferencesWithAllReferencesSet(Submitter submitter, Flow flow, Sink sink, FlowBinder flowBinder) {
        final FlowStoreReferences flowStoreReferences = new FlowStoreReferences();

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

    private AddJobParam getAddJobParamWithAllParametersSet(Submitter submitter, Flow flow, Sink sink, FlowBinder flowBinder) throws FlowStoreServiceConnectorException, FileStoreServiceConnectorException {

        when(mockedFlowStoreServiceConnector.getSubmitterBySubmitterNumber(eq(jobSpecification.getSubmitterId()))).thenReturn(submitter);

        when(mockedFlowStoreServiceConnector.getFlowBinder(
                eq(jobSpecification.getPackaging()),
                eq(jobSpecification.getFormat()),
                eq(jobSpecification.getCharset()),
                eq(jobSpecification.getSubmitterId()),
                eq(jobSpecification.getDestination()))).thenReturn(flowBinder);

        when(mockedFlowStoreServiceConnector.getFlow(eq(flowBinder.getContent().getFlowId()))).thenReturn(flow);
        when(mockedFlowStoreServiceConnector.getSink(eq(flowBinder.getContent().getSinkId()))).thenReturn(sink);
        when(mockedFileStoreServiceConnector.getFile(anyString())).thenReturn(mock(InputStream.class));

        return constructAddJobParam(true);
    }
}
