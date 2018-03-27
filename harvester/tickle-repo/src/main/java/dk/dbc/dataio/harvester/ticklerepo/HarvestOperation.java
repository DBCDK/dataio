/*
 * DataIO - Data IO
 * Copyright (C) 2015 Dansk Bibliotekscenter a/s, Tempovej 7-11, DK-2750 Ballerup,
 * Denmark. CVR: 15149043
 *
 * This file is part of DataIO.
 *
 * DataIO is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * DataIO is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with DataIO.  If not, see <http://www.gnu.org/licenses/>.
 */

package dk.dbc.dataio.harvester.ticklerepo;

import dk.dbc.commons.addi.AddiRecord;
import dk.dbc.dataio.bfs.api.BinaryFileStore;
import dk.dbc.dataio.common.utils.flowstore.FlowStoreServiceConnector;
import dk.dbc.dataio.commons.time.StopWatch;
import dk.dbc.dataio.commons.types.AddiMetaData;
import dk.dbc.invariant.InvariantUtil;
import dk.dbc.dataio.commons.utils.jobstore.JobStoreServiceConnector;
import dk.dbc.dataio.commons.utils.jobstore.JobStoreServiceConnectorException;
import dk.dbc.dataio.filestore.service.connector.FileStoreServiceConnector;
import dk.dbc.dataio.harvester.task.TaskRepo;
import dk.dbc.dataio.harvester.types.HarvesterException;
import dk.dbc.dataio.harvester.types.TickleRepoHarvesterConfig;
import dk.dbc.dataio.jobstore.types.criteria.JobListCriteria;
import dk.dbc.dataio.jobstore.types.criteria.ListFilter;
import dk.dbc.dataio.jsonb.JSONBContext;
import dk.dbc.dataio.jsonb.JSONBException;
import dk.dbc.ticklerepo.TickleRepo;
import dk.dbc.ticklerepo.dto.Batch;
import dk.dbc.ticklerepo.dto.DataSet;
import dk.dbc.ticklerepo.dto.Record;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.Optional;

/**
 * Class representing a single harvest operation
 */
public class HarvestOperation {
    private static final Logger LOGGER = LoggerFactory.getLogger(HarvestOperation.class);

    private final BinaryFileStore binaryFileStore;
    private final FileStoreServiceConnector fileStoreServiceConnector;
    private final JobStoreServiceConnector jobStoreServiceConnector;
    private final FlowStoreServiceConnector flowStoreServiceConnector;
    private final TickleRepo tickleRepo;
    private final TaskRepo taskRepo;
    private final TickleRepoHarvesterConfig config;
    private final DataSet dataset;
    private final JSONBContext jsonbContext = new JSONBContext();

    /**
     * Class constructor
     * @param config configuration used for this harvest
     * @param flowStoreServiceConnector connector used to update configuration
     * @param binaryFileStore used to create HarvesterJobBuilder in order to create dataIO job
     * @param fileStoreServiceConnector used to create HarvesterJobBuilder in order to create dataIO job
     * @param jobStoreServiceConnector used to create HarvesterJobBuilder in order to create dataIO job
     * @param tickleRepo tickle repository API
     * @param taskRepo harvest task repository API
     * @throws NullPointerException if given any null-valued argument
     * @throws IllegalStateException if unable to resolve tickle dataset
     */
    HarvestOperation(TickleRepoHarvesterConfig config,
                     FlowStoreServiceConnector flowStoreServiceConnector,
                     BinaryFileStore binaryFileStore,
                     FileStoreServiceConnector fileStoreServiceConnector,
                     JobStoreServiceConnector jobStoreServiceConnector,
                     TickleRepo tickleRepo,
                     TaskRepo taskRepo) throws NullPointerException, IllegalStateException {
        this.config = InvariantUtil.checkNotNullOrThrow(config, "config");
        this.binaryFileStore = InvariantUtil.checkNotNullOrThrow(binaryFileStore, "binaryFileStore");
        this.fileStoreServiceConnector = InvariantUtil.checkNotNullOrThrow(fileStoreServiceConnector, "fileStoreServiceConnector");
        this.jobStoreServiceConnector = InvariantUtil.checkNotNullOrThrow(jobStoreServiceConnector, "jobStoreServiceConnector");
        this.flowStoreServiceConnector = InvariantUtil.checkNotNullOrThrow(flowStoreServiceConnector, "flowStoreServiceConnector");
        this.tickleRepo = tickleRepo;
        this.taskRepo = taskRepo;
        this.dataset = getDataset(config)
                .orElseThrow(() -> new IllegalStateException(
                        "No dataset found for " + config.getContent().getDatasetName()));
    }

