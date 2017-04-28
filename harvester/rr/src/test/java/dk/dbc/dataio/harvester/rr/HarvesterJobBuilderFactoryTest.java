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
import org.junit.Test;

import static org.mockito.Mockito.mock;

public class HarvesterJobBuilderFactoryTest {
    private static final String EMPTY = "";
    private final BinaryFileStore binaryFileStore = mock(BinaryFileStore.class);
    private final FileStoreServiceConnector fileStoreServiceConnector = mock(FileStoreServiceConnector.class);
    private final JobStoreServiceConnector jobStoreServiceConnector = mock(JobStoreServiceConnector.class);
    private final JobSpecification jobSpecificationTemplate = getJobSpecificationTemplate();

    @Test
    public void newHarvesterJobBuilder_binaryFileStoreArgIsNull_throws() throws HarvesterException {
        final HarvesterJobBuilderFactory harvesterJobBuilderFactory =
                new HarvesterJobBuilderFactory(null, fileStoreServiceConnector, jobStoreServiceConnector);
        try {
            harvesterJobBuilderFactory.newHarvesterJobBuilder(jobSpecificationTemplate);
        } catch (NullPointerException e) {
        }
    }

    @Test
    public void newHarvesterJobBuilder_fileStoreServiceConnectorArgIsNull_throws() throws HarvesterException {
        final HarvesterJobBuilderFactory harvesterJobBuilderFactory =
                new HarvesterJobBuilderFactory(binaryFileStore, null, jobStoreServiceConnector);
        try {
            harvesterJobBuilderFactory.newHarvesterJobBuilder(jobSpecificationTemplate);
        } catch (NullPointerException e) {
        }
    }

    @Test
    public void newHarvesterJobBuilder_jobStoreServiceConnectorArgIsNull_throws() throws HarvesterException {
        final HarvesterJobBuilderFactory harvesterJobBuilderFactory =
                new HarvesterJobBuilderFactory(binaryFileStore, fileStoreServiceConnector, null);
        try {
            harvesterJobBuilderFactory.newHarvesterJobBuilder(jobSpecificationTemplate);
        } catch (NullPointerException e) {
        }
    }

    @Test
    public void newHarvesterJobBuilder_jobSpecificationArgIsNull_throws() throws HarvesterException {
        final HarvesterJobBuilderFactory harvesterJobBuilderFactory =
                new HarvesterJobBuilderFactory(binaryFileStore, fileStoreServiceConnector, jobStoreServiceConnector);
        try {
            harvesterJobBuilderFactory.newHarvesterJobBuilder(null);
        } catch (NullPointerException e) {
        }
    }

    private JobSpecification getJobSpecificationTemplate() {
        return new JobSpecification()
                .withPackaging("packaging")
                .withFormat("format")
                .withCharset("utf8")
                .withDestination("destination")
                .withSubmitterId(222)
                .withMailForNotificationAboutVerification(EMPTY)
                .withMailForNotificationAboutProcessing(EMPTY)
                .withResultmailInitials(EMPTY)
                .withDataFile("datafile")
                .withType(JobSpecification.Type.TEST);
    }
}