package dk.dbc.dataio.sink.fbs.connector;

import dk.dbc.dataio.commons.utils.invariant.InvariantUtil;
import dk.dbc.dataio.sink.fbs.types.FbsUpdateConnectorException;
import dk.dbc.oss.ns.updatemarcxchange.MarcXchangeRecord;
import dk.dbc.oss.ns.updatemarcxchange.UpdateMarcXchangePortType;
import dk.dbc.oss.ns.updatemarcxchange.UpdateMarcXchangeRequest;
import dk.dbc.oss.ns.updatemarcxchange.UpdateMarcXchangeResult;
import dk.dbc.oss.ns.updatemarcxchange.UpdateMarcXchangeServices;
import info.lc.xmlns.marcxchange_v1.CollectionType;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;
import javax.xml.transform.stream.StreamSource;
import javax.xml.ws.BindingProvider;
import javax.xml.ws.WebServiceException;
import java.io.StringReader;

/**
 * FBS UpdateMarcXchange web service connector
 * <p>
 * This class is thread safe
 * </p>
 */
public class FbsUpdateConnector {
    public static final String CONNECT_TIMEOUT_PROPERTY = "com.sun.xml.ws.connect.timeout";
    public static final String REQUEST_TIMEOUT_PROPERTY = "com.sun.xml.ws.request.timeout";
    public static final int CONNECT_TIMEOUT_DEFAULT_IN_MS =  1 * 60 * 1000;    // 1 minute
    public static final int REQUEST_TIMEOUT_DEFAULT_IN_MS =  3 * 60 * 1000;    // 3 minutes

    private final String endpoint;
    /* JAX-WS class generated from WSDL */
    private final UpdateMarcXchangeServices services;
    private final JAXBContext jaxbContext;

    /**
     * Class constructor
     * @param endpoint web service endpoint base URL on the form "http(s)://host:port/path"
     * @throws NullPointerException if passed any null valued {@code endpoint}
     * @throws IllegalArgumentException if passed empty valued {@code endpoint}
     * @throws FbsUpdateConnectorException if unable to create JAXB context
     */
    public FbsUpdateConnector(String endpoint)
            throws NullPointerException, IllegalArgumentException, FbsUpdateConnectorException {
        this(new UpdateMarcXchangeServices(), endpoint);
    }

    /**
     * Class constructor
     * @param services web service client view of the UpdateMarcXchange Web service
     * @param endpoint web service endpoint base URL on the form "http(s)://host:port/path"
     * @throws NullPointerException if passed any null valued argument
     * @throws IllegalArgumentException if passed empty valued {@code endpoint}
     * @throws FbsUpdateConnectorException if unable to create JAXB context
     */
    FbsUpdateConnector(UpdateMarcXchangeServices services, String endpoint)
            throws NullPointerException, IllegalArgumentException, FbsUpdateConnectorException {
        this.services = InvariantUtil.checkNotNullOrThrow(services, "services");
        this.endpoint = InvariantUtil.checkNotNullNotEmptyOrThrow(endpoint, "endpoint");
        try {
            jaxbContext = JAXBContext.newInstance(CollectionType.class);
        } catch (JAXBException e) {
            throw new FbsUpdateConnectorException("Exception caught while instantiating JaxbContext", e);
        }
    }

    /**
     * Calls updateMarcExchange operation of the UpdateMarcXchange Web service
     * @param collection MARC exchange collection
     * @param trackingId tracking ID (can be null or empty)
     * @return UpdateMarcXchangeResult instance
     * @throws NullPointerException if passed any null valued {@code agencyId} or {@code collection} argument
     * @throws IllegalArgumentException if passed empty valued {@code agencyId} or {@code collection} argument
     * @throws FbsUpdateConnectorException if unable to transform collection to CollectionType
     * @throws WebServiceException on general transport layer failure or service internal error
     */
    public UpdateMarcXchangeResult updateMarcExchange(String collection, String trackingId)
            throws NullPointerException, IllegalArgumentException, WebServiceException, FbsUpdateConnectorException {
        InvariantUtil.checkNotNullNotEmptyOrThrow(collection, "collection");

        final UpdateMarcXchangeRequest updateMarcXchangeRequest = new UpdateMarcXchangeRequest();
        try {
            updateMarcXchangeRequest.setMarcXchangeRecord(toMarcXchangeRecord(collection));
        } catch (JAXBException e) {
            throw new FbsUpdateConnectorException("Exception caught while converting to MarcXchangeRecord type", e);
        }
        if (trackingId != null && !trackingId.isEmpty()) {
            updateMarcXchangeRequest.setTrackingId(trackingId);
        }
        return getProxy().updateMarcXchange(updateMarcXchangeRequest);
    }

    public String getEndpoint() {
        return endpoint;
    }

    private UpdateMarcXchangePortType getProxy() {
        // getUpdateMarcXchangePort() calls getPort() which is not thread safe, so
        // we cannot let the proxy be application scoped.
        // If performance is lacking we should consider options for reuse.
        final UpdateMarcXchangePortType proxy = services.getUpdateMarcXchangePort();

        // We don't want to rely on the endpoint from the WSDL
        BindingProvider bindingProvider = (BindingProvider)proxy;
        bindingProvider.getRequestContext().put(BindingProvider.ENDPOINT_ADDRESS_PROPERTY, endpoint);
        // FixMe: timeouts should be made configurable
        bindingProvider.getRequestContext().put(CONNECT_TIMEOUT_PROPERTY, CONNECT_TIMEOUT_DEFAULT_IN_MS);
        bindingProvider.getRequestContext().put(REQUEST_TIMEOUT_PROPERTY, REQUEST_TIMEOUT_DEFAULT_IN_MS);

        return proxy;
    }

    /* Transforms given MARC exchange collection into its corresponding CollectionType
       representation and wraps it in a MarcXchangeRecord
     */
    private MarcXchangeRecord toMarcXchangeRecord(String collection) throws JAXBException {
        final Unmarshaller unmarshaller = jaxbContext.createUnmarshaller();
        try (final StringReader reader = new StringReader(collection)) {
            final JAXBElement<CollectionType> jaxbCollection = unmarshaller.unmarshal(
                    new StreamSource(reader), CollectionType.class);
            final MarcXchangeRecord marcXchangeRecord = new MarcXchangeRecord();
            marcXchangeRecord.setCollection(jaxbCollection.getValue());
            return marcXchangeRecord;
        }
    }
}
