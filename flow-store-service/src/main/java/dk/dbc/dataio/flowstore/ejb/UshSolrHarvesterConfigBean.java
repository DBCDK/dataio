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

package dk.dbc.dataio.flowstore.ejb;

import com.fasterxml.jackson.databind.type.CollectionType;
import dk.dbc.dataio.commons.utils.ush.UshHarvesterConnectorException;
import dk.dbc.dataio.commons.utils.ush.ejb.UshHarvesterConnectorBean;
import dk.dbc.dataio.flowstore.FlowStoreException;
import dk.dbc.dataio.flowstore.entity.HarvesterConfig;
import dk.dbc.dataio.flowstore.rest.PersistenceExceptionMapper;
import dk.dbc.dataio.harvester.types.UshHarvesterProperties;
import dk.dbc.dataio.harvester.types.UshSolrHarvesterConfig;
import dk.dbc.dataio.jsonb.JSONBContext;
import dk.dbc.dataio.jsonb.JSONBException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Resource;
import javax.ejb.EJB;
import javax.ejb.SessionContext;
import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.persistence.EntityManager;
import javax.persistence.EntityNotFoundException;
import javax.persistence.PersistenceContext;
import javax.persistence.PersistenceException;
import javax.persistence.Query;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;


@Stateless
public class UshSolrHarvesterConfigBean {

    @Resource
    SessionContext sessionContext;

    @EJB
    UshHarvesterConnectorBean ushHarvesterConnectorBean;

    @PersistenceContext
    EntityManager entityManager;


    JSONBContext jsonbContext = new JSONBContext();
    private static final Logger LOGGER = LoggerFactory.getLogger(UshSolrHarvesterConfigBean.class);

    /**
     * Retrieves all ushHarvesterProperties present in the universal search system and all ushSolrHarvester configs.
     * present in flow store.
     *  - New UshSolrHarvesterConfigs are created if absent in flow store but present in the universal search system.
     *  - Existing UshSolrHarvesterConfigs are deleted if present in flow store but absent in the universal search system.
     *
     * @return list of harvesterConfigs, mirroring the induced synchronization between the universal search system and flow store.
     * @throws FlowStoreException on failure
     */
    public List<HarvesterConfig> findAllAndSyncWithUsh() throws FlowStoreException {
        try {
            // Retrieve ushHarvesterProperties from the universal search system
            final List<UshHarvesterProperties> existingUshHarvesterProperties = ushHarvesterConnectorBean.getConnector().listUshHarvesterJobs();

            // Retrieve ushHarvesterConfigs from flow store
            final Map<Integer, UshSolrHarvesterConfig> indexedUshSolrHarvesterConfigs = getIndexedUshSolrHarvesterConfigs();

            // Synchronize with the universal search system
            final List<UshSolrHarvesterConfig> updatedUshSolrHarvesterConfigs = syncWithUsh(existingUshHarvesterProperties, indexedUshSolrHarvesterConfigs);

            // return list of harvesterConfigs each enriched with corresponding UshHarvesterProperties
            return updatedUshSolrHarvesterConfigs.stream().map(this::toHarvesterConfig).collect(Collectors.toList());
        } catch (UshHarvesterConnectorException | RuntimeException | JSONBException e) {
            throw new FlowStoreException("Error occurred while retrieving harvesterConfigs", e);
        }
    }

