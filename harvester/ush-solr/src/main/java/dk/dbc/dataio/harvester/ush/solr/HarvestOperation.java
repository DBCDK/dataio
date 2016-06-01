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

import dk.dbc.dataio.common.utils.flowstore.FlowStoreServiceConnector;
import dk.dbc.dataio.common.utils.flowstore.FlowStoreServiceConnectorException;
import dk.dbc.dataio.commons.utils.invariant.InvariantUtil;
import dk.dbc.dataio.commons.utils.jobstore.JobStoreServiceConnectorException;
import dk.dbc.dataio.harvester.types.HarvesterException;
import dk.dbc.dataio.harvester.types.UshSolrHarvesterConfig;
import dk.dbc.dataio.jobstore.types.criteria.JobListCriteria;
import dk.dbc.dataio.jobstore.types.criteria.ListFilter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Optional;

/**
 * Class representing a single harvest operation
 */
public class HarvestOperation {
    private static final Logger LOGGER = LoggerFactory.getLogger(HarvestOperation.class);

    private final HarvesterJobBuilder harvesterJobBuilder;
    private final FlowStoreServiceConnector flowStoreServiceConnector;
    private final HarvesterWal wal;

    private UshSolrHarvesterConfig config;

    /**
     * Class constructor
     * @param config configuration used for this harvest
     * @param flowStoreServiceConnector connector used to update configuration
     * @param harvesterJobBuilder builder used to create dataIO job
     * @throws NullPointerException if given any null-valued argument
     * @throws HarvesterException on low-level binary wal file failure
     */
    public HarvestOperation(UshSolrHarvesterConfig config,
                            FlowStoreServiceConnector flowStoreServiceConnector,
                            HarvesterJobBuilder harvesterJobBuilder) throws NullPointerException, HarvesterException {
        this.config = InvariantUtil.checkNotNullOrThrow(config, "config");
        this.flowStoreServiceConnector = InvariantUtil.checkNotNullOrThrow(flowStoreServiceConnector, "flowStoreServiceConnector");
        this.harvesterJobBuilder = InvariantUtil.checkNotNullOrThrow(harvesterJobBuilder, "harvesterJobBuilder");
        this.wal = new HarvesterWal(config, harvesterJobBuilder.getBinaryFileStore());
    }

    /**
     * Runs this harvest operation, (re)doing configuration updates as needed.
     * @return number of records harvested
     * @throws HarvesterException if unable to complete harvest operation
     */
    public int execute() throws HarvesterException {
        redoConfigUpdateIfUncommitted();
        wal.write(getWalEntry());  // Write new WAL entry

        // do harvest...

        wal.commit();
        return 0;
    }

    void redoConfigUpdateIfUncommitted() throws HarvesterException {
        final Optional<HarvesterWal.WalEntry> walEntry = wal.read();
        if (walEntry.isPresent() && harvesterTokenExistsInDataIo(walEntry.get().toString())) {
            LOGGER.info("Found uncommitted WAL entry for existing dataIO job - updating config");
            config.getContent().withTimeOfLastHarvest(walEntry.get().getUntil());
            config = updateHarvesterConfig(config);
        }
        wal.commit();
    }

    boolean harvesterTokenExistsInDataIo(String harvesterToken) throws HarvesterException {
        final String harvesterTokenJson = String.format("{\"ancestry\": {\"harvesterToken\": \"%s\"}}", harvesterToken);
        final JobListCriteria criteria = new JobListCriteria()
                .where(new ListFilter<>(JobListCriteria.Field.SPECIFICATION, ListFilter.Op.JSON_LEFT_CONTAINS, harvesterTokenJson));
        try {
            return !harvesterJobBuilder.getJobStoreServiceConnector().listJobs(criteria).isEmpty();
        } catch (JobStoreServiceConnectorException | RuntimeException e) {
            throw new HarvesterException("Failed to query dataIO job-store for harvester token: " + harvesterToken, e);
        }
    }

    private UshSolrHarvesterConfig updateHarvesterConfig(UshSolrHarvesterConfig config) throws HarvesterException {
        try {
            return flowStoreServiceConnector.updateHarvesterConfig(config);
        } catch (FlowStoreServiceConnectorException | RuntimeException e) {
            throw new HarvesterException("Failed to update harvester config: " + config.toString(), e);
        }
    }

    private HarvesterWal.WalEntry getWalEntry() {
        return HarvesterWal.WalEntry.create(
                config.getId(),
                config.getVersion(),
                config.getContent().getTimeOfLastHarvest(),
                config.getContent().getUshHarvesterProperties().getLastHarvested());
    }
}
