package dk.dbc.dataio.jobstore.service.ejb;

import dk.dbc.dataio.commons.types.Flow;
import dk.dbc.dataio.commons.types.FlowBinder;
import dk.dbc.dataio.commons.types.Sink;
import dk.dbc.dataio.commons.types.Submitter;
import dk.dbc.dataio.commons.utils.test.model.FlowBinderBuilder;
import dk.dbc.dataio.commons.utils.test.model.FlowBuilder;
import dk.dbc.dataio.commons.utils.test.model.JobSpecificationBuilder;
import dk.dbc.dataio.commons.utils.test.model.SinkBuilder;
import dk.dbc.dataio.commons.utils.test.model.SubmitterBuilder;
import dk.dbc.dataio.filestore.service.connector.FileStoreServiceConnectorException;
import dk.dbc.dataio.jobstore.service.entity.JobEntity;
import dk.dbc.dataio.jobstore.service.param.AddJobParam;
import dk.dbc.dataio.jobstore.service.partitioner.DefaultXmlDataPartitionerFactory;
import dk.dbc.dataio.jobstore.test.types.FlowStoreReferencesBuilder;
import dk.dbc.dataio.jobstore.types.Diagnostic;
import dk.dbc.dataio.jobstore.types.FlowStoreReference;
import dk.dbc.dataio.jobstore.types.FlowStoreReferences;
import dk.dbc.dataio.jobstore.types.JobInfoSnapshot;
import dk.dbc.dataio.jobstore.types.JobInputStream;
import dk.dbc.dataio.jobstore.types.JobStoreException;
import dk.dbc.dataio.jobstore.types.State;
import dk.dbc.dataio.sequenceanalyser.keygenerator.SequenceAnalyserSinkKeyGenerator;
import org.junit.Test;

import javax.persistence.LockModeType;
import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.when;

public class PgJobStore_HandlePartitioningTest extends PgJobStoreBaseTest {

    private static final String ERROR_MESSAGE = "Error Message";
    private static final int EXPECTED_NUMBER_OF_CHUNKS = 2;

    @Test
    public void handlePartitioning_diagnosticWithLevelFatalFound_returnsJobInformationSnapshotWithJobMarkedAsCompleted() throws JobStoreException {
        final PgJobStore pgJobStore = newPgJobStore();

        final MockedAddJobParam mockedAddJobParam = new MockedAddJobParam();
        mockedAddJobParam.setDiagnostics(Collections.singletonList(new Diagnostic(Diagnostic.Level.FATAL, ERROR_MESSAGE)));

        //OLD JobInfoSnapshot jobInfoSnapshot = pgJobStore.addJob(mockedAddJobParam);
        JobEntity jobEntity = pgJobStore.createJobEntity(mockedAddJobParam);
        final JobInfoSnapshot jobInfoSnapshot = pgJobStore.handlePartitioning(mockedAddJobParam, pgJobStore, jobEntity);

        assertThat("JobInfoSnapshot", jobInfoSnapshot, is(notNullValue()));
        assertThat("JobInfoSnapshot.timeOfCompletion", jobInfoSnapshot.getTimeOfCompletion(), is(notNullValue()));
        assertThat("State.Phase.PARTITIONING.beginDate", jobInfoSnapshot.getState().getPhase(State.Phase.PARTITIONING).getBeginDate(), is(nullValue()));

        assertThat("JobInfoSnapshot.State.Diagnostics", jobInfoSnapshot.getState().getDiagnostics(), is(mockedAddJobParam.getDiagnostics()));
        assertThat("JobInfoSnapshot.FlowStoreReferences", jobInfoSnapshot.getFlowStoreReferences(), is(mockedAddJobParam.getFlowStoreReferences()));
    }

