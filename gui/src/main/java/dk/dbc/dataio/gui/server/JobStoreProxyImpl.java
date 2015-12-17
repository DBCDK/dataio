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

package dk.dbc.dataio.gui.server;

import dk.dbc.dataio.commons.time.StopWatch;
import dk.dbc.dataio.commons.utils.httpclient.HttpClient;
import dk.dbc.dataio.commons.utils.jobstore.JobStoreServiceConnector;
import dk.dbc.dataio.commons.utils.jobstore.JobStoreServiceConnectorException;
import dk.dbc.dataio.commons.utils.jobstore.JobStoreServiceConnectorUnexpectedStatusCodeException;
import dk.dbc.dataio.commons.utils.service.ServiceUtil;
import dk.dbc.dataio.gui.client.exceptions.JavaScriptProjectFetcherException;
import dk.dbc.dataio.gui.client.exceptions.ProxyError;
import dk.dbc.dataio.gui.client.exceptions.ProxyException;
import dk.dbc.dataio.gui.client.exceptions.StatusCodeTranslator;
import dk.dbc.dataio.gui.client.model.ItemModel;
import dk.dbc.dataio.gui.client.model.JobModel;
import dk.dbc.dataio.gui.client.model.WorkflowNoteModel;
import dk.dbc.dataio.gui.client.proxies.JobStoreProxy;
import dk.dbc.dataio.gui.server.modelmappers.ItemModelMapper;
import dk.dbc.dataio.gui.server.modelmappers.JobModelMapper;
import dk.dbc.dataio.gui.server.modelmappers.WorkflowNoteModelMapper;
import dk.dbc.dataio.jobstore.types.ItemInfoSnapshot;
import dk.dbc.dataio.jobstore.types.JobInfoSnapshot;
import dk.dbc.dataio.jobstore.types.JobNotification;
import dk.dbc.dataio.jobstore.types.State;
import dk.dbc.dataio.jobstore.types.criteria.ItemListCriteria;
import dk.dbc.dataio.jobstore.types.criteria.JobListCriteria;
import dk.dbc.dataio.jobstore.types.criteria.ListOrderBy;
import org.glassfish.jersey.client.ClientConfig;
import org.glassfish.jersey.jackson.JacksonFeature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.naming.NamingException;
import javax.ws.rs.client.Client;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class JobStoreProxyImpl implements JobStoreProxy {
    private static final Logger log = LoggerFactory.getLogger(JobStoreProxyImpl.class);
    Client client;
    String endpoint;
    JobStoreServiceConnector jobStoreServiceConnector;

    public JobStoreProxyImpl() throws NamingException {
        final ClientConfig clientConfig = new ClientConfig().register(new JacksonFeature());
        client = HttpClient.newClient(clientConfig);
        endpoint = ServiceUtil.getJobStoreServiceEndpoint();
        log.info("JobStoreProxy: Using Endpoint {}", endpoint);
        jobStoreServiceConnector = new JobStoreServiceConnector(client, endpoint);
    }

    // This constructor is intended for test purpose only (new job store) with reference to dependency injection.
    public JobStoreProxyImpl(JobStoreServiceConnector jobStoreServiceConnector) throws NamingException {
        final ClientConfig clientConfig = new ClientConfig().register(new JacksonFeature());
        client = HttpClient.newClient(clientConfig);
        endpoint = ServiceUtil.getJobStoreServiceEndpoint();
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
            }
            else {
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
            }
            else {
                log.error("JobStoreProxy: countJobs - Unexpected Status Code Exception({})", StatusCodeTranslator.toProxyError(e.getStatusCode()), e);
                throw new ProxyException(StatusCodeTranslator.toProxyError(e.getStatusCode()), e);
            }
        } catch (JobStoreServiceConnectorException e) {
            log.error("JobStoreProxy: countJobs - Service Not Found Exception", e);
            throw new ProxyException(ProxyError.SERVICE_NOT_FOUND, e);
        } catch (IllegalArgumentException e) {
            log.error("JobStoreProxy: countJobs - Invalid Field Value Exception", e);
            throw new ProxyException(ProxyError.MODEL_MAPPER_INVALID_FIELD_VALUE, e);
        }
        finally {
            log.debug("JobStoreProxy: countJobs took {} milliseconds", stopWatch.getElapsedTime());
        }
        return jobCount;
    }

    @Override
    public List<ItemModel> listItems(ItemListCriteria.Field searchType, ItemListCriteria criteria) throws ProxyException{
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
            }
            else {
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
            }
            else {
                log.error("JobStoreProxy: countItems - Unexpected Status Code Exception", e);
                throw new ProxyException(StatusCodeTranslator.toProxyError(e.getStatusCode()), e);
            }
        } catch (JobStoreServiceConnectorException e) {
            log.error("JobStoreProxy: countItems - Service Not Found Exception", e);
            throw new ProxyException(ProxyError.SERVICE_NOT_FOUND, e);
        }
        finally {
            log.debug("JobStoreProxy: countItems took {} milliseconds", stopWatch.getElapsedTime());
        }
        return itemCount;
    }

    @Override
    public String getItemData (int jobId, int chunkId, short itemId, ItemModel.LifeCycle lifeCycle) throws ProxyException {
        final State.Phase phase;
        log.trace("JobStoreProxy: getChunkItem(\"{}\", \"{}\", \"{}\", {});", jobId, chunkId, itemId, lifeCycle);
        final StopWatch stopWatch = new StopWatch();
        try {
        switch (lifeCycle) {
            case PARTITIONING:
                phase = State.Phase.PARTITIONING;
                break;
            case PROCESSING:
                phase = State.Phase.PROCESSING;
                break;
            default:
                phase = State.Phase.DELIVERING;
                break;
            }
            return format(jobStoreServiceConnector.getItemData(jobId, chunkId, itemId, phase));

        } catch (JobStoreServiceConnectorUnexpectedStatusCodeException e) {
            if (e.getJobError() != null) {
                log.error("JobStoreProxy: getChunkItem - Unexpected Status Code Exception({}, {})", StatusCodeTranslator.toProxyError(e.getStatusCode()), e.getJobError().getDescription(), e);
                throw new ProxyException(StatusCodeTranslator.toProxyError(e.getStatusCode()), e.getJobError().getDescription());
            }
            else {
                log.error("JobStoreProxy: getChunkItem - Unexpected Status Code Exception", e);
                throw new ProxyException(StatusCodeTranslator.toProxyError(e.getStatusCode()), e);
            }
        } catch (JobStoreServiceConnectorException e) {
            log.error("JobStoreProxy: getChunkItem - Service Not Found Exception", e);
            throw new ProxyException(ProxyError.SERVICE_NOT_FOUND, e);
        } finally {
            log.debug("JobStoreProxy: getChunkItem took {} milliseconds", stopWatch.getElapsedTime());
        }
    }

    @Override
    public String getProcessedNextResult(int jobId, int chunkId, short itemId) throws ProxyException {
        log.trace("JobStoreProxy: getProcessedNextResult(\"{}\", \"{}\", \"{}\");", jobId, chunkId, itemId);
        final StopWatch stopWatch = new StopWatch();
        try {
            return format(jobStoreServiceConnector.getProcessedNextResult(jobId, chunkId, itemId));
        } catch (JobStoreServiceConnectorUnexpectedStatusCodeException e) {
            if (e.getJobError() != null) {
                log.error("JobStoreProxy: getProcessedNextResult - Unexpected Status Code Exception({}, {})", StatusCodeTranslator.toProxyError(e.getStatusCode()), e.getJobError().getDescription(), e);
                throw new ProxyException(StatusCodeTranslator.toProxyError(e.getStatusCode()), e.getJobError().getDescription());
            }
            else {
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
    public List<JobNotification> listJobNotificationsForJob(int jobId) throws ProxyException {
        final List<JobNotification> jobNotifications;
        log.trace("JobStoreProxy: listJobNotificationsForJob({})", jobId);
        final StopWatch stopWatch = new StopWatch();
        try {
            jobNotifications = jobStoreServiceConnector.listJobNotificationsForJob(jobId);
        } catch (JobStoreServiceConnectorUnexpectedStatusCodeException e) {
            if (e.getJobError() != null) {
                log.error("JobStoreProxy: listJobNotificationsForJob - Unexpected Status Code Exception({}, {})", StatusCodeTranslator.toProxyError(e.getStatusCode()), e.getJobError().getDescription(), e);
                throw new ProxyException(StatusCodeTranslator.toProxyError(e.getStatusCode()), e.getJobError().getDescription());
            }
            else {
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
    public JobModel addJob(JobModel jobModel) throws NullPointerException, ProxyException {
        final String callerMethodName = "addJob";
        JobInfoSnapshot jobInfoSnapshot = null;
        log.trace("JobStoreProxy: " + callerMethodName + "(\"{}\");", jobModel.getJobId());
        try {
            jobInfoSnapshot = jobStoreServiceConnector.addJob(JobModelMapper.toJobInputStream(jobModel));
        } catch(Exception genericException) {
            handleExceptions(genericException, callerMethodName);
        }

        return JobModelMapper.toModel(jobInfoSnapshot);
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
            }
            else {
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
        log.debug("JobStoreProxy: setWorkflowNote({}, {}, {})", jobId, chunkId, itemId);
        final StopWatch stopWatch = new StopWatch();
        try {
            itemInfoSnapshot = jobStoreServiceConnector.setWorkflowNote(WorkflowNoteModelMapper.toWorkflowNote(workflowNoteModel), jobId, chunkId, itemId);
            return ItemModelMapper.toFailedItemsModel(itemInfoSnapshot);
        } catch (JobStoreServiceConnectorUnexpectedStatusCodeException e) {
            if (e.getJobError() != null) {
                log.error("JobStoreProxy: setWorkflowNote - Unexpected Status Code Exception({}, {})", StatusCodeTranslator.toProxyError(e.getStatusCode()), e.getJobError().getDescription(), e);
                throw new ProxyException(StatusCodeTranslator.toProxyError(e.getStatusCode()), e.getJobError().getDescription());
            }
            else {
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
     * @param exception generic exception which in turn can be both Checked and Unchecked
     * @param callerMethodName calling method name for logging
     * @throws ProxyException GUI exception
     * @throws NullPointerException
     */
    private void handleExceptions(Exception exception, String callerMethodName) throws ProxyException, NullPointerException {

        if(exception instanceof JobStoreServiceConnectorUnexpectedStatusCodeException)  {
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
        } else if(exception instanceof NullPointerException){
            throw (NullPointerException) exception;
        } else {
            throw new ProxyException(ProxyError.ERROR_UNKNOWN, exception);
        }
    }

    /**
     * Determines if a string is xml alike:
     *  if TRUE : Adds tabs and new lines to a xml string
     *  if FALSE: Returns the string without formatting
     * @param xmlString the string to format
     * @return formatted string if it should be displayed as xml, unformatted string if not
     */
    static String format(String xmlString) {
        String formattedString = xmlString;
        // Might be xml alike and should be displayed as xml even though the xml string starts with a number...
        // check for the xmlns attribute.
        if(xmlString.contains("xmlns") || xmlString.contains("?xml")) {
            /* Remove new lines */
            final String LINE_BREAK = "\n";
            xmlString = xmlString.replaceAll(LINE_BREAK, "");
            StringBuffer prettyPrintXml = new StringBuffer();
            /* Group the xml tags */
            Pattern pattern = Pattern.compile("(<[^/][^>]+>)?([^<]*)(</[^>]+>)?(<[^/][^>]+/>)?");
            Matcher matcher = pattern.matcher(xmlString);
            int tabCount = 0;
            while (matcher.find()) {
                String str1 = null == matcher.group(1) || "null".equals(matcher.group()) ? "" : matcher.group(1);
                String str2 = null == matcher.group(2) || "null".equals(matcher.group()) ? "" : matcher.group(2);
                String str3 = null == matcher.group(3) || "null".equals(matcher.group()) ? "" : matcher.group(3);
                String str4 = null == matcher.group(4) || "null".equals(matcher.group()) ? "" : matcher.group(4);

                if (matcher.group() != null && !matcher.group().trim().equals("")) {
                    printTabs(tabCount, prettyPrintXml);
                    if (!str1.equals("") && str3.equals("")) {
                        ++tabCount;
                    }
                    if (str1.equals("") && !str3.equals("")) {
                        --tabCount;
                        prettyPrintXml.deleteCharAt(prettyPrintXml.length() - 1);
                    }
                    prettyPrintXml.append(str1);
                    prettyPrintXml.append(str2);
                    prettyPrintXml.append(str3);
                    if (!str4.equals("")) {
                        prettyPrintXml.append(LINE_BREAK);
                        printTabs(tabCount, prettyPrintXml);
                        prettyPrintXml.append(str4);
                    }
                    prettyPrintXml.append(LINE_BREAK);
                }
            }
            formattedString = prettyPrintXml.toString();
        }
        return formattedString;
    }

    private static void printTabs(int count, StringBuffer stringBuffer) {
        for (int i = 0; i < count; i++) {
            stringBuffer.append("\t");
        }
    }

    public void close() {
        HttpClient.closeClient(client);
    }

}
