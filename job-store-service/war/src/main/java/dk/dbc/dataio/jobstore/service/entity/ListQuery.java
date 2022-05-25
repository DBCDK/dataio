package dk.dbc.dataio.jobstore.service.entity;

import dk.dbc.dataio.jobstore.types.criteria.ListCriteria;
import dk.dbc.dataio.jobstore.types.criteria.ListFilter;
import dk.dbc.dataio.jobstore.types.criteria.ListFilterField;
import dk.dbc.dataio.jobstore.types.criteria.ListFilterGroup;
import dk.dbc.dataio.jobstore.types.criteria.ListOrderBy;
import org.eclipse.persistence.config.HintValues;
import org.eclipse.persistence.config.QueryHints;
import org.eclipse.persistence.queries.CursoredStream;

import javax.persistence.Query;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;

/**
 * Abstract listing query class
 *
 * @param <T> ListCriteria subtype
 * @param <U> ListFilterField subtype
 * @param <V> value type to be returned by this query
 */
public abstract class ListQuery<T extends ListCriteria, U extends ListFilterField, V> {
    /* !!! DEPRECATION WARNING !!!

        Future enhancements should NOT use the Criteria based API
        but work towards using the IO query language instead.

        Below code is therefore considered deprecated.
     */

    protected final Map<U, FieldMapping> fieldMap = new HashMap<>();

    private static Set<ListFilter.Op> unaryOpSet = new HashSet<>();

    static {
        unaryOpSet.add(ListFilter.Op.IS_NOT_NULL);
        unaryOpSet.add(ListFilter.Op.IS_NULL);
    }

    /**
     * Creates and executes listing query with given criteria
     *
     * @param criteria query criteria
     * @return list of selected objects
     */
    public abstract List<V> execute(T criteria);

    /* Builds and returns query expression as string
     */
    protected String buildQueryString(String queryBase, T criteria) throws NullPointerException {
        final StringBuilder queryString = new StringBuilder(queryBase);
        addWhereClauses(queryString, criteria.getFiltering());
        addOrderByClauses(queryString, criteria.getOrdering());
        addLimitClause(queryString, criteria.getLimit());
        addOffsetClause(queryString, criteria.getOffset());
        return queryString.toString();
    }


    /* Builds and returns query expression as string
     */
    protected String buildCountQueryString(String queryBase, T criteria) throws NullPointerException {
        final StringBuilder queryString = new StringBuilder(queryBase);
        addWhereClauses(queryString, criteria.getFiltering());
        return queryString.toString();
    }

    /* Binds field values extracted from given criteria to positional parameter to prevent SQL injection attacks
     */
    protected void setParameters(Query query, T criteria) {
        final List<ListFilterGroup<U>> filtering = criteria.getFiltering();
        int parameterIndex = 1;
        for (ListFilterGroup<U> filterGroup : filtering) {
            for (ListFilterGroup.Member<U> member : filterGroup) {
                final ListFilter<U> filter = member.getFilter();
                FieldMapping fieldMapping = fieldMap.get(filter.getField());
                if (fieldMapping instanceof BooleanOpField && !unaryOpSet.contains(filter.getOperator())) {
                    final ParameterValue value = ((BooleanOpField) fieldMapping).getValue();
                    value.set(query, parameterIndex, filter.getValue());
                    parameterIndex++;
                }
            }
        }
    }

    /* Adds WHERE part to query if given non-empty list of ListFilterGroup
     */
    private void addWhereClauses(StringBuilder queryString, List<ListFilterGroup<U>> filtering) {
        final Iterator<ListFilterGroup<U>> iterator = filtering.iterator();
        if (iterator.hasNext()) {
            queryString.append(" WHERE");
        }
        boolean firstGroup = true;
        int nextParameterIndex = 1;
        while (iterator.hasNext()) {
            final ListFilterGroup<U> filterGroup = iterator.next();
            if (!firstGroup) {
                // We have multiple ListFilterGroups in criteria, so we combine with a default AND operator
                queryString.append(" AND");
            }
            if (filtering.size() > 1) {
                // We have multiple ListFilterGroups in criteria, so we group each in their own set of parentheses
                queryString.append(" (");
            }
            nextParameterIndex = addListFilterGroup(queryString, filterGroup, nextParameterIndex);
            if (filtering.size() > 1) {
                queryString.append(" )");
            }
            firstGroup = false;
        }
    }

