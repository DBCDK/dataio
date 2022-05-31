package dk.dbc.dataio.gui.server.modelmappers;

import dk.dbc.dataio.commons.types.HarvesterToken;
import dk.dbc.dataio.commons.types.JobSpecification;
import dk.dbc.dataio.gui.client.model.JobModel;
import dk.dbc.dataio.gui.client.model.StateModel;
import dk.dbc.dataio.jobstore.test.types.FlowStoreReferencesBuilder;
import dk.dbc.dataio.jobstore.types.FlowStoreReferences;
import dk.dbc.dataio.jobstore.types.JobInfoSnapshot;
import dk.dbc.dataio.jobstore.types.JobInputStream;
import dk.dbc.dataio.jobstore.types.State;
import dk.dbc.dataio.jobstore.types.StateElement;
import org.junit.Test;

import java.nio.charset.StandardCharsets;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

import static dk.dbc.dataio.gui.server.modelmappers.JobModelMapper.toJobInputStream;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;

/**
 * JobModelMapper unit tests
 * <p>
 * The test methods of this class uses the following naming convention:
 * <p>
 * unitOfWork_stateUnderTest_expectedBehavior
 */
public class JobModelMapperTest {

    private JobSpecification.Ancestry ancestry = new JobSpecification.Ancestry()
            .withPreviousJobId(4321).withDetails("details".getBytes())
            .withHarvesterToken(getHarvesterToken(HarvesterToken.HarvesterVariant.RAW_REPO));

    private final JobModel testJobModel = new JobModel()
            .withPackaging("packaging")
            .withFormat("format")
            .withCharset("utf8")
            .withDestination("dest")
            .withSubmitterNumber("12345")
            .withMailForNotificationAboutVerification("mail")
            .withMailForNotificationAboutProcessing("mail")
            .withResultMailInitials("mail")
            .withDataFile("42")
            .withType(JobSpecification.Type.TEST)
            .withAncestry(ancestry)
            .withSinkId(42)
            .withSinkName("sinkName");

    /*
     * Test toJobInputStream
     */

    @Test(expected = NullPointerException.class)
    public void toModel_nullInput_throws() {
        // Subject Under Test
        JobModelMapper.toModel((JobInfoSnapshot) null);
    }

    @Test(expected = NullPointerException.class)
    public void toModels_nullInput_throws() {
        // Subject Under Test
        JobModelMapper.toModel((List<JobInfoSnapshot>) null);
    }

    @Test
    public void toModel_nullReference_emptyValues() {
        // Subject Under Test
        final JobInfoSnapshot jobInfoSnapshot = getJobInfoSnapshot().withFlowStoreReferences(new FlowStoreReferences());
        final JobModel jobModel = JobModelMapper.toModel(jobInfoSnapshot);

        // Test Verification
        assertThat(jobModel.getSubmitterName(), is(""));
        assertThat(jobModel.getFlowBinderName(), is(""));
        assertThat(jobModel.getSinkName(), is(""));
    }

    @Test
    public void toModel_nullJobTimeOfCompletionReference_emptyJobTimeOfCompletion() {
        // Subject Under Test
        JobModel jobModel = JobModelMapper.toModel(getJobInfoSnapshot().withTimeOfCompletion(null));

        // Test Verification
        assertThat(jobModel.getJobCompletionTime(), is(""));
    }

    @Test
    public void toModel_deliveringIgnoredCountIsNotZero_processingIgnoredCountReturned() {
        final State state = new State();
        state.getPhase(State.Phase.PROCESSING).withIgnored(7);
        state.getPhase(State.Phase.DELIVERING).withIgnored(5);
        final JobInfoSnapshot jobInfoSnapshot = getJobInfoSnapshot().withState(state);

        // Subject Under Test
        JobModel jobModel = JobModelMapper.toModel(jobInfoSnapshot);

        // Test Verification
        assertThat(jobModel.getStateModel().getProcessing().getIgnored(), is(7));
    }

    @Test
    public void toModel_partitioningIgnoredCountNotZero_processingIgnoredCountReturned() {
        final State state = new State();
        state.getPhase(State.Phase.PARTITIONING).withIgnored(2);
        state.getPhase(State.Phase.PROCESSING).withIgnored(7);
        final JobInfoSnapshot jobInfoSnapshot = getJobInfoSnapshot().withState(state);

        // Subject Under Test
        JobModel jobModel = JobModelMapper.toModel(jobInfoSnapshot);

        // Test Verification
        assertThat(jobModel.getStateModel().getProcessing().getIgnored(), is(7));
    }

