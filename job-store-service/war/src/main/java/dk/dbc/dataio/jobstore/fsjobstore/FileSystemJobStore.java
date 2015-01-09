package dk.dbc.dataio.jobstore.fsjobstore;

import dk.dbc.dataio.commons.types.Chunk;
import dk.dbc.dataio.commons.types.ChunkItem;
import dk.dbc.dataio.commons.types.ChunkResult;
import dk.dbc.dataio.commons.types.Constants;
import dk.dbc.dataio.commons.types.Flow;
import dk.dbc.dataio.commons.types.FlowBinder;
import dk.dbc.dataio.commons.types.JobCompletionState;
import dk.dbc.dataio.commons.types.JobErrorCode;
import dk.dbc.dataio.commons.types.JobInfo;
import dk.dbc.dataio.commons.types.JobSpecification;
import dk.dbc.dataio.commons.types.JobState;
import dk.dbc.dataio.commons.types.Sink;
import dk.dbc.dataio.commons.types.SinkChunkResult;
import dk.dbc.dataio.commons.types.SupplementaryProcessData;
import dk.dbc.dataio.commons.utils.json.JsonException;
import dk.dbc.dataio.commons.utils.json.JsonUtil;
import dk.dbc.dataio.jobstore.JobStore;
import dk.dbc.dataio.jobstore.recordsplitter.DefaultXMLRecordSplitter;
import dk.dbc.dataio.jobstore.types.IllegalDataException;
import dk.dbc.dataio.jobstore.types.Job;
import dk.dbc.dataio.jobstore.types.JobStoreException;
import dk.dbc.dataio.sequenceanalyser.keygenerator.SequenceAnalyserKeyGenerator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.xml.stream.XMLStreamException;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static dk.dbc.dataio.jobstore.util.Base64Util.base64encode;

public class FileSystemJobStore implements JobStore {

    static final String FLOW_FILE = "flow.json";
    static final String FLOWBINDER_FILE = "flowbinder.json";
    static final String SINK_FILE = "sink.json";
    static final String JOBSPECIFICATION_FILE = "jobspec.json";
    static final String JOBINFO_FILE = "jobinfo.json";
    static final String JOBSTATE_FILE = "jobstate.json";
    static final String CHUNK_COUNTER_FILE = "chunk.cnt";
    static final String CHUNK_FILENAME_PATTERN = "%d.json";
    static final String PROCESSOR_COUNTER_FILE = "processor.cnt";
    static final String PROCESSOR_RESULT_FILENAME_PATTERN = "%d.res.json";
    static final String SINK_COUNTER_FILE = "sink.cnt";
    static final String SINK_RESULT_FILENAME_PATTERN = "%d.sink.json";
    static final Charset LOCAL_CHARSET = Charset.forName("UTF-8");

    private static final Logger LOGGER = LoggerFactory.getLogger(FileSystemJobStore.class);

    private final Path storePath;
    private final ChunkFileHandler<Chunk> chunkFileHandler;
    private final ChunkFileHandler<ChunkResult> processorResultFileHandler;
    private final ChunkFileHandler<SinkChunkResult> sinkResultFileHandler;

    public FileSystemJobStore(Path storePath) throws JobStoreException {
        if (storePath == null) {
            throw new NullPointerException("storePath can not be null");
        }
        if (!canUseExistingStorePath(storePath)) {
            createDirectory(storePath);
        }
        this.storePath = storePath;
        this.chunkFileHandler = new ChunkFileHandler<>(Chunk.class, storePath, CHUNK_COUNTER_FILE, CHUNK_FILENAME_PATTERN);
        this.processorResultFileHandler = new ChunkFileHandler<>(ChunkResult.class, storePath, PROCESSOR_COUNTER_FILE, PROCESSOR_RESULT_FILENAME_PATTERN);
        this.sinkResultFileHandler = new ChunkFileHandler<>(SinkChunkResult.class, storePath, SINK_COUNTER_FILE, SINK_RESULT_FILENAME_PATTERN);

        LOGGER.info("Placing job store in {}", this.storePath);
    }

