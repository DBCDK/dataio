package dk.dbc.dataio.cli.lhrretriever;

import dk.dbc.httpclient.FailSafeHttpClient;
import dk.dbc.httpclient.HttpGet;
import dk.dbc.httpclient.PathBuilder;
import dk.dbc.invariant.InvariantUtil;
import net.jodah.failsafe.RetryPolicy;

import javax.ws.rs.ProcessingException;
import javax.ws.rs.client.Client;
import javax.ws.rs.core.Response;
import java.io.InputStream;
import java.util.Collections;
import java.util.concurrent.TimeUnit;

public class Ocn2PidServiceConnector {
    private static final String PIDS_WITH_LHR_ENDPOINT = "pid/lhr";
    private static final String PID_VARIABLE = "pid";
    private static final String OCN_BY_PID_ENDPOINT = "ocn-by-pid/{pid}";

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
        PathBuilder path = new PathBuilder(PIDS_WITH_LHR_ENDPOINT);
        final Response response = new HttpGet(failSafeHttpClient)
            .withBaseUrl(baseUrl)
            .withPathElements(path.build())
            .execute();
        return response.readEntity(InputStream.class);
    }

    /**
     * Gets ocn by pid
     * @param pid pid
     * @return ocn of record with corresponding pid
     */
    public String getOcnByPid(String pid) {
        PathBuilder path = new PathBuilder(OCN_BY_PID_ENDPOINT)
            .bind(PID_VARIABLE, pid);
        final Response response = new HttpGet(failSafeHttpClient)
            .withBaseUrl(baseUrl)
            .withPathElements(path.build())
            .execute();
        return response.readEntity(String.class);
    }
}
