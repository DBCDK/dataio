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

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.node.ArrayNode;
import dk.dbc.commons.jsonb.JSONBContext;
import dk.dbc.commons.jsonb.JSONBException;
import dk.dbc.dataio.flowstore.entity.HarvesterConfig;
import dk.dbc.dataio.harvester.types.RRHarvesterConfig;
import org.junit.Before;
import org.junit.Test;

import javax.persistence.EntityManager;
import javax.persistence.Query;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriBuilder;
import javax.ws.rs.core.UriInfo;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Arrays;
import java.util.Collections;

import static dk.dbc.commons.testutil.Assert.assertThat;
import static dk.dbc.commons.testutil.Assert.isThrowing;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.nullValue;
import static org.hamcrest.core.Is.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class HarvestersBeanTest {
    private final EntityManager entityManager = mock(EntityManager.class);
    private final UriInfo uriInfo = mock(UriInfo.class);
    private final UriBuilder uriBuilder = mock(UriBuilder.class);
    private final Query query = mock(Query.class);
    private final JSONBContext jsonbContext = new JSONBContext();

    private final long id = 123;
    private final long version = 42;
    private final String rrHarvesterConfigType = "dk.dbc.dataio.harvester.types.RRHarvesterConfig";

    @Before
    public void setup() throws URISyntaxException {
        when(uriInfo.getAbsolutePathBuilder()).thenReturn(uriBuilder);
        when(uriBuilder.path(anyString())).thenReturn(uriBuilder);
        when(uriBuilder.build()).thenReturn(new URI("location"));
    }

//----------------------------------------------------------------------------------------------------------------------

    @Test
    public void createHarvesterConfig_configContentArgIsNull_throws() {
        final HarvestersBean harvestersBean = newharvestersBeanWithMockedEntityManager();
        assertThat(() -> harvestersBean.createHarvesterConfig(null, rrHarvesterConfigType, null), isThrowing(NullPointerException.class));
    }

    @Test
    public void createHarvesterConfig_configContentArgIsEmpty_throws() {
        final HarvestersBean harvestersBean = newharvestersBeanWithMockedEntityManager();
        assertThat(() -> harvestersBean.createHarvesterConfig(null, rrHarvesterConfigType, " "), isThrowing(IllegalArgumentException.class));
    }

    @Test
    public void createHarvesterConfig_configContentArgIsInvalidJson_throws() {
        final HarvestersBean harvestersBean = newharvestersBeanWithMockedEntityManager();
        assertThat(() -> harvestersBean.createHarvesterConfig(null, rrHarvesterConfigType, "invalid json"), isThrowing(JSONBException.class));
    }

    @Test
    public void createHarvesterConfig_typeArgCanNotBeResolvedAsClass_throws() {
        final HarvestersBean harvestersBean = newharvestersBeanWithMockedEntityManager();
        assertThat( () -> harvestersBean.createHarvesterConfig(null, "dk.dbc.NoSuchClass", "{}"), isThrowing(ClassNotFoundException.class));
    }

    @Test
    public void createHarvesterConfig_contentArgIsNotCompatibleWithTypeArg_throws() {
        final HarvestersBean harvestersBean = newharvestersBeanWithMockedEntityManager();
        assertThat(() -> harvestersBean.createHarvesterConfig(null, rrHarvesterConfigType, "{\"key\": \"value\"}"), isThrowing(JSONBException.class));
    }

    @Test
    public void updateHarvesterConfig_configContentArgIsNull_throws() {
        final HarvestersBean harvestersBean = newharvestersBeanWithMockedEntityManager();
        assertThat(() -> harvestersBean.updateHarvesterConfig(id, version, rrHarvesterConfigType, null), isThrowing(NullPointerException.class));
    }

    @Test
    public void updateHarvesterConfig_configContentArgIsEmpty_throws() {
        final HarvestersBean harvestersBean = newharvestersBeanWithMockedEntityManager();
        assertThat(() -> harvestersBean.updateHarvesterConfig(id, version, rrHarvesterConfigType, " "), isThrowing(IllegalArgumentException.class));
    }

    @Test
    public void updateHarvesterConfig_configNotFound_returnsResponseWithHttpStatusNotFound() throws JSONBException, ClassNotFoundException {
        when(entityManager.find(HarvesterConfig.class, id)).thenReturn(null);

        final HarvestersBean harvestersBean = newharvestersBeanWithMockedEntityManager();
        final Response response = harvestersBean.updateHarvesterConfig(id, version, rrHarvesterConfigType, "{}");
        assertThat("Response status", response.getStatus(), is(Response.Status.NOT_FOUND.getStatusCode()));
        assertThat("Response has no content entity", response.getEntity(), is(HarvestersBean.NO_CONTENT));
    }

    @Test
    public void updateHarvesterConfig_typeArgCanNotBeResolvedAsClass_throws() {
        final HarvesterConfig harvesterConfig = new HarvesterConfig();
        when(entityManager.find(HarvesterConfig.class, id)).thenReturn(harvesterConfig);

        final HarvestersBean harvestersBean = newharvestersBeanWithMockedEntityManager();
        assertThat(() -> harvestersBean.updateHarvesterConfig(id, version, "dk.dbc.NoSuchClass", "{}"), isThrowing(ClassNotFoundException.class));
    }

    @Test
    public void updateHarvesterConfig_contentArgIsNotCompatibleWithTypeArg_throws() {
        final HarvesterConfig harvesterConfig = new HarvesterConfig();
        when(entityManager.find(HarvesterConfig.class, id)).thenReturn(harvesterConfig);

        final HarvestersBean harvestersBean = newharvestersBeanWithMockedEntityManager();
        final TestHarvesterConfig newConfig = new TestHarvesterConfig(id, version, new TestHarvesterConfig.Content());
        assertThat(() -> harvestersBean.updateHarvesterConfig(id, version, rrHarvesterConfigType, jsonbContext.marshall(newConfig)), isThrowing(JSONBException.class));
    }

    @Test
    public void updateHarvesterConfig_typeArgIsnullAndConfigIsFound_returnsResponseWithHttpStatusOkAndEntity() throws JSONBException, ClassNotFoundException {
        final HarvesterConfig harvesterConfig = new HarvesterConfig()
                .withType(rrHarvesterConfigType)
                .withVersion(version);
        when(entityManager.find(HarvesterConfig.class, id)).thenReturn(harvesterConfig);
        when(entityManager.merge(any(HarvesterConfig.class))).thenReturn(harvesterConfig);

        final HarvestersBean harvestersBean = newharvestersBeanWithMockedEntityManager();
        final Response response = harvestersBean.updateHarvesterConfig(id, version, null, jsonbContext.marshall(new RRHarvesterConfig.Content()));

        assertThat("response status", response.getStatus(), is(Response.Status.OK.getStatusCode()));
        assertThat("response has entity", response.hasEntity(), is(true));
        assertThat("response has ETAG", response.getEntityTag().getValue(), is(Long.toString(version)));
    }

    @Test
    public void updateHarvesterConfig_typeIsChangedAndConfigIsFound_returnsResponseWithHttpStatusOkAndEntity() throws JSONBException, ClassNotFoundException {
        final HarvesterConfig harvesterConfig = new HarvesterConfig()
                .withType(rrHarvesterConfigType)
                .withVersion(version);
        when(entityManager.find(HarvesterConfig.class, id)).thenReturn(harvesterConfig);
        when(entityManager.merge(any(HarvesterConfig.class))).thenReturn(harvesterConfig);

        final HarvestersBean harvestersBean = newharvestersBeanWithMockedEntityManager();
        final Response response = harvestersBean.updateHarvesterConfig(id, version, TestHarvesterConfig.class.getName(), jsonbContext.marshall(new TestHarvesterConfig.Content()));

        assertThat("response status", response.getStatus(), is(Response.Status.OK.getStatusCode()));
        assertThat("response has entity", response.hasEntity(), is(true));
        assertThat("response has ETAG", response.getEntityTag().getValue(), is(Long.toString(version)));
    }

    @Test
    @SuppressWarnings("unchecked")
    public void findAllHarvesterConfigsByType_noConfigsFound_returnsResponseWithHttpStatusOkAndEmptyList() throws JSONBException {
        when(entityManager.createNamedQuery(HarvesterConfig.QUERY_FIND_ALL_OF_TYPE)).thenReturn(query);
        when(query.setParameter("type", rrHarvesterConfigType)).thenReturn(query);
        when(query.getResultList()).thenReturn(Collections.emptyList());

        final HarvestersBean harvestersBean = newharvestersBeanWithMockedEntityManager();
        final Response response = harvestersBean.findAllHarvesterConfigsByType(rrHarvesterConfigType);
        assertThat("response status", response.getStatus(), is(Response.Status.OK.getStatusCode()));
        assertThat("response has entity", response.hasEntity(), is(true));
        final ArrayNode entityNode = (ArrayNode) jsonbContext.getJsonTree((String) response.getEntity());
        assertThat("response entity size", entityNode.size(), is(0));
    }

    @Test
    @SuppressWarnings("unchecked")
    public void findAllHarvesterConfigsByType_configsFound_returnsResponseWithHttpStatusOkAndConfigEntities() throws JSONBException {
        final HarvesterConfig firstConfig = new HarvesterConfig().withId(1L);
        final HarvesterConfig secondConfig = new HarvesterConfig().withId(2L);

        when(entityManager.createNamedQuery(HarvesterConfig.QUERY_FIND_ALL_OF_TYPE)).thenReturn(query);
        when(query.setParameter("type", rrHarvesterConfigType)).thenReturn(query);
        when(query.getResultList()).thenReturn(Arrays.asList(firstConfig, secondConfig));

        final HarvestersBean harvestersBean = newharvestersBeanWithMockedEntityManager();
        final Response response = harvestersBean.findAllHarvesterConfigsByType(rrHarvesterConfigType);
        assertThat("response status", response.getStatus(), is(Response.Status.OK.getStatusCode()));
        assertThat("response has entity", response.hasEntity(), is(true));
        final ArrayNode entityNode = (ArrayNode) jsonbContext.getJsonTree((String) response.getEntity());
        assertThat("response entity size", entityNode.size(), is(2));
        assertThat("1st entry in entity list", entityNode.get(0).get("id").asLong(), is(firstConfig.getId()));
        assertThat("2nd entry in entity list", entityNode.get(1).get("id").asLong(), is(secondConfig.getId()));
    }

    @Test
    @SuppressWarnings("unchecked")
    public void findEnabledHarvesterConfigsByType_noConfigsFound_returnsResponseWithHttpStatusOkAndEmptyList() throws JSONBException {
        when(entityManager.createNamedQuery(HarvesterConfig.QUERY_FIND_ALL_ENABLED_OF_TYPE)).thenReturn(query);
        when(query.setParameter(1, rrHarvesterConfigType)).thenReturn(query);
        when(query.getResultList()).thenReturn(Collections.emptyList());

        final HarvestersBean harvestersBean = newharvestersBeanWithMockedEntityManager();
        final Response response = harvestersBean.findEnabledHarvesterConfigsByType(rrHarvesterConfigType);
        assertThat("response status", response.getStatus(), is(Response.Status.OK.getStatusCode()));
        assertThat("response has entity", response.hasEntity(), is(true));
        final ArrayNode entityNode = (ArrayNode) jsonbContext.getJsonTree((String) response.getEntity());
        assertThat("response entity size", entityNode.size(), is(0));
    }

    @Test
    @SuppressWarnings("unchecked")
    public void findEnabledHarvesterConfigsByType_configsFound_returnsResponseWithHttpStatusOkAndConfigEntities() throws JSONBException {
        final HarvesterConfig firstConfig = new HarvesterConfig().withId(1L);
        final HarvesterConfig secondConfig = new HarvesterConfig().withId(2L);

        when(entityManager.createNamedQuery(HarvesterConfig.QUERY_FIND_ALL_ENABLED_OF_TYPE)).thenReturn(query);
        when(query.setParameter(1, rrHarvesterConfigType)).thenReturn(query);
        when(query.getResultList()).thenReturn(Arrays.asList(firstConfig, secondConfig));

        final HarvestersBean harvestersBean = newharvestersBeanWithMockedEntityManager();
        final Response response = harvestersBean.findEnabledHarvesterConfigsByType(rrHarvesterConfigType);
        assertThat("response status", response.getStatus(), is(Response.Status.OK.getStatusCode()));
        assertThat("response has entity", response.hasEntity(), is(true));
        final ArrayNode entityNode = (ArrayNode) jsonbContext.getJsonTree((String) response.getEntity());
        assertThat("response entity size", entityNode.size(), is(2));
        assertThat("1st entry in entity list", entityNode.get(0).get("id").asLong(), is(firstConfig.getId()));
        assertThat("2nd entry in entity list", entityNode.get(1).get("id").asLong(), is(secondConfig.getId()));
    }

    @Test
    public void getHarvesterConfig_notFound_returnsResponseWithHttpStatusNotFound() throws JSONBException {
        final HarvestersBean harvestersBean = newharvestersBeanWithMockedEntityManager();
        final Response response = harvestersBean.getHarvesterConfig(42L);
        assertThat("Response status", response.getStatus(), is(Response.Status.NOT_FOUND.getStatusCode()));
        assertThat("Response entity tag", response.getEntityTag(), is(nullValue()));
        assertThat("Response has no content entity", response.getEntity(), is(HarvestersBean.NO_CONTENT));
    }

    @Test
    public void getHarvesterConfig_found_returnsResponseWithHttpStatusOkAndConfigEntity() throws JSONBException {
        final long id = 42;
        final long version = 2;
        final HarvesterConfig harvesterConfig = new HarvesterConfig()
                .withId(id)
                .withVersion(version);
        when(entityManager.find(HarvesterConfig.class, id)).thenReturn(harvesterConfig);

        final HarvestersBean harvestersBean = newharvestersBeanWithMockedEntityManager();
        final Response response = harvestersBean.getHarvesterConfig(id);
        assertThat("Response status", response.getStatus(), is(Response.Status.OK.getStatusCode()));
        assertThat("Response has entity", response.hasEntity(), is(true));
        assertThat("Response entity tag", response.getEntityTag().getValue(), is(Long.toString(version)));
    }

    @Test
    public void deleteHarvesterConfig_notFound_returnsResponseWithHttpStatusNotFound() {
        final HarvestersBean harvestersBean = newharvestersBeanWithMockedEntityManager();
        final Response response = harvestersBean.deleteHarvesterConfig(42L, 1L);
        assertThat("Response status", response.getStatus(), is(Response.Status.NOT_FOUND.getStatusCode()));
        assertThat("Response entity tag", response.getEntityTag(), is(nullValue()));
        assertThat("Response has no content entity", response.getEntity(), is(HarvestersBean.NO_CONTENT));
    }

    @Test
    public void deleteHarvesterConfig_deleted_returnsResponseWithHttpStatusNoContent() {
        final long id = 42;
        final long version = 2;
        final HarvesterConfig harvesterConfig = new HarvesterConfig()
                .withId(id)
                .withVersion(version);
        when(entityManager.find(HarvesterConfig.class, id)).thenReturn(harvesterConfig);

        final HarvestersBean harvestersBean = newharvestersBeanWithMockedEntityManager();
        final Response response = harvestersBean.deleteHarvesterConfig(id, version);
        assertThat("Response status", response.getStatus(), is(Response.Status.NO_CONTENT.getStatusCode()));
        assertThat("Response has entity", response.hasEntity(), is(false));
    }

    public HarvestersBean newharvestersBeanWithMockedEntityManager() {
        final HarvestersBean harvestersBean = new HarvestersBean();
        harvestersBean.entityManager = entityManager;
        return harvestersBean;
    }

    private static class TestHarvesterConfig extends dk.dbc.dataio.harvester.types.HarvesterConfig<TestHarvesterConfig.Content> {
        private static final long serialVersionUID = 5750099468125210041L;

        @JsonCreator
        public TestHarvesterConfig(
                @JsonProperty("id") long id,
                @JsonProperty("version") long version,
                @JsonProperty("content") Content content)
                throws NullPointerException, IllegalArgumentException {
            super(id, version, content);
        }

        @Override
        public String getLogId() {
            return "test";
        }

        public static class Content {
            @JsonProperty
            private String testHarvesterConfigContentString;
        }
    }
}