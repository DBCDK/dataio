package dk.dbc.dataio.gui.client.querylanguage;

public class GwtStringClause extends GwtQueryClause {
    private String identifier;
    private BiOperator operator;
    private String value;
    private boolean arrayProperty = false;
    private boolean negated = false;

    public String getIdentifier() {
        return identifier;
    }

    public GwtStringClause withIdentifier(String identifier) {
        this.identifier = identifier;
        return this;
    }

    public BiOperator getOperator() {
        return operator;
    }

    public GwtStringClause withOperator(BiOperator operator) {
        this.operator = operator;
        return this;
    }

    public String getValue() {
        return value;
    }

    public GwtStringClause withValue(String value) {
        this.value = value;
        return this;
    }

    public boolean isArrayProperty() {
        return arrayProperty;
    }

    public GwtStringClause withArrayProperty(boolean arrayProperty) {
        this.arrayProperty = arrayProperty;
        return this;
    }

    public boolean isNegated() {
        return negated;
    }

    public GwtStringClause withNegated(boolean negated) {
        this.negated = negated;
        return this;
    }
}
