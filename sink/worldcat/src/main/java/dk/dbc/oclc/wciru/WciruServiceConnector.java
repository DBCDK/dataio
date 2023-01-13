package dk.dbc.oclc.wciru;

import dk.dbc.dataio.commons.utils.lang.JaxpUtil;
import dk.dbc.invariant.InvariantUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Element;
import org.xml.sax.SAXException;

import javax.xml.ws.BindingProvider;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * WorldCat Interactive Record Update (WCIRU) web-service client.
 * <p>
 * To use this class, you construct an instance, specifying parameters for the endpoint
 * you will be communicating with. Subsequently records can be transferred to the service
 * via calls of the addOrUpdateRecord method.
 * </p>
 * <p>
 * Instances of this class are NOT thread safe.
 * </p>
 * <p>
 * Be advised that this class utilizes the JaxpUtil class which uses thread local
 * variables internally. If not handled carefully in environments using thread pools
 * with long lived threads this might cause memory leak problems so make sure to use
 * appropriate memory analysis tools to verify correct behaviour.
 * </p>
 */
public class WciruServiceConnector {
    private static final Logger LOGGER = LoggerFactory.getLogger(WciruServiceConnector.class);

    public static final String SRW_VERSION = "1.0";
    public static final String RECORD_PACKING = "xml";
    public static final String RECORD_SCHEMA = "info:srw/schema/1/marcxml-v1.1";
    public static final String CREATE_ACTION = "info:srw/action/1/create";
    public static final String REPLACE_ACTION = "info:srw/action/1/replace";
    public static final String DELETE_ACTION = "info:srw/action/1/delete";

    private final String baseUrl;
    private final String authenticationToken;
    private final String projectId;

    /* web-service proxy */
    private final UpdateInterface proxy;

    /* For retrying if a known acceptable failure diagnostic is returned */
    private final RetryScheme retryScheme;

    /* For suppressing failures with diagnostics known to be insignificant */
    private final ErrorSuppressor errorSuppressor;

    /**
     * RetryScheme - represents a WciruServiceConnector failed operation retry scheme
     * <p>
     * This class is immutable, and exposes the following public fields: maxNumberOfRetries, millisecondsToSleepBetweenRetries and knownFailureDiagnostics
     * </p>
     */
    public static class RetryScheme {
        public final int maxNumberOfRetries;
        public final int millisecondsToSleepBetweenRetries;
        public final Set<String> knownFailureDiagnostics;

        /**
         * Class constructor
         *
         * @param maxNumberOfRetries                maximum number of retries of unsuccessful operation
         * @param millisecondsToSleepBetweenRetries wait time between retries in milliseconds
         * @param knownFailureDiagnostics           only operations failing with a diagnostic from this set is eligible for retry
         * @throws NullPointerException     when given null valued knownFailureDiagnostics argument
         * @throws IllegalArgumentException when given maxNumberOfRetries or millisecondsToSleepBetweenRetries arguments are less than zero
         */
        public RetryScheme(int maxNumberOfRetries, int millisecondsToSleepBetweenRetries, Set<String> knownFailureDiagnostics)
                throws NullPointerException, IllegalArgumentException {
            this.maxNumberOfRetries = InvariantUtil.checkIntLowerBoundOrThrow(maxNumberOfRetries, "maxNumberOfRetries", 0);
            this.millisecondsToSleepBetweenRetries = InvariantUtil.checkIntLowerBoundOrThrow(millisecondsToSleepBetweenRetries, "millisecondsToSleepBetweenRetries", 0);
            this.knownFailureDiagnostics = Collections.unmodifiableSet(InvariantUtil.checkNotNullOrThrow(knownFailureDiagnostics, "knownFailureDiagnostics"));
        }
    }

    /**
     * {@link ErrorSuppressor} class for suppressing certain failure diagnostics
     */
    private static class ErrorSuppressor {
        private final static List<SuppressedDiagnostic> suppressedDiagnostics = getSuppressedDiagnostics();

        /**
         * Tests whether or not given {@link Diagnostic} is to be suppressed
         *
         * @param diagnostic {@link Diagnostic} to test
         * @return true if matching known suppressed {@link Diagnostic}, otherwise false
         */
        boolean isSuppressed(Diagnostic diagnostic) {
            for (SuppressedDiagnostic suppressedDiagnostic : suppressedDiagnostics) {
                if (suppressedDiagnostic.isMatch(diagnostic)) {
                    return true;
                }
            }
            return false;
        }

