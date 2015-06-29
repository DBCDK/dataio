package dk.dbc.dataio.jobstore.service.entity;

import dk.dbc.dataio.jobstore.types.criteria.ListCriteria;
import dk.dbc.dataio.jobstore.types.criteria.ListFilter;
import dk.dbc.dataio.jobstore.types.criteria.ListFilterField;
import dk.dbc.dataio.jobstore.types.criteria.ListFilterGroup;
import dk.dbc.dataio.jobstore.types.criteria.ListOrderBy;

import javax.persistence.Query;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Abstract listing query class
 * @param <T> ListCriteria subtype
 * @param <U> ListFilterField subtype
 */
public abstract class ListQuery<T extends ListCriteria, U extends ListFilterField> {
    protected final Map<U, FieldMapping> fieldMap = new HashMap<>();

    private static Set<ListFilter.Op> unaryOpSet = new HashSet<>();
    static {
        unaryOpSet.add(ListFilter.Op.IS_NOT_NULL);
        unaryOpSet.add(ListFilter.Op.IS_NULL);
    }

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
                if(fieldMapping instanceof BooleanOpField) {
                    final ParameterValue value = ((BooleanOpField)fieldMapping).getValue();
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
        int nextParameterIndex = firstParameterIndex;
        int memberIndex = 0;
        while (iterator.hasNext()) {
            final ListFilterGroup.Member<U> member = iterator.next();
            if (memberIndex != 0) {
                queryString.append(" ").append(member.getLogicalOperator());
            }
            final ListFilter<U> filter = member.getFilter();
            FieldMapping fieldMapping = fieldMap.get(filter.getField());
            final String columnName = fieldMap.get(filter.getField()).getName();

            if(fieldMapping instanceof BooleanOpField) {
                ListFilter.Op operator = filter.getOperator();
                if (unaryOpSet.contains(operator)) {
                    queryString.append(" ").append(columnName).append(filterOpToString(operator));
                } else {
                    // add column name, operator and value triplets to query
                    queryString.append(" ").append(columnName).append(filterOpToString(operator)).append("?").append(nextParameterIndex);
                    nextParameterIndex++;
                }
            } else if (fieldMapping instanceof VerbatimBooleanOpField) {
                queryString.append(" ").append(columnName).append(filterOpToString(filter.getOperator())).append(((VerbatimBooleanOpField) fieldMapping).getValue().toString(filter.getValue()));
            } else {
                queryString.append(" ").append(columnName);
            }
            memberIndex++;
        }
        return nextParameterIndex;
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
        if(offset > 0) {
            queryString.append(" OFFSET ").append(offset);
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
            case IS_NULL:                   return " IS NULL";
            case IS_NOT_NULL:               return " IS NOT NULL";
            case JSON_LEFT_CONTAINS:        return "@>";
            default: throw new IllegalArgumentException("Unknown filter operator " + op);
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

    public interface ParameterValue {
        void set(Query query, int parameterIndex, Object value);
    }

    public interface VerbatimValue {
        String toString(Object raw);
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
            if (value != null) {
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

    /**
     * VerbatimValue type where the object value is interpreted as a String to be
     * cast to a JSONB type when executing the query
     */
    public static class JsonbValue implements VerbatimValue {
        public String toString (Object raw) {
            return "'" + escapeSQL(raw.toString()) + "'::jsonb";
        }
    }
}
