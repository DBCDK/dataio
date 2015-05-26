package dk.dbc.dataio.gui.server;


import dk.dbc.dataio.commons.time.StopWatch;
import dk.dbc.dataio.commons.utils.httpclient.HttpClient;
import dk.dbc.dataio.commons.utils.jersey.jackson.Jackson2xFeature;
import dk.dbc.dataio.commons.utils.jobstore.JobStoreServiceConnector;
import dk.dbc.dataio.commons.utils.jobstore.JobStoreServiceConnectorException;
import dk.dbc.dataio.commons.utils.jobstore.JobStoreServiceConnectorUnexpectedStatusCodeException;
import dk.dbc.dataio.commons.utils.service.ServiceUtil;
import dk.dbc.dataio.gui.client.exceptions.ProxyError;
import dk.dbc.dataio.gui.client.exceptions.StatusCodeTranslator;
import dk.dbc.dataio.gui.client.exceptions.ProxyException;
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
import org.glassfish.jersey.client.ClientConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.naming.NamingException;
import javax.ws.rs.client.Client;
import java.util.ArrayList;
import java.util.List;

public class JobStoreProxyImpl implements JobStoreProxy {
    private static final Logger log = LoggerFactory.getLogger(FlowStoreProxyImpl.class);
    Client client;
    String endpoint;
    JobStoreServiceConnector jobStoreServiceConnector;

    public JobStoreProxyImpl() throws NamingException {
        final ClientConfig clientConfig = new ClientConfig().register(new Jackson2xFeature());
        client = HttpClient.newClient(clientConfig);
        endpoint = ServiceUtil.getNewJobStoreServiceEndpoint();
        log.info("JobStoreProxy: Using Endpoint {}", endpoint);
        jobStoreServiceConnector = new JobStoreServiceConnector(client, endpoint);
    }

    // This constructor is intended for test purpose only (new job store) with reference to dependency injection.
    public JobStoreProxyImpl(JobStoreServiceConnector jobStoreServiceConnector) throws NamingException {
        final ClientConfig clientConfig = new ClientConfig().register(new Jackson2xFeature());
        client = HttpClient.newClient(clientConfig);
        endpoint = ServiceUtil.getNewJobStoreServiceEndpoint();
        log.info("JobStoreProxy: Using Endpoint {}", endpoint);
        this.jobStoreServiceConnector = jobStoreServiceConnector;
    }

    @Override
    public List<JobModel> listJobs(JobListCriteriaModel model) throws ProxyException {
        List<JobInfoSnapshot> jobInfoSnapshotList;
        log.trace("JobStoreProxy: listJobs(\"{}\");", model.getSearchType());
        final StopWatch stopWatch = new StopWatch();
        try {
            jobInfoSnapshotList = jobStoreServiceConnector.listJobs(JobListCriteriaModelMapper.toJobListCriteria(model));
        } catch (JobStoreServiceConnectorUnexpectedStatusCodeException e) {
            if (e.getJobError() != null) {
                log.error("JobStoreProxy: listJobs - Unexpcted Status Code Exception({}, {})", StatusCodeTranslator.toProxyError(e.getStatusCode()), e.getJobError().getDescription(), e);
                throw new ProxyException(StatusCodeTranslator.toProxyError(e.getStatusCode()), e.getJobError().getDescription());
            }
            else {
                log.error("JobStoreProxy: listJobs - Unexpcted Status Code Exception({})", StatusCodeTranslator.toProxyError(e.getStatusCode()), e);
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
        List<JobModel> result = JobModelMapper.toModel(jobInfoSnapshotList);
        return result;
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

    /*
     * private methods
     */

    public void close() {
        HttpClient.closeClient(client);
    }

}
