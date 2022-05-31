package dk.dbc.dataio.gui.server.modelmappers;

import dk.dbc.dataio.commons.types.SinkContent;
import dk.dbc.dataio.gui.client.pages.sink.status.SinkStatusTable;
import dk.dbc.dataio.jobstore.types.SinkStatusSnapshot;
import org.junit.Test;

import java.util.Collections;
import java.util.List;

import static dk.dbc.commons.testutil.Assert.assertThat;
import static dk.dbc.commons.testutil.Assert.isThrowing;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;

public class SinkStatusModelMapperTest {

    private SinkStatusSnapshot sinkStatusSnapshot = new SinkStatusSnapshot().withSinkId(1).withSinkType(SinkContent.SinkType.OPENUPDATE).withName("testSink").withNumberOfJobs(1).withNumberOfChunks(2);

    @Test
    public void toModel_sinkStatusSnapshotListIsNull_throws() {
        List<SinkStatusSnapshot> sinkStatusSnapshotList = null;
        assertThat(() -> SinkStatusModelMapper.toModel(sinkStatusSnapshotList), isThrowing(NullPointerException.class));
    }

    @Test
    public void toModel_sinkStatusSnapshotIsnull_throws() {
        SinkStatusSnapshot sinkStatusSnapshot = null;
        assertThat(() -> SinkStatusModelMapper.toModel(sinkStatusSnapshot), isThrowing(NullPointerException.class));
    }

    @Test
    public void toModel_validInputSinkStatusSnapshot_returnsValidModel() {
        // Activate Subject Under Test
        SinkStatusTable.SinkStatusModel model = SinkStatusModelMapper.toModel(sinkStatusSnapshot);

        // Verification
        assertThat(model.getSinkId(), is(sinkStatusSnapshot.getSinkId()));
        assertThat(model.getSinkType(), is(sinkStatusSnapshot.getType().name()));
        assertThat(model.getName(), is(sinkStatusSnapshot.getName()));
        assertThat(model.getOutstandingJobs(), is(sinkStatusSnapshot.getNumberOfJobs()));
        assertThat(model.getOutstandingChunks(), is(sinkStatusSnapshot.getNumberOfChunks()));
        assertThat(model.getLatestMovement(), is(nullValue()));
    }

    @Test
    public void toModel_validInputSinkStatusSnapshot_returnsValidListOfModels() {
        // Activate Subject Under Test
        List<SinkStatusTable.SinkStatusModel> sinkStatusModels = SinkStatusModelMapper.toModel(Collections.singletonList(sinkStatusSnapshot));

        // Verification
        final SinkStatusTable.SinkStatusModel model = sinkStatusModels.get(0);
        assertThat(sinkStatusModels.get(0).getSinkId(), is(sinkStatusSnapshot.getSinkId()));
        assertThat(model.getSinkType(), is(sinkStatusSnapshot.getType().name()));
        assertThat(model.getName(), is(sinkStatusSnapshot.getName()));
        assertThat(model.getOutstandingJobs(), is(sinkStatusSnapshot.getNumberOfJobs()));
        assertThat(model.getOutstandingChunks(), is(sinkStatusSnapshot.getNumberOfChunks()));
        assertThat(model.getLatestMovement(), is(nullValue()));
    }
}
