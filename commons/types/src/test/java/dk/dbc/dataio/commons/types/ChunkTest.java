package dk.dbc.dataio.commons.types;

import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.junit.Assert.assertThat;

public class ChunkTest {
    private static final long JOB_ID = 2;
    private static final long CHUNK_ID = 1;
    private static final Flow FLOW = FlowTest.newFlowInstance();
    private static final List<String> RECORDS = Collections.emptyList();

    @Test(expected = IllegalArgumentException.class)
    public void constructor4arg_jobIdArgIsBelowThreshold_throws() {
        new Chunk(Chunk.JOBID_LOWER_THRESHOLD, CHUNK_ID, FLOW, RECORDS);
    }

    @Test(expected = IllegalArgumentException.class)
    public void constructor4arg_chunkIdArgIsBelowThreshold_throws() {
        new Chunk(JOB_ID, Chunk.CHUNKID_LOWER_THRESHOLD, FLOW, RECORDS);
    }

    @Test(expected = NullPointerException.class)
    public void constructor4arg_flowArgIsNull_throws() {
        new Chunk(JOB_ID, CHUNK_ID, null, RECORDS);
    }

    @Test(expected = NullPointerException.class)
    public void constructor4arg_recordsArgIsNull_throws() {
        new Chunk(JOB_ID, CHUNK_ID, FLOW, null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void constructor4arg_recordsArgSizeIsGreaterThanMaxChunkSize_throws() {
        new Chunk(JOB_ID, Chunk.CHUNKID_LOWER_THRESHOLD, FLOW, new ArrayList<String>(Chunk.MAX_RECORDS_PER_CHUNK + 1));
    }

    @Test
    public void constructor4arg_allArgsAreValid_returnsInstance() {
        assertThat(new Chunk(JOB_ID, CHUNK_ID, FLOW, RECORDS), is(notNullValue()));
    }

    @Test(expected = IllegalArgumentException.class)
    public void constructor3arg_jobIdArgIsBelowThreshold_throws() {
        new Chunk(Chunk.JOBID_LOWER_THRESHOLD, CHUNK_ID, FLOW);
    }

    @Test(expected = IllegalArgumentException.class)
    public void constructor3arg_chunkIdArgIsBelowThreshold_throws() {
        new Chunk(JOB_ID, Chunk.CHUNKID_LOWER_THRESHOLD, FLOW);
    }

    @Test(expected = NullPointerException.class)
    public void constructor3arg_flowArgIsNull_throws() {
        new Chunk(JOB_ID, CHUNK_ID, null);
    }

    @Test
    public void constructor3arg_allArgsAreValid_returnsInstance() {
        assertThat(new Chunk(JOB_ID, CHUNK_ID, FLOW), is(notNullValue()));
    }

    @Test(expected = IndexOutOfBoundsException.class)
    public void addRecord_whenMoreThanMaxChunkSizeRecordsAreAdded_throws() {
        final Chunk instance = new Chunk(JOB_ID, CHUNK_ID, FLOW);
        for (int i = 0; i <= Chunk.MAX_RECORDS_PER_CHUNK; i++) {
            instance.addRecord("");
        }
    }

    @Test
    public void getJobId_idCanBeRetrieved() {
        final Chunk instance = new Chunk(JOB_ID, CHUNK_ID, FLOW);
        assertThat(instance.getJobId(), is(JOB_ID));
    }

    @Test
    public void getChunkId_idCanBeRetrieved() {
        final Chunk instance = new Chunk(JOB_ID, CHUNK_ID, FLOW);
        assertThat(instance.getChunkId(), is(CHUNK_ID));
    }

    @Test
    public void getFlow_flowCanBeRetrieved() {
        final Chunk instance = new Chunk(JOB_ID, CHUNK_ID, FLOW);
        assertThat(instance.getFlow(), is(FLOW));
    }

    @Test
    public void getRecords_recordsCanBeRetrieved() {
        final String data1 = "data1";
        final String data2 = "data2";
        final String data3 = "data3";
        final Chunk chunk = new Chunk(JOB_ID, CHUNK_ID, FLOW, Arrays.asList(data1, data2, data3));
        final List<String> records = chunk.getRecords();
        assertThat(records.size(), is(3));
        assertThat(records.get(0), is(data1));
        assertThat(records.get(1), is(data2));
        assertThat(records.get(2), is(data3));
    }

    @Test
    public void getResults_internalResultListCanNotBeMutated() {
        final String data1 = "data1";
        final String data2 = "data2";
        final String data3 = "data3";
        final Chunk chunk = new Chunk(JOB_ID, CHUNK_ID, FLOW, Arrays.asList(data1, data2, data3));
        List<String> records = chunk.getRecords();
        // Try mutating returned result
        records.remove(0);
        records.set(0, "Jack");
        records.set(1, "Sparrow");
        // assert that internal data is still the original
        final List<String> records2 = chunk.getRecords();
        assertThat(records2.size(), is(3));
        assertThat(records2.get(0), is(data1));
        assertThat(records2.get(1), is(data2));
        assertThat(records2.get(2), is(data3));
    }

    public static Chunk newChunkInstance() {
        return new Chunk(JOB_ID, CHUNK_ID, FLOW, RECORDS);
    }
}
