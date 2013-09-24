package dk.dbc.dataio.engine;

import dk.dbc.dataio.commons.types.Flow;
import dk.dbc.dataio.commons.types.json.mixins.MixIns;
import dk.dbc.dataio.commons.utils.json.JsonException;
import dk.dbc.dataio.commons.utils.json.JsonUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

public class FileSystemJobStore implements JobStore {
    private static final Logger log = LoggerFactory.getLogger(FileSystemJobStore.class);
    private static Map<Class<?>, Class<?>> mixIns = new HashMap<>(MixIns.getMixIns());
    static {
        mixIns.put(Chunk.class, ChunkJsonMixIn.class);
        mixIns.put(Job.class, JobJsonMixIn.class);
        mixIns.put(ProcessChunkResult.class, ProcessChunkResultJsonMixIn.class);
    }

    private final Path storePath;
    
    private final Charset LOCAL_CHARSET = Charset.forName("UTF-8");

    public FileSystemJobStore(Path storePath) throws JobStoreException {
        if (storePath == null) {
            throw new NullPointerException("storePath can not be null");
        }
        if (!canUseExistingStorePath(storePath)) {
            createDirectory(storePath);
        }
        this.storePath = storePath;

        log.info("Placing job store in {}", this.storePath);
    }

    @Override
    public Job createJob(Path dataObjectPath, Flow flow) throws JobStoreException {
        final long jobId = System.currentTimeMillis();
        final Path jobPath = getJobPath(jobId);

        log.info("Creating job in {}", jobPath);
        createDirectory(FileSystems.getDefault().getPath(storePath.toString(), Long.toString(jobId)));

        storeFlowInfoInJob(jobPath, flow);
        createJobChunkCounterFile(jobId);
        
        return new Job(jobId, dataObjectPath, flow);
    }

    private void storeFlowInfoInJob(Path jobPath, Flow flow) {
        final Path flowPath =  FileSystems.getDefault().getPath(jobPath.toString(), "flow.json");
        log.info("Creating Flow json-file: {}", flowPath);
        try(BufferedWriter bw = Files.newBufferedWriter(flowPath, LOCAL_CHARSET)) {
          bw.write(JsonUtil.toJson(flow));
        } catch(IOException ex) {
            log.warn("Exception caught when trying to write Flow: {}", flow.getId(), ex);
        } catch (JsonException ex) {
            log.warn("Exception caught when trying to write Flow: {}", flow.getId(), ex);
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
            Files.createDirectory(dir);
        } catch (IOException e) {
            throw new JobStoreException(String.format("Unable to create directory: %s", dir), e);
        }
    }

    private Path getJobPath(long jobId) {
        return FileSystems.getDefault().getPath(storePath.toString(), Long.toString(jobId));
    }

    public static FileSystemJobStore newFileSystemJobStore(Path storePath) throws JobStoreException {
        return new FileSystemJobStore(storePath);
    }

    @Override
    public void addChunk(Job job, Chunk chunk) throws JobStoreException {
        final Path chunkPath =  FileSystems.getDefault().getPath(getJobPath(job.getId()).toString(), String.format("%d.json", chunk.getId()));
        log.info("Creating chunk json-file: {}", chunkPath);
        try (BufferedWriter bw = Files.newBufferedWriter(chunkPath, LOCAL_CHARSET)) {
            bw.write(JsonUtil.toJson(chunk));
        } catch (IOException ex) {
            log.warn("Exception caught when trying to write chunk: {}", chunk.getId(), ex);
        } catch (JsonException e) {
            log.warn("Exception caught when trying to write chunk: {}", chunk.getId(), e);
        }

        incrementJobChunkCounter(job);
    }

    @Override
    public Chunk getChunk(Job job, long i) throws JobStoreException {
        final Path chunkPath =  FileSystems.getDefault().getPath(getJobPath(job.getId()).toString(), String.format("%d.json", i));
        Chunk chunk = null;
        try (BufferedReader br = Files.newBufferedReader(chunkPath, LOCAL_CHARSET)) {
            StringBuffer sb = new StringBuffer();
            String data = null;
            while((data = br.readLine())!=null) {
                sb.append(data);
            }
            log.info("Data: [{}]", sb.toString());
            chunk = JsonUtil.fromJson(sb.toString(), Chunk.class, mixIns);
        } catch (IOException ex) {
            String msg = "Could not read chunk file: " + i;
            log.error(msg, ex);
            throw new JobStoreException(msg, ex);
        } catch (JsonException e) {
            String msg = "Could not unmarshall chunk file: " + i;
            log.error(msg, e);
            throw new JobStoreException(msg, e);
        }
        return chunk;
    }

