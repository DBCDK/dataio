package dk.dbc.dataio.gui.server.modelmappers;

import dk.dbc.dataio.commons.types.JobSpecification;
import dk.dbc.dataio.commons.utils.test.model.JobSpecificationBuilder;
import dk.dbc.dataio.gui.client.model.JobModel;
import dk.dbc.dataio.jobstore.test.types.JobInfoSnapshotBuilder;
import dk.dbc.dataio.jobstore.types.FlowStoreReference;
import dk.dbc.dataio.jobstore.types.FlowStoreReferences;
import dk.dbc.dataio.jobstore.types.JobInfoSnapshot;
import dk.dbc.dataio.jobstore.types.State;
import dk.dbc.dataio.jobstore.types.StateElement;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.when;

/**
 * JobModelMapper unit tests
 *
 * The test methods of this class uses the following naming convention:
 *
 *  unitOfWork_stateUnderTest_expectedBehavior
 */
public class JobModelMapperTest {

    @Mock State mockedState;
    @Mock StateElement mockedDeliveringStateElement;
    @Mock StateElement mockedProcessingStateElement;
    @Mock StateElement mockedPartitioningStateElement;
    @Mock FlowStoreReferences mockedFlowStoreReferences;
    @Mock FlowStoreReference mockedFlowBinderFlowStoreReference;
    @Mock FlowStoreReference mockedFlowFlowStoreReference;
    @Mock FlowStoreReference mockedSinkFlowStoreReference;
    @Mock FlowStoreReference mockedSubmitterFlowStoreReference;
    @Mock State mockedState2;
    @Mock StateElement mockedDeliveringStateElement2;
    @Mock StateElement mockedProcessingStateElement2;
    @Mock StateElement mockedPartitioningStateElement2;
    @Mock FlowStoreReferences mockedFlowStoreReferences2;
    @Mock FlowStoreReference mockedFlowBinderFlowStoreReference2;
    @Mock FlowStoreReference mockedFlowFlowStoreReference2;
    @Mock FlowStoreReference mockedSinkFlowStoreReference2;
    @Mock FlowStoreReference mockedSubmitterFlowStoreReference2;
    JobSpecification testJobSpecification;
    JobInfoSnapshot testJobInfoSnapshot;
    JobSpecification testJobSpecification2;
    JobInfoSnapshot testJobInfoSnapshot2;
    List<JobInfoSnapshot> testJobInfoSnapshots;

