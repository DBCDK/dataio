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

import dk.dbc.dataio.bfs.api.BinaryFileStore;
import dk.dbc.dataio.commons.types.FileStoreUrn;
import dk.dbc.dataio.commons.types.JobSpecification;
import dk.dbc.dataio.commons.utils.jobstore.JobStoreServiceConnector;
import dk.dbc.dataio.filestore.service.connector.FileStoreServiceConnector;
import dk.dbc.dataio.harvester.types.HarvesterException;
import dk.dbc.dataio.harvester.utils.harvesterjobbuilder.AbstractHarvesterJobBuilder;

/**
 * Class used to build dataIO job from harvested records
 */
public class HarvesterJobBuilder extends AbstractHarvesterJobBuilder {

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
    public HarvesterJobBuilder(BinaryFileStore binaryFileStore, FileStoreServiceConnector fileStoreServiceConnector,
                               JobStoreServiceConnector jobStoreServiceConnector, JobSpecification jobSpecificationTemplate)
            throws NullPointerException, HarvesterException {
        super(binaryFileStore, fileStoreServiceConnector, jobStoreServiceConnector, jobSpecificationTemplate);
    }

    /* Returns job specification for given file ID */
    @Override
    protected JobSpecification createJobSpecification(String fileId) {
        final FileStoreUrn fileStoreUrn = FileStoreUrn.create(fileId);
        return new JobSpecification()
                .withPackaging(jobSpecificationTemplate.getPackaging())
                .withFormat(jobSpecificationTemplate.getFormat())
                .withCharset(jobSpecificationTemplate.getCharset())
                .withDestination(jobSpecificationTemplate.getDestination())
                .withSubmitterId(jobSpecificationTemplate.getSubmitterId())
                .withMailForNotificationAboutVerification(
                        jobSpecificationTemplate.getMailForNotificationAboutVerification())
                .withMailForNotificationAboutProcessing(
                        jobSpecificationTemplate.getMailForNotificationAboutProcessing())
                .withResultmailInitials(
                        jobSpecificationTemplate.getResultmailInitials())
                .withDataFile(fileStoreUrn.toString())
                .withType(jobSpecificationTemplate.getType())
                .withAncestry(jobSpecificationTemplate.getAncestry());
    }
}
