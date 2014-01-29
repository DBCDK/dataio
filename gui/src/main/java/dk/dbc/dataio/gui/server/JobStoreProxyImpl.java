package dk.dbc.dataio.gui.server;

import dk.dbc.dataio.commons.types.JobInfo;
import dk.dbc.dataio.commons.types.JobStoreServiceEntryPoint;
import dk.dbc.dataio.commons.utils.httpclient.HttpClient;
import dk.dbc.dataio.gui.client.exceptions.JobStoreProxyError;
import dk.dbc.dataio.gui.client.exceptions.JobStoreProxyException;
import dk.dbc.dataio.gui.client.proxies.JobStoreProxy;
import java.util.List;
import javax.servlet.ServletException;
import javax.ws.rs.client.Client;
import javax.ws.rs.core.GenericType;
import javax.ws.rs.core.Response;
import org.glassfish.jersey.client.ClientConfig;
import org.glassfish.jersey.jackson.JacksonFeature;

public class JobStoreProxyImpl implements JobStoreProxy {
    Client client = null;

    public JobStoreProxyImpl() {
        final ClientConfig clientConfig = new ClientConfig().register(new JacksonFeature());
        client = HttpClient.newClient(clientConfig);
    }

    @Override
    public List<JobInfo> findAllJobs() throws JobStoreProxyException {
        final Response response;
        final List<JobInfo> result;
        try {
            response = HttpClient.doGet(client, ServletUtil.getJobStoreServiceEndpoint(), JobStoreServiceEntryPoint.JOBS);
        } catch (ServletException e) {
            throw new JobStoreProxyException(JobStoreProxyError.SERVICE_NOT_FOUND, e);
        }
        try {
            assertStatusCode(response, Response.Status.OK);
            result = response.readEntity(new GenericType<List<JobInfo>>() { });
        } finally {
            response.close();
        }
        return result;
    }

    public void close() {
        HttpClient.closeClient(client);
    }

    private void assertStatusCode(Response response, Response.Status expectedStatus) throws JobStoreProxyException {
        final Response.Status status = Response.Status.fromStatusCode(response.getStatus());
        if (status != expectedStatus) {
            final JobStoreProxyError errorCode;
            switch (status) {
                case BAD_REQUEST: errorCode = JobStoreProxyError.BAD_REQUEST;
                    break;
                case NOT_ACCEPTABLE: errorCode = JobStoreProxyError.NOT_ACCEPTABLE;
                    break;
                case PRECONDITION_FAILED: errorCode = JobStoreProxyError.ENTITY_NOT_FOUND;
                    break;
                default:
                    errorCode = JobStoreProxyError.INTERNAL_SERVER_ERROR;
            }
            throw new JobStoreProxyException(errorCode, response.readEntity(String.class));
        }
    }

}