    @Test
    //Error reading data file {42}. DataPartitioner.byteSize was: 269. FileStore.byteSize was: 0
    public void handlePartitioning_allArgsAreValid_returnsJobInformationSnapshot() throws JobStoreException, FileStoreServiceConnectorException {
        final PgJobStore pgJobStore = newPgJobStore();
        final MockedAddJobParam mockedAddJobParam = new MockedAddJobParam();
        final FlowStoreReferences flowStoreReferences = buildFlowStoreReferences(
                mockedAddJobParam.getSubmitter(),
                mockedAddJobParam.getFlowBinder(),
                mockedAddJobParam.getFlow(),
                mockedAddJobParam.getSink());

        mockedAddJobParam.setFlowStoreReferences(flowStoreReferences);

        final TestableJobEntity jobEntity = new TestableJobEntity();

        jobEntity.setTimeOfCreation(new Timestamp(new Date().getTime()));
        jobEntity.setState(new State());
        jobEntity.setFlowStoreReferences(mockedAddJobParam.getFlowStoreReferences());
        jobEntity.setSpecification(mockedAddJobParam.getJobInputStream().getJobSpecification());

        when(entityManager.find(eq(JobEntity.class), anyInt(), eq(LockModeType.PESSIMISTIC_WRITE))).thenReturn(jobEntity);
        when(mockedFileStoreServiceConnector.getByteSize(anyString())).thenReturn(269l);


        // OLD final JobInfoSnapshot jobInfoSnapshot = pgJobStore.addJob(mockedAddJobParam);
        final JobInfoSnapshot jobInfoSnapshot = pgJobStore.handlePartitioning(mockedAddJobParam, pgJobStore, jobEntity);

        assertThat("Returned JobInfoSnapshot", jobInfoSnapshot, is(notNullValue()));
        assertThat("Number of chunks created", jobInfoSnapshot.getNumberOfChunks(), is(EXPECTED_NUMBER_OF_CHUNKS));
        assertThat("Number of items created", jobInfoSnapshot.getNumberOfItems(), is(EXPECTED_NUMBER_OF_ITEMS));
        assertThat("Partitioning phase endDate set", jobInfoSnapshot.getState().getPhase(State.Phase.PARTITIONING).getEndDate(), is(notNullValue()));
        assertThat("Time of completion not set", jobInfoSnapshot.getTimeOfCompletion(), is(nullValue()));

        assertThat("JobInfoSnapshot.State.Diagnostics", jobInfoSnapshot.getState().getDiagnostics(), is(mockedAddJobParam.getDiagnostics()));
        assertThat("JobInfoSnapshot.FlowStoreReferences",jobInfoSnapshot.getFlowStoreReferences(), is(mockedAddJobParam.getFlowStoreReferences()));
    }

