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

import dk.dbc.dataio.jobstore.types.criteria.ListCriteria;
import dk.dbc.dataio.jobstore.types.criteria.ListFilter;
import dk.dbc.dataio.jobstore.types.criteria.ListFilterField;
import dk.dbc.dataio.jobstore.types.criteria.ListOrderBy;
import org.junit.Test;

import java.util.Date;
import java.util.List;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

public class ListQueryTest {
    private static final String FIELD_OBJECT_NAME = "field_object";
    private static final String FIELD_TIMESTAMP_NAME = "field_timestamp";
    private static final String VERBATIM_FIELD_JSONB_NAME = "verbatim_field_jsonb";

    private static final String VERBATIM_VALUE = "value";

    @Test
    public void buildQueryString_noCriteria_returnsQueryBase() {
        final ListQueryImpl listQuery = new ListQueryImpl();
        assertThat(listQuery.buildQueryString(ListQueryImpl.QUERY_BASE, new ListCriteriaImpl()), is(ListQueryImpl.QUERY_BASE));
    }

    @Test
    public void buildQueryString_singleOrderByClause_returnsQueryString() {
        final String expectedQuery = ListQueryImpl.QUERY_BASE + " ORDER BY " + FIELD_OBJECT_NAME + " ASC";
        final ListQueryImpl listQuery = new ListQueryImpl();
        final ListCriteriaImpl listCriteria = new ListCriteriaImpl()
                .orderBy(new ListOrderBy<>(ListCriteriaImpl.Field.FIELD_OBJECT, ListOrderBy.Sort.ASC));
        assertThat(listQuery.buildQueryString(ListQueryImpl.QUERY_BASE, listCriteria), is(expectedQuery));
    }

    @Test
    public void buildQueryString_multipleOrderByClauses_returnsQueryString() {
        final String expectedQuery = ListQueryImpl.QUERY_BASE + " ORDER BY " + FIELD_OBJECT_NAME + " ASC, " + FIELD_TIMESTAMP_NAME + " DESC";
        final ListQueryImpl listQuery = new ListQueryImpl();
        final ListCriteriaImpl listCriteria = new ListCriteriaImpl()
                .orderBy(new ListOrderBy<>(ListCriteriaImpl.Field.FIELD_OBJECT, ListOrderBy.Sort.ASC))
                .orderBy(new ListOrderBy<>(ListCriteriaImpl.Field.FIELD_TIMESTAMP, ListOrderBy.Sort.DESC));
        assertThat(listQuery.buildQueryString(ListQueryImpl.QUERY_BASE, listCriteria), is(expectedQuery));
    }

    @Test
    public void buildQueryString_whereClauseWithBooleanOpFieldWithEqualsOperator_returnsQueryString() {
        final String expectedQuery = ListQueryImpl.QUERY_BASE + " WHERE " + FIELD_OBJECT_NAME + "=?1";
        final ListQueryImpl listQuery = new ListQueryImpl();
        final ListCriteriaImpl listCriteria = new ListCriteriaImpl()
                .where(new ListFilter<>(ListCriteriaImpl.Field.FIELD_OBJECT, ListFilter.Op.EQUAL, 42));
        assertThat(listQuery.buildQueryString(ListQueryImpl.QUERY_BASE, listCriteria), is(expectedQuery));
    }

    @Test
    public void buildQueryString_whereClauseWithBooleanOpFieldWithGreaterThanOperator_returnsQueryString() {
        final String expectedQuery = ListQueryImpl.QUERY_BASE + " WHERE " + FIELD_OBJECT_NAME + ">?1";
        final ListQueryImpl listQuery = new ListQueryImpl();
        final ListCriteriaImpl listCriteria = new ListCriteriaImpl()
                .where(new ListFilter<>(ListCriteriaImpl.Field.FIELD_OBJECT, ListFilter.Op.GREATER_THAN, 42));
        assertThat(listQuery.buildQueryString(ListQueryImpl.QUERY_BASE, listCriteria), is(expectedQuery));
    }

