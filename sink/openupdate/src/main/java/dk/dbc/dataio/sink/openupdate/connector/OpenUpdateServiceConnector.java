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

import dk.dbc.dataio.commons.utils.invariant.InvariantUtil;
import dk.dbc.oss.ns.catalogingupdate.Authentication;
import dk.dbc.oss.ns.catalogingupdate.BibliographicRecord;
import dk.dbc.oss.ns.catalogingupdate.CatalogingUpdatePortType;
import dk.dbc.oss.ns.catalogingupdate.CatalogingUpdateServices;
import dk.dbc.oss.ns.catalogingupdate.UpdateRecordRequest;
import dk.dbc.oss.ns.catalogingupdate.UpdateRecordResult;

import javax.xml.ws.BindingProvider;

/**
 * Open Update web service connector
 */
public class OpenUpdateServiceConnector {

    private static final String CONNECT_TIMEOUT_PROPERTY   = "com.sun.xml.ws.connect.timeout";
    private static final String REQUEST_TIMEOUT_PROPERTY   = "com.sun.xml.ws.request.timeout";
    private static final int CONNECT_TIMEOUT_DEFAULT_IN_MS = 60 * 1000;    // 1 minute
    private static final int REQUEST_TIMEOUT_DEFAULT_IN_MS = 3 * 60 * 1000;    // 3 minutes

    //FixMe authentication values should not be visible in the code
    private static final String GROUP_ID = "010100";
    private static final String USER_ID  = "netpunkt";
    private static final String PASSWORD = "20Koster";

    private final String endpoint;
    /* JAX-WS class generated from WSDL */
    private final CatalogingUpdateServices services;

    /**
     * Class constructor
     * @param endpoint web service endpoint base URL on the form "http(s)://host:port/path"
     * @throws NullPointerException if passed any null valued {@code endpoint}
     * @throws IllegalArgumentException if passed empty valued {@code endpoint}
     */
    public OpenUpdateServiceConnector(String endpoint)
            throws NullPointerException, IllegalArgumentException {
        this(new CatalogingUpdateServices(), endpoint);
    }

    /**
     * Class constructor
     * @param services web service client view of the CatalogingUpdate Web service
     * @param endpoint web service endpoint base URL on the form "http(s)://host:port/path"
     * @throws NullPointerException if passed any null valued argument
     * @throws IllegalArgumentException if passed empty valued {@code endpoint}
     */
    OpenUpdateServiceConnector(CatalogingUpdateServices services, String endpoint)
            throws NullPointerException, IllegalArgumentException {
        this.services = InvariantUtil.checkNotNullOrThrow(services, "services");
        this.endpoint = InvariantUtil.checkNotNullNotEmptyOrThrow(endpoint, "endpoint");
    }

    /**
     * Calls updateRecord operation of the Open Update Web service
     * @param template the template towards which the validation should be performed
     * @param bibliographicRecord containing the MarcXChange to validate
     * @return UpdateRecordRequest instance
     * @throws NullPointerException if passed any null valued {@code template} or {@code bibliographicRecord} argument
     * @throws IllegalArgumentException if passed empty valued {@code template}
     */
    public UpdateRecordResult updateRecord(String template, BibliographicRecord bibliographicRecord)
            throws NullPointerException, IllegalArgumentException {
        InvariantUtil.checkNotNullNotEmptyOrThrow(template, "template");
        InvariantUtil.checkNotNullOrThrow(bibliographicRecord, "bibliographicRecord");

        final UpdateRecordRequest updateRecordRequest = buildUpdateRecordRequest(template, bibliographicRecord);
        return getProxy().updateRecord(updateRecordRequest);
    }

    /*
     * Private methods
     */


    /**
     * Builds an UpdateRecordRequest
     * @param schemaName the template towards which the validation should be performed
     * @param bibliographicRecord containing the MarcXChange to validate
     * @return a new updateRecordRequest containing schemeName and bibliographicRecord
     */
    private UpdateRecordRequest buildUpdateRecordRequest(String schemaName, BibliographicRecord bibliographicRecord) {
        UpdateRecordRequest updateRecordRequest = new UpdateRecordRequest();
        Authentication authentication = new Authentication();
        authentication.setGroupIdAut(GROUP_ID);
        authentication.setUserIdAut(USER_ID);
        authentication.setPasswordAut(PASSWORD);
        updateRecordRequest.setAuthentication(authentication);
        updateRecordRequest.setSchemaName(schemaName);
        updateRecordRequest.setBibliographicRecord(bibliographicRecord);
        return updateRecordRequest;
    }

    private CatalogingUpdatePortType getProxy() {
        // getCatalogingUpdatePort() calls getPort() which is not thread safe.
        // Therefore, we cannot let the proxy be application scoped.
        // If performance is lacking we should consider options for reuse.
        final CatalogingUpdatePortType proxy = services.getCatalogingUpdatePort();

        // We don't want to rely on the endpoint from the WSDL
        BindingProvider bindingProvider = (BindingProvider)proxy;
        bindingProvider.getRequestContext().put(BindingProvider.ENDPOINT_ADDRESS_PROPERTY, endpoint);
        // FixMe: timeouts should be made configurable
        bindingProvider.getRequestContext().put(CONNECT_TIMEOUT_PROPERTY, CONNECT_TIMEOUT_DEFAULT_IN_MS);
        bindingProvider.getRequestContext().put(REQUEST_TIMEOUT_PROPERTY, REQUEST_TIMEOUT_DEFAULT_IN_MS);

        return proxy;
    }

}
