package dk.dbc.dataio.sink.fbs.ejb;

import dk.dbc.dataio.commons.types.ConsumedMessage;
import dk.dbc.dataio.commons.types.ExternalChunk;
import dk.dbc.dataio.commons.types.exceptions.InvalidMessageException;
import dk.dbc.dataio.commons.utils.jobstore.JobStoreServiceConnectorException;
import dk.dbc.dataio.commons.utils.jobstore.ejb.JobStoreServiceConnectorBean;
import dk.dbc.dataio.commons.utils.service.AbstractSinkMessageConsumerBean;

import javax.ejb.EJB;
import javax.ejb.EJBException;
import javax.ejb.MessageDriven;

@MessageDriven
public class JobProcessorMessageConsumerBean extends AbstractSinkMessageConsumerBean {
    @EJB
    FbsPusherBean fbsPusher;

    @EJB
    JobStoreServiceConnectorBean jobStoreServiceConnectorBean;

    @Override
    public void handleConsumedMessage(ConsumedMessage consumedMessage) throws InvalidMessageException {
        final ExternalChunk deliveredChunk = fbsPusher.push(unmarshallPayload(consumedMessage));
        try {
            jobStoreServiceConnectorBean.getConnector().addChunkIgnoreDuplicates(deliveredChunk, deliveredChunk.getJobId(), deliveredChunk.getChunkId());
        } catch (JobStoreServiceConnectorException e) {
            throw new EJBException(e);
        }
    }
}
