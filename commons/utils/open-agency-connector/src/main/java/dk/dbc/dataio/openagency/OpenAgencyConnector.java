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

import dk.dbc.dataio.commons.time.StopWatch;
import dk.dbc.dataio.commons.utils.invariant.InvariantUtil;
import dk.dbc.oss.ns.openagency.ErrorType;
import dk.dbc.oss.ns.openagency.Information;
import dk.dbc.oss.ns.openagency.LibraryRule;
import dk.dbc.oss.ns.openagency.LibraryRules;
import dk.dbc.oss.ns.openagency.LibraryRulesRequest;
import dk.dbc.oss.ns.openagency.LibraryRulesResponse;
import dk.dbc.oss.ns.openagency.ServiceRequest;
import dk.dbc.oss.ns.openagency.ServiceResponse;
import dk.dbc.oss.ns.openagency.ServiceType;
import dk.dbc.oss.ns.openagency_wsdl.OpenAgencyPortType;
import dk.dbc.oss.ns.openagency_wsdl.OpenAgencyService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.xml.ws.BindingProvider;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

public class OpenAgencyConnector {
    private static final Logger LOGGER = LoggerFactory.getLogger(OpenAgencyConnector.class);

    private static final String CONNECT_TIMEOUT_PROPERTY   = "com.sun.xml.ws.connect.timeout";
    private static final String REQUEST_TIMEOUT_PROPERTY   = "com.sun.xml.ws.request.timeout";
    private static final int CONNECT_TIMEOUT_DEFAULT_IN_MS = 30 * 1000;     // 30 seconds
    private static final int REQUEST_TIMEOUT_DEFAULT_IN_MS = 30 * 1000;     // 30 seconds

    private static final LibraryRulesRequest fbsImsLibrariesRequest = getFbsImsLibrariesRequest();

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
        String baseurl = InvariantUtil.checkNotNullNotEmptyOrThrow(endpoint, "endpoint");
        if (!baseurl.endsWith("/")) {
            baseurl += "/";
        }
        this.endpoint = baseurl;
        LOGGER.info("Using endpoint: {}", endpoint);
    }

    /**
     * Retrieves agency information for given agency ID
     * @param agencyId agency ID
     * @return agency information or empty if agency ID could not be found
     * @throws OpenAgencyConnectorException in case of JAX-WS API runtime exception or service error
     */
    public Optional<Information> getAgencyInformation(long agencyId) throws OpenAgencyConnectorException {
        final StopWatch stopWatch = new StopWatch();
        try {
            final ServiceRequest serviceRequest = new ServiceRequest();
            serviceRequest.setAgencyId(Long.toString(agencyId));
            serviceRequest.setService(ServiceType.INFORMATION);
            final ServiceResponse serviceResponse = getProxy().service(serviceRequest);

            final Information information = serviceResponse.getInformation();
            if (information != null) {
                return Optional.of(information);
            }

            final ErrorType error = serviceResponse.getError();
            if (error != null && error == ErrorType.AGENCY_NOT_FOUND) {
                return Optional.empty();
            }

            throw new OpenAgencyConnectorException(
                    "Information retrieval for agency ID " + agencyId + " returned error " + error);

        } catch (RuntimeException e) {
           throw new OpenAgencyConnectorException(
                   "Exception caught during information retrieval for agency ID " + agencyId, e);
        } finally {
            LOGGER.info("getAgencyInformation() operation took {} ms", stopWatch.getElapsedTime());
        }
    }

    /**
     * Retrieves library rules for given agency ID
     * @param agencyId agency ID
     * @param trackingId tracking ID, can be null
     * @return library rules or empty if agency ID could not be found
     * @throws OpenAgencyConnectorException in case of JAX-WS API runtime exception or service error
     */
    public Optional<LibraryRules> getLibraryRules(long agencyId, String trackingId) throws OpenAgencyConnectorException {
        final StopWatch stopWatch = new StopWatch();
        try {
            final LibraryRulesRequest libraryRulesRequest = new LibraryRulesRequest();
            libraryRulesRequest.setAgencyId(Long.toString(agencyId));
            if (trackingId != null) {
                libraryRulesRequest.setTrackingId(trackingId);
            }
            final LibraryRulesResponse libraryRulesResponse = getProxy().libraryRules(libraryRulesRequest);

            final List<LibraryRules> libraryRules = libraryRulesResponse.getLibraryRules();
            if (libraryRules != null && !libraryRules.isEmpty()) {
                return Optional.of(libraryRules.get(0));
            }

            final ErrorType error = libraryRulesResponse.getError();
            if (error == null) {
                return Optional.empty();
            }

            throw new OpenAgencyConnectorException(
                    "Library rules retrieval for agency ID " + agencyId + " returned error " + error);

        } catch (RuntimeException e) {
           throw new OpenAgencyConnectorException(
                   "Exception caught during library rules retrieval for agency ID " + agencyId, e);
        } finally {
            LOGGER.info("getLibraryRules() operation took {} ms", stopWatch.getElapsedTime());
        }
    }

    /**
     * Retrieves set of FBS IMS agency IDs
     * @return set of FBS IMS agency IDs
     * @throws OpenAgencyConnectorException in case of JAX-WS API runtime exception or service error
     */
    public Set<Integer> getFbsImsLibraries() throws OpenAgencyConnectorException {
        final StopWatch stopWatch = new StopWatch();
        try {
            final LibraryRulesResponse libraryRulesResponse = getProxy().libraryRules(fbsImsLibrariesRequest);

            final ErrorType error = libraryRulesResponse.getError();
            if (error != null) {
                throw new OpenAgencyConnectorException("FBS IMS libraries retrieval returned error " + error);
            }

            return libraryRulesResponse.getLibraryRules().stream()
                    .map(LibraryRules::getAgencyId)
                    .map(Integer::valueOf)
                    .collect(Collectors.toSet());

        } catch (RuntimeException e) {
           throw new OpenAgencyConnectorException("Exception caught during FBS IMS libraries retrieval", e);
        } finally {
            LOGGER.info("getFbsImsLibraries() operation took {} ms", stopWatch.getElapsedTime());
        }
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

    private static LibraryRulesRequest getFbsImsLibrariesRequest() {
        final LibraryRulesRequest libraryRulesRequest = new LibraryRulesRequest();
        final LibraryRule imsLibraryRule = new LibraryRule();
        imsLibraryRule.setName("ims_library");
        imsLibraryRule.setBool(true);
        libraryRulesRequest.getLibraryRule().add(imsLibraryRule);
        final LibraryRule createEnrichmentLibraryRule = new LibraryRule();
        createEnrichmentLibraryRule.setName("create_enrichments");
        createEnrichmentLibraryRule.setBool(true);
        libraryRulesRequest.getLibraryRule().add(createEnrichmentLibraryRule);
        return libraryRulesRequest;
    }
}
