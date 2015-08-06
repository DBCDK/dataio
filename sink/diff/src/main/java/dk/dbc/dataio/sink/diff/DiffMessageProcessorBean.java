package dk.dbc.dataio.sink.diff;

import dk.dbc.dataio.commons.types.ChunkItem;
import dk.dbc.dataio.commons.types.ConsumedMessage;
import dk.dbc.dataio.commons.types.ExternalChunk;
import dk.dbc.dataio.commons.types.exceptions.InvalidMessageException;
import dk.dbc.dataio.commons.types.exceptions.ServiceException;
import dk.dbc.dataio.commons.utils.jobstore.JobStoreServiceConnectorException;
import dk.dbc.dataio.commons.utils.jobstore.JobStoreServiceConnectorUnexpectedStatusCodeException;
import dk.dbc.dataio.commons.utils.jobstore.ejb.JobStoreServiceConnectorBean;
import dk.dbc.dataio.commons.utils.lang.StringUtil;
import dk.dbc.dataio.commons.utils.service.AbstractSinkMessageConsumerBean;
import dk.dbc.dataio.jobstore.types.JobError;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ejb.EJB;
import javax.ejb.EJBException;
import javax.ejb.MessageDriven;
import java.util.ArrayList;
import java.util.List;

@MessageDriven
public class DiffMessageProcessorBean extends AbstractSinkMessageConsumerBean {
    private static final Logger LOGGER = LoggerFactory.getLogger(DiffMessageProcessorBean.class);

    @EJB
    JobStoreServiceConnectorBean jobStoreServiceConnectorBean;

    @Override
    public void handleConsumedMessage(ConsumedMessage consumedMessage) throws ServiceException, InvalidMessageException {
        final ExternalChunk processedChunk = unmarshallPayload(consumedMessage);
        final ExternalChunk deliveredChunk = processPayload(processedChunk);
        try {
            jobStoreServiceConnectorBean.getConnector().addChunkIgnoreDuplicates(deliveredChunk, deliveredChunk.getJobId(), deliveredChunk.getChunkId());
        } catch (JobStoreServiceConnectorException e) {
            if (e instanceof JobStoreServiceConnectorUnexpectedStatusCodeException) {
                final JobError jobError = ((JobStoreServiceConnectorUnexpectedStatusCodeException) e).getJobError();
                if (jobError != null) {
                    LOGGER.error("job-store returned error: {}", jobError.getDescription());
                }
            }
            throw new EJBException(e);
        }
    }


    private ExternalChunk failWithMissingNextItem(ExternalChunk processedChunk) {
        final ExternalChunk deliveredChunk = new ExternalChunk(processedChunk.getJobId(), processedChunk.getChunkId(), ExternalChunk.Type.DELIVERED);

        for (final ChunkItem item : processedChunk) {
            deliveredChunk.insertItem(
                    new ChunkItem(item.getId(), StringUtil.asBytes("Missing Next Items"), ChunkItem.Status.FAILURE));
        }
        return deliveredChunk;

    }

    ExternalChunk processPayload(ExternalChunk processedChunk) {
        if( !processedChunk.hasNextItems()) {
            return failWithMissingNextItem( processedChunk );
        }

        final ExternalChunk deliveredChunk = new ExternalChunk(processedChunk.getJobId(), processedChunk.getChunkId(), ExternalChunk.Type.DELIVERED);
        for (final ChunkItemPair item : buildCurrentNextChunkList(processedChunk)) {
            if( item.current.getStatus() != item.next.getStatus() ) {
                String message = String.format("Different status %s -> %s\n%s",
                        statusToString(item.current.getStatus()),
                        statusToString(item.next.getStatus()),
                        StringUtil.asString(item.next.getData())
                );
                deliveredChunk.insertItem(
                        new ChunkItem(item.next.getId(), StringUtil.asBytes(message), ChunkItem.Status.FAILURE));
            } else {
                try {
                    final XmlDiffGenerator xmlDiffGenerator = new XmlDiffGenerator();
                    final String diff = xmlDiffGenerator.getDiff(item.current.getData(), item.next.getData());

                    if(diff.isEmpty()) {
                        deliveredChunk.insertItem(
                                new ChunkItem(item.current.getId(), StringUtil.asBytes(diff, processedChunk.getEncoding()), ChunkItem.Status.SUCCESS));
                    } else {
                        deliveredChunk.insertItem(
                                new ChunkItem(item.current.getId(), StringUtil.asBytes(diff, processedChunk.getEncoding()), ChunkItem.Status.FAILURE));
                    }

                } catch (DiffGeneratorException e) {
                    deliveredChunk.insertItem(
                            new ChunkItem(item.current.getId(), StringUtil.asBytes(e.toString()), ChunkItem.Status.FAILURE));
                }
            }
        }
        return deliveredChunk;
    }


    static private String statusToString( ChunkItem.Status status) {
        switch ( status ) {
            case FAILURE: return "Failure";
            case SUCCESS: return "Success";
            case IGNORE: return "Ignore";
            default:
                return "Internal Error: Unknown Status";
        }
    }

    static class ChunkItemPair {
        public ChunkItemPair(ChunkItem current, ChunkItem next) {
            this.current = current;
            this.next = next;
        }

        public ChunkItem current;
        public ChunkItem next;
    }
    List<ChunkItemPair> buildCurrentNextChunkList( ExternalChunk processed ) {
        final List<ChunkItem> items=processed.getItems();
        final List<ChunkItem> next=processed.getNext();
        if( items.size() != next.size() ) {
            throw new IllegalArgumentException("Internal Error item and next length differ");
        }
        final List<ChunkItemPair> result=new ArrayList<>();
        for( int i = 0 ; i < items.size() ; i++ ) {
            result.add( new ChunkItemPair(items.get(i), next.get(i)));
        }
        return result;
    }
}
