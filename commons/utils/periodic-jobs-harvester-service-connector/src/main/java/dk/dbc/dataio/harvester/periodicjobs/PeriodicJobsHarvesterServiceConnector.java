package dk.dbc.dataio.harvester.periodicjobs;

import dk.dbc.dataio.commons.time.StopWatch;
import dk.dbc.httpclient.FailSafeHttpClient;
import dk.dbc.httpclient.HttpPost;
import dk.dbc.invariant.InvariantUtil;
import net.jodah.failsafe.RetryPolicy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.ProcessingException;
import javax.ws.rs.client.Client;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.time.Duration;

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

    private static final RetryPolicy<Response> RETRY_POLICY = new RetryPolicy<Response>()
            .handle(ProcessingException.class)
            .handleResultIf(response ->
                    response.getStatus() == 404
                            || response.getStatus() == 500
                            || response.getStatus() == 502)
            .withDelay(Duration.ofSeconds(10))
            .withMaxRetries(6);

    private final FailSafeHttpClient failSafeHttpClient;
    private final String baseUrl;

    /**
     * Class constructor
     *
     * @param httpClient web resources client
     * @param baseUrl    base URL for job-store service endpoint
     * @throws NullPointerException     if given null-valued argument
     * @throws IllegalArgumentException if given empty-valued {@code baseUrl} argument
     */
    public PeriodicJobsHarvesterServiceConnector(Client httpClient, String baseUrl) throws NullPointerException, IllegalArgumentException {
        this(FailSafeHttpClient.create(httpClient, RETRY_POLICY), baseUrl);
    }

    public PeriodicJobsHarvesterServiceConnector(FailSafeHttpClient failSafeHttpClient, String baseUrl) {
        this.failSafeHttpClient = InvariantUtil.checkNotNullOrThrow(failSafeHttpClient, "failSafeHttpClient");
        this.baseUrl = InvariantUtil.checkNotNullNotEmptyOrThrow(baseUrl, "baseUrl");
    }

    public void createPeriodicJob(Long id) throws PeriodicJobsHarvesterServiceConnectorException {
        Response response = null;
        final StopWatch stopWatch = new StopWatch();
        try {
            response = new HttpPost(failSafeHttpClient)
                    .withBaseUrl(baseUrl)
                    .withPathElements("/jobs")
                    .withData(id, MediaType.TEXT_PLAIN)
                    .execute();
        } catch (ProcessingException e) {
            throw new PeriodicJobsHarvesterServiceConnectorException("Harvester connection exception", e);
        } finally {
            log.debug("PeriodicJobsHarvesterServiceConnector.createPeriodicJob took {} ms ", stopWatch.getElapsedTime());
        }
        verifyResponseStatus(Response.Status.fromStatusCode(response.getStatus()));
    }

    public String validatePeriodicJob(Long id) throws PeriodicJobsHarvesterServiceConnectorException {
        final StopWatch stopWatch = new StopWatch();
        try {
            final Response response = new HttpPost(failSafeHttpClient)
                    .withBaseUrl(baseUrl)
                    .withPathElements("/jobs/validate")
                    .withData(id, MediaType.TEXT_PLAIN)
                    .execute();

            verifyResponseStatus(Response.Status.fromStatusCode(response.getStatus()));

            return response.readEntity(String.class);
        } catch (ProcessingException e) {
            throw new PeriodicJobsHarvesterServiceConnectorException("Harvester connection exception", e);
        } finally {
            log.debug("PeriodicJobsHarvesterServiceConnector.validatePeriodicJob took {} ms ", stopWatch.getElapsedTime());
        }

    }

    private void verifyResponseStatus(Response.Status actualStatus) throws PeriodicJobsHarvesterConnectorUnexpectedStatusCodeException {
        if (actualStatus != Response.Status.OK) {
            throw new PeriodicJobsHarvesterConnectorUnexpectedStatusCodeException(
                    String.format("periodic-jobs-harvester service returned with unexpected status code: %s", actualStatus),
                    actualStatus.getStatusCode());
        }
    }
}
