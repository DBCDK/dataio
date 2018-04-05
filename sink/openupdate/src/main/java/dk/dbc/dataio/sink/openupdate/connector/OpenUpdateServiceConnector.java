/*
 * DataIO - Data IO
 * Copyright (C) 2015 Dansk Bibliotekscenter a/s, Tempovej 7-11, DK-2750 Ballerup,
 * Denmark. CVR: 15149043
 *
 * This file is part of DataIO.
 *
 * DataIO is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * DataIO is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with DataIO.  If not, see <http://www.gnu.org/licenses/>.
 */

package dk.dbc.dataio.sink.openupdate.connector;

import dk.dbc.dataio.commons.types.Constants;
import dk.dbc.invariant.InvariantUtil;
import dk.dbc.oss.ns.catalogingupdate.Authentication;
import dk.dbc.oss.ns.catalogingupdate.BibliographicRecord;
import dk.dbc.oss.ns.catalogingupdate.CatalogingUpdatePortType;
import dk.dbc.oss.ns.catalogingupdate.Options;
import dk.dbc.oss.ns.catalogingupdate.UpdateOptionEnum;
import dk.dbc.oss.ns.catalogingupdate.UpdateRecordRequest;
import dk.dbc.oss.ns.catalogingupdate.UpdateRecordResult;
import dk.dbc.oss.ns.catalogingupdate.UpdateService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.xml.ws.BindingProvider;

/**
 * Update web service connector.
 * Instances of this class are NOT thread safe.
 */
public class OpenUpdateServiceConnector {
    private static final Logger LOGGER = LoggerFactory.getLogger(OpenUpdateServiceConnector.class);

    private static final String CONNECT_TIMEOUT_PROPERTY   = "com.sun.xml.ws.connect.timeout";
    private static final String REQUEST_TIMEOUT_PROPERTY   = "com.sun.xml.ws.request.timeout";
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
     * @param endpoint web service endpoint base URL on the form "http(s)://host:port/path"
     * @throws NullPointerException if passed null valued {@code endpoint}
     * @throws IllegalArgumentException if passed empty valued {@code endpoint}
     */
    public OpenUpdateServiceConnector(String endpoint)
            throws NullPointerException, IllegalArgumentException {
        this(endpoint, "", "");
    }

    /**
     * Class constructor
     * @param endpoint web service endpoint base URL on the form "http(s)://host:port/path"
     * @param userName for authenticating any user requiring access to the webservice
     * @param password for authenticating any user requiring access to the webservice
     * @throws NullPointerException if passed any null valued argument
     * @throws IllegalArgumentException if passed empty valued {@code endpoint}
     */
    public OpenUpdateServiceConnector(String endpoint, String userName, String password)
            throws NullPointerException, IllegalArgumentException {
        this(new UpdateService(), endpoint, userName, password);
    }

    /**
     * Class constructor
     * @param service web service client view of the CatalogingUpdate Web service
     * @param endpoint web service endpoint base URL on the form "http(s)://host:port/path"
     * @param userName for authenticating any user requiring access to the webservice
     * @param password for authenticating any user requiring access to the webservice
     * @throws NullPointerException if passed any null valued argument
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
        if(validateOnlyEnv != null && !validateOnly) {
            LOGGER.warn("{} had unexpected value {}",
                Constants.UPDATE_VALIDATE_ONLY_FLAG, validateOnlyEnv);
        }
    }

    /**
     * Calls updateRecord operation of the Open Update Web service
     * @param groupId group id used for authorization
     * @param schemaName the template towards which the validation should be performed
     * @param bibliographicRecord containing the MarcXChange to validate
     * @param trackingId unique ID for each OpenUpdate request
     * @return UpdateRecordRequest instance
     * @throws NullPointerException if passed any null valued {@code template} or {@code bibliographicRecord} argument
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
     * Builds an UpdateRecordRequest
     * @param groupId group id used for authorization
     * @param schemaName the template towards which the validation should be performed
     * @param bibliographicRecord containing the MarcXChange to validate
     * @param trackingId unique ID for each OpenUpdate request
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
        if(validateOnly) {
            if(updateRecordRequest.getOptions() == null) {
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
        BindingProvider bindingProvider = (BindingProvider)proxy;
        bindingProvider.getRequestContext().put(BindingProvider.ENDPOINT_ADDRESS_PROPERTY, endpoint);
        // FixMe: timeouts should be made configurable
        bindingProvider.getRequestContext().put(CONNECT_TIMEOUT_PROPERTY, CONNECT_TIMEOUT_DEFAULT_IN_MS);
        bindingProvider.getRequestContext().put(REQUEST_TIMEOUT_PROPERTY, REQUEST_TIMEOUT_DEFAULT_IN_MS);

        return proxy;
    }
}
