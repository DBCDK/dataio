package dk.dbc.dataio.jobstore.service.dbhelper;

import dk.dbc.dataio.jobstore.service.digest.Md5;
import dk.dbc.dataio.jsonb.JSONBContext;
import dk.dbc.dataio.jsonb.JSONBException;

import java.nio.charset.StandardCharsets;
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

    public AddEntityStatement(Connection connection, JSONBContext jsonbContext) {
        super(connection, jsonbContext, ADD_ENTITY_STATEMENT);
    }

    /**
     * Sets binding value for entity checksum
     */
    private AddEntityStatement setChecksum(String checksum) {
        return (AddEntityStatement) bind("checksum", checksum, 1);
    }

    /**
     * Sets binding and checksum values for entity object
     * @throws IllegalStateException if unable to create checksum digest
     * @throws JSONBException on failure to marshall entity
     */
    public AddEntityStatement setEntity(Object entity)
            throws IllegalStateException, JSONBException, SQLException {
        final String jsonEntity = jsonbContext.marshall(entity);
        setChecksum(Md5.asHex(jsonEntity.getBytes(StandardCharsets.UTF_8)));
        return (AddEntityStatement) bind("entity", asJsonPgObject(jsonEntity), 2);
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
