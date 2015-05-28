package dk.dbc.dataio.jobstore.service.entity;

import dk.dbc.dataio.jobstore.types.criteria.ChunkListCriteria;
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

    @Test
    public void buildQueryString_noCriteria_returnsQueryBase() {
        final ChunkListQuery chunkListQuery = new ChunkListQuery(ENTITY_MANAGER);
        assertThat(chunkListQuery.buildQueryString(ChunkListQuery.QUERY_BASE, new ChunkListCriteria()), is(ChunkListQuery.QUERY_BASE));
    }

    @Test
    public void buildQueryString_singleOrderByClause_returnsQueryString() {
        final String expectedQuery = ChunkListQuery.QUERY_BASE + " ORDER BY timeOfCreation ASC";
        final ChunkListQuery chunkListQuery = new ChunkListQuery(ENTITY_MANAGER);
        final ChunkListCriteria chunkListCriteria = new ChunkListCriteria()
                .orderBy(new ListOrderBy<>(ChunkListCriteria.Field.TIME_OF_CREATION, ListOrderBy.Sort.ASC));
        assertThat(chunkListQuery.buildQueryString(ChunkListQuery.QUERY_BASE, chunkListCriteria), is(expectedQuery));
    }

    @Test
    public void buildQueryString_multipleOrderByClauses_returnsQueryString() {
        final String expectedQuery = ChunkListQuery.QUERY_BASE + " ORDER BY timeOfCreation ASC, timeOfCompletion DESC";
        final ChunkListQuery chunkListQuery = new ChunkListQuery(ENTITY_MANAGER);
        final ChunkListCriteria chunkListCriteria = new ChunkListCriteria()
                .orderBy(new ListOrderBy<>(ChunkListCriteria.Field.TIME_OF_CREATION, ListOrderBy.Sort.ASC))
                .orderBy(new ListOrderBy<>(ChunkListCriteria.Field.TIME_OF_COMPLETION, ListOrderBy.Sort.DESC));
        assertThat(chunkListQuery.buildQueryString(ChunkListQuery.QUERY_BASE, chunkListCriteria), is(expectedQuery));
    }

    @Test
    public void buildQueryString_singleWhereClauseWithUnaryExpression_returnsQueryString() {
        final String expectedQuery = ChunkListQuery.QUERY_BASE + " WHERE timeOfCompletion IS NULL";
        final ChunkListQuery chunkListQuery = new ChunkListQuery(ENTITY_MANAGER);
        final ChunkListCriteria chunkListCriteria = new ChunkListCriteria()
                .where(new ListFilter<>(ChunkListCriteria.Field.TIME_OF_COMPLETION, ListFilter.Op.IS_NULL));
        assertThat(chunkListQuery.buildQueryString(ChunkListQuery.QUERY_BASE, chunkListCriteria), is(expectedQuery));
    }

    @Test
    public void buildQueryString_singleWhereClauseWithBinaryExpression_returnsQueryString() {
        final String expectedQuery = ChunkListQuery.QUERY_BASE + " WHERE timeOfCompletion=?1";
        final ChunkListQuery chunkListQuery = new ChunkListQuery(ENTITY_MANAGER);
        final ChunkListCriteria chunkListCriteria = new ChunkListCriteria()
                .where(new ListFilter<>(ChunkListCriteria.Field.TIME_OF_COMPLETION, ListFilter.Op.EQUAL, 42));
        assertThat(chunkListQuery.buildQueryString(ChunkListQuery.QUERY_BASE, chunkListCriteria), is(expectedQuery));
    }

    @Test
    public void buildQueryString_singleWhereClauseMultipleFilters_returnsQueryString() {
        final String expectedQuery = ChunkListQuery.QUERY_BASE + " WHERE timeOfCreation=?1 OR timeOfCreation<?2 AND timeOfCompletion IS NOT NULL";
        final ChunkListQuery chunkListQuery = new ChunkListQuery(ENTITY_MANAGER);
        final ChunkListCriteria chunkListCriteria = new ChunkListCriteria()
                .where(new ListFilter<>(ChunkListCriteria.Field.TIME_OF_CREATION, ListFilter.Op.EQUAL, 42))
                .or(new ListFilter<>(ChunkListCriteria.Field.TIME_OF_CREATION, ListFilter.Op.LESS_THAN, 42))
                .and(new ListFilter<>(ChunkListCriteria.Field.TIME_OF_COMPLETION, ListFilter.Op.IS_NOT_NULL));
        assertThat(chunkListQuery.buildQueryString(ChunkListQuery.QUERY_BASE, chunkListCriteria), is(expectedQuery));
    }

    @Test
    public void buildQueryString_multipleWhereClauses_returnsQueryString() {
        final String expectedQuery = ChunkListQuery.QUERY_BASE + " WHERE ( timeOfCreation=?1 ) AND ( timeOfCreation IS NOT NULL )";
        final ChunkListQuery chunkListQuery = new ChunkListQuery(ENTITY_MANAGER);
        final ChunkListCriteria chunkListCriteria = new ChunkListCriteria()
                .where(new ListFilter<>(ChunkListCriteria.Field.TIME_OF_CREATION, ListFilter.Op.EQUAL, 42))
                .where(new ListFilter<>(ChunkListCriteria.Field.TIME_OF_CREATION, ListFilter.Op.IS_NOT_NULL));
        assertThat(chunkListQuery.buildQueryString(ChunkListQuery.QUERY_BASE, chunkListCriteria), is(expectedQuery));
    }

    @Test
    public void buildQueryString_multipleWhereClausesMultipleFilters_returnsQueryString() {
        final String expectedQuery = ChunkListQuery.QUERY_BASE + " WHERE ( timeOfCreation=?1 AND timeOfCreation IS NOT NULL OR timeOfCreation>?2 ) AND ( timeOfCompletion IS NULL )";
        final ChunkListQuery chunkListQuery = new ChunkListQuery(ENTITY_MANAGER);
        final ChunkListCriteria chunkListCriteria = new ChunkListCriteria()
                .where(new ListFilter<>(ChunkListCriteria.Field.TIME_OF_CREATION, ListFilter.Op.EQUAL, 42))
                .and(new ListFilter<>(ChunkListCriteria.Field.TIME_OF_CREATION, ListFilter.Op.IS_NOT_NULL))
                .or(new ListFilter<>(ChunkListCriteria.Field.TIME_OF_CREATION, ListFilter.Op.GREATER_THAN, 42))
                .where(new ListFilter<>(ChunkListCriteria.Field.TIME_OF_COMPLETION, ListFilter.Op.IS_NULL));
        assertThat(chunkListQuery.buildQueryString(ChunkListQuery.QUERY_BASE, chunkListCriteria), is(expectedQuery));
    }

    @Test
    public void buildQueryString_SingleWhereClauseMultipleOrderByClauses_returnsQueryString() {
        final String expectedQuery = ChunkListQuery.QUERY_BASE +
                " WHERE timeOfCreation>=?1 ORDER BY timeOfCreation ASC, timeOfCompletion ASC";
        final ChunkListQuery chunkListQuery = new ChunkListQuery(ENTITY_MANAGER);
        final ChunkListCriteria chunkListCriteria = new ChunkListCriteria()
                .where(new ListFilter<>(ChunkListCriteria.Field.TIME_OF_CREATION, ListFilter.Op.GREATER_THAN_OR_EQUAL_TO, 42))
                .orderBy(new ListOrderBy<>(ChunkListCriteria.Field.TIME_OF_CREATION, ListOrderBy.Sort.ASC))
                .orderBy(new ListOrderBy<>(ChunkListCriteria.Field.TIME_OF_COMPLETION, ListOrderBy.Sort.ASC));
        assertThat(chunkListQuery.buildQueryString(ChunkListQuery.QUERY_BASE, chunkListCriteria), is(expectedQuery));
    }

    @Test
    public void buildQueryString_SingleWhereClauseLimitClause_returnsQueryString() {
        final String expectedQuery = ChunkListQuery.QUERY_BASE + " LIMIT 10";
        final ChunkListQuery chunkListQuery = new ChunkListQuery(ENTITY_MANAGER);
        final ChunkListCriteria chunkListCriteria = new ChunkListCriteria().limit(10);
        assertThat(chunkListQuery.buildQueryString(ChunkListQuery.QUERY_BASE, chunkListCriteria), is(expectedQuery));
    }

    @Test
    public void buildQueryString_SingleWhereClauseLimitClauseZero_returnsQueryString() {
        final String expectedQuery = ChunkListQuery.QUERY_BASE;
        final ChunkListQuery chunkListQuery = new ChunkListQuery(ENTITY_MANAGER);
        final ChunkListCriteria chunkListCriteria = new ChunkListCriteria().limit(0);
        assertThat(chunkListQuery.buildQueryString(ChunkListQuery.QUERY_BASE, chunkListCriteria), is(expectedQuery));
    }

    @Test
    public void buildQueryString_SingleWhereClauseOffsetClause_returnsQueryString() {
        final String expectedQuery = ChunkListQuery.QUERY_BASE + " OFFSET 20";
        final ChunkListQuery chunkListQuery = new ChunkListQuery(ENTITY_MANAGER);
        final ChunkListCriteria chunkListCriteria = new ChunkListCriteria().offset(20);
        assertThat(chunkListQuery.buildQueryString(ChunkListQuery.QUERY_BASE, chunkListCriteria), is(expectedQuery));
    }

    @Test
    public void buildQueryString_SingleWhereClauseOffsetClauseZero_returnsQueryString() {
        final String expectedQuery = ChunkListQuery.QUERY_BASE;
        final ChunkListQuery chunkListQuery = new ChunkListQuery(ENTITY_MANAGER);
        final ChunkListCriteria chunkListCriteria = new ChunkListCriteria().offset(0);
        assertThat(chunkListQuery.buildQueryString(ChunkListQuery.QUERY_BASE, chunkListCriteria), is(expectedQuery));
    }
}
