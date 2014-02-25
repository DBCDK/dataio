package dk.dbc.dataio.commons.types;

import org.junit.Test;

import java.nio.charset.Charset;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

public class ChunkResultTest {
    private static final long JOBID = 31L;
    private static final long CHUNKID = 17L;
    private static final Charset ENCODING = Charset.forName("UTF-8");
    private static final List<ChunkItem> ITEMS = Collections.emptyList();

    @Test(expected = NullPointerException.class)
    public void constructor_encodingArgIsNull_throws() {
        new ChunkResult(JOBID, CHUNKID, null, ITEMS);
    }

    @Test(expected = NullPointerException.class)
    public void constructor_resultsArgIsNull_throws() {
        new ChunkResult(JOBID, CHUNKID, ENCODING, null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void constructor_jobIdArgIsLessThanZero_throws() {
        new ChunkResult(-1, CHUNKID, ENCODING, ITEMS);
    }

    @Test(expected = IllegalArgumentException.class)
    public void constructor_chunkIdArgIsLessThanZero_throws() {
        new ChunkResult(JOBID, -1, ENCODING, ITEMS);
    }

    @Test
    public void getEncoding_encodingCanBeRetrieved() {
        ChunkResult res = new ChunkResult(JOBID, CHUNKID, ENCODING, ITEMS);
        assertThat(res.getEncoding(), is(ENCODING));
    }

    @Test
    public void getResults_resultsCanBeRetrieved() {
        final ChunkItem data1 = ChunkItemTest.newChunkItemInstance();
        final ChunkItem data2 = ChunkItemTest.newChunkItemInstance();
        final ChunkItem data3 = ChunkItemTest.newChunkItemInstance();
        final ChunkResult instance = new ChunkResult(JOBID, CHUNKID, ENCODING, Arrays.asList(data1, data2, data3));
        final List<ChunkItem> items = instance.getItems();
        assertThat(items.size(), is(3));
        assertThat(items.get(0), is(data1));
        assertThat(items.get(1), is(data2));
        assertThat(items.get(2), is(data3));
    }

    @Test
    public void getResults_internalResultListCanNotBeMutated() {
        final ChunkItem data1 = ChunkItemTest.newChunkItemInstance();
        final ChunkItem data2 = ChunkItemTest.newChunkItemInstance();
        final ChunkItem data3 = ChunkItemTest.newChunkItemInstance();
        final ChunkResult instance = new ChunkResult(JOBID, CHUNKID, ENCODING, Arrays.asList(data1, data2, data3));
        final List<ChunkItem> items = instance.getItems();
        // Try mutating returned result
        items.remove(0);
        items.set(0, ChunkItemTest.newChunkItemInstance());
        items.set(1, null);
        // assert that internal data is still the original
        List<ChunkItem> items2 = instance.getItems();
        assertThat(items2.size(), is(3));
        assertThat(items2.get(0), is(data1));
        assertThat(items2.get(1), is(data2));
        assertThat(items2.get(2), is(data3));
    }

    public static ChunkResult newChunkResultInstance() {
        return new ChunkResult(JOBID, CHUNKID, ENCODING, ITEMS);
    }
}
