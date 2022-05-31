package dk.dbc.dataio.querylanguage;

import dk.dbc.commons.jsonb.JSONBContext;
import dk.dbc.commons.jsonb.JSONBException;

import java.util.ArrayList;
import java.util.List;

/**
 * Class used to build query strings which can be parsed by the DataIOQLParser
 */
public class QueryBuilder {
    public enum BooleanOperator {AND, OR}

    private final List<LogicalPair> logicalPairs;
    private final List<Ordering> orderings;
    private final JSONBContext jsonbContext;

    private Integer limit;
    private Integer offset;

    public QueryBuilder() {
        logicalPairs = new ArrayList<>();
        orderings = new ArrayList<>();
        jsonbContext = new JSONBContext();
    }

    public QueryBuilder(Clause clause) {
        this();
        logicalPairs.add(new LogicalPair(BooleanOperator.AND, clause));
    }

    /**
     * Adds given {@link Clause} to those already added using boolean AND
     *
     * @param clause {@link Clause} to add
     * @return this {@link QueryBuilder}
     */
    public QueryBuilder and(Clause clause) {
        logicalPairs.add(new LogicalPair(BooleanOperator.AND, clause));
        return this;
    }

    /**
     * Adds given {@link Clause} to those already added using boolean OR
     *
     * @param clause {@link Clause} to add
     * @return this {@link QueryBuilder}
     */
    public QueryBuilder or(Clause clause) {
        logicalPairs.add(new LogicalPair(BooleanOperator.OR, clause));
        return this;
    }

    /**
     * Adds ORDER BY condition
     *
     * @param ordering {@link Ordering} to add
     * @return this {@link QueryBuilder}
     */
    public QueryBuilder orderBy(Ordering ordering) {
        orderings.add(ordering);
        return this;
    }

    /**
     * Limits number of results returned
     *
     * @param limit upper limit
     * @return this {@link QueryBuilder}
     */
    public QueryBuilder limit(Integer limit) {
        this.limit = limit;
        return this;
    }

    /**
     * Sets starting offset for results returned
     *
     * @param offset offset
     * @return this {@link QueryBuilder}
     */
    public QueryBuilder offset(Integer offset) {
        this.offset = offset;
        return this;
    }

    /**
     * Builds query expression
     *
     * @return query expression as string
     */
    public String buildQuery() {
        String query = buildExpression();
        query += buildOrderings();
        if (limit != null) {
            query += " LIMIT " + limit;
        }
        if (offset != null) {
            query += " OFFSET " + offset;
        }
        return query;
    }

    /**
     * Builds COUNT query expression
     *
     * @return query expression as string
     */
    public String buildCountQuery() {
        return "COUNT " + buildQuery();
    }

    private String buildExpression() {
        final StringBuilder buffer = new StringBuilder();
        boolean firstClause = true;
        for (LogicalPair logicalPair : logicalPairs) {
            if (!firstClause) {
                buffer.append(" ").append(logicalPair.booleanOperator).append(" ");
            }
            buffer.append(buildClause(logicalPair.clause));
            firstClause = false;
        }
        return buffer.toString();
    }

    private String buildClause(Clause clause) {
        if (clause instanceof BiClause) {
            return buildBiClause((BiClause) clause);
        } else if (clause instanceof WithClause) {
            return buildWithClause((WithClause) clause);
        } else if (clause instanceof NotClause) {
            return buildNotClause((NotClause) clause);
        }
        return "";
    }

    private String buildBiClause(BiClause biClause) {
        return biClause.getIdentifier() + " " + biClause.getOperator().toString() + " " +
                buildBiClauseValue(biClause.getValue());
    }

    private String buildBiClauseValue(Object value) {
        if (value instanceof String) {
            return "'" + escapeString(value.toString()) + "'";
        } else if (value instanceof Number) {
            return String.valueOf(value);
        } else if (value instanceof Boolean) {
            return String.valueOf(value);
        }
        try {
            return "'" + escapeString(jsonbContext.marshall(value)) + "'";
        } catch (JSONBException e) {
            throw new IllegalArgumentException(e);
        }
    }

    private String buildWithClause(WithClause withClause) {
        return "WITH " + withClause.getIdentifier();
    }

    private String buildNotClause(NotClause notClause) {
        return "NOT " + buildClause(notClause.getClause());
    }

    private String buildOrderings() {
        final StringBuilder buffer = new StringBuilder();
        boolean firstOrdering = true;
        for (Ordering ordering : orderings) {
            if (firstOrdering) {
                buffer.append(" ORDER BY ");
            } else {
                buffer.append(" ");
            }
            if (ordering.getSortCase() != null) {
                buffer.append(ordering.getSortCase()).append(" ");
            }
            buffer.append(ordering.getIdentifier()).append(" ").append(ordering.getOrder());
            firstOrdering = false;
        }
        return buffer.toString();
    }

    private String escapeString(String str) {
        return str.replaceAll("'", "\\\\'");
    }

    private static class LogicalPair {
        final BooleanOperator booleanOperator;
        final Clause clause;

        public LogicalPair(BooleanOperator booleanOperator, Clause clause) {
            this.booleanOperator = booleanOperator;
            this.clause = clause;
        }
    }
}