        static List<SuppressedDiagnostic> getSuppressedDiagnostics() {
            final List<SuppressedDiagnostic> suppressedDiagnostics = new ArrayList<>();
            suppressedDiagnostics.add(getDeletingPpnsNotFound());
            suppressedDiagnostics.add(getPpnNotFound());
            return suppressedDiagnostics;
        }

        static SuppressedDiagnostic getDeletingPpnsNotFound() {
            final SuppressedDiagnostic suppressedDiagnostic = new SuppressedDiagnostic();
            suppressedDiagnostic.setUri("info:srw/diagnostic/12/61");
            suppressedDiagnostic.setMessage("Unspecified database error");
            suppressedDiagnostic.setDetailsPattern(
                    "SRU_RemoveLSN_Failures error from deleting ppns org\\.oclc\\.xwc\\.util\\.XWCException: PPN is not found in the database record\\.:Unspecified error\\(100\\)");
            return suppressedDiagnostic;
        }

        static SuppressedDiagnostic getPpnNotFound() {
            final SuppressedDiagnostic suppressedDiagnostic = new SuppressedDiagnostic();
            suppressedDiagnostic.setUri("info:srw/diagnostic/12/13");
            suppressedDiagnostic.setMessage("Invalid data structure: component rejected");
            suppressedDiagnostic.setDetailsPattern(
                    "SRU_RemoveLSN_Failures_No_LSN_Found\\. The PPN .*? was not found in the database record\\.:Unspecified error\\(100\\)");
            return suppressedDiagnostic;
        }

        private static class SuppressedDiagnostic extends Diagnostic {
            Pattern detailsPattern = null;

            void setDetailsPattern(String regex) {
                if (regex != null) {
                    this.detailsPattern = Pattern.compile(regex, Pattern.MULTILINE);
                }
            }

            boolean isMatch(Diagnostic diagnostic) {
                return uri.equals(diagnostic.getUri())
                        && message.equals(diagnostic.getMessage())
                        && detailsPattern != null
                        && detailsPattern.matcher(diagnostic.getDetails()).find();
            }
        }
    }

    /**
     * class constructor
     *
     * @param baseUrl     web service base URL on the form "http(s)://host:port/path"
     * @param userId      user ID for authentication
     * @param password    password for authentication
     * @param projectId   an OCLC defined identifier used to associate incoming requests with a WCIRU inbound profile
     * @param retryScheme retry scheme for failed operations
     * @throws NullPointerException     if passed any null valued arguments
     * @throws IllegalArgumentException if passed any empty String arguments
     */
    public WciruServiceConnector(String baseUrl, String userId, String password, String projectId, RetryScheme retryScheme) {
        this(new UpdateService(), baseUrl, userId, password, projectId, retryScheme);
    }

    WciruServiceConnector(UpdateService service, String baseUrl, String userId, String password, String projectId, RetryScheme retryScheme) {
        this.baseUrl = InvariantUtil.checkNotNullNotEmptyOrThrow(baseUrl, "baseurl");
        this.projectId = InvariantUtil.checkNotNullNotEmptyOrThrow(projectId, "projectId");
        this.authenticationToken = InvariantUtil.checkNotNullNotEmptyOrThrow(userId, "userId") +
                "/" + InvariantUtil.checkNotNullNotEmptyOrThrow(password, "password");
        this.retryScheme = InvariantUtil.checkNotNullOrThrow(retryScheme, "retryScheme");
        this.proxy = this.getProxy(InvariantUtil.checkNotNullOrThrow(service, "service"));
        this.errorSuppressor = new ErrorSuppressor();
    }

