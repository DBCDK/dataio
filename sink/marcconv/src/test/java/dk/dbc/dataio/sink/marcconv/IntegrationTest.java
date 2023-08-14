package dk.dbc.dataio.sink.marcconv;

import dk.dbc.commons.persistence.JpaIntegrationTest;
import dk.dbc.commons.persistence.JpaTestEnvironment;
import dk.dbc.commons.testcontainers.postgres.DBCPostgreSQLContainer;
import dk.dbc.dataio.commons.testcontainers.PostgresContainerJPAUtils;
import dk.dbc.dataio.jse.artemis.common.db.JPAHelper;
import org.junit.Before;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Map;

public abstract class IntegrationTest extends JpaIntegrationTest implements PostgresContainerJPAUtils {
    private static final Logger LOGGER = LoggerFactory.getLogger(IntegrationTest.class);

    @Override
    public JpaTestEnvironment setup() {
        DataSource dataSource = getDataSource();
        JPAHelper.migrate(dataSource);

        jpaTestEnvironment = new JpaTestEnvironment(dataSource, "marcconvIT_PU", getEntityManagerFactoryProperties());
        return jpaTestEnvironment;
    }

    @Before
    public void resetDatabase() throws SQLException {
        try (Connection conn = jpaTestEnvironment.getDatasource().getConnection(); Statement statement = conn.createStatement()) {
            statement.executeUpdate("DELETE FROM block");
        }
    }

    private DataSource getDataSource() {
        return dbContainer.datasource();
    }

    private Map<String, String> getEntityManagerFactoryProperties() {
        return dbContainer.entityManagerProperties();
    }
}
