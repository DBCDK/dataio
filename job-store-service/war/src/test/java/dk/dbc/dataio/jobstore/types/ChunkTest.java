package dk.dbc.dataio.jobstore.types;

import dk.dbc.dataio.commons.types.ChunkItem;
import dk.dbc.dataio.commons.types.Constants;
import dk.dbc.dataio.commons.types.Flow;
import dk.dbc.dataio.commons.types.FlowComponent;
import dk.dbc.dataio.commons.types.FlowComponentContent;
import dk.dbc.dataio.commons.types.FlowContent;
import dk.dbc.dataio.commons.types.JavaScript;
import dk.dbc.dataio.commons.types.SupplementaryProcessData;
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
    private static final Flow FLOW = newFlowInstance();
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

//    @Test(expected = NullPointerException.class)
//    public void constructor5arg_flowArgIsNull_throws() {
//        new Chunk(JOB_ID, CHUNK_ID, null, SUPPLEMENTARY_PROCESS_DATA, ITEMS);
//    }

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

//    @Test(expected = NullPointerException.class)
//    public void constructor4arg_flowArgIsNull_throws() {
//        new Chunk(JOB_ID, CHUNK_ID, null, SUPPLEMENTARY_PROCESS_DATA);
//    }

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
            instance.addItem(newChunkItemInstance());
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
        final ChunkItem data1 = newChunkItemInstance();
        final ChunkItem data2 = newChunkItemInstance();
        final ChunkItem data3 = newChunkItemInstance();
        final Chunk chunk = new Chunk(JOB_ID, CHUNK_ID, FLOW, SUPPLEMENTARY_PROCESS_DATA, Arrays.asList(data1, data2, data3));
        final List<ChunkItem> items = chunk.getItems();
        assertThat(items.size(), is(3));
        assertThat(items.get(0), is(data1));
        assertThat(items.get(1), is(data2));
        assertThat(items.get(2), is(data3));
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
    
    public static ChunkItem newChunkItemInstance() {
        final long ID = 0L;
        final String DATA = "data";
        final ChunkItem.Status STATUS = ChunkItem.Status.SUCCESS;
        return new ChunkItem(ID, DATA, STATUS);
    }

    public static Flow newFlowInstance() {
        final long ID = 42L;
        final long VERSION = 1L;
        final FlowContent CONTENT = newFlowContentInstance();
        return new Flow(ID, VERSION, CONTENT);
    }
    
    public static FlowContent newFlowContentInstance() {
        final String NAME = "name";
        final String DESCRIPTION = "description";
        final List<FlowComponent> COMPONENTS = Arrays.asList(newFlowComponentInstance());
        return new FlowContent(NAME, DESCRIPTION, COMPONENTS);
    }

    public static FlowComponent newFlowComponentInstance() {
        final long ID = 42L;
        final long VERSION = 1L;
        final FlowComponentContent CONTENT = newFlowComponentContentInstance();
        return new FlowComponent(ID, VERSION, CONTENT);
    }

    public static FlowComponentContent newFlowComponentContentInstance() {
        final String NAME = "name";
        final String SVN_PROJECT_FOR_INVOCATION_JAVASCRIPT = "svnProjectForInvocationJavascript";
        final long SVN_REVISION = 1L;
        final String JAVA_SCRIPT_NAME = "invocationJavascriptName";
        final String INVOCATION_METHOD = "method";
        final List<JavaScript> JAVASCRIPTS = Arrays.asList(newJavaScriptInstance());
        return new FlowComponentContent(NAME, SVN_PROJECT_FOR_INVOCATION_JAVASCRIPT, SVN_REVISION, JAVA_SCRIPT_NAME, JAVASCRIPTS, INVOCATION_METHOD);
    }

    public static JavaScript newJavaScriptInstance() {
        final String MODULE_NAME = "module";
        final String JAVASCRIPT = "javascript";
        return new JavaScript(JAVASCRIPT, MODULE_NAME);
    }
}