    @Test
    public void handlePartitioning_diagnosticWithLevelWarningFound_returnsJobInformationSnapshot() throws JobStoreException, FileStoreServiceConnectorException {
        final PgJobStore pgJobStore = newPgJobStore();
        final MockedAddJobParam mockedAddJobParam = new MockedAddJobParam();
        mockedAddJobParam.setDiagnostics(Collections.singletonList(new Diagnostic(Diagnostic.Level.WARNING, ERROR_MESSAGE)));

        final FlowStoreReferences flowStoreReferences = buildFlowStoreReferences(
                mockedAddJobParam.getSubmitter(),
                mockedAddJobParam.getFlowBinder(),
                mockedAddJobParam.getFlow(),
                mockedAddJobParam.getSink());

        mockedAddJobParam.setFlowStoreReferences(flowStoreReferences);

        final State state = new State();
        state.getDiagnostics().addAll(mockedAddJobParam.getDiagnostics());

        final TestableJobEntity jobEntity = new TestableJobEntity();
        jobEntity.setTimeOfCreation(new Timestamp(new Date().getTime()));
        jobEntity.setState(state);
        jobEntity.setFlowStoreReferences(mockedAddJobParam.getFlowStoreReferences());
        jobEntity.setSpecification(new JobSpecificationBuilder().build());

        when(entityManager.find(eq(JobEntity.class), anyInt(), eq(LockModeType.PESSIMISTIC_WRITE))).thenReturn(jobEntity);
        when(mockedFileStoreServiceConnector.getByteSize(anyString())).thenReturn(269l);

        // OLD final JobInfoSnapshot jobInfoSnapshot = pgJobStore.addJob(mockedAddJobParam);
        final JobInfoSnapshot jobInfoSnapshot = pgJobStore.handlePartitioning(mockedAddJobParam, pgJobStore, jobEntity);

        assertThat("Returned JobInfoSnapshot", jobInfoSnapshot, is(notNullValue()));
        assertThat("Number of chunks created", jobInfoSnapshot.getNumberOfChunks(), is(EXPECTED_NUMBER_OF_CHUNKS));
        assertThat("Number of items created", jobInfoSnapshot.getNumberOfItems(), is(EXPECTED_NUMBER_OF_ITEMS));
        assertThat("Partitioning phase endDate set", jobInfoSnapshot.getState().getPhase(State.Phase.PARTITIONING).getEndDate(), is(notNullValue()));
        assertThat("Time of completion not set", jobInfoSnapshot.getTimeOfCompletion(), is(nullValue()));

        assertThat("JobInfoSnapshot.State.Diagnostics", jobInfoSnapshot.getState().getDiagnostics(), is(mockedAddJobParam.getDiagnostics()));
        assertThat("JobInfoSnapshot.FlowStoreReferences",jobInfoSnapshot.getFlowStoreReferences(), is(mockedAddJobParam.getFlowStoreReferences()));
    }

    // private methods
    private FlowStoreReferences buildFlowStoreReferences(Submitter submitter, FlowBinder flowBinder, Flow flow, Sink sink) {
        return new FlowStoreReferencesBuilder()
                .setFlowStoreReference(FlowStoreReferences.Elements.SUBMITTER,
                        new FlowStoreReference(submitter.getId(), submitter.getVersion(), submitter.getContent().getName()))
                .setFlowStoreReference(FlowStoreReferences.Elements.FLOW_BINDER,
                        new FlowStoreReference(flowBinder.getId(), flowBinder.getVersion(), flowBinder.getContent().getName()))
                .setFlowStoreReference(FlowStoreReferences.Elements.FLOW,
                        new FlowStoreReference(flow.getId(), flow.getVersion(), flow.getContent().getName()))
                .setFlowStoreReference(FlowStoreReferences.Elements.SINK,
                        new FlowStoreReference(sink.getId(), sink.getVersion(), sink.getContent().getName()))
                .build();
    }

    private class MockedAddJobParam extends AddJobParam {
        final String xml =
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
                        + "</records>";

        public MockedAddJobParam() {
            super(new JobInputStream(new JobSpecificationBuilder()
                    .setDataFile(FILE_STORE_URN.toString())
                    .build(), true, 0), mockedFlowStoreServiceConnector, mockedFileStoreServiceConnector);
            submitter = new SubmitterBuilder().build();
            flow = new FlowBuilder().build();
            sink = new SinkBuilder().build();
            flowBinder = new FlowBinderBuilder().build();
            flowStoreReferences = new FlowStoreReferencesBuilder().build();
            diagnostics = new ArrayList<>();

            sequenceAnalyserKeyGenerator = new SequenceAnalyserSinkKeyGenerator(sink);
            dataFileInputStream = new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8));
            dataPartitioner = new DefaultXmlDataPartitionerFactory().createDataPartitioner(dataFileInputStream,
                    StandardCharsets.UTF_8.name());
        }

        public void setFlowStoreReferences(FlowStoreReferences flowStoreReferences) {
            this.flowStoreReferences = flowStoreReferences;
        }

        public void setDiagnostics(List<Diagnostic> diagnostics) {
            this.diagnostics.clear();
            this.diagnostics.addAll(diagnostics);
        }
    }

}