    @Before
    public void setupMocksAndTestData() {
        MockitoAnnotations.initMocks(this);

        testJobSpecification = new JobSpecificationBuilder()
                .setCharset("charseT")
                .setDataFile("/tmp/datafilE")
                .setDestination("destinatioN")
                .setFormat("formaT")
                .setMailForNotificationAboutProcessing("mail4ProcessinG")
                .setMailForNotificationAboutVerification("mail4VerificatioN")
                .setPackaging("packaginG")
                .setResultmailInitials("mailInitialS")
                .setSubmitterId(64646L)
                .build();
        testJobInfoSnapshot = new JobInfoSnapshotBuilder()
                .setSpecification(testJobSpecification)
                .setFlowStoreReferences(mockedFlowStoreReferences)
                .setState(mockedState)
                .setJobId(1748)
                .setEoj(true)
                .setNumberOfChunks(3245)
                .setNumberOfItems(48)
                .setPartNumber(444)
                .setTimeOfCreation(newDate(2015, 4, 6, 13, 1, 23))
                .setTimeOfCompletion(newDate(2015, 4, 7, 13, 1, 23))
                .setTimeOfLastModification(newDate(2015, 4, 8, 13, 1, 23))
                .build();
        when(mockedState.allPhasesAreDone()).thenReturn(true);
        when(mockedState.getPhase(State.Phase.DELIVERING)).thenReturn(mockedDeliveringStateElement);
        when(mockedState.getPhase(State.Phase.PROCESSING)).thenReturn(mockedProcessingStateElement);
        when(mockedState.getPhase(State.Phase.PARTITIONING)).thenReturn(mockedPartitioningStateElement);
        when(mockedDeliveringStateElement.getSucceeded()).thenReturn(123);
        when(mockedDeliveringStateElement.getFailed()).thenReturn(33);
        when(mockedDeliveringStateElement.getIgnored()).thenReturn(643);
        when(mockedProcessingStateElement.getSucceeded()).thenReturn(43);
        when(mockedProcessingStateElement.getFailed()).thenReturn(66);
        when(mockedProcessingStateElement.getIgnored()).thenReturn(8323);
        when(mockedPartitioningStateElement.getSucceeded()).thenReturn(564);
        when(mockedPartitioningStateElement.getFailed()).thenReturn(4);
        when(mockedPartitioningStateElement.getIgnored()).thenReturn(567);
        when(mockedFlowStoreReferences.getReference(FlowStoreReferences.Elements.FLOW_BINDER)).thenReturn(mockedFlowBinderFlowStoreReference);
        when(mockedFlowStoreReferences.getReference(FlowStoreReferences.Elements.FLOW)).thenReturn(mockedFlowFlowStoreReference);
        when(mockedFlowStoreReferences.getReference(FlowStoreReferences.Elements.SINK)).thenReturn(mockedSinkFlowStoreReference);
        when(mockedFlowStoreReferences.getReference(FlowStoreReferences.Elements.SUBMITTER)).thenReturn(mockedSubmitterFlowStoreReference);
        when(mockedFlowBinderFlowStoreReference.getName()).thenReturn("flowbindeR");
        when(mockedFlowFlowStoreReference.getName()).thenReturn("floW");
        when(mockedSinkFlowStoreReference.getName()).thenReturn("sinK");
        when(mockedSubmitterFlowStoreReference.getName()).thenReturn("submitteR");

        testJobSpecification2 = new JobSpecificationBuilder()
                .setCharset("charseT2")
                .setDataFile("/tmp/datafilE2")
                .setDestination("destinatioN2")
                .setFormat("formaT2")
                .setMailForNotificationAboutProcessing("mail4ProcessinG2")
                .setMailForNotificationAboutVerification("mail4VerificatioN2")
                .setPackaging("packaginG2")
                .setResultmailInitials("mailInitialS2")
                .setSubmitterId(64647L)
                .build();
        testJobInfoSnapshot2 = new JobInfoSnapshotBuilder()
                .setSpecification(testJobSpecification2)
                .setFlowStoreReferences(mockedFlowStoreReferences2)
                .setState(mockedState2)
                .setJobId(1749)
                .setEoj(true)
                .setNumberOfChunks(3246)
                .setNumberOfItems(49)
                .setPartNumber(445)
                .setTimeOfCreation(newDate(2015, 4, 6, 13, 1, 24))
                .setTimeOfCompletion(newDate(2015, 4, 7, 13, 1, 24))
                .setTimeOfLastModification(newDate(2015, 4, 8, 13, 1, 24))
                .build();
        when(mockedState2.allPhasesAreDone()).thenReturn(true);
        when(mockedState2.getPhase(State.Phase.DELIVERING)).thenReturn(mockedDeliveringStateElement2);
        when(mockedState2.getPhase(State.Phase.PROCESSING)).thenReturn(mockedProcessingStateElement2);
        when(mockedState2.getPhase(State.Phase.PARTITIONING)).thenReturn(mockedPartitioningStateElement2);
        when(mockedDeliveringStateElement2.getSucceeded()).thenReturn(124);
        when(mockedDeliveringStateElement2.getFailed()).thenReturn(34);
        when(mockedDeliveringStateElement2.getIgnored()).thenReturn(644);
        when(mockedProcessingStateElement2.getSucceeded()).thenReturn(44);
        when(mockedProcessingStateElement2.getFailed()).thenReturn(67);
        when(mockedProcessingStateElement2.getIgnored()).thenReturn(8324);
        when(mockedPartitioningStateElement2.getSucceeded()).thenReturn(565);
        when(mockedPartitioningStateElement2.getFailed()).thenReturn(5);
        when(mockedPartitioningStateElement2.getIgnored()).thenReturn(568);
        when(mockedFlowStoreReferences2.getReference(FlowStoreReferences.Elements.FLOW_BINDER)).thenReturn(mockedFlowBinderFlowStoreReference2);
        when(mockedFlowStoreReferences2.getReference(FlowStoreReferences.Elements.FLOW)).thenReturn(mockedFlowFlowStoreReference2);
        when(mockedFlowStoreReferences2.getReference(FlowStoreReferences.Elements.SINK)).thenReturn(mockedSinkFlowStoreReference2);
        when(mockedFlowStoreReferences2.getReference(FlowStoreReferences.Elements.SUBMITTER)).thenReturn(mockedSubmitterFlowStoreReference2);
        when(mockedFlowBinderFlowStoreReference2.getName()).thenReturn("flowbindeR2");
        when(mockedFlowFlowStoreReference2.getName()).thenReturn("floW2");
        when(mockedSinkFlowStoreReference2.getName()).thenReturn("sinK2");
        when(mockedSubmitterFlowStoreReference2.getName()).thenReturn("submitteR2");
        testJobInfoSnapshots = Arrays.asList(testJobInfoSnapshot, testJobInfoSnapshot2);
    }



