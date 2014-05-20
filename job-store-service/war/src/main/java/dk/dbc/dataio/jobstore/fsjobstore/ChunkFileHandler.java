package dk.dbc.dataio.jobstore.fsjobstore;

import dk.dbc.dataio.commons.types.AbstractChunk;
import dk.dbc.dataio.commons.types.ChunkItem;
import dk.dbc.dataio.commons.utils.json.JsonException;
import dk.dbc.dataio.commons.utils.json.JsonUtil;
import dk.dbc.dataio.jobstore.types.ChunkCounter;
import dk.dbc.dataio.jobstore.types.JobStoreException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class ChunkFileHandler<X extends AbstractChunk> {

    private static final Logger LOGGER = LoggerFactory.getLogger(ChunkFileHandler.class);

    static final Charset LOCAL_CHARSET = Charset.forName("UTF-8");

    private final Path storePath;
    private final String counterFilename;
    private final String resultFilenamePattern;

    private final Class<X> clazz;

    public ChunkFileHandler(Class<X> clazz, Path storePath, String counterFilename, String resultFilenamePattern) {
        this.clazz = clazz;
        this.storePath = storePath;
        this.counterFilename = counterFilename;
        this.resultFilenamePattern = resultFilenamePattern;
    }

    public X getResult(long jobId, long chunkId) throws JobStoreException {
        final Path resultPath = Paths.get(getJobPath(jobId).toString(), String.format(resultFilenamePattern, chunkId));
        if (!Files.exists(resultPath)) {
            return null;
        }
        return JsonFileUtil.getJsonFileUtil(LOCAL_CHARSET).readObjectFromFile(resultPath, clazz);
    }

    public void addResult(X result) throws JobStoreException {
        final Path chunkPath = Paths.get(getJobPath(result.getJobId()).toString(), String.format(resultFilenamePattern, result.getChunkId()));
        LOGGER.info("Creating result json-file: {}", chunkPath);
        try (BufferedWriter bw = Files.newBufferedWriter(chunkPath, LOCAL_CHARSET)) {
            bw.write(JsonUtil.toJson(result));
        } catch (IOException | JsonException e) {
            final String errorMsg = String.format("Exception caught when trying to write result: [%s, %s]",
                    result.getJobId(), result.getChunkId());
            LOGGER.error(errorMsg, e);
            throw new JobStoreException(errorMsg, e);
        }
        incrementCounter(result.getJobId(), result);
    }

    public void createCounterFile(long jobId) throws JobStoreException {
        final Path counterFile = getCounterFile(jobId);
        if (Files.exists(counterFile)) {
            throw new JobStoreException("Counter file already exists");
        }
        JsonFileUtil.getJsonFileUtil(LOCAL_CHARSET).writeObjectToFile(counterFile, new ChunkCounter());
        LOGGER.info("Created counter file: {}", counterFile);
    }

    public synchronized long getNumberOfChunksInJob(long jobId) throws JobStoreException {
        final Path counterFile = getCounterFile(jobId);
        if (!Files.exists(counterFile)) {
            throw new JobStoreException(String.format("Counter file %s not found", counterFile));
        }
        return JsonFileUtil.getJsonFileUtil(LOCAL_CHARSET)
                .readObjectFromFile(counterFile, ChunkCounter.class)
                .getTotal();
    }

    private Path getCounterFile(long jobId) {
        return Paths.get(getJobPath(jobId).toString(), counterFilename);
    }

    private synchronized void incrementCounter(long jobId, X result) throws JobStoreException {
        final Path counterFile = getCounterFile(jobId);
        if (!Files.exists(counterFile)) {
            throw new JobStoreException(String.format("Counter file %s not found", counterFile));
        }
        final ChunkCounter chunkCounter = JsonFileUtil.getJsonFileUtil(LOCAL_CHARSET)
                .readObjectFromFile(counterFile, ChunkCounter.class);
        chunkCounter.incrementTotal();
        for (ChunkItem item : result.getItems()) {
            switch (item.getStatus()) {
                case FAILURE: chunkCounter.getItemResultCounter().incrementFailure();
                    break;
                case IGNORE: chunkCounter.getItemResultCounter().incrementIgnore();
                    break;
                case SUCCESS: chunkCounter.getItemResultCounter().incrementSuccess();
                    break;
                default:
                    throw new JobStoreException(String.format("Unhandled item status %s",
                            item.getStatus().name()));
            }
        }
        JsonFileUtil.getJsonFileUtil(LOCAL_CHARSET).writeObjectToFile(counterFile, chunkCounter);
    }

    private Path getJobPath(long jobId) {
        return Paths.get(storePath.toString(), Long.toString(jobId));
    }
}
