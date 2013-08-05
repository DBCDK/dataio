package dk.dbc.dataio.engine;

import java.io.BufferedReader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.logging.Level;

public class FileSystemJobStore implements JobStore {
    private static final Logger log = LoggerFactory.getLogger(FileSystemJobStore.class);

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
    public Job createJob(Path dataObjectPath, FlowInfo flowInfo) throws JobStoreException {
        final long jobId = System.currentTimeMillis();
        final Path jobPath = getJobPath(jobId);

        log.info("Creating job in {}", jobPath);
        createDirectory(FileSystems.getDefault().getPath(storePath.toString(), Long.toString(jobId)));

        storeFlowInfoInJob(jobPath, flowInfo);
        createJobChunkCounterFile(jobId);
        
        return new Job(jobId, dataObjectPath, flowInfo);
    }

    private void storeFlowInfoInJob(Path jobPath, FlowInfo flowInfo) {
        final Path flowInfoPath =  FileSystems.getDefault().getPath(jobPath.toString(), "flowinfo.json");
        log.info("Creating FlowInfo json-file: {}", flowInfoPath);
        try(BufferedWriter bw = Files.newBufferedWriter(flowInfoPath, LOCAL_CHARSET)) {
          bw.write(flowInfo.getData());
        } catch(IOException ex) {
            log.warn("Exception caught when trying to write FlowInfo: {}", flowInfo.getData(), ex);
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
            bw.write(chunk.toJson());
        } catch (IOException ex) {
            log.warn("Exception caught when trying to write chunk: {}", chunk.getId(), ex);
        }

        incrementJobChunkCounter(job);
    }

    @Override
    public int getNumberOfChunksInJob(Job job) throws JobStoreException {
        throw new UnsupportedOperationException("Not supported yet.");
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

        try(BufferedWriter bw = Files.newBufferedWriter(chunkCounterFile.toPath(), LOCAL_CHARSET)) {
            bw.write("0");
        } catch (IOException ex) {
            log.warn("Could not create chunkCounterFile: {}", chunkCounterFile.toString(), ex);
        }
        log.info("Created chunk counter file: {}", chunkCounterFile);    
    }
    
    private void incrementJobChunkCounter(Job job) throws JobStoreException {
        File chunkCounterFile = getChunkCounterFile(job.getId());
        if(!chunkCounterFile.exists()) {
            String msg = "chunkCounterFile did not exists.";
            log.warn(msg);
            throw new JobStoreException(msg);
        }

        Long chunkCounter = null;
        try(BufferedReader br = Files.newBufferedReader(chunkCounterFile.toPath(), LOCAL_CHARSET)) {
            String value = br.readLine().trim();
            log.info("Reading count value :  [{}]", value);
            chunkCounter = Long.valueOf(value);
        } catch (IOException ex) {
            log.warn("Could not read from chunkCounterFile", ex);
        }
        
        chunkCounter++;

        try(BufferedWriter bw = Files.newBufferedWriter(chunkCounterFile.toPath(), LOCAL_CHARSET)) {
            bw.write(chunkCounter.toString());
        } catch (IOException ex) {
            log.warn("Could not write to chunkCounterFile", ex);
        }
    }
}