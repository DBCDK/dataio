/*
 * DataIO - Data IO
 *
 * Copyright (C) 2018 Dansk Bibliotekscenter a/s, Tempovej 7-11, DK-2750 Ballerup,
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

import dk.dbc.dataio.bfs.ejb.BinaryFileStoreBean;
import dk.dbc.dataio.common.utils.flowstore.FlowStoreServiceConnector;
import dk.dbc.dataio.commons.utils.jobstore.JobStoreServiceConnector;
import dk.dbc.dataio.filestore.service.connector.FileStoreServiceConnector;
import dk.dbc.dataio.harvester.task.TaskRepo;
import dk.dbc.dataio.harvester.types.TickleRepoHarvesterConfig;
import dk.dbc.ticklerepo.TickleRepo;

public class ViafHarvestOperation extends HarvestOperation {
    public ViafHarvestOperation(TickleRepoHarvesterConfig config,
                                FlowStoreServiceConnector flowStoreServiceConnector,
                                BinaryFileStoreBean binaryFileStore,
                                FileStoreServiceConnector fileStoreServiceConnector,
                                JobStoreServiceConnector jobStoreServiceConnector,
                                TickleRepo tickleRepo, TaskRepo taskRepo) {
        super(config, flowStoreServiceConnector, binaryFileStore, fileStoreServiceConnector,
                jobStoreServiceConnector, tickleRepo, taskRepo);
    }

    // TODO: 20-06-18 Implement special VIAF handling
}