    @Test
    public void buildQueryString_whereClauseWithBooleanOpFieldWithGreaterThanOrEqualToOperator_returnsQueryString() {
        final String expectedQuery = ListQueryImpl.QUERY_BASE + " WHERE " + FIELD_OBJECT_NAME + ">=?1";
        final ListQueryImpl listQuery = new ListQueryImpl();
        final ListCriteriaImpl listCriteria = new ListCriteriaImpl()
                .where(new ListFilter<>(ListCriteriaImpl.Field.FIELD_OBJECT, ListFilter.Op.GREATER_THAN_OR_EQUAL_TO, 42));
        assertThat(listQuery.buildQueryString(ListQueryImpl.QUERY_BASE, listCriteria), is(expectedQuery));
    }

    @Test
    public void buildQueryString_whereClauseWithBooleanOpFieldWithLessThanOperator_returnsQueryString() {
        final String expectedQuery = ListQueryImpl.QUERY_BASE + " WHERE " + FIELD_OBJECT_NAME + "<?1";
        final ListQueryImpl listQuery = new ListQueryImpl();
        final ListCriteriaImpl listCriteria = new ListCriteriaImpl()
                .where(new ListFilter<>(ListCriteriaImpl.Field.FIELD_OBJECT, ListFilter.Op.LESS_THAN, 42));
        assertThat(listQuery.buildQueryString(ListQueryImpl.QUERY_BASE, listCriteria), is(expectedQuery));
    }

    @Test
    public void buildQueryString_whereClauseWithBooleanOpFieldWithLessThanOrEqualToOperator_returnsQueryString() {
        final String expectedQuery = ListQueryImpl.QUERY_BASE + " WHERE " + FIELD_OBJECT_NAME + "<=?1";
        final ListQueryImpl listQuery = new ListQueryImpl();
        final ListCriteriaImpl listCriteria = new ListCriteriaImpl()
                .where(new ListFilter<>(ListCriteriaImpl.Field.FIELD_OBJECT, ListFilter.Op.LESS_THAN_OR_EQUAL_TO, 42));
        assertThat(listQuery.buildQueryString(ListQueryImpl.QUERY_BASE, listCriteria), is(expectedQuery));
    }

    @Test
    public void buildQueryString_whereClauseWithBooleanOpFieldWithNotEqualOperator_returnsQueryString() {
        final String expectedQuery = ListQueryImpl.QUERY_BASE + " WHERE " + FIELD_OBJECT_NAME + "!=?1";
        final ListQueryImpl listQuery = new ListQueryImpl();
        final ListCriteriaImpl listCriteria = new ListCriteriaImpl()
                .where(new ListFilter<>(ListCriteriaImpl.Field.FIELD_OBJECT, ListFilter.Op.NOT_EQUAL, 42));
        assertThat(listQuery.buildQueryString(ListQueryImpl.QUERY_BASE, listCriteria), is(expectedQuery));
    }

    @Test
    public void buildQueryString_whereClauseWithBooleanOpFieldWithIsNullOperator_returnsQueryString() {
        final String expectedQuery = ListQueryImpl.QUERY_BASE + " WHERE " + FIELD_OBJECT_NAME + " IS NULL";
        final ListQueryImpl listQuery = new ListQueryImpl();
        final ListCriteriaImpl listCriteria = new ListCriteriaImpl()
                .where(new ListFilter<>(ListCriteriaImpl.Field.FIELD_OBJECT, ListFilter.Op.IS_NULL, 42));
        assertThat(listQuery.buildQueryString(ListQueryImpl.QUERY_BASE, listCriteria), is(expectedQuery));
    }

    @Test
    public void buildQueryString_whereClauseWithBooleanOpFieldWithIsNotNullOperator_returnsQueryString() {
        final String expectedQuery = ListQueryImpl.QUERY_BASE + " WHERE " + FIELD_OBJECT_NAME + " IS NOT NULL";
        final ListQueryImpl listQuery = new ListQueryImpl();
        final ListCriteriaImpl listCriteria = new ListCriteriaImpl()
                .where(new ListFilter<>(ListCriteriaImpl.Field.FIELD_OBJECT, ListFilter.Op.IS_NOT_NULL, 42));
        assertThat(listQuery.buildQueryString(ListQueryImpl.QUERY_BASE, listCriteria), is(expectedQuery));
    }

