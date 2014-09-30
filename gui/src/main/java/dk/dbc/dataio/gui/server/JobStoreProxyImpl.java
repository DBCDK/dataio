package dk.dbc.dataio.gui.server;


import dk.dbc.dataio.commons.types.JobCompletionState;
import dk.dbc.dataio.commons.types.JobInfo;
import dk.dbc.dataio.commons.types.rest.JobStoreServiceConstants;
import dk.dbc.dataio.commons.utils.httpclient.HttpClient;
import dk.dbc.dataio.commons.utils.jersey.jackson.Jackson2xFeature;
import dk.dbc.dataio.commons.utils.jobstore.JobStoreServiceConnector;
import dk.dbc.dataio.commons.utils.jobstore.JobStoreServiceConnectorException;
import dk.dbc.dataio.commons.utils.service.ServiceUtil;
import dk.dbc.dataio.gui.client.exceptions.ProxyError;
import dk.dbc.dataio.gui.client.exceptions.ProxyException;
import dk.dbc.dataio.gui.client.proxies.JobStoreProxy;
import org.glassfish.jersey.client.ClientConfig;

import javax.naming.NamingException;
import javax.servlet.ServletException;
import javax.ws.rs.client.Client;
import javax.ws.rs.core.GenericType;
import javax.ws.rs.core.Response;
import java.util.List;

public class JobStoreProxyImpl implements JobStoreProxy {
    Client client = null;
    final String baseUrl;
    JobStoreServiceConnector jobStoreServiceConnector;

    public JobStoreProxyImpl() throws NamingException {
        final ClientConfig clientConfig = new ClientConfig().register(new Jackson2xFeature());
        client = HttpClient.newClient(clientConfig);
        baseUrl = ServiceUtil.getJobStoreServiceEndpoint();
        jobStoreServiceConnector = new JobStoreServiceConnector(client, baseUrl);
    }

    // This constructor is intended for test purpose only with reference to dependency injection.
    public JobStoreProxyImpl(JobStoreServiceConnector jobStoreServiceConnector) throws NamingException {
        final ClientConfig clientConfig = new ClientConfig().register(new Jackson2xFeature());
        client = HttpClient.newClient(clientConfig);
        baseUrl = ServiceUtil.getJobStoreServiceEndpoint();
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
    public JobCompletionState getJobCompletionState(long jobId) throws ProxyException {
        final JobCompletionState jobCompletionState;
        try {
            jobCompletionState = jobStoreServiceConnector.getJobCompletionState(jobId);
        } catch (JobStoreServiceConnectorException e) {
            throw new ProxyException(ProxyError.SERVICE_NOT_FOUND, e);
        }
        return jobCompletionState;
    }

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
