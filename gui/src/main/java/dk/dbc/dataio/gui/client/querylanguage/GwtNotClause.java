/*
 * Copyright Dansk Bibliotekscenter a/s. Licensed under GNU GPLv3
 * See license text in LICENSE.txt
 */

package dk.dbc.dataio.gui.client.querylanguage;

public class GwtNotClause extends GwtQueryClause {
    private GwtQueryClause gwtQueryClause;

    public GwtQueryClause getGwtQueryClause() {
        return gwtQueryClause;
    }

    public GwtNotClause withGwtQueryClause(GwtQueryClause gwtQueryClause) {
        this.gwtQueryClause = gwtQueryClause;
        return this;
    }
}
