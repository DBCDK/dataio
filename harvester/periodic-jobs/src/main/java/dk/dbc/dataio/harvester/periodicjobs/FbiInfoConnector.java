package dk.dbc.dataio.harvester.periodicjobs;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import dk.dbc.httpclient.FailSafeHttpClient;
import dk.dbc.httpclient.HttpClient;
import dk.dbc.httpclient.HttpGet;
import dk.dbc.rawrepo.dto.RecordIdDTO;
import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.ProcessingException;
import jakarta.ws.rs.client.ClientBuilder;
import jakarta.ws.rs.core.Response;
import net.jodah.failsafe.RetryPolicy;

import java.time.Duration;
import java.util.List;

public class FbiInfoConnector {
    private static final RetryPolicy<Response> RETRY_POLICY = new RetryPolicy<Response>()
            .handle(ProcessingException.class)
            .handleResultIf(response -> response.getStatus() == 500 || response.getStatus() == 502)
            .withDelay(Duration.ofSeconds(10))
            .withMaxRetries(6);
    private final String baseUrl;
    private final HttpClient httpClient;


    public FbiInfoConnector(String baseUrl) {
        httpClient = FailSafeHttpClient.create(ClientBuilder.newClient(), RETRY_POLICY);
        this.baseUrl = baseUrl;
    }

    public boolean hasCover(RecordIdDTO recordId) {
        HttpGet httpGet = new HttpGet(httpClient).withBaseUrl(baseUrl).withPathElements("manifestation", recordId.getAgencyId() + "-basis:" + recordId.getBibliographicRecordId()).withQueryParameter("trackingId", "uuid");
        try {
            FbiInfoResponse response = httpGet.executeAndExpect(FbiInfoResponse.class);
            if (response == null || response.resources == null) return false;
            return response.resources.contains("forside");
        } catch (NotFoundException nfe) {
            return false;
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class FbiInfoResponse {
        public List<String> resources;
    }
}
