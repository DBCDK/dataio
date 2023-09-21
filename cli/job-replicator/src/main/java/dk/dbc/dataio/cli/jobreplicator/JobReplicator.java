package dk.dbc.dataio.cli.jobreplicator;

import dk.dbc.dataio.cli.jobreplicator.arguments.ArgParseException;
import dk.dbc.dataio.cli.jobreplicator.arguments.Arguments;
import dk.dbc.dataio.common.utils.flowstore.FlowStoreServiceConnector;
import dk.dbc.dataio.common.utils.flowstore.FlowStoreServiceConnectorException;
import dk.dbc.dataio.commons.types.FileStoreUrn;
import dk.dbc.dataio.commons.types.Flow;
import dk.dbc.dataio.commons.types.FlowBinder;
import dk.dbc.dataio.commons.types.FlowBinderContent;
import dk.dbc.dataio.commons.types.FlowComponent;
import dk.dbc.dataio.commons.types.FlowComponentView;
import dk.dbc.dataio.commons.types.FlowContent;
import dk.dbc.dataio.commons.types.FlowView;
import dk.dbc.dataio.commons.types.JobSpecification;
import dk.dbc.dataio.commons.types.Sink;
import dk.dbc.dataio.commons.types.Submitter;
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
import dk.dbc.httpclient.HttpClient;
import jakarta.ws.rs.client.Client;