    private int addListFilterGroup(StringBuilder queryString, ListFilterGroup<U> filterGroup, int firstParameterIndex) {
        final Iterator<ListFilterGroup.Member<U>> iterator = filterGroup.iterator();
        int parameterIndex = firstParameterIndex;
        int memberIndex = 0;
        if (filterGroup.isNot()) {
            queryString.append(" NOT (");
        }
        while (iterator.hasNext()) {
            final ListFilterGroup.Member<U> member = iterator.next();
            if (memberIndex != 0) {
                queryString.append(" ").append(member.getLogicalOperator());
            }
            final ListFilter<U> filter = member.getFilter();
            FieldMapping fieldMapping = fieldMap.get(filter.getField());
            final String columnName = fieldMap.get(filter.getField()).getName();

            ListFilter.Op operator = filter.getOperator();

            if (fieldMapping instanceof BooleanOpField) {  // fieldMapping is a BooleanOpField
                if (unaryOpSet.contains(operator)) {  // IS_NULL or IS_NOT_NULL
                    queryString.
                            append(filterOpToPrefixString(operator)).
                            append(columnName).
                            append(filterOpToString(operator));
                } else {
                    // add column name, operator and value triplets to query
                    queryString.
                            append(filterOpToPrefixString(operator)).
                            append(columnName).
                            append(filterOpToString(operator)).
                            append(((BooleanOpField) fieldMapping).getValue().getSqlString(parameterIndex)).
                            append(filterOpToPostfixString(operator));
                    parameterIndex++;
                }
            } else if (fieldMapping instanceof VerbatimBooleanOpField) {  // fieldMapping is a VerbatimBooleanOpField
                queryString.
                        append(filterOpToPrefixString(operator)).
                        append(columnName).
                        append(filterOpToString(operator)).
                        append(((VerbatimBooleanOpField) fieldMapping).getValue().toString(filter.getValue())).
                        append(filterOpToPostfixString(operator));
            } else {  // fieldMapping is a VerbatimField
                queryString.
                        append(filterOpToPrefixString(operator)).
                        append(columnName).
                        append(filterOpToPostfixString(operator));
            }
            memberIndex++;
        }
        if (filterGroup.isNot()) {
            queryString.append(" )");
        }
        return parameterIndex;
    }

    /* Adds WHERE part to query if given non-empty list of ListFilterGroup
     */
    private void addOrderByClauses(StringBuilder queryString, List<ListOrderBy<U>> ordering) {
        final Iterator<ListOrderBy<U>> iterator = ordering.iterator();
        if (iterator.hasNext()) {
            queryString.append(" ORDER BY");
        }
        while (iterator.hasNext()) {
            final ListOrderBy<U> clause = iterator.next();
            final String columnName = fieldMap.get(clause.getField()).getName();
            queryString.append(" ").append(columnName).append(" ").append(clause.getSort());
            if (iterator.hasNext()) {
                queryString.append(",");
            }
        }
    }

    private void addLimitClause(StringBuilder queryString, int limit) {
        if (limit > 0) {
            queryString.append(" LIMIT ").append(limit);
        }
    }

    private void addOffsetClause(StringBuilder queryString, int offset) {
        if (offset > 0) {
            queryString.append(" OFFSET ").append(offset);
        }
    }