    private boolean canUseExistingStorePath(Path storePath) throws JobStoreException {
        if (Files.exists(storePath)) {
            if (!Files.isDirectory(storePath)) {
                throw new JobStoreException(String.format("Job store path is not a directory: %s", storePath));
            }
            return true;
        }
        return false;
    }

    private void createDirectory(Path dir) throws JobStoreException {
        try {
            LOGGER.info("Creating directory: {}", dir.toString());
            Files.createDirectory(dir);
        } catch (IOException e) {
            throw new JobStoreException(String.format("Unable to create directory: %s", dir), e);
        }
    }

    @Override
    public Job createJob(JobSpecification jobSpec, FlowBinder flowBinder, Flow flow, Sink sink, InputStream jobInputStream, SequenceAnalyserKeyGenerator sequenceAnalyserKeyGenerator) throws JobStoreException {
        final long jobCreationTime = System.currentTimeMillis();
        final long jobId = jobCreationTime;
        final Path jobPath = getJobPath(jobId);
        long recordCount;

        LOGGER.info("Creating job in {}", jobPath);
        createDirectory(getJobPath(jobId));
        createDirectory(getChunksPath(jobId));

        storeJsonObjectAsFileInJob(jobPath, FLOWBINDER_FILE, flowBinder, flowBinder.getId());
        storeJsonObjectAsFileInJob(jobPath, FLOW_FILE, flow, flow.getId());
        storeJsonObjectAsFileInJob(jobPath, SINK_FILE, sink, sink.getId());
        storeJsonObjectAsFileInJob(jobPath, JOBSPECIFICATION_FILE, jobSpec, jobId);
        chunkFileHandler.createCounterFile(jobId);
        processorResultFileHandler.createCounterFile(jobId);
        sinkResultFileHandler.createCounterFile(jobId);

        JobInfo jobInfo = new JobInfo(jobId, jobSpec, jobCreationTime);

        Job job = new Job(jobInfo, new JobState(), flow);
        setJobState(jobId, job.getJobState());
        try {
            job.getJobState().setLifeCycleStateFor(JobState.OperationalState.CHUNKIFYING, JobState.LifeCycleState.ACTIVE);
            setJobState(jobId, job.getJobState());

            final DefaultXMLRecordSplitter recordSplitter;
            try {
                recordSplitter = newRecordSplitter(jobSpec, jobInputStream);
                recordCount = applyRecordSplitter(job, recordSplitter, sequenceAnalyserKeyGenerator, sink);
            } catch (IOException | IllegalStateException | XMLStreamException | IllegalDataException e) {
                jobInfo = new JobInfo(jobId, jobSpec, jobCreationTime);
                if (e instanceof IOException) {
                    jobInfo.setJobErrorCode(JobErrorCode.DATA_FILE_NOT_FOUND);
                } else if (e instanceof IllegalStateException) {
                    jobInfo.setJobErrorCode(JobErrorCode.DATA_FILE_ENCODING_MISMATCH);
                } else if (e instanceof XMLStreamException || e instanceof IllegalDataException) {
                    jobInfo.setJobErrorCode(JobErrorCode.DATA_FILE_INVALID);
                }
                job = new Job(jobInfo, job.getJobState(), flow);
                return job;
            }

            jobInfo = new JobInfo(jobId, jobSpec, jobCreationTime);
            jobInfo.setJobRecordCount(recordCount);
            job = new Job(jobInfo, job.getJobState(), flow);
            return job;
        } finally {
            job.getJobState().setLifeCycleStateFor(JobState.OperationalState.CHUNKIFYING, JobState.LifeCycleState.DONE);
            setJobState(jobId, job.getJobState());
            updateJobInfo(job, jobInfo);
        }
    }

    @Override
    public void updateJobInfo(Job job, JobInfo jobInfo) throws JobStoreException {
        try {
            LOGGER.debug("Updating job info: {}", JsonUtil.toJson(jobInfo));
        } catch (JsonException e) {
            LOGGER.error("Error marshalling JobInfo object into JSON", e);
        }
        final Path jobPath = getJobPath(job.getId());
        storeJobInfoInJob(jobPath, jobInfo);
    }