    @Test
    public void buildQueryString_whereClauseWithBooleanOpFieldWithJsonLeftContainsOperator_returnsQueryString() {
        final String expectedQuery = ListQueryImpl.QUERY_BASE + " WHERE " + FIELD_OBJECT_NAME + "@>?1";
        final ListQueryImpl listQuery = new ListQueryImpl();
        final ListCriteriaImpl listCriteria = new ListCriteriaImpl()
                .where(new ListFilter<>(ListCriteriaImpl.Field.FIELD_OBJECT, ListFilter.Op.JSON_LEFT_CONTAINS, "42"));
        assertThat(listQuery.buildQueryString(ListQueryImpl.QUERY_BASE, listCriteria), is(expectedQuery));
    }

    @Test
    public void buildQueryString_whereClauseWithBooleanOpFieldWithJsonNotLeftContainsOperator_returnsQueryString() {
        final String expectedQuery = ListQueryImpl.QUERY_BASE + " WHERE NOT " + FIELD_OBJECT_NAME + "@>?1";
        final ListQueryImpl listQuery = new ListQueryImpl();
        final ListCriteriaImpl listCriteria = new ListCriteriaImpl()
                .where(new ListFilter<>(ListCriteriaImpl.Field.FIELD_OBJECT, ListFilter.Op.JSON_NOT_LEFT_CONTAINS, "42"));
        assertThat(listQuery.buildQueryString(ListQueryImpl.QUERY_BASE, listCriteria), is(expectedQuery));
    }

    @Test
    public void buildQueryString_whereClauseWithBooleanOpFieldWithObjectValue_returnsQueryString() {
        final String expectedQuery = ListQueryImpl.QUERY_BASE + " WHERE " + FIELD_OBJECT_NAME + "=?1";
        final ListQueryImpl listQuery = new ListQueryImpl();
        final ListCriteriaImpl listCriteria = new ListCriteriaImpl()
                .where(new ListFilter<>(ListCriteriaImpl.Field.FIELD_OBJECT, ListFilter.Op.EQUAL, 42));
        assertThat(listQuery.buildQueryString(ListQueryImpl.QUERY_BASE, listCriteria), is(expectedQuery));
    }

    @Test
    public void buildQueryString_whereClauseWithBooleanOpFieldWithTimestampValue_returnsQueryString() {
        final String expectedQuery = ListQueryImpl.QUERY_BASE + " WHERE " + FIELD_TIMESTAMP_NAME + "=?1";
        final ListQueryImpl listQuery = new ListQueryImpl();
        final ListCriteriaImpl listCriteria = new ListCriteriaImpl()
                .where(new ListFilter<>(ListCriteriaImpl.Field.FIELD_TIMESTAMP, ListFilter.Op.EQUAL, new Date()));
        assertThat(listQuery.buildQueryString(ListQueryImpl.QUERY_BASE, listCriteria), is(expectedQuery));
    }

    @Test
    public void buildQueryString_whereClauseWithVerbatimBooleanOpFieldWithJsonbValue_returnsEscapedQueryString() {
        final String jsonObject = "{type:\"''\"}";
        final String expectedQuery = ListQueryImpl.QUERY_BASE + " WHERE " + VERBATIM_FIELD_JSONB_NAME + "@>'{type:\"''''\"}'::jsonb";
        final ListQueryImpl listQuery = new ListQueryImpl();
        final ListCriteriaImpl listCriteria = new ListCriteriaImpl()
                .where(new ListFilter<>(ListCriteriaImpl.Field.VERBATIM_FIELD_JSONB, ListFilter.Op.JSON_LEFT_CONTAINS, jsonObject));
        assertThat(listQuery.buildQueryString(ListQueryImpl.QUERY_BASE, listCriteria), is(expectedQuery));
    }

    @Test
    public void buildQueryString_whereClauseWithVerbatimBooleanOpFieldWithNotJsonbValue_returnsEscapedQueryString() {
        final String jsonObject = "{type:\"''\"}";
        final String expectedQuery = ListQueryImpl.QUERY_BASE + " WHERE NOT " + VERBATIM_FIELD_JSONB_NAME + "@>'{type:\"''''\"}'::jsonb";
        final ListQueryImpl listQuery = new ListQueryImpl();
        final ListCriteriaImpl listCriteria = new ListCriteriaImpl()
                .where(new ListFilter<>(ListCriteriaImpl.Field.VERBATIM_FIELD_JSONB, ListFilter.Op.JSON_NOT_LEFT_CONTAINS, jsonObject));
        assertThat(listQuery.buildQueryString(ListQueryImpl.QUERY_BASE, listCriteria), is(expectedQuery));
    }