    private static String filterOpToString(ListFilter.Op op) {
        switch (op) {
            case LESS_THAN:
                return "<";
            case GREATER_THAN:
                return ">";
            case LESS_THAN_OR_EQUAL_TO:
                return "<=";
            case GREATER_THAN_OR_EQUAL_TO:
                return ">=";
            case EQUAL:
                return "=";
            case NOT_EQUAL:
                return "!=";
            case IS_NULL:
                return " IS NULL";
            case IS_NOT_NULL:
                return " IS NOT NULL";
            case JSON_LEFT_CONTAINS:
                return "@>";
            case JSON_NOT_LEFT_CONTAINS:
                return "@>";
            case IN:
                return " IN (";
            default:
                throw new IllegalArgumentException("Unknown filter operator " + op);
        }
    }

    private static String filterOpToPrefixString(ListFilter.Op op) {
        switch (op) {
            case LESS_THAN:
                return " ";
            case GREATER_THAN:
                return " ";
            case LESS_THAN_OR_EQUAL_TO:
                return " ";
            case GREATER_THAN_OR_EQUAL_TO:
                return " ";
            case EQUAL:
                return " ";
            case NOT_EQUAL:
                return " ";
            case NOOP:
                return " ";
            case IS_NULL:
                return " ";
            case IS_NOT_NULL:
                return " ";
            case JSON_LEFT_CONTAINS:
                return " ";
            case JSON_NOT_LEFT_CONTAINS:
                return " NOT ";
            case IN:
                return " ";
            default:
                throw new IllegalArgumentException("Unknown prefix filter operator " + op);
        }
    }

    private static String filterOpToPostfixString(ListFilter.Op op) {
        switch (op) {
            case LESS_THAN:
                return "";
            case GREATER_THAN:
                return "";
            case LESS_THAN_OR_EQUAL_TO:
                return "";
            case GREATER_THAN_OR_EQUAL_TO:
                return "";
            case EQUAL:
                return "";
            case NOT_EQUAL:
                return "";
            case NOOP:
                return "";
            case IS_NULL:
                return "";
            case IS_NOT_NULL:
                return "";
            case JSON_LEFT_CONTAINS:
                return "";
            case JSON_NOT_LEFT_CONTAINS:
                return "";
            case IN:
                return ")";
            default:
                throw new IllegalArgumentException("Unknown postfix filter operator " + op);
        }
    }

    private static String escapeSQL(String str) {
        if (str != null) {
            return str.replace("'", "''");
        }
        return null;
    }

    /**
     * Class used when mapping a ListFilterField to its corresponding
     * column name and value
     */
    public abstract static class FieldMapping {
        private final String name;

        public FieldMapping(String name) {
            this.name = name;
        }

        public String getName() {
            return name;
        }
    }

    public static class VerbatimField extends FieldMapping {
        public VerbatimField(String name) {
            super(name);
        }
    }

    public static class BooleanOpField extends FieldMapping {
        private final ParameterValue value;

        public BooleanOpField(String name, ParameterValue value) {
            super(name);
            this.value = value;
        }

        public ParameterValue getValue() {
            return value;
        }
    }

    public static class VerbatimBooleanOpField extends FieldMapping {
        private final VerbatimValue value;

        public VerbatimBooleanOpField(String name, VerbatimValue value) {
            super(name);
            this.value = value;
        }

        public VerbatimValue getValue() {
            return value;
        }
    }


    /**
     * Parameter values
     * Values, that are represented as '?1', '?2' etc in the query
     * A call to query.set binds the actual value to the query
     */
    public static class ParameterValue<T> {
        private Function<String, T> f;

        ParameterValue(Function<String, T> f) {
            this.f = f;
        }

        /**
         * Binds the actual value to the query
         *
         * @param query          The query
         * @param parameterIndex The parameter index
         * @param value          The actual value, to be bound to the query
         */
        public void set(Query query, int parameterIndex, String value) {
            query.setParameter(parameterIndex, f.apply(value));
        }

        /**
         * Returns the SQL query representation of the parameter in the SQL query
         * Which is simply the text: "?n" - where n is the index number of the parameter
         *
         * @param parameterIndex The parameter index
         * @return The SQL query representation of the parameter
         */
        public String getSqlString(int parameterIndex) {
            return "?" + parameterIndex;
        }
    }

