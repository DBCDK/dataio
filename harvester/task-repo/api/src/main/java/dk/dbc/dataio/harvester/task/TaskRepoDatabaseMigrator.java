package dk.dbc.dataio.harvester.task;

import org.flywaydb.core.Flyway;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import javax.ejb.Singleton;
import javax.ejb.Startup;
import javax.ejb.TransactionManagement;
import javax.ejb.TransactionManagementType;
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
