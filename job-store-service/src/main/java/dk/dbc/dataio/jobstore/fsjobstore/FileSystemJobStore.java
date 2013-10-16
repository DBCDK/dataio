package dk.dbc.dataio.jobstore.fsjobstore;

import dk.dbc.dataio.commons.types.Flow;
import dk.dbc.dataio.commons.types.JobSpecification;
import dk.dbc.dataio.commons.utils.json.JsonException;
import dk.dbc.dataio.commons.utils.json.JsonUtil;
import dk.dbc.dataio.jobstore.JobStore;
import dk.dbc.dataio.jobstore.recordsplitter.DefaultXMLRecordSplitter;
import dk.dbc.dataio.jobstore.types.Chunk;
import dk.dbc.dataio.jobstore.types.Job;
import dk.dbc.dataio.jobstore.types.JobStoreException;
import dk.dbc.dataio.jobstore.types.ProcessChunkResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.xml.stream.XMLStreamException;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static dk.dbc.dataio.jobstore.util.Base64Util.base64encode;

public class FileSystemJobStore implements JobStore {
    static final String FLOW_FILE = "flow.json";
    static final String JOBSPECIFICATION_FILE = "jobspec.json";
    static final String CHUNK_COUNTER_FILE = "chunk.cnt";
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
    public Job createJob(JobSpecification jobSpec, Flow flow) throws JobStoreException {
        Path dataObjectPath = Paths.get(jobSpec.getDataFile());
        final long jobId = System.currentTimeMillis();
        final Path jobPath = getJobPath(jobId);

        LOGGER.info("Creating job in {}", jobPath);
        createDirectory(getJobPath(jobId));
        createDirectory(getChunksPath(jobId));

        storeFlowInJob(jobPath, flow);
        storeJobSpecificationInJob(jobPath, jobSpec);
        createJobChunkCounterFile(jobId);

        return chunkify(new Job(jobId, dataObjectPath, flow));
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

    private void storeJobSpecificationInJob(Path jobPath, JobSpecification jobSpec) throws JobStoreException {
        final Path jobSpecPath = Paths.get(jobPath.toString(), JOBSPECIFICATION_FILE);
        LOGGER.info("Creating Flow json-file: {}", jobSpecPath);
        try (BufferedWriter bw = Files.newBufferedWriter(jobSpecPath, LOCAL_CHARSET)) {
          bw.write(JsonUtil.toJson(jobSpec));
        } catch (IOException | JsonException e) {
            throw new JobStoreException(String.format("Exception caught when trying to write JobSpecification: %s", jobSpecPath.toString()), e);
        }
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
    public Chunk getChunk(Job job, long chunkId) throws JobStoreException {
        final Path chunkPath = Paths.get(getJobPath(job.getId()).toString(), String.format("%d.json", chunkId));
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
    public ProcessChunkResult getProcessChunkResult(Job job, long chunkResultId) throws JobStoreException {
        final Path chunkResultPath = Paths.get(getJobPath(job.getId()).toString(), String.format("%d.res.json", chunkResultId));
        ProcessChunkResult chunkResult;
        try (BufferedReader br = Files.newBufferedReader(chunkResultPath, LOCAL_CHARSET)) {
            final StringBuilder sb = new StringBuilder();
            String data;
            while ((data = br.readLine()) != null) {
                sb.append(data);
            }
            chunkResult = JsonUtil.fromJson(sb.toString(), ProcessChunkResult.class);
        } catch (IOException | JsonException e) {
            throw new JobStoreException(String.format("Exception caught when trying to read chunk result: %d", chunkResultId), e);
        }
        return chunkResult;
    }

    @Override
    public void addChunkResult(Job job, ProcessChunkResult processChunkResult) throws JobStoreException {
        final Path chunkPath = Paths.get(getJobPath(job.getId()).toString(), String.format("%d.res.json", processChunkResult.getId()));
        LOGGER.info("Creating chunk result json-file: {}", chunkPath);
        try (BufferedWriter bw = Files.newBufferedWriter(chunkPath, LOCAL_CHARSET)) {
            bw.write(JsonUtil.toJson(processChunkResult));
        } catch (IOException | JsonException e) {
            throw new JobStoreException(String.format("Exception caught when trying to write chunk result: {}", processChunkResult.getId()), e);
        }
    }

    @Override
    public long getNumberOfChunksInJob(Job job) throws JobStoreException {
        final Path chunkCounterFile = getChunkCounterFile(job.getId());
        return readLongValueFromChunkCounterFile(chunkCounterFile);
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
        writeLongValueToChunkCounterFile(chunkCounterFile, 0L);
        LOGGER.info("Created chunk counter file: {}", chunkCounterFile);
    }

    private void incrementJobChunkCounter(Job job) throws JobStoreException {
        final Path chunkCounterFile = getChunkCounterFile(job.getId());
        if (!Files.exists(chunkCounterFile)) {
            throw new JobStoreException("Chunk counter file not found");
        }
        Long chunkCounter = readLongValueFromChunkCounterFile(chunkCounterFile);
        chunkCounter++;
        writeLongValueToChunkCounterFile(chunkCounterFile, chunkCounter);
    }

    private Long readLongValueFromChunkCounterFile(Path chunkCounterFile) throws JobStoreException {
        Long chunkCounter;
        try (BufferedReader br = Files.newBufferedReader(chunkCounterFile, LOCAL_CHARSET)) {
            String value = br.readLine();
            if (value == null) {
                throw new NullPointerException("Value from chunk counter file is null");
            }
            value = value.trim();
            LOGGER.info("Reading count value :  [{}]", value);
            chunkCounter = Long.valueOf(value);
        } catch (IOException e) {
            throw new JobStoreException("Exception caught when trying to read from chunk counter file", e);
        }
        return chunkCounter;
    }

    private void writeLongValueToChunkCounterFile(Path chunkCounterFile, Long chunkCounter) throws JobStoreException {
        try (BufferedWriter bw = Files.newBufferedWriter(chunkCounterFile, LOCAL_CHARSET)) {
            bw.write(chunkCounter.toString());
        } catch (IOException e) {
            throw new JobStoreException("Exception caught when trying to write to chunk counter file", e);
        }
    }

    private Job chunkify(Job job) throws JobStoreException {
        try {
            final long chunks = applyDefaultXmlSplitter(job.getOriginalDataPath(), job);
            LOGGER.info("Created {} chunks for job {}", chunks, job.getId());
        } catch (XMLStreamException | IOException e) {
            throw new JobStoreException(String.format("Exception caught during chunk creation for job %d", job.getId()), e);
        }
        return job;
    }

    private long applyDefaultXmlSplitter(Path path, Job job) throws IOException, XMLStreamException, JobStoreException {
        LOGGER.trace("Got path: " + path.toString());
        final DefaultXMLRecordSplitter recordSplitter = new DefaultXMLRecordSplitter(Files.newInputStream(path));

        long chunkId = 1;
        int counter = 0;
        Chunk chunk = new Chunk(chunkId, job.getFlow());
        for (String record : recordSplitter) {
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
        return chunkId;
    }
}
