package dk.dbc.dataio.sink.es;

import dk.dbc.commons.addi.AddiRecord;
import dk.dbc.dataio.commons.time.StopWatch;
import dk.dbc.dataio.commons.types.ChunkItem;
import dk.dbc.dataio.commons.types.ConsumedMessage;
import dk.dbc.dataio.commons.types.ExternalChunk;
import dk.dbc.dataio.commons.types.exceptions.InvalidMessageException;
import dk.dbc.dataio.commons.utils.jobstore.JobStoreServiceConnectorException;
import dk.dbc.dataio.commons.utils.jobstore.JobStoreServiceConnectorUnexpectedStatusCodeException;
import dk.dbc.dataio.commons.utils.jobstore.ejb.JobStoreServiceConnectorBean;
import dk.dbc.dataio.commons.utils.json.JsonUtil;
import dk.dbc.dataio.commons.utils.lang.StringUtil;
import dk.dbc.dataio.commons.utils.service.AbstractSinkMessageConsumerBean;
import dk.dbc.dataio.jobstore.types.JobError;
import dk.dbc.dataio.sink.es.entity.EsInFlight;
import dk.dbc.dataio.sink.types.SinkException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.PostConstruct;
import javax.ejb.EJB;
import javax.ejb.MessageDriven;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@MessageDriven
public class EsMessageProcessorBean extends AbstractSinkMessageConsumerBean {
    private static final Logger LOGGER = LoggerFactory.getLogger(EsMessageProcessorBean.class);

    private AddiRecordPreprocessor addiRecordPreprocessor;

    @EJB
    EsConnectorBean esConnector;

    @EJB
    EsInFlightBean esInFlightAdmin;

    @EJB
    EsSinkConfigurationBean configuration;

    @EJB
    JobStoreServiceConnectorBean jobStoreServiceConnectorBean;

    @PostConstruct
    public void setup() {
        addiRecordPreprocessor = new AddiRecordPreprocessor();
    }

    /**
     * Generates ES workload from given processor result, creates ES task package, and
     * marks it as being in-flight.
     * @param consumedMessage message containing chunk payload
     * @throws SinkException on any failure during workload processing
     * @throws InvalidMessageException if unable to unmarshall message payload to chunk
     */
    @Override
    public void handleConsumedMessage(ConsumedMessage consumedMessage) throws SinkException, InvalidMessageException {
        final ExternalChunk processedChunk = unmarshallPayload(consumedMessage);
        final EsWorkload workload = getEsWorkloadFromChunkResult(processedChunk);
        final ExternalChunk deliveredChunk = workload.getDeliveredChunk();

        try {
            if (workload.getAddiRecords().isEmpty()) {
                LOGGER.info("chunk {} of job {} contained no Addi records - sending result",
                        deliveredChunk.getChunkId(), deliveredChunk.getJobId());
                try {
                    jobStoreServiceConnectorBean.getConnector().addChunkIgnoreDuplicates(deliveredChunk, deliveredChunk.getJobId(), deliveredChunk.getChunkId());
                } catch (JobStoreServiceConnectorException e) {
                    if (e instanceof JobStoreServiceConnectorUnexpectedStatusCodeException) {
                        final JobError jobError = ((JobStoreServiceConnectorUnexpectedStatusCodeException) e).getJobError();
                        if (jobError != null) {
                            LOGGER.error("job-store returned error: {}", jobError.getDescription());
                        }
                    }
                }
            } else {
                final int targetReference = esConnector.insertEsTaskPackage(workload);
                final EsInFlight esInFlight = new EsInFlight();
                esInFlight.setResourceName(configuration.getEsResourceName());
                esInFlight.setJobId(deliveredChunk.getJobId());
                esInFlight.setChunkId(deliveredChunk.getChunkId());
                esInFlight.setRecordSlots(workload.getAddiRecords().size());
                esInFlight.setTargetReference(targetReference);
                esInFlight.setIncompleteDeliveredChunk(JsonUtil.toJson(deliveredChunk));
                esInFlightAdmin.addEsInFlight(esInFlight);

                LOGGER.info("Created ES task package with target reference {} for chunk {} of job {}",
                        targetReference, deliveredChunk.getChunkId(), deliveredChunk.getJobId());
            }
        } catch (Exception e) {
            throw new SinkException("Exception caught during workload processing", e);
        }
    }

    /**
     * Generates ES workload based on given chunk content.
     * <br/> All input ChunkItems with status SUCCESS containing valid Addi records are converted into ChunkItem placeholders with status SUCCESS in Delivered Chunk
     * <br/> All input ChunkItems with status SUCCESS containing invalid Addi records are converted into ChunkItems with status FAILURE in Delivered Chunk
     * <br/> All input ChunkItems with status IGNORE are converted into ChunkItems with status IGNORE in Delivered Chunk
     * <br/> All input ChunkItems with status FAILURE are converted into ChunkItems with status IGNORE in Delivered Chunk
     * @param processedChunk processor result
     * @return ES workload
     * @throws SinkException on unhandled ChunkItem status
     */
    EsWorkload getEsWorkloadFromChunkResult(ExternalChunk processedChunk) throws SinkException {
        final int numberOfItems = processedChunk.size();
        final List<AddiRecord> addiRecords = new ArrayList<>(numberOfItems);
        final ExternalChunk incompleteDeliveredChunk = new ExternalChunk(processedChunk.getJobId(), processedChunk.getChunkId(), ExternalChunk.Type.DELIVERED);
        incompleteDeliveredChunk.setEncoding(processedChunk.getEncoding());
        final StopWatch stopWatch = new StopWatch();

        for(ChunkItem chunkItem : processedChunk) {
            switch (chunkItem.getStatus()) {
                case SUCCESS:
                    try {
                        addiRecords.add(buildAddiRecord(chunkItem));
                        incompleteDeliveredChunk.insertItem(new ChunkItem(
                                chunkItem.getId(), StringUtil.asBytes("Empty slot"), ChunkItem.Status.SUCCESS));
                    } catch (RuntimeException | IOException e) {
                        incompleteDeliveredChunk.insertItem(new ChunkItem(
                                chunkItem.getId(), StringUtil.asBytes(e.getMessage()), ChunkItem.Status.FAILURE));
                    } finally {
                        LOGGER.info("Operation took {} milliseconds", stopWatch.getElapsedTime());
                    }
                    break;
                case FAILURE:
                    incompleteDeliveredChunk.insertItem(new ChunkItem(
                            chunkItem.getId(), StringUtil.asBytes("Failed by processor"), ChunkItem.Status.IGNORE));
                    break;
                case IGNORE:
                    incompleteDeliveredChunk.insertItem(new ChunkItem(
                            chunkItem.getId(), StringUtil.asBytes("Ignored by processor"), ChunkItem.Status.IGNORE));
                    break;
                default:
                    throw new SinkException("Unknown chunk item state: " + chunkItem.getStatus().name());
            }
        }
        return new EsWorkload(incompleteDeliveredChunk, addiRecords,
                configuration.getEsUserId(), configuration.getEsPackageType(), configuration.getEsAction());
    }

    AddiRecord buildAddiRecord(ChunkItem chunkItem) throws IllegalArgumentException, IOException {
        AddiRecord addiRecordFromChunkItem = ESTaskPackageUtil.getAddiRecordFromChunkItem(chunkItem);
        return addiRecordPreprocessor.execute(addiRecordFromChunkItem);
    }
}
