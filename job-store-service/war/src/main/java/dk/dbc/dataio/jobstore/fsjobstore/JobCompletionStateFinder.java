package dk.dbc.dataio.jobstore.fsjobstore;

import dk.dbc.dataio.commons.types.AbstractChunk;
import dk.dbc.dataio.commons.types.Chunk;
import dk.dbc.dataio.commons.types.ChunkCompletionState;
import dk.dbc.dataio.commons.types.ChunkItem;
import dk.dbc.dataio.commons.types.ChunkResult;
import dk.dbc.dataio.commons.types.Constants;
import dk.dbc.dataio.commons.types.ItemCompletionState;
import dk.dbc.dataio.commons.types.JobCompletionState;
import dk.dbc.dataio.commons.types.SinkChunkResult;
import dk.dbc.dataio.jobstore.JobStore;
import dk.dbc.dataio.jobstore.types.JobStoreException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

class JobCompletionStateFinder {
    private static final Logger LOGGER = LoggerFactory.getLogger(JobCompletionStateFinder.class);

    private final JobStore jobStore;

    public JobCompletionStateFinder(JobStore jobStore) {
        this.jobStore = jobStore;
    }

    public JobCompletionState getJobCompletionState(long jobId) throws JobStoreException {
        List<ChunkCompletionState> chunkCompletionStates = getAllChunkCompletionStatesForJob(jobId);
        JobCompletionState jobCompletionState = new JobCompletionState(jobId, chunkCompletionStates);
        return jobCompletionState;
    }

    public ChunkCompletionState getChunkCompletionState(long jobId, long chunkId) throws JobStoreException, NoSuchChunkException {
        Chunk chunkifiedChunk = jobStore.getChunk(jobId, chunkId);
        ChunkResult processedChunk = jobStore.getProcessorResult(jobId, chunkId);
        SinkChunkResult deliveredChunk = jobStore.getSinkResult(jobId, chunkId);

        // There must be a chunkifiedChunk in order to detect the size of the chunks
        // It is assumed that all chunks have equal size.
        // If no chunkifiedChunk is found an exception is thrown.
        // todo: Should it be a NoSuchChunkException?
        if(chunkifiedChunk == null) {
            String message = String.format("No chunk with id: [%d] found in job: [%d]", chunkId, jobId);
            LOGGER.info(message);
            throw new NoSuchChunkException(message);
        }

        int chunkSize = chunkifiedChunk.getItems().size(); // this size must hold for all chunks!

        // if processedChunk is null, throw an exception
        // - this should not happen, since there is both a
        // chunkifiedChunk and a deliveredChunk.
        if(processedChunk == null && deliveredChunk != null) {
            // this is wrong
            String message = String.format("No process chunk found for [%d, %d], even though both a chunkified chunk and a sink chunk exists.",
                    jobId, chunkId);
            LOGGER.info(message);
            throw new IllegalStateException(message);
        }
        
        // Check if the processedChunk chunk size differes from the chunkified chunk:
        if(processedChunk != null && processedChunk.getItems().size() != chunkSize) {
            String message = String.format("The chunkified chunk and the delivered chunk differ in size: [%d / %d] for chunkId [%d] in jobId [%d]",
                    chunkifiedChunk.getItems().size(), deliveredChunk.getItems().size(), chunkId, jobId);
            LOGGER.info(message);
            throw new IllegalStateException(message);
        }
        // Check if the delivered chunk size differes from the chunkified chunk:
        if(deliveredChunk != null && deliveredChunk.getItems().size() != chunkSize) {
            String message = String.format("The chunkified chunk and the delivered chunk differ in size: [%d / %d] for chunkId [%d] in jobId [%d]",
                    chunkifiedChunk.getItems().size(), deliveredChunk.getItems().size(), chunkId, jobId);
            LOGGER.info(message);
            throw new IllegalStateException(message);
        }

        long[] itemIds = getItemIdsFromChunk(chunkifiedChunk);
        if(!validateItemIds(itemIds)) {
            throw new IllegalStateException("The ItemIds in chunkifiedChunks are not valid");
        }

        List<ItemCompletionState> itemCompletionStates = new ArrayList<>();
        for(long id : itemIds) {
            ItemCompletionState.State chunkifiedStatus = convertChunkItemStatusToItemCompletionStatus(getItem(chunkifiedChunk, id).getStatus());
            ItemCompletionState.State processStatus = processedChunk != null ? convertChunkItemStatusToItemCompletionStatus(getItem(processedChunk, id).getStatus()) : ItemCompletionState.State.INCOMPLETE;
            ItemCompletionState.State deliveryStatus = deliveredChunk != null ? convertChunkItemStatusToItemCompletionStatus(getItem(deliveredChunk, id).getStatus()) : ItemCompletionState.State.INCOMPLETE;
            
            itemCompletionStates.add(new ItemCompletionState(id, chunkifiedStatus, processStatus, deliveryStatus));
        }
        return new ChunkCompletionState(chunkId, itemCompletionStates);
    }

    private ChunkItem getItem(AbstractChunk chunk, long id) {
        for (ChunkItem item : chunk.getItems()) {
            if (item.getId() == id) {
                return item;
            }
        }
        return null;
    }
    
    private long[] getItemIdsFromChunk(AbstractChunk chunk) {
        long[] ids = new long[chunk.getItems().size()];
        int idx = 0;
        for(ChunkItem item : chunk.getItems()) {
            ids[idx++] = item.getId();
        }
        Arrays.sort(ids);
        return ids;
    }

    private boolean validateItemIds(long[] ids) {
        long id = ids[0];
        for(int i=1;i<ids.length;i++) {
            if(ids[i] != id+1)
                return false;
            id++;
        }
        return true;
    }

    private ItemCompletionState.State convertChunkItemStatusToItemCompletionStatus(ChunkItem.Status status) throws JobStoreException {
        final ItemCompletionState.State completionState;
        switch(status) {
            case FAILURE:
                completionState = ItemCompletionState.State.FAILURE;
                break;
            case IGNORE:
                completionState = ItemCompletionState.State.IGNORED;
                break;
            case SUCCESS:
                completionState = ItemCompletionState.State.SUCCESS;
                break;
            default:
                throw new JobStoreException("Unhandled state: " + status);
        }
        return completionState;
    }

    private List<ChunkCompletionState> getAllChunkCompletionStatesForJob(long jobId) throws JobStoreException {
        long numberOfChunksInJob = jobStore.getNumberOfChunksInJob(jobId);
        LOGGER.info("NumberOfChunksInJob: job: {}  numChunks: {}", jobId, numberOfChunksInJob);
        List<ChunkCompletionState> chunkCompletionStates = new ArrayList<>((int) numberOfChunksInJob);
        for(long i=0L, chunkId = Constants.CHUNK_ID_LOWER_BOUND+1; i<numberOfChunksInJob; i++, chunkId++) {
            ChunkCompletionState chunkCompletionState;
            try {
                chunkCompletionState = getChunkCompletionState(jobId, chunkId);
                chunkCompletionStates.add(chunkCompletionState);
            } catch(NoSuchChunkException ex) {
                LOGGER.debug("Could not find chunk: {} for job: {}", chunkId, jobId);
            }
        }
        return chunkCompletionStates;
    }


}
