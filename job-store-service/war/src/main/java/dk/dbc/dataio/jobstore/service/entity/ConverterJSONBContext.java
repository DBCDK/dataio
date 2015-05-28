package dk.dbc.dataio.jobstore.service.entity;

import dk.dbc.dataio.jsonb.JSONBContext;

/**
 * Singleton providing JSONBContext instance to be used by JPA converters
 */
public class ConverterJSONBContext {
    private static final JSONBContext JSONB_CONTEXT = new JSONBContext();

    private ConverterJSONBContext() {}

    public static JSONBContext getInstance() {
        return JSONB_CONTEXT;
    }
}
