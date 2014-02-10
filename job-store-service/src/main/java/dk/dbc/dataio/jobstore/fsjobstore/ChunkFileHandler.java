package dk.dbc.dataio.jobstore.fsjobstore;

import dk.dbc.dataio.commons.types.ChunkResult;
import dk.dbc.dataio.commons.utils.json.JsonException;
import dk.dbc.dataio.commons.utils.json.JsonUtil;
import dk.dbc.dataio.jobstore.types.JobStoreException;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ChunkFileHandler {
    private static final Logger LOGGER = LoggerFactory.getLogger(ChunkFileHandler.class);

    static final Charset LOCAL_CHARSET = Charset.forName("UTF-8");

    private final Path storePath;
    private final String counterFile;
    private final String resultFilenamePattern;

    public ChunkFileHandler(Path storePath, String counterFile, String resultFilenamePattern) {
        this.storePath = storePath;
        this.counterFile = counterFile;
        this.resultFilenamePattern = resultFilenamePattern;
    }

    public ChunkResult getProcessorResult(long jobId, long chunkId) throws JobStoreException {
        final Path processorResultPath = Paths.get(getJobPath(jobId).toString(), String.format(resultFilenamePattern, chunkId));
        return readObjectFromFile(processorResultPath, ChunkResult.class);
    }

    public void createProcessorCounterFile(long jobId) throws JobStoreException {
        final Path processorCounterFile = getProcessorCounterFile(jobId);
        if (Files.exists(processorCounterFile)) {
            throw new JobStoreException("Processor counter file already exists");
        }
        writeLongValueToCounterFile(processorCounterFile, 0L);
        LOGGER.info("Created processor counter file: {}", processorCounterFile);
    }

    public void addProcessorResult(ChunkResult processorResult) throws JobStoreException {
        final Path chunkPath = Paths.get(getJobPath(processorResult.getJobId()).toString(), String.format(resultFilenamePattern, processorResult.getChunkId()));
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
    }

    public long getNumberOfProcessedChunksInJob(long jobId) throws JobStoreException {
        return readLongValueFromCounterFile(getProcessorCounterFile(jobId));
    }

    private Path getProcessorCounterFile(long jobId) {
        return Paths.get(getJobPath(jobId).toString(), counterFile);
    }

    private synchronized void incrementProcessorCounter(long jobId) throws JobStoreException {
        final Path processorCounterFile = getProcessorCounterFile(jobId);
        if (!Files.exists(processorCounterFile)) {
            throw new JobStoreException(String.format("Processor counter file %s not found", processorCounterFile));
        }
        Long processorCounter = readLongValueFromCounterFile(processorCounterFile);
        writeLongValueToCounterFile(processorCounterFile, ++processorCounter);
    }

    // Following are currently duplicated in FSJobStore.java
    // Moved to more generel location
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

    private <T> T readObjectFromFile(Path objectPath, Class<T> tClass) throws JobStoreException {
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

    private void writeLongValueToCounterFile(Path counterFile, Long counter) throws JobStoreException {
        try (BufferedWriter bw = Files.newBufferedWriter(counterFile, LOCAL_CHARSET)) {
            bw.write(counter.toString());
        } catch (IOException e) {
            throw new JobStoreException(String.format("Exception caught when trying to write to counter file %s", counterFile), e);
        }
    }

    private Path getJobPath(long jobId) {
        return Paths.get(storePath.toString(), Long.toString(jobId));
    }
}
