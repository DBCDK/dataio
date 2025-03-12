package dk.dbc.dataio.harvester.periodicjobs;

import dk.dbc.httpclient.FailSafeHttpClient;
import dk.dbc.httpclient.HttpClient;
import dk.dbc.httpclient.HttpPost;
import dk.dbc.rawrepo.dto.RecordIdDTO;
import jakarta.ws.rs.ProcessingException;
import jakarta.ws.rs.client.ClientBuilder;
import jakarta.ws.rs.core.Response;
import net.jodah.failsafe.RetryPolicy;

import java.time.Duration;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class FbiInfoConnector {
    private static final Pattern pattern = Pattern.compile("(\\d+)-[^:]+:(.+)");

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

    public Set<RecordIdDTO> hasCoverFilter(List<RecordIdDTO> recordIds) {
        List<String> list = recordIds.stream().map(this::toManifestationId).toList();
        HttpPost httpPost = new HttpPost(httpClient)
                .withBaseUrl(baseUrl)
                .withPathElements("manifestation", "forside")
                .withJsonData(list);
        //noinspection unchecked
        List<String> ids = httpPost.executeAndExpect(List.class);
        return ids.stream().map(this::toRecordIdDTO).collect(Collectors.toSet());
    }

    private String toManifestationId(RecordIdDTO recordIdDTO) {
        // Slapping the basis catalogue code on the record only work, because the periodic job query is limited to 870970
        return recordIdDTO.getAgencyId() + "-basis:" + recordIdDTO.getBibliographicRecordId();
    }

    private RecordIdDTO toRecordIdDTO(String manifestationId) {
        Matcher matcher = pattern.matcher(manifestationId);
        if(!matcher.find()) throw new IllegalArgumentException("Invalid manifestationId: " + manifestationId);
        return new RecordIdDTO(matcher.group(2), Integer.parseInt(matcher.group(1)));
    }
}
