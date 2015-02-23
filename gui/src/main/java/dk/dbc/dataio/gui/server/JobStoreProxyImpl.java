package dk.dbc.dataio.gui.server;


import dk.dbc.dataio.commons.types.JobCompletionState;
import dk.dbc.dataio.commons.types.JobInfo;
import dk.dbc.dataio.commons.types.rest.JobStoreServiceConstants;
import dk.dbc.dataio.commons.utils.httpclient.HttpClient;
import dk.dbc.dataio.commons.utils.jersey.jackson.Jackson2xFeature;
import dk.dbc.dataio.commons.utils.jobstore.JobStoreServiceConnectorException;
import dk.dbc.dataio.commons.utils.newjobstore.JobStoreServiceConnectorUnexpectedStatusCodeException;
import dk.dbc.dataio.commons.utils.service.ServiceUtil;
import dk.dbc.dataio.gui.client.exceptions.ProxyError;
import dk.dbc.dataio.gui.client.exceptions.ProxyErrorTranslator;
import dk.dbc.dataio.gui.client.exceptions.ProxyException;
import dk.dbc.dataio.gui.client.model.JobListCriteriaModel;
import dk.dbc.dataio.gui.client.model.JobModel;
import dk.dbc.dataio.gui.client.model.JobModelOld;
import dk.dbc.dataio.gui.client.proxies.JobStoreProxy;
import dk.dbc.dataio.gui.server.ModelMappers.JobListCriteriaModelMapper;
import dk.dbc.dataio.gui.server.ModelMappers.JobModelMapper;
import dk.dbc.dataio.gui.server.ModelMappers.JobModelMapperJob;
import dk.dbc.dataio.jobstore.types.JobInfoSnapshot;
import org.glassfish.jersey.client.ClientConfig;

import javax.naming.NamingException;
import javax.servlet.ServletException;
import javax.ws.rs.client.Client;
import javax.ws.rs.core.GenericType;
import javax.ws.rs.core.Response;
import java.util.ArrayList;
import java.util.List;

public class JobStoreProxyImpl implements JobStoreProxy {
    Client client;
    String baseUrl;
    String endpoint;
    dk.dbc.dataio.commons.utils.jobstore.JobStoreServiceConnector oldJobStoreServiceConnector;
    dk.dbc.dataio.commons.utils.newjobstore.JobStoreServiceConnector jobStoreServiceConnector;

    public JobStoreProxyImpl() throws NamingException {
        final ClientConfig clientConfig = new ClientConfig().register(new Jackson2xFeature());
        client = HttpClient.newClient(clientConfig);
        baseUrl = ServiceUtil.getJobStoreServiceEndpoint();
        endpoint = ServiceUtil.getNewJobStoreServiceEndpoint();
        oldJobStoreServiceConnector = new dk.dbc.dataio.commons.utils.jobstore.JobStoreServiceConnector(client, baseUrl);
        jobStoreServiceConnector = new dk.dbc.dataio.commons.utils.newjobstore.JobStoreServiceConnector(client, endpoint);
    }

    // This constructor is intended for test purpose only (old job store) with reference to dependency injection.
    // Should be removed when the old job store is removed
    public JobStoreProxyImpl(dk.dbc.dataio.commons.utils.jobstore.JobStoreServiceConnector oldJobStoreServiceConnector) throws NamingException {
        final ClientConfig clientConfig = new ClientConfig().register(new Jackson2xFeature());
        client = HttpClient.newClient(clientConfig);
        baseUrl = ServiceUtil.getJobStoreServiceEndpoint();
        this.oldJobStoreServiceConnector = oldJobStoreServiceConnector;
    }

    // This constructor is intended for test purpose only (new job store) with reference to dependency injection.
    public JobStoreProxyImpl(dk.dbc.dataio.commons.utils.newjobstore.JobStoreServiceConnector jobStoreServiceConnector) throws NamingException {
        final ClientConfig clientConfig = new ClientConfig().register(new Jackson2xFeature());
        client = HttpClient.newClient(clientConfig);
        endpoint = ServiceUtil.getNewJobStoreServiceEndpoint();
        this.jobStoreServiceConnector = jobStoreServiceConnector;
    }

    @Override
    public String getJobStoreFilesystemUrl() throws ProxyException {
        try {
            return ServletUtil.getJobStoreFilesystemUrl();
        } catch (ServletException e) {
            throw new ProxyException(ProxyError.SERVICE_NOT_FOUND, e);
        }
    }

    @Override
    public List<JobInfo> findAllJobs() throws ProxyException {
        final Response response;
        final List<JobInfo> result;
        try {
            response = HttpClient.doGet(client, ServletUtil.getJobStoreServiceEndpoint(), JobStoreServiceConstants.JOB_COLLECTION);
        } catch (ServletException e) {
            throw new ProxyException(ProxyError.SERVICE_NOT_FOUND, e);
        }
        try {
            assertStatusCode(response, Response.Status.OK);
            result = response.readEntity(new GenericType<List<JobInfo>>() { });
        } finally {
            response.close();
        }
        return result;
    }

    @Override
    public List<JobModelOld> findAllJobsNew() throws ProxyException {
        final Response response;
        final List<JobModelOld> jobModels = new ArrayList<JobModelOld>();
        final List<JobInfo> jobInfos;
        try {
            response = HttpClient.doGet(client, ServletUtil.getJobStoreServiceEndpoint(), JobStoreServiceConstants.JOB_COLLECTION);
        } catch (ServletException e) {
            throw new ProxyException(ProxyError.SERVICE_NOT_FOUND, e);
        }
        try {
            assertStatusCode(response, Response.Status.OK);
            jobInfos = response.readEntity(new GenericType<List<JobInfo>>() { });
            for (JobInfo jobInfo: jobInfos) {
                jobModels.add(JobModelMapperJob.toModel(jobInfo));
            }
        } finally {
            response.close();
        }
        return jobModels;
    }

    @Override
    public JobCompletionState getJobCompletionState(long jobId) throws ProxyException {
        final JobCompletionState jobCompletionState;
        try {
            jobCompletionState = oldJobStoreServiceConnector.getJobCompletionState(jobId);
        } catch (JobStoreServiceConnectorException e) {
            throw new ProxyException(ProxyError.SERVICE_NOT_FOUND, e);
        }
        return jobCompletionState;
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
        } catch (dk.dbc.dataio.commons.utils.newjobstore.JobStoreServiceConnectorException e) {
            throw new ProxyException(ProxyError.SERVICE_NOT_FOUND, e);
        } catch (IllegalArgumentException e) {
            throw new ProxyException(ProxyError.MODEL_MAPPER_EMPTY_FIELDS, e);
        }
        return JobModelMapper.toModel(jobInfoSnapshotList);
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
