package dk.dbc.dataio.gui.server;

import dk.dbc.commons.addi.AddiReader;
import dk.dbc.commons.addi.AddiRecord;
import dk.dbc.commons.jsonb.JSONBException;
import dk.dbc.dataio.commons.time.StopWatch;
import dk.dbc.dataio.commons.types.ChunkItem;
import dk.dbc.dataio.commons.types.Diagnostic;
import dk.dbc.dataio.commons.utils.jobstore.JobStoreServiceConnector;
import dk.dbc.dataio.commons.utils.jobstore.JobStoreServiceConnectorException;
import dk.dbc.dataio.commons.utils.jobstore.JobStoreServiceConnectorUnexpectedStatusCodeException;
import dk.dbc.dataio.commons.utils.lang.PrettyPrint;
import dk.dbc.dataio.commons.utils.service.ServiceUtil;
import dk.dbc.dataio.gui.client.exceptions.JavaScriptProjectFetcherException;
import dk.dbc.dataio.gui.client.exceptions.ProxyError;
import dk.dbc.dataio.gui.client.exceptions.ProxyException;
import dk.dbc.dataio.gui.client.exceptions.StatusCodeTranslator;
import dk.dbc.dataio.gui.client.model.ItemModel;
import dk.dbc.dataio.gui.client.model.JobModel;
import dk.dbc.dataio.gui.client.model.WorkflowNoteModel;
import dk.dbc.dataio.gui.client.pages.sink.status.SinkStatusTable;
import dk.dbc.dataio.gui.client.proxies.JobStoreProxy;
import dk.dbc.dataio.gui.server.modelmappers.ItemModelMapper;
import dk.dbc.dataio.gui.server.modelmappers.JobModelMapper;
import dk.dbc.dataio.gui.server.modelmappers.SinkStatusModelMapper;
import dk.dbc.dataio.gui.server.modelmappers.WorkflowNoteModelMapper;
import dk.dbc.dataio.jobstore.types.ItemInfoSnapshot;
import dk.dbc.dataio.jobstore.types.JobInfoSnapshot;
import dk.dbc.dataio.jobstore.types.Notification;
import dk.dbc.dataio.jobstore.types.State;
import dk.dbc.dataio.jobstore.types.criteria.ItemListCriteria;
import dk.dbc.dataio.jobstore.types.criteria.JobListCriteria;
import dk.dbc.dataio.jobstore.types.criteria.ListFilter;
import dk.dbc.dataio.jobstore.types.criteria.ListOrderBy;
import dk.dbc.httpclient.HttpClient;
import org.glassfish.jersey.client.ClientConfig;
import org.glassfish.jersey.jackson.JacksonFeature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.client.Client;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class JobStoreProxyImpl implements JobStoreProxy {
    private static final Logger log = LoggerFactory.getLogger(JobStoreProxyImpl.class);
    Client client;
    String endpoint;
    JobStoreServiceConnector jobStoreServiceConnector;

    public JobStoreProxyImpl() {
        final ClientConfig clientConfig = new ClientConfig().register(new JacksonFeature());
        client = HttpClient.newClient(clientConfig);
        endpoint = ServiceUtil.getStringValueFromSystemEnvironmentOrProperty("JOBSTORE_URL");
        log.info("JobStoreProxy: Using Endpoint {}", endpoint);
        jobStoreServiceConnector = new JobStoreServiceConnector(client, endpoint);
    }

    // This constructor is intended for test purpose only (new job store) with reference to dependency injection.
    public JobStoreProxyImpl(JobStoreServiceConnector jobStoreServiceConnector) {
        client = jobStoreServiceConnector.getClient();
        endpoint = jobStoreServiceConnector.getBaseUrl();
        log.info("JobStoreProxy: Using Endpoint {}", endpoint);
        this.jobStoreServiceConnector = jobStoreServiceConnector;
    }

    @Override
    public List<JobModel> listJobs(JobListCriteria criteria) throws ProxyException {
        final List<JobInfoSnapshot> jobInfoSnapshotList;
        log.trace("JobStoreProxy: listJobs()");
        final StopWatch stopWatch = new StopWatch();
        try {
            criteria.orderBy(new ListOrderBy<>(JobListCriteria.Field.JOB_ID, ListOrderBy.Sort.DESC));
            jobInfoSnapshotList = jobStoreServiceConnector.listJobs(criteria);
        } catch (JobStoreServiceConnectorUnexpectedStatusCodeException e) {
            if (e.getJobError() != null) {
                log.error("JobStoreProxy: listJobs - Unexpected Status Code Exception({}, {})", StatusCodeTranslator.toProxyError(e.getStatusCode()), e.getJobError().getDescription(), e);
                throw new ProxyException(StatusCodeTranslator.toProxyError(e.getStatusCode()), e.getJobError().getDescription());
            } else {
                log.error("JobStoreProxy: listJobs - Unexpected Status Code Exception({})", StatusCodeTranslator.toProxyError(e.getStatusCode()), e);
                throw new ProxyException(StatusCodeTranslator.toProxyError(e.getStatusCode()), e);
            }
        } catch (JobStoreServiceConnectorException e) {
            log.error("JobStoreProxy: listJobs - Service Not Found Exception", e);
            throw new ProxyException(ProxyError.SERVICE_NOT_FOUND, e);
        } catch (IllegalArgumentException e) {
            log.error("JobStoreProxy: listJobs - Invalid Field Value Exception", e);
            throw new ProxyException(ProxyError.MODEL_MAPPER_INVALID_FIELD_VALUE, e);
        } finally {
            log.debug("JobStoreProxy: listJobs took {} milliseconds", stopWatch.getElapsedTime());
        }
        return JobModelMapper.toModel(jobInfoSnapshotList);
    }

    public JobModel fetchEarliestActiveJob(int sinkId) throws ProxyException {
        final List<JobInfoSnapshot> jobInfoSnapshots;
        log.trace("JobStoreProxy: fetchEarliestActiveJob()");
        final StopWatch stopWatch = new StopWatch();
        try {
            JobListCriteria criteria = new JobListCriteria().where(new ListFilter<>(JobListCriteria.Field.SINK_ID, ListFilter.Op.EQUAL, String.valueOf(sinkId)));
            criteria.and(new JobListCriteria().where(new ListFilter<>(JobListCriteria.Field.TIME_OF_COMPLETION, ListFilter.Op.IS_NULL)));
            criteria.orderBy(new ListOrderBy<>(JobListCriteria.Field.TIME_OF_CREATION, ListOrderBy.Sort.ASC));
            jobInfoSnapshots = jobStoreServiceConnector.listJobs(criteria);
        } catch (JobStoreServiceConnectorUnexpectedStatusCodeException e) {
            if (e.getJobError() != null) {
                log.error("JobStoreProxy: fetchEarliestActiveJob - Unexpected Status Code Exception({}, {})", StatusCodeTranslator.toProxyError(e.getStatusCode()), e.getJobError().getDescription(), e);
                throw new ProxyException(StatusCodeTranslator.toProxyError(e.getStatusCode()), e.getJobError().getDescription());
            } else {
                log.error("JobStoreProxy: fetchEarliestActiveJob - Unexpected Status Code Exception({})", StatusCodeTranslator.toProxyError(e.getStatusCode()), e);
                throw new ProxyException(StatusCodeTranslator.toProxyError(e.getStatusCode()), e);
            }
        } catch (JobStoreServiceConnectorException e) {
            log.error("JobStoreProxy: fetchEarliestActiveJob - Service Not Found Exception", e);
            throw new ProxyException(ProxyError.SERVICE_NOT_FOUND, e);
        } catch (IllegalArgumentException e) {
            log.error("JobStoreProxy: fetchEarliestActiveJob - Invalid Field Value Exception", e);
            throw new ProxyException(ProxyError.MODEL_MAPPER_INVALID_FIELD_VALUE, e);
        } finally {
            log.debug("JobStoreProxy: fetchEarliestActiveJob took {} milliseconds", stopWatch.getElapsedTime());
        }
        return jobInfoSnapshots == null || jobInfoSnapshots.isEmpty() ? null : JobModelMapper.toModel(jobInfoSnapshots.get(0));
    }

    @Override
    public long countJobs(JobListCriteria criteria) throws ProxyException {
        final long jobCount;
        final StopWatch stopWatch = new StopWatch();
        try {
            jobCount = jobStoreServiceConnector.countJobs(criteria);
        } catch (JobStoreServiceConnectorUnexpectedStatusCodeException e) {
            if (e.getJobError() != null) {
                log.error("JobStoreProxy: countJobs - Unexpected Status Code Exception({}, {})", StatusCodeTranslator.toProxyError(e.getStatusCode()), e.getJobError().getDescription(), e);
                throw new ProxyException(StatusCodeTranslator.toProxyError(e.getStatusCode()), e.getJobError().getDescription());
            } else {
                log.error("JobStoreProxy: countJobs - Unexpected Status Code Exception({})", StatusCodeTranslator.toProxyError(e.getStatusCode()), e);
                throw new ProxyException(StatusCodeTranslator.toProxyError(e.getStatusCode()), e);
            }
        } catch (JobStoreServiceConnectorException e) {
            log.error("JobStoreProxy: countJobs - Service Not Found Exception", e);
            throw new ProxyException(ProxyError.SERVICE_NOT_FOUND, e);
        } catch (IllegalArgumentException e) {
            log.error("JobStoreProxy: countJobs - Invalid Field Value Exception", e);
            throw new ProxyException(ProxyError.MODEL_MAPPER_INVALID_FIELD_VALUE, e);
        } finally {
            log.debug("JobStoreProxy: countJobs took {} milliseconds", stopWatch.getElapsedTime());
        }
        return jobCount;
    }

    @Override
    public List<ItemModel> listItems(ItemListCriteria.Field searchType, ItemListCriteria criteria) throws ProxyException {
        List<ItemInfoSnapshot> itemInfoSnapshotList;

        log.trace("JobStoreProxy: listItems(\"{}\");", searchType);
        final StopWatch stopWatch = new StopWatch();
        try {
            criteria.orderBy(new ListOrderBy<>(ItemListCriteria.Field.CHUNK_ID, ListOrderBy.Sort.ASC));
            criteria.orderBy(new ListOrderBy<>(ItemListCriteria.Field.ITEM_ID, ListOrderBy.Sort.ASC));
            itemInfoSnapshotList = jobStoreServiceConnector.listItems(criteria);
            return toPredefinedItemModel(searchType, itemInfoSnapshotList);
        } catch (JobStoreServiceConnectorUnexpectedStatusCodeException e) {
            if (e.getJobError() != null) {
                log.error("JobStoreProxy: listItems - Unexpected Status Code Exception({}, {})", StatusCodeTranslator.toProxyError(e.getStatusCode()), e.getJobError().getDescription(), e);
                throw new ProxyException(StatusCodeTranslator.toProxyError(e.getStatusCode()), e.getJobError().getDescription());
            } else {
                log.error("JobStoreProxy: listItems - Unexpected Status Code Exception", e);
                throw new ProxyException(StatusCodeTranslator.toProxyError(e.getStatusCode()), e);
            }
        } catch (JobStoreServiceConnectorException e) {
            log.error("JobStoreProxy: listItems - Service Not Found Exception", e);
            throw new ProxyException(ProxyError.SERVICE_NOT_FOUND, e);
        } finally {
            log.debug("JobStoreProxy: listItems took {} milliseconds", stopWatch.getElapsedTime());
        }
    }

    @Override
    public long countItems(ItemListCriteria criteria) throws ProxyException {
        final long itemCount;
        log.trace("JobStoreProxy: countItems();");
        final StopWatch stopWatch = new StopWatch();

        try {
            itemCount = jobStoreServiceConnector.countItems(criteria);
        } catch (JobStoreServiceConnectorUnexpectedStatusCodeException e) {
            if (e.getJobError() != null) {
                log.error("JobStoreProxy: countItems - Unexpected Status Code Exception({}, {})", StatusCodeTranslator.toProxyError(e.getStatusCode()), e.getJobError().getDescription(), e);
                throw new ProxyException(StatusCodeTranslator.toProxyError(e.getStatusCode()), e.getJobError().getDescription());
            } else {
                log.error("JobStoreProxy: countItems - Unexpected Status Code Exception", e);
                throw new ProxyException(StatusCodeTranslator.toProxyError(e.getStatusCode()), e);
            }
        } catch (JobStoreServiceConnectorException e) {
            log.error("JobStoreProxy: countItems - Service Not Found Exception", e);
            throw new ProxyException(ProxyError.SERVICE_NOT_FOUND, e);
        } finally {
            log.debug("JobStoreProxy: countItems took {} milliseconds", stopWatch.getElapsedTime());
        }
        return itemCount;
    }

    @Override
    public String getItemData(ItemModel itemModel, ItemModel.LifeCycle lifeCycle) throws ProxyException {
        log.trace("JobStoreProxy: getItemData(\"{}\", {});", itemModel, lifeCycle);
        final StopWatch stopWatch = new StopWatch();
        try {
            final State.Phase phase = getPhase(lifeCycle);
            final ChunkItem chunkItem = jobStoreServiceConnector.getChunkItem(
                    Integer.parseInt(itemModel.getJobId()),
                    Integer.parseInt(itemModel.getChunkId()),
                    Short.parseShort(itemModel.getItemId()),
                    phase);
            return prettyPrintChunkItem(chunkItem, phase);
        } catch (JobStoreServiceConnectorUnexpectedStatusCodeException e) {
            if (e.getJobError() != null) {
                log.error("JobStoreProxy: getItemData - Unexpected Status Code Exception({}, {})",
                        StatusCodeTranslator.toProxyError(e.getStatusCode()), e.getJobError().getDescription(), e);
                throw new ProxyException(StatusCodeTranslator.toProxyError(e.getStatusCode()), e.getJobError().getDescription());
            } else {
                log.error("JobStoreProxy: getItemData - Unexpected Status Code Exception", e);
                throw new ProxyException(StatusCodeTranslator.toProxyError(e.getStatusCode()), e);
            }
        } catch (JobStoreServiceConnectorException | JSONBException | IOException e) {
            log.error("JobStoreProxy: getItemData - Service Not Found Exception", e);
            throw new ProxyException(ProxyError.SERVICE_NOT_FOUND, e);
        } finally {
            log.debug("JobStoreProxy: getItemData took {} milliseconds", stopWatch.getElapsedTime());
        }
    }

    private String prettyPrintChunkItem(ChunkItem chunkItem, State.Phase phase) throws IOException, JSONBException {
        String itemDataString = null;
        if (chunkItem.getType() != null && chunkItem.getType().stream().anyMatch(type -> type == ChunkItem.Type.ADDI)) {
            final AddiReader addiReader = new AddiReader(new ByteArrayInputStream(chunkItem.getData()));
            if (addiReader.hasNext()) {
                AddiRecord addiRecord = addiReader.next();
                final String metaData = PrettyPrint.asJson(addiRecord.getMetaData(), chunkItem.getEncoding());
                final String contentData = PrettyPrint.asXml(addiRecord.getContentData(), chunkItem.getEncoding());
                itemDataString = PrettyPrint.combinePrintElements(metaData, contentData);
            }
        }
        if (itemDataString == null) {
            itemDataString = PrettyPrint.asXml(chunkItem.getData(), chunkItem.getEncoding());
        }
        // For the remaining phases the diagnostic content is typically
        // included as part of the chunk item data. For the sake of
        // consistency we should probably refactor so that error
        // messages in the future are only contained in diagnostics.
        if (phase == State.Phase.PARTITIONING && chunkItem.getDiagnostics() != null) {
            for (Diagnostic diagnostic : chunkItem.getDiagnostics()) {
                itemDataString = PrettyPrint.combinePrintElements(
                        itemDataString, prettyPrintDiagnostic(diagnostic));
            }
        }
        return itemDataString;
    }

    private String prettyPrintDiagnostic(Diagnostic diagnostic) {
        String diagnosticString = "Diagnostic: " + diagnostic.getMessage();
        if (diagnostic.getStacktrace() != null) {
            diagnosticString += "\n" + diagnostic.getStacktrace();
        }
        return diagnosticString;
    }

    @Override
    public String getProcessedNextResult(int jobId, int chunkId, short itemId) throws ProxyException {
        log.trace("JobStoreProxy: getProcessedNextResult(\"{}\", \"{}\", \"{}\");", jobId, chunkId, itemId);
        final StopWatch stopWatch = new StopWatch();
        try {
            ChunkItem chunkItem = jobStoreServiceConnector.getProcessedNextResult(jobId, chunkId, itemId);
            return PrettyPrint.asXml(chunkItem.getData(), chunkItem.getEncoding());
        } catch (JobStoreServiceConnectorUnexpectedStatusCodeException e) {
            if (e.getJobError() != null) {
                log.error("JobStoreProxy: getProcessedNextResult - Unexpected Status Code Exception({}, {})", StatusCodeTranslator.toProxyError(e.getStatusCode()), e.getJobError().getDescription(), e);
                throw new ProxyException(StatusCodeTranslator.toProxyError(e.getStatusCode()), e.getJobError().getDescription());
            } else {
                log.error("JobStoreProxy: getProcessedNextResult - Unexpected Status Code Exception", e);
                throw new ProxyException(StatusCodeTranslator.toProxyError(e.getStatusCode()), e);
            }
        } catch (JobStoreServiceConnectorException e) {
            log.error("JobStoreProxy: getProcessedNextResult - Service Not Found Exception", e);
            throw new ProxyException(ProxyError.SERVICE_NOT_FOUND, e);
        } finally {
            log.debug("JobStoreProxy: getProcessedNextResult took {} milliseconds", stopWatch.getElapsedTime());
        }
    }

    @Override
    public List<Notification> listJobNotificationsForJob(int jobId) throws ProxyException {
        final List<Notification> jobNotifications;
        log.trace("JobStoreProxy: listJobNotificationsForJob({})", jobId);
        final StopWatch stopWatch = new StopWatch();
        try {
            jobNotifications = jobStoreServiceConnector.listJobNotificationsForJob(jobId);
        } catch (JobStoreServiceConnectorUnexpectedStatusCodeException e) {
            if (e.getJobError() != null) {
                log.error("JobStoreProxy: listJobNotificationsForJob - Unexpected Status Code Exception({}, {})", StatusCodeTranslator.toProxyError(e.getStatusCode()), e.getJobError().getDescription(), e);
                throw new ProxyException(StatusCodeTranslator.toProxyError(e.getStatusCode()), e.getJobError().getDescription());
            } else {
                log.error("JobStoreProxy: listJobNotificationsForJob - Unexpected Status Code Exception({})", StatusCodeTranslator.toProxyError(e.getStatusCode()), e);
                throw new ProxyException(StatusCodeTranslator.toProxyError(e.getStatusCode()), e);
            }
        } catch (JobStoreServiceConnectorException e) {
            log.error("JobStoreProxy: listJobNotificationsForJob - Service Not Found Exception", e);
            throw new ProxyException(ProxyError.SERVICE_NOT_FOUND, e);
        } catch (IllegalArgumentException e) {
            log.error("JobStoreProxy: listJobNotificationsForJob - Invalid Field Value Exception", e);
            throw new ProxyException(ProxyError.MODEL_MAPPER_INVALID_FIELD_VALUE, e);
        } finally {
            log.debug("JobStoreProxy: listJobNotificationsForJob took {} milliseconds", stopWatch.getElapsedTime());
        }
        return jobNotifications;
    }

    @Override
    public JobModel reSubmitJob(JobModel jobModel) throws NullPointerException, ProxyException {
        final String callerMethodName = "reSubmitJob";
        JobInfoSnapshot jobInfoSnapshot = null;
        log.trace("JobStoreProxy: " + callerMethodName + "(\"{}\");", jobModel.getJobId());
        try {
            jobModel.withPreviousJobIdAncestry(Integer.parseInt(jobModel.getJobId()));  // Remember the job id for the previous run
            jobInfoSnapshot = jobStoreServiceConnector.addJob(JobModelMapper.toJobInputStream(jobModel));
        } catch (Exception genericException) {
            handleExceptions(genericException, callerMethodName);
        }
        return JobModelMapper.toModel(jobInfoSnapshot);
    }

    @Override
    public JobModel resendJob(JobModel jobModel) throws ProxyException {
        final String callerMethodName = "resendJob";
        JobInfoSnapshot jobInfoSnapshot = null;
        log.trace("JobStoreProxy: " + callerMethodName + "(\"{}\");", jobModel.getJobId());
        try {
            jobInfoSnapshot = jobStoreServiceConnector.resendJob(Integer.parseInt(jobModel.getJobId()));
        } catch (Exception genericException) {
            handleExceptions(genericException, callerMethodName);
        }
        return JobModelMapper.toModel(jobInfoSnapshot);
    }

    @Override
    public List<JobModel> reSubmitJobs(List<JobModel> jobModels) throws NullPointerException, ProxyException {
        final String callerMethodName = "reSubmitJobs";
        List<JobInfoSnapshot> jobInfoSnapshots = new ArrayList<>();
        log.trace("JobStoreProxy: " + callerMethodName + "(\"{}\");", getJobModelIdList(jobModels));
        try {
            for (JobModel jobModel : jobModels) {
                jobModel.withPreviousJobIdAncestry(Integer.parseInt(jobModel.getJobId()));  // Remember the job id for the previous run
                jobInfoSnapshots.add(jobStoreServiceConnector.addJob(JobModelMapper.toJobInputStream(jobModel)));
            }
        } catch (Exception genericException) {
            handleExceptions(genericException, callerMethodName);
        }
        return JobModelMapper.toModel(jobInfoSnapshots);
    }

    @Override
    public JobModel abortJob(JobModel jobModel) throws ProxyException {
        final String callerMethodName = "abortJob";
        JobInfoSnapshot jobInfoSnapshot = null;
        log.trace("JobStoreProxy: " + callerMethodName + "(\"{}\");", jobModel.getJobId());
        try {
            jobInfoSnapshot = jobStoreServiceConnector.abortJob(Integer.parseInt(jobModel.getJobId()));
        } catch (Exception genericException) {
            handleExceptions(genericException, callerMethodName);
        }
        return JobModelMapper.toModel(jobInfoSnapshot);
    }

    @Override
    public List<Notification> listInvalidTransfileNotifications() throws ProxyException {
        final String callerMethodName = "listInvalidTransfileNotifications";
        final List<Notification> notifications;
        log.trace("JobStoreProxy: " + callerMethodName + "()");
        final StopWatch stopWatch = new StopWatch();
        try {
            notifications = jobStoreServiceConnector.listInvalidTransfileNotifications();
        } catch (JobStoreServiceConnectorUnexpectedStatusCodeException e) {
            if (e.getJobError() != null) {
                log.error("JobStoreProxy: " + callerMethodName + " - Unexpected Status Code Exception({}, {})", StatusCodeTranslator.toProxyError(e.getStatusCode()), e.getJobError().getDescription(), e);
                throw new ProxyException(StatusCodeTranslator.toProxyError(e.getStatusCode()), e.getJobError().getDescription());
            } else {
                log.error("JobStoreProxy: " + callerMethodName + " - Unexpected Status Code Exception({})", StatusCodeTranslator.toProxyError(e.getStatusCode()), e);
                throw new ProxyException(StatusCodeTranslator.toProxyError(e.getStatusCode()), e);
            }
        } catch (JobStoreServiceConnectorException e) {
            log.error("JobStoreProxy: " + callerMethodName + " - Service Not Found Exception", e);
            throw new ProxyException(ProxyError.SERVICE_NOT_FOUND, e);
        } finally {
            log.debug("JobStoreProxy: " + callerMethodName + " took {} milliseconds", stopWatch.getElapsedTime());
        }
        return notifications;
    }

    @Override
    public JobModel setWorkflowNote(WorkflowNoteModel workflowNoteModel, int jobId) throws ProxyException {
        final JobInfoSnapshot jobInfoSnapshot;
        log.trace("JobStoreProxy: setWorkflowNote({})", jobId);
        final StopWatch stopWatch = new StopWatch();
        try {
            jobInfoSnapshot = jobStoreServiceConnector.setWorkflowNote(WorkflowNoteModelMapper.toWorkflowNote(workflowNoteModel), jobId);
        } catch (JobStoreServiceConnectorUnexpectedStatusCodeException e) {
            if (e.getJobError() != null) {
                log.error("JobStoreProxy: setWorkflowNote - Unexpected Status Code Exception({}, {})", StatusCodeTranslator.toProxyError(e.getStatusCode()), e.getJobError().getDescription(), e);
                throw new ProxyException(StatusCodeTranslator.toProxyError(e.getStatusCode()), e.getJobError().getDescription());
            } else {
                log.error("JobStoreProxy: setWorkflowNote - Unexpected Status Code Exception({})", StatusCodeTranslator.toProxyError(e.getStatusCode()), e);
                throw new ProxyException(StatusCodeTranslator.toProxyError(e.getStatusCode()), e);
            }
        } catch (JobStoreServiceConnectorException e) {
            log.error("JobStoreProxy: setWorkflowNote - Service Not Found Exception", e);
            throw new ProxyException(ProxyError.SERVICE_NOT_FOUND, e);
        } catch (IllegalArgumentException e) {
            log.error("JobStoreProxy: setWorkflowNote - Invalid Field Value Exception", e);
            throw new ProxyException(ProxyError.MODEL_MAPPER_INVALID_FIELD_VALUE, e);
        } finally {
            log.debug("JobStoreProxy: setWorkflowNote took {} milliseconds", stopWatch.getElapsedTime());
        }
        return JobModelMapper.toModel(jobInfoSnapshot);
    }

    @Override
    public ItemModel setWorkflowNote(WorkflowNoteModel workflowNoteModel, int jobId, int chunkId, short itemId) throws ProxyException {
        final ItemInfoSnapshot itemInfoSnapshot;
        log.debug("JobStoreProxy: setWorkflowNote({}, {}, {}, {})", jobId, chunkId, itemId, workflowNoteModel.isProcessed());
        final StopWatch stopWatch = new StopWatch();
        try {
            itemInfoSnapshot = jobStoreServiceConnector.setWorkflowNote(WorkflowNoteModelMapper.toWorkflowNote(workflowNoteModel), jobId, chunkId, itemId);
            return ItemModelMapper.toFailedItemsModel(itemInfoSnapshot);
        } catch (JobStoreServiceConnectorUnexpectedStatusCodeException e) {
            if (e.getJobError() != null) {
                log.error("JobStoreProxy: setWorkflowNote - Unexpected Status Code Exception({}, {})", StatusCodeTranslator.toProxyError(e.getStatusCode()), e.getJobError().getDescription(), e);
                throw new ProxyException(StatusCodeTranslator.toProxyError(e.getStatusCode()), e.getJobError().getDescription());
            } else {
                log.error("JobStoreProxy: setWorkflowNote - Unexpected Status Code Exception({})", StatusCodeTranslator.toProxyError(e.getStatusCode()), e);
                throw new ProxyException(StatusCodeTranslator.toProxyError(e.getStatusCode()), e);
            }
        } catch (JobStoreServiceConnectorException e) {
            log.error("JobStoreProxy: setWorkflowNote - Service Not Found Exception", e);
            throw new ProxyException(ProxyError.SERVICE_NOT_FOUND, e);
        } catch (IllegalArgumentException e) {
            log.error("JobStoreProxy: setWorkflowNote - Invalid Field Value Exception", e);
            throw new ProxyException(ProxyError.MODEL_MAPPER_INVALID_FIELD_VALUE, e);
        } finally {
            log.debug("JobStoreProxy: setWorkflowNote took {} milliseconds", stopWatch.getElapsedTime());
        }
    }

    @Override
    public List<SinkStatusTable.SinkStatusModel> getSinkStatusModels() throws ProxyException {
        log.debug("JobStoreProxy: getSinkStatusList()");
        final StopWatch stopWatch = new StopWatch();
        try {
            return SinkStatusModelMapper.toModel(jobStoreServiceConnector.getSinkStatusList());
        } catch (JobStoreServiceConnectorUnexpectedStatusCodeException e) {

            log.error("JobStoreProxy: getSinkStatusList - Unexpected Status Code Exception({})", StatusCodeTranslator.toProxyError(e.getStatusCode()), e);
            throw new ProxyException(StatusCodeTranslator.toProxyError(e.getStatusCode()), e);
        } catch (JobStoreServiceConnectorException e) {
            log.error("JobStoreProxy: getSinkStatusList - Service Not Found Exception", e);
            throw new ProxyException(ProxyError.SERVICE_NOT_FOUND, e);
        } finally {
            log.debug("JobStoreProxy: getSinkStatusList took {} milliseconds", stopWatch.getElapsedTime());
        }
    }

    @Override
    public void createJobRerun(int jobId, boolean failedItemsOnly) throws ProxyException {
        final String callerMethodName = "createJobRerun";
        log.trace("JobStoreProxy: " + callerMethodName + "({}, {})", jobId, failedItemsOnly);
        try {
            jobStoreServiceConnector.createJobRerun(jobId, failedItemsOnly);
        } catch (JobStoreServiceConnectorUnexpectedStatusCodeException e) {
            if (e.getJobError() != null) {
                log.error("JobStoreProxy: createJobRerun - Unexpected Status Code Exception({}, {})", StatusCodeTranslator.toProxyError(e.getStatusCode()), e.getJobError().getDescription(), e);
                throw new ProxyException(StatusCodeTranslator.toProxyError(e.getJobError().getCode()));
            } else {
                handleExceptions(e, callerMethodName);
            }
        } catch (Exception genericException) {
            handleExceptions(genericException, callerMethodName);
        }
    }

    private State.Phase getPhase(ItemModel.LifeCycle lifeCycle) {
        switch (lifeCycle) {
            case PARTITIONING:
                return State.Phase.PARTITIONING;
            case PROCESSING:
                return State.Phase.PROCESSING;
            default:
                return State.Phase.DELIVERING;
        }
    }

    private List<ItemModel> toPredefinedItemModel(ItemListCriteria.Field searchType, List<ItemInfoSnapshot> itemInfoSnapshotList) {
        switch (searchType) {
            case STATE_FAILED:
                return ItemModelMapper.toFailedItemsModel(itemInfoSnapshotList);
            case STATE_IGNORED:
                return ItemModelMapper.toIgnoredItemsModel(itemInfoSnapshotList);
            default:
                return ItemModelMapper.toAllItemsModel(itemInfoSnapshotList);
        }
    }

    /**
     * Handle exceptions thrown by the JobStoreServiceConnector and wrap them in ProxyExceptions
     *
     * @param exception        generic exception which in turn can be both Checked and Unchecked
     * @param callerMethodName calling method name for logging
     * @throws ProxyException       GUI exception
     * @throws NullPointerException Null pointer exception
     */
    private void handleExceptions(Exception exception, String callerMethodName) throws ProxyException, NullPointerException {

        if (exception instanceof JobStoreServiceConnectorUnexpectedStatusCodeException) {
            JobStoreServiceConnectorUnexpectedStatusCodeException jsscusce = (JobStoreServiceConnectorUnexpectedStatusCodeException) exception;
            log.error("JobStoreProxy: " + callerMethodName + " - Unexpected Status Code Exception({})", StatusCodeTranslator.toProxyError(jsscusce.getStatusCode()), jsscusce);
            throw new ProxyException(StatusCodeTranslator.toProxyError(jsscusce.getStatusCode()), jsscusce.getMessage());
        } else if (exception instanceof JobStoreServiceConnectorException) {
            JobStoreServiceConnectorException fssce = (JobStoreServiceConnectorException) exception;
            log.error("JobStoreProxy: " + callerMethodName + " - Service Not Found", fssce);
            throw new ProxyException(ProxyError.SERVICE_NOT_FOUND, fssce);
        } else if (exception instanceof IllegalArgumentException) {
            IllegalArgumentException iae = (IllegalArgumentException) exception;
            log.error("JobStoreProxy: " + callerMethodName + " - Invalid Field Value Exception", iae);
            throw new ProxyException(ProxyError.MODEL_MAPPER_INVALID_FIELD_VALUE, iae);
        } else if (exception instanceof JavaScriptProjectFetcherException) {
            JavaScriptProjectFetcherException jspfe = (JavaScriptProjectFetcherException) exception;
            log.error("JobStoreProxy: " + callerMethodName + " - Subversion Lookup Failed Exception", jspfe);
            throw new ProxyException(ProxyError.SUBVERSION_LOOKUP_FAILED, jspfe);
        } else if (exception instanceof NullPointerException) {
            throw (NullPointerException) exception;
        } else {
            throw new ProxyException(ProxyError.ERROR_UNKNOWN, exception);
        }
    }

    public void close() {
        HttpClient.closeClient(client);
    }

    private String getJobModelIdList(List<JobModel> jobModels) {
        String jobIdList = "";
        for (JobModel jobModel : jobModels) {
            jobIdList += jobIdList.isEmpty() ? jobModel.getJobId() : ", " + jobModel.getJobId();
        }
        return jobIdList;
    }


}
