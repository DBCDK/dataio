package dk.dbc.dataio.jobstore;

import dk.dbc.dataio.commons.types.Flow;
import dk.dbc.dataio.jobstore.types.Chunk;
import dk.dbc.dataio.jobstore.types.Job;
import dk.dbc.dataio.jobstore.types.JobStoreException;

import java.nio.file.Path;

public interface JobStoreBean {
    Job createJob(Path jobPath, Flow flow) throws JobStoreException;

    long getNumberOfChunksInJob(Job job) throws JobStoreException;

    Chunk getChunk(Job job, long chunkId) throws JobStoreException;
}
