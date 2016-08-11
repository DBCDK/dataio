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

package dk.dbc.dataio.harvester.rr;

import dk.dbc.dataio.commons.types.AddiMetaData.LibraryRules;
import dk.dbc.dataio.openagency.OpenAgencyConnector;
import dk.dbc.dataio.openagency.OpenAgencyConnectorException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Optional;

/**
 * This class is wrapper for OpenAgency web-service communication.
 * <p>
 * This class is not thread safe. For it to be made thread safe, the
 * internal caching mechanisms must be made thread safe also.
 * </p>
 */
public class AgencyConnection {
    private static final Logger LOGGER = LoggerFactory.getLogger(AgencyConnection.class);

    private final OpenAgencyConnector connector;
    private final HashMap<Integer, LibraryRules> libraryRulesCache;    // unbounded in-memory cache

    /**
     * Class constructor
     * @param endpoint OpenAgency web-service endpoint
     * @throws NullPointerException if given null-valued endpoint
     * @throws IllegalArgumentException if given empty-valued endpoint
     */
    public AgencyConnection(String endpoint) throws NullPointerException, IllegalArgumentException {
        this(new OpenAgencyConnector(endpoint));
    }

    /* Used for dependency injection during testing */
    AgencyConnection(OpenAgencyConnector connector) {
        this.connector = connector;
        this.libraryRulesCache = new HashMap<>();
    }

    /**
     * Retrieves library rules for given agency ID.
     * <p>
     * All non-null rule sets are cached. All subsequent requests for the same agency ID
     * will return the result from the cache.
     * </p>
     * @param agencyId agency ID
     * @param trackingId tracking ID used in web-service call, can be null
     * @return library rules or null if no rules could be found.
     * @throws IllegalStateException on error communicating with the OpenAgency web-service
     */
    public LibraryRules getLibraryRules(int agencyId, String trackingId) throws IllegalStateException {
        return libraryRulesCache.computeIfAbsent(agencyId, id -> {
            try {
                final Optional<dk.dbc.oss.ns.openagency.LibraryRules> response = connector.getLibraryRules(id, trackingId);
                if (response.isPresent()) {
                    final LibraryRules libraryRules = new LibraryRules();
                    final dk.dbc.oss.ns.openagency.LibraryRules objectToConvert = response.get();
                    objectToConvert.getLibraryRule()
                            .forEach(entry -> libraryRules.withLibraryRule(entry.getName(), entry.isBool()));
                    return libraryRules.withAgencyType(objectToConvert.getAgencyType());
                } else {
                    LOGGER.error("No library rules found for agency ID " + id);
                }
                return null;
            } catch (OpenAgencyConnectorException e) {
                throw new IllegalStateException("Error while looking up library rules for agency ID " + id, e);
            }
        });
    }
}