    /**
     * Adds or updates record in WorldCat.
     * <p>
     * Note: the caller of this method does not need to relate to whether
     * the given record results in an add or an update operation at the
     * remote endpoint, this is all handled transparently by the method
     * call.
     *
     * @param record        bibliographic record representation
     * @param holdingSymbol client holding symbol
     * @param oclcId        OCLC defined id, where a non-null value indicates an update
     * @return web service response object
     * @throws WciruServiceConnectorException when unable to add record data
     */
    public UpdateResponseType addOrUpdateRecord(Element record, String holdingSymbol, String oclcId)
            throws WciruServiceConnectorException, NullPointerException, IllegalArgumentException {
        final UpdateRequestType updateRequest = new UpdateRequestType();
        updateRequest.setAction(CREATE_ACTION);
        updateRequest.setVersion(SRW_VERSION);
        if (oclcId != null && !oclcId.isEmpty()) {
            updateRequest.setRecordIdentifier(oclcId);
        }
        updateRequest.setRecord(buildRecord(record));
        updateRequest.setExtraRequestData(buildExtraRequestData(holdingSymbol, "I"));
        return executeRequest(updateRequest);
    }

    /**
     * Adds or updates record in WorldCat.
     * <p>
     * Note: the caller of this method does not need to relate to whether
     * the given record results in an add or an update operation at the
     * remote endpoint, this is all handled transparently by the method
     * call.
     *
     * @param record        bibliographic record representation
     * @param holdingSymbol client holding symbol
     * @param oclcId        OCLC defined id, where a non-null value indicates an update
     * @return web service response object
     * @throws WciruServiceConnectorException when unable to add record data
     * @throws NullPointerException           if passed null valued record argument
     * @throws IllegalArgumentException       if passed empty record or holdingSymbol argument
     */
    public UpdateResponseType addOrUpdateRecord(String record, String holdingSymbol, String oclcId)
            throws WciruServiceConnectorException, NullPointerException, IllegalArgumentException {
        Element recordNode;
        try {
            // We convert the XML fragment string to a DOM element to
            // avoid double encodings in request.
            recordNode = JaxpUtil.parseDocument(record).getDocumentElement();
        } catch (IOException | SAXException e) {
            throw new WciruServiceConnectorException(String.format("Unable to handle XML fragment '%s'", record), e);
        }
        return addOrUpdateRecord(recordNode, holdingSymbol, oclcId);
    }

    /**
     * Replaces record in WorldCat
     *
     * @param record        bibliographic record representation
     * @param oclcId        OCLC defined id of record to replace
     * @param holdingSymbol valid OCLC symbol
     * @param holdingAction "I" for insertion or "D" for deletion
     * @return web service response object
     * @throws WciruServiceConnectorException when unable to add record data
     * @throws NullPointerException           if passed null valued argument
     * @throws IllegalArgumentException       if passed empty valued argument or invalid holding action
     */
    public UpdateResponseType replaceRecord(Element record, String oclcId, String holdingSymbol, String holdingAction)
            throws WciruServiceConnectorException, NullPointerException, IllegalArgumentException {
        final UpdateRequestType updateRequest = new UpdateRequestType();
        updateRequest.setAction(REPLACE_ACTION);
        updateRequest.setVersion(SRW_VERSION);
        updateRequest.setRecordIdentifier(oclcId);
        updateRequest.setRecord(buildRecord(record));
        updateRequest.setExtraRequestData(buildExtraRequestData(holdingSymbol, holdingAction));
        return executeRequest(updateRequest);
    }

    /**
     * Deletes a record in WorldCat
     *
     * @param record bibliographic record representation
     * @param oclcId OCLC defined id of record to delete
     * @return web service response object
     * @throws WciruServiceConnectorException When unable to delete record
     * @throws NullPointerException           if passed null valued argument
     * @throws IllegalArgumentException       if passed empty valued argument
     */
    public UpdateResponseType deleteRecord(Element record, String oclcId)
            throws WciruServiceConnectorException, NullPointerException, IllegalArgumentException {
        final UpdateRequestType updateRequest = new UpdateRequestType();
        updateRequest.setAction(DELETE_ACTION);
        updateRequest.setVersion(SRW_VERSION);
        updateRequest.setRecordIdentifier(oclcId);
        updateRequest.setRecord(buildRecord(record));
        updateRequest.setExtraRequestData(buildExtraRequestData());
        return executeRequest(updateRequest);
    }