    @Test
    public void buildQueryString_whereClauseWithVerbatimField_returnsQueryString() {
        final String expectedQuery = ListQueryImpl.QUERY_BASE + " WHERE " + VERBATIM_VALUE;
        final ListQueryImpl listQuery = new ListQueryImpl();
        final ListCriteriaImpl listCriteria = new ListCriteriaImpl()
                .where(new ListFilter<>(ListCriteriaImpl.Field.VERBATIM_FIELD, ListFilter.Op.NOOP, 42));
        assertThat(listQuery.buildQueryString(ListQueryImpl.QUERY_BASE, listCriteria), is(expectedQuery));
    }

    @Test
    public void buildQueryString_whereClauseWithMultipleFiltersStartingWithBooleanOpField_returnsQueryString() {
        final String jsonObject = "{}";
        final String expectedQuery = ListQueryImpl.QUERY_BASE + " WHERE " + FIELD_OBJECT_NAME + "=?1 AND " + VERBATIM_FIELD_JSONB_NAME + "@>'" + jsonObject + "'::jsonb";
        final ListQueryImpl listQuery = new ListQueryImpl();
        final ListCriteriaImpl listCriteria = new ListCriteriaImpl()
                .where(new ListFilter<>(ListCriteriaImpl.Field.FIELD_OBJECT, ListFilter.Op.EQUAL, 42))
                .and(new ListFilter<>(ListCriteriaImpl.Field.VERBATIM_FIELD_JSONB, ListFilter.Op.JSON_LEFT_CONTAINS, jsonObject));
        assertThat(listQuery.buildQueryString(ListQueryImpl.QUERY_BASE, listCriteria), is(expectedQuery));
    }

    @Test
    public void buildQueryString_whereClauseWithMultipleFiltersStartingWithNegatedBooleanOpField_returnsQueryString() {
        final String jsonObject = "{}";
        final String expectedQuery = ListQueryImpl.QUERY_BASE + " WHERE " + FIELD_OBJECT_NAME + "=?1 AND NOT " + VERBATIM_FIELD_JSONB_NAME + "@>'" + jsonObject + "'::jsonb";
        final ListQueryImpl listQuery = new ListQueryImpl();
        final ListCriteriaImpl listCriteria = new ListCriteriaImpl()
                .where(new ListFilter<>(ListCriteriaImpl.Field.FIELD_OBJECT, ListFilter.Op.EQUAL, 42))
                .and(new ListFilter<>(ListCriteriaImpl.Field.VERBATIM_FIELD_JSONB, ListFilter.Op.JSON_NOT_LEFT_CONTAINS, jsonObject));
        assertThat(listQuery.buildQueryString(ListQueryImpl.QUERY_BASE, listCriteria), is(expectedQuery));
    }

    @Test
    public void buildQueryString_whereClauseWithMultipleFiltersStartingWithVerbatimBooleanOpField_returnsQueryString() {
        final String jsonObject = "{}";
        final String expectedQuery = ListQueryImpl.QUERY_BASE + " WHERE " + VERBATIM_FIELD_JSONB_NAME + "@>'" + jsonObject + "'::jsonb AND " + FIELD_OBJECT_NAME + "=?1";
        final ListQueryImpl listQuery = new ListQueryImpl();
        final ListCriteriaImpl listCriteria = new ListCriteriaImpl()
                .where(new ListFilter<>(ListCriteriaImpl.Field.VERBATIM_FIELD_JSONB, ListFilter.Op.JSON_LEFT_CONTAINS, jsonObject))
                .and(new ListFilter<>(ListCriteriaImpl.Field.FIELD_OBJECT, ListFilter.Op.EQUAL, 42));
        assertThat(listQuery.buildQueryString(ListQueryImpl.QUERY_BASE, listCriteria), is(expectedQuery));
    }

