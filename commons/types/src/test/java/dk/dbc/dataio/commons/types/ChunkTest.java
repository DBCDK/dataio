package dk.dbc.dataio.commons.types;

import org.hamcrest.CoreMatchers;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.junit.Assert.assertThat;

public class ChunkTest {
    private static final long JOB_ID = 2;
    private static final long CHUNK_ID = 1;
    private static final Flow FLOW = FlowTest.newFlowInstance();
    private static final List<ChunkItem> ITEMS = Collections.emptyList();
    private static final SupplementaryProcessData SUPPLEMENTARY_PROCESS_DATA = new SupplementaryProcessData(123456L, "utf-8");

    @Test(expected = IllegalArgumentException.class)
    public void constructor5arg_jobIdArgIsLessThanLowerBound_throws() {
        new Chunk(Constants.JOB_ID_LOWER_BOUND - 1, CHUNK_ID, FLOW, SUPPLEMENTARY_PROCESS_DATA, ITEMS);
    }

    @Test(expected = IllegalArgumentException.class)
    public void constructor5arg_chunkIdArgIsLessThanLowerBound_throws() {
        new Chunk(JOB_ID, Constants.CHUNK_ID_LOWER_BOUND - 1, FLOW, SUPPLEMENTARY_PROCESS_DATA, ITEMS);
    }

    @Test(expected = NullPointerException.class)
    public void constructor5arg_flowArgIsNull_throws() {
        new Chunk(JOB_ID, CHUNK_ID, null, SUPPLEMENTARY_PROCESS_DATA, ITEMS);
    }

    @Test(expected = NullPointerException.class)
    public void constructor5arg_supplementaryProcessDataArgIsNull_throws() {
        new Chunk(JOB_ID, CHUNK_ID, FLOW, null, ITEMS);
    }

