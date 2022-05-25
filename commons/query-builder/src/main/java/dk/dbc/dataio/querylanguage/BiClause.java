package dk.dbc.dataio.querylanguage;

/**
 * Class representing a query clause with a left-hand side identifier on the form RESOURCE:FIELD
 * followed by an {@link Operator} operator, followed by a value.
 */
public class BiClause implements Clause {
    public enum Operator {
        EQUALS("="),
        GREATER_THAN(">"),
        GREATER_THAN_OR_EQUAL_TO(">="),
        JSON_LEFT_CONTAINS("@>"),
        LESS_THAN("<"),
        LESS_THAN_OR_EQUAL_TO("<="),
        NOT_EQUALS("!=");

        private final String symbol;

        Operator(String symbol) {
            this.symbol = symbol;
        }

        @Override
        public String toString() {
            return symbol;
        }
    }

    private String identifier;
    private Operator operator;
    private Object value;

    public String getIdentifier() {
        return identifier;
    }

    public BiClause withIdentifier(String identifier) {
        this.identifier = identifier;
        return this;
    }

    public Operator getOperator() {
        return operator;
    }

    public BiClause withOperator(Operator operator) {
        this.operator = operator;
        return this;
    }

    public Object getValue() {
        return value;
    }

    public BiClause withValue(Object value) {
        this.value = value;
        return this;
    }
}
