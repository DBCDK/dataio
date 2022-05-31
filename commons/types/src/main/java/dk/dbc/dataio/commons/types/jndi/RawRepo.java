package dk.dbc.dataio.commons.types.jndi;

import java.util.HashMap;
import java.util.Map;

/**
 * Enum providing bidirectional mapping between raw-repo name and JNDI resource name
 */
public enum RawRepo {
    BASISMIG("jdbc/dataio/basis-rawrepo"),
    BOBLEBAD("jdbc/dataio/rawrepo-boblebad"),
    CISTERNE("jdbc/dataio/rawrepo-cisterne"),
    FBSTEST("jdbc/dataio/rawrepo-exttest");

    private static final Map<String, RawRepo> BY_STRING = new HashMap<>();

    static {
        for (RawRepo rawRepo : values()) {
            BY_STRING.put(rawRepo.name().toLowerCase(), rawRepo);
            BY_STRING.put(rawRepo.jndiResourceName, rawRepo);
        }
    }

    public static RawRepo fromString(String text) {
        if (text != null) {
            return BY_STRING.get(text.toLowerCase());
        }
        return null;
    }

    private final String jndiResourceName;

    RawRepo(String jndiResourceName) {
        this.jndiResourceName = jndiResourceName;
    }

    public String getJndiResourceName() {
        return jndiResourceName;
    }
}
