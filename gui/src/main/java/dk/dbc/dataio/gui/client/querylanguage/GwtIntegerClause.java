package dk.dbc.dataio.gui.client.querylanguage;

public class GwtIntegerClause extends GwtQueryClause {
    private String identifier;
    private BiOperator operator;
    private Integer value;
    private boolean arrayProperty = false;
    private boolean negated = false;
    private boolean isFlag = false;

    public String getIdentifier() {
        return identifier;
    }

    public GwtIntegerClause withIdentifier(String identifier) {
        this.identifier = identifier;
        return this;
    }

    public BiOperator getOperator() {
        return operator;
    }

    public GwtIntegerClause withOperator(BiOperator operator) {
        this.operator = operator;
        return this;
    }

    public Integer getValue() {
        return value;
    }

    public GwtIntegerClause withValue(Integer value) {
        this.value = value;
        return this;
    }

    public boolean isArrayProperty() {
        return arrayProperty;
    }

    public GwtIntegerClause withArrayProperty(boolean arrayProperty) {
        this.arrayProperty = arrayProperty;
        return this;
    }

    public boolean isNegated() {
        return negated;
    }

    public GwtIntegerClause withNegated(boolean negated) {
        this.negated = negated;
        return this;
    }

    public boolean isFlag() {
        return isFlag;
    }

    public GwtIntegerClause withFlag(boolean flag) {
        isFlag = flag;
        return this;
    }
}
