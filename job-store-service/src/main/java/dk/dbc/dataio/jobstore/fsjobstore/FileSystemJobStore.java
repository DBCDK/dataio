package dk.dbc.dataio.jobstore.fsjobstore;

import dk.dbc.dataio.commons.types.Chunk;
import dk.dbc.dataio.commons.types.ChunkResult;
import dk.dbc.dataio.commons.types.Flow;
import dk.dbc.dataio.commons.types.FlowBinder;
import dk.dbc.dataio.commons.types.JobErrorCode;
import dk.dbc.dataio.commons.types.JobInfo;
import dk.dbc.dataio.commons.types.JobSpecification;
import dk.dbc.dataio.commons.types.Sink;
import dk.dbc.dataio.commons.utils.json.JsonException;
import dk.dbc.dataio.commons.utils.json.JsonUtil;
import dk.dbc.dataio.jobstore.JobStore;
import dk.dbc.dataio.jobstore.recordsplitter.DefaultXMLRecordSplitter;
import dk.dbc.dataio.jobstore.types.IllegalDataException;
import dk.dbc.dataio.jobstore.types.Job;
import dk.dbc.dataio.jobstore.types.JobState;
import dk.dbc.dataio.jobstore.types.JobStoreException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.xml.stream.XMLStreamException;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
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
    static final String PROCESSOR_COUNTER_FILE = "processor.cnt";
    static final String PROCESSOR_RESULT_FILENAME_PATTERN = "%d.res.json";
    static final Charset LOCAL_CHARSET = Charset.forName("UTF-8");

    private static final Logger LOGGER = LoggerFactory.getLogger(FileSystemJobStore.class);

    private final Path storePath;


    public FileSystemJobStore(Path storePath) throws JobStoreException {
        if (storePath == null) {
            throw new NullPointerException("storePath can not be null");
        }
        if (!canUseExistingStorePath(storePath)) {
            createDirectory(storePath);
        }
        this.storePath = storePath;

        LOGGER.info("Placing job store in {}", this.storePath);
    }

    @Override
    public Job createJob(JobSpecification jobSpec, FlowBinder flowBinder, Flow flow, Sink sink) throws JobStoreException {
        final long jobId = System.currentTimeMillis();
        final Date jobCreationTime = new Date();
        final Path jobPath = getJobPath(jobId);
        final Path dataObjectPath = Paths.get(jobSpec.getDataFile());
        long recordCount = 0;

        LOGGER.info("Creating job in {}", jobPath);
        createDirectory(getJobPath(jobId));
        createDirectory(getChunksPath(jobId));

        storeFlowBinderInJob(jobPath, flowBinder);
        storeFlowInJob(jobPath, flow);
        storeSinkInJob(jobPath, sink);
        storeJobSpecificationInJob(jobPath, jobSpec);
        createJobChunkCounterFile(jobId);
        createProcessorCounterFile(jobId);

        JobInfo jobInfo = new JobInfo(jobId, jobSpec, jobCreationTime, JobErrorCode.NO_ERROR, 0, null);

        Job job = new Job(jobInfo, new JobState(), flow);
        setJobState(jobId, job.getJobState());
        try {
            job.getJobState().setLifeCycleStateFor(JobState.OperationalState.CHUNKIFYING, JobState.LifeCycleState.ACTIVE);
            setJobState(jobId, job.getJobState());

            final DefaultXMLRecordSplitter recordSplitter;
            try {
                recordSplitter = newRecordSplitter(jobSpec, dataObjectPath);
                recordCount = applyDefaultXmlSplitter(job, recordSplitter);
            } catch (IOException e) {
                jobInfo = new JobInfo(jobId, jobSpec, jobCreationTime, JobErrorCode.DATA_FILE_NOT_FOUND, 0, null);
                job = new Job(jobInfo, job.getJobState(), flow);
                return job;
            } catch (IllegalStateException e) {
                jobInfo = new JobInfo(jobId, jobSpec, jobCreationTime, JobErrorCode.DATA_FILE_ENCODING_MISMATCH, 0, null);
                job = new Job(jobInfo, job.getJobState(), flow);
                return job;
            } catch (XMLStreamException | IllegalDataException e) {
                jobInfo = new JobInfo(jobId, jobSpec, jobCreationTime, JobErrorCode.DATA_FILE_INVALID, 0, null);
                job = new Job(jobInfo, job.getJobState(), flow);
                return job;
            }

            jobInfo = new JobInfo(jobId, jobSpec, jobCreationTime, JobErrorCode.NO_ERROR, recordCount, null);
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
        writeObjectToFile(getJobStatePath(jobId), jobState);
    }

    @Override
    public synchronized JobState getJobState(long jobId) throws JobStoreException {
        final Path jobStatePath = getJobStatePath(jobId);
        if (Files.exists(jobStatePath)) {
            return readObjectFromFile(jobStatePath, JobState.class);
        } else {
            return null;
        }
    }

    private static <T> T readObjectFromFile(Path objectPath, Class<T> tClass) throws JobStoreException {
        T object;
        try (BufferedReader br = Files.newBufferedReader(objectPath, LOCAL_CHARSET)) {
            final StringBuilder sb = new StringBuilder();
            String data;
            while ((data = br.readLine()) != null) {
                sb.append(data);
            }
            object = JsonUtil.fromJson(sb.toString(), tClass);
        } catch (IOException | JsonException e) {
            final String errorMsg = String.format("Exception caught while reading object from Path: %s", objectPath.toString());
            LOGGER.error(errorMsg, e);
            throw new JobStoreException(errorMsg, e);
        }
        return object;
    }

    private static <T> void writeObjectToFile(Path objectPath, T object) throws JobStoreException {
        LOGGER.info("Writing JSON file: {}", objectPath);
        try (BufferedWriter bw = Files.newBufferedWriter(objectPath, LOCAL_CHARSET)) {
            bw.write(JsonUtil.toJson(object));
        } catch (IOException | JsonException e) {
            final String errorMsg = String.format("Exception caught when trying to write object to Path: %s", objectPath.toString());
            LOGGER.error(errorMsg, e);
            throw new JobStoreException(errorMsg, e);
        }
    }

    @Override
    public List<JobInfo> getAllJobInfos() throws JobStoreException {
        final List<Path> jobPaths = getDirectories(storePath);
        List<JobInfo> jobInfos = new ArrayList<>(jobPaths.size());
        for(Path jobPath : jobPaths) {
            jobInfos.add(readJobInfoFromJob(jobPath));
        }
        return jobInfos;
    }

    private void storeFlowBinderInJob(Path jobPath, FlowBinder flowBinder) throws JobStoreException {
        final Path flowPath = Paths.get(jobPath.toString(), FLOWBINDER_FILE);
        LOGGER.info("Creating FlowBinder json-file: {}", flowPath);
        try (BufferedWriter bw = Files.newBufferedWriter(flowPath, LOCAL_CHARSET)) {
          bw.write(JsonUtil.toJson(flowBinder));
        } catch (IOException | JsonException e) {
            throw new JobStoreException(String.format("Exception caught when trying to write FlowBinder: %d", flowBinder.getId()), e);
        }
    }

    private void storeFlowInJob(Path jobPath, Flow flow) throws JobStoreException {
        final Path flowPath = Paths.get(jobPath.toString(), FLOW_FILE);
        LOGGER.info("Creating Flow json-file: {}", flowPath);
        try (BufferedWriter bw = Files.newBufferedWriter(flowPath, LOCAL_CHARSET)) {
          bw.write(JsonUtil.toJson(flow));
        } catch (IOException | JsonException e) {
            throw new JobStoreException(String.format("Exception caught when trying to write Flow: %d", flow.getId()), e);
        }
    }

    private void storeSinkInJob(Path jobPath, Sink sink) throws JobStoreException {
        final Path flowPath = Paths.get(jobPath.toString(), SINK_FILE);
        LOGGER.info("Creating Sink json-file: {}", flowPath);
        try (BufferedWriter bw = Files.newBufferedWriter(flowPath, LOCAL_CHARSET)) {
          bw.write(JsonUtil.toJson(sink));
        } catch (IOException | JsonException e) {
            throw new JobStoreException(String.format("Exception caught when trying to write Sink: %d", sink.getId()), e);
        }
    }

    private void storeJobSpecificationInJob(Path jobPath, JobSpecification jobSpec) throws JobStoreException {
        final Path jobSpecPath = Paths.get(jobPath.toString(), JOBSPECIFICATION_FILE);
        LOGGER.info("Creating JobSpecification json-file: {}", jobSpecPath);
        try (BufferedWriter bw = Files.newBufferedWriter(jobSpecPath, LOCAL_CHARSET)) {
          bw.write(JsonUtil.toJson(jobSpec));
        } catch (IOException | JsonException e) {
            throw new JobStoreException(String.format("Exception caught when trying to write JobSpecification: %s", jobSpecPath.toString()), e);
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
        try(BufferedReader br = Files.newBufferedReader(jobInfoPath, LOCAL_CHARSET)) {
            final StringBuilder sb = new StringBuilder();
            String data;
            while ((data = br.readLine()) != null) {
                sb.append(data);
            }
            jobInfo = JsonUtil.fromJson(sb.toString(), JobInfo.class);
        } catch(IOException | JsonException e) {
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

    private static List<Path> getDirectories(final Path dir) throws JobStoreException {
        List<Path> directories = new ArrayList<>();
        try (DirectoryStream<Path> directoryStream = Files.newDirectoryStream(dir)) {
            for (Path path : directoryStream) {
                if(Files.isDirectory(path)) {
                    directories.add(path);
                }
            }
        } catch (IOException e) {
                throw new JobStoreException("Exception caught while reading job-directories in jobStore", e);
        }
        Collections.sort(directories);
        return directories;
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

    private Path getJobPath(long jobId) {
        return Paths.get(storePath.toString(), Long.toString(jobId));
    }

    private Path getChunksPath(long jobId) {
        return Paths.get(storePath.toString(), Long.toString(jobId), "chunks");
    }

    public static FileSystemJobStore newFileSystemJobStore(Path storePath) throws JobStoreException {
        return new FileSystemJobStore(storePath);
    }

    public void addChunk(Job job, Chunk chunk) throws JobStoreException {
        final Path chunkPath = Paths.get(getJobPath(job.getId()).toString(), String.format("%d.json", chunk.getId()));
        LOGGER.info("Creating chunk json-file: {}", chunkPath);
        try (BufferedWriter bw = Files.newBufferedWriter(chunkPath, LOCAL_CHARSET)) {
            bw.write(JsonUtil.toJson(chunk));
        } catch (IOException | JsonException e) {
            throw new JobStoreException(String.format("Exception caught when trying to write chunk: %d", chunk.getId()), e);
        }
        incrementJobChunkCounter(job);
    }

    @Override
    public Chunk getChunk(long jobId, long chunkId) throws JobStoreException {
        final Path chunkPath = Paths.get(getJobPath(jobId).toString(), String.format("%d.json", chunkId));
        if (!Files.exists(chunkPath)) {
            return null;
        }
        Chunk chunk;
        try (BufferedReader br = Files.newBufferedReader(chunkPath, LOCAL_CHARSET)) {
            final StringBuilder sb = new StringBuilder();
            String data;
            while ((data = br.readLine()) != null) {
                sb.append(data);
            }
            chunk = JsonUtil.fromJson(sb.toString(), Chunk.class);
        } catch (IOException | JsonException e) {
            throw new JobStoreException(String.format("Exception caught when trying to read chunk: %d", chunkId), e);
        }
        return chunk;
    }

    @Override
    public ChunkResult getProcessorResult(long jobId, long chunkId) throws JobStoreException {
        final Path processorResultPath = Paths.get(getJobPath(jobId).toString(), String.format(PROCESSOR_RESULT_FILENAME_PATTERN, chunkId));
        return readObjectFromFile(processorResultPath, ChunkResult.class);
    }

    @Override
    public void addProcessorResult(ChunkResult processorResult) throws JobStoreException {
        final Path chunkPath = Paths.get(getJobPath(processorResult.getJobId()).toString(), String.format(PROCESSOR_RESULT_FILENAME_PATTERN, processorResult.getChunkId()));
        LOGGER.info("Creating processor result json-file: {}", chunkPath);
        try (BufferedWriter bw = Files.newBufferedWriter(chunkPath, LOCAL_CHARSET)) {
            bw.write(JsonUtil.toJson(processorResult));
        } catch (IOException | JsonException e) {
            final String errorMsg = String.format("Exception caught when trying to write processor result: [%s, %s]",
                    processorResult.getJobId(), processorResult.getChunkId());
            LOGGER.error(errorMsg, e);
            throw new JobStoreException(errorMsg, e);
        }
        incrementProcessorCounter(processorResult.getJobId());
        updateJobState(processorResult.getJobId());
    }

    private synchronized void updateJobState(long jobId) throws JobStoreException {
        final long chunkCount = getNumberOfChunksInJob(jobId);
        final long processorCount = getNumberOfProcessedChunksInJob(jobId);
        JobState jobState = null;
        if (processorCount == chunkCount) {
            jobState = getJobState(jobId);
            jobState.setLifeCycleStateFor(JobState.OperationalState.PROCESSING, JobState.LifeCycleState.DONE);
        } else if (processorCount == 1) {
            jobState = getJobState(jobId);
            jobState.setLifeCycleStateFor(JobState.OperationalState.PROCESSING, JobState.LifeCycleState.ACTIVE);
        }
        if (jobState != null) {
            LOGGER.debug("Updating job state for job {}", jobId);
            setJobState(jobId, jobState);
        }
    }

    @Override
    public long getNumberOfChunksInJob(long jobId) throws JobStoreException {
        return readLongValueFromCounterFile(getChunkCounterFile(jobId));
    }

    private Path getChunkCounterFile(long jobId) {
        final Path jobPath = getJobPath(jobId);
        return Paths.get(jobPath.toString(), CHUNK_COUNTER_FILE);
    }

    private void createJobChunkCounterFile(long jobId) throws JobStoreException {
        final Path chunkCounterFile = getChunkCounterFile(jobId);
        if (Files.exists(chunkCounterFile)) {
            throw new JobStoreException("Chunk counter file already exists");
        }
        writeLongValueToCounterFile(chunkCounterFile, 0L);
        LOGGER.info("Created chunk counter file: {}", chunkCounterFile);
    }

    private void incrementJobChunkCounter(Job job) throws JobStoreException {
        final Path chunkCounterFile = getChunkCounterFile(job.getId());
        if (!Files.exists(chunkCounterFile)) {
            throw new JobStoreException("Chunk counter file not found");
        }
        Long chunkCounter = readLongValueFromCounterFile(chunkCounterFile);
        chunkCounter++;
        writeLongValueToCounterFile(chunkCounterFile, chunkCounter);
    }

    private long getNumberOfProcessedChunksInJob(long jobId) throws JobStoreException {
        return readLongValueFromCounterFile(getProcessorCounterFile(jobId));
    }

    private Path getProcessorCounterFile(long jobId) {
        final Path jobPath = getJobPath(jobId);
        return Paths.get(jobPath.toString(), PROCESSOR_COUNTER_FILE);
    }

    private void createProcessorCounterFile(long jobId) throws JobStoreException {
        final Path processorCounterFile = getProcessorCounterFile(jobId);
        if (Files.exists(processorCounterFile)) {
            throw new JobStoreException("Processor counter file already exists");
        }
        writeLongValueToCounterFile(processorCounterFile, 0L);
        LOGGER.info("Created processor counter file: {}", processorCounterFile);
    }

    private synchronized void incrementProcessorCounter(long jobId) throws JobStoreException {
        final Path processorCounterFile = getProcessorCounterFile(jobId);
        if (!Files.exists(processorCounterFile)) {
            throw new JobStoreException(String.format("Processor counter file %s not found", processorCounterFile));
        }
        Long processorCounter = readLongValueFromCounterFile(processorCounterFile);
        writeLongValueToCounterFile(processorCounterFile, ++processorCounter);
    }

    private Long readLongValueFromCounterFile(Path counterFile) throws JobStoreException {
        Long counter;
        try (BufferedReader br = Files.newBufferedReader(counterFile, LOCAL_CHARSET)) {
            String value = br.readLine();
            if (value != null) {
                value = value.trim();
            } else {
                throw new NullPointerException(String.format("Value from counter file %s is null", counterFile));
            }
            counter = Long.valueOf(value);
        } catch (IOException e) {
            throw new JobStoreException(String.format("Exception caught when trying to read from counter file %s", counterFile), e);
        }
        return counter;
    }

    private void writeLongValueToCounterFile(Path counterFile, Long counter) throws JobStoreException {
        try (BufferedWriter bw = Files.newBufferedWriter(counterFile, LOCAL_CHARSET)) {
            bw.write(counter.toString());
        } catch (IOException e) {
            throw new JobStoreException(String.format("Exception caught when trying to write to counter file %s", counterFile), e);
        }
    }

    private long applyDefaultXmlSplitter(Job job, DefaultXMLRecordSplitter recordSplitter) throws IllegalDataException, JobStoreException {
        long chunkId = 1;
        long recordCount = 0;
        int counter = 0;
        Chunk chunk = new Chunk(chunkId, job.getFlow());
        for (String record : recordSplitter) {
            recordCount++;
            LOGGER.trace("======> Before [" + record + "]");
            final String recordBase64 = base64encode(record);
            LOGGER.trace("======> After  [" + recordBase64 + "]");
            if (counter++ < Chunk.MAX_RECORDS_PER_CHUNK) {
                chunk.addRecord(recordBase64);
            } else {
                addChunk(job, chunk);
                chunk = new Chunk(++chunkId, job.getFlow());
                chunk.addRecord(recordBase64);
                counter = 1;
            }
        }
        if (counter != 0) {
            addChunk(job, chunk);
        }
        LOGGER.info("Created {} chunks for job {}", chunkId, job.getId());

        return recordCount;
    }

    private static DefaultXMLRecordSplitter newRecordSplitter(JobSpecification jobSpec, Path dataPath) throws IllegalStateException, IOException, XMLStreamException {
        final DefaultXMLRecordSplitter recordSplitter = new DefaultXMLRecordSplitter(Files.newInputStream(dataPath));
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
}
