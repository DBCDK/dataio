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

import dk.dbc.dataio.bfs.api.BinaryFileStore;
import dk.dbc.dataio.commons.types.JobSpecification;
import dk.dbc.dataio.commons.utils.jobstore.JobStoreServiceConnector;
import dk.dbc.dataio.filestore.service.connector.FileStoreServiceConnector;
import dk.dbc.dataio.harvester.types.HarvesterException;

public class HarvesterJobBuilderFactory {
    private final BinaryFileStore binaryFileStore;
    private final FileStoreServiceConnector fileStoreServiceConnector;
    private final JobStoreServiceConnector jobStoreServiceConnector;

    public HarvesterJobBuilderFactory(BinaryFileStore binaryFileStore,
        FileStoreServiceConnector fileStoreServiceConnector, JobStoreServiceConnector jobStoreServiceConnector) {
        this.binaryFileStore = binaryFileStore;
        this.fileStoreServiceConnector = fileStoreServiceConnector;
        this.jobStoreServiceConnector = jobStoreServiceConnector;
    }

    public HarvesterJobBuilder newHarvesterJobBuilder(JobSpecification jobSpecificationTemplate)
            throws NullPointerException, HarvesterException {
        return new HarvesterJobBuilder(binaryFileStore, fileStoreServiceConnector, jobStoreServiceConnector,
                jobSpecificationTemplate);
    }
}