    // Not thread-safe
    private void setJobState(long jobId, JobState jobState) throws JobStoreException {
        JsonFileUtil.getJsonFileUtil(LOCAL_CHARSET).writeObjectToFile(getJobStatePath(jobId), jobState);
    }

    @Override
    public Sink getSink(long jobId) throws JobStoreException {
        final Path sinkPath = getSinkPath(jobId);
        if (Files.exists(sinkPath)) {
            return JsonFileUtil.getJsonFileUtil(LOCAL_CHARSET).readObjectFromFile(sinkPath, Sink.class);
        } else {
            return null;
        }
    }

    @Override
    public SupplementaryProcessData getSupplementaryProcessData(long jobId) throws JobStoreException {
        final Path jobSpecificationPath = getJobSpecificationPath(jobId);
        if (Files.exists(jobSpecificationPath)) {
            JobSpecification jobSpecification = JsonFileUtil.getJsonFileUtil(LOCAL_CHARSET).readObjectFromFile(jobSpecificationPath, JobSpecification.class);
            return createSupplementaryProcessData(jobSpecification);
        } else {
            return null;
        }
    }

    @Override
    public synchronized JobState getJobState(long jobId) throws JobStoreException {
        final Path jobStatePath = getJobStatePath(jobId);
        if (Files.exists(jobStatePath)) {
            return JsonFileUtil.getJsonFileUtil(LOCAL_CHARSET).readObjectFromFile(jobStatePath, JobState.class);
        } else {
            return null;
        }
    }

    @Override
    public List<JobInfo> getAllJobInfos() throws JobStoreException {
        final List<Path> jobPaths = getDirectories(storePath);
        List<JobInfo> jobInfos = new ArrayList<>(jobPaths.size());
        for (Path jobPath : jobPaths) {
            jobInfos.add(readJobInfoFromJob(jobPath));
        }
        return jobInfos;
    }

//    private void storeFlowBinderInJob(Path jobPath, FlowBinder flowBinder) throws JobStoreException {
//        final Path flowPath = Paths.get(jobPath.toString(), FLOWBINDER_FILE);
//        LOGGER.info("Creating FlowBinder json-file: {}", flowPath);
//        try (BufferedWriter bw = Files.newBufferedWriter(flowPath, LOCAL_CHARSET)) {
//          bw.write(JsonUtil.toJson(flowBinder));
//        } catch (IOException | JsonException e) {
//            throw new JobStoreException(String.format("Exception caught when trying to write FlowBinder: %d", flowBinder.getId()), e);
//        }
//    }
//
//    private void storeFlowInJob(Path jobPath, Flow flow) throws JobStoreException {
//        final Path flowPath = Paths.get(jobPath.toString(), FLOW_FILE);
//        LOGGER.info("Creating Flow json-file: {}", flowPath);
//        try (BufferedWriter bw = Files.newBufferedWriter(flowPath, LOCAL_CHARSET)) {
//          bw.write(JsonUtil.toJson(flow));
//        } catch (IOException | JsonException e) {
//            throw new JobStoreException(String.format("Exception caught when trying to write Flow: %d", flow.getId()), e);
//        }
//    }
//
//    private void storeSinkInJob(Path jobPath, Sink sink) throws JobStoreException {
//        final Path flowPath = Paths.get(jobPath.toString(), SINK_FILE);
//        LOGGER.info("Creating Sink json-file: {}", flowPath);
//        try (BufferedWriter bw = Files.newBufferedWriter(flowPath, LOCAL_CHARSET)) {
//          bw.write(JsonUtil.toJson(sink));
//        } catch (IOException | JsonException e) {
//            throw new JobStoreException(String.format("Exception caught when trying to write Sink: %d", sink.getId()), e);
//        }
//    }
//
//    private void storeJobSpecificationInJob(Path jobPath, JobSpecification jobSpec) throws JobStoreException {
//        final Path jobSpecPath = Paths.get(jobPath.toString(), JOBSPECIFICATION_FILE);
//        LOGGER.info("Creating JobSpecification json-file: {}", jobSpecPath);
//        try (BufferedWriter bw = Files.newBufferedWriter(jobSpecPath, LOCAL_CHARSET)) {
//          bw.write(JsonUtil.toJson(jobSpec));
//        } catch (IOException | JsonException e) {
//            throw new JobStoreException(String.format("Exception caught when trying to write JobSpecification: %s", jobSpecPath.toString()), e);
//        }
//    }

