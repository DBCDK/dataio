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

package dk.dbc.dataio.openagency;

import dk.dbc.dataio.commons.utils.invariant.InvariantUtil;
import dk.dbc.oss.ns.openagency.Information;
import dk.dbc.oss.ns.openagency_wsdl.OpenAgencyPortType;
import dk.dbc.oss.ns.openagency_wsdl.OpenAgencyService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.xml.ws.BindingProvider;

public class OpenAgencyConnector {
    private static final Logger LOGGER = LoggerFactory.getLogger(OpenAgencyConnector.class);

    private static final String CONNECT_TIMEOUT_PROPERTY   = "com.sun.xml.ws.connect.timeout";
    private static final String REQUEST_TIMEOUT_PROPERTY   = "com.sun.xml.ws.request.timeout";
    private static final int CONNECT_TIMEOUT_DEFAULT_IN_MS = 30 * 1000;     // 30 seconds
    private static final int REQUEST_TIMEOUT_DEFAULT_IN_MS = 30 * 1000;     // 30 seconds

    private final String endpoint;

    /* JAX-WS class generated from WSDL */
    private final OpenAgencyService service;

    /**
     * Constructor for Open Agency web service connector
     * @param endpoint web service endpoint
     * @throws NullPointerException if given null-valued endpoint argument
     * @throws IllegalArgumentException if given empty-valued endpoint argument
     */
    public OpenAgencyConnector(String endpoint) throws NullPointerException, IllegalArgumentException {
        this(new OpenAgencyService(), endpoint);
    }

    OpenAgencyConnector(OpenAgencyService openAgencyService, String endpoint) throws NullPointerException, IllegalArgumentException {
        this.service = InvariantUtil.checkNotNullOrThrow(openAgencyService, "service");
        this.endpoint = InvariantUtil.checkNotNullNotEmptyOrThrow(endpoint, "endpoint");
        LOGGER.info("Using endpoint: {}", endpoint);
    }

    public Information getAgencyInformation(long agencyId) {
        return getProxy().service(null).getInformation();
    }

    private OpenAgencyPortType getProxy() {
        // getOpenAgencyPortType() calls getPort() which is not thread safe.
        // Therefore, we cannot let the proxy be application scoped.
        // If performance is lacking we should consider options for reuse.
        final OpenAgencyPortType proxy = service.getOpenAgencyPortType();

        // We don't want to rely on the endpoint from the WSDL
        final BindingProvider bindingProvider = (BindingProvider)proxy;
        bindingProvider.getRequestContext().put(BindingProvider.ENDPOINT_ADDRESS_PROPERTY, endpoint);
        // FixMe: timeouts should be made configurable
        bindingProvider.getRequestContext().put(CONNECT_TIMEOUT_PROPERTY, CONNECT_TIMEOUT_DEFAULT_IN_MS);
        bindingProvider.getRequestContext().put(REQUEST_TIMEOUT_PROPERTY, REQUEST_TIMEOUT_DEFAULT_IN_MS);

        return proxy;
    }
}
