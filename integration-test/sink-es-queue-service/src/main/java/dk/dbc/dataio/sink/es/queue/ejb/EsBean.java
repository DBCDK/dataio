package dk.dbc.dataio.sink.es.queue.ejb;

import dk.dbc.commons.es.ESUtil;
import dk.dbc.commons.jdbc.util.JDBCUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ejb.EJBException;
import javax.ejb.Stateless;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.sql.DataSource;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.FormParam;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

@Stateless
@Path("es")
public class EsBean {
    private static final Logger LOGGER = LoggerFactory.getLogger(EsBean.class);

    private static final String ES_JNDI_NAME = "jdbc/dataio/es";

    @POST
    @Consumes({ MediaType.APPLICATION_FORM_URLENCODED })
    public Response createDatabaseIfNotExisting(@FormParam("dbname") String dbname) throws SQLException, NamingException {
        LOGGER.info("Creating database: '{}'", dbname);

        try (final Connection connection = getConnection()) {
            ESUtil.createDatabaseIfNotExisting(connection, dbname);
        }

        return Response.ok().build();
    }

    @DELETE
    @Path("{dbname}")
    public Response deleteDatabase(@PathParam("dbname") String dbname) throws SQLException, NamingException {
        LOGGER.info("Deleting database: '{}'", dbname);

        try (final Connection connection = getConnection()) {
            ESUtil.deleteTaskpackages(connection, dbname);
            ESUtil.deleteDatabase(connection, dbname);
        }

        return Response.ok().build();
    }

    @GET
    @Path("{dbname}")
    @Produces({ MediaType.TEXT_PLAIN })
    public Response getNumberOfTargetReferences(@PathParam("dbname") String dbname) throws SQLException, NamingException {
        LOGGER.info("Getting number of target references from ES database {}", dbname);
        int numberOfReferences = 0;
        try (final Connection connection = getConnection()) {
            final String getNumberOfTargetReferencesStmt = "SELECT COUNT(targetreference) FROM taskspecificupdate WHERE databasename = ?";
            final PreparedStatement pstmt = JDBCUtil.query(connection, getNumberOfTargetReferencesStmt, dbname);
            final ResultSet rs = pstmt.getResultSet();
            while (rs.next()) {
                numberOfReferences = rs.getInt(1);
            }
            JDBCUtil.closeResultSet(rs);
            JDBCUtil.closeStatement(pstmt);
        }

        return Response.ok().entity(numberOfReferences).build();
    }

    public Connection getConnection() throws SQLException, NamingException {
        final DataSource dataSource = doDataSourceLookup();
        return dataSource.getConnection();
    }

    private InitialContext getInitialContext() throws EJBException {
        final InitialContext initialContext;
        try {
            initialContext = new InitialContext();
        } catch (NamingException e) {
            throw new EJBException(e);
        }
        return initialContext;
    }

    private void closeInitialContext(InitialContext initialContext) {
        if (initialContext != null) {
            try {
                initialContext.close();
            } catch (NamingException e) {
                LOGGER.warn("Unable to close initial context", e);
            }
        }
    }

    private DataSource doDataSourceLookup() throws NamingException {
        final InitialContext initialContext = getInitialContext();
        try {
            LOGGER.debug("Looking up ES resource {}", ES_JNDI_NAME);
            final Object lookup = initialContext.lookup(ES_JNDI_NAME);
            if (!(lookup instanceof DataSource)) {
                throw new NamingException("Unexpected type of resource returned from lookup");
            }
            return (DataSource) lookup;
        } finally {
            closeInitialContext(initialContext);
        }
    }

}