    @Test(expected = NullPointerException.class)
    public void constructor5arg_recordsArgIsNull_throws() {
        new Chunk(JOB_ID, CHUNK_ID, FLOW, SUPPLEMENTARY_PROCESS_DATA, null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void constructor5arg_recordsArgSizeIsGreaterThanMaxChunkSize_throws() {
        final ArrayList<ChunkItem> items = new ArrayList<>(Constants.CHUNK_RECORD_COUNT_UPPER_BOUND + 1);
        for (int i = 0; i <= Constants.CHUNK_RECORD_COUNT_UPPER_BOUND; i++) {
            items.add(null);
        }
        new Chunk(JOB_ID, CHUNK_ID, FLOW, SUPPLEMENTARY_PROCESS_DATA, items);
    }

    @Test
    public void constructor5arg_allArgsAreValid_returnsInstance() {
        assertThat(new Chunk(JOB_ID, CHUNK_ID, FLOW, SUPPLEMENTARY_PROCESS_DATA, ITEMS), is(notNullValue()));
    }

    @Test(expected = IllegalArgumentException.class)
    public void constructor4arg_jobIdArgIsLessThanLowerBound_throws() {
        new Chunk(Constants.JOB_ID_LOWER_BOUND - 1, CHUNK_ID, FLOW, SUPPLEMENTARY_PROCESS_DATA);
    }

    @Test(expected = IllegalArgumentException.class)
    public void constructor4arg_chunkIdArgIsLessThanLowerBound_throws() {
        new Chunk(JOB_ID, Constants.CHUNK_ID_LOWER_BOUND - 1, FLOW, SUPPLEMENTARY_PROCESS_DATA);
    }

    @Test(expected = NullPointerException.class)
    public void constructor4arg_flowArgIsNull_throws() {
        new Chunk(JOB_ID, CHUNK_ID, null, SUPPLEMENTARY_PROCESS_DATA);
    }

    @Test(expected = NullPointerException.class)
    public void constructor4arg_supplementaryProcessDataArgIsNull_throws() {
        new Chunk(JOB_ID, CHUNK_ID, FLOW, null);
    }

    @Test
    public void constructor4arg_allArgsAreValid_returnsInstance() {
        assertThat(new Chunk(JOB_ID, CHUNK_ID, FLOW, SUPPLEMENTARY_PROCESS_DATA), is(notNullValue()));
    }

    @Test(expected = IndexOutOfBoundsException.class)
    public void addRecord_whenMoreThanMaxChunkSizeRecordsAreAdded_throws() {
        final Chunk instance = new Chunk(JOB_ID, CHUNK_ID, FLOW, SUPPLEMENTARY_PROCESS_DATA);
        for (int i = 0; i <= Constants.CHUNK_RECORD_COUNT_UPPER_BOUND; i++) {
            instance.addItem(ChunkItemTest.newChunkItemInstance());
        }
    }

    @Test
    public void getJobId_idCanBeRetrieved() {
        final Chunk instance = new Chunk(JOB_ID, CHUNK_ID, FLOW, SUPPLEMENTARY_PROCESS_DATA);
        assertThat(instance.getJobId(), is(JOB_ID));
    }

    @Test
    public void getChunkId_idCanBeRetrieved() {
        final Chunk instance = new Chunk(JOB_ID, CHUNK_ID, FLOW, SUPPLEMENTARY_PROCESS_DATA);
        assertThat(instance.getChunkId(), is(CHUNK_ID));
    }

    @Test
    public void getFlow_flowCanBeRetrieved() {
        final Chunk instance = new Chunk(JOB_ID, CHUNK_ID, FLOW, SUPPLEMENTARY_PROCESS_DATA);
        assertThat(instance.getFlow(), is(FLOW));
    }

    @Test
    public void getSupplementaryProcessData_supplementaryProcessDataCanBeRetrieved() {
        final Chunk instance = new Chunk(JOB_ID, CHUNK_ID, FLOW, SUPPLEMENTARY_PROCESS_DATA);
        assertThat(instance.getSupplementaryProcessData(), is(SUPPLEMENTARY_PROCESS_DATA));
    }

    @Test
    public void getItems_itemsCanBeRetrieved() {
        final ChunkItem data1 = ChunkItemTest.newChunkItemInstance();
        final ChunkItem data2 = ChunkItemTest.newChunkItemInstance();
        final ChunkItem data3 = ChunkItemTest.newChunkItemInstance();
        final Chunk chunk = new Chunk(JOB_ID, CHUNK_ID, FLOW, SUPPLEMENTARY_PROCESS_DATA, Arrays.asList(data1, data2, data3));
        final List<ChunkItem> items = chunk.getItems();
        assertThat(items.size(), is(3));
        assertThat(items.get(0), is(data1));
        assertThat(items.get(1), is(data2));
        assertThat(items.get(2), is(data3));
    }

    @Test
    public void getItems_internalItemsListCanNotBeMutated() {
        final ChunkItem data1 = ChunkItemTest.newChunkItemInstance();
        final ChunkItem data2 = ChunkItemTest.newChunkItemInstance();
        final ChunkItem data3 = ChunkItemTest.newChunkItemInstance();
        final Chunk chunk = new Chunk(JOB_ID, CHUNK_ID, FLOW, SUPPLEMENTARY_PROCESS_DATA, Arrays.asList(data1, data2, data3));
        List<ChunkItem> items = chunk.getItems();
        // Try mutating returned result
        items.remove(0);
        items.set(0, ChunkItemTest.newChunkItemInstance());
        items.set(1, null);
        // assert that internal data is still the original
        final List<ChunkItem> items2 = chunk.getItems();
        assertThat(items2.size(), is(3));
        assertThat(items2.get(0), is(data1));
        assertThat(items2.get(1), is(data2));
        assertThat(items2.get(2), is(data3));
    }

    @Test
    public void getKeys_initiallyEmpty() {
        final Chunk chunk = newChunkInstance();
        assertThat(chunk.getKeys(), is(notNullValue()));
        assertThat(chunk.getKeys().size(), is(0));
    }

    @Test
    public void addKeys_keysAddedToInternalSet() {
        final HashSet<String> keys = new HashSet<>();
        keys.add("key1");
        keys.add("key2");
        keys.add(null);
        final Chunk chunk = newChunkInstance();
        for (final String key : keys) {
            chunk.addKey(key);
        }
        for (final String key : keys) {
            chunk.addKey(key);
        }
        assertThat(chunk.getKeys().size(), is(3));
    }

    @Test
    public void getKeys_keysCanBeRetrieved() {
        final HashSet<String> keys = new HashSet<>();
        keys.add("key1");
        keys.add("key2");
        keys.add(null);
        final Chunk chunk = newChunkInstance();
        for (final String key : keys) {
            chunk.addKey(key);
        }
        assertThat(chunk.getKeys(), CoreMatchers.<Set<String>>is(keys));
    }

    @Test
    public void getKeys_internalKeysSetCanNotBeMutated() {
        final HashSet<String> keys = new HashSet<>();
        keys.add("key1");
        keys.add("key2");
        keys.add(null);
        final Chunk chunk = newChunkInstance();
        for (final String key : keys) {
            chunk.addKey(key);
        }
        final Set<String> returnedKeys = chunk.getKeys();
        // Try mutating returned result
        returnedKeys.remove("key1");
        returnedKeys.add("key3");
        // assert that internal data is still the original
        assertThat(chunk.getKeys(), CoreMatchers.<Set<String>>is(keys));
    }

    public static Chunk newChunkInstance() {
        return new Chunk(JOB_ID, CHUNK_ID, FLOW, SUPPLEMENTARY_PROCESS_DATA, ITEMS);
    }
}
