package dk.dbc.dataio.jobstore;

import dk.dbc.dataio.jobstore.types.Chunk;
import dk.dbc.dataio.jobstore.types.Job;
import dk.dbc.dataio.jobstore.types.JobStoreException;
import dk.dbc.dataio.jobstore.types.ProcessChunkResult;

import dk.dbc.dataio.commons.types.Flow;
import dk.dbc.dataio.commons.types.JobSpecification;


public interface JobStore {
    Job createJob(JobSpecification jobSpec, Flow flow) throws JobStoreException;

    long getNumberOfChunksInJob(Job job) throws JobStoreException;

    Chunk getChunk(Job job, long chunkId) throws JobStoreException;

    void addChunkResult(Job job, ProcessChunkResult processChunkResult) throws JobStoreException;

    ProcessChunkResult getProcessChunkResult(Job job, long chunkId) throws JobStoreException;
}
