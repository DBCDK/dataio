package dk.dbc.dataio.harvester.periodicjobs;

import dk.dbc.dataio.commons.time.StopWatch;
import dk.dbc.httpclient.FailSafeHttpClient;
import dk.dbc.invariant.InvariantUtil;
import java.util.Collections;
import java.util.concurrent.TimeUnit;
import javax.ws.rs.ProcessingException;
import javax.ws.rs.client.Client;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import net.jodah.failsafe.RetryPolicy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import dk.dbc.httpclient.HttpPost;

/**
 * PeriodicJobsHarvesterServiceConnector - dataIO periodic jobs harvester REST service client.
 * <p>
 * To use this class, you construct an instance, specifying a web resources client as well as
 * a base URL for the periodic jobs harveste service endpoint you will be communicating with.
 * </p>
 * <p>
 * This class is thread safe, as long as the given web resources client remains thread safe.
 * </p>
 */
public class PeriodicJobsHarvesterServiceConnector {
    private static final Logger log = LoggerFactory.getLogger(PeriodicJobsHarvesterServiceConnector.class);

    private static final RetryPolicy RETRY_POLICY = new RetryPolicy()
            .retryOn(Collections.singletonList(ProcessingException.class))
            .retryIf((Response response) -> response.getStatus() == 404 || response.getStatus() == 500 || response.getStatus() == 502)
            .withDelay(1, TimeUnit.SECONDS)
            .withMaxRetries(3);

    private final FailSafeHttpClient failSafeHttpClient;
    private final String baseUrl;

    /**
     * Class constructor
     * @param httpClient web resources client
     * @param baseUrl base URL for job-store service endpoint
     * @throws NullPointerException if given null-valued argument
     * @throws IllegalArgumentException if given empty-valued {@code baseUrl} argument
     */
    public PeriodicJobsHarvesterServiceConnector(Client httpClient, String baseUrl) throws NullPointerException, IllegalArgumentException {
        this(FailSafeHttpClient.create(httpClient, RETRY_POLICY), baseUrl);
    }

    public PeriodicJobsHarvesterServiceConnector(FailSafeHttpClient failSafeHttpClient, String baseUrl) {
        this.failSafeHttpClient = InvariantUtil.checkNotNullOrThrow(failSafeHttpClient, "failSafeHttpClient");
        this.baseUrl = InvariantUtil.checkNotNullNotEmptyOrThrow(baseUrl, "baseUrl");
    }

    public void createPeriodicJob(Long id) throws PeriodicJobsHarvesterConnectorUnexpectedStatusCodeException {
        final StopWatch stopWatch = new StopWatch();
        final Response response = new HttpPost(failSafeHttpClient)
                .withBaseUrl(baseUrl)
                .withPathElements("/jobs")
                .withData(id, MediaType.TEXT_PLAIN)
                .execute();
        verifyResponseStatus(Response.Status.fromStatusCode(response.getStatus()));
    }

    private void verifyResponseStatus(Response.Status actualStatus) throws PeriodicJobsHarvesterConnectorUnexpectedStatusCodeException {
        if (actualStatus != Response.Status.OK) {
            throw new PeriodicJobsHarvesterConnectorUnexpectedStatusCodeException(
                    String.format("periodic-jobs-harvester service returned with unexpected status code: %s", actualStatus),
                    actualStatus.getStatusCode());
        }
    }
}
