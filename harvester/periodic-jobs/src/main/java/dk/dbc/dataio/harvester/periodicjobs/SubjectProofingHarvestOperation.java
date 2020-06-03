/*
 * Copyright Dansk Bibliotekscenter a/s. Licensed under GNU GPLv3
 * See license text in LICENSE.txt
 */

package dk.dbc.dataio.harvester.periodicjobs;

import dk.dbc.dataio.bfs.api.BinaryFileStore;
import dk.dbc.dataio.common.utils.flowstore.FlowStoreServiceConnector;
import dk.dbc.dataio.commons.utils.jobstore.JobStoreServiceConnector;
import dk.dbc.dataio.filestore.service.connector.FileStoreServiceConnector;
import dk.dbc.dataio.harvester.types.PeriodicJobsHarvesterConfig;
import dk.dbc.dataio.harvester.utils.rawrepo.RawRepoConnector;
import dk.dbc.weekresolver.WeekResolverConnector;

import javax.enterprise.concurrent.ManagedExecutorService;

public class SubjectProofingHarvestOperation extends HarvestOperation {
    public SubjectProofingHarvestOperation(PeriodicJobsHarvesterConfig config,
                                           BinaryFileStore binaryFileStore,
                                           FileStoreServiceConnector fileStoreServiceConnector,
                                           FlowStoreServiceConnector flowStoreServiceConnector,
                                           JobStoreServiceConnector jobStoreServiceConnector,
                                           WeekResolverConnector weekResolverConnector,
                                           ManagedExecutorService executor) {
        this(config, binaryFileStore, fileStoreServiceConnector, flowStoreServiceConnector, jobStoreServiceConnector,
                weekResolverConnector, executor, null);
    }

    SubjectProofingHarvestOperation(PeriodicJobsHarvesterConfig config,
                                    BinaryFileStore binaryFileStore,
                                    FileStoreServiceConnector fileStoreServiceConnector,
                                    FlowStoreServiceConnector flowStoreServiceConnector,
                                    JobStoreServiceConnector jobStoreServiceConnector,
                                    WeekResolverConnector weekResolverConnector,
                                    ManagedExecutorService executor,
                                    RawRepoConnector rawRepoConnector) {
        super(config, binaryFileStore, fileStoreServiceConnector, flowStoreServiceConnector, jobStoreServiceConnector,
                weekResolverConnector, executor, rawRepoConnector);
    }
}
