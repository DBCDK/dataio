package dk.dbc.dataio.jobstore;

import dk.dbc.dataio.commons.types.Flow;
import dk.dbc.dataio.commons.types.JobInfo;
import dk.dbc.dataio.commons.types.JobSpecification;
import dk.dbc.dataio.jobstore.types.Chunk;
import dk.dbc.dataio.jobstore.types.Job;
import dk.dbc.dataio.jobstore.types.JobStoreException;
import dk.dbc.dataio.commons.types.ChunkResult;
import dk.dbc.dataio.commons.types.FlowBinder;
import dk.dbc.dataio.commons.types.Sink;


public interface JobStore {
    Job createJob(JobSpecification jobSpec, FlowBinder flowBinder, Flow flow, Sink sink) throws JobStoreException;

    void updateJobInfo(Job job, JobInfo jobInfo) throws JobStoreException;

    long getNumberOfChunksInJob(Job job) throws JobStoreException;

    Chunk getChunk(Job job, long chunkId) throws JobStoreException;

    void addChunkResult(Job job, ChunkResult processChunkResult) throws JobStoreException;

    ChunkResult getProcessChunkResult(Job job, long chunkId) throws JobStoreException;
}