    @Test
    public void buildQueryString_whereClauseWithMultipleFiltersStartingWithNegatedVerbatimBooleanOpField_returnsQueryString() {
        final String jsonObject = "{}";
        final String expectedQuery = ListQueryImpl.QUERY_BASE + " WHERE NOT " + VERBATIM_FIELD_JSONB_NAME + "@>'" + jsonObject + "'::jsonb AND " + FIELD_OBJECT_NAME + "=?1";
        final ListQueryImpl listQuery = new ListQueryImpl();
        final ListCriteriaImpl listCriteria = new ListCriteriaImpl()
                .where(new ListFilter<>(ListCriteriaImpl.Field.VERBATIM_FIELD_JSONB, ListFilter.Op.JSON_NOT_LEFT_CONTAINS, jsonObject))
                .and(new ListFilter<>(ListCriteriaImpl.Field.FIELD_OBJECT, ListFilter.Op.EQUAL, 42));
        assertThat(listQuery.buildQueryString(ListQueryImpl.QUERY_BASE, listCriteria), is(expectedQuery));
    }

    @Test
    public void buildQueryString_whereClauseWithMultipleFiltersStartingWithVerbatimField_returnsQueryString() {
        final String expectedQuery = ListQueryImpl.QUERY_BASE + " WHERE " + VERBATIM_VALUE + " AND " + FIELD_OBJECT_NAME + "=?1";
        final ListQueryImpl listQuery = new ListQueryImpl();
        final ListCriteriaImpl listCriteria = new ListCriteriaImpl()
                .where(new ListFilter<>(ListCriteriaImpl.Field.VERBATIM_FIELD, ListFilter.Op.NOOP, 42))
                .and(new ListFilter<>(ListCriteriaImpl.Field.FIELD_OBJECT, ListFilter.Op.EQUAL, 42));
        assertThat(listQuery.buildQueryString(ListQueryImpl.QUERY_BASE, listCriteria), is(expectedQuery));
    }

    @Test
    public void buildQueryString_multipleWhereClauses_returnsQueryString() {
        final String expectedQuery = ListQueryImpl.QUERY_BASE + " WHERE ( " + FIELD_OBJECT_NAME + "=?1 ) AND ( " + FIELD_TIMESTAMP_NAME + "=?2 )";
        final ListQueryImpl listQuery = new ListQueryImpl();
        final ListCriteriaImpl listCriteria = new ListCriteriaImpl()
                .where(new ListFilter<>(ListCriteriaImpl.Field.FIELD_OBJECT, ListFilter.Op.EQUAL, 42))
                .where(new ListFilter<>(ListCriteriaImpl.Field.FIELD_TIMESTAMP, ListFilter.Op.EQUAL, new Date()));
        assertThat(listQuery.buildQueryString(ListQueryImpl.QUERY_BASE, listCriteria), is(expectedQuery));
    }

    @Test
    public void buildQueryString_multipleWhereClausesWithMultipleFilters_returnsQueryString() {
        final String expectedQuery = ListQueryImpl.QUERY_BASE + " WHERE ( " + FIELD_OBJECT_NAME + "=?1 AND " + FIELD_TIMESTAMP_NAME + "=?2 ) AND ( " + FIELD_OBJECT_NAME + ">?3 OR " + FIELD_TIMESTAMP_NAME + ">?4 )";
        final ListQueryImpl listQuery = new ListQueryImpl();
        final ListCriteriaImpl listCriteria = new ListCriteriaImpl()
                .where(new ListFilter<>(ListCriteriaImpl.Field.FIELD_OBJECT, ListFilter.Op.EQUAL, 42))
                .and(new ListFilter<>(ListCriteriaImpl.Field.FIELD_TIMESTAMP, ListFilter.Op.EQUAL, new Date()))
                .where(new ListFilter<>(ListCriteriaImpl.Field.FIELD_OBJECT, ListFilter.Op.GREATER_THAN, 42))
                .or(new ListFilter<>(ListCriteriaImpl.Field.FIELD_TIMESTAMP, ListFilter.Op.GREATER_THAN, new Date()));
        assertThat(listQuery.buildQueryString(ListQueryImpl.QUERY_BASE, listCriteria), is(expectedQuery));
    }

