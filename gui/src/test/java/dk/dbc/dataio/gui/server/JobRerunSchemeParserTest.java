package dk.dbc.dataio.gui.server;

import dk.dbc.dataio.common.utils.flowstore.FlowStoreServiceConnector;
import dk.dbc.dataio.common.utils.flowstore.FlowStoreServiceConnectorException;
import dk.dbc.dataio.commons.types.HarvesterToken;
import dk.dbc.dataio.commons.types.JobSpecification;
import dk.dbc.dataio.commons.types.Sink;
import dk.dbc.dataio.commons.types.SinkContent;
import dk.dbc.dataio.commons.utils.test.model.SinkContentBuilder;
import dk.dbc.dataio.gui.server.jobrerun.JobRerunScheme;
import dk.dbc.dataio.jobstore.types.FlowStoreReference;
import dk.dbc.dataio.jobstore.types.FlowStoreReferences;
import dk.dbc.dataio.jobstore.types.JobInfoSnapshot;
import dk.dbc.dataio.jobstore.types.State;
import dk.dbc.dataio.jobstore.types.StateChange;
import org.junit.Test;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class JobRerunSchemeParserTest {

    private final FlowStoreServiceConnector flowStoreServiceConnector = mock(FlowStoreServiceConnector.class);

    @Test
    public void parse_previewJob_hasActionCopy() throws FlowStoreServiceConnectorException {
        final JobInfoSnapshot jobInfoSnapshot = new JobInfoSnapshot().withNumberOfChunks(0).withNumberOfItems(5)
                .withSpecification(new JobSpecification().withType(JobSpecification.Type.ACCTEST));

        final JobRerunSchemeParser jobRerunSchemeParser = new JobRerunSchemeParser(flowStoreServiceConnector);

        // Subject under test
        final JobRerunScheme jobRerunScheme = jobRerunSchemeParser.parse(jobInfoSnapshot);

        // Verification
        assertThat("jobRerunScheme.actions.size = 1", jobRerunScheme.getActions().size(), is(1));
        assertThat("jobRerunScheme.actions.action = COPY", jobRerunScheme.getActions().contains(JobRerunScheme.Action.COPY), is(true));
    }

    @Test
    public void parse_fatalErrorJob_hasActionCopy() throws FlowStoreServiceConnectorException {
        final JobInfoSnapshot jobInfoSnapshot = new JobInfoSnapshot().withFatalError(true)
                .withSpecification(new JobSpecification().withType(JobSpecification.Type.ACCTEST));

        final JobRerunSchemeParser jobRerunSchemeParser = new JobRerunSchemeParser(flowStoreServiceConnector);

        // Subject under test
        final JobRerunScheme jobRerunScheme = jobRerunSchemeParser.parse(jobInfoSnapshot);

        // Verification
        assertThat("jobRerunScheme.actions.size = 1", jobRerunScheme.getActions().size(), is(1));
        assertThat("jobRerunScheme.actions.action = COPY", jobRerunScheme.getActions().contains(JobRerunScheme.Action.COPY), is(true));
    }

    @Test
    public void parse_acceptanceTestJob_hasActionRerunAndRerunAllIsTypeOriginalFile() throws FlowStoreServiceConnectorException {
        final JobInfoSnapshot jobInfoSnapshot = getJobInfoSnapshotContainingFailed().withSpecification(new JobSpecification().withType(JobSpecification.Type.ACCTEST));
        final JobRerunSchemeParser jobRerunSchemeParser = new JobRerunSchemeParser(flowStoreServiceConnector);

        // Subject under test
        final JobRerunScheme jobRerunScheme = jobRerunSchemeParser.parse(jobInfoSnapshot);

        // Verification
        assertThat("jobRerunScheme.type = ORIGINAL_FILE", jobRerunScheme.getType(), is(JobRerunScheme.Type.ORIGINAL_FILE));
        assertThat("jobRerunScheme.actions.size = 2", jobRerunScheme.getActions().size(), is(2));
        assertThat("jobRerunScheme.actions.action = RERUN_ALL", jobRerunScheme.getActions().contains(JobRerunScheme.Action.RERUN_ALL), is(true));
        assertThat("jobRerunScheme.actions.action = RERUN_FAILED", jobRerunScheme.getActions().contains(JobRerunScheme.Action.RERUN_FAILED), is(true));
    }

    @Test
    public void parse_toTickleJob_hasActionRerunAndRerunAllIsTypeTickle() throws FlowStoreServiceConnectorException {
        final JobInfoSnapshot jobInfoSnapshot = getJobInfoSnapshotContainingFailed()
                .withSpecification(new JobSpecification()).withFlowStoreReferences(getFlowStoreReferencesWithSink());
        when(flowStoreServiceConnector.getSink(anyLong())).thenReturn(new Sink(1, 1, new SinkContentBuilder().setSinkType(SinkContent.SinkType.TICKLE).build()));
        final JobRerunSchemeParser jobRerunSchemeParser = new JobRerunSchemeParser(flowStoreServiceConnector);

        // Subject under test
        final JobRerunScheme jobRerunScheme = jobRerunSchemeParser.parse(jobInfoSnapshot);

        // Verification
        assertThat("jobRerunScheme.type = TICKLE", jobRerunScheme.getType(), is(JobRerunScheme.Type.TICKLE));
        assertThat("jobRerunScheme.actions.size = 2", jobRerunScheme.getActions().size(), is(2));
        assertThat("jobRerunScheme.actions.action = RERUN_ALL", jobRerunScheme.getActions().contains(JobRerunScheme.Action.RERUN_ALL), is(true));
        assertThat("jobRerunScheme.actions.action = RERUN_FAILED", jobRerunScheme.getActions().contains(JobRerunScheme.Action.RERUN_FAILED), is(true));
    }

    @Test
    public void parse_toTickleJob_hasActionRerunIsTypeTickle() throws FlowStoreServiceConnectorException {
        final JobInfoSnapshot jobInfoSnapshot = getJobInfoSnapshotContainingFailed()
                .withSpecification(new JobSpecification()).withFlowStoreReferences(getFlowStoreReferencesWithSink());
        when(flowStoreServiceConnector.getSink(anyLong())).thenReturn(new Sink(1, 1, new SinkContentBuilder().setQueue(JobRerunScheme.TICKLE_TOTAL).setSinkType(SinkContent.SinkType.TICKLE).build()));
        final JobRerunSchemeParser jobRerunSchemeParser = new JobRerunSchemeParser(flowStoreServiceConnector);

        // Subject under test
        final JobRerunScheme jobRerunScheme = jobRerunSchemeParser.parse(jobInfoSnapshot);

        // Verification
        assertThat("jobRerunScheme.type = TICKLE", jobRerunScheme.getType(), is(JobRerunScheme.Type.TICKLE));
        assertThat("jobRerunScheme.actions.size = 1", jobRerunScheme.getActions().size(), is(1));
        assertThat("jobRerunScheme.actions.action = RERUN_ALL", jobRerunScheme.getActions().contains(JobRerunScheme.Action.RERUN_ALL), is(true));
    }

    @Test
    public void parse_toTickleJob_hasNoSinkHasActionCopyIsTypeOriginalFile() throws FlowStoreServiceConnectorException {
        final JobInfoSnapshot jobInfoSnapshot = getJobInfoSnapshotContainingFailed().withFatalError(true)
                .withSpecification(new JobSpecification()).withFlowStoreReferences(new FlowStoreReferences());
        when(flowStoreServiceConnector.getSink(anyLong())).thenReturn(new Sink(1, 1, new SinkContentBuilder().setSinkType(SinkContent.SinkType.TICKLE).build()));
        final JobRerunSchemeParser jobRerunSchemeParser = new JobRerunSchemeParser(flowStoreServiceConnector);

        // Subject under test
        final JobRerunScheme jobRerunScheme = jobRerunSchemeParser.parse(jobInfoSnapshot);

        // Verification
        assertThat("jobRerunScheme.type = ORIGINAL_FILE", jobRerunScheme.getType(), is(JobRerunScheme.Type.ORIGINAL_FILE));
        assertThat("jobRerunScheme.actions.size = 1", jobRerunScheme.getActions().size(), is(1));
        assertThat("jobRerunScheme.actions.action = COPY", jobRerunScheme.getActions().contains(JobRerunScheme.Action.COPY), is(true));
    }

    @Test
    public void parse_fromTickleJob_hasActionRerunIsTypeTickle() throws FlowStoreServiceConnectorException {
        final JobSpecification.Ancestry ancestry = new JobSpecification.Ancestry().withHarvesterToken(getHarvesterToken(HarvesterToken.HarvesterVariant.TICKLE_REPO));
        final JobInfoSnapshot jobInfoSnapshot = getJobInfoSnapshotContainingFailed().withSpecification(new JobSpecification().withAncestry(ancestry));
        final JobRerunSchemeParser jobRerunSchemeParser = new JobRerunSchemeParser(flowStoreServiceConnector);

        // Subject under test
        final JobRerunScheme jobRerunScheme = jobRerunSchemeParser.parse(jobInfoSnapshot);

        // Verification
        assertThat("jobRerunScheme.type = TICKLE", jobRerunScheme.getType(), is(JobRerunScheme.Type.TICKLE));
        assertThat("jobRerunScheme.actions.size = 2", jobRerunScheme.getActions().size(), is(2));
        assertThat("jobRerunScheme.actions.action = RERUN_ALL", jobRerunScheme.getActions().contains(JobRerunScheme.Action.RERUN_ALL), is(true));
        assertThat("jobRerunScheme.actions.action = RERUN_FAILED", jobRerunScheme.getActions().contains(JobRerunScheme.Action.RERUN_FAILED), is(true));
    }

    @Test
    public void parse_fromRawRepoJob_hasActionRerunAndRerunAllIsTypeRawRepo() throws FlowStoreServiceConnectorException {
        final JobSpecification.Ancestry ancestry = new JobSpecification.Ancestry().withHarvesterToken(getHarvesterToken(HarvesterToken.HarvesterVariant.RAW_REPO));
        final JobInfoSnapshot jobInfoSnapshot = getJobInfoSnapshotContainingFailed().withSpecification(new JobSpecification().withAncestry(ancestry)).withFlowStoreReferences(getFlowStoreReferencesWithSink());
        final JobRerunSchemeParser jobRerunSchemeParser = new JobRerunSchemeParser(flowStoreServiceConnector);
        when(flowStoreServiceConnector.getSink(anyLong())).thenReturn(new Sink(1, 1, new SinkContentBuilder().setSinkType(SinkContent.SinkType.HIVE).build()));

        // Subject under test
        final JobRerunScheme jobRerunScheme = jobRerunSchemeParser.parse(jobInfoSnapshot);

        // Verification
        assertThat("jobRerunScheme.type = RR", jobRerunScheme.getType(), is(JobRerunScheme.Type.RR));
        assertThat("jobRerunScheme.actions.size = 2", jobRerunScheme.getActions().size(), is(2));
        assertThat("jobRerunScheme.actions.action = RERUN_ALL", jobRerunScheme.getActions().contains(JobRerunScheme.Action.RERUN_ALL), is(true));
        assertThat("jobRerunScheme.actions.action = RERUN_FAILED", jobRerunScheme.getActions().contains(JobRerunScheme.Action.RERUN_FAILED), is(true));
    }

    /* private methods */

    private JobInfoSnapshot getJobInfoSnapshotContainingFailed() {
        final StateChange stateChange = new StateChange();
        stateChange.setFailed(1);
        stateChange.setPhase(State.Phase.PROCESSING);

        final State state = new State();
        state.updateState(stateChange);

        return new JobInfoSnapshot().withState(state);
    }

    private String getHarvesterToken(HarvesterToken.HarvesterVariant harvesterVariant) {
        return new HarvesterToken()
                .withHarvesterVariant(harvesterVariant)
                .withId(42)
                .withVersion(1)
                .toString();
    }

    private FlowStoreReferences getFlowStoreReferencesWithSink() {
        return new FlowStoreReferences().withReference(FlowStoreReferences.Elements.SINK, new FlowStoreReference(1, 1, "sink"));
    }
}

