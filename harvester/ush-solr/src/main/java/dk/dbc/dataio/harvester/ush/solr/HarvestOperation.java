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

package dk.dbc.dataio.harvester.ush.solr;

import dk.dbc.commons.addi.AddiRecord;
import dk.dbc.dataio.bfs.api.BinaryFileStore;
import dk.dbc.dataio.common.utils.flowstore.FlowStoreServiceConnector;
import dk.dbc.dataio.common.utils.flowstore.FlowStoreServiceConnectorException;
import dk.dbc.dataio.commons.types.AddiMetaData;
import dk.dbc.dataio.commons.types.JobSpecification;
import dk.dbc.invariant.InvariantUtil;
import dk.dbc.dataio.commons.utils.jobstore.JobStoreServiceConnector;
import dk.dbc.dataio.filestore.service.connector.FileStoreServiceConnector;
import dk.dbc.dataio.harvester.types.HarvesterException;
import dk.dbc.dataio.harvester.types.UshHarvesterProperties;
import dk.dbc.dataio.harvester.types.UshSolrHarvesterConfig;
import dk.dbc.dataio.harvester.utils.ush.UshSolrConnector;
import dk.dbc.dataio.harvester.utils.ush.UshSolrDocument;
import dk.dbc.dataio.jobstore.types.JobInfoSnapshot;
import dk.dbc.dataio.jsonb.JSONBContext;
import dk.dbc.dataio.jsonb.JSONBException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.charset.StandardCharsets;
import java.util.Optional;

/**
 * Class representing a single harvest operation
 */
public class HarvestOperation {
    private static final Logger LOGGER = LoggerFactory.getLogger(HarvestOperation.class);

    private final FlowStoreServiceConnector flowStoreServiceConnector;
    private final BinaryFileStore binaryFileStore;
    private final FileStoreServiceConnector fileStoreServiceConnector;
    private final JobStoreServiceConnector jobStoreServiceConnector;

    private UshSolrHarvesterConfig config;
    private final UshHarvesterProperties ushHarvesterProperties;
    private final JSONBContext jsonbContext;

    UshSolrConnector ushSolrConnector;

    /**
     * Class constructor
     * @param config configuration used for this harvest
     * @param flowStoreServiceConnector connector used to update configuration
     * @param binaryFileStore used to create HarvesterJobBuilder in order to create dataIO job
     * @param fileStoreServiceConnector used to create HarvesterJobBuilder in order to create dataIO job
     * @param jobStoreServiceConnector used to look up harvester token in dataIO
     * @throws NullPointerException if given any null-valued argument
     * @throws HarvesterException on low-level binary wal file failure
     */
    public HarvestOperation(UshSolrHarvesterConfig config,
                            FlowStoreServiceConnector flowStoreServiceConnector,
                            BinaryFileStore binaryFileStore,
                            FileStoreServiceConnector fileStoreServiceConnector,
                            JobStoreServiceConnector jobStoreServiceConnector) throws NullPointerException, HarvesterException {
        this.config = InvariantUtil.checkNotNullOrThrow(config, "config");
        this.flowStoreServiceConnector = InvariantUtil.checkNotNullOrThrow(flowStoreServiceConnector, "flowStoreServiceConnector");
        this.binaryFileStore = InvariantUtil.checkNotNullOrThrow(binaryFileStore, "binaryFileStore");
        this.fileStoreServiceConnector = InvariantUtil.checkNotNullOrThrow(fileStoreServiceConnector, "fileStoreServiceConnector");
        this.jobStoreServiceConnector = InvariantUtil.checkNotNullOrThrow(jobStoreServiceConnector, "jobStoreServiceConnector");
        this.ushHarvesterProperties = config.getContent().getUshHarvesterProperties();
        this.ushSolrConnector = new UshSolrConnector(ushHarvesterProperties.getStorageUrl());
        this.jsonbContext = new JSONBContext();
    }

