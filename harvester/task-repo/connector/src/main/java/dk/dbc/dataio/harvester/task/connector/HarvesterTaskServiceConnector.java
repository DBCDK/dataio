package dk.dbc.dataio.harvester.task.connector;

import dk.dbc.dataio.commons.time.StopWatch;
import dk.dbc.dataio.commons.types.rest.HarvesterServiceConstants;
import dk.dbc.dataio.harvester.types.HarvestRequest;
import dk.dbc.httpclient.FailSafeHttpClient;
import dk.dbc.httpclient.HttpPost;
import dk.dbc.httpclient.PathBuilder;
import dk.dbc.invariant.InvariantUtil;
import net.jodah.failsafe.RetryPolicy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.ProcessingException;
import javax.ws.rs.client.Client;
import javax.ws.rs.core.Response;
import java.time.Duration;

/**
 * HarvesterTaskServiceConnector - dataIO Harvester task REST service client.
 * <p>
 * To use this class, you construct an instance, specifying a web resources client as well as
 * a base URL for the harvester service endpoint you will be communicating with.
 * </p>
 * <p>
 * This class is thread safe, as long as the given web resources client remains thread safe.
 * </p>
 */
public class HarvesterTaskServiceConnector {
    private static final Logger log = LoggerFactory.getLogger(HarvesterTaskServiceConnector.class);

    private static final RetryPolicy<Response> RETRY_POLICY = new RetryPolicy<Response>()
            .handle(ProcessingException.class)
            .handleResultIf(response ->
                    response.getStatus() == 404
                            || response.getStatus() == 500
                            || response.getStatus() == 502)
            .withDelay(Duration.ofSeconds(10))
            .withMaxRetries(6);

    protected final FailSafeHttpClient failSafeHttpClient;
    protected final String baseUrl;

    /**
     * Class constructor
     *
     * @param httpClient web resources client
     * @param baseUrl    base URL for rr-harvester service endpoint
     * @throws NullPointerException     if given null-valued argument
     * @throws IllegalArgumentException if given empty-valued {@code baseUrl} argument
     */
    public HarvesterTaskServiceConnector(Client httpClient, String baseUrl) throws NullPointerException, IllegalArgumentException {
        this(FailSafeHttpClient.create(httpClient, RETRY_POLICY), baseUrl);
    }

    public HarvesterTaskServiceConnector(FailSafeHttpClient failSafeHttpClient, String baseUrl) {
        this.failSafeHttpClient = InvariantUtil.checkNotNullOrThrow(failSafeHttpClient, "failSafeHttpClient");
        this.baseUrl = InvariantUtil.checkNotNullNotEmptyOrThrow(baseUrl, "baseUrl");
    }

    public String createHarvestTask(long harvestId, HarvestRequest request)
            throws ProcessingException, HarvesterTaskServiceConnectorException {
        log.trace("createHarvestTask({});", harvestId);
        final StopWatch stopWatch = new StopWatch();
        try {
            final PathBuilder path = new PathBuilder(HarvesterServiceConstants.HARVEST_TASKS)
                    .bind(HarvesterServiceConstants.HARVEST_ID_VARIABLE, harvestId);
            final Response response = new HttpPost(failSafeHttpClient)
                    .withBaseUrl(baseUrl)
                    .withPathElements(path.build())
                    .withJsonData(request)
                    .execute();
            try {
                verifyResponseStatus(Response.Status.fromStatusCode(response.getStatus()), Response.Status.CREATED);
                log.info("HarvestTask created with location header {}.", response.getLocation().toString());
                return response.getLocation().toString();
            } finally {
                response.close();
            }
        } finally {
            log.debug("createHarvestTask() took {} milliseconds", stopWatch.getElapsedTime());
        }
    }


    public Client getHttpClient() {
        return failSafeHttpClient.getClient();
    }

    public String getBaseUrl() {
        return baseUrl;
    }

    protected void verifyResponseStatus(Response.Status actualStatus, Response.Status expectedStatus)
            throws HarvesterTaskServiceConnectorException {
        if (actualStatus != expectedStatus) {
            throw new HarvesterTaskServiceConnectorUnexpectedStatusCodeException(
                    String.format("Harvester service returned with unexpected status code: %s", actualStatus),
                    actualStatus.getStatusCode());
        }
    }

    protected <T> T readResponseEntity(Response response, Class<T> tClass)
            throws HarvesterTaskServiceConnectorException {
        response.bufferEntity(); // must be done in order to possible avoid a timeout-exception from readEntity.
        final T entity = response.readEntity(tClass);
        if (entity == null) {
            throw new HarvesterTaskServiceConnectorException(
                    String.format("Service returned with null-valued %s entity", tClass.getName()));
        }
        return entity;
    }
}
