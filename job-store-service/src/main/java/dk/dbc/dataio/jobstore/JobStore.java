package dk.dbc.dataio.jobstore;

import dk.dbc.dataio.jobstore.types.Chunk;
import dk.dbc.dataio.jobstore.types.Job;
import dk.dbc.dataio.jobstore.types.JobStoreException;
import dk.dbc.dataio.jobstore.types.ProcessChunkResult;

import dk.dbc.dataio.commons.types.Flow;

import java.nio.file.Path;

public interface JobStore {
    Job createJob(Path dataObjectPath, Flow flow) throws JobStoreException;

    long getNumberOfChunksInJob(Job job) throws JobStoreException;

    Chunk getChunk(Job job, long i) throws JobStoreException;

    void addChunkResult(Job job, ProcessChunkResult processChunkResult) throws JobStoreException;

    ProcessChunkResult getProcessChunkResult(Job job, long i) throws JobStoreException;
}
