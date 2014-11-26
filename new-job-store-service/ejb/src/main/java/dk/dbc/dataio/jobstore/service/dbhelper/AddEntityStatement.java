package dk.dbc.dataio.jobstore.service.dbhelper;

import org.postgresql.util.PGobject;

import java.sql.Connection;
import java.sql.SQLException;

/**
 * Wrapper of SQL statement for caching of entities.
 * <p>
 * Uses fluent API.
 * </p>
 */
public class AddEntityStatement extends QueryStatement {
    private static final String ADD_ENTITY_STATEMENT = "SELECT * FROM set_entitycache(?, ?)";  // (checksum, entity)

    public AddEntityStatement(Connection connection) {
        super(connection, ADD_ENTITY_STATEMENT);
    }

    /**
     * Sets binding value for entity checksum
     */
    public AddEntityStatement setChecksum(String checksum) {
        return (AddEntityStatement) bind("checksum", checksum, 1);
    }

    /**
     * Sets binding value for entity object
     */
    public AddEntityStatement setEntity(PGobject entity) {
        return (AddEntityStatement) bind("entity", entity, 2);
    }

    /**
     * Executes statement
     * @return result of execution as Result instance
     * @throws SQLException in case of database error
     */
    public Result execute() throws SQLException {
        return new Result(executeAndGetInt());
    }

    /**
     * Result of entity cache set operation providing
     * accessor for ID of cache row
     */
    public static class Result implements QueryResult {
        private final int id;

        public Result(int id) {
            this.id = id;
        }

        public int getId() {
            return id;
        }
    }
}
