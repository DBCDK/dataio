/*
 * Copyright Dansk Bibliotekscenter a/s. Licensed under GNU GPLv3
 * See license text in LICENSE.txt
 */

package dk.dbc.dataio.querylanguage;

/**
 * Class representing a negated query clause
 */
public class NotClause implements Clause {
    private Clause clause;

    public Clause getClause() {
        return clause;
    }

    public NotClause withClause(Clause clause) {
        this.clause = clause;
        return this;
    }
}
