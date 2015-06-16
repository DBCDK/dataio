package dk.dbc.dataio.gui.server;

import dk.dbc.dataio.commons.time.StopWatch;
import dk.dbc.dataio.commons.utils.httpclient.HttpClient;
import dk.dbc.dataio.commons.utils.jobstore.JobStoreServiceConnector;
import dk.dbc.dataio.commons.utils.jobstore.JobStoreServiceConnectorException;
import dk.dbc.dataio.commons.utils.jobstore.JobStoreServiceConnectorUnexpectedStatusCodeException;
import dk.dbc.dataio.commons.utils.service.ServiceUtil;
import dk.dbc.dataio.gui.client.exceptions.ProxyError;
import dk.dbc.dataio.gui.client.exceptions.ProxyException;
import dk.dbc.dataio.gui.client.exceptions.StatusCodeTranslator;
import dk.dbc.dataio.gui.client.model.ItemListCriteriaModel;
import dk.dbc.dataio.gui.client.model.ItemModel;
import dk.dbc.dataio.gui.client.model.JobListCriteriaModel;
import dk.dbc.dataio.gui.client.model.JobModel;
import dk.dbc.dataio.gui.client.proxies.JobStoreProxy;
import dk.dbc.dataio.gui.server.modelmappers.ItemModelMapper;
import dk.dbc.dataio.gui.server.modelmappers.JobModelMapper;
import dk.dbc.dataio.gui.server.modelmappers.criterias.ItemListCriteriaModelMapper;
import dk.dbc.dataio.gui.server.modelmappers.criterias.JobListCriteriaModelMapper;
import dk.dbc.dataio.jobstore.types.ItemInfoSnapshot;
import dk.dbc.dataio.jobstore.types.JobInfoSnapshot;
import dk.dbc.dataio.jobstore.types.State;
import org.glassfish.jersey.client.ClientConfig;
import org.glassfish.jersey.jackson.JacksonFeature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.naming.NamingException;
import javax.ws.rs.client.Client;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class JobStoreProxyImpl implements JobStoreProxy {
    private static final Logger log = LoggerFactory.getLogger(FlowStoreProxyImpl.class);
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
    public List<JobModel> listJobs(JobListCriteriaModel model) throws ProxyException {
        final List<JobInfoSnapshot> jobInfoSnapshotList;
        log.trace("JobStoreProxy: listJobs(\"{}\");", model.getSearchType());
        final StopWatch stopWatch = new StopWatch();
        try {
            jobInfoSnapshotList = jobStoreServiceConnector.listJobs(JobListCriteriaModelMapper.toJobListCriteria(model));
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
    public List<ItemModel> listItems(ItemListCriteriaModel model) throws ProxyException{
        List<ItemInfoSnapshot> itemInfoSnapshotList;
        List<ItemModel> itemModels = new ArrayList<ItemModel>();

        log.trace("JobStoreProxy: listItems(\"{}\", \"{}\", \"{}\", {}, {}, {});", model.getItemId(), model.getChunkId(), model.getJobId(), model.getItemSearchType(), model.getLimit(), model.getOffset());
        final StopWatch stopWatch = new StopWatch();
        try {
            switch (model.getItemSearchType()) {
                case FAILED:
                    itemInfoSnapshotList = jobStoreServiceConnector.listItems(ItemListCriteriaModelMapper.toFailedItemListCriteria(model));
                    itemModels = ItemModelMapper.toFailedItemsModel(itemInfoSnapshotList);
                    break;
                case IGNORED:
                    itemInfoSnapshotList = jobStoreServiceConnector.listItems(ItemListCriteriaModelMapper.toIgnoredItemListCriteria(model));
                    itemModels = ItemModelMapper.toIgnoredItemsModel(itemInfoSnapshotList);
                    break;
                case ALL:
                    itemInfoSnapshotList = jobStoreServiceConnector.listItems(ItemListCriteriaModelMapper.toItemListCriteriaAll(model));
                    itemModels = ItemModelMapper.toAllItemsModel(itemInfoSnapshotList);
                    break;
            }
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
        return itemModels;
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


    /**
     * Determines if a string is xml alike:
     *  if TRUE : Adds tabs and new lines to a xml string
     *  if FALSE: Returns the string without formatting
     * @param xmlString
     * @return formatted string if it should be displayed as xml, unformatted string if not
     */
    static String format(String xmlString) {
        String formattedString = xmlString;
        // Might be xml alike and should be displayed as xml even though the xml string starts with a number...
        // check for the xmlns attribute.
        if(xmlString.contains("xmlns")) {
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
