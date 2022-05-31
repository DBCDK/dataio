package dk.dbc.dataio.gui.server.query;

import dk.dbc.dataio.gui.client.querylanguage.GwtIntegerClause;
import dk.dbc.dataio.gui.client.querylanguage.GwtQueryClause;
import dk.dbc.dataio.gui.client.querylanguage.GwtStringClause;
import dk.dbc.dataio.querylanguage.BiClause;
import dk.dbc.dataio.querylanguage.Clause;
import dk.dbc.dataio.querylanguage.JsonValue;
import dk.dbc.dataio.querylanguage.NotClause;
import dk.dbc.dataio.querylanguage.Ordering;
import dk.dbc.dataio.querylanguage.QueryBuilder;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Constructs IOQL queries from GWT query clauses
 */
public class GwtQueryBuilder {
    private final List<GwtQueryClause> gwtQueryClauses = new ArrayList<>();
    private final Map<String, JsonValue> jsonValues = new HashMap<>();
    private final Map<String, JsonValue> negatedJsonValues = new HashMap<>();
    private final List<Ordering> orderings = new ArrayList<>();

    public GwtQueryBuilder add(GwtQueryClause gwtQueryClause) {
        gwtQueryClauses.add(gwtQueryClause);
        return this;
    }

    public GwtQueryBuilder addAll(List<GwtQueryClause> gwtQueryClauses) {
        this.gwtQueryClauses.addAll(gwtQueryClauses);
        return this;
    }

    public GwtQueryBuilder sortBy(Ordering ordering) {
        orderings.add(ordering);
        return this;
    }

    public String build() {
        jsonValues.clear();

        final QueryBuilder queryBuilder = new QueryBuilder();
        for (GwtQueryClause gwtQueryClause : gwtQueryClauses) {
            fromGwtQueryClause(gwtQueryClause).ifPresent(queryBuilder::and);
        }
        for (Map.Entry<String, JsonValue> jsonValueEntry : jsonValues.entrySet()) {
            queryBuilder.and(new BiClause()
                    .withIdentifier(jsonValueEntry.getKey())
                    .withOperator(BiClause.Operator.JSON_LEFT_CONTAINS)
                    .withValue(jsonValueEntry.getValue()));
        }
        for (Map.Entry<String, JsonValue> jsonValueEntry : negatedJsonValues.entrySet()) {
            queryBuilder.and(new NotClause().withClause(new BiClause()
                    .withIdentifier(jsonValueEntry.getKey())
                    .withOperator(BiClause.Operator.JSON_LEFT_CONTAINS)
                    .withValue(jsonValueEntry.getValue())));
        }

        orderings.forEach(queryBuilder::orderBy);

        return queryBuilder.buildQuery();
    }

    private Optional<Clause> fromGwtQueryClause(GwtQueryClause gwtQueryClause) {
        if (gwtQueryClause instanceof GwtStringClause) {
            return toBiClause((GwtStringClause) gwtQueryClause);
        } else if (gwtQueryClause instanceof GwtIntegerClause) {
            return toBiClause((GwtIntegerClause) gwtQueryClause);
        }
        throw new IllegalArgumentException("Unknown GwtQueryClause: " + gwtQueryClause);
    }

    private Optional<Clause> toBiClause(GwtStringClause gwtStringClause) {
        if (gwtStringClause.getOperator() == GwtQueryClause.BiOperator.JSON_LEFT_CONTAINS) {
            if (gwtStringClause.isNegated()) {
                updateJsonValues(negatedJsonValues, gwtStringClause.getIdentifier(), gwtStringClause.getValue(),
                        gwtStringClause.isArrayProperty());
            } else {
                updateJsonValues(jsonValues, gwtStringClause.getIdentifier(), gwtStringClause.getValue(),
                        gwtStringClause.isArrayProperty());
            }
            return Optional.empty();
        }
        final BiClause biClause = new BiClause()
                .withIdentifier(gwtStringClause.getIdentifier())
                .withOperator(toBiClauseOperator(gwtStringClause.getOperator()))
                .withValue(gwtStringClause.getValue());
        if (gwtStringClause.isNegated()) {
            return Optional.of(new NotClause().withClause(biClause));
        }
        return Optional.of(biClause);
    }

    private Optional<Clause> toBiClause(GwtIntegerClause gwtIntegerClause) {
        Object value = gwtIntegerClause.getValue();
        if (gwtIntegerClause.isFlag()) {
            value = gwtIntegerClause.getValue() != 0;
        }

        if (gwtIntegerClause.getOperator() == GwtQueryClause.BiOperator.JSON_LEFT_CONTAINS) {
            if (gwtIntegerClause.isNegated()) {
                updateJsonValues(negatedJsonValues, gwtIntegerClause.getIdentifier(), value,
                        gwtIntegerClause.isArrayProperty());
            } else {
                updateJsonValues(jsonValues, gwtIntegerClause.getIdentifier(), value,
                        gwtIntegerClause.isArrayProperty());
            }
            return Optional.empty();
        }
        final BiClause biClause = new BiClause()
                .withIdentifier(gwtIntegerClause.getIdentifier())
                .withOperator(toBiClauseOperator(gwtIntegerClause.getOperator()))
                .withValue(value);
        if (gwtIntegerClause.isNegated()) {
            return Optional.of(new NotClause().withClause(biClause));
        }
        return Optional.of(biClause);
    }

    private static void updateJsonValues(Map<String, JsonValue> jsonValues,
                                         String identifier, Object value, boolean isArrayProperty) {
        final String[] identifierParts = splitIdentifier(identifier);
        if (identifierParts.length > 2) {
            throw new IllegalArgumentException("Identifier has too many levels: " + identifier);
        }
        JsonValue jsonValue;
        if (jsonValues.containsKey(identifierParts[0])) {
            jsonValue = jsonValues.get(identifierParts[0]);
        } else {
            jsonValue = new JsonValue();
            jsonValues.put(identifierParts[0], jsonValue);
        }
        if (isArrayProperty) {
            jsonValue.add(identifierParts[1], value);
        } else {
            jsonValue.put(identifierParts[1], value);
        }
    }

    private static String[] splitIdentifier(String identifier) {
        return identifier.split("\\.");
    }

    private static BiClause.Operator toBiClauseOperator(GwtQueryClause.BiOperator biOperator) {
        switch (biOperator) {
            case EQUALS:
                return BiClause.Operator.EQUALS;
            case GREATER_THAN:
                return BiClause.Operator.GREATER_THAN;
            case GREATER_THAN_OR_EQUAL_TO:
                return BiClause.Operator.GREATER_THAN_OR_EQUAL_TO;
            case JSON_LEFT_CONTAINS:
                return BiClause.Operator.JSON_LEFT_CONTAINS;
            case LESS_THAN:
                return BiClause.Operator.LESS_THAN;
            case LESS_THAN_OR_EQUAL_TO:
                return BiClause.Operator.LESS_THAN_OR_EQUAL_TO;
            case NOT_EQUALS:
                return BiClause.Operator.NOT_EQUALS;
            default:
                throw new IllegalArgumentException("Unknown operator: " + biOperator);
        }
    }
}
