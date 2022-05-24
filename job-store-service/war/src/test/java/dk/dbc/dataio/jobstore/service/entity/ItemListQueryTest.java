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

import dk.dbc.dataio.jobstore.types.criteria.ItemListCriteria;
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

public class ItemListQueryTest {

    private static final EntityManager ENTITY_MANAGER = mock(EntityManager.class);
    private static final Query QUERY = mock(Query.class);

    @Test(expected = NullPointerException.class)
    public void constructor_entityManagerArgIsNull_throws() {
        new ItemListQuery(null);
    }

    @Test
    public void execute_criteriaArgIsNull_throws() {
        final ItemListQuery itemListQuery = new ItemListQuery(ENTITY_MANAGER);
        try {
            itemListQuery.execute((ItemListCriteria) null);
            fail("No exception thrown");
        } catch (NullPointerException e) {
        }
    }

    @Test
    public void execute_queryReturnsEmptyList_returnsEmptyItemEntityList() {
        when(ENTITY_MANAGER.createNativeQuery(ItemListQuery.QUERY_BASE, ItemEntity.class)).thenReturn(QUERY);
        when(QUERY.getResultList()).thenReturn(Collections.emptyList());

        final ItemListQuery itemListQuery = new ItemListQuery(ENTITY_MANAGER);
        final List<ItemEntity> itemEntities = itemListQuery.execute(new ItemListCriteria());
        assertThat("List<ItemEntity>", itemEntities, is(notNullValue()));
        assertThat("List<ItemEntity> is empty", itemEntities.isEmpty(), is(true));
    }

    @Test
    public void execute_queryReturnsNonEmptyList_returnsItemEntityList() {
        final ItemEntity itemEntity1 = new ItemEntity();
        itemEntity1.setKey(new ItemEntity.Key(1, 1, (short) 1));

        final ItemEntity itemEntity2 = new ItemEntity();
        itemEntity2.setKey(new ItemEntity.Key(1, 2, (short) 2));

        when(ENTITY_MANAGER.createNativeQuery(ItemListQuery.QUERY_BASE, ItemEntity.class)).thenReturn(QUERY);
        when(QUERY.getResultList()).thenReturn(Arrays.asList(itemEntity1, itemEntity2));

        final ItemListQuery itemListQuery = new ItemListQuery(ENTITY_MANAGER);
        final List<ItemEntity> itemEntities = itemListQuery.execute(new ItemListCriteria());

        assertThat("List<ItemEntity>", itemEntities, is(notNullValue()));
        assertThat("List<ItemEntity>.size()", itemEntities.size(), is(2));
        assertThat("List<ItemEntity>.get(0).getKey()", itemEntities.get(0).getKey(), is(itemEntity1.getKey()));
        assertThat("List<ItemEntity>.get(1).getKey()", itemEntities.get(1).getKey(), is(itemEntity2.getKey()));
    }
}
