package dk.dbc.dataio.gui.client.querylanguage;

import java.io.Serializable;

public class GwtQueryClause implements Serializable {
    public enum BiOperator {
        EQUALS,
        GREATER_THAN,
        GREATER_THAN_OR_EQUAL_TO,
        JSON_LEFT_CONTAINS,
        LESS_THAN,
        LESS_THAN_OR_EQUAL_TO,
        NOT_EQUALS
    }
}