    @Test
    public void toModel_deliveringIgnoredCountIsZero_processingIgnoredCount() {
        final State state = new State();
        state.getPhase(State.Phase.PROCESSING).withIgnored(7);
        state.getPhase(State.Phase.DELIVERING).withIgnored(0);
        final JobInfoSnapshot jobInfoSnapshot = getJobInfoSnapshot().withState(state);

        // Subject Under Test
        JobModel jobModel = JobModelMapper.toModel(jobInfoSnapshot);

        // Test Verification
        assertThat(jobModel.getStateModel().getIgnoredCounter(), is(7));
    }

    @Test
    public void toModel_deliveringAndProcessingIgnoredCountsAreZero_partitioningIgnoredCount() {
        final State state = new State();
        state.getPhase(State.Phase.PARTITIONING).withIgnored(5);
        state.getPhase(State.Phase.PROCESSING).withIgnored(0);
        state.getPhase(State.Phase.DELIVERING).withIgnored(0);
        final JobInfoSnapshot jobInfoSnapshot = getJobInfoSnapshot().withState(state);

        // Subject Under Test
        JobModel jobModel = JobModelMapper.toModel(jobInfoSnapshot);

        // Test Verification
        assertThat(jobModel.getStateModel().getIgnoredCounter(), is(5));
    }

    @Test
    public void toModel_validInput_validOutput() {
        // Subject Under Test
        final JobSpecification testJobSpecification = new JobSpecification()
                .withAncestry(ancestry);

        final JobInfoSnapshot testJobInfoSnapshot = getJobInfoSnapshot()
                .withSpecification(testJobSpecification)
                .withTimeOfCreation(newDate(2015, 4, 6, 13, 1, 23))
                .withTimeOfCompletion(newDate(2015, 4, 7, 13, 1, 23));

        final JobModel jobModel = JobModelMapper.toModel(testJobInfoSnapshot);
        final FlowStoreReferences flowStoreReferences1 = testJobInfoSnapshot.getFlowStoreReferences();

        // Test Verification
        assertThat(jobModel.getJobCreationTime(), is("2015-04-06 13:01:23"));
        assertThat(jobModel.getJobCompletionTime(), is("2015-04-07 13:01:23"));
        assertThat(jobModel.getJobId(), is(String.valueOf(testJobInfoSnapshot.getJobId())));
        assertThat(jobModel.getSubmitterNumber(), is(String.valueOf(testJobInfoSnapshot.getSpecification().getSubmitterId())));
        assertThat(jobModel.getSubmitterName(), is((flowStoreReferences1.getReference(FlowStoreReferences.Elements.SUBMITTER).getName())));
        assertThat(jobModel.getFlowBinderName(), is(flowStoreReferences1.getReference(FlowStoreReferences.Elements.FLOW_BINDER).getName()));
        assertThat(jobModel.getSinkName(), is(flowStoreReferences1.getReference(FlowStoreReferences.Elements.SINK).getName()));
        assertThat(jobModel.getNumberOfItems(), is(testJobInfoSnapshot.getNumberOfItems()));
        assertThat(jobModel.getStateModel().getFailedCounter(), is(testJobInfoSnapshot.getState().getPhase(State.Phase.PARTITIONING).getFailed() + testJobInfoSnapshot.getState().getPhase(State.Phase.PROCESSING).getFailed() + testJobInfoSnapshot.getState().getPhase(State.Phase.DELIVERING).getFailed()));
        assertThat(jobModel.getStateModel().getIgnoredCounter(), is(testJobInfoSnapshot.getState().getPhase(State.Phase.PARTITIONING).getIgnored() + testJobInfoSnapshot.getState().getPhase(State.Phase.PROCESSING).getIgnored() + testJobInfoSnapshot.getState().getPhase(State.Phase.DELIVERING).getIgnored()));
        assertThat(jobModel.getStateModel().getProcessing().getIgnored(), is(testJobInfoSnapshot.getState().getPhase(State.Phase.PROCESSING).getIgnored()));
        assertThat(jobModel.getPackaging(), is(testJobInfoSnapshot.getSpecification().getPackaging()));
        assertThat(jobModel.getFormat(), is(testJobInfoSnapshot.getSpecification().getFormat()));
        assertThat(jobModel.getCharset(), is(testJobInfoSnapshot.getSpecification().getCharset()));
        assertThat(jobModel.getDestination(), is(testJobInfoSnapshot.getSpecification().getDestination()));
        assertThat(jobModel.getMailForNotificationAboutVerification(), is(testJobInfoSnapshot.getSpecification().getMailForNotificationAboutVerification()));
        assertThat(jobModel.getMailForNotificationAboutProcessing(), is(testJobInfoSnapshot.getSpecification().getMailForNotificationAboutProcessing()));
        assertThat(jobModel.getResultMailInitials(), is(testJobInfoSnapshot.getSpecification().getResultmailInitials()));
        assertThat(jobModel.getTransFileAncestry(), is(testJobInfoSnapshot.getSpecification().getAncestry().getTransfile()));
        assertThat(jobModel.getDataFileAncestry(), is(testJobInfoSnapshot.getSpecification().getAncestry().getDatafile()));
        assertThat(jobModel.getBatchIdAncestry(), is(testJobInfoSnapshot.getSpecification().getAncestry().getBatchId()));
        assertThat(jobModel.getDetailsAncestry(), is(new String(testJobInfoSnapshot.getSpecification().getAncestry().getDetails(), StandardCharsets.UTF_8)));
    }