    /**
     * Compares ushHarvesterProperties from the universal search system with UshSolrHarvesterConfigs created in flow store.
     *
     * If an ushHarvesterProperty does not have a matching UshSolrHarvesterConfig in flow store:
     *      A new UshSolrHarvesterConfig is created with name as in the universal search system and with ushHarvesterJobId referencing
     *      the UshHarvesterProperties.id.
     *      The newly created UshSolrHarvesterConfig is persisted without UshHarvesterProperties, but is enriched with
     *      matching UshHarvesterProperties before being added to the result list.
     *
     * Otherwise the existing UshSolrHarvesterConfig enriched with matching UshHarvesterProperties and added to the result list.
     *
     * Existing UshSolrHarvesterConfigs are deleted if present in flow store but absent in the universal search system.
     *
     * @param existingUshHarvesterProperties present in the universal search system
     * @param indexedUshSolrHarvesterConfigs present in flow store
     * @return updatedUshSolrHarvesterConfigs enriched with corresponding ushHarvesterProperties
     *
     * @throws FlowStoreException on PersistenceException
     * @throws JSONBException on marshalling failure
     */
    private List<UshSolrHarvesterConfig> syncWithUsh(
            List<UshHarvesterProperties> existingUshHarvesterProperties,
            Map<Integer, UshSolrHarvesterConfig> indexedUshSolrHarvesterConfigs) throws FlowStoreException, JSONBException {

        final int numberOfExistingUshHarvesterConfigs = indexedUshSolrHarvesterConfigs.size();
        final List<UshSolrHarvesterConfig> updatedUshSolrHarvesterConfigs = new ArrayList<>();

        // Check if UshSolrHarvester configuration is present in flow store for each ushHarvesterProperties
        for(UshHarvesterProperties ushHarvesterProperties : existingUshHarvesterProperties) {
            final UshSolrHarvesterConfig ushSolrHarvesterConfig;

            if (indexedUshSolrHarvesterConfigs.containsKey(ushHarvesterProperties.getId())) {
                ushSolrHarvesterConfig = indexedUshSolrHarvesterConfigs.get(ushHarvesterProperties.getId());
            } else {
                // Create new UshSolrHarvesterConfig since absent in flow store but present in the universal search system
                HarvesterConfig harvesterConfig = createIfAbsentInFlowStore(ushHarvesterProperties);
                ushSolrHarvesterConfig = jsonbContext.unmarshall(jsonbContext.marshall(harvesterConfig), UshSolrHarvesterConfig.class);
            }
            ushSolrHarvesterConfig.getContent().withUshHarvesterProperties(ushHarvesterProperties);
            updatedUshSolrHarvesterConfigs.add(ushSolrHarvesterConfig);
            indexedUshSolrHarvesterConfigs.remove(ushHarvesterProperties.getId());
        }

        // Delete existing any UshSolrHarvesterConfig from flow store since absent in the universal search system
        if(!indexedUshSolrHarvesterConfigs.isEmpty()) {
            if(existingUshHarvesterProperties.isEmpty() && indexedUshSolrHarvesterConfigs.size() == numberOfExistingUshHarvesterConfigs) {
                throw new FlowStoreException("Flowstore panic: Attempt to delete all existing ushSolrHarvesterConfigs detected");
            }
            deleteIfAbsentInUsh(indexedUshSolrHarvesterConfigs);
        }
        return updatedUshSolrHarvesterConfigs;
    }

    /**
     * Attempts to create a new HarvesterConfig in flow store
     * @param ushHarvesterProperties containing values to map to new HarvesterConfig
     * @return newly created HarvesterConfig if no unique constraint violation, otherwise existing HarvesterConfig
     *
     * @throws FlowStoreException if a PersistenceException occurs, that is not caused by unique constraint violation
     * @throws JSONBException on marshalling failure
     */
    HarvesterConfig createIfAbsentInFlowStore(UshHarvesterProperties ushHarvesterProperties) throws FlowStoreException, JSONBException {
        try {
            return self().tryCreate(toHarvesterConfig(ushHarvesterProperties));
        } catch (PersistenceException e) {
            if (e.getMessage() != null) {
                final String message = e.getMessage().toLowerCase();
                if (message.contains(PersistenceExceptionMapper.UNIQUE_CONSTRAINT_VIOLATION)) {
                    return findUshHarvesterConfigByUshHarvesterJobId(ushHarvesterProperties.getId());
                }
            }
            throw new FlowStoreException(e.getMessage());
        }
    }

    @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
    public HarvesterConfig tryCreate(HarvesterConfig notYetPersisted) {
        notYetPersisted.withType(UshSolrHarvesterConfig.class.getName());
        entityManager.persist(notYetPersisted);
        entityManager.flush();
        return notYetPersisted;
    }


    /**
     * Deletes orphaned ushSolrHarvesterConfigs in flow store
     * @param indexedUshSolrHarvesterConfigs containing the orphaned UshSolrHarvesterConfigs
     */
    void deleteIfAbsentInUsh(Map<Integer, UshSolrHarvesterConfig> indexedUshSolrHarvesterConfigs) {
        for (Map.Entry<Integer, UshSolrHarvesterConfig> entry : indexedUshSolrHarvesterConfigs.entrySet()) {
            self().tryDelete(toHarvesterConfig(entry.getValue()));
        }
    }


    @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
    public void tryDelete(HarvesterConfig persisted) {
        // Find harvester config to make sure it has not previously been deleted
        HarvesterConfig harvesterConfig = entityManager.find(HarvesterConfig.class, persisted.getId());
        if(harvesterConfig != null) {
            entityManager.remove(harvesterConfig);
        }
    }

