package dk.dbc.dataio.harvester.task;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.Resource;
import jakarta.ejb.Singleton;
import jakarta.ejb.Startup;
import jakarta.ejb.TransactionManagement;
import jakarta.ejb.TransactionManagementType;
import org.flywaydb.core.Flyway;

import javax.sql.DataSource;

@Startup
@Singleton
@TransactionManagement(TransactionManagementType.BEAN)
public class TaskRepoDatabaseMigrator {
    @Resource(lookup = "jdbc/dataio/harvester/tasks")
    DataSource dataSource;

    public TaskRepoDatabaseMigrator() {
    }

    public TaskRepoDatabaseMigrator(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    @PostConstruct
    public void migrate() {
        final Flyway flyway = Flyway.configure()
                .table("schema_version")
                .baselineOnMigrate(true)
                .dataSource(dataSource)
                .locations("classpath:dk/dbc/dataio/harvester/task/db/migration")
                .load();
        flyway.migrate();
    }
}
