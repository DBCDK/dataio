package dk.dbc.dataio.commons.types;

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

    @Test(expected = NullPointerException.class)
    public void constructor_encodingArgIsNull_throws() {
        new SinkChunkResult(JOBID, CHUNKID, null, Collections.EMPTY_LIST);
    }

    @Test(expected = NullPointerException.class)
    public void constructor_resultsArgIsNull_throws() {
        new SinkChunkResult(JOBID, CHUNKID, ENCODING, null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void constructor_jobIdArgIsLessThanZero_throws() {
        new SinkChunkResult(-1, CHUNKID, ENCODING, Collections.EMPTY_LIST);
    }

    @Test(expected = IllegalArgumentException.class)
    public void constructor_chunkIdArgIsLessThanZero_throws() {
        new SinkChunkResult(JOBID, -1, ENCODING, Collections.EMPTY_LIST);
    }

    @Test
    public void getEncoding_encodingCanBeRetrieved() {
        SinkChunkResult res = new SinkChunkResult(JOBID, CHUNKID, ENCODING, Collections.EMPTY_LIST);
        assertThat(res.getEncoding(), is(ENCODING));
    }

    @Test
    public void getResults_resultsCanBeRetrieved() {
        SinkChunkResult res = new SinkChunkResult(JOBID, CHUNKID, ENCODING, Arrays.asList("data1", "data2", "data3"));
        List<String> results = res.getResults();
        assertThat(results.size(), is(3));
        assertThat(results.get(0), is("data1"));
        assertThat(results.get(1), is("data2"));
        assertThat(results.get(2), is("data3"));
    }

    @Test
    public void getResults_internalResultListCanNotBeMutated() {
        SinkChunkResult res = new SinkChunkResult(JOBID, CHUNKID, ENCODING, Arrays.asList("data1", "data2", "data3"));
        List<String> results = res.getResults();
        // Try mutating returned result
        results.remove(0);
        results.set(0, "Jack");
        results.set(1, "Sparrow");
        // assert that internal data is still the original
        List<String> results2 = res.getResults();
        assertThat(results2.size(), is(3));
        assertThat(results2.get(0), is("data1"));
        assertThat(results2.get(1), is("data2"));
        assertThat(results2.get(2), is("data3"));
    }

    public static SinkChunkResult newSinkChunkResultInstance() {
        return new SinkChunkResult(JOBID, CHUNKID, ENCODING, Collections.EMPTY_LIST);
    }
}