    /*
     * Helper methods
     */

    private HarvesterConfig toHarvesterConfig(UshSolrHarvesterConfig ushSolrHarvesterConfig) {
        try {
            return new HarvesterConfig()
                    .withType(UshSolrHarvesterConfig.class.getName())
                    .withContent(jsonbContext.marshall(ushSolrHarvesterConfig.getContent()))
                    .withId(ushSolrHarvesterConfig.getId())
                    .withVersion(ushSolrHarvesterConfig.getVersion());
        } catch (JSONBException e) {
            throw new IllegalStateException(e);
        }
    }

    private UshSolrHarvesterConfigBean self() {
        return sessionContext.getBusinessObject(UshSolrHarvesterConfigBean.class);
    }

    /**
     * Retrieves all SolrHarvesterConfigs present in flow store and transforms the list into a map containing
     * key(ushHarvesterJobId) / value(harvesterConfig)
     * @return indexed list of ushHarvesterConfigs
     *
     * @throws JSONBException on failure to marshall/unmarshall harvesterConfig as UshSolrHarvesterConfig
     */
    private Map<Integer, UshSolrHarvesterConfig> getIndexedUshSolrHarvesterConfigs() throws JSONBException {
        final List<HarvesterConfig> harvesterConfigs = findAllUshSolrHarvesterConfigs();
        final CollectionType javaType = jsonbContext.getTypeFactory().constructCollectionType(List.class, UshSolrHarvesterConfig.class);
        final List<UshSolrHarvesterConfig> ushHarvesterConfigs = jsonbContext.unmarshall(jsonbContext.marshall(harvesterConfigs), javaType);
        return ushHarvesterConfigs.stream().collect(Collectors.toMap(c -> c.getContent().getUshHarvesterJobId(), c -> c));
    }

    /**
     * Retrieves all SolrHarvesterConfigs present in flow store
     * @return list of Harvester configs
     */
    private List<HarvesterConfig> findAllUshSolrHarvesterConfigs() {
        final Query namedQuery = entityManager.createNamedQuery(HarvesterConfig.QUERY_FIND_ALL_OF_TYPE);
        namedQuery.setParameter("type", UshSolrHarvesterConfig.class.getName());
        return namedQuery.getResultList();
    }

    /**
     * Maps values from ushHarvesterProperties to a new harvesterConfig
     * @param ushHarvesterProperties containing the values to map
     * @return harvesterConfig
     *
     * @throws JSONBException on failure to marshal ushSolrHarvesterConfig.Content
     */
    private HarvesterConfig toHarvesterConfig(UshHarvesterProperties ushHarvesterProperties) throws JSONBException {
        final UshSolrHarvesterConfig.Content ushHarvesterConfigContent = toUshHarvesterConfigContent(ushHarvesterProperties);
        return new HarvesterConfig().withContent(jsonbContext.marshall(ushHarvesterConfigContent))
                .withType(UshSolrHarvesterConfig.class.getName());
    }

    /**
     * Maps values from ushHarvesterProperties to a new UshSolrHarvesterConfig.Content
     * @param ushHarvesterProperties containing the values to map
     * @return ushSolrHarvesterConfig.Content
     */
    private UshSolrHarvesterConfig.Content toUshHarvesterConfigContent(UshHarvesterProperties ushHarvesterProperties) {
        return new UshSolrHarvesterConfig.Content()
                .withUshHarvesterJobId(ushHarvesterProperties.getId())
                .withEnabled(false)
                .withName(ushHarvesterProperties.getName());
    }

    /**
     * @param ushHarvesterJobId to uniquely identify a HarvesterConfig by
     * @return single harvesterConfig containing the sought after ushHarvesterJobId
     *
     * @throws FlowStoreException on failure to look up harvester config by ushHarvesterJobId
     */
    private HarvesterConfig findUshHarvesterConfigByUshHarvesterJobId (int ushHarvesterJobId) throws FlowStoreException {
        final Query namedQuery = entityManager.createNamedQuery(HarvesterConfig.QUERY_FIND_TYPE_WITH_CONTENT);
        namedQuery.setParameter(1, UshSolrHarvesterConfig.class);
        namedQuery.setParameter(2, ushHarvesterJobId);
        List<HarvesterConfig> result = namedQuery.getResultList();
        if(result.isEmpty()) {
            throw new FlowStoreException(String.format("harvester config with ushHarvesterJobId: {%d} was not found", ushHarvesterJobId));
        } else {
            return result.get(0);
        }
    }
}