    @Test
    public void buildQueryString_limitClause_returnsQueryString() {
        final String expectedQuery = ListQueryImpl.QUERY_BASE + " LIMIT 10";
        final ListQueryImpl listQuery = new ListQueryImpl();
        final ListCriteriaImpl listCriteria = new ListCriteriaImpl()
                .limit(10);
        assertThat(listQuery.buildQueryString(ListQueryImpl.QUERY_BASE, listCriteria), is(expectedQuery));
    }

    @Test
    public void buildQueryString_limitClauseZero_returnsQueryString() {
        final String expectedQuery = ListQueryImpl.QUERY_BASE;
        final ListQueryImpl listQuery = new ListQueryImpl();
        final ListCriteriaImpl listCriteria = new ListCriteriaImpl()
                .limit(0);
        assertThat(listQuery.buildQueryString(ListQueryImpl.QUERY_BASE, listCriteria), is(expectedQuery));
    }

    @Test
    public void buildQueryString_offsetClause_returnsQueryString() {
        final String expectedQuery = ListQueryImpl.QUERY_BASE + " OFFSET 20";
        final ListQueryImpl listQuery = new ListQueryImpl();
        final ListCriteriaImpl listCriteria = new ListCriteriaImpl()
                .offset(20);
        assertThat(listQuery.buildQueryString(ListQueryImpl.QUERY_BASE, listCriteria), is(expectedQuery));
    }

    @Test
    public void buildQueryString_offsetClauseZero_returnsQueryString() {
        final String expectedQuery = ListQueryImpl.QUERY_BASE;
        final ListQueryImpl listQuery = new ListQueryImpl();
        final ListCriteriaImpl listCriteria = new ListCriteriaImpl()
                .offset(0);
        assertThat(listQuery.buildQueryString(ListQueryImpl.QUERY_BASE, listCriteria), is(expectedQuery));
    }

    @Test
    public void buildQueryString_allConstructsCombined_returnsQueryString() {
        final String expectedQuery = ListQueryImpl.QUERY_BASE +
                " WHERE ( " + FIELD_OBJECT_NAME + "=?1 AND value )" +
                " AND ( " + FIELD_OBJECT_NAME + ">?2 OR " + VERBATIM_FIELD_JSONB_NAME + "@>'{}'::jsonb OR NOT " + VERBATIM_FIELD_JSONB_NAME + "@>'{}'::jsonb )" +
                " ORDER BY " + FIELD_OBJECT_NAME + " ASC, " + FIELD_TIMESTAMP_NAME + " DESC" +
                " LIMIT 10" +
                " OFFSET 2";
        final ListQueryImpl listQuery = new ListQueryImpl();
        final ListCriteriaImpl listCriteria = new ListCriteriaImpl()
                .where(new ListFilter<>(ListCriteriaImpl.Field.FIELD_OBJECT, ListFilter.Op.EQUAL, 42))
                .and(new ListFilter<>(ListCriteriaImpl.Field.VERBATIM_FIELD, ListFilter.Op.NOOP, 42))
                .where(new ListFilter<>(ListCriteriaImpl.Field.FIELD_OBJECT, ListFilter.Op.GREATER_THAN, 42))
                .or(new ListFilter<>(ListCriteriaImpl.Field.VERBATIM_FIELD_JSONB, ListFilter.Op.JSON_LEFT_CONTAINS, "{}"))
                .or(new ListFilter<>(ListCriteriaImpl.Field.VERBATIM_FIELD_JSONB, ListFilter.Op.JSON_NOT_LEFT_CONTAINS, "{}"))
                .limit(100)
                .limit(10)
                .offset(20)
                .offset(2)
                .orderBy(new ListOrderBy<>(ListCriteriaImpl.Field.FIELD_OBJECT, ListOrderBy.Sort.ASC))
                .orderBy(new ListOrderBy<>(ListCriteriaImpl.Field.FIELD_TIMESTAMP, ListOrderBy.Sort.DESC));
        assertThat(listQuery.buildQueryString(ListQueryImpl.QUERY_BASE, listCriteria), is(expectedQuery));
    }