    @Test
    public void toJobInfoSnapshotForRerunScheme_validInput_ok() {
        final StateModel stateModel = new StateModel();
        stateModel.withPartitioning(new StateElement());
        stateModel.withProcessing(new StateElement());
        stateModel.withDelivering(new StateElement().withFailed(1));

        testJobModel.withStateModel(stateModel).withNumberOfChunks(1).withNumberOfItems(2);

        // Subject under test
        final JobInfoSnapshot jobInfoSnapshot = JobModelMapper.toJobInfoSnapshotForRerunScheme(testJobModel);

        //Verification
        assertThat(jobInfoSnapshot.hasFatalError(), is(false));
        assertThat(jobInfoSnapshot.getFlowStoreReferences().getReference(FlowStoreReferences.Elements.SINK).getId(), is(testJobModel.getSinkId()));
        assertThat(jobInfoSnapshot.getNumberOfChunks(), is(testJobModel.getNumberOfChunks()));
        assertThat(jobInfoSnapshot.getNumberOfItems(), is(testJobModel.getNumberOfItems()));
        assertThat(jobInfoSnapshot.getSpecification().getAncestry().getHarvesterToken(), is(testJobModel.getHarvesterTokenAncestry()));
        assertThat((jobInfoSnapshot.getState().getPhase(State.Phase.DELIVERING).getFailed()), is(1));
    }

    @Test(expected = NullPointerException.class)
    public void toJobInputStream_nullInput_throws() {
        // Subject Under Test
        toJobInputStream(null);
    }

    @Test
    public void toJobInputStream_validInput_validOutput() {
        // Subject Under Test
        JobInputStream jobInputStream = JobModelMapper.toJobInputStream(testJobModel);

        // Verify Test
        JobSpecification jobSpecification = jobInputStream.getJobSpecification();
        assertThat(jobSpecification.getPackaging(), is(testJobModel.getPackaging()));
        assertThat(jobSpecification.getFormat(), is(testJobModel.getFormat()));
        assertThat(jobSpecification.getCharset(), is(testJobModel.getCharset()));
        assertThat(jobSpecification.getDestination(), is(testJobModel.getDestination()));
        assertThat(jobSpecification.getSubmitterId(), is(Long.valueOf(testJobModel.getSubmitterNumber())));
        assertThat(jobSpecification.getMailForNotificationAboutVerification(), is(testJobModel.getMailForNotificationAboutVerification()));
        assertThat(jobSpecification.getMailForNotificationAboutProcessing(), is(testJobModel.getMailForNotificationAboutProcessing()));
        assertThat(jobSpecification.getResultmailInitials(), is(testJobModel.getResultMailInitials()));
        assertThat(jobSpecification.getDataFile(), is(testJobModel.getDataFile()));
        assertThat(jobSpecification.getType().name(), is(testJobModel.getType().name()));
        assertThat(jobSpecification.getAncestry().getTransfile(), is(testJobModel.getTransFileAncestry()));
        assertThat(jobSpecification.getAncestry().getDatafile(), is(testJobModel.getDataFileAncestry()));
        assertThat(jobSpecification.getAncestry().getBatchId(), is(testJobModel.getBatchIdAncestry()));
        assertThat(jobSpecification.getAncestry().getDetails(), is(testJobModel.getDetailsAncestry().getBytes()));
        assertThat(jobSpecification.getAncestry().getPreviousJobId(), is(testJobModel.getPreviousJobIdAncestry()));
    }