    /*
     * Tests start here...
     */

    @Test(expected = NullPointerException.class)
    public void toModel_nullInput_throws() {
        // Subject Under Test
        JobModelMapper.toModel((JobInfoSnapshot)null);
    }

    @Test(expected = NullPointerException.class)
    public void toModels_nullInput_throws() {
        // Subject Under Test
        JobModelMapper.toModel((List<JobInfoSnapshot>) null);
    }

    @Test
    public void toModel_validInput_validOutput() {
        // Subject Under Test
        JobModel jobModel = JobModelMapper.toModel(testJobInfoSnapshot);

        // Test Verification
        assertThat(jobModel.getJobCreationTime(), is("2015-04-06 13:01:23"));
        assertThat(jobModel.getJobId(), is("1748"));
        assertThat(jobModel.getFileName(), is("datafilE"));
        assertThat(jobModel.getSubmitterNumber(), is("64646"));
        assertThat(jobModel.getSubmitterName(), is("submitteR"));
        assertThat(jobModel.getFlowBinderName(), is("flowbindeR"));
        assertThat(jobModel.getSinkName(), is("sinK"));
        assertThat(jobModel.isJobDone(), is(true));
        assertThat(jobModel.getItemCounter(), is(564L + 4L + 567L));
        assertThat(jobModel.getSucceededCounter(), is(123L));
        assertThat(jobModel.getFailedCounter(), is(33L + 66L + 4L));
        assertThat(jobModel.getIgnoredCounter(), is(643L));
        assertThat(jobModel.getPartitionedCounter(), is(565L - 1 + 5L - 1 + 568L - 1));
        assertThat(jobModel.getProcessedCounter(), is(44L - 1 + 67L - 1 + 8324L - 1));
        assertThat(jobModel.getDeliveredCounter(), is(124L - 1 + 34L - 1 + 644L - 1));
        assertThat(jobModel.getPackaging(), is("packaginG"));
        assertThat(jobModel.getFormat(), is("formaT"));
        assertThat(jobModel.getCharset(), is("charseT"));
        assertThat(jobModel.getDestination(), is("destinatioN"));
        assertThat(jobModel.getMailForNotificationAboutVerification(), is("mail4VerificatioN"));
        assertThat(jobModel.getMailForNotificationAboutProcessing(), is("mail4ProcessinG"));
        assertThat(jobModel.getResultmailInitials(), is("mailInitialS"));
    }

    @Test
    public void toModel_nullSubmitterReference_emptySubmitterName() {
        // Subject Under Test
        when(mockedFlowStoreReferences.getReference(FlowStoreReferences.Elements.SUBMITTER)).thenReturn(null);
        JobModel jobModel = JobModelMapper.toModel(testJobInfoSnapshot);

        // Test Verification
        assertThat(jobModel.getSubmitterName(), is(""));
        assertThat(jobModel.getFlowBinderName(), is("flowbindeR"));
        assertThat(jobModel.getSinkName(), is("sinK"));
    }