    @SuppressWarnings("PMD.UnusedPrivateMethod")
    private <T> void storeJsonObjectAsFileInJob(Path jobPath, String filename, T jsonObject, long objectId) throws JobStoreException {
        final Path path = Paths.get(jobPath.toString(), filename);
        LOGGER.info("Creating json-file: {}", path);
        try (BufferedWriter bw = Files.newBufferedWriter(path, LOCAL_CHARSET)) {
            bw.write(JsonUtil.toJson(jsonObject));
        } catch (IOException | JsonException e) {
            throw new JobStoreException(String.format("Exception caught when trying to write %s with id: %d", filename, objectId), e);
        }
    }

    private void storeJobInfoInJob(Path jobPath, JobInfo jobInfo) throws JobStoreException {
        final Path jobInfoPath = getJobInfoPath(jobPath);
        LOGGER.info("Creating JobInfo json-file: {}", jobInfoPath);
        try (BufferedWriter bw = Files.newBufferedWriter(jobInfoPath, LOCAL_CHARSET)) {
            bw.write(JsonUtil.toJson(jobInfo));
        } catch (IOException | JsonException e) {
            throw new JobStoreException(String.format("Exception caught when trying to write JobInfo: %s", jobInfoPath.toString()), e);
        }
    }

    private JobInfo readJobInfoFromJob(Path jobPath) throws JobStoreException {
        final Path jobInfoPath = getJobInfoPath(jobPath);
        JobInfo jobInfo;
        try (BufferedReader br = Files.newBufferedReader(jobInfoPath, LOCAL_CHARSET)) {
            final StringBuilder sb = new StringBuilder();
            String data;
            while ((data = br.readLine()) != null) {
                sb.append(data);
            }
            jobInfo = JsonUtil.fromJson(sb.toString(), JobInfo.class);
        } catch (IOException | JsonException e) {
            LOGGER.error(e.getMessage());
            throw new JobStoreException(String.format("Exception caught while reading JobInfo from Path: %s", jobInfoPath.toString()));
        }
        return jobInfo;
    }

    private Path getJobInfoPath(Path jobPath) {
        return Paths.get(jobPath.toString(), JOBINFO_FILE);
    }

    private Path getJobStatePath(long jobId) {
        return Paths.get(getJobPath(jobId).toString(), JOBSTATE_FILE);
    }

    private Path getJobSpecificationPath(long jobId) {
        return Paths.get(getJobPath(jobId).toString(), JOBSPECIFICATION_FILE);
    }

    private Path getSinkPath(long jobId) {
        return Paths.get(getJobPath(jobId).toString(), SINK_FILE);
    }

    private Path getFlowPath(long jobId) {
        return Paths.get(getJobPath(jobId).toString(), FLOW_FILE);
    }

    private static List<Path> getDirectories(final Path dir) throws JobStoreException {
        List<Path> directories = new ArrayList<>();
        try (DirectoryStream<Path> directoryStream = Files.newDirectoryStream(dir)) {
            for (Path path : directoryStream) {
                if (Files.isDirectory(path)) {
                    directories.add(path);
                }
            }
        } catch (IOException e) {
            throw new JobStoreException("Exception caught while reading job-directories in jobStore", e);
        }
        Collections.sort(directories);
        return directories;
    }

    private Path getJobPath(long jobId) {
        return Paths.get(storePath.toString(), Long.toString(jobId));
    }

