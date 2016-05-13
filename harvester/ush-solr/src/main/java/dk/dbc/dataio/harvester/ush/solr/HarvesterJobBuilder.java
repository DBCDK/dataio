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

import dk.dbc.dataio.bfs.api.BinaryFileStore;
import dk.dbc.dataio.commons.types.JobSpecification;
import dk.dbc.dataio.commons.utils.invariant.InvariantUtil;
import dk.dbc.dataio.commons.utils.jobstore.JobStoreServiceConnector;
import dk.dbc.dataio.filestore.service.connector.FileStoreServiceConnector;
import dk.dbc.dataio.harvester.types.HarvesterException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@SuppressWarnings("PMD") // TODO: 5/11/16 Remove suppression when ready
public class HarvesterJobBuilder implements AutoCloseable {
    private static final Logger LOGGER = LoggerFactory.getLogger(HarvesterJobBuilder.class);

    private final JobSpecification jobSpecificationTemplate;
    private final BinaryFileStore binaryFileStore;
    private final FileStoreServiceConnector fileStoreServiceConnector;
    private final JobStoreServiceConnector jobStoreServiceConnector;

    /**
     * Class constructor
     * @param binaryFileStore binaryFileStore implementation for tmp file writing
     * @param fileStoreServiceConnector file-store service connector for datafile uploads
     * @param jobStoreServiceConnector job-store service connector for job creation
     * @param jobSpecificationTemplate job specification template
     * @throws NullPointerException if given null-valued argument
     * @throws HarvesterException on failure to create harvester data file
     * backed by temporary binary file
     */
    public HarvesterJobBuilder(BinaryFileStore binaryFileStore, FileStoreServiceConnector fileStoreServiceConnector, JobStoreServiceConnector jobStoreServiceConnector, JobSpecification jobSpecificationTemplate)
            throws NullPointerException, HarvesterException {
        this.binaryFileStore = InvariantUtil.checkNotNullOrThrow(binaryFileStore, "binaryFileStore");
        this.fileStoreServiceConnector = InvariantUtil.checkNotNullOrThrow(fileStoreServiceConnector, "fileStoreServiceConnector");
        this.jobStoreServiceConnector = InvariantUtil.checkNotNullOrThrow(jobStoreServiceConnector, "jobStoreServiceConnector");
        this.jobSpecificationTemplate = InvariantUtil.checkNotNullOrThrow(jobSpecificationTemplate, "jobSpecificationTemplate");
        LOGGER.debug("No-op instance created");
    }

    @Override
    public void close() throws Exception {}
}