    @Test
    public void toModel_nullFlowBinderReference_emptyFlowBinderName() {
        // Subject Under Test
        when(mockedFlowStoreReferences.getReference(FlowStoreReferences.Elements.FLOW_BINDER)).thenReturn(null);
        JobModel jobModel = JobModelMapper.toModel(testJobInfoSnapshot);

        // Test Verification
        assertThat(jobModel.getSubmitterName(), is("submitteR"));
        assertThat(jobModel.getFlowBinderName(), is(""));
        assertThat(jobModel.getSinkName(), is("sinK"));
    }

    @Test
    public void toModel_nullSinkReference_emptySinkName() {
        // Subject Under Test
        when(mockedFlowStoreReferences.getReference(FlowStoreReferences.Elements.SINK)).thenReturn(null);
        JobModel jobModel = JobModelMapper.toModel(testJobInfoSnapshot);

        // Test Verification
        assertThat(jobModel.getSubmitterName(), is("submitteR"));
        assertThat(jobModel.getFlowBinderName(), is("flowbindeR"));
        assertThat(jobModel.getSinkName(), is(""));
    }

    @Test
    public void toModel_deliveringSucceededCountIsZero_processingSucceededCount() {
        // Subject Under Test
        when(mockedDeliveringStateElement.getSucceeded()).thenReturn(0);
        when(mockedProcessingStateElement.getSucceeded()).thenReturn(43);
        when(mockedPartitioningStateElement.getSucceeded()).thenReturn(564);
        JobModel jobModel = JobModelMapper.toModel(testJobInfoSnapshot);

        // Test Verification
        assertThat(jobModel.getSucceededCounter(), is(43L));
    }

    @Test
    public void toModel_deliveringAndProcessingSucceededCountsAreZero_partitioningSucceededCount() {
        // Subject Under Test
        when(mockedDeliveringStateElement.getSucceeded()).thenReturn(0);
        when(mockedProcessingStateElement.getSucceeded()).thenReturn(0);
        when(mockedPartitioningStateElement.getSucceeded()).thenReturn(564);
        JobModel jobModel = JobModelMapper.toModel(testJobInfoSnapshot);

        // Test Verification
        assertThat(jobModel.getSucceededCounter(), is(564L));
    }

    @Test
    public void toModel_allSucceededCountsAreZero_zeroCount() {
        // Subject Under Test
        when(mockedDeliveringStateElement.getSucceeded()).thenReturn(0);
        when(mockedProcessingStateElement.getSucceeded()).thenReturn(0);
        when(mockedPartitioningStateElement.getSucceeded()).thenReturn(0);
        JobModel jobModel = JobModelMapper.toModel(testJobInfoSnapshot);

        // Test Verification
        assertThat(jobModel.getSucceededCounter(), is(0L));
    }

    @Test
    public void toModel_deliveringIgnoredCountIsZero_processingIgnoredCount() {
        // Subject Under Test
        when(mockedDeliveringStateElement.getIgnored()).thenReturn(0);
        when(mockedProcessingStateElement.getIgnored()).thenReturn(8323);
        when(mockedPartitioningStateElement.getIgnored()).thenReturn(567);
        JobModel jobModel = JobModelMapper.toModel(testJobInfoSnapshot);

        // Test Verification
        assertThat(jobModel.getIgnoredCounter(), is(8323L));
    }

    @Test
    public void toModel_deliveringAndProcessingIgnoredCountsAreZero_partitioningIgnoredCount() {
        // Subject Under Test
        when(mockedDeliveringStateElement.getIgnored()).thenReturn(0);
        when(mockedProcessingStateElement.getIgnored()).thenReturn(0);
        when(mockedPartitioningStateElement.getIgnored()).thenReturn(567);
        JobModel jobModel = JobModelMapper.toModel(testJobInfoSnapshot);

        // Test Verification
        assertThat(jobModel.getIgnoredCounter(), is(567L));
    }

    @Test
    public void toModel_allIgnoredCountsAreZero_zeroCount() {
        // Subject Under Test
        when(mockedDeliveringStateElement.getIgnored()).thenReturn(0);
        when(mockedProcessingStateElement.getIgnored()).thenReturn(0);
        when(mockedPartitioningStateElement.getIgnored()).thenReturn(0);
        JobModel jobModel = JobModelMapper.toModel(testJobInfoSnapshot);

        // Test Verification
        assertThat(jobModel.getIgnoredCounter(), is(0L));
    }

