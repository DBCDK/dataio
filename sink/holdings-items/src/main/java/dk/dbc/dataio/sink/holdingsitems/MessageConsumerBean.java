/*
 * Copyright Dansk Bibliotekscenter a/s. Licensed under GNU GPLv3
 * See license text in LICENSE.txt
 */

package dk.dbc.dataio.sink.holdingsitems;

import com.fasterxml.jackson.databind.type.CollectionType;
import dk.dbc.commons.addi.AddiReader;
import dk.dbc.commons.addi.AddiRecord;
import dk.dbc.commons.jsonb.JSONBContext;
import dk.dbc.commons.jsonb.JSONBException;
import dk.dbc.dataio.commons.types.Chunk;
import dk.dbc.dataio.commons.types.ChunkItem;
import dk.dbc.dataio.commons.types.ConsumedMessage;
import dk.dbc.dataio.commons.types.Diagnostic;
import dk.dbc.dataio.commons.types.exceptions.InvalidMessageException;
import dk.dbc.dataio.commons.utils.lang.StringUtil;
import dk.dbc.dataio.sink.types.AbstractSinkMessageConsumerBean;
import dk.dbc.dataio.sink.types.SinkException;
import dk.dbc.log.DBCTrackedLogContext;
import dk.dbc.solrdocstore.connector.SolrDocStoreConnector;
import dk.dbc.solrdocstore.connector.SolrDocStoreConnectorException;
import dk.dbc.solrdocstore.connector.SolrDocStoreConnectorUnexpectedStatusCodeException;
import dk.dbc.solrdocstore.connector.model.HoldingsItems;
import dk.dbc.solrdocstore.connector.model.Status;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ejb.MessageDriven;
import javax.inject.Inject;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;

@MessageDriven
public class MessageConsumerBean extends AbstractSinkMessageConsumerBean {
    private static final Logger LOGGER = LoggerFactory.getLogger(MessageConsumerBean.class);

    private final JSONBContext jsonbContext = new JSONBContext();
    private final CollectionType holdingsItemsListType = jsonbContext.getTypeFactory()
            .constructCollectionType(List.class, HoldingsItems.class);

    @Inject SolrDocStoreConnector solrDocStoreConnector;

    @Override
    public void handleConsumedMessage(ConsumedMessage consumedMessage) throws InvalidMessageException, SinkException {
        final Chunk chunk = unmarshallPayload(consumedMessage);
        LOGGER.info("Received chunk {}/{}", chunk.getJobId(), chunk.getChunkId());

        uploadChunk(handleChunk(chunk));
    }

    Chunk handleChunk(Chunk chunk) throws SinkException {
        final Chunk result = new Chunk(chunk.getJobId(), chunk.getChunkId(), Chunk.Type.DELIVERED);
        try {
            for (ChunkItem chunkItem : chunk.getItems()) {
                DBCTrackedLogContext.setTrackingId(chunkItem.getTrackingId());
                result.insertItem(handleChunkItem(chunkItem));
            }
        } finally {
            DBCTrackedLogContext.remove();
        }
        return result;
    }

    private ChunkItem handleChunkItem(ChunkItem chunkItem) throws SinkException {
        final ChunkItem result = new ChunkItem()
                .withId(chunkItem.getId())
                .withTrackingId(chunkItem.getTrackingId())
                .withType(ChunkItem.Type.STRING)
                .withEncoding(StandardCharsets.UTF_8);
        try {
            switch (chunkItem.getStatus()) {
                case FAILURE:
                    return result
                            .withStatus(ChunkItem.Status.IGNORE)
                            .withData("Failed by processor");
                case IGNORE:
                    return result
                            .withStatus(ChunkItem.Status.IGNORE)
                            .withData("Ignored by processor");
                default:
                    return result
                            .withStatus(ChunkItem.Status.SUCCESS)
                            .withData(deliverHoldingsItems(chunkItem));
            }
        } catch (IllegalArgumentException | IllegalStateException e) {
            return result
                    .withStatus(ChunkItem.Status.FAILURE)
                    .withDiagnostics(new Diagnostic(Diagnostic.Level.FATAL, e.getMessage(), e))
                    .withData(e.getMessage());
        } catch (SolrDocStoreConnectorException e) {
            throw new SinkException(e);
        }
    }

    private String deliverHoldingsItems(ChunkItem chunkItem) throws SolrDocStoreConnectorException {
        final StringBuilder resultStatus = new StringBuilder();
        final AddiReader addiReader = new AddiReader(new ByteArrayInputStream(chunkItem.getData()));
        boolean hasError = false;
        try {
            while (addiReader.hasNext()) {
                final AddiRecord addiRecord = addiReader.next();

                final List<HoldingsItems> holdingsItemsList = jsonbContext.unmarshall(
                        StringUtil.asString(addiRecord.getContentData()), holdingsItemsListType);
                for (HoldingsItems holdingsItems : holdingsItemsList) {
                    try {
                        final Status status = solrDocStoreConnector.setHoldings(holdingsItems);
                        resultStatus.append(String.format("%s:%d consumer service response - %s\n",
                                holdingsItems.getBibliographicRecordId(), holdingsItems.getAgencyId(), status.getText()));
                    } catch (SolrDocStoreConnectorUnexpectedStatusCodeException e) {
                        String error = "status code " + e.getStatusCode();
                        if (e.getStatus() != null) {
                            error = e.getStatus().getText();
                        }
                        resultStatus.append(String.format("%s:%d consumer service response - %s\n",
                                holdingsItems.getBibliographicRecordId(), holdingsItems.getAgencyId(), error));

                        hasError = true;
                    }
                }
            }
        } catch (IOException | JSONBException e) {
            throw new IllegalArgumentException("Invalid chunk item", e);
        }

        if (hasError) {
            throw new IllegalStateException(resultStatus.toString());
        }
        return resultStatus.toString();
    }
}