import java.io.InputStream;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public class JobReplicator {
    public static void main(String[] args) {
        final JobReplicator jobReplicator = new JobReplicator();
        jobReplicator.run(args);
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
        } catch (ArgParseException e) {
            System.err.printf("error parsing arguments: %s%n", e);
            System.exit(1);
        }

        Client client = HttpClient.newClient();
        try {
            Map<String, String> sourceEndpoints = getEndpoints(client,
                    arguments.source, arguments.overriddenSourceEndpoints);
            Map<String, String> targetEndpoints = getEndpoints(client,
                    arguments.target, arguments.overriddenTargetEndpoints);

            String jobStoreEndpoint = sourceEndpoints.get("JOBSTORE_URL");
            JobSpecification specification = getJobSpecificationFromJobId(
                    arguments.jobId, client, jobStoreEndpoint);
            specification.withAncestry(null);
            specification.withMailForNotificationAboutProcessing(
                            arguments.mailAddressProcessing)
                    .withMailForNotificationAboutVerification(
                            arguments.mailAddressVerification);

            String sourceFlowStoreEndpoint = sourceEndpoints.get("FLOWSTORE_URL");
            String targetFlowStoreEndpoint = targetEndpoints.get("FLOWSTORE_URL");
            FlowStoreServiceConnector sourceFlowStoreServiceConnector =
                    new FlowStoreServiceConnector(client, sourceFlowStoreEndpoint);
            FlowStoreServiceConnector targetFlowStoreConnector =
                    new FlowStoreServiceConnector(client, targetFlowStoreEndpoint);
            long submitterNumber = specification.getSubmitterId();
            JobReplicatorInfo jobReplicatorInfo = new JobReplicatorInfo()
                    .withJobSpecification(specification)
                    .withSubmitterNumber(submitterNumber)
                    .withTargetSinkName(arguments.targetSinkName)
                    .withSourceFlowStoreConnector(sourceFlowStoreServiceConnector)
                    .withTargetFlowStoreConnector(targetFlowStoreConnector);

            long submitterId = createSubmitterIfNeeded(jobReplicatorInfo);
            jobReplicatorInfo.withSubmitterId(submitterId);

            createFlowBinderIfNeeded(jobReplicatorInfo);

            String newDataFileId = recreateDataFile(
                    specification.getDataFile(), client, sourceEndpoints,
                    targetEndpoints);
            specification.withDataFile(newDataFileId);

            JobInputStream jobInputStream = new JobInputStream(specification);
            String targetJobStoreEndpoint = targetEndpoints.get("JOBSTORE_URL");
            JobStoreServiceConnector targetJobStore =
                    new JobStoreServiceConnector(client, targetJobStoreEndpoint);
            JobInfoSnapshot jobInfoSnapshot = targetJobStore.addJob(
                    jobInputStream);
            System.out.printf("added job %d%n", jobInfoSnapshot.getJobId());
        } catch (JobReplicatorException | UrlResolverServiceConnectorException |
                 JobStoreServiceConnectorException e) {
            System.err.printf("caught exception: %s%n", e);
            System.exit(1);
        }
    }

    private Map<String, String> getEndpoints(Client client, String hostUrl,
                                             Map<String, String> overriddenEndpoints)
            throws UrlResolverServiceConnectorException {
        UrlResolverServiceConnector urlResolverServiceConnector =
                new UrlResolverServiceConnector(client, hostUrl);
        Map<String, String> endpoints = urlResolverServiceConnector.getUrls();
        endpoints.putAll(overriddenEndpoints);
        return endpoints;
    }

    private JobSpecification getJobSpecificationFromJobId(long jobId,
                                                          Client client, String jobStoreEndpoint) throws JobReplicatorException {
        JobStoreServiceConnector jobStoreServiceConnector =
                new JobStoreServiceConnector(client, jobStoreEndpoint);
        JobListCriteria criteria = new JobListCriteria();
        criteria.where(new ListFilter<>(JobListCriteria.Field.JOB_ID, ListFilter.Op.EQUAL, jobId));
        try {
            List<JobInfoSnapshot> jobInfoSnapshots = jobStoreServiceConnector.listJobs(criteria);
            if (jobInfoSnapshots.size() > 1) {
                throw new JobReplicatorException("error: more than one job found");
            }
            return jobInfoSnapshots.get(0).getSpecification();
        } catch (JobStoreServiceConnectorException e) {
            throw new JobReplicatorException("error getting job specification", e);
        }
    }

    private String recreateDataFile(String dataFile, Client client,
                                    Map<String, String> sourceEndpoints,
                                    Map<String, String> targetEndpoints)
            throws JobReplicatorException {
        try {
            String sourceFileStoreEndpoint = sourceEndpoints.get("FILESTORE_URL");
            String targetFileStoreEndpoint = targetEndpoints.get("FILESTORE_URL");
            FileStoreServiceConnector sourceFileStoreServiceConnector =
                    new FileStoreServiceConnector(client, sourceFileStoreEndpoint);
            String fileId = new FileStoreUrn(dataFile).getFileId();
            InputStream is = sourceFileStoreServiceConnector.getFile(fileId);

            FileStoreServiceConnector targetFileStoreServiceConnector =
                    new FileStoreServiceConnector(client, targetFileStoreEndpoint);
            String newFileId = targetFileStoreServiceConnector.addFile(is);

            return FileStoreUrn.create(newFileId).toString();
        } catch (URISyntaxException | FileStoreServiceConnectorException e) {
            throw new JobReplicatorException(String.format("error adding file to file store: %s", e), e);
        }
    }

    private long createSubmitterIfNeeded(JobReplicatorInfo jobReplicatorInfo)
            throws JobReplicatorException {
        try {
            Submitter submitter = jobReplicatorInfo.getTargetFlowStoreConnector()
                    .getSubmitterBySubmitterNumber(
                            jobReplicatorInfo.getSubmitterNumber());
            return submitter.getId();
        } catch (FlowStoreServiceConnectorException e) {
            try {
                Submitter sourceSubmitter = jobReplicatorInfo
                        .getSourceFlowStoreConnector()
                        .getSubmitterBySubmitterNumber(
                                jobReplicatorInfo.getSubmitterNumber());
                Submitter targetSubmitter = jobReplicatorInfo
                        .getTargetFlowStoreConnector()
                        .createSubmitter(sourceSubmitter.getContent());
                return targetSubmitter.getId();
            } catch (FlowStoreServiceConnectorException e2) {
                throw new JobReplicatorException(String.format("error adding submitter: %s", e), e2);
            }
        }
    }

    private List<FlowComponent> createFlowComponents(
            List<FlowComponent> sourceComponents,
            FlowStoreServiceConnector targetFlowStoreConnector)
            throws FlowStoreServiceConnectorException {
        final List<FlowComponent> targetComponents = new ArrayList<>();
        final List<FlowComponentView> existingTargetComponents =
                targetFlowStoreConnector.findAllFlowComponents();
        final Set<String> componentNames = existingTargetComponents.stream()
                .map(FlowComponentView::getName)
                .collect(Collectors.toSet());
        for (FlowComponent component : sourceComponents) {
            if (componentNames.contains(component.getContent().getName())) {
                continue;
            }
            targetComponents.add(targetFlowStoreConnector
                    .createFlowComponent(component.getContent()));
        }
        return targetComponents;
    }

    private Flow createFlow(Flow sourceFlow,
                            List<FlowComponent> targetComponents,
                            FlowStoreServiceConnector targetFlowStoreConnector)
            throws FlowStoreServiceConnectorException, JobReplicatorException {
        final List<FlowView> existingFlows = targetFlowStoreConnector.findAllFlows();
        final Set<String> flowNames = existingFlows.stream()
                .map(FlowView::getName)
                .collect(Collectors.toSet());
        if (!flowNames.contains(sourceFlow.getContent().getName())) {
            FlowContent targetFlowContent = new FlowContent(
                    sourceFlow.getContent().getName(), sourceFlow.getContent()
                    .getDescription(), targetComponents, null);
            return targetFlowStoreConnector.createFlow(targetFlowContent);
        } else {
            List<FlowView> targetFlow = existingFlows.stream()
                    .filter(flowView -> flowView.getName().equals(sourceFlow.getContent().getName()))
                    .limit(2)
                    .collect(Collectors.toList());
            if (targetFlow.size() == 1) {
                return targetFlowStoreConnector.findFlowByName(targetFlow.get(0).getName());
            }
        }
        throw new JobReplicatorException("error creating flow in target flow store");
    }

    private Sink getTargetSink(String name,
                               FlowStoreServiceConnector targetFlowStoreConnector)
            throws JobReplicatorException, FlowStoreServiceConnectorException {
        final List<Sink> existingSinks = targetFlowStoreConnector.findAllSinks();
        final Set<String> sinkNames = existingSinks.stream().map(
                sink -> sink.getContent().getName()).collect(Collectors.toSet());
        if (sinkNames.contains(name)) {
            List<Sink> targetSink = existingSinks.stream().filter(
                            flow -> flow.getContent().getName().equals(
                                    name)).limit(2)
                    .collect(Collectors.toList());
            if (targetSink.size() == 1) return targetSink.get(0);
        }
        throw new JobReplicatorException(String.format(
                "cannot find sink %s in target flow store", name));
    }

    private void createFlowBinderIfNeeded(JobReplicatorInfo jobReplicatorInfo)
            throws JobReplicatorException {
        try {
            FlowBinder flowBinder = checkFlowBinder(jobReplicatorInfo);
            System.out.printf("using flowbinder %s%n", flowBinder.getContent().getName());
            if (jobReplicatorInfo.getTargetSinkName() != null) {
                System.err.println("warning: flow binder exists so " +
                        "target-sink-name argument will be ignored");
            }
        } catch (FlowStoreServiceConnectorException e) {
            if (jobReplicatorInfo.getTargetSinkName() == null) {
                throw new JobReplicatorException("cannot create flow binder " +
                        "without argument target-sink-name");
            }
            FlowBinder flowBinder = createFlowBinder(jobReplicatorInfo);
            System.out.printf("creating flowbinder %s%n", flowBinder.getContent().getName());
        }
    }

    private FlowBinder createFlowBinder(JobReplicatorInfo jobReplicatorInfo)
            throws JobReplicatorException {
        try {
            JobSpecification specification = jobReplicatorInfo.getJobSpecification();
            FlowBinder flowBinder = jobReplicatorInfo.getSourceFlowStoreConnector()
                    .getFlowBinder(specification.getPackaging(),
                            specification.getFormat(),
                            specification.getCharset(),
                            specification.getSubmitterId(),
                            specification.getDestination());
            long flowId = flowBinder.getContent().getFlowId();
            Flow sourceFlow = jobReplicatorInfo.getSourceFlowStoreConnector()
                    .getFlow(flowId);

            List<FlowComponent> sourceComponents = sourceFlow.getContent()
                    .getComponents();
            List<FlowComponent> targetComponents = createFlowComponents(
                    sourceComponents, jobReplicatorInfo.getTargetFlowStoreConnector());

            Flow targetFlow = createFlow(sourceFlow, targetComponents,
                    jobReplicatorInfo.getTargetFlowStoreConnector());
            Sink targetSink = getTargetSink(jobReplicatorInfo.getTargetSinkName(),
                    jobReplicatorInfo.getTargetFlowStoreConnector());

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
                    Collections.singletonList(jobReplicatorInfo.getSubmitterId()),
                    targetSink.getId(),
                    flowBinder.getContent().getQueueProvider()
            );
            return jobReplicatorInfo.getTargetFlowStoreConnector().createFlowBinder(
                    targetFlowBinder);
        } catch (FlowStoreServiceConnectorException e) {
            throw new JobReplicatorException(String.format("error creating flow binder: %s", e), e);
        }
    }

    private FlowBinder checkFlowBinder(JobReplicatorInfo jobReplicatorInfo)
            throws FlowStoreServiceConnectorException {
        JobSpecification specification = jobReplicatorInfo
                .getJobSpecification();
        return jobReplicatorInfo.getTargetFlowStoreConnector()
                .getFlowBinder(
                        specification.getPackaging(),
                        specification.getFormat(),
                        specification.getCharset(),
                        specification.getSubmitterId(),
                        specification.getDestination());
    }
}
