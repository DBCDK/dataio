package dk.dbc.dataio.jobstore.service.ejb;

import dk.dbc.dataio.commons.types.Chunk;
import dk.dbc.dataio.commons.types.rest.JobStoreServiceConstants;
import dk.dbc.dataio.jobstore.types.JobStoreException;
import dk.dbc.httpclient.HttpClient;
import dk.dbc.httpclient.PathBuilder;
import org.glassfish.jersey.client.ClientConfig;
import org.glassfish.jersey.jackson.JacksonFeature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.client.Client;
import javax.ws.rs.core.Response;

/**
 * Created by ja7 on 1/14/17.
 * <p>
 * Fake Static JobStore Conenction Class used by Arquillian Tests
 */
public class TestJobStoreConnection {
    private static final Logger LOGGER = LoggerFactory.getLogger(TestJobStoreConnection.class);


    private static Client httpClient;
    private static String jobStoreUrl;


    /**/
    static public void initializeConnector(String endPoint) {
        LOGGER.debug("Initializing connector");
        httpClient = HttpClient.newClient(new ClientConfig()
                .register(new JacksonFeature()));
        jobStoreUrl = endPoint;

        LOGGER.info("Using service endpoint {}", jobStoreUrl);
    }


    static void sendChunkToJobstoreAsType(Chunk chunk, Chunk.Type chunkType) throws JobStoreException {
        if (httpClient == null) {
            return;
        }

        Chunk chunkToSend = new Chunk(chunk.getJobId(), chunk.getChunkId(), chunkType);
        chunkToSend.addAllItems(chunk.getItems());

        final PathBuilder path = new PathBuilder(chunkTypeToPath(chunkType))
                .bind(JobStoreServiceConstants.JOB_ID_VARIABLE, chunkToSend.getJobId())
                .bind(JobStoreServiceConstants.CHUNK_ID_VARIABLE, chunkToSend.getChunkId());
        final Response response = HttpClient.doPostWithJson(httpClient, chunkToSend, jobStoreUrl, path.build());

        final Response.Status actualStatus = Response.Status.fromStatusCode(response.getStatus());

        if (actualStatus != Response.Status.CREATED) {
            throw new JobStoreException("HTTP call Failed " + response.toString());
        }
    }

    static String chunkTypeToPath(Chunk.Type chunkType) {
        switch (chunkType) {
            case PROCESSED:
                return JobStoreServiceConstants.JOB_CHUNK_PROCESSED;
            case DELIVERED:
                return JobStoreServiceConstants.JOB_CHUNK_DELIVERED;
            default:
                return null;
        }
    }

    public static void resetConnector() {
        httpClient = null;
    }
}
