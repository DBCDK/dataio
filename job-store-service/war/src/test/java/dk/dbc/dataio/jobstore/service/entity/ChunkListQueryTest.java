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

package dk.dbc.dataio.jobstore.service.entity;

import dk.dbc.dataio.jobstore.types.criteria.ChunkListCriteria;
import org.junit.Test;

import javax.persistence.EntityManager;
import javax.persistence.Query;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class ChunkListQueryTest {
    private static final EntityManager ENTITY_MANAGER = mock(EntityManager.class);
    private static final Query QUERY = mock(Query.class);

    @Test(expected = NullPointerException.class)
    public void constructor_entityManagerArgIsNull_throws() {
        new ChunkListQuery(null);
    }

    @Test
    public void execute_criteriaArgIsNull_throws() {
        final ChunkListQuery chunkListQuery = new ChunkListQuery(ENTITY_MANAGER);
        try {
            chunkListQuery.execute(null);
            fail("No exception thrown");
        } catch (NullPointerException e) {
        }
    }

    @Test
    public void execute_queryReturnsEmptyList_returnsEmptyChunkEntityList() {
        when(ENTITY_MANAGER.createNativeQuery(ChunkListQuery.QUERY_BASE, ChunkEntity.class)).thenReturn(QUERY);
        when(QUERY.getResultList()).thenReturn(Collections.emptyList());

        final ChunkListQuery chunkListQuery = new ChunkListQuery(ENTITY_MANAGER);
        final List<ChunkEntity> chunkEntities = chunkListQuery.execute(new ChunkListCriteria());
        assertThat("List<ChunkEntity>", chunkEntities, is(notNullValue()));
        assertThat("List<ChunkEntity> is empty", chunkEntities.isEmpty(), is(true));
    }

    @Test
    public void execute_queryReturnsNonEmptyList_returnsChunkEntityList() {
        final ChunkEntity chunkEntity1 = new ChunkEntity();
        chunkEntity1.setKey(new ChunkEntity.Key(1, 1));

        final ChunkEntity chunkEntity2 = new ChunkEntity();
        chunkEntity2.setKey(new ChunkEntity.Key(1, 2));

        when(ENTITY_MANAGER.createNativeQuery(ChunkListQuery.QUERY_BASE, ChunkEntity.class)).thenReturn(QUERY);
        when(QUERY.getResultList()).thenReturn(Arrays.asList(chunkEntity1, chunkEntity2));

        final ChunkListQuery chunkListQuery = new ChunkListQuery(ENTITY_MANAGER);
        final List<ChunkEntity> chunkEntities = chunkListQuery.execute(new ChunkListCriteria());

        assertThat("List<ChunkEntity>", chunkEntities, is(notNullValue()));
        assertThat("List<ChunkEntity>.size()", chunkEntities.size(), is(2));
        assertThat("List<ChunkEntity>.get(0).getKey()", chunkEntities.get(0).getKey(), is(chunkEntity1.getKey()));
        assertThat("List<ChunkEntity>.get(1).getKey()", chunkEntities.get(1).getKey(), is(chunkEntity2.getKey()));
    }
}
