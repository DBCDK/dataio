package dk.dbc.dataio.jobstore.types;

import dk.dbc.dataio.commons.types.ChunkItem;
import dk.dbc.dataio.commons.types.Constants;
import dk.dbc.dataio.jobstore.types.SinkChunkResult;
import org.junit.Test;

import java.nio.charset.Charset;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

public class SinkChunkResultTest {
    private static final long JOBID = 31L;
    private static final long CHUNKID = 17L;
    private static final Charset ENCODING = Charset.forName("UTF-8");
    private static final List<ChunkItem> ITEMS = Collections.emptyList();

    @Test(expected = NullPointerException.class)
    public void constructor_encodingArgIsNull_throws() {
        new SinkChunkResult(JOBID, CHUNKID, null, ITEMS);
    }

    @Test(expected = NullPointerException.class)
    public void constructor_resultsArgIsNull_throws() {
        new SinkChunkResult(JOBID, CHUNKID, ENCODING, null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void constructor_jobIdArgIsLessThanLowerBound_throws() {
        new SinkChunkResult(Constants.JOB_ID_LOWER_BOUND - 1, CHUNKID, ENCODING, ITEMS);
    }

    @Test(expected = IllegalArgumentException.class)
    public void constructor_chunkIdArgIsLessThanLowerBound_throws() {
        new SinkChunkResult(JOBID, Constants.CHUNK_ID_LOWER_BOUND - 1, ENCODING, ITEMS);
    }

    @Test
    public void getEncoding_encodingCanBeRetrieved() {
        SinkChunkResult res = new SinkChunkResult(JOBID, CHUNKID, ENCODING, ITEMS);
        assertThat(res.getEncoding(), is(ENCODING));
    }

    @Test
    public void getResults_resultsCanBeRetrieved() {
        final ChunkItem data1 = ChunkTest.newChunkItemInstance();
        final ChunkItem data2 = ChunkTest.newChunkItemInstance();
        final ChunkItem data3 = ChunkTest.newChunkItemInstance();
        final SinkChunkResult instance = new SinkChunkResult(JOBID, CHUNKID, ENCODING, Arrays.asList(data1, data2, data3));
        final List<ChunkItem> items = instance.getItems();
        assertThat(items.size(), is(3));
        assertThat(items.get(0), is(data1));
        assertThat(items.get(1), is(data2));
        assertThat(items.get(2), is(data3));
    }

    public static SinkChunkResult newSinkChunkResultInstance() {
        return new SinkChunkResult(JOBID, CHUNKID, ENCODING, ITEMS);
    }
}
