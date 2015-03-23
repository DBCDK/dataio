package dk.dbc.dataio.jobstore.service.entity;

import dk.dbc.dataio.jobstore.types.criteria.ListCriteria;
import dk.dbc.dataio.jobstore.types.criteria.ListFilter;
import dk.dbc.dataio.jobstore.types.criteria.ListFilterField;
import dk.dbc.dataio.jobstore.types.criteria.ListFilterGroup;
import dk.dbc.dataio.jobstore.types.criteria.ListOrderBy;

import javax.persistence.Query;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * Abstract listing query class
 * @param <T> ListCriteria subtype
 * @param <U> ListFilterField subtype
 */
public abstract class ListQuery<T extends ListCriteria, U extends ListFilterField> {
    protected final Map<U, FieldMapping> fieldMap = new HashMap<>();

    /**
     * Creates and executes listing query with given criteria
     * @param criteria query criteria
     * @return list of selected objects
     */
    public abstract List execute(T criteria);

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

    /* Binds field values extracted from given criteria to positional parameter to prevent SQL injection attacks
     */
    protected void setParameters(Query query, T criteria) {
        final List<ListFilterGroup<U>> filtering = criteria.getFiltering();
        int parameterIndex = 1;
        for (ListFilterGroup<U> filterGroup : filtering) {
            for (ListFilterGroup.Member<U> member : filterGroup) {
                final ListFilter<U> filter = member.getFilter();
                FieldMapping fieldMapping = fieldMap.get(filter.getField());
                if(fieldMapping instanceof BinaryOpField) {
                    final ParameterValue value = ((BinaryOpField)fieldMapping).getValue();
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
        int firstParameterIndex = 1;
        while (iterator.hasNext()) {
            final ListFilterGroup<U> filterGroup = iterator.next();
            if (firstParameterIndex != 1) {
                // We have multiple ListFilterGroups in criteria, so we combine with a default AND operator
                queryString.append(" AND");
            }
            if (filtering.size() > 1) {
                // We have multiple ListFilterGroups in criteria, so we group each in their own set of parentheses
                queryString.append(" (");
            }
            addListFilterGroup(queryString, filterGroup, firstParameterIndex);
            if (filtering.size() > 1) {
                queryString.append(" )");
            }
            firstParameterIndex += filterGroup.size();
        }
    }

    private void addListFilterGroup(StringBuilder queryString, ListFilterGroup<U> filterGroup, int firstParameterIndex) {
        final Iterator<ListFilterGroup.Member<U>> iterator = filterGroup.iterator();
        int nextParameterIndex = firstParameterIndex;
        while (iterator.hasNext()) {
            final ListFilterGroup.Member<U> member = iterator.next();
            if (nextParameterIndex != firstParameterIndex) {
                queryString.append(" ").append(member.getLogicalOperator());
            }
            final ListFilter<U> filter = member.getFilter();
            FieldMapping fieldMapping = fieldMap.get(filter.getField());
            final String columnName = fieldMap.get(filter.getField()).getName();
            if(fieldMapping instanceof BinaryOpField) {


                // add column name, operator and value triplets to query

                queryString.append(" ").append(columnName).append(filterOpToString(filter.getOperator())).append("?").append(nextParameterIndex);
                nextParameterIndex++;
            } else {
                queryString.append(" ").append(columnName);
            }

        }
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
            queryString.append(" LIMIT");
            queryString.append(" ").append(limit);
        }
    }

    private void addOffsetClause(StringBuilder queryString, int offset) {
        if(offset > 0) {
            queryString.append(" OFFSET");
            queryString.append(" ").append(offset);
        }
    }

    private static String filterOpToString(ListFilter.Op op) {
        switch (op) {
            case EQUAL:                     return "=";
            case GREATER_THAN:              return ">";
            case GREATER_THAN_OR_EQUAL_TO:  return ">=";
            case LESS_THAN:                 return "<";
            case LESS_THAN_OR_EQUAL_TO:     return "<=";
            case NOT_EQUAL:                 return "!=";
            default: throw new IllegalArgumentException("Unknown filter operator " + op);
        }
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

    public static class BinaryOpField extends FieldMapping {
        private final ParameterValue value;
        public BinaryOpField(String name, ParameterValue value) {
            super(name);
            this.value = value;
        }

        public ParameterValue getValue() {
            return value;
        }
    }

    public interface ParameterValue {
        void set(Query query, int parameterIndex, Object value);
    }

    /**
     * ParameterValue type where the object value is taken as-is
     */
    public static class ObjectValue implements ParameterValue {
        @Override
        public void set(Query query, int parameterIndex, Object value) {
            query.setParameter(parameterIndex, value);
        }
    }

    /**
     * ParameterValue type where the object value is interpreted as a timestamp
     */
    public static class TimestampValue implements ParameterValue {
        @Override
        public void set(Query query, int parameterIndex, Object value) {
            if (value instanceof Long) {
                // When value is taken from criteria unmarshalled from
                // JSON the Date type information is lost
                query.setParameter(parameterIndex, new Date((long) value));
            } else {
                query.setParameter(parameterIndex, value);
            }
        }
    }
}