    private Path getChunksPath(long jobId) {
        return Paths.get(storePath.toString(), Long.toString(jobId), "chunks");
    }

    public static FileSystemJobStore newFileSystemJobStore(Path storePath) throws JobStoreException {
        return new FileSystemJobStore(storePath);
    }

    private void generateChunkKeys(Chunk chunk, SequenceAnalyserKeyGenerator sequenceAnalyserKeyGenerator, Sink sink) {
        LOGGER.info("Generating chunk keys for chunk.id {} in job.id {}", chunk.getChunkId(), chunk.getJobId());
        for (final String key : sequenceAnalyserKeyGenerator.generateKeys(chunk, sink)) {
            chunk.addKey(key);
        }
    }

    private void addChunk(Chunk chunk, SequenceAnalyserKeyGenerator sequenceAnalyserKeyGenerator, Sink sink) throws JobStoreException {
        LOGGER.info("Adding chunk.id {} for job.id {}", chunk.getChunkId(), chunk.getJobId());
        generateChunkKeys(chunk, sequenceAnalyserKeyGenerator, sink);
        chunkFileHandler.addResult(chunk);
    }

    @Override
    public Chunk getChunk(long jobId, long chunkId) throws JobStoreException {
        return chunkFileHandler.getResult(jobId, chunkId);
    }

    @Override
    public void addProcessorResult(ChunkResult processorResult) throws JobStoreException {
        processorResultFileHandler.addResult(processorResult);
        updateJobState(processorResult.getJobId());
    }

    @Override
    public ChunkResult getProcessorResult(long jobId, long chunkId) throws JobStoreException {
        return processorResultFileHandler.getResult(jobId, chunkId);
    }

    @Override
    public void addSinkResult(SinkChunkResult sinkResult) throws JobStoreException {
        sinkResultFileHandler.addResult(sinkResult);
        long jobId = sinkResult.getJobId();
        updateJobState(jobId);
        if(getJobState(jobId).checkIfAllIsDone()) {
            addChunkCountersToJobInfo(jobId);
        }
    }

    private void addChunkCountersToJobInfo(long jobId) throws JobStoreException {
        // Retrieve job info
        JobInfo jobInfo = readJobInfoFromJob(getJobPath(jobId));

        // Add chunk counters for the three FileHandlers.
        jobInfo.setChunkifyingChunkCounter(chunkFileHandler.getChunkCounter(jobId));
        jobInfo.setProcessingChunkCounter(processorResultFileHandler.getChunkCounter(jobId));
        jobInfo.setDeliveringChunkCounter(sinkResultFileHandler.getChunkCounter(jobId));

        // Save jobInfo to filesystem
        storeJobInfoInJob(getJobPath(jobId), jobInfo);
    }

    @Override
    public SinkChunkResult getSinkResult(long jobId, long chunkId) throws JobStoreException {
        return sinkResultFileHandler.getResult(jobId, chunkId);
    }

    @Override
    public long getNumberOfChunksInJob(long jobId) throws JobStoreException {
        return chunkFileHandler.getNumberOfChunksInJob(jobId);
    }

    @Override
    public JobCompletionState getJobCompletionState(long jobId) throws JobStoreException {
        JobCompletionStateFinder jobCompletionStateFinder = new JobCompletionStateFinder(this);
        return jobCompletionStateFinder.getJobCompletionState(jobId);
    }

    @Override
    public Flow getFlow(long jobId) throws JobStoreException {
        final Path flowPath = getFlowPath(jobId);
        if (Files.exists(flowPath)) {
            return JsonFileUtil.getJsonFileUtil(LOCAL_CHARSET).readObjectFromFile(flowPath, Flow.class);
        } else {
            return null;
        }
    }

