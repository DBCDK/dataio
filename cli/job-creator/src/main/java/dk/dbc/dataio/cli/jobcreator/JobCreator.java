/*
 * DataIO - Data IO
 * Copyright (C) 2017 Dansk Bibliotekscenter a/s, Tempovej 7-11, DK-2750 Ballerup,
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

package dk.dbc.dataio.cli.jobcreator;

import dk.dbc.dataio.cli.jobcreator.arguments.ArgParseException;
import dk.dbc.dataio.cli.jobcreator.arguments.Arguments;
import dk.dbc.dataio.common.utils.flowstore.FlowStoreServiceConnector;
import dk.dbc.dataio.common.utils.flowstore.FlowStoreServiceConnectorException;
import dk.dbc.dataio.commons.types.FileStoreUrn;
import dk.dbc.dataio.commons.types.JobSpecification;
import dk.dbc.dataio.commons.types.Submitter;
import dk.dbc.dataio.commons.types.jndi.JndiConstants;
import dk.dbc.dataio.commons.utils.httpclient.HttpClient;
import dk.dbc.dataio.commons.utils.jobstore.JobStoreServiceConnector;
import dk.dbc.dataio.commons.utils.jobstore.JobStoreServiceConnectorException;
import dk.dbc.dataio.filestore.service.connector.FileStoreServiceConnector;
import dk.dbc.dataio.filestore.service.connector.FileStoreServiceConnectorException;
import dk.dbc.dataio.jobstore.types.JobInfoSnapshot;
import dk.dbc.dataio.jobstore.types.JobInputStream;
import dk.dbc.dataio.jobstore.types.criteria.JobListCriteria;
import dk.dbc.dataio.jobstore.types.criteria.ListFilter;
import dk.dbc.dataio.urlresolver.service.connector.UrlResolverServiceConnector;
import dk.dbc.dataio.urlresolver.service.connector.UrlResolverServiceConnectorException;

import javax.ws.rs.client.Client;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.util.List;
import java.util.Map;

public class JobCreator {
    public static void main(String[] args) {
        final JobCreator jobCreator = new JobCreator();
        jobCreator.run(args);
    }

    /**
     * Finds the job specification of a given job and creates a new job in
     * the target job store based on it
     *
     * @param args arguments, must include job id
     */
    private void run(String[] args) {
        Arguments arguments = new Arguments();
        try {
            arguments.parseArgs(args);
        } catch(ArgParseException e) {
            System.err.println(String.format("error parsing arguments: %s",
                e.toString()));
            System.exit(1);
        }

        Client client = HttpClient.newClient();
        try {
            Map<String, String> sourceEndpoints = getEndpoints(client,
                arguments.source, arguments.overriddenSourceEndpoints);
            Map<String, String> targetEndpoints = getEndpoints(client,
                arguments.target, arguments.overriddenTargetEndpoints);

            String jobStoreEndpoint = sourceEndpoints.get(
                JndiConstants.URL_RESOURCE_JOBSTORE_RS);
            JobSpecification specification = getJobSpecificationFromJobId(
                arguments.jobId, client, jobStoreEndpoint);
            specification.withAncestry(null);

            String sourceFileStoreEndpoint = sourceEndpoints.get(
                JndiConstants.URL_RESOURCE_FILESTORE_RS);
            String targetFileStoreEndpoint = targetEndpoints.get(
                JndiConstants.URL_RESOURCE_FILESTORE_RS);
            String newDataFileId = recreateDataFile(
                specification.getDataFile(), client, sourceFileStoreEndpoint,
                targetFileStoreEndpoint);
            specification.withDataFile(newDataFileId);

            String sourceFlowStoreEndpoint = sourceEndpoints.get(
                JndiConstants.FLOW_STORE_SERVICE_ENDPOINT_RESOURCE);
            String targetFlowStoreEndpoint = targetEndpoints.get(
                JndiConstants.FLOW_STORE_SERVICE_ENDPOINT_RESOURCE);
            FlowStoreServiceConnector sourceFlowStoreServiceConnector =
                new FlowStoreServiceConnector(client, sourceFlowStoreEndpoint);
            FlowStoreServiceConnector targetFlowStoreConnector =
                new FlowStoreServiceConnector(client, targetFlowStoreEndpoint);
            long submitter = specification.getSubmitterId();
            createSubmitterIfNeeded(submitter, sourceFlowStoreServiceConnector,
                targetFlowStoreConnector);

            JobInputStream jobInputStream = new JobInputStream(specification);
            String targetJobStoreEndpoint = targetEndpoints.get(
                JndiConstants.URL_RESOURCE_JOBSTORE_RS);
            JobStoreServiceConnector targetJobStore =
                new JobStoreServiceConnector(client, targetJobStoreEndpoint);
            targetJobStore.addJob(jobInputStream);
        } catch(JobCreatorException | UrlResolverServiceConnectorException |
                JobStoreServiceConnectorException e) {
            System.err.println(String.format("caught exception: %s",
                e.toString()));
            System.exit(1);
        }
    }

    private Map<String, String> getEndpoints(Client client, String hostUrl,
            Map<String, String> overriddenEndpoints)
            throws UrlResolverServiceConnectorException {
        UrlResolverServiceConnector urlResolverServiceConnector =
            new UrlResolverServiceConnector(client, hostUrl);
        Map<String, String> endpoints = urlResolverServiceConnector.getUrls();
        for(Map.Entry<String, String> entry : overriddenEndpoints.entrySet())
            endpoints.put(entry.getKey(), entry.getValue());
        return endpoints;
    }

    private JobSpecification getJobSpecificationFromJobId(long jobId,
            Client client, String jobStoreEndpoint) throws JobCreatorException {
        JobStoreServiceConnector jobStoreServiceConnector =
            new JobStoreServiceConnector(client, jobStoreEndpoint);
        JobListCriteria criteria = new JobListCriteria();
        criteria.where(new ListFilter<>(JobListCriteria.Field.JOB_ID, ListFilter.Op.EQUAL, jobId));
        try {
            List<JobInfoSnapshot> jobInfoSnapshots = jobStoreServiceConnector.listJobs(criteria);
            if(jobInfoSnapshots.size() > 1) {
                throw new JobCreatorException("error: more than one job found");
            }
            return jobInfoSnapshots.get(0).getSpecification();
        } catch(JobStoreServiceConnectorException e) {
            throw new JobCreatorException("error getting job specification", e);
        }
    }

    private String recreateDataFile(String dataFile, Client client,
            String sourceFileStoreEndpoint, String targetFileStoreEndpoint)
            throws JobCreatorException{
        try {
            FileStoreServiceConnector sourceFileStoreServiceConnector =
                new FileStoreServiceConnector(client, sourceFileStoreEndpoint);
            String fileId = new FileStoreUrn(dataFile).getFileId();
            InputStream is = sourceFileStoreServiceConnector.getFile(fileId);

            FileStoreServiceConnector targetFileStoreServiceConnector =
                new FileStoreServiceConnector(client, targetFileStoreEndpoint);
            String newFileId = targetFileStoreServiceConnector.addFile(is);

            return FileStoreUrn.create(newFileId).toString();
        } catch(URISyntaxException | FileStoreServiceConnectorException e) {
            throw new JobCreatorException(String.format(
                "error adding file to file store: %s", e.toString()), e);
        }
    }

    private void createSubmitterIfNeeded(long submitterNumber,
            FlowStoreServiceConnector sourceFlowStoreConnector,
            FlowStoreServiceConnector targetFlowStoreConnector)
            throws JobCreatorException {
        try {
            targetFlowStoreConnector.getSubmitterBySubmitterNumber(
                submitterNumber);
        } catch(FlowStoreServiceConnectorException e) {
            try {
                Submitter submitter = sourceFlowStoreConnector
                    .getSubmitterBySubmitterNumber(submitterNumber);
                targetFlowStoreConnector.createSubmitter(submitter.getContent());
            } catch(FlowStoreServiceConnectorException e2) {
                throw new JobCreatorException(String.format(
                    "error adding submitter: %s", e.toString()), e2);
            }
        }
    }
}