    @Test
    public void toModelWithList_validInput_validOutput() {
        // Subject Under Test
        List<JobModel> jobModels = JobModelMapper.toModel(testJobInfoSnapshots);

        // Test Verification
        assertThat(jobModels.size(), is(2));
        assertThat(jobModels.get(0).getJobCreationTime(), is("2015-04-06 13:01:23"));
        assertThat(jobModels.get(0).getJobId(), is("1748"));
        assertThat(jobModels.get(0).getFileName(), is("datafilE"));
        assertThat(jobModels.get(0).getSubmitterNumber(), is("64646"));
        assertThat(jobModels.get(0).getSubmitterName(), is("submitteR"));
        assertThat(jobModels.get(0).getFlowBinderName(), is("flowbindeR"));
        assertThat(jobModels.get(0).getSinkName(), is("sinK"));
        assertThat(jobModels.get(0).isJobDone(), is(true));
        assertThat(jobModels.get(0).getItemCounter(), is(564L + 4L + 567L));
        assertThat(jobModels.get(0).getSucceededCounter(), is(123L));
        assertThat(jobModels.get(0).getFailedCounter(), is(33L + 66L + 4L));
        assertThat(jobModels.get(0).getIgnoredCounter(), is(643L));
        assertThat(jobModels.get(0).getPackaging(), is("packaginG"));
        assertThat(jobModels.get(0).getFormat(), is("formaT"));
        assertThat(jobModels.get(0).getCharset(), is("charseT"));
        assertThat(jobModels.get(0).getDestination(), is("destinatioN"));
        assertThat(jobModels.get(0).getMailForNotificationAboutVerification(), is("mail4VerificatioN"));
        assertThat(jobModels.get(0).getMailForNotificationAboutProcessing(), is("mail4ProcessinG"));
        assertThat(jobModels.get(0).getResultmailInitials(), is("mailInitialS"));
        assertThat(jobModels.get(1).getJobCreationTime(), is("2015-04-06 13:01:24"));
        assertThat(jobModels.get(1).getJobId(), is("1749"));
        assertThat(jobModels.get(1).getFileName(), is("datafilE2"));
        assertThat(jobModels.get(1).getSubmitterNumber(), is("64647"));
        assertThat(jobModels.get(1).getSubmitterName(), is("submitteR2"));
        assertThat(jobModels.get(1).getFlowBinderName(), is("flowbindeR2"));
        assertThat(jobModels.get(1).getSinkName(), is("sinK2"));
        assertThat(jobModels.get(1).isJobDone(), is(true));
        assertThat(jobModels.get(1).getItemCounter(), is(565L + 5L + 568L));
        assertThat(jobModels.get(1).getSucceededCounter(), is(124L));
        assertThat(jobModels.get(1).getFailedCounter(), is(34L + 67L + 5L));
        assertThat(jobModels.get(1).getIgnoredCounter(), is(644L));
        assertThat(jobModels.get(1).getPackaging(), is("packaginG2"));
        assertThat(jobModels.get(1).getFormat(), is("formaT2"));
        assertThat(jobModels.get(1).getCharset(), is("charseT2"));
        assertThat(jobModels.get(1).getDestination(), is("destinatioN2"));
        assertThat(jobModels.get(1).getMailForNotificationAboutVerification(), is("mail4VerificatioN2"));
        assertThat(jobModels.get(1).getMailForNotificationAboutProcessing(), is("mail4ProcessinG2"));
        assertThat(jobModels.get(1).getResultmailInitials(), is("mailInitialS2"));

    }


    /*
     * Private methods
     */
    private Date newDate(int year, int month, int day, int hour, int minute, int second) {
        Calendar cal = Calendar.getInstance();
        cal.set(Calendar.YEAR, year);
        cal.set(Calendar.MONTH, month-1);
        cal.set(Calendar.DAY_OF_MONTH, day);
        cal.set(Calendar.HOUR_OF_DAY, hour);
        cal.set(Calendar.MINUTE, minute);
        cal.set(Calendar.SECOND, second);
        return cal.getTime();
    }
}