    /**
     * Runs this harvest operation
     * @return number of records harvested
     * @throws HarvesterException if unable to complete harvest operation
    */
    int execute() throws HarvesterException {
        final StopWatch stopWatch = new StopWatch();

        Batch batchToHarvest = getNextBatch(config);
        while (configUpdateResubmitted(config, batchToHarvest)) {
            batchToHarvest = getNextBatch(config);
        }

        int recordsHarvested;
        final RecordsIterator recordsIterator = createRecordsIterator(batchToHarvest);
        try (HarvesterJobBuilder jobBuilder = new HarvesterJobBuilder(binaryFileStore, fileStoreServiceConnector, jobStoreServiceConnector,
                JobSpecificationTemplate.create(config, dataset, batchToHarvest))) {
            for (Record record : recordsIterator) {
                LOGGER.info("{} ready for harvesting from {}/{}", record.getLocalId(), record.getDataset(), record.getBatch());

                final AddiMetaData addiMetaData = new AddiMetaData()
                        .withTrackingId(record.getTrackingId())
                        .withBibliographicRecordId(record.getLocalId())
                        .withCreationDate(record.getTimeOfCreation())
                        .withDeleted(record.getStatus() == Record.Status.DELETED)
                        .withSubmitterNumber(dataset.getAgencyId())
                        .withFormat(config.getContent().getFormat());

                jobBuilder.addRecord(createAddiRecord(addiMetaData, record.getContent()));
            }
            jobBuilder.build();
            recordsHarvested = jobBuilder.getRecordsAdded();
        } finally {
            recordsIterator.close();
        }
        config.getContent().withTimeOfLastBatchHarvested(new Date());
        ConfigUpdater.create(flowStoreServiceConnector).updateHarvesterConfig(config, batchToHarvest);

        recordsIterator.commit();

        LOGGER.info("Harvested {} records in {} ms", recordsHarvested, stopWatch.getElapsedTime());
        return recordsHarvested;
    }

    private Optional<DataSet> getDataset(TickleRepoHarvesterConfig config) {
        return tickleRepo.lookupDataSet(new DataSet().withName(config.getContent().getDatasetName()));
    }

    private Batch getNextBatch(TickleRepoHarvesterConfig config) {
        final Batch lastBatchHarvested = new Batch()
                .withId(config.getContent().getLastBatchHarvested())
                .withDataset(dataset.getId());

        final Optional<Batch> nextBatch = tickleRepo.getNextBatch(lastBatchHarvested);
        LOGGER.debug("Searching for next batch with {} returned {}", lastBatchHarvested, nextBatch.orElse(null));
        return nextBatch.orElse(null);
    }

    private AddiRecord createAddiRecord(AddiMetaData addiMetaData, byte[] content) throws HarvesterException {
        try {
            return new AddiRecord(jsonbContext.marshall(addiMetaData).getBytes(StandardCharsets.UTF_8), content);
        } catch (JSONBException e) {
            throw new HarvesterException("Error marshalling Addi metadata", e);
        }
    }

    private boolean configUpdateResubmitted(TickleRepoHarvesterConfig config, Batch batch) throws HarvesterException {
        if (batch != null && harvesterTokenExistsInJobStore(config.getHarvesterToken(batch.getId()))) {
            LOGGER.info("Re-submitting config update for batch {}", batch.getId());
            ConfigUpdater.create(flowStoreServiceConnector).updateHarvesterConfig(config, batch);
            return true;
        }
        return false;
    }

    private boolean harvesterTokenExistsInJobStore(String harvesterToken) throws HarvesterException {
        final String harvesterTokenJson = String.format("{\"ancestry\": {\"harvesterToken\": \"%s\"}}", harvesterToken);
        final JobListCriteria criteria = new JobListCriteria()
                .where(new ListFilter<>(JobListCriteria.Field.SPECIFICATION, ListFilter.Op.JSON_LEFT_CONTAINS, harvesterTokenJson));
        try {
            return !jobStoreServiceConnector.listJobs(criteria).isEmpty();
        } catch (JobStoreServiceConnectorException | RuntimeException e) {
            throw new HarvesterException("Failed to query dataIO job-store for harvester token: " + harvesterToken, e);
        }
    }

    private RecordsIterator createRecordsIterator(Batch batch) {
        if (batch != null) {
            final BatchRecordsIterator batchRecordsIterator = new BatchRecordsIterator(tickleRepo, batch);
            if (batchRecordsIterator.iterator().hasNext()) {
                return batchRecordsIterator;
            }
            batchRecordsIterator.close();
        }
        return new TaskRecordsIterator(tickleRepo, taskRepo, config);
    }
}