    @Test(expected = NullPointerException.class)
    public void toJobInputStream_nullPackaging_throws() {
        testJobModel.withPackaging(null);

        // Subject Under Test
        JobModelMapper.toJobInputStream(testJobModel);
    }

    @Test(expected = NullPointerException.class)
    public void toJobInputStream_nullFormat_throws() {
        testJobModel.withPackaging(null);

        // Subject Under Test
        JobModelMapper.toJobInputStream(testJobModel);
    }

    @Test(expected = NullPointerException.class)
    public void toJobInputStream_nullCharset_throws() {
        testJobModel.withCharset(null);

        // Subject Under Test
        JobModelMapper.toJobInputStream(testJobModel);
    }

    @Test(expected = NullPointerException.class)
    public void toJobInputStream_nullDestination_throws() {
        testJobModel.withDestination(null);

        // Subject Under Test
        JobModelMapper.toJobInputStream(testJobModel);
    }

    @Test(expected = NullPointerException.class)
    public void toJobInputStream_nullMailForNotificationAboutVerification_throws() {
        testJobModel.withMailForNotificationAboutVerification(null);

        // Subject Under Test
        JobModelMapper.toJobInputStream(testJobModel);
    }

    @Test(expected = NullPointerException.class)
    public void toJobInputStream_nullMailForNotificationAboutProcessing_throws() {
        testJobModel.withMailForNotificationAboutProcessing(null);

        // Subject Under Test
        JobModelMapper.toJobInputStream(testJobModel);
    }

    @Test(expected = NullPointerException.class)
    public void toJobInputStream_nullResultMailInitials_throws() {
        testJobModel.withResultMailInitials(null);

        // Subject Under Test
        JobModelMapper.toJobInputStream(testJobModel);
    }

    @Test(expected = NullPointerException.class)
    public void toJobInputStream_nullDataFile_throws() {
        testJobModel.withDataFile(null);

        // Subject Under Test
        JobModelMapper.toJobInputStream(testJobModel);
    }

    @Test(expected = NullPointerException.class)
    public void toJobInputStream_nullType_throws() {
        testJobModel.withType(null);

        // Subject Under Test
        JobModelMapper.toJobInputStream(testJobModel);
    }

    /*
     * Private methods
     */
    private Date newDate(int year, int month, int day, int hour, int minute, int second) {
        Calendar cal = Calendar.getInstance();
        cal.set(Calendar.YEAR, year);
        cal.set(Calendar.MONTH, month - 1);
        cal.set(Calendar.DAY_OF_MONTH, day);
        cal.set(Calendar.HOUR_OF_DAY, hour);
        cal.set(Calendar.MINUTE, minute);
        cal.set(Calendar.SECOND, second);
        return cal.getTime();
    }

    private JobInfoSnapshot getJobInfoSnapshot() {
        return new JobInfoSnapshot()
                .withJobId(1)
                .withEoj(true)
                .withFatalError(false)
                .withPartNumber(0)
                .withNumberOfChunks(2)
                .withNumberOfItems(11)
                .withTimeOfCreation(new Date())
                .withTimeOfLastModification(new Date())
                .withTimeOfCompletion(new Date())
                .withSpecification(getJobSpecification())
                .withState(new State())
                .withFlowStoreReferences(new FlowStoreReferencesBuilder().build());
    }

    private JobSpecification getJobSpecification() {
        return new JobSpecification()
                .withPackaging("packaging")
                .withFormat("format")
                .withCharset("utf8")
                .withDestination("destination")
                .withSubmitterId(222)
                .withMailForNotificationAboutVerification("")
                .withMailForNotificationAboutProcessing("")
                .withResultmailInitials("")
                .withDataFile("datafile")
                .withType(JobSpecification.Type.TEST);
    }

    private String getHarvesterToken(HarvesterToken.HarvesterVariant harvesterVariant) {
        return new HarvesterToken()
                .withHarvesterVariant(harvesterVariant)
                .withId(42)
                .withVersion(1)
                .toString();
    }
}
