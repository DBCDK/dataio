package dk.dbc.dataio.jobstore.ejb;

import dk.dbc.dataio.commons.types.ChunkResult;
import dk.dbc.dataio.commons.types.JobInfo;
import dk.dbc.dataio.commons.types.Sink;
import dk.dbc.dataio.commons.utils.json.JsonException;
import dk.dbc.dataio.commons.utils.json.JsonUtil;
import dk.dbc.dataio.jobstore.processor.ChunkProcessor;
import dk.dbc.dataio.commons.types.Chunk;
import dk.dbc.dataio.jobstore.types.Job;
import dk.dbc.dataio.jobstore.types.JobState;
import dk.dbc.dataio.jobstore.types.JobStoreException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Resource;
import javax.ejb.ConcurrencyManagement;
import javax.ejb.ConcurrencyManagementType;
import javax.ejb.EJB;
import javax.ejb.EJBException;
import javax.ejb.Singleton;
import javax.jms.ConnectionFactory;
import javax.jms.JMSContext;
import javax.jms.JMSException;
import javax.jms.Queue;
import javax.jms.TextMessage;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;

import static dk.dbc.dataio.jobstore.util.Base64Util.base64decode;

@ConcurrencyManagement(ConcurrencyManagementType.BEAN)
@Singleton
public class JobHandlerBean {
    private static final Logger LOGGER = LoggerFactory.getLogger(JobHandlerBean.class);

    @Resource
    private ConnectionFactory sinkQueueConnectionFactory;

    @Resource(mappedName = "jms/dataio/sinks")
    private Queue sinkQueue;

    @EJB
    JobStoreBean jobStore;

    public JobInfo handleJob(Job job, Sink sink) throws JobStoreException {
        try {
            job.getJobState().setLifeCycleStateFor(JobState.OperationalState.PROCESSING, JobState.LifeCycleState.ACTIVE);
            //jobStore.updateJobState(job);
            processJob(job, sink);
            return job.getJobInfo();
        } finally {
            job.getJobState().setLifeCycleStateFor(JobState.OperationalState.PROCESSING, JobState.LifeCycleState.DONE);
            //jobStore.updateJobState(job);
        }
    }

    private void processJob(Job job, Sink sink) throws JobStoreException {
        final long numberOfChunks = jobStore.getNumberOfChunksInJob(job.getId());
        LOGGER.info("Processing {} chunks for job({})", numberOfChunks, job.getId());
        for (int chunkId = 1; chunkId <= numberOfChunks; chunkId++) {
            processChunk(job, chunkId, sink);
        }
    }

    private void processChunk(Job job, int chunkId, Sink sink) throws JobStoreException {
        final Chunk chunk = jobStore.getChunk(job.getId(), chunkId);
        final ChunkResult processedChunk = ChunkProcessor.processChunk(chunk);
        //jobStore.addProcessorResult(job, processedChunk);
        dispatchChunkResult(processedChunk, sink);
    }

    private void dispatchChunkResult(ChunkResult chunkResult, Sink sink) {
        try (JMSContext context = sinkQueueConnectionFactory.createContext()) {
            final TextMessage message = context.createTextMessage(JsonUtil.toJson(chunkResult));
            message.setStringProperty("resource", sink.getContent().getResource());
            context.createProducer().send(sinkQueue, message);
        } catch (JsonException | JMSException e) {
            throw new EJBException(e);
        }
    }

    public String sendToSink(Job job) throws JobStoreException {
        final long numberOfChunks = jobStore.getNumberOfChunksInJob(job.getId());
        final String sinkFileName = String.format("%s.sink.txt", job.getId());
        final Path sinkFilePath = Paths.get(jobStore.jobStorePath.toString(), sinkFileName);
        LOGGER.info("Combining decoded chunk results from {} chunks for jobId {} in {}", numberOfChunks, job.getId(), sinkFilePath);
        try (BufferedWriter writer = Files.newBufferedWriter(sinkFilePath,
                StandardCharsets.UTF_8, StandardOpenOption.CREATE, StandardOpenOption.APPEND)) {

            for (int chunkId = 1; chunkId <= numberOfChunks; chunkId++) {
                final ChunkResult chunkResult = jobStore.getProcessorResult(job.getId(), chunkId);
                for (String encodedRecord : chunkResult.getResults()) {
                    writer.write(base64decode(encodedRecord));
                    writer.newLine();
                }
            }
        } catch (IOException e) {
            throw new JobStoreException(String.format("Exception caught when trying to write to sink file %s", sinkFilePath), e);
        }
        return sinkFilePath.toString();
    }
}
