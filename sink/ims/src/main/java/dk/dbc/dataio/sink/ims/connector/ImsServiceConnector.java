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

package dk.dbc.dataio.sink.ims.connector;

import dk.dbc.dataio.commons.utils.invariant.InvariantUtil;
import dk.dbc.oss.ns.updatemarcxchange.MarcXchangeRecord;
import dk.dbc.oss.ns.updatemarcxchange.UpdateMarcXchangePortType;
import dk.dbc.oss.ns.updatemarcxchange.UpdateMarcXchangeRequest;
import dk.dbc.oss.ns.updatemarcxchange.UpdateMarcXchangeResult;
import dk.dbc.oss.ns.updatemarcxchange.UpdateMarcXchangeServices;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.xml.ws.BindingProvider;
import javax.xml.ws.WebServiceException;
import java.util.List;

public class ImsServiceConnector {
    private static final Logger LOGGER = LoggerFactory.getLogger(ImsServiceConnector.class);
    public static final String CONNECT_TIMEOUT_PROPERTY = "com.sun.xml.ws.connect.timeout";
    public static final String REQUEST_TIMEOUT_PROPERTY = "com.sun.xml.ws.request.timeout";
    public static final int CONNECT_TIMEOUT_DEFAULT_IN_MS =  1 * 60 * 1000;    // 1 minute
    public static final int REQUEST_TIMEOUT_DEFAULT_IN_MS =  3 * 60 * 1000;    // 3 minutes

    private final String endpoint;

    /* JAX-WS class generated from WSDL */
    private final UpdateMarcXchangeServices services;

    public ImsServiceConnector(String endpoint) throws NullPointerException, IllegalArgumentException {
        this(new UpdateMarcXchangeServices(), endpoint);
    }

    ImsServiceConnector(UpdateMarcXchangeServices services, String endpoint)
            throws NullPointerException, IllegalArgumentException {
        this.services = InvariantUtil.checkNotNullOrThrow(services, "services");
        this.endpoint = InvariantUtil.checkNotNullNotEmptyOrThrow(endpoint, "endpoint");
    }

    /** todo make doc corrent
     * Calls updateMarcXchange operation of the ims Web service
     * @param trackingId unique ID for each chunk within the job
     * @return list containing UpdateMarcXchangeResults
     * @throws WebServiceException on failure communicating with the ims web service
     */
    public List<UpdateMarcXchangeResult> updateMarcXchange(String trackingId, List<MarcXchangeRecord> marcXchangeRecords) throws WebServiceException {
        LOGGER.trace("Using endpoint: {}", endpoint);
        final UpdateMarcXchangeRequest updateMarcXchangeRequest = new UpdateMarcXchangeRequest();
        updateMarcXchangeRequest.setTrackingId(trackingId);
        updateMarcXchangeRequest.getMarcXchangeRecord().addAll(marcXchangeRecords);
        return getProxy().updateMarcXchange(updateMarcXchangeRequest);
    }

    /*
     * Private methods
     */

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
}
