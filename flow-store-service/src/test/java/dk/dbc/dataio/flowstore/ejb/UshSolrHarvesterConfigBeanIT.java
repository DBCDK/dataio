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


import dk.dbc.dataio.commons.utils.test.jpa.JPATestUtils;
import dk.dbc.dataio.commons.utils.test.jpa.TransactionScopedPersistenceContext;
import dk.dbc.dataio.commons.utils.ush.UshHarvesterConnector;
import dk.dbc.dataio.commons.utils.ush.UshHarvesterConnectorException;
import dk.dbc.dataio.commons.utils.ush.ejb.UshHarvesterConnectorBean;
import dk.dbc.dataio.flowstore.FlowStoreException;
import dk.dbc.dataio.flowstore.entity.HarvesterConfig;
import dk.dbc.dataio.harvester.types.UshHarvesterProperties;
import dk.dbc.dataio.harvester.types.UshSolrHarvesterConfig;
import dk.dbc.dataio.jsonb.JSONBContext;
import dk.dbc.dataio.jsonb.JSONBException;
import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.MigrationInfo;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ejb.SessionContext;
import javax.persistence.EntityManager;
import javax.persistence.Query;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.hamcrest.Matchers.nullValue;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class UshSolrHarvesterConfigBeanIT {

    private static final UshHarvesterConnectorBean ushHarvesterConnectorBean = mock(UshHarvesterConnectorBean.class);
    private static final UshHarvesterConnector ushHarvesterConnector = mock(UshHarvesterConnector.class);
    private final SessionContext sessionContext = mock(SessionContext.class);;
    private TransactionScopedPersistenceContext persistenceContext;
    private final JSONBContext jsonbContext = new JSONBContext();
    private UshSolrHarvesterConfigBean ushSolrHarvesterConfigBean;

    @BeforeClass
    public static void setupMockedUshHarvesterConnector() {
        when(ushHarvesterConnectorBean.getConnector()).thenReturn(ushHarvesterConnector);
    }

    private EntityManager em;
    private static final Logger LOGGER = LoggerFactory.getLogger(UshSolrHarvesterConfigBeanIT.class);

    @Before
    public void setup() throws Exception {
        // Execute flyway upgrade
        StartupDBMigrator startupDBMigrator=new StartupDBMigrator().withDataSource( JPATestUtils.getTestDataSource("testdb") );
        startupDBMigrator.onStartup();


        em = JPATestUtils.createEntityManagerForIntegrationTest("flowStoreIT");
        em.getTransaction().begin();
        em.createNativeQuery("delete from harvester_configs").executeUpdate();
        em.getTransaction().commit();

        persistenceContext = new TransactionScopedPersistenceContext(em);
        ushSolrHarvesterConfigBean = newUshSolrHarvesterConfigBean();
        when(sessionContext.getBusinessObject(UshSolrHarvesterConfigBean.class)).thenReturn(ushSolrHarvesterConfigBean);
    }

    @After
    public void drop() {
        if( em.getTransaction().isActive() ) em.getTransaction().rollback();
        em.getTransaction().begin();
        em.createNativeQuery("delete from harvester_configs").executeUpdate();
        em.getTransaction().commit();
    }

    /**
     * Given: a job flow store containing one UshSolrHarvesterConfig and the matching harvester config existing in index data
     * When : attempting to find all ushSolrHarvesterConfigs
     * Then : the existing UshSolrHarvesterConfig is returned
     */
    @Test
    public void findAllAndSyncWithUsh_isPresent_returnsExisting() throws IOException, URISyntaxException, FlowStoreException, UshHarvesterConnectorException {
        // Given...
        JPATestUtils.runSqlFromResource(em, this, "harvesterConfigIT_testdata.sql");

        UshHarvesterProperties ushHarvesterProperties = getPresentUshHarvesterProperties();
        when(ushHarvesterConnector.listUshHarvesterJobs()).thenReturn(Collections.singletonList(ushHarvesterProperties));

        // When...
        final List<HarvesterConfig> returnedHarvesterConfigs = persistenceContext.run(() ->
                ushSolrHarvesterConfigBean.findAllAndSyncWithUsh()
        );

        // Then...
        assertThat("returnedHarvesterConfigs. size", returnedHarvesterConfigs.size(), is(1));
        assertThat("harvesterConfig.type", returnedHarvesterConfigs.get(0).getType(), is(UshSolrHarvesterConfig.class.getName()));

        final UshSolrHarvesterConfig.Content configContent = toUshSolrHarvesterConfigContent(returnedHarvesterConfigs.get(0).getContent());
        assertThat("configContent.name", configContent.getName(), is("existing UshSolrHarvesterConfig"));
        assertThat("configContent.ushHarvesterJobId", configContent.getUshHarvesterJobId(), is(10002));
        assertThat("configContent.ushHarvesterProperties", configContent.getUshHarvesterProperties(), is(ushHarvesterProperties));
    }

    /**
     * Given: a job flow store not containing any UshSolrHarvesterConfigs and one harvester config existing in index data
     * When : attempting to find all ushSolrHarvesterConfigs
     * Then : a new UshSolrHarvesterConfig is created and returned
     */
    @Test
    public void findAllAndSyncWithUsh_isAbsentInFlowStore_returnsNew() throws IOException, URISyntaxException, FlowStoreException, UshHarvesterConnectorException {
        // Given...
        UshHarvesterProperties ushHarvesterProperties = getAbsentUshHarvesterProperties();

        when(ushHarvesterConnector.listUshHarvesterJobs()).thenReturn(Collections.singletonList(ushHarvesterProperties));

        // When...
        final List<HarvesterConfig> returnedHarvesterConfigs = persistenceContext.run(() ->
                ushSolrHarvesterConfigBean.findAllAndSyncWithUsh()
        );

        // Then...
        assertThat("returnedHarvesterConfigs. size", returnedHarvesterConfigs.size(), is(1));
        assertThat("harvesterConfig.type", returnedHarvesterConfigs.get(0).getType(), is(UshSolrHarvesterConfig.class.getName()));

        final UshSolrHarvesterConfig.Content content = toUshSolrHarvesterConfigContent(returnedHarvesterConfigs.get(0).getContent());
        assertThat("configContent.name", content.getName(), is(ushHarvesterProperties.getName()));
        assertThat("configContent.ushHarvesterJobId", content.getUshHarvesterJobId(), is(ushHarvesterProperties.getId()));
        assertThat("configContent.ushHarvesterProperties", content.getUshHarvesterProperties(), is(ushHarvesterProperties));
    }

    /**
     * Given: a job flow store containing one UshSolrHarvesterConfig and two harvester config existing in index data
     *        where one of them matches the UshSolrHarvesterConfig
     * When : attempting to find all ushSolrHarvesterConfigs
     * Then : neither the existing UshSolrHarvesterConfig nor the newly created is persisted with matching UshHarvesterProperties
     */
    @Test
    public void findAllAndSyncWithUsh_UshHarvesterPropertiesNotPersisted() throws IOException, URISyntaxException, UshHarvesterConnectorException {
        // Given...
        JPATestUtils.runSqlFromResource(em, this, "harvesterConfigIT_testdata.sql");

        when(ushHarvesterConnector.listUshHarvesterJobs()).thenReturn(Arrays.asList(
                getPresentUshHarvesterProperties(), getAbsentUshHarvesterProperties()));

        // When...
        persistenceContext.run(() -> ushSolrHarvesterConfigBean.findAllAndSyncWithUsh());

        // Then...
        Query q = em.createNamedQuery(HarvesterConfig.QUERY_FIND_ALL_OF_TYPE).setParameter("type", UshSolrHarvesterConfig.class.getName());
        List<HarvesterConfig> persistedHarvesterConfig = q.getResultList();

        assertThat(persistedHarvesterConfig.size(), is(2));
        assertThat(toUshSolrHarvesterConfigContent(persistedHarvesterConfig.get(0).getContent()).getUshHarvesterProperties(), is(nullValue()));
        assertThat(toUshSolrHarvesterConfigContent(persistedHarvesterConfig.get(1).getContent()).getUshHarvesterProperties(), is(nullValue()));
    }

    @Ignore("This must be tested through arquillian since we have multiple transactions in scope")
    @Test
    public void createIfAbsentInFlowStore_uniqueConstraintViolation_returnsExisting() throws IOException, URISyntaxException {
        JPATestUtils.runSqlFromResource(em, this, "harvesterConfigIT_testdata.sql");
        UshSolrHarvesterConfigBean ushSolrHarvesterConfigBean = newUshSolrHarvesterConfigBean();
        final HarvesterConfig returnedHarvesterConfig = persistenceContext.run(() ->
                ushSolrHarvesterConfigBean.createIfAbsentInFlowStore(getPresentUshHarvesterProperties())
        );

        assertThat(toUshSolrHarvesterConfigContent(returnedHarvesterConfig.getContent()).getName(), is("existing UshSolrHarvesterConfig"));
    }


    @Ignore("This must be tested through arquillian since we have multiple transactions in scope")
    @Test
    public void deleteIfAbsentInUsh_entityNotFoundException_ok() throws IOException, URISyntaxException, UshHarvesterConnectorException {

        Map<Integer, UshSolrHarvesterConfig> ushSolrHarvesterConfigMap = new HashMap<>();
        ushSolrHarvesterConfigMap.put(1, new UshSolrHarvesterConfig(1, 1, new UshSolrHarvesterConfig.Content()));

        persistenceContext.run(() -> ushSolrHarvesterConfigBean.deleteIfAbsentInUsh(ushSolrHarvesterConfigMap));
    }


    private UshSolrHarvesterConfigBean newUshSolrHarvesterConfigBean () {
        UshSolrHarvesterConfigBean ushSolrHarvesterConfigBean = new UshSolrHarvesterConfigBean();
        ushSolrHarvesterConfigBean.entityManager = em;
        ushSolrHarvesterConfigBean.ushHarvesterConnectorBean = ushHarvesterConnectorBean;
        ushSolrHarvesterConfigBean.sessionContext = sessionContext;
        return ushSolrHarvesterConfigBean;
    }

    private UshHarvesterProperties getPresentUshHarvesterProperties() {
        return new UshHarvesterProperties()
                .withId(10002)
                .withName("seen before");
    }

    private UshHarvesterProperties getAbsentUshHarvesterProperties() {
        return new UshHarvesterProperties()
                .withId(10001)
                .withName("not seen before");
    }

    private UshSolrHarvesterConfig.Content toUshSolrHarvesterConfigContent(String content) {
        try {
            return jsonbContext.unmarshall(content, UshSolrHarvesterConfig.Content.class);
        } catch (JSONBException e) {
            throw new IllegalArgumentException("Error occurred while unmarshalling");
        }
    }

}
