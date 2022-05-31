package dk.dbc.dataio.sink.openupdate.connector;

import dk.dbc.dataio.commons.types.Constants;
import dk.dbc.invariant.InvariantUtil;
import dk.dbc.marc.binding.DataField;
import dk.dbc.marc.binding.MarcRecord;
import dk.dbc.marc.binding.SubField;
import dk.dbc.oss.ns.catalogingupdate.Authentication;
import dk.dbc.oss.ns.catalogingupdate.BibliographicRecord;
import dk.dbc.oss.ns.catalogingupdate.CatalogingUpdatePortType;
import dk.dbc.oss.ns.catalogingupdate.MessageEntry;
import dk.dbc.oss.ns.catalogingupdate.Options;
import dk.dbc.oss.ns.catalogingupdate.UpdateOptionEnum;
import dk.dbc.oss.ns.catalogingupdate.UpdateRecordRequest;
import dk.dbc.oss.ns.catalogingupdate.UpdateRecordResult;
import dk.dbc.oss.ns.catalogingupdate.UpdateService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.xml.ws.BindingProvider;
import java.util.ArrayList;
import java.util.List;

/**
 * Update web service connector.
 * Instances of this class are NOT thread safe.
 */
public class OpenUpdateServiceConnector {
    private static final Logger LOGGER = LoggerFactory.getLogger(OpenUpdateServiceConnector.class);

    private static final String CONNECT_TIMEOUT_PROPERTY = "com.sun.xml.ws.connect.timeout";
    private static final String REQUEST_TIMEOUT_PROPERTY = "com.sun.xml.ws.request.timeout";
    private static final int CONNECT_TIMEOUT_DEFAULT_IN_MS = 60 * 1000;       // 1 minute
    private static final int REQUEST_TIMEOUT_DEFAULT_IN_MS = 60 * 60 * 1000;  // 60 minutes -- we wait and wait on open update.

    private final String endpoint;
    private final String userName;
    private final String password;

    /* web-service proxy */
    private final CatalogingUpdatePortType proxy;

    private final boolean validateOnly;

    /**
     * Class constructor
     *
     * @param endpoint web service endpoint base URL on the form "http(s)://host:port/path"
     * @throws NullPointerException     if passed null valued {@code endpoint}
     * @throws IllegalArgumentException if passed empty valued {@code endpoint}
     */
    public OpenUpdateServiceConnector(String endpoint)
            throws NullPointerException, IllegalArgumentException {
        this(endpoint, "", "");
    }

    /**
     * Class constructor
     *
     * @param endpoint web service endpoint base URL on the form "http(s)://host:port/path"
     * @param userName for authenticating any user requiring access to the webservice
     * @param password for authenticating any user requiring access to the webservice
     * @throws NullPointerException     if passed any null valued argument
     * @throws IllegalArgumentException if passed empty valued {@code endpoint}
     */
    public OpenUpdateServiceConnector(String endpoint, String userName, String password)
            throws NullPointerException, IllegalArgumentException {
        this(new UpdateService(), endpoint, userName, password);
    }

    /**
     * Class constructor
     *
     * @param service  web service client view of the CatalogingUpdate Web service
     * @param endpoint web service endpoint base URL on the form "http(s)://host:port/path"
     * @param userName for authenticating any user requiring access to the webservice
     * @param password for authenticating any user requiring access to the webservice
     * @throws NullPointerException     if passed any null valued argument
     * @throws IllegalArgumentException if passed empty valued {@code endpoint}, {@code userName}, {@code password}
     */
    OpenUpdateServiceConnector(UpdateService service, String endpoint, String userName, String password)
            throws NullPointerException, IllegalArgumentException {
        InvariantUtil.checkNotNullOrThrow(service, "service");
        this.endpoint = InvariantUtil.checkNotNullNotEmptyOrThrow(endpoint, "endpoint");
        this.userName = InvariantUtil.checkNotNullOrThrow(userName, "userName");
        this.password = InvariantUtil.checkNotNullOrThrow(password, "password");
        proxy = this.getProxy(service);
        final String validateOnlyEnv = System.getenv(Constants.UPDATE_VALIDATE_ONLY_FLAG);
        validateOnly = validateOnlyEnv != null &&
                validateOnlyEnv.toLowerCase().equals("true");
        if (validateOnlyEnv != null && !validateOnly) {
            LOGGER.warn("{} had unexpected value {}",
                    Constants.UPDATE_VALIDATE_ONLY_FLAG, validateOnlyEnv);
        }
    }

    /**
     * Calls updateRecord operation of the Open Update Web service
     *
     * @param groupId             group id used for authorization
     * @param schemaName          the template towards which the validation should be performed
     * @param bibliographicRecord containing the MarcXChange to validate
     * @param trackingId          unique ID for each OpenUpdate request
     * @return UpdateRecordResult instance
     * @throws NullPointerException     if passed any null valued {@code template} or {@code bibliographicRecord} argument
     * @throws IllegalArgumentException if passed empty valued {@code template}
     */
    public UpdateRecordResult updateRecord(String groupId, String schemaName, BibliographicRecord bibliographicRecord, String trackingId)
            throws NullPointerException, IllegalArgumentException {
        InvariantUtil.checkNotNullNotEmptyOrThrow(groupId, "groupId");
        InvariantUtil.checkNotNullNotEmptyOrThrow(schemaName, "schemaName");
        InvariantUtil.checkNotNullOrThrow(bibliographicRecord, "bibliographicRecord");
        LOGGER.trace("Using endpoint: {}", endpoint);
        final UpdateRecordRequest updateRecordRequest = buildUpdateRecordRequest(groupId, schemaName, bibliographicRecord, trackingId);
        return proxy.updateRecord(updateRecordRequest);
    }