    private synchronized void updateJobState(long jobId) throws JobStoreException {
        final long chunkCount = getNumberOfChunksInJob(jobId);
        final long processorCount = processorResultFileHandler.getNumberOfChunksInJob(jobId);
        final long sinkCount = sinkResultFileHandler.getNumberOfChunksInJob(jobId);
        JobState jobState = null;
        if (processorCount == chunkCount) {
            jobState = getJobState(jobId);
            jobState.setLifeCycleStateFor(JobState.OperationalState.PROCESSING, JobState.LifeCycleState.DONE);
        } else if (processorCount == 1) {
            jobState = getJobState(jobId);
            jobState.setLifeCycleStateFor(JobState.OperationalState.PROCESSING, JobState.LifeCycleState.ACTIVE);
        }
        if (sinkCount == chunkCount) {
            jobState = jobState != null ? jobState : getJobState(jobId);
            jobState.setLifeCycleStateFor(JobState.OperationalState.DELIVERING, JobState.LifeCycleState.DONE);
        } else if (sinkCount == 1) {
            jobState = jobState != null ? jobState : getJobState(jobId);
            jobState.setLifeCycleStateFor(JobState.OperationalState.DELIVERING, JobState.LifeCycleState.ACTIVE);
        }
        if (jobState != null) {
            LOGGER.debug("Updating job state for job {}", jobId);
            setJobState(jobId, jobState);
        }
    }

    private long applyRecordSplitter(Job job, DefaultXMLRecordSplitter recordSplitter, SequenceAnalyserKeyGenerator sequenceAnalyserKeyGenerator, Sink sink) throws IllegalDataException, JobStoreException {
        long jobId = job.getId();
        long chunkId = 1;
        long recordCount = 0;
        long counter = 0;
        SupplementaryProcessData supplementaryProcessData = createSupplementaryProcessData(job);
        Chunk chunk = new Chunk(jobId, chunkId, null, supplementaryProcessData);
        for (String record : recordSplitter) {
            recordCount++;
            LOGGER.trace("======> Before [" + record + "]");
            final String recordBase64 = base64encode(record);
            LOGGER.trace("======> After  [" + recordBase64 + "]");
            if (counter < Constants.CHUNK_RECORD_COUNT_UPPER_BOUND) {
                chunk.addItem(new ChunkItem(counter, recordBase64, ChunkItem.Status.SUCCESS));
            } else {
                counter = 0;
                addChunk(chunk, sequenceAnalyserKeyGenerator, sink);
                chunk = new Chunk(jobId, ++chunkId, null, supplementaryProcessData);
                chunk.addItem(new ChunkItem(counter, recordBase64, ChunkItem.Status.SUCCESS));
            }
            counter++;
        }
        if (counter != 0) {
            addChunk(chunk, sequenceAnalyserKeyGenerator, sink);
        }
        LOGGER.info("Created {} chunks for job {}", chunkId, job.getId());

        return recordCount;
    }

    private static DefaultXMLRecordSplitter newRecordSplitter(JobSpecification jobSpec, InputStream jobInputStream) throws IllegalStateException, IOException, XMLStreamException {
        final DefaultXMLRecordSplitter recordSplitter = new DefaultXMLRecordSplitter(jobInputStream);
        validateEncodings(jobSpec, recordSplitter);
        return recordSplitter;
    }

    private static void validateEncodings(JobSpecification jobSpec, DefaultXMLRecordSplitter recordSplitter) {
        final String expectedEncoding = normalizeEncoding(jobSpec.getCharset());
        final String actualEncoding = normalizeEncoding(recordSplitter.getEncoding());
        if (!actualEncoding.equals(expectedEncoding)) {
            throw new IllegalStateException(String.format("Actual encoding '%s' differs from expected '%s' encoding", actualEncoding, expectedEncoding));
        }
    }

    private static String normalizeEncoding(String encoding) {
        return encoding.replaceAll("-", "").toLowerCase();
    }

    private SupplementaryProcessData createSupplementaryProcessData(Job job) {
        return createSupplementaryProcessData(job.getJobInfo().getJobSpecification());
    }

    private SupplementaryProcessData createSupplementaryProcessData(JobSpecification jobSpecification) {
        return new SupplementaryProcessData(jobSpecification.getSubmitterId(), jobSpecification.getFormat());
    }
}
