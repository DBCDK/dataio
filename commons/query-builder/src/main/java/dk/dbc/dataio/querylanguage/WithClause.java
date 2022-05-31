package dk.dbc.dataio.querylanguage;

/**
 * Class representing a query clause consisting of an
 * identifier on the form RESOURCE:FIELD
 */
public class WithClause implements Clause {
    private String identifier;

    public String getIdentifier() {
        return identifier;
    }

    public WithClause withIdentifier(String identifier) {
        this.identifier = identifier;
        return this;
    }
}
