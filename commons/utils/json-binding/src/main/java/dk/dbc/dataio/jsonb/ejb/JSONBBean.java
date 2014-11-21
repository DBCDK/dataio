package dk.dbc.dataio.jsonb.ejb;

import dk.dbc.dataio.jsonb.JSONBContext;

import javax.annotation.PostConstruct;
import javax.ejb.Singleton;

/**
 * This Enterprise Java Bean (EJB) is used to access the safe JSON binding context
 */
@Singleton
public class JSONBBean {
    private JSONBContext jsonbContext;

    @PostConstruct
    public void initialiseContext() {
        jsonbContext = new JSONBContext();
    }

    public JSONBContext getContext() {
        return jsonbContext;
    }
}
