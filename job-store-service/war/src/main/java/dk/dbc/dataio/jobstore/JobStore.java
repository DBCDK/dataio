package dk.dbc.dataio.jobstore;

import dk.dbc.dataio.commons.types.Chunk;
import dk.dbc.dataio.commons.types.ChunkResult;
import dk.dbc.dataio.commons.types.Flow;
import dk.dbc.dataio.commons.types.FlowBinder;
import dk.dbc.dataio.commons.types.JobCompletionState;
import dk.dbc.dataio.commons.types.JobInfo;
import dk.dbc.dataio.commons.types.JobSpecification;
import dk.dbc.dataio.commons.types.Sink;
import dk.dbc.dataio.commons.types.SinkChunkResult;
import dk.dbc.dataio.jobstore.types.Job;
import dk.dbc.dataio.commons.types.JobState;
import dk.dbc.dataio.commons.types.SupplementaryProcessData;
import dk.dbc.dataio.jobstore.types.JobStoreException;
import dk.dbc.dataio.sequenceanalyser.keygenerator.SequenceAnalyserKeyGenerator;

import java.io.InputStream;

import java.util.List;


public interface JobStore {
    Job createJob(JobSpecification jobSpec, FlowBinder flowBinder, Flow flow, Sink sink, InputStream jobInputStream, SequenceAnalyserKeyGenerator sequenceAnalyserKeyGenerator) throws JobStoreException;

    void updateJobInfo(Job job, JobInfo jobInfo) throws JobStoreException;

    List<JobInfo> getAllJobInfos() throws JobStoreException;

    long getNumberOfChunksInJob(long jobId) throws JobStoreException;

    Chunk getChunk(long jobId, long chunkId) throws JobStoreException;

    void addProcessorResult(ChunkResult processorResult) throws JobStoreException;
    ChunkResult getProcessorResult(long jobId, long chunkId) throws JobStoreException;

    void addSinkResult(SinkChunkResult sinkResult) throws JobStoreException;
    SinkChunkResult getSinkResult(long jobId, long chunkId) throws JobStoreException;

    Sink getSink(long jobId) throws JobStoreException;
    JobState getJobState(long jobId) throws JobStoreException;

    JobCompletionState getJobCompletionState(long jobId) throws JobStoreException;

    SupplementaryProcessData getSupplementaryProcessData(long jobId) throws JobStoreException;

    Flow getFlow(long jobId) throws JobStoreException;
}
