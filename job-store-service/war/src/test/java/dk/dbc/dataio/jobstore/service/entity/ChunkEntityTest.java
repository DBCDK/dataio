package dk.dbc.dataio.jobstore.service.entity;

import dk.dbc.dataio.jobstore.types.SequenceAnalysisData;
import dk.dbc.dataio.jobstore.service.sequenceanalyser.ChunkIdentifier;
import dk.dbc.dataio.sequenceanalyser.CollisionDetectionElement;
import org.junit.Test;

import java.util.Arrays;
import java.util.HashSet;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.junit.Assert.assertThat;

public class ChunkEntityTest {
    @Test
    public void toCollisionDetectionElement() {
        final int jobId = 42;
        final int chunkId = 1;
        final SequenceAnalysisData sequenceAnalysisData = new SequenceAnalysisData(new HashSet<>(Arrays.asList("key")));
        final ChunkEntity chunkEntity = new ChunkEntity();
        chunkEntity.setKey(new ChunkEntity.Key(chunkId, jobId));
        chunkEntity.setSequenceAnalysisData(sequenceAnalysisData);

        final CollisionDetectionElement cde = chunkEntity.toCollisionDetectionElement();
        final ChunkIdentifier chunkIdentifier = (ChunkIdentifier) cde.getIdentifier();
        assertThat("CollisionDetectionElement", cde, is(notNullValue()));
        assertThat("CollisionDetectionElement.getIdentifier()", cde.getIdentifier(), is(notNullValue()));
        assertThat("CollisionDetectionElement.getIdentifier().getJobId()", (int) chunkIdentifier.getJobId(), is(jobId));
        assertThat("CollisionDetectionElement.getIdentifier().getChunkId(),", (int) chunkIdentifier.getChunkId(), is(chunkId));
        assertThat("CollisionDetectionElement.getKeys()", cde.getKeys(), is(sequenceAnalysisData.getData()));
    }

}