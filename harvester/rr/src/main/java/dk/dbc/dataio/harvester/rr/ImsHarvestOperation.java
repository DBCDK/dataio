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

package dk.dbc.dataio.harvester.rr;

import dk.dbc.dataio.commons.time.StopWatch;
import dk.dbc.dataio.commons.types.AddiMetaData;
import dk.dbc.dataio.harvester.types.HarvesterException;
import dk.dbc.dataio.harvester.types.RRHarvesterConfig;
import dk.dbc.dataio.harvester.utils.holdingsitems.HoldingsItemsConnector;
import dk.dbc.dataio.harvester.utils.rawrepo.RawRepoConnector;
import dk.dbc.rawrepo.RecordId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.persistence.EntityManager;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class ImsHarvestOperation extends HarvestOperation {
    private static final Logger LOGGER = LoggerFactory.getLogger(ImsHarvestOperation.class);

    private final HoldingsItemsConnector holdingsItemsConnector;

    public ImsHarvestOperation(RRHarvesterConfig config, HarvesterJobBuilderFactory harvesterJobBuilderFactory)
            throws NullPointerException, IllegalArgumentException {
        this(config, harvesterJobBuilderFactory, null, null, null);
    }

    ImsHarvestOperation(RRHarvesterConfig config, HarvesterJobBuilderFactory harvesterJobBuilderFactory,
                     AgencyConnection agencyConnection, RawRepoConnector rawRepoConnector, HoldingsItemsConnector holdingsItemsConnector) {
        super(config, harvesterJobBuilderFactory, agencyConnection, rawRepoConnector);
        this.holdingsItemsConnector = holdingsItemsConnector != null ? holdingsItemsConnector : getHoldingsItemsConnector(config);
    }

    /**
     * Runs this harvest operation, creating dataIO jobs from harvested records.
     * If any non-internal error occurs a record is marked as failed. Only records from
     * IMS agency IDs are processed and DBC library are process, all others are skipped.
     * Records from DBC library are mapped into IMS libraries with holdings (if any).
     * @param entityManager local database entity manager
     * @return number of records processed
     * @throws HarvesterException on failure to complete harvest operation
     */
    @Override
    public int execute(EntityManager entityManager) throws HarvesterException {
        final StopWatch stopWatch = new StopWatch();
        final RecordQueue recordQueue = getRecordQueue(config, rawRepoConnector, entityManager);
        // Since we might (re)run batches with a size larger than the one currently configured
        final int batchSize = Math.max(configContent.getBatchSize(), recordQueue.size());

        Set<Integer> imsLibraries = null;
        if (recordQueue.size() > 0) {
            imsLibraries = agencyConnection.getFbsImsLibraries();
        }

        int itemsProcessed = 0;
        RecordId recordId = recordQueue.poll();
        while (recordId != null) {
            LOGGER.info("{} ready for harvesting", recordId);

            // There is quite a bit of waisted effort being done here when
            // the workload contains more than one record, since we will actually be
            // fetching/merging the same record more than once. Fixing this entails
            // either an ImsHarvestOperation implementation having very little code
            // in common with HarvestOperation or a complete rewrite of the
            // HarvestOperation class, neither of which we have the time for
            // currently.
            for (AddiMetaData addiMetaData : getWorkLoad(recordId, imsLibraries)) {
                processRecord(recordId, addiMetaData);
            }

            if (++itemsProcessed == batchSize) {
                break;
            }
            recordId = recordQueue.poll();
        }
        flushHarvesterJobBuilders();

        recordQueue.commit();

        LOGGER.info("Processed {} items from {} queue in {} ms",
                itemsProcessed, configContent.getConsumerId(), stopWatch.getElapsedTime());

        return itemsProcessed;
    }

    private HoldingsItemsConnector getHoldingsItemsConnector(RRHarvesterConfig config) throws NullPointerException, IllegalArgumentException {
        return new HoldingsItemsConnector(config.getContent().getImsHoldingsTarget());
    }

    private List<AddiMetaData> getWorkLoad(RecordId recordId, Set<Integer> imsLibraries) {
        if (recordId.getAgencyId() == DBC_LIBRARY) {
            return getWorkloadByHoldingsItemsLookup(recordId, imsLibraries);
        }

        if (imsLibraries.contains(recordId.getAgencyId())) {
            return Collections.singletonList(new AddiMetaData()
                    .withBibliographicRecordId(recordId.getBibliographicRecordId())
                    .withSubmitterNumber(recordId.getAgencyId()));
        }

        return Collections.emptyList();
    }

    private List<AddiMetaData> getWorkloadByHoldingsItemsLookup(RecordId recordId, Set<Integer> imsLibraries) {
        final Set<Integer> agenciesWithHoldings = holdingsItemsConnector.hasHoldings(recordId.getBibliographicRecordId(), imsLibraries);
        if (!agenciesWithHoldings.isEmpty()) {
            return agenciesWithHoldings.stream()
                    .filter(imsLibraries::contains)
                    .map(agencyId -> new AddiMetaData()
                            .withBibliographicRecordId(recordId.getBibliographicRecordId())
                            .withSubmitterNumber(agencyId))
                    .collect(Collectors.toList());
        }
        return Collections.emptyList();
    }
}
