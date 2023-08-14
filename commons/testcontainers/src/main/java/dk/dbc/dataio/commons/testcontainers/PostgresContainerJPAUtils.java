package dk.dbc.dataio.commons.testcontainers;

import dk.dbc.commons.testcontainers.postgres.DBCPostgreSQLContainer;

public interface PostgresContainerJPAUtils {
    DBCPostgreSQLContainer dbContainer = makeDBContainer();
    private static DBCPostgreSQLContainer makeDBContainer() {
        DBCPostgreSQLContainer container = new DBCPostgreSQLContainer().withReuse(false);
        container.start();
        container.exposeHostPort();
        return container;
    }
}
