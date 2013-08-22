package dk.dbc.dataio.engine;

import java.nio.file.Path;

interface JobStore {
    Job createJob(Path dataObjectPath, Flow flow) throws JobStoreException;
    
    void addChunk(Job job, Chunk chunk) throws JobStoreException;

    long getNumberOfChunksInJob(Job job) throws JobStoreException;

    Chunk getChunk(Job job, long i) throws JobStoreException;

    void addChunkResult(Job job, ProcessChunkResult processChunkResult) throws JobStoreException;

    ProcessChunkResult getProcessChunkResult(Job job, long i) throws JobStoreException;
}
