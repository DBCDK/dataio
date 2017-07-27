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
import dk.dbc.dataio.commons.types.Flow;
import dk.dbc.dataio.commons.types.FlowBinder;
import dk.dbc.dataio.commons.types.FlowBinderContent;
import dk.dbc.dataio.commons.types.FlowComponent;
import dk.dbc.dataio.commons.types.FlowContent;
import dk.dbc.dataio.commons.types.JobSpecification;
import dk.dbc.dataio.commons.types.Sink;
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
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

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
            specification.withMailForNotificationAboutProcessing(
                arguments.mailAddressProcessing)
                .withMailForNotificationAboutVerification(
                arguments.mailAddressVerification);

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
            long submitterNumber = specification.getSubmitterId();
            JobCreatorInfo jobCreatorInfo = new JobCreatorInfo()
                .withJobSpecification(specification)
                .withSubmitterNumber(submitterNumber)
                .withTargetSinkName(arguments.targetSinkName)
                .withSourceFlowStoreConnector(sourceFlowStoreServiceConnector)
                .withTargetFlowStoreConnector(targetFlowStoreConnector);

            long submitterId = createSubmitterIfNeeded(jobCreatorInfo);
            jobCreatorInfo.withSubmitterId(submitterId);

            createFlowBinderIfNeeded(jobCreatorInfo);

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

    private long createSubmitterIfNeeded(JobCreatorInfo jobCreatorInfo)
            throws JobCreatorException {
        try {
            Submitter submitter = jobCreatorInfo.getTargetFlowStoreConnector()
                .getSubmitterBySubmitterNumber(
                jobCreatorInfo.getSubmitterNumber());
            return submitter.getId();
        } catch(FlowStoreServiceConnectorException e) {
            try {
                Submitter sourceSubmitter = jobCreatorInfo
                    .getSourceFlowStoreConnector()
                    .getSubmitterBySubmitterNumber(
                    jobCreatorInfo.getSubmitterNumber());
                Submitter targetSubmitter = jobCreatorInfo
                    .getTargetFlowStoreConnector()
                    .createSubmitter(sourceSubmitter.getContent());
                return targetSubmitter.getId();
            } catch(FlowStoreServiceConnectorException e2) {
                throw new JobCreatorException(String.format(
                    "error adding submitter: %s", e.toString()), e2);
            }
        }
    }

    private List<FlowComponent> createFlowComponents(
            List<FlowComponent> sourceComponents,
            FlowStoreServiceConnector targetFlowStoreConnector)
            throws FlowStoreServiceConnectorException {
        List<FlowComponent> targetComponents = new ArrayList<>();
        List<FlowComponent> existingTargetComponents =
            targetFlowStoreConnector.findAllFlowComponents();
        Set<String> componentNames = existingTargetComponents.stream().map(
            component -> component.getContent().getName())
            .collect(Collectors.toSet());
        for(FlowComponent component : sourceComponents) {
            if(componentNames.contains(component.getContent().getName()))
                continue;
            targetComponents.add(targetFlowStoreConnector.createFlowComponent(
                component.getContent()));
        }
        return targetComponents;
    }

    private Flow createFlow(Flow sourceFlow,
            List<FlowComponent> targetComponents,
            FlowStoreServiceConnector targetFlowStoreConnector)
            throws FlowStoreServiceConnectorException, JobCreatorException {
        final List<Flow> existingFlows = targetFlowStoreConnector.findAllFlows();
        final Set<String> flowNames = existingFlows.stream().map(
            flow -> flow.getContent().getName()).collect(Collectors.toSet());
        if(!flowNames.contains(sourceFlow.getContent().getName())) {
            FlowContent targetFlowContent = new FlowContent(
                sourceFlow.getContent().getName(), sourceFlow.getContent()
                .getDescription(), targetComponents, null);
            return targetFlowStoreConnector.createFlow(targetFlowContent);
        } else {
            List<Flow> targetFlow = existingFlows.stream().filter(
                flow -> flow.getContent().getName().equals(
                sourceFlow.getContent().getName())).limit(2)
                .collect(Collectors.toList());
            if(targetFlow.size() == 1) return targetFlow.get(0);
        }
        throw new JobCreatorException("error creating flow in target flow store");
    }

    private Sink getTargetSink(String name,
            FlowStoreServiceConnector targetFlowStoreConnector)
            throws JobCreatorException, FlowStoreServiceConnectorException{
        final List<Sink> existingSinks = targetFlowStoreConnector.findAllSinks();
        final Set<String> sinkNames = existingSinks.stream().map(
            sink -> sink.getContent().getName()).collect(Collectors.toSet());
        if(sinkNames.contains(name)) {
            List<Sink> targetSink = existingSinks.stream().filter(
                flow -> flow.getContent().getName().equals(
                name)).limit(2)
                .collect(Collectors.toList());
            if(targetSink.size() == 1) return targetSink.get(0);
        }
        throw new JobCreatorException(String.format(
            "cannot find sink %s in target flow store", name));
    }

    private void createFlowBinder(JobCreatorInfo jobCreatorInfo)
            throws FlowStoreServiceConnectorException, JobCreatorException {
        JobSpecification specification = jobCreatorInfo.getJobSpecification();
        FlowBinder flowBinder = jobCreatorInfo.getSourceFlowStoreConnector()
            .getFlowBinder(specification.getPackaging(),
            specification.getFormat(),
            specification.getCharset(),
            specification.getSubmitterId(),
            specification.getDestination());
        long flowId = flowBinder.getContent().getFlowId();
        Flow sourceFlow = jobCreatorInfo.getSourceFlowStoreConnector()
            .getFlow(flowId);

        List<FlowComponent> sourceComponents = sourceFlow.getContent()
                .getComponents();
        List<FlowComponent> targetComponents = createFlowComponents(
                sourceComponents, jobCreatorInfo.getTargetFlowStoreConnector());

        Flow targetFlow = createFlow(sourceFlow, targetComponents,
            jobCreatorInfo.getTargetFlowStoreConnector());
        Sink targetSink = getTargetSink(jobCreatorInfo.getTargetSinkName(),
            jobCreatorInfo.getTargetFlowStoreConnector());

        FlowBinderContent targetFlowBinder = new FlowBinderContent(
            flowBinder.getContent().getName(),
            flowBinder.getContent().getDescription(),
            specification.getPackaging(),
            specification.getFormat(),
            specification.getCharset(),
            specification.getDestination(),
            flowBinder.getContent().getPriority(),
            flowBinder.getContent().getRecordSplitter(),
            targetFlow.getId(),
            Collections.singletonList(jobCreatorInfo.getSubmitterId()),
            targetSink.getId(),
            flowBinder.getContent().getQueueProvider()
        );
        jobCreatorInfo.getTargetFlowStoreConnector().createFlowBinder(
            targetFlowBinder);
    }

    private void createFlowBinderIfNeeded(JobCreatorInfo jobCreatorInfo)
            throws JobCreatorException {
        try {
            JobSpecification specification = jobCreatorInfo
                .getJobSpecification();
            jobCreatorInfo.getTargetFlowStoreConnector().getFlowBinder(
                specification.getPackaging(),
                specification.getFormat(),
                specification.getCharset(),
                specification.getSubmitterId(),
                specification.getDestination());
        } catch(FlowStoreServiceConnectorException e) {
            try {
                createFlowBinder(jobCreatorInfo);
            } catch(FlowStoreServiceConnectorException e2) {
                throw new JobCreatorException(String.format(
                    "error creating flow binder: %s", e.toString()), e);
            }
        }
    }
}
