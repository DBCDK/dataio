package dk.dbc.dataio.jobstore.service.entity;

import dk.dbc.dataio.jobstore.types.criteria.ItemListCriteria;
import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class ItemListQueryTest {

    private static final EntityManager ENTITY_MANAGER = mock(EntityManager.class);
    private static final Query QUERY = mock(Query.class);

    @Test
    public void constructor_entityManagerArgIsNull_throws() {
        assertThrows(NullPointerException.class, () -> new ItemListQuery(null));
    }

    @Test
    public void execute_criteriaArgIsNull_throws() {
        ItemListQuery itemListQuery = new ItemListQuery(ENTITY_MANAGER);
        assertThrows(NullPointerException.class, () -> itemListQuery.execute((ItemListCriteria) null));
    }

    @Test
    public void execute_queryReturnsEmptyList_returnsEmptyItemEntityList() {
        when(ENTITY_MANAGER.createNativeQuery(ItemListQuery.QUERY_BASE, ItemEntity.class)).thenReturn(QUERY);
        when(QUERY.getResultList()).thenReturn(Collections.emptyList());

        ItemListQuery itemListQuery = new ItemListQuery(ENTITY_MANAGER);
        List<ItemEntity> itemEntities = itemListQuery.execute(new ItemListCriteria());
        assertThat("List<ItemEntity>", itemEntities, is(notNullValue()));
        assertThat("List<ItemEntity> is empty", itemEntities.isEmpty(), is(true));
    }

    @Test
    public void execute_queryReturnsNonEmptyList_returnsItemEntityList() {
        ItemEntity itemEntity1 = new ItemEntity();
        itemEntity1.setKey(new ItemEntity.Key(1, 1, (short) 1));

        ItemEntity itemEntity2 = new ItemEntity();
        itemEntity2.setKey(new ItemEntity.Key(1, 2, (short) 2));

        when(ENTITY_MANAGER.createNativeQuery(ItemListQuery.QUERY_BASE, ItemEntity.class)).thenReturn(QUERY);
        when(QUERY.getResultList()).thenReturn(Arrays.asList(itemEntity1, itemEntity2));

        ItemListQuery itemListQuery = new ItemListQuery(ENTITY_MANAGER);
        List<ItemEntity> itemEntities = itemListQuery.execute(new ItemListCriteria());

        assertThat("List<ItemEntity>", itemEntities, is(notNullValue()));
        assertThat("List<ItemEntity>.size()", itemEntities.size(), is(2));
        assertThat("List<ItemEntity>.get(0).getKey()", itemEntities.get(0).getKey(), is(itemEntity1.getKey()));
        assertThat("List<ItemEntity>.get(1).getKey()", itemEntities.get(1).getKey(), is(itemEntity2.getKey()));
    }
}