    /**
     * Runs this harvest operation, (re)doing configuration updates as needed.
     * @return number of records harvested
     * @throws HarvesterException if unable to complete harvest operation
    */
    public int execute() throws HarvesterException {
        int recordsAdded;
        try (HarvesterJobBuilder jobBuilder = new HarvesterJobBuilder(
                binaryFileStore,
                fileStoreServiceConnector,
                jobStoreServiceConnector,
                getJobSpecificationTemplate(config.getContent().getType())))
        {
            final UshSolrConnector.ResultSet resultSet = findDatabaseDocumentsHarvestedInInterval();
            LOGGER.info("Found {} records on Solr", resultSet.getSize());

            for (UshSolrDocument solrDocument : resultSet) {
                jobBuilder.addRecord(toAddiRecord(solrDocument));
            }
            jobBuilder.build();
            recordsAdded = jobBuilder.getRecordsAdded();

            // update config in flow store
            config.getContent().withTimeOfLastHarvest(ushHarvesterProperties.getLastHarvestFinished());
            updateHarvesterConfig(config);
        }
        return recordsAdded;
    }

    /**
     * Runs this test harvest operation which will harvest a maximum of 100 records.
     * @return Optional JobInfoSnapshot
     * @throws HarvesterException if unable to complete harvest operation
     */
    public Optional<JobInfoSnapshot> executeTest() throws HarvesterException {
        // do harvest 100 records...
        int testRecordsAdded = 100;
        try (HarvesterJobBuilder jobBuilder = new HarvesterJobBuilder(
                binaryFileStore,
                fileStoreServiceConnector,
                jobStoreServiceConnector,
                getJobSpecificationTemplate(JobSpecification.Type.ACCTEST)))
        {
            final UshSolrConnector.ResultSet resultSet = findDatabaseDocumentsHarvestedInInterval();

            for (UshSolrDocument solrDocument : resultSet) {
                jobBuilder.addRecord(toAddiRecord(solrDocument));
                if(jobBuilder.getRecordsAdded() == testRecordsAdded) {
                    break;
                }
            }
            return jobBuilder.build();
        }
    }

    JobSpecification getJobSpecificationTemplate(JobSpecification.Type type) throws HarvesterException {
        try {
            final UshSolrHarvesterConfig.Content configFields = config.getContent();
            return new JobSpecification()
                    .withPackaging(configFields.getPackaging())
                    .withFormat(configFields.getFormat())
                    .withCharset(configFields.getCharset())
                    .withDestination(configFields.getDestination())
                    .withSubmitterId(configFields.getSubmitterNumber())
                    .withMailForNotificationAboutVerification("placeholder")
                    .withMailForNotificationAboutProcessing("placeholder")
                    .withResultmailInitials("placeholder")
                    .withDataFile("placeholder")
                    .withType(type)
                    .withAncestry(new JobSpecification.Ancestry()
                            .withHarvesterToken(config.getHarvesterToken()));
        } catch (RuntimeException e) {
            throw new HarvesterException("Unable to create job specification template", e);
        }
    }

    private AddiRecord toAddiRecord(UshSolrDocument document) throws HarvesterException {
        final AddiMetaData addiMetaData = new AddiMetaData()
                .withSubmitterNumber(config.getContent().getSubmitterNumber())
                .withFormat(config.getContent().getFormat())
                .withBibliographicRecordId(document.id)
                .withTrackingId(document.id + "." + document.version);

        return createAddiRecord(addiMetaData, document.originalRecord());
    }

    private UshSolrConnector.ResultSet findDatabaseDocumentsHarvestedInInterval() {
        return ushSolrConnector.findDatabaseDocumentsHarvestedInInterval(
                config.getContent().getUshHarvesterJobId().toString(),
                config.getContent().getTimeOfLastHarvest(),
                ushHarvesterProperties.getLastHarvestFinished());
    }

    private AddiRecord createAddiRecord(AddiMetaData addiMetaData, byte[] content) throws HarvesterException {
        try {
            return new AddiRecord(jsonbContext.marshall(addiMetaData).getBytes(StandardCharsets.UTF_8), content);
        } catch (JSONBException e) {
            throw new HarvesterException("Error marshalling Addi metadata", e);
        }
    }

    private UshSolrHarvesterConfig updateHarvesterConfig(UshSolrHarvesterConfig config) throws HarvesterException {
        try {
            return flowStoreServiceConnector.updateHarvesterConfig(config);
        } catch (FlowStoreServiceConnectorException | RuntimeException e) {
            throw new HarvesterException("Failed to update harvester config: " + config.toString(), e);
        }
    }
}
