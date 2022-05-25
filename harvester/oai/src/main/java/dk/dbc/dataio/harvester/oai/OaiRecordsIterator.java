package dk.dbc.dataio.harvester.oai;

import dk.dbc.dataio.harvester.TimeInterval;
import dk.dbc.dataio.harvester.types.OaiHarvesterConfig;
import dk.dbc.oai.OaiConnector;
import dk.dbc.oai.OaiConnectorException;
import org.openarchives.oai.ListRecords;
import org.openarchives.oai.Record;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.ZoneId;
import java.util.ArrayDeque;
import java.util.Collections;
import java.util.Deque;
import java.util.Iterator;

/**
 * This class abstracts away an OAI ListRecords call followed by zero or more
 * ListRecords calls with resumption tokens into a series of {@link Record}s.
 */
public class OaiRecordsIterator implements Iterable<Record> {
    private static final Logger LOGGER = LoggerFactory.getLogger(OaiRecordsIterator.class);

    private final OaiHarvesterConfig config;
    private final OaiConnector oaiConnector;
    private final TimeInterval timeInterval;
    private Deque<Record> records;
    private String resumptionToken;

    public OaiRecordsIterator(OaiHarvesterConfig config,
                              OaiConnector oaiConnector, TimeInterval timeInterval) {
        this.config = config;
        this.oaiConnector = oaiConnector;
        this.timeInterval = timeInterval;
    }

    @Override
    public Iterator<Record> iterator() {
        return new Iterator<Record>() {
            @Override
            public boolean hasNext() {
                try {
                    return hasNextRecord();
                } catch (OaiConnectorException e) {
                    throw new IllegalStateException(
                            "Unable to retrieve records from OAI endpoint", e);
                }
            }

            @Override
            public Record next() {
                return getNextRecord();
            }
        };
    }

    private boolean hasNextRecord() throws OaiConnectorException {
        if (records == null) {
            // make the initial ListRecords call
            listRecords();
        } else if (records.isEmpty()
                && resumptionToken != null
                && !resumptionToken.isEmpty()) {
            // continue to make ListRecords calls
            // as long as resumptionToken is defined
            resumeListRecords();
        }
        return !records.isEmpty();
    }

    private void listRecords() throws OaiConnectorException {
        final OaiConnector.Params params = new OaiConnector.Params()
                .withFrom(timeInterval.getFrom().atZone(ZoneId.of("UTC")))
                .withUntil(timeInterval.getTo().atZone(ZoneId.of("UTC")))
                .withSet(config.getContent().getSet())
                .withMetadataPrefix(config.getContent().getFormat());
        LOGGER.info("Executing listRecords with params {}", params);
        executeListRecords(params);
    }

    private void resumeListRecords() throws OaiConnectorException {
        final OaiConnector.Params params = new OaiConnector.Params()
                .withResumptionToken(resumptionToken);
        resumptionToken = null;
        LOGGER.info("Resuming listRecords with params {}", params);
        executeListRecords(params);
    }

    private void executeListRecords(OaiConnector.Params params) throws OaiConnectorException {
        final ListRecords listRecords = oaiConnector.listRecords(params);
        if (listRecords.getRecords() != null) {
            records = new ArrayDeque<>(listRecords.getRecords());
            if (listRecords.getResumptionToken() != null) {
                resumptionToken = listRecords.getResumptionToken().getValue();
            }
        } else {
            records = new ArrayDeque<>(Collections.emptyList());
        }
    }

    private Record getNextRecord() {
        if (records == null) {
            return null;
        }
        return records.pollFirst();
    }
}
