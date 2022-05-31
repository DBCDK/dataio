package dk.dbc.dataio.harvester.connector;

import dk.dbc.dataio.harvester.task.connector.HarvesterTaskServiceConnector;
import dk.dbc.dataio.harvester.task.connector.HarvesterTaskServiceConnectorException;
import dk.dbc.httpclient.FailSafeHttpClient;
import dk.dbc.httpclient.HttpGet;
import dk.dbc.httpclient.HttpPost;

import javax.ws.rs.client.Client;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.time.Instant;

public class TickleHarvesterServiceConnector extends HarvesterTaskServiceConnector {
    public TickleHarvesterServiceConnector(Client httpClient, String baseUrl)
            throws NullPointerException, IllegalArgumentException {
        super(httpClient, baseUrl);
    }

    public TickleHarvesterServiceConnector(FailSafeHttpClient failSafeHttpClient, String baseUrl) {
        super(failSafeHttpClient, baseUrl);
    }

    public int getDataSetSizeEstimate(int dataSetId) throws HarvesterTaskServiceConnectorException {
        return fetchDataSetSizeEstimate(String.valueOf(dataSetId));
    }

    public int getDataSetSizeEstimate(String dataSetName) throws HarvesterTaskServiceConnectorException {
        return fetchDataSetSizeEstimate(dataSetName);
    }

    public void deleteOutdatedRecords(int dataSetId, Instant cutOff)
            throws HarvesterTaskServiceConnectorException {
        deleteOutdatedRecords(String.valueOf(dataSetId), cutOff);
    }

    public void deleteOutdatedRecords(String dataSet, Instant cutOff)
            throws HarvesterTaskServiceConnectorException {
        final Response response = new HttpPost(failSafeHttpClient)
                .withBaseUrl(baseUrl)
                .withPathElements("dataset", dataSet,
                        "time-of-last-modification-cut-off")
                .withData(cutOff.toEpochMilli(), MediaType.TEXT_PLAIN)
                .execute();
        try {
            verifyResponseStatus(Response.Status.fromStatusCode(response.getStatus()),
                    Response.Status.OK);
        } finally {
            response.close();
        }
    }

    private int fetchDataSetSizeEstimate(String idPathParam) throws HarvesterTaskServiceConnectorException {
        final Response response = new HttpGet(failSafeHttpClient)
                .withBaseUrl(baseUrl)
                .withPathElements("dataset", idPathParam, "size-estimate")
                .execute();
        try {
            verifyResponseStatus(Response.Status.fromStatusCode(response.getStatus()), Response.Status.OK);
            return readResponseEntity(response, Integer.class);
        } finally {
            response.close();
        }
    }
}
