package dk.dbc.dataio.jobstore.service.entity;

import dk.dbc.dataio.jobstore.types.ItemInfoSnapshot;
import dk.dbc.dataio.jobstore.types.criteria.ItemListCriteria;
import dk.dbc.dataio.jobstore.types.criteria.ListFilter;
import dk.dbc.dataio.jobstore.types.criteria.ListOrderBy;
import org.junit.Test;

import javax.persistence.EntityManager;
import javax.persistence.Query;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.junit.Assert.assertThat;
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
            itemListQuery.execute(null);
            fail("No exception thrown");
        } catch (NullPointerException e) {
        }
    }

    @Test
    public void execute_queryReturnsEmptyList_returnsEmptySnapshotList() {
        when(ENTITY_MANAGER.createNativeQuery(ItemListQuery.QUERY_BASE, ItemEntity.class)).thenReturn(QUERY);
        when(QUERY.getResultList()).thenReturn(Collections.emptyList());

        final ItemListQuery itemListQuery = new ItemListQuery(ENTITY_MANAGER);
        final List<ItemInfoSnapshot> itemInfoSnapshots = itemListQuery.execute(new ItemListCriteria());
        assertThat("List of ItemInfoSnapshot", itemInfoSnapshots, is(notNullValue()));
        assertThat("List of ItemInfoSnapshot is empty", itemInfoSnapshots.isEmpty(), is(true));
    }

    @Test
    public void execute_queryReturnsNonEmptyList_returnsSnapshotList() {
        final ItemEntity itemEntity1 = new ItemEntity();
        itemEntity1.setKey(new ItemEntity.Key(1, 1, (short) 1));

        final ItemEntity itemEntity2 = new ItemEntity();
        itemEntity2.setKey(new ItemEntity.Key(1, 2, (short) 2));

        when(ENTITY_MANAGER.createNativeQuery(ItemListQuery.QUERY_BASE, ItemEntity.class)).thenReturn(QUERY);
        when(QUERY.getResultList()).thenReturn(Arrays.asList(itemEntity1, itemEntity2));

        final ItemListQuery itemListQuery = new ItemListQuery(ENTITY_MANAGER);
        final List<ItemInfoSnapshot> itemInfoSnapshots = itemListQuery.execute(new ItemListCriteria());

        assertThat("List of ItemInfoSnapshot", itemInfoSnapshots, is(notNullValue()));
        assertThat("List of ItemInfoSnapshot size", itemInfoSnapshots.size(), is(2));

        assertThat("List of ItemInfoSnapshot first element item id",
                itemInfoSnapshots.get(0).getItemId(), is(itemEntity1.getKey().getId()));
        assertThat("List of ItemInfoSnapshot first element chunk id",
                itemInfoSnapshots.get(0).getChunkId(), is(itemEntity1.getKey().getChunkId()));
        assertThat("List of ItemInfoSnapshot first element job id",
                itemInfoSnapshots.get(0).getJobId(), is(itemEntity1.getKey().getJobId()));


        assertThat("List of ItemInfoSnapshot second element item id",
                itemInfoSnapshots.get(1).getItemId(), is(itemEntity2.getKey().getId()));
        assertThat("List of ItemInfoSnapshot second element chunk id",
                itemInfoSnapshots.get(1).getChunkId(), is(itemEntity2.getKey().getChunkId()));
        assertThat("List of ItemInfoSnapshot second element job id",
                itemInfoSnapshots.get(1).getJobId(), is(itemEntity2.getKey().getJobId()));
    }

    @Test
    public void buildQueryString_noCriteria_returnsQueryBase() {
        final ItemListQuery itemListQuery = new ItemListQuery(ENTITY_MANAGER);
        assertThat(itemListQuery.buildQueryString(ItemListQuery.QUERY_BASE, new ItemListCriteria()), is(ItemListQuery.QUERY_BASE));
    }

    @Test
    public void buildQueryString_singleOrderByClause_returnsQueryString() {
        final String expectedQuery = ItemListQuery.QUERY_BASE + " ORDER BY id ASC";
        final ItemListQuery itemListQuery = new ItemListQuery(ENTITY_MANAGER);
        final ItemListCriteria itemListCriteria = new ItemListCriteria()
                .orderBy(new ListOrderBy<>(ItemListCriteria.Field.ITEM_ID, ListOrderBy.Sort.ASC));
        assertThat(itemListQuery.buildQueryString(ItemListQuery.QUERY_BASE, itemListCriteria), is(expectedQuery));
    }

    @Test
    public void buildQueryString_multipleOrderByClauses_returnsQueryString() {
        final String expectedQuery = ItemListQuery.QUERY_BASE + " ORDER BY id ASC, timeOfCreation DESC";
        final ItemListQuery itemListQuery = new ItemListQuery(ENTITY_MANAGER);
        final ItemListCriteria itemListCriteria = new ItemListCriteria()
                .orderBy(new ListOrderBy<>(ItemListCriteria.Field.ITEM_ID, ListOrderBy.Sort.ASC))
                .orderBy(new ListOrderBy<>(ItemListCriteria.Field.TIME_OF_CREATION, ListOrderBy.Sort.DESC));
        assertThat(itemListQuery.buildQueryString(ItemListQuery.QUERY_BASE, itemListCriteria), is(expectedQuery));
    }

    @Test
    public void buildQueryString_singleWhereClause_returnsQueryString() {
        final String expectedQuery = ItemListQuery.QUERY_BASE + " WHERE jobId=?1";
        final ItemListQuery itemListQuery = new ItemListQuery(ENTITY_MANAGER);
        final ItemListCriteria itemListCriteria = new ItemListCriteria()
                .where(new ListFilter<>(ItemListCriteria.Field.JOB_ID, ListFilter.Op.EQUAL, 33));
        assertThat(itemListQuery.buildQueryString(ItemListQuery.QUERY_BASE, itemListCriteria), is(expectedQuery));
    }

    @Test
    public void buildQueryString_singleWhereClauseMultipleFilters_returnsQueryString() {
        final String expectedQuery = ItemListQuery.QUERY_BASE + " WHERE id=?1 AND jobId=?2 OR timeOfCreation<?3";
        final ItemListQuery itemListQuery = new ItemListQuery(ENTITY_MANAGER);
        final ItemListCriteria itemListCriteria = new ItemListCriteria()
                .where(new ListFilter<>(ItemListCriteria.Field.ITEM_ID, ListFilter.Op.EQUAL, 42))
                .and(new ListFilter<>(ItemListCriteria.Field.JOB_ID, ListFilter.Op.EQUAL, 42))
                .or(new ListFilter<>(ItemListCriteria.Field.TIME_OF_CREATION, ListFilter.Op.LESS_THAN, 42));
        assertThat(itemListQuery.buildQueryString(ItemListQuery.QUERY_BASE, itemListCriteria), is(expectedQuery));
    }

    @Test
    public void buildQueryString_multipleWhereClauses_returnsQueryString() {
        final String expectedQuery = ItemListQuery.QUERY_BASE + " WHERE ( chunkId=?1 ) AND ( timeOfCreation>?2 )";
        final ItemListQuery itemListQuery = new ItemListQuery(ENTITY_MANAGER);
        final ItemListCriteria itemListCriteria = new ItemListCriteria()
                .where(new ListFilter<>(ItemListCriteria.Field.CHUNK_ID, ListFilter.Op.EQUAL, 42))
                .where(new ListFilter<>(ItemListCriteria.Field.TIME_OF_CREATION, ListFilter.Op.GREATER_THAN, 42));
        assertThat(itemListQuery.buildQueryString(ItemListQuery.QUERY_BASE, itemListCriteria), is(expectedQuery));
    }

    @Test
    public void buildQueryString_multipleWhereClausesMultipleFilters_returnsQueryString() {
        final String expectedQuery = ItemListQuery.QUERY_BASE + " WHERE ( id=?1 AND chunkId=?2 AND timeOfCreation<=?3 ) AND ( timeOfCreation>?4 )";
        final ItemListQuery itemListQuery = new ItemListQuery(ENTITY_MANAGER);
        final ItemListCriteria itemListCriteria = new ItemListCriteria()
                .where(new ListFilter<>(ItemListCriteria.Field.ITEM_ID, ListFilter.Op.EQUAL, 42))
                .and(new ListFilter<>(ItemListCriteria.Field.CHUNK_ID, ListFilter.Op.EQUAL, 43))
                .and(new ListFilter<>(ItemListCriteria.Field.TIME_OF_CREATION, ListFilter.Op.LESS_THAN_OR_EQUAL_TO, 42))
                .where(new ListFilter<>(ItemListCriteria.Field.TIME_OF_CREATION, ListFilter.Op.GREATER_THAN, 42));
        assertThat(itemListQuery.buildQueryString(ItemListQuery.QUERY_BASE, itemListCriteria), is(expectedQuery));
    }

    @Test
    public void buildQueryString_SingleWhereClauseMultipleFiltersMultipleOrderByClauses_returnsQueryString() {
        final String expectedQuery = ItemListQuery.QUERY_BASE +
                " WHERE jobId=?1 " +
                "AND (state->'states'->'PARTITIONING'->>'failed' != '0' " +
                "OR state->'states'->'PROCESSING'->>'failed' != '0' " +
                "OR state->'states'->'DELIVERING'->>'failed' != '0') " +
                "AND (state->'states'->'PARTITIONING'->>'ignored' != '0' " +
                "OR state->'states'->'PROCESSING'->>'ignored' != '0' " +
                "OR state->'states'->'DELIVERING'->>'ignored' != '0') " +
                "ORDER BY chunkId ASC, id ASC";
        final ItemListQuery itemListQuery = new ItemListQuery(ENTITY_MANAGER);
        final ItemListCriteria itemListCriteria = new ItemListCriteria()
                .where(new ListFilter<>(ItemListCriteria.Field.JOB_ID, ListFilter.Op.EQUAL, 42))
                .and(new ListFilter<>(ItemListCriteria.Field.STATE_FAILED))
                .and(new ListFilter<>(ItemListCriteria.Field.STATE_IGNORED))
                .orderBy(new ListOrderBy<>(ItemListCriteria.Field.CHUNK_ID, ListOrderBy.Sort.ASC))
                .orderBy(new ListOrderBy<>(ItemListCriteria.Field.ITEM_ID, ListOrderBy.Sort.ASC));
        assertThat(itemListQuery.buildQueryString(ItemListQuery.QUERY_BASE, itemListCriteria), is(expectedQuery));
    }
}
