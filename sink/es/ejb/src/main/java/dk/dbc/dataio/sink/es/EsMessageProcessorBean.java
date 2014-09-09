package dk.dbc.dataio.sink.es;

import dk.dbc.dataio.commons.types.ChunkResult;
import dk.dbc.dataio.commons.types.ConsumedMessage;
import dk.dbc.dataio.commons.types.exceptions.InvalidMessageException;
import dk.dbc.dataio.commons.utils.service.AbstractSinkMessageConsumerBean;
import dk.dbc.dataio.sink.es.entity.EsInFlight;
import dk.dbc.dataio.sink.types.SinkException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ejb.EJB;
import javax.ejb.MessageDriven;
import java.io.IOException;

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

    @Override
    public void handleConsumedMessage(ConsumedMessage consumedMessage) throws SinkException, InvalidMessageException {
        final ChunkResult chunkResult = unmarshallPayload(consumedMessage);
        final EsWorkload workload;
        try {
            workload = new EsWorkload(chunkResult, ESTaskPackageUtil.getAddiRecordsFromChunk(chunkResult));
        } catch (RuntimeException | IOException e) {
            throw new InvalidMessageException(String.format(
                    "Message<%s> ChunkResult payload contained invalid addi", consumedMessage.getMessageId()), e);
        }

        try {
            esThrottler.acquireRecordSlots(workload.getAddiRecords().size());
        } catch (InterruptedException e) {
            throw new SinkException("Interrupted while waiting for free record slots", e);
        }
        try {
            final int targetReference = esConnector.insertEsTaskPackage(workload);
            final EsInFlight esInFlight = new EsInFlight();
            esInFlight.setResourceName(configuration.getEsResourceName());
            esInFlight.setJobId(workload.getChunkResult().getJobId());
            esInFlight.setChunkId(workload.getChunkResult().getChunkId());
            esInFlight.setRecordSlots(workload.getAddiRecords().size());
            esInFlight.setTargetReference(targetReference);
            esInFlightAdmin.addEsInFlight(esInFlight);

            LOGGER.info("Created ES task package with target reference {} for chunk {} of job {}",
                    targetReference, workload.getChunkResult().getChunkId(), workload.getChunkResult().getJobId());

        } catch (Exception e) {
            esThrottler.releaseRecordSlots(workload.getAddiRecords().size());
            throw new SinkException("Exception caught during workload processing", e);
        }
    }
}
