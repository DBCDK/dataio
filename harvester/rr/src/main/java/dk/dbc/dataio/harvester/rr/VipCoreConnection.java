package dk.dbc.dataio.harvester.rr;

import dk.dbc.dataio.commons.types.AddiMetaData;
import dk.dbc.dataio.harvester.types.HarvesterException;
import dk.dbc.vipcore.exception.AgencyNotFoundException;
import dk.dbc.vipcore.exception.VipCoreException;
import dk.dbc.vipcore.libraryrules.VipCoreLibraryRulesConnector;
import dk.dbc.vipcore.marshallers.LibraryRules;

import java.util.HashMap;
import java.util.Set;
import java.util.stream.Collectors;

public class VipCoreConnection {
    private final VipCoreLibraryRulesConnector connector;
    private final HashMap<Integer, AddiMetaData.LibraryRules> libraryRulesCache;    // unbounded in-memory cache

    /**
     * Class constructor
     * @param connector instance of VipCoreLibraryRulesConnector
     * @throws NullPointerException if given null-valued endpoint
     * @throws IllegalArgumentException if given empty-valued endpoint
     */
    public VipCoreConnection(VipCoreLibraryRulesConnector connector) throws NullPointerException, IllegalArgumentException {
        if (connector == null) {
            throw new IllegalArgumentException("VipCoreLibraryRulesConnector can not be null");
        }
        this.connector = connector;
        this.libraryRulesCache = new HashMap<>();
    }

    /**
     * Retrieves library rules for given agency ID.
     * <p>
     * All non-null rule sets are cached. All subsequent requests for the same agency ID
     * will return the result from the cache.
     * </p>
     *
     * @param agencyId   agency ID
     * @param trackingId tracking ID used in web-service call, can be null
     * @return library rules or null if no rules could be found.
     * @throws IllegalStateException on error communicating with the vipcore web-service
     */
    public AddiMetaData.LibraryRules getLibraryRules(int agencyId, String trackingId) throws IllegalStateException {
        return libraryRulesCache.computeIfAbsent(agencyId, id -> {
            try {
                final LibraryRules libraryRules = connector.getLibraryRulesByAgencyId(Integer.toString(agencyId), trackingId);
                final AddiMetaData.LibraryRules addiMetaDatalibraryRules = new AddiMetaData.LibraryRules();
                libraryRules.getLibraryRule()
                        .forEach(entry -> addiMetaDatalibraryRules.withLibraryRule(entry.getName(),
                                entry.getBool() != null ? entry.getBool() : entry.getString()));
                return addiMetaDatalibraryRules.withAgencyType(libraryRules.getAgencyType());
            } catch (AgencyNotFoundException e) {
                return null;
            } catch (VipCoreException e) {
                throw new IllegalStateException("Error while looking up library rules for agency ID " + agencyId, e);
            }
        });
    }

    /**
     * Retrieves FBS IMS libraries
     *
     * @return set of agency IDs
     * @throws HarvesterException on error communicating with the vipcore web-service
     */
    public Set<Integer> getFbsImsLibraries() throws HarvesterException {
        try {
            return connector.getLibrariesByLibraryRule(VipCoreLibraryRulesConnector.Rule.IMS_LIBRARY, true).
                    stream().
                    map(Integer::parseInt).
                    collect(Collectors.toSet());
        } catch (VipCoreException e) {
            throw new HarvesterException("Error while looking up FBS IMS libraries", e);
        }
    }
}
