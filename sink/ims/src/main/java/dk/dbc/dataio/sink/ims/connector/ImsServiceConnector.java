package dk.dbc.dataio.sink.ims.connector;

import dk.dbc.invariant.InvariantUtil;
import dk.dbc.oss.ns.updatemarcxchange.MarcXchangeRecord;
import dk.dbc.oss.ns.updatemarcxchange.UpdateMarcXchangePortType;
import dk.dbc.oss.ns.updatemarcxchange.UpdateMarcXchangeRequest;
import dk.dbc.oss.ns.updatemarcxchange.UpdateMarcXchangeResult;
import dk.dbc.oss.ns.updatemarcxchange.UpdateMarcXchangeServices;
import jakarta.xml.ws.BindingProvider;
import jakarta.xml.ws.WebServiceException;
import net.jodah.failsafe.Failsafe;
import net.jodah.failsafe.RetryPolicy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.util.List;

/**
 * IMS web service connector.
 * Instances of this class are NOT thread safe.
 */
public class ImsServiceConnector {
    public static final String CONNECT_TIMEOUT_PROPERTY = "com.sun.xml.ws.connect.timeout";
    public static final String REQUEST_TIMEOUT_PROPERTY = "com.sun.xml.ws.request.timeout";
    public static final int CONNECT_TIMEOUT_DEFAULT_IN_MS = 60 * 1000;    // 1 minute
    public static final int REQUEST_TIMEOUT_DEFAULT_IN_MS = 3 * 60 * 1000;    // 3 minutes
    private static final Logger LOGGER = LoggerFactory.getLogger(ImsServiceConnector.class);
    private final String endpoint;
    /* web-service proxy */
    private final UpdateMarcXchangePortType proxy;
    private RetryPolicy<Object> retryPolicy = new RetryPolicy<>()
            .handle(WebServiceException.class)
            .withDelay(Duration.ofSeconds(10))
            .withMaxRetries(6);

    public ImsServiceConnector(String endpoint) throws NullPointerException, IllegalArgumentException {
        this(new UpdateMarcXchangeServices(), endpoint);
    }

    ImsServiceConnector(UpdateMarcXchangeServices services, String endpoint)
            throws NullPointerException, IllegalArgumentException {
        this.endpoint = InvariantUtil.checkNotNullNotEmptyOrThrow(endpoint, "endpoint");
        proxy = getProxy(InvariantUtil.checkNotNullOrThrow(services, "services"));
    }

    public ImsServiceConnector withRetryPolicy(RetryPolicy<Object> retryPolicy) {
        this.retryPolicy = retryPolicy;
        return this;
    }

    /**
     * Calls updateMarcXchange operation of the ims Web service
     *
     * @param trackingId         unique ID for each chunk within the job
     * @param marcXchangeRecords list of marcXchange records to set on updateMarcXchangeRequest
     * @return list containing UpdateMarcXchangeResults
     * @throws WebServiceException on failure communicating with the ims web service
     */
    public List<UpdateMarcXchangeResult> updateMarcXchange(String trackingId, List<MarcXchangeRecord> marcXchangeRecords) throws WebServiceException {
        LOGGER.trace("Using endpoint: {}", endpoint);
        UpdateMarcXchangeRequest updateMarcXchangeRequest = new UpdateMarcXchangeRequest();
        updateMarcXchangeRequest.setTrackingId(trackingId);
        updateMarcXchangeRequest.getMarcXchangeRecord().addAll(marcXchangeRecords);
        return Failsafe.with(retryPolicy).get(() -> proxy.updateMarcXchange(updateMarcXchangeRequest));
    }

    private UpdateMarcXchangePortType getProxy(UpdateMarcXchangeServices services) {
        UpdateMarcXchangePortType proxy = services.getUpdateMarcXchangePort();
        // We don't want to rely on the endpoint from the WSDL
        BindingProvider bindingProvider = (BindingProvider) proxy;
        bindingProvider.getRequestContext().put(BindingProvider.ENDPOINT_ADDRESS_PROPERTY, endpoint);
        // FixMe: timeouts should be made configurable
        bindingProvider.getRequestContext().put(CONNECT_TIMEOUT_PROPERTY, CONNECT_TIMEOUT_DEFAULT_IN_MS);
        bindingProvider.getRequestContext().put(REQUEST_TIMEOUT_PROPERTY, REQUEST_TIMEOUT_DEFAULT_IN_MS);
        return proxy;
    }
}
