package dk.dbc.dataio.sink.es;

import dk.dbc.commons.addi.AddiRecord;
import dk.dbc.dataio.commons.types.ChunkItem;
import dk.dbc.dataio.commons.types.ConsumedMessage;
import dk.dbc.dataio.commons.types.ExternalChunk;
import dk.dbc.dataio.commons.types.SinkChunkResult;
import dk.dbc.dataio.commons.types.exceptions.InvalidMessageException;
import dk.dbc.dataio.commons.utils.json.JsonUtil;
import dk.dbc.dataio.commons.utils.service.AbstractSinkMessageConsumerBean;
import dk.dbc.dataio.sink.es.entity.EsInFlight;
import dk.dbc.dataio.sink.types.SinkException;
import dk.dbc.dataio.sink.utils.messageproducer.JobProcessorMessageProducerBean;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ejb.EJB;
import javax.ejb.MessageDriven;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@MessageDriven
public class EsMessageProcessorBean extends AbstractSinkMessageConsumerBean {
    private static final Logger LOGGER = LoggerFactory.getLogger(EsMessageProcessorBean.class);

    @EJB
    EsThrottlerBean esThrottler;

    @EJB
    EsConnectorBean esConnector;

    @EJB
    EsInFlightBean esInFlightAdmin;

    @EJB
    EsSinkConfigurationBean configuration;

    @EJB
    JobProcessorMessageProducerBean jobProcessorMessageProducer;

    /**
     * Generates ES workload from given processor result, ensures that ES processing
     * capacity is not exceeded by acquiring the necessary number of record slots from
     * the ES throttler, creates ES task package, and marks it as being in-flight.
     * @param consumedMessage message containing ChunkResult payload
     * @throws SinkException on any failure during workload processing
     * @throws InvalidMessageException if unable to unmarshall message payload to ChunkResult
     */
    @Override
    public void handleConsumedMessage(ConsumedMessage consumedMessage) throws SinkException, InvalidMessageException {
        final ExternalChunk processedChunk = unmarshallPayload(consumedMessage);
        final EsWorkload workload = getEsWorkloadFromChunkResult(processedChunk);

        try {
            esThrottler.acquireRecordSlots(workload.getAddiRecords().size());
        } catch (InterruptedException e) {
            throw new SinkException("Interrupted while waiting for free record slots", e);
        }
        try {
            if (workload.getAddiRecords().isEmpty()) {
                jobProcessorMessageProducer.send(workload.getSinkChunkResult());

                LOGGER.info("chunk {} of job {} contained no Addi records - sending result",
                        workload.getSinkChunkResult().getChunkId(), workload.getSinkChunkResult().getJobId());
            } else {
                final int targetReference = esConnector.insertEsTaskPackage(workload);
                final EsInFlight esInFlight = new EsInFlight();
                esInFlight.setResourceName(configuration.getEsResourceName());
                esInFlight.setJobId(workload.getSinkChunkResult().getJobId());
                esInFlight.setChunkId(workload.getSinkChunkResult().getChunkId());
                esInFlight.setRecordSlots(workload.getAddiRecords().size());
                esInFlight.setTargetReference(targetReference);
                esInFlight.setSinkChunkResult(JsonUtil.toJson(workload.getSinkChunkResult()));
                esInFlightAdmin.addEsInFlight(esInFlight);

                LOGGER.info("Created ES task package with target reference {} for chunk {} of job {}",
                        targetReference, workload.getSinkChunkResult().getChunkId(), workload.getSinkChunkResult().getJobId());
            }
        } catch (Exception e) {
            esThrottler.releaseRecordSlots(workload.getAddiRecords().size());
            throw new SinkException("Exception caught during workload processing", e);
        }
    }

    /**
     * Generates ES workload based on given ChunkResult content.
     * <br/> All input ChunkItems with status SUCCESS containing valid Addi records are converted into ChunkItem placeholders with status SUCCESS in SinkChunkResult
     * <br/> All input ChunkItems with status SUCCESS containing invalid Addi records are converted into ChunkItems with status FAILURE in SinkChunkResult
     * <br/> All input ChunkItems with status IGNORE are converted into ChunkItems with status IGNORE in SinkChunkResult
     * <br/> All input ChunkItems with status FAILURE are converted into ChunkItems with status IGNORE in SinkChunkResult
     * @param processedChunk processor result
     * @return ES workload
     * @throws SinkException on unhandled ChunkItem status
     */
    EsWorkload getEsWorkloadFromChunkResult(ExternalChunk processedChunk) throws SinkException {
        final int numberOfItems = processedChunk.size();
        final List<AddiRecord> addiRecords = new ArrayList<>(numberOfItems);
        final SinkChunkResult sinkChunkResult = new SinkChunkResult(processedChunk.getJobId(), processedChunk.getChunkId(),
                processedChunk.getEncoding(), new ArrayList<ChunkItem>(numberOfItems));

        for(ChunkItem chunkItem : processedChunk) {
            switch (chunkItem.getStatus()) {
                case SUCCESS:
                    try {
                        addiRecords.add(ESTaskPackageUtil.getAddiRecordFromChunkItem(chunkItem, processedChunk.getEncoding()));
                        sinkChunkResult.getItems().add(new ChunkItem(chunkItem.getId(), "Empty slot", ChunkItem.Status.SUCCESS));
                    } catch (RuntimeException | IOException e) {
                        sinkChunkResult.getItems().add(new ChunkItem(chunkItem.getId(), e.getMessage(), ChunkItem.Status.FAILURE));
                    }
                    break;
                case FAILURE:
                    sinkChunkResult.getItems().add(new ChunkItem(chunkItem.getId(), "Failed by processor", ChunkItem.Status.IGNORE));
                    break;
                case IGNORE:
                    sinkChunkResult.getItems().add(new ChunkItem(chunkItem.getId(), "Ignored by processor", ChunkItem.Status.IGNORE));
                    break;
                default:
                    throw new SinkException("Unknown chunk item state: " + chunkItem.getStatus().name());
            }
        }
        return new EsWorkload(sinkChunkResult, addiRecords);
    }
}
