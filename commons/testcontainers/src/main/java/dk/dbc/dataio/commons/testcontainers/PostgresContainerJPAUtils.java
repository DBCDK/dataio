package dk.dbc.dataio.commons.testcontainers;

import dk.dbc.commons.testcontainers.postgres.DBCPostgreSQLContainer;
import dk.dbc.dataio.commons.utils.test.jndi.InMemoryInitialContextFactory;

import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;

public interface PostgresContainerJPAUtils {
    DataIOPostgresSQLContainer dbContainer = makeDBContainer();
    private static DataIOPostgresSQLContainer makeDBContainer() {
        DataIOPostgresSQLContainer container = new DataIOPostgresSQLContainer();
        container.withReuse(false);
        container.start();
        container.exposeHostPort();
        return container;
    }

    public class DataIOPostgresSQLContainer extends DBCPostgreSQLContainer {
        public DataIOPostgresSQLContainer bindDatasource(String jndi) {
            System.setProperty(Context.INITIAL_CONTEXT_FACTORY, InMemoryInitialContextFactory.class.getName());
            try {
                new InitialContext().bind(jndi, dbContainer.datasource());
                return this;
            } catch (NamingException e) {
                throw new RuntimeException(e);
            }
        }
    }
}
