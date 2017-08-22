package dk.dbc.dataio.cli.lhrretriever;

import dk.dbc.dataio.commons.utils.httpclient.FailSafeHttpClient;
import dk.dbc.dataio.commons.utils.httpclient.HttpGet;
import dk.dbc.dataio.commons.utils.invariant.InvariantUtil;
import net.jodah.failsafe.RetryPolicy;

import javax.ws.rs.ProcessingException;
import javax.ws.rs.client.Client;
import javax.ws.rs.core.Response;
import java.io.InputStream;
import java.util.Collections;
import java.util.concurrent.TimeUnit;

public class Ocn2PidServiceConnector {
    private static final String lhrEndpoint = "records-with-lhr";
    private static final RetryPolicy RETRY_POLICY = new RetryPolicy()
        .retryOn(Collections.singletonList(ProcessingException.class))
        .retryIf((Response response) -> response.getStatus() == 404 || response.getStatus() == 500 || response.getStatus() == 502)
        .withDelay(10, TimeUnit.SECONDS)
        .withMaxRetries(6);

    private final FailSafeHttpClient failSafeHttpClient;
    private final String baseUrl;

    public Ocn2PidServiceConnector(Client httpClient, String baseUrl) throws NullPointerException, IllegalArgumentException {
        this(FailSafeHttpClient.create(httpClient, RETRY_POLICY), baseUrl);
    }

    private Ocn2PidServiceConnector(FailSafeHttpClient failSafeHttpClient, String baseUrl) {
        this.failSafeHttpClient = InvariantUtil.checkNotNullOrThrow(failSafeHttpClient, "failSafeHttpClient");
        this.baseUrl = InvariantUtil.checkNotNullNotEmptyOrThrow(baseUrl, "baseUrl");
    }

    public InputStream getEntitiesWithLHRStream() {
        final Response response = new HttpGet(failSafeHttpClient)
            .withBaseUrl(baseUrl)
            .withPathElements(new String[] {lhrEndpoint})
            .execute();
        return response.readEntity(InputStream.class);
    }
}
