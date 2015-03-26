package dk.dbc.dataio.gui.server;


import dk.dbc.dataio.commons.utils.httpclient.HttpClient;
import dk.dbc.dataio.commons.utils.jersey.jackson.Jackson2xFeature;
import dk.dbc.dataio.commons.utils.newjobstore.JobStoreServiceConnector;
import dk.dbc.dataio.commons.utils.newjobstore.JobStoreServiceConnectorException;
import dk.dbc.dataio.commons.utils.newjobstore.JobStoreServiceConnectorUnexpectedStatusCodeException;
import dk.dbc.dataio.commons.utils.service.ServiceUtil;
import dk.dbc.dataio.gui.client.exceptions.ProxyError;
import dk.dbc.dataio.gui.client.exceptions.ProxyErrorTranslator;
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

import javax.naming.NamingException;
import javax.ws.rs.client.Client;
import javax.ws.rs.core.Response;
import java.util.ArrayList;
import java.util.List;

public class JobStoreProxyImpl implements JobStoreProxy {
    Client client;
    String baseUrl;
    String endpoint;
    JobStoreServiceConnector jobStoreServiceConnector;

    public JobStoreProxyImpl() throws NamingException {
        final ClientConfig clientConfig = new ClientConfig().register(new Jackson2xFeature());
        client = HttpClient.newClient(clientConfig);
        baseUrl = ServiceUtil.getJobStoreServiceEndpoint();
        endpoint = ServiceUtil.getNewJobStoreServiceEndpoint();
        jobStoreServiceConnector = new JobStoreServiceConnector(client, endpoint);
    }

    // This constructor is intended for test purpose only (old job store) with reference to dependency injection.
    // Should be removed when the old job store is removed
    public JobStoreProxyImpl(dk.dbc.dataio.commons.utils.jobstore.JobStoreServiceConnector oldJobStoreServiceConnector) throws NamingException {
        final ClientConfig clientConfig = new ClientConfig().register(new Jackson2xFeature());
        client = HttpClient.newClient(clientConfig);
        baseUrl = ServiceUtil.getJobStoreServiceEndpoint();
    }

    // This constructor is intended for test purpose only (new job store) with reference to dependency injection.
    public JobStoreProxyImpl(JobStoreServiceConnector jobStoreServiceConnector) throws NamingException {
        final ClientConfig clientConfig = new ClientConfig().register(new Jackson2xFeature());
        client = HttpClient.newClient(clientConfig);
        endpoint = ServiceUtil.getNewJobStoreServiceEndpoint();
        this.jobStoreServiceConnector = jobStoreServiceConnector;
    }

    @Override
    public List<JobModel> listJobs(JobListCriteriaModel model) throws ProxyException {
        List<JobInfoSnapshot> jobInfoSnapshotList;
        try {
            jobInfoSnapshotList = jobStoreServiceConnector.listJobs(JobListCriteriaModelMapper.toJobListCriteria(model));
        } catch (JobStoreServiceConnectorUnexpectedStatusCodeException e) {
            if(e.getJobError() != null) {
                throw new ProxyException(ProxyErrorTranslator.toProxyError(e.getStatusCode()), e.getJobError().getDescription());
            }
            else {
                throw new ProxyException(ProxyErrorTranslator.toProxyError(e.getStatusCode()), e);
            }
        } catch (JobStoreServiceConnectorException e) {
            throw new ProxyException(ProxyError.SERVICE_NOT_FOUND, e);
        } catch (IllegalArgumentException e) {
            throw new ProxyException(ProxyError.MODEL_MAPPER_EMPTY_FIELDS, e);
        }
        return JobModelMapper.toModel(jobInfoSnapshotList);
    }

    @Override
    public List<ItemModel> listItems(ItemListCriteriaModel model) throws ProxyException{
        List<ItemInfoSnapshot> itemInfoSnapshotList;
        List<ItemModel> itemModels = new ArrayList<ItemModel>();

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
            if(e.getJobError() != null) {
                throw new ProxyException(ProxyErrorTranslator.toProxyError(e.getStatusCode()), e.getJobError().getDescription());
            }
            else {
                throw new ProxyException(ProxyErrorTranslator.toProxyError(e.getStatusCode()), e);
            }
        } catch (JobStoreServiceConnectorException e) {
            throw new ProxyException(ProxyError.SERVICE_NOT_FOUND, e);
        }
        return itemModels;
    }

    /*
     * private methods
     */

    public void close() {
        HttpClient.closeClient(client);
    }

    private void assertStatusCode(Response response, Response.Status expectedStatus) throws ProxyException {
        final Response.Status status = Response.Status.fromStatusCode(response.getStatus());
        if (status != expectedStatus) {
            final ProxyError errorCode;
            switch (status) {
                case BAD_REQUEST: errorCode = ProxyError.BAD_REQUEST;
                    break;
                case NOT_ACCEPTABLE: errorCode = ProxyError.NOT_ACCEPTABLE;
                    break;
                case PRECONDITION_FAILED: errorCode = ProxyError.ENTITY_NOT_FOUND;
                    break;
                default:
                    errorCode = ProxyError.INTERNAL_SERVER_ERROR;
            }
            throw new ProxyException(errorCode, response.readEntity(String.class));
        }
    }
}