    @Test
    public void buildCountQueryString_allConstructsCombined_returnsQueryString() {
        final String expectedQuery = ListQueryImpl.QUERY_BASE +
                " WHERE ( " + FIELD_OBJECT_NAME + "=?1 AND value )" +
                " AND ( " + FIELD_OBJECT_NAME + ">?2 OR " + VERBATIM_FIELD_JSONB_NAME + "@>'{}'::jsonb OR NOT " + VERBATIM_FIELD_JSONB_NAME + "@>'{}'::jsonb )";

        final ListQueryImpl listQuery = new ListQueryImpl();
        final ListCriteriaImpl listCriteria = new ListCriteriaImpl()
                .where(new ListFilter<>(ListCriteriaImpl.Field.FIELD_OBJECT, ListFilter.Op.EQUAL, 42))
                .and(new ListFilter<>(ListCriteriaImpl.Field.VERBATIM_FIELD, ListFilter.Op.NOOP, 42))
                .where(new ListFilter<>(ListCriteriaImpl.Field.FIELD_OBJECT, ListFilter.Op.GREATER_THAN, 42))
                .or(new ListFilter<>(ListCriteriaImpl.Field.VERBATIM_FIELD_JSONB, ListFilter.Op.JSON_LEFT_CONTAINS, "{}"))
                .or(new ListFilter<>(ListCriteriaImpl.Field.VERBATIM_FIELD_JSONB, ListFilter.Op.JSON_NOT_LEFT_CONTAINS, "{}"))
                .limit(100)
                .limit(10)
                .offset(20)
                .offset(2)
                .orderBy(new ListOrderBy<>(ListCriteriaImpl.Field.FIELD_OBJECT, ListOrderBy.Sort.ASC))
                .orderBy(new ListOrderBy<>(ListCriteriaImpl.Field.FIELD_TIMESTAMP, ListOrderBy.Sort.DESC));
        assertThat(listQuery.buildCountQueryString(ListQueryImpl.QUERY_BASE, listCriteria), is(expectedQuery));
    }


    @Test
    public void buildQueryString_multipleFilterGroupsNoBindParameterInFirstGroup_returnsQueryString() {
        final String expectedQuery = ListQueryImpl.QUERY_BASE +
                " WHERE ( value )" +
                " AND ( " + FIELD_OBJECT_NAME + ">?1 OR " + VERBATIM_FIELD_JSONB_NAME + "@>'{}'::jsonb OR NOT " + VERBATIM_FIELD_JSONB_NAME + "@>'{}'::jsonb )" +
                " ORDER BY " + FIELD_OBJECT_NAME + " ASC, " + FIELD_TIMESTAMP_NAME + " DESC" +
                " LIMIT 10" +
                " OFFSET 2";
        final ListQueryImpl listQuery = new ListQueryImpl();
        final ListCriteriaImpl listCriteria = new ListCriteriaImpl()
                .where(new ListFilter<>(ListCriteriaImpl.Field.VERBATIM_FIELD, ListFilter.Op.NOOP, 42))
                .where(new ListFilter<>(ListCriteriaImpl.Field.FIELD_OBJECT, ListFilter.Op.GREATER_THAN, 42))
                .or(new ListFilter<>(ListCriteriaImpl.Field.VERBATIM_FIELD_JSONB, ListFilter.Op.JSON_LEFT_CONTAINS, "{}"))
                .or(new ListFilter<>(ListCriteriaImpl.Field.VERBATIM_FIELD_JSONB, ListFilter.Op.JSON_NOT_LEFT_CONTAINS, "{}"))
                .limit(100)
                .limit(10)
                .offset(20)
                .offset(2)
                .orderBy(new ListOrderBy<>(ListCriteriaImpl.Field.FIELD_OBJECT, ListOrderBy.Sort.ASC))
                .orderBy(new ListOrderBy<>(ListCriteriaImpl.Field.FIELD_TIMESTAMP, ListOrderBy.Sort.DESC));
        assertThat(listQuery.buildQueryString(ListQueryImpl.QUERY_BASE, listCriteria), is(expectedQuery));
    }