    /**
     * ParameterValue type where the object value is taken as-is
     */
    public static class StringValue extends ParameterValue<String> {
        StringValue() {
            super(s -> s);
        }
    }

    /**
     * ParameterValue type where the object value is interpreted as a long value
     */
    public static class NumericValue extends ParameterValue<Long> {
        NumericValue() {
            super(Long::valueOf);
        }
    }

    /**
     * ParameterValue type where the object value is interpreted as a timestamp
     */
    public static class TimestampValue extends ParameterValue<Date> {
        TimestampValue() {
            super(s -> new Date(Long.valueOf(s)));
        }
    }

    /**
     * ParameterValue type where the object value is interpreted as a Select query, making a query
     * in the underlying JSON value
     */
    public static class SubSelectJsonValue extends ParameterValue<String> {
        private static String subselectRepresentation;

        public SubSelectJsonValue(String column, String table, String jsonColumn, String jsonValue) {
            super(s -> s);
            subselectRepresentation = new StringBuilder().
                    append("SELECT ").
                    append(column).
                    append(" FROM ").
                    append(table).
                    append(" WHERE ").
                    append(jsonColumn).
                    append("->>'").
                    append(jsonValue).
                    append("' = ").
                    toString();
        }

        @Override
        public String getSqlString(int parameterIndex) {
            return subselectRepresentation + super.getSqlString(parameterIndex);
        }
    }


    /**
     * VerbatimValue
     * Values, that are coded directly into the SQL query
     */
    public interface VerbatimValue {
        String toString(Object raw);
    }

    /**
     * VerbatimValue type where the object value is interpreted as a String to be
     * cast to a JSONB type when executing the query
     */
    public static class JsonbValue implements VerbatimValue {
        @Override
        public String toString(Object raw) {
            return "'" + escapeSQL(raw.toString()) + "'::jsonb";
        }
    }

    /**
     * This class represents a one-time iteration of a job-store list query result set of non-managed entities.
     */
    public class ResultSet implements Iterable<V>, AutoCloseable {
        private final int BUFFER_SIZE = 50;

        final CursoredStream cursor;

        ResultSet(Query query) {
            // Yes we are breaking general JPA compatibility using below QueryHints and CursoredStream,
            // but we need to be able to handle very large result sets.

            // Configures the query to return a CursoredStream, which is a stream of the JDBC ResultSet.
            query.setHint(QueryHints.CURSOR, HintValues.TRUE);
            // Configures the CursoredStream with the number of objects fetched from the stream on a next() call.
            query.setHint(QueryHints.CURSOR_PAGE_SIZE, BUFFER_SIZE);
            // Configures the JDBC fetch-size for the result set.
            query.setHint(QueryHints.JDBC_FETCH_SIZE, BUFFER_SIZE);
            // Configures the query to not use the shared cache and the transactional cache/persistence context.
            // Resulting objects will be read and built directly from the database, and not registered in the
            // persistence context. Changes made to the objects will not be updated unless merged and object identity
            // will not be maintained.
            // This is necessary to avoid OutOfMemoryError from very large persistence contexts.
            query.setHint(QueryHints.MAINTAIN_CACHE, HintValues.FALSE);
            cursor = (CursoredStream) query.getSingleResult();
        }

        @Override
        public Iterator<V> iterator() {
            return new Iterator<V>() {
                @Override
                public boolean hasNext() {
                    return cursor.hasNext();
                }

                @Override
                @SuppressWarnings("unchecked")
                public V next() {
                    // To avoid OutOfMemoryError we occasionally need to clear the internal data structure of the
                    // CursoredStream.
                    if (cursor.getPosition() % BUFFER_SIZE == 0) {
                        cursor.clear();
                    }
                    return (V) cursor.next();
                }
            };
        }

        @Override
        public void close() {
            if (cursor != null) {
                cursor.close();
            }
        }
    }
}
