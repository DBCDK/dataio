/*
 * Copyright Dansk Bibliotekscenter a/s. Licensed under GNU GPLv3
 * See license text in LICENSE.txt
 */

package dk.dbc.dataio.harvester.oai;

import dk.dbc.commons.addi.AddiRecord;
import dk.dbc.dataio.bfs.api.BinaryFileStore;
import dk.dbc.dataio.common.utils.flowstore.FlowStoreServiceConnector;
import dk.dbc.dataio.commons.time.StopWatch;
import dk.dbc.dataio.commons.types.AddiMetaData;
import dk.dbc.dataio.commons.utils.jobstore.JobStoreServiceConnector;
import dk.dbc.dataio.filestore.service.connector.FileStoreServiceConnector;
import dk.dbc.dataio.harvester.TimeInterval;
import dk.dbc.dataio.harvester.TimeIntervalGenerator;
import dk.dbc.dataio.harvester.types.HarvesterException;
import dk.dbc.dataio.harvester.types.OaiHarvesterConfig;
import dk.dbc.dataio.jsonb.JSONBContext;
import dk.dbc.dataio.jsonb.JSONBException;
import dk.dbc.oai.OaiConnector;
import dk.dbc.oai.OaiConnectorException;
import org.openarchives.oai.Header;
import org.openarchives.oai.Metadata;
import org.openarchives.oai.Record;
import org.openarchives.oai.Status;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.Iterator;

public class HarvestOperation {
    private static final Logger LOGGER = LoggerFactory.getLogger(HarvestOperation.class);
    private static final long HARVEST_INTERVAL_DURATION_IN_SECONDS = 7200;
    private static final long HARVEST_LAG_IN_SECONDS = 30;

    static int HARVEST_MAX_BATCH_SIZE = 10000;  // This is not a hard limit!

    private OaiHarvesterConfig config;
    private final BinaryFileStore binaryFileStore;
    private final FlowStoreServiceConnector flowStoreServiceConnector;
    private final FileStoreServiceConnector fileStoreServiceConnector;
    private final JobStoreServiceConnector jobStoreServiceConnector;
    private final OaiConnector oaiConnector;
    private final JSONBContext jsonbContext = new JSONBContext();

    public HarvestOperation(OaiHarvesterConfig config,
                            BinaryFileStore binaryFileStore,
                            FlowStoreServiceConnector flowStoreServiceConnector,
                            FileStoreServiceConnector fileStoreServiceConnector,
                            JobStoreServiceConnector jobStoreServiceConnector,
                            OaiConnector oaiConnector) {
        this.config = config;
        this.binaryFileStore = binaryFileStore;
        this.flowStoreServiceConnector = flowStoreServiceConnector;
        this.fileStoreServiceConnector = fileStoreServiceConnector;
        this.jobStoreServiceConnector = jobStoreServiceConnector;
        this.oaiConnector = oaiConnector;
    }

    public int execute() throws HarvesterException {
        final StopWatch stopwatch = new StopWatch();
        int recordsHarvested = 0;
        try {
            final Instant endpointCurrentTime = getEndpointCurrentTime();

            while (true) {
                // Divide the harvest time interval into smaller sub-intervals.
                Instant timeOfLastHarvest = getTimeOfLastHarvestFromConfig();
                final TimeIntervalGenerator timeIntervalGenerator = new TimeIntervalGenerator()
                        .withIntervalDuration(HARVEST_INTERVAL_DURATION_IN_SECONDS, ChronoUnit.SECONDS)
                        .withStartingPoint(timeOfLastHarvest)
                        .withEndPoint(endpointCurrentTime, HARVEST_LAG_IN_SECONDS, ChronoUnit.SECONDS);
                final Iterator<TimeInterval> timeIntervalIterator = timeIntervalGenerator.iterator();

                try (JobBuilder jobBuilder = new JobBuilder(
                        binaryFileStore, fileStoreServiceConnector, jobStoreServiceConnector,
                        JobSpecificationTemplate.create(config))) {

                    while (timeIntervalIterator.hasNext()) {
                        final TimeInterval timeInterval = timeIntervalIterator.next();
                        for (Record record : new OaiRecordsIterator(config, oaiConnector, timeInterval)) {
                            jobBuilder.addRecord(createAddiRecord(record));
                        }
                        config.getContent().withTimeOfLastHarvest(Date.from(timeInterval.getTo()));
                        if (jobBuilder.getRecordsAdded() >= HARVEST_MAX_BATCH_SIZE) {
                            // To avoid overly large jobs we break the inner
                            // while loop and force a new JobBuilder to be created
                            // after successfully building the current job.
                            break;
                        }
                    }

                    jobBuilder.build();
                    recordsHarvested = jobBuilder.getRecordsAdded();
                }
                config = ConfigUpdater.create(flowStoreServiceConnector).push(config);

                if (!timeIntervalIterator.hasNext()) {
                    // End outer while loop since the TimeInterval series
                    // is exhausted.
                    break;
                }
            }

            return recordsHarvested;
        } catch (RuntimeException e) {
            throw new HarvesterException(e);
        } finally {
            LOGGER.info("Harvested {} records in {} ms",
                    recordsHarvested, stopwatch.getElapsedTime());
        }
    }

    private Instant getTimeOfLastHarvestFromConfig() {
        final Date timeOfLastHarvest = config.getContent().getTimeOfLastHarvest();
        if (timeOfLastHarvest != null) {
            return timeOfLastHarvest.toInstant();
        }
        return Instant.EPOCH;
    }

    private Instant getEndpointCurrentTime() throws HarvesterException {
        final ZonedDateTime currentTime;
        try {
            currentTime = oaiConnector.getServerCurrentTime();
        } catch (OaiConnectorException e) {
            throw new HarvesterException(
                    "Unable to retrieve current time from OAI endpoint", e);
        }
        return currentTime.toInstant();
    }

    private AddiRecord createAddiRecord(Record record) throws HarvesterException {
        try {
            // Addi metadata
            final Header header = record.getHeader();
            final AddiMetaData addiMetaData = new AddiMetaData()
                    .withTrackingId("oai." + config.getLogId() + "."
                        + header.getIdentifier())
                    .withBibliographicRecordId(header.getIdentifier())
                    .withFormat(config.getContent().getFormat())
                    .withSubmitterNumber(Integer.parseInt(config.getContent().getSubmitterNumber()));
            if (header.getStatus() != null
                    && header.getStatus() == Status.DELETED) {
                addiMetaData.withDeleted(true);
            }

            // Addi content
            byte[] content = null;
            final Metadata metadata = record.getMetadata();
            if (metadata != null) {
                // TODO: 27-05-19 Pass TransformerFactory to getBytes() to achieve performance boost
                content = metadata.getBytes();
            }

            return new AddiRecord(
                    jsonbContext.marshall(addiMetaData).getBytes(StandardCharsets.UTF_8),
                    content);
        } catch (JSONBException e) {
            throw new HarvesterException(e);
        }
    }
}