    @Override
    public ProcessChunkResult getProcessChunkResult(Job job, long i) throws JobStoreException {
        final Path chunkResultPath =  FileSystems.getDefault().getPath(getJobPath(job.getId()).toString(), String.format("%d.res.json", i));
        ProcessChunkResult chunkResult;
        try (BufferedReader br = Files.newBufferedReader(chunkResultPath, LOCAL_CHARSET)) {
            final StringBuilder sb = new StringBuilder();
            String data;
            while ((data = br.readLine()) != null) {
                sb.append(data);
            }
            log.info("Data: [{}]", sb.toString());
            chunkResult = JsonUtil.fromJson(sb.toString(), ProcessChunkResult.class, mixIns);
        } catch (IOException ex) {
            final String msg = "Could not read chunk file: " + i;
            log.error(msg, ex);
            throw new JobStoreException(msg, ex);
        } catch (JsonException e) {
            final String msg = "Could not unmarshall chunk file: " + i;
            log.error(msg, e);
            throw new JobStoreException(msg, e);
        }
        return chunkResult;
    }

    @Override
    public void addChunkResult(Job job, ProcessChunkResult processChunkResult) throws JobStoreException {
        final Path chunkPath =  FileSystems.getDefault().getPath(getJobPath(job.getId()).toString(), String.format("%d.res.json", processChunkResult.getId()));
        log.info("Creating chunk result json-file: {}", chunkPath);
        try (BufferedWriter bw = Files.newBufferedWriter(chunkPath, LOCAL_CHARSET)) {
            bw.write(JsonUtil.toJson(processChunkResult));
        } catch (IOException ex) {
            log.warn("Exception caught when trying to write chunk result: {}", processChunkResult.getId(), ex);
        } catch (JsonException e) {
            log.warn("Exception caught when trying to marshall chunk result: {}", processChunkResult.getId(), e);
        }
    }

    @Override
    public long getNumberOfChunksInJob(Job job) throws JobStoreException {
        File chunkCounterFile = getChunkCounterFile(job.getId());
        Long chunkCounterValue = readLongValueFromChunkCounterFile(chunkCounterFile);
        return chunkCounterValue.longValue();
    }

    private File getChunkCounterFile(long jobId) {
        Path jobPath = getJobPath(jobId);
        return new File(jobPath.toString()+File.separator+"chunk.cnt");
    }
    
    private void createJobChunkCounterFile(long jobId) throws JobStoreException {
        File chunkCounterFile = getChunkCounterFile(jobId);
        if(chunkCounterFile.exists()) {
            String msg = "chunkCounterFile already exists.";
            log.warn(msg);
            throw new JobStoreException(msg);
        }

        writeLongValueToChunkCounterFile(chunkCounterFile, Long.valueOf(0));
        log.info("Created chunk counter file: {}", chunkCounterFile);    
    }
    
    private void incrementJobChunkCounter(Job job) throws JobStoreException {
        File chunkCounterFile = getChunkCounterFile(job.getId());
        if(!chunkCounterFile.exists()) {
            String msg = "chunkCounterFile did not exists.";
            log.warn(msg);
            throw new JobStoreException(msg);
        }

        Long chunkCounter = readLongValueFromChunkCounterFile(chunkCounterFile);
        chunkCounter++;
        writeLongValueToChunkCounterFile(chunkCounterFile, chunkCounter);
    }

    private Long readLongValueFromChunkCounterFile(File chunkCounterFile) {
        Long chunkCounter = null;
        try(BufferedReader br = Files.newBufferedReader(chunkCounterFile.toPath(), LOCAL_CHARSET)) {
            String value = br.readLine();
            if(value == null) {
                throw new NullPointerException("Value from ChunkCounterFile is null!");
            }
            value = value.trim();
            log.info("Reading count value :  [{}]", value);
            chunkCounter = Long.valueOf(value);
        } catch (IOException ex) {
            log.warn("Could not read from chunkCounterFile", ex);
        }
        return chunkCounter;
    }

    private void writeLongValueToChunkCounterFile(File chunkCounterFile, Long chunkCounter) {
        try(BufferedWriter bw = Files.newBufferedWriter(chunkCounterFile.toPath(), LOCAL_CHARSET)) {
            bw.write(chunkCounter.toString());
        } catch (IOException ex) {
            log.warn("Could not write to chunkCounterFile", ex);
        }
    }
}