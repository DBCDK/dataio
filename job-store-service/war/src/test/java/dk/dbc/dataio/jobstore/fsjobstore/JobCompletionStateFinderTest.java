package dk.dbc.dataio.jobstore.fsjobstore;

import dk.dbc.dataio.commons.types.Chunk;
import dk.dbc.dataio.commons.types.ChunkCompletionState;
import dk.dbc.dataio.commons.types.ChunkItem;
import dk.dbc.dataio.commons.types.ChunkResult;
import dk.dbc.dataio.commons.types.ItemCompletionState;
import dk.dbc.dataio.commons.types.JobCompletionState;
import dk.dbc.dataio.commons.types.SinkChunkResult;
import dk.dbc.dataio.commons.utils.test.model.ChunkBuilder;
import dk.dbc.dataio.commons.utils.test.model.ChunkResultBuilder;
import dk.dbc.dataio.commons.utils.test.model.SinkChunkResultBuilder;
import dk.dbc.dataio.jobstore.JobStore;
import dk.dbc.dataio.jobstore.types.JobStoreException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.mockito.Mockito;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class JobCompletionStateFinderTest {

    @Rule
    public TemporaryFolder tmpFolder = new TemporaryFolder();
    
    private JobStore jobStore;

    @Before
    public void setup() {
        jobStore = mock(FileSystemJobStore.class);
    }

    @Test(expected = NoSuchChunkException.class)
    public void chunkCompletionState_noAvailableChunksForChunkId_throws() throws JobStoreException, NoSuchChunkException {
        final long jobId = 1L;
        final long chunkId = 1L;
        JobCompletionStateFinder jobCompletionStateFinder = new JobCompletionStateFinder(jobStore);
        Mockito.when(jobStore.getChunk(jobId, chunkId)).thenReturn(null);
        Mockito.when(jobStore.getProcessorResult(jobId, chunkId)).thenReturn(null);
        Mockito.when(jobStore.getSinkResult(jobId, chunkId)).thenReturn(null);
        jobCompletionStateFinder.getChunkCompletionState(jobId, chunkId);
    }

    @Test(expected = IllegalStateException.class)
    public void chunkCompletionState_sinkItemsAndChunkItemsDifferInSize_throws() throws JobStoreException, NoSuchChunkException {
        final long jobId = 1L;
        final long chunkId = 1L;
        JobCompletionStateFinder jobCompletionStateFinder = new JobCompletionStateFinder(jobStore);
        List<ChunkItem> chunkifiedChunkItems = new ChunkItemStatusListBuilder().setItems(ChunkItem.Status.SUCCESS, 1).build();
        List<ChunkItem> deliveredChunkItems = new ChunkItemStatusListBuilder().setItems(ChunkItem.Status.SUCCESS, 2).build();
        Chunk chunkifiedChunk = new ChunkBuilder().setJobId(jobId).setChunkId(chunkId).setItems(chunkifiedChunkItems).build();
        SinkChunkResult deliveredChunk = new SinkChunkResultBuilder().setJobId(jobId).setChunkId(chunkId).setItems(deliveredChunkItems).build();

        Mockito.when(jobStore.getChunk(jobId, chunkId)).thenReturn(chunkifiedChunk);
        Mockito.when(jobStore.getSinkResult(jobId, chunkId)).thenReturn(deliveredChunk);

        jobCompletionStateFinder.getChunkCompletionState(jobId, chunkId);
    }

    @Test(expected = IllegalStateException.class)
    public void chunkCompletionState_processorItemsAndChunkItemsDifferInSize_throws() throws JobStoreException, NoSuchChunkException {
        final long jobId = 1L;
        final long chunkId = 1L;
        JobCompletionStateFinder jobCompletionStateFinder = new JobCompletionStateFinder(jobStore);
        List<ChunkItem> chunkifiedChunkItems = new ChunkItemStatusListBuilder().setItems(ChunkItem.Status.SUCCESS, 1).build();
        List<ChunkItem> processedChunkItems = new ChunkItemStatusListBuilder().setItems(ChunkItem.Status.SUCCESS, 2).build();
        List<ChunkItem> deliveredChunkItems = new ChunkItemStatusListBuilder().setItems(ChunkItem.Status.SUCCESS, 1).build();
        Chunk chunkifiedChunk = new ChunkBuilder().setJobId(jobId).setChunkId(chunkId).setItems(chunkifiedChunkItems).build();
        ChunkResult processedChunk = new ChunkResultBuilder().setJobId(jobId).setChunkId(chunkId).setItems(processedChunkItems).build();
        SinkChunkResult deliveredChunk = new SinkChunkResultBuilder().setJobId(jobId).setChunkId(chunkId).setItems(deliveredChunkItems).build();

        Mockito.when(jobStore.getChunk(jobId, chunkId)).thenReturn(chunkifiedChunk);
        Mockito.when(jobStore.getProcessorResult(jobId, chunkId)).thenReturn(processedChunk);
        Mockito.when(jobStore.getSinkResult(jobId, chunkId)).thenReturn(deliveredChunk);

        jobCompletionStateFinder.getChunkCompletionState(jobId, chunkId);
    }

    @Test(expected = IllegalStateException.class)
    public void chunkCompletionState_chunkifyChunkAndSinkChunkExistsButProcessChunkIsMissing_throws() throws JobStoreException, NoSuchChunkException {
        final long jobId = 1L;
        final long chunkId = 1L;
        JobCompletionStateFinder jobCompletionStateFinder = new JobCompletionStateFinder(jobStore);
        List<ChunkItem> chunkifiedChunkItems = new ChunkItemStatusListBuilder().setItems(ChunkItem.Status.SUCCESS, 1).build();
        List<ChunkItem> deliveredChunkItems = new ChunkItemStatusListBuilder().setItems(ChunkItem.Status.IGNORE, 1).build();
        Chunk chunkifiedChunk = new ChunkBuilder().setJobId(jobId).setChunkId(chunkId).setItems(chunkifiedChunkItems).build();
        SinkChunkResult deliveredChunk = new SinkChunkResultBuilder().setJobId(jobId).setChunkId(chunkId).setItems(deliveredChunkItems).build();

        Mockito.when(jobStore.getChunk(jobId, chunkId)).thenReturn(chunkifiedChunk);
        Mockito.when(jobStore.getProcessorResult(jobId, chunkId)).thenReturn(null);
        Mockito.when(jobStore.getSinkResult(jobId, chunkId)).thenReturn(deliveredChunk);

        jobCompletionStateFinder.getChunkCompletionState(jobId, chunkId);
    }

    @Test
    public void chunkCompletionState_SuccessUnavailableUnavailable_SuccessIncompleteIncomplete() throws JobStoreException, NoSuchChunkException {
        final long jobId = 1L;
        final long chunkId = 1L;
        setupMockito(jobId, chunkId, ChunkItem.Status.SUCCESS, null, null);

        JobCompletionStateFinder jobCompletionStateFinder = new JobCompletionStateFinder(jobStore);
        ChunkCompletionState chunkCompletionState = jobCompletionStateFinder.getChunkCompletionState(jobId, chunkId);

        assertSingleItemCompletionStateWithStates(chunkCompletionState,
                ItemCompletionState.State.SUCCESS,
                ItemCompletionState.State.INCOMPLETE,
                ItemCompletionState.State.INCOMPLETE);
    }

    @Test
    public void chunkCompletionState_FailureUnavailableUnavailable_FailureIncompleteIncomplete() throws JobStoreException, NoSuchChunkException {
        final long jobId = 1L;
        final long chunkId = 1L;
        setupMockito(jobId, chunkId, ChunkItem.Status.FAILURE, null, null);

        JobCompletionStateFinder jobCompletionStateFinder = new JobCompletionStateFinder(jobStore);
        ChunkCompletionState chunkCompletionState = jobCompletionStateFinder.getChunkCompletionState(jobId, chunkId);

        assertSingleItemCompletionStateWithStates(chunkCompletionState,
                ItemCompletionState.State.FAILURE,
                ItemCompletionState.State.INCOMPLETE,
                ItemCompletionState.State.INCOMPLETE);
    }

    @Test
    public void chunkCompletionState_IgnoreUnavailableUnavailable_IgnoredIncompleteIncomplete() throws JobStoreException, NoSuchChunkException {
        final long jobId = 1L;
        final long chunkId = 1L;
        setupMockito(jobId, chunkId, ChunkItem.Status.IGNORE, null, null);

        JobCompletionStateFinder jobCompletionStateFinder = new JobCompletionStateFinder(jobStore);
        ChunkCompletionState chunkCompletionState = jobCompletionStateFinder.getChunkCompletionState(jobId, chunkId);

        assertSingleItemCompletionStateWithStates(chunkCompletionState,
                ItemCompletionState.State.IGNORED,
                ItemCompletionState.State.INCOMPLETE,
                ItemCompletionState.State.INCOMPLETE);
    }

    @Test
    public void chunkCompletionState_SuccessSuccessUnavailable_SuccessSuccessIncomplete() throws JobStoreException, NoSuchChunkException {
        final long jobId = 1L;
        final long chunkId = 1L;
        setupMockito(jobId, chunkId, ChunkItem.Status.SUCCESS, ChunkItem.Status.SUCCESS, null);

        JobCompletionStateFinder jobCompletionStateFinder = new JobCompletionStateFinder(jobStore);
        ChunkCompletionState chunkCompletionState = jobCompletionStateFinder.getChunkCompletionState(jobId, chunkId);

        assertSingleItemCompletionStateWithStates(chunkCompletionState,
                ItemCompletionState.State.SUCCESS,
                ItemCompletionState.State.SUCCESS,
                ItemCompletionState.State.INCOMPLETE);
    }

    @Test
    public void chunkCompletionState_SuccessFailureUnavailable_SuccessFailureIncomplete() throws JobStoreException, NoSuchChunkException {
        final long jobId = 1L;
        final long chunkId = 1L;
        setupMockito(jobId, chunkId, ChunkItem.Status.SUCCESS, ChunkItem.Status.FAILURE, null);

        JobCompletionStateFinder jobCompletionStateFinder = new JobCompletionStateFinder(jobStore);
        ChunkCompletionState chunkCompletionState = jobCompletionStateFinder.getChunkCompletionState(jobId, chunkId);

        assertSingleItemCompletionStateWithStates(chunkCompletionState,
                ItemCompletionState.State.SUCCESS,
                ItemCompletionState.State.FAILURE,
                ItemCompletionState.State.INCOMPLETE);
    }

    @Test
    public void chunkCompletionState_SuccessIgnoreUnavailable_SuccessIgnoredIncomplete() throws JobStoreException, NoSuchChunkException {
        final long jobId = 1L;
        final long chunkId = 1L;
        setupMockito(jobId, chunkId, ChunkItem.Status.SUCCESS, ChunkItem.Status.IGNORE, null);

        JobCompletionStateFinder jobCompletionStateFinder = new JobCompletionStateFinder(jobStore);
        ChunkCompletionState chunkCompletionState = jobCompletionStateFinder.getChunkCompletionState(jobId, chunkId);

        assertSingleItemCompletionStateWithStates(chunkCompletionState,
                ItemCompletionState.State.SUCCESS,
                ItemCompletionState.State.IGNORED,
                ItemCompletionState.State.INCOMPLETE);
    }

    @Test
    public void chunkCompletionState_SuccessFailureIgnore_SuccessFailureIgnored() throws JobStoreException, NoSuchChunkException {
        final long jobId = 1L;
        final long chunkId = 1L;
        setupMockito(jobId, chunkId, ChunkItem.Status.SUCCESS, ChunkItem.Status.FAILURE, ChunkItem.Status.IGNORE);

        JobCompletionStateFinder jobCompletionStateFinder = new JobCompletionStateFinder(jobStore);
        ChunkCompletionState chunkCompletionState = jobCompletionStateFinder.getChunkCompletionState(jobId, chunkId);

        assertSingleItemCompletionStateWithStates(chunkCompletionState,
                ItemCompletionState.State.SUCCESS,
                ItemCompletionState.State.FAILURE,
                ItemCompletionState.State.IGNORED);
    }

    @Test
    public void chunkCompletionState_complexExample() throws JobStoreException, NoSuchChunkException {
        final long jobId = 1L;
        final long chunkId = 1L;
        final int chunkSize = 7;
        JobCompletionStateFinder jobCompletionStateFinder = new JobCompletionStateFinder(jobStore);
        List<ChunkItem> chunkifiedItems = new ChunkItemStatusListBuilder()
                .setItem(ChunkItem.Status.SUCCESS)
                .setItem(ChunkItem.Status.SUCCESS)
                .setItem(ChunkItem.Status.SUCCESS)
                .setItem(ChunkItem.Status.SUCCESS)
                .setItem(ChunkItem.Status.SUCCESS)
                .setItem(ChunkItem.Status.FAILURE)
                .setItem(ChunkItem.Status.IGNORE)
                .build();
        List<ChunkItem> processedItems = new ChunkItemStatusListBuilder()
                .setItem(ChunkItem.Status.SUCCESS)
                .setItem(ChunkItem.Status.SUCCESS)
                .setItem(ChunkItem.Status.SUCCESS)
                .setItem(ChunkItem.Status.FAILURE)
                .setItem(ChunkItem.Status.IGNORE)
                .setItem(ChunkItem.Status.IGNORE)
                .setItem(ChunkItem.Status.IGNORE)
                .build();
        List<ChunkItem> deliveredItems = new ChunkItemStatusListBuilder()
                .setItem(ChunkItem.Status.SUCCESS)
                .setItem(ChunkItem.Status.FAILURE)
                .setItem(ChunkItem.Status.IGNORE)
                .setItem(ChunkItem.Status.IGNORE)
                .setItem(ChunkItem.Status.IGNORE)
                .setItem(ChunkItem.Status.IGNORE)
                .setItem(ChunkItem.Status.IGNORE)
                .build();
        Chunk chunkifiedChunk = new ChunkBuilder().setJobId(jobId).setChunkId(chunkId).setItems(chunkifiedItems).build();
        ChunkResult processedChunk = new ChunkResultBuilder().setJobId(jobId).setChunkId(chunkId).setItems(processedItems).build();
        SinkChunkResult deliveredChunk = new SinkChunkResultBuilder().setJobId(jobId).setChunkId(chunkId).setItems(deliveredItems).build();

        Mockito.when(jobStore.getChunk(jobId, chunkId)).thenReturn(chunkifiedChunk);
        Mockito.when(jobStore.getProcessorResult(jobId, chunkId)).thenReturn(processedChunk);
        Mockito.when(jobStore.getSinkResult(jobId, chunkId)).thenReturn(deliveredChunk);

        ChunkCompletionState chunkCompletionState = jobCompletionStateFinder.getChunkCompletionState(jobId, chunkId);
        assertThat(chunkCompletionState.getItems().size(), is(chunkSize));
        assertItemCompletionStateWithStates(chunkCompletionState, 0, ItemCompletionState.State.SUCCESS, ItemCompletionState.State.SUCCESS, ItemCompletionState.State.SUCCESS);
        assertItemCompletionStateWithStates(chunkCompletionState, 1, ItemCompletionState.State.SUCCESS, ItemCompletionState.State.SUCCESS, ItemCompletionState.State.FAILURE);
        assertItemCompletionStateWithStates(chunkCompletionState, 2, ItemCompletionState.State.SUCCESS, ItemCompletionState.State.SUCCESS, ItemCompletionState.State.IGNORED);
        assertItemCompletionStateWithStates(chunkCompletionState, 3, ItemCompletionState.State.SUCCESS, ItemCompletionState.State.FAILURE, ItemCompletionState.State.IGNORED);
        assertItemCompletionStateWithStates(chunkCompletionState, 4, ItemCompletionState.State.SUCCESS, ItemCompletionState.State.IGNORED, ItemCompletionState.State.IGNORED);
        assertItemCompletionStateWithStates(chunkCompletionState, 5, ItemCompletionState.State.FAILURE, ItemCompletionState.State.IGNORED, ItemCompletionState.State.IGNORED);
        assertItemCompletionStateWithStates(chunkCompletionState, 6, ItemCompletionState.State.IGNORED, ItemCompletionState.State.IGNORED, ItemCompletionState.State.IGNORED);
    }

    @Test
    public void jobCompletionState_happyPath() throws JobStoreException {
        final long jobId = 47L;
        createChunkifyChunkAndSetItUpInMockito(jobId, 1L, ChunkItem.Status.SUCCESS, ChunkItem.Status.SUCCESS);
        createChunkifyChunkAndSetItUpInMockito(jobId, 2L, ChunkItem.Status.SUCCESS, ChunkItem.Status.SUCCESS);
        createChunkifyChunkAndSetItUpInMockito(jobId, 3L, ChunkItem.Status.SUCCESS, ChunkItem.Status.SUCCESS);

        createProcessingChunkAndSetItUpInMockito(jobId, 1L, ChunkItem.Status.SUCCESS, ChunkItem.Status.SUCCESS);
        createProcessingChunkAndSetItUpInMockito(jobId, 2L, ChunkItem.Status.SUCCESS, ChunkItem.Status.FAILURE);
        createProcessingChunkAndSetItUpInMockito(jobId, 3L, ChunkItem.Status.FAILURE, ChunkItem.Status.SUCCESS);

        createDeliveringChunkAndSetItUpInMockito(jobId, 1L, ChunkItem.Status.SUCCESS, ChunkItem.Status.SUCCESS);
        createDeliveringChunkAndSetItUpInMockito(jobId, 2L, ChunkItem.Status.SUCCESS, ChunkItem.Status.IGNORE);

        when(jobStore.getNumberOfChunksInJob(jobId)).thenReturn(3L);

        JobCompletionStateFinder jobCompletionStateFinder = new JobCompletionStateFinder(jobStore);
        JobCompletionState jobCompletionState = jobCompletionStateFinder.getJobCompletionState(jobId);
        
        assertThat(jobCompletionState.getJobId(), is(jobId));
        assertThat(jobCompletionState.getChunks().size(), is(3));
        
        List<ChunkCompletionState> chunkCompletionStates = jobCompletionState.getChunks();
        ChunkCompletionState chunkCompletionState1 = chunkCompletionStates.get(0);
        ChunkCompletionState chunkCompletionState2 = chunkCompletionStates.get(1);
        ChunkCompletionState chunkCompletionState3 = chunkCompletionStates.get(2);

        assertItemCompletionStateWithStates(chunkCompletionState1, 0, ItemCompletionState.State.SUCCESS, ItemCompletionState.State.SUCCESS, ItemCompletionState.State.SUCCESS);
        assertItemCompletionStateWithStates(chunkCompletionState1, 1, ItemCompletionState.State.SUCCESS, ItemCompletionState.State.SUCCESS, ItemCompletionState.State.SUCCESS);
        
        assertItemCompletionStateWithStates(chunkCompletionState2, 0, ItemCompletionState.State.SUCCESS, ItemCompletionState.State.SUCCESS, ItemCompletionState.State.SUCCESS);
        assertItemCompletionStateWithStates(chunkCompletionState2, 1, ItemCompletionState.State.SUCCESS, ItemCompletionState.State.FAILURE, ItemCompletionState.State.IGNORED);
        
        assertItemCompletionStateWithStates(chunkCompletionState3, 0, ItemCompletionState.State.SUCCESS, ItemCompletionState.State.FAILURE, ItemCompletionState.State.INCOMPLETE);
        assertItemCompletionStateWithStates(chunkCompletionState3, 1, ItemCompletionState.State.SUCCESS, ItemCompletionState.State.SUCCESS, ItemCompletionState.State.INCOMPLETE);
    }
    
    @Test(expected=JobStoreException.class)
    public void getJobCompletionState_unknownJobId_throws() throws JobStoreException, IOException {
        JobStore jobStore = new FileSystemJobStore(tmpFolder.newFolder("jobstore").toPath());
        jobStore.getJobCompletionState(123456L);
    }
    
   
    private void createChunkifyChunkAndSetItUpInMockito(long jobId, long chunkId, ChunkItem.Status... states) throws JobStoreException {
        List<ChunkItem> chunkItemList = createChunkItemList(states);
        Chunk chunkifiedChunk = new ChunkBuilder().setJobId(jobId).setChunkId(chunkId).setItems(chunkItemList).build();
        Mockito.when(jobStore.getChunk(jobId, chunkId)).thenReturn(chunkifiedChunk);
    }

    private void createProcessingChunkAndSetItUpInMockito(long jobId, long chunkId, ChunkItem.Status... states) throws JobStoreException {
        List<ChunkItem> chunkItemList = createChunkItemList(states);
        ChunkResult processingChunk = new ChunkResultBuilder().setJobId(jobId).setChunkId(chunkId).setItems(chunkItemList).build();
        Mockito.when(jobStore.getProcessorResult(jobId, chunkId)).thenReturn(processingChunk);
    }

    private void createDeliveringChunkAndSetItUpInMockito(long jobId, long chunkId, ChunkItem.Status... states) throws JobStoreException {
        List<ChunkItem> chunkItemList = createChunkItemList(states);
        SinkChunkResult deliveringChunk = new SinkChunkResultBuilder().setJobId(jobId).setChunkId(chunkId).setItems(chunkItemList).build();
        Mockito.when(jobStore.getSinkResult(jobId, chunkId)).thenReturn(deliveringChunk);
    }

    private  List<ChunkItem> createChunkItemList(ChunkItem.Status... states) {
        ChunkItemStatusListBuilder chunkItemStatusListBuilder = new ChunkItemStatusListBuilder();
        for (ChunkItem.Status state : states) {
            chunkItemStatusListBuilder.setItem(state);
        }
        return chunkItemStatusListBuilder.build();
    }
    
    private void setupMockito(long jobId, long chunkId, ChunkItem.Status chunkifyStatus, ChunkItem.Status processingStatus, ChunkItem.Status deliveringStatus) throws JobStoreException {
        // Setup chunkifyChunk:
        if (chunkifyStatus != null) {
            List<ChunkItem> chunkItems = new ChunkItemStatusListBuilder()
                    .setItem(chunkifyStatus)
                    .build();
            Chunk chunkifiedChunk = new ChunkBuilder().setJobId(jobId).setChunkId(chunkId).setItems(chunkItems).build();
            Mockito.when(jobStore.getChunk(jobId, chunkId)).thenReturn(chunkifiedChunk);
        } else {
            Mockito.when(jobStore.getChunk(jobId, chunkId)).thenReturn(null);
        }

        // Setup processingChunk:
        if (processingStatus != null) {
            List<ChunkItem> chunkItems = new ChunkItemStatusListBuilder()
                    .setItem(processingStatus)
                    .build();
            ChunkResult processedChunk = new ChunkResultBuilder().setJobId(jobId).setChunkId(chunkId).setItems(chunkItems).build();
            Mockito.when(jobStore.getProcessorResult(jobId, chunkId)).thenReturn(processedChunk);
        } else {
            Mockito.when(jobStore.getProcessorResult(jobId, chunkId)).thenReturn(null);
        }

        // Setup deliveringChunk:
        if (deliveringStatus != null) {
            List<ChunkItem> chunkItems = new ChunkItemStatusListBuilder()
                    .setItem(deliveringStatus)
                    .build();
            SinkChunkResult deliveredChunk = new SinkChunkResultBuilder().setJobId(jobId).setChunkId(chunkId).setItems(chunkItems).build();
            Mockito.when(jobStore.getSinkResult(jobId, chunkId)).thenReturn(deliveredChunk);
        } else {
            Mockito.when(jobStore.getSinkResult(jobId, chunkId)).thenReturn(null);
        }
    }

    private void assertSingleItemCompletionStateWithStates(ChunkCompletionState chunkCompletionState,
            ItemCompletionState.State chunkifyState,
            ItemCompletionState.State processorState,
            ItemCompletionState.State deliveryState) {
        assertThat(chunkCompletionState.getItems().size(), is(1));
        assertItemCompletionStateWithStates(chunkCompletionState, 0, chunkifyState, processorState, deliveryState);
    }

    private void assertItemCompletionStateWithStates(ChunkCompletionState chunkCompletionState,
            int index,
            ItemCompletionState.State chunkifyState,
            ItemCompletionState.State processorState,
            ItemCompletionState.State deliveryState) {
        ItemCompletionState itemCompletionState = chunkCompletionState.getItems().get(index);
        assertThat(itemCompletionState.getChunkifyState(), is(chunkifyState));
        assertThat(itemCompletionState.getProcessingState(), is(processorState));
        assertThat(itemCompletionState.getDeliveryState(), is(deliveryState));
    }

    private class ChunkItemStatusListBuilder {

        private List<ChunkItem> chunkItems = new ArrayList<>();

        public ChunkItemStatusListBuilder() {
        }

        public ChunkItemStatusListBuilder setItems(ChunkItem.Status status, int numberOfItemsToSet) {
            for (int i = 0; i < numberOfItemsToSet; i++) {
                chunkItems.add(new ChunkItem(chunkItems.size() + 1, "", status));
            }
            return this;
        }

        public ChunkItemStatusListBuilder setItem(ChunkItem.Status status) {
            chunkItems.add(new ChunkItem(chunkItems.size() + 1, "", status));
            return this;
        }

        public List<ChunkItem> build() {
            return chunkItems;
        }
    }
}