    @Test
    public void buildQueryString_jsonSelectField_returnsQueryString() {
        final String expectedQuery = ListQueryImpl.QUERY_BASE + " WHERE id IN (SELECT jobid FROM item WHERE recordinfo->>'id' = ?1)";
        final ListQueryImpl listQuery = new ListQueryImpl();
        final ListCriteriaImpl listCriteria = new ListCriteriaImpl()
                .where(new ListFilter<>(ListCriteriaImpl.Field.JSON_SELECT_FIELD, ListFilter.Op.IN, "00982369"));
        assertThat(listQuery.buildQueryString(ListQueryImpl.QUERY_BASE, listCriteria), is(expectedQuery));
    }


    @Test
    public void buildCountQueryString_allConstructsCombinedWithOneInverte_returnsQueryString() {
        final String expectedQuery = ListQueryImpl.QUERY_BASE +
                " WHERE ( " + FIELD_OBJECT_NAME + "=?1 AND value )" +
                " AND ( NOT ( " + FIELD_OBJECT_NAME + ">?2 OR " + VERBATIM_FIELD_JSONB_NAME + "@>'{}'::jsonb OR NOT " + VERBATIM_FIELD_JSONB_NAME + "@>'{}'::jsonb ) )";

        final ListQueryImpl listQuery = new ListQueryImpl();
        final ListCriteriaImpl listCriteria = new ListCriteriaImpl()
                .where(new ListFilter<>(ListCriteriaImpl.Field.FIELD_OBJECT, ListFilter.Op.EQUAL, 42))
                .and(new ListFilter<>(ListCriteriaImpl.Field.VERBATIM_FIELD, ListFilter.Op.NOOP, 42))
                .where(new ListFilter<>(ListCriteriaImpl.Field.FIELD_OBJECT, ListFilter.Op.GREATER_THAN, 42))
                .or(new ListFilter<>(ListCriteriaImpl.Field.VERBATIM_FIELD_JSONB, ListFilter.Op.JSON_LEFT_CONTAINS, "{}"))
                .or(new ListFilter<>(ListCriteriaImpl.Field.VERBATIM_FIELD_JSONB, ListFilter.Op.JSON_NOT_LEFT_CONTAINS, "{}")).not()
                .limit(100)
                .limit(10)
                .offset(20)
                .offset(2)
                .orderBy(new ListOrderBy<>(ListCriteriaImpl.Field.FIELD_OBJECT, ListOrderBy.Sort.ASC))
                .orderBy(new ListOrderBy<>(ListCriteriaImpl.Field.FIELD_TIMESTAMP, ListOrderBy.Sort.DESC));
        assertThat(listQuery.buildCountQueryString(ListQueryImpl.QUERY_BASE, listCriteria), is(expectedQuery));
    }

    //------------------------------------------------------------------------------------------------------------------

    public class ListQueryImpl extends ListQuery<ListCriteriaImpl, ListCriteriaImpl.Field, Object> {
        public static final String QUERY_BASE = "SELECT * FROM t";

        public ListQueryImpl() throws NullPointerException {
            fieldMap.put(ListCriteriaImpl.Field.FIELD_OBJECT, new BooleanOpField(FIELD_OBJECT_NAME, new NumericValue()));
            fieldMap.put(ListCriteriaImpl.Field.FIELD_TIMESTAMP, new BooleanOpField(FIELD_TIMESTAMP_NAME, new TimestampValue()));
            fieldMap.put(ListCriteriaImpl.Field.VERBATIM_FIELD_JSONB, new VerbatimBooleanOpField(VERBATIM_FIELD_JSONB_NAME, new JsonbValue()));
            fieldMap.put(ListCriteriaImpl.Field.VERBATIM_FIELD, new VerbatimField(VERBATIM_VALUE));
            fieldMap.put(ListCriteriaImpl.Field.JSON_SELECT_FIELD, new BooleanOpField("id", new SubSelectJsonValue("jobid", "item", "recordinfo", "id")));
        }

        @Override
        public List<Object> execute(ListCriteriaImpl criteria) {
            return null;
        }
    }

    private static class ListCriteriaImpl extends ListCriteria<ListCriteriaImpl.Field, ListCriteriaImpl> {
        public enum Field implements ListFilterField {
            FIELD_OBJECT,
            FIELD_TIMESTAMP,
            VERBATIM_FIELD,
            VERBATIM_FIELD_JSONB,
            JSON_SELECT_FIELD,
        }
    }
}