    private UpdateResponseType executeRequest(UpdateRequestType updateRequest) throws WciruServiceConnectorException {
        // This next part is a change to the previous version of the code.
        // The purpose is to have a way to differentiate behaviour given different
        // diagnostics in the response.
        // If a response contain a known diagnostic, then the request should be retried
        // up to a predetermined (configurable) number of times - with a sleep period
        // between retries..
        // If, after the given number of retries, a known diagnostic still occurs,
        // then a WciruServiceConnectorRetryException should be thrown.
        // If an unknown diagnostic is returned in the response, then a WciruServiceConnectorException
        // should be thrown.
        // If no diagnostic is returned in the response, then no exceptions
        // should be thrown and the code should continue the normal flow.

        int retryCounter = 0;
        UpdateResponseType response;
        do {
            response = proxy.update(updateRequest);
            if (OperationStatusType.FAIL != response.getOperationStatus()) {
                break;
            }
            // OperationStatusType.FAIL so a diagnostic is present
            final Diagnostic diagnostic = response.getDiagnostics().getDiagnostic().get(0);
            LOGGER.info("Diagnostic uri: {}", diagnostic.getUri());

            if (errorSuppressor.isSuppressed(diagnostic)) {
                break;
            }

            if (!retryScheme.knownFailureDiagnostics.contains(diagnostic.getUri())) {
                throw new WciruServiceConnectorException(
                        "Unknown Diagnostic in response: " + diagnostic.getUri(),
                        diagnostic);
            }
            if (retryCounter >= retryScheme.maxNumberOfRetries) {
                throw new WciruServiceConnectorRetryException(
                        "Maximum number of retries reached",
                        diagnostic, retryScheme.maxNumberOfRetries);
            }
            retryCounter++;
            try {
                Thread.sleep(retryScheme.millisecondsToSleepBetweenRetries);
            } catch (InterruptedException ex) {
                LOGGER.warn("Interrupted exception was caught during sleep", ex);
            }
        } while (retryCounter <= retryScheme.maxNumberOfRetries);

        return response;
    }

    private UpdateInterface getProxy(UpdateService service) {
        final UpdateInterface proxy = service.getUpdate();

        // We don't want to rely on the endpoint from the WSDL
        final BindingProvider bindingProvider = (BindingProvider) proxy;
        bindingProvider.getRequestContext().put(BindingProvider.ENDPOINT_ADDRESS_PROPERTY, baseUrl);
        // setConnectTimeout() - 1 minute
        bindingProvider.getRequestContext().put("com.sun.xml.ws.connect.timeout", 1 * 60 * 1000);
        // setReadTimeout() - 3 minutes
        bindingProvider.getRequestContext().put("com.sun.xml.ws.request.timeout", 3 * 60 * 1000);

        return proxy;
    }

    /* Builds extra request data object containing authentication token
     * and project ID and editReplace element with holding
     */
    private ExtraRequestDataType buildExtraRequestData(String holdingSymbol, String holdingAction)
            throws NullPointerException, IllegalArgumentException {
        if (!(holdingAction.equals("I") || holdingAction.equals("D"))) {
            throw new IllegalArgumentException(String.format("Invalid holding action '%s'", holdingAction));
        }
        final ExtraRequestDataType extraRequestData = buildExtraRequestData();
        final EditReplaceType editReplace = new EditReplaceType();
        editReplace.setDataIdentifier("Holdings");
        editReplace.setEditReplaceType(holdingAction);
        if (holdingAction.equals("I")) {
            editReplace.setNewValue(holdingSymbol);
        } else if (holdingAction.equals("D")) {
            editReplace.setOldValue(holdingSymbol);
        }
        extraRequestData.setEditReplace(editReplace);
        return extraRequestData;
    }

    /* Builds extra request data object containing authentication token
     * and project ID
     */
    private ExtraRequestDataType buildExtraRequestData() {
        final ExtraRequestDataType extraRequestData = new ExtraRequestDataType();
        extraRequestData.setAuthenticationToken(authenticationToken);
        extraRequestData.setProjectid(projectId);
        return extraRequestData;
    }

    /* Builds record object from XML fragment given as Element
     */
    private RecordType buildRecord(Element xmlFragment) throws WciruServiceConnectorException {
        final StringOrXmlFragment recordData = new StringOrXmlFragment();
        recordData.getContent().add(xmlFragment);
        final RecordType record = new RecordType();
        record.setRecordPacking(RECORD_PACKING);
        record.setRecordSchema(RECORD_SCHEMA);
        record.setRecordData(recordData);
        return record;
    }
}
