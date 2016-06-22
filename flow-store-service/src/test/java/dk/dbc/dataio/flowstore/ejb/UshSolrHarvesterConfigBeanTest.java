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

import dk.dbc.dataio.commons.utils.ush.UshHarvesterConnector;
import dk.dbc.dataio.commons.utils.ush.UshHarvesterConnectorException;
import dk.dbc.dataio.commons.utils.ush.ejb.UshHarvesterConnectorBean;
import dk.dbc.dataio.flowstore.FlowStoreException;
import dk.dbc.dataio.flowstore.entity.HarvesterConfig;
import dk.dbc.dataio.flowstore.rest.PersistenceExceptionMapper;
import dk.dbc.dataio.harvester.types.UshHarvesterProperties;
import dk.dbc.dataio.harvester.types.UshSolrHarvesterConfig;
import dk.dbc.dataio.jsonb.JSONBContext;
import dk.dbc.dataio.jsonb.JSONBException;
import org.junit.Before;
import org.junit.Test;

import javax.ejb.SessionContext;
import javax.persistence.EntityManager;
import javax.persistence.EntityNotFoundException;
import javax.persistence.PersistenceException;
import javax.persistence.Query;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import static dk.dbc.commons.testutil.Assert.assertThat;
import static dk.dbc.commons.testutil.Assert.isThrowing;
import static org.hamcrest.Matchers.any;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class UshSolrHarvesterConfigBeanTest {

    private final EntityManager entityManager = mock(EntityManager.class);
    private final UshHarvesterConnectorBean ushHarvesterConnectorBean = mock(UshHarvesterConnectorBean.class);
    private final UshHarvesterConnector ushHarvesterConnector = mock(UshHarvesterConnector.class);
    private final SessionContext sessionContext = mock(SessionContext.class);
    private final Query findTypeWithContent = mock(Query.class);
    private final Query findAllOfType = mock(Query.class);
    // ----------------------------------------------------------------------------------------------------------------------
    private final JSONBContext jsonbContext = new JSONBContext();
    private UshSolrHarvesterConfigBean ushSolrHarvesterConfigBean;
    // ----------------------------------------------------------------------------------------------------------------------
    private final UshHarvesterProperties ushHarvesterProperties = new UshHarvesterProperties().withId(1).withName("name1");
    private final List<UshHarvesterProperties> ushHarvesterPropertiesList = Collections.singletonList(ushHarvesterProperties);

    private final UshSolrHarvesterConfig.Content configContent = newUshSolrHarvesterConfigContent("name1", 1);
    private final HarvesterConfig harvesterConfig = newHarvesterConfig(1, configContent);

    private final UshSolrHarvesterConfig.Content orphanedConfigContent = newUshSolrHarvesterConfigContent("name2", 2);
    private final HarvesterConfig orphanedHarvesterConfig = newHarvesterConfig(2, orphanedConfigContent);
    // ----------------------------------------------------------------------------------------------------------------------


    @Before
    public void setup() {
        ushSolrHarvesterConfigBean = newUshSolrHarvesterConfigBean();
    }

    @Before
    public void setupMockedReturns() throws UshHarvesterConnectorException {
        when(ushHarvesterConnectorBean.getConnector()).thenReturn(ushHarvesterConnector);
        when(ushHarvesterConnector.listUshHarvesterJobs()).thenReturn(ushHarvesterPropertiesList);
        when(ushHarvesterConnector.listIndexedUshHarvesterJobs()).thenReturn(ushHarvesterPropertiesList
                .stream()
                .collect(Collectors.toMap(UshHarvesterProperties::getId, c -> c))
        );

        when(sessionContext.getBusinessObject(UshSolrHarvesterConfigBean.class)).thenReturn(newUshSolrHarvesterConfigBean());

        when(entityManager.createNamedQuery(HarvesterConfig.QUERY_FIND_TYPE_WITH_CONTENT)).thenReturn(findTypeWithContent);
        when(entityManager.createNamedQuery(HarvesterConfig.QUERY_FIND_ALL_OF_TYPE)).thenReturn(findAllOfType);
    }


    @Test
    public void findAllAndSyncWithUsh_queryFindByUshHarvesterJobIdReturnsEmpty_throwsFlowStoreException() {
        when(findTypeWithContent.getResultList()).thenReturn(Collections.emptyList());
        assertThat(() -> ushSolrHarvesterConfigBean.findAllAndSyncWithUsh(), isThrowing(FlowStoreException.class));
    }

    @Test
    public void createIfAbsentInFlowStore_persistenceExceptionIsThrownWithoutExpectedMessage_throwsFlowStoreException() {
        doThrow(new PersistenceException("msg")).when(entityManager).flush();
        assertThat(() -> ushSolrHarvesterConfigBean.createIfAbsentInFlowStore(new UshHarvesterProperties()), isThrowing(FlowStoreException.class));
    }

    @Test
    public void findAllAndSyncWithUsh_allConfigsPresentInFlowStore_returns() throws FlowStoreException {

        when(findAllOfType.getResultList()).thenReturn(Collections.singletonList(harvesterConfig));

        // Subject under test
        final List<HarvesterConfig> harvesterConfigList = ushSolrHarvesterConfigBean.findAllAndSyncWithUsh();

        // Verification
        assertThat("harvesterConfigList.size", harvesterConfigList.size(), is(1));
        assertThat("UshSolrHarvesterConfig.Content", toUshHarvesterConfigContent(harvesterConfigList.get(0).getContent()),
                is(configContent.withUshHarvesterProperties(ushHarvesterProperties)));
    }

    @Test
    public void findAllAndSyncWithUsh_orphanedUshSolrHarvesterConfigDeleted_ok() throws FlowStoreException {
        when(findAllOfType.getResultList()).thenReturn(Arrays.asList(harvesterConfig, orphanedHarvesterConfig));

        // Subject under test
        ushSolrHarvesterConfigBean.findAllAndSyncWithUsh();

        // Verification
        verify(entityManager, times(1)).remove(orphanedHarvesterConfig);
    }

    @Test
    public void findAllAndSyncWithUsh_tryDeleteCausesUnexpectedException_throwsFlowStoreException() throws FlowStoreException {
        doThrow(new IllegalArgumentException()).when(entityManager).refresh(any(HarvesterConfig.class));
        assertThat(() -> ushSolrHarvesterConfigBean.findAllAndSyncWithUsh(), isThrowing(FlowStoreException.class));
    }

    @Test
    public void findAllAndSyncWithUsh_ushHarvesterConnectorExceptionIsThrown_throwsFlowStoreException() throws UshHarvesterConnectorException {
        when(ushHarvesterConnector.listUshHarvesterJobs()).thenThrow(new UshHarvesterConnectorException("Error"));
        assertThat(() -> ushSolrHarvesterConfigBean.findAllAndSyncWithUsh(), isThrowing(FlowStoreException.class));
    }

    @Test
    public void createIfAbsentInFlowStore_tryCreateCausesPersistenceExceptionWithExpectedMessage_callsFindUshHarvesterConfigByUshHarvesterJobId() throws FlowStoreException, JSONBException {
        doThrow(new PersistenceException(PersistenceExceptionMapper.UNIQUE_CONSTRAINT_VIOLATION)).when(entityManager).flush();
        when(findTypeWithContent.getResultList()).thenReturn(Collections.singletonList(new HarvesterConfig()));

        // Subject under test
        ushSolrHarvesterConfigBean.createIfAbsentInFlowStore(new UshHarvesterProperties());

        // Verification
        verify(entityManager, times(1)).createNamedQuery(HarvesterConfig.QUERY_FIND_TYPE_WITH_CONTENT);
    }

    @Test
    public void findAllAndSyncWithUsh_tryCreateCausesPersistenceExceptionWithExpectedMessage_returns() throws FlowStoreException {
        doThrow(new PersistenceException(PersistenceExceptionMapper.UNIQUE_CONSTRAINT_VIOLATION)).when(entityManager).flush();
        when(findTypeWithContent.getResultList()).thenReturn(Collections.singletonList(harvesterConfig));

        // Subject under test
        assertThat(ushSolrHarvesterConfigBean.findAllAndSyncWithUsh().size(), is(1));
    }

    @Test
    public void findAllAndSyncWithUsh_tryDeleteCausesEntityNotFoundException_returns() throws FlowStoreException {
        doThrow(new EntityNotFoundException()).when(entityManager).refresh(any(HarvesterConfig.class));
        when(findAllOfType.getResultList()).thenReturn(Arrays.asList(harvesterConfig, orphanedHarvesterConfig));

        // Subject under test
        assertThat(ushSolrHarvesterConfigBean.findAllAndSyncWithUsh().size(), is(1));
    }

    /*
     * Private methods
     */
    private UshSolrHarvesterConfigBean newUshSolrHarvesterConfigBean() {
        UshSolrHarvesterConfigBean ushSolrHarvesterConfigBean = new UshSolrHarvesterConfigBean();
        ushSolrHarvesterConfigBean.jsonbContext = jsonbContext;
        ushSolrHarvesterConfigBean.sessionContext = sessionContext;
        ushSolrHarvesterConfigBean.entityManager = entityManager;
        ushSolrHarvesterConfigBean.ushHarvesterConnectorBean = ushHarvesterConnectorBean;
        return ushSolrHarvesterConfigBean;
    }

    private UshSolrHarvesterConfig.Content toUshHarvesterConfigContent(String harvesterContent) {
        try {
            return jsonbContext.unmarshall(harvesterContent, UshSolrHarvesterConfig.Content.class);
        } catch (JSONBException e) {
            throw new IllegalStateException("Unable to unmarshal input to UshSolrHarvesterConfig.Content");
        }
    }

    private HarvesterConfig newHarvesterConfig(long id, UshSolrHarvesterConfig.Content content) {
        try {
        return new HarvesterConfig()
                .withContent(jsonbContext.marshall(content))
                .withType(UshSolrHarvesterConfig.class.getName())
                .withId(id)
                .withVersion(1L);
        } catch (JSONBException e) {
            throw new IllegalStateException("Unable to marshal input");
        }
    }

    private UshSolrHarvesterConfig.Content newUshSolrHarvesterConfigContent(String name, int ushHarvesterJobId) {
        return new UshSolrHarvesterConfig.Content().withName(name).withUshHarvesterJobId(ushHarvesterJobId);
    }
}