    /**
     * Converts all message entries in given result into MARC datafields
     *
     * @param errorFieldTag tag of created MARC datafields
     * @param result        result of update service request
     * @param marcRecord    MARC record used to resolve ordinal positions given in result (typically
     *                      the MARC record for which the updateRecord() call produced the result),
     * @return List of {@link DataField}, one for each message entry in result
     */
    public List<DataField> toErrorFields(String errorFieldTag, UpdateRecordResult result, MarcRecord marcRecord) {
        final List<DataField> fields = new ArrayList<>();
        if (result != null && result.getMessages() != null) {
            final List<MessageEntry> messages = result.getMessages().getMessageEntry();
            if (messages != null && !messages.isEmpty()) {
                for (MessageEntry message : messages) {
                    fields.add(toErrorField(errorFieldTag, message, marcRecord));
                }
            }
        }
        return fields;
    }

    private DataField toErrorField(String tag, MessageEntry message, MarcRecord marcRecord) {
        final DataField errorField = new DataField()
                .setTag(tag)
                .setInd1('0')
                .setInd2('0')
                .addSubField(
                        new SubField()
                                .setCode('a')
                                .setData(message.getMessage()));
        if (marcRecord != null) {
            final DataField datafield = getDatafield(message, marcRecord);
            if (datafield != null) {
                final List<String> parts = new ArrayList<>(4);
                final SubField subfield = getSubfield(message, datafield);
                if (subfield != null) {
                    parts.add("delfelt");
                    parts.add(String.valueOf(subfield.getCode()));
                }
                parts.add("felt");
                parts.add(datafield.getTag());
                errorField.addSubField(
                        new SubField()
                                .setCode('b')
                                .setData(String.join(" ", parts)));
            }
        }
        return errorField;
    }

    private DataField getDatafield(MessageEntry message, MarcRecord marcRecord) {
        final Integer fieldpos = message.getOrdinalPositionOfField();
        if (fieldpos != null) {
            try {
                return (DataField) marcRecord.getFields().get(fieldpos);
            } catch (RuntimeException e) {
                LOGGER.error("Caught exception while extracting datafield {} from MARC record",
                        fieldpos, e);
            }
        }
        return null;
    }

    private SubField getSubfield(MessageEntry message, DataField datafield) {
        final Integer subfieldpos = message.getOrdinalPositionOfSubfield();
        if (subfieldpos != null) {
            try {
                return datafield.getSubfields().get(subfieldpos);
            } catch (RuntimeException e) {
                LOGGER.error("Caught exception while extracting subfield {} from datafield {}",
                        subfieldpos, datafield, e);
            }
        }
        return null;
    }

    /**
     * Builds an UpdateRecordRequest
     *
     * @param groupId             group id used for authorization
     * @param schemaName          the template towards which the validation should be performed
     * @param bibliographicRecord containing the MarcXChange to validate
     * @param trackingId          unique ID for each OpenUpdate request
     * @return a new updateRecordRequest containing schemeName and bibliographicRecord
     */
    private UpdateRecordRequest buildUpdateRecordRequest(String groupId, String schemaName, BibliographicRecord bibliographicRecord, String trackingId) {
        UpdateRecordRequest updateRecordRequest = new UpdateRecordRequest();
        Authentication authentication = new Authentication();
        authentication.setGroupIdAut(groupId);
        authentication.setUserIdAut(userName);
        authentication.setPasswordAut(password);
        updateRecordRequest.setAuthentication(authentication);
        updateRecordRequest.setSchemaName(schemaName);
        updateRecordRequest.setBibliographicRecord(bibliographicRecord);
        updateRecordRequest.setTrackingId(trackingId);
        if (validateOnly) {
            if (updateRecordRequest.getOptions() == null) {
                Options options = new Options();
                options.getOption().add(UpdateOptionEnum.VALIDATE_ONLY);
                updateRecordRequest.setOptions(options);
            } else {
                updateRecordRequest.getOptions().getOption().add(
                        UpdateOptionEnum.VALIDATE_ONLY);
            }
        }
        return updateRecordRequest;
    }

    private CatalogingUpdatePortType getProxy(UpdateService service) {
        final CatalogingUpdatePortType proxy = service.getCatalogingUpdatePort();

        // We don't want to rely on the endpoint from the WSDL
        BindingProvider bindingProvider = (BindingProvider) proxy;
        bindingProvider.getRequestContext().put(BindingProvider.ENDPOINT_ADDRESS_PROPERTY, endpoint);
        // FixMe: timeouts should be made configurable
        bindingProvider.getRequestContext().put(CONNECT_TIMEOUT_PROPERTY, CONNECT_TIMEOUT_DEFAULT_IN_MS);
        bindingProvider.getRequestContext().put(REQUEST_TIMEOUT_PROPERTY, REQUEST_TIMEOUT_DEFAULT_IN_MS);

        return proxy;
    }
}
