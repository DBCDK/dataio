package dk.dbc.dataio.sink.es.queue.ejb;

import dk.dbc.commons.jdbc.util.JDBCUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ejb.EJBException;
import javax.ejb.Stateless;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.sql.DataSource;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

@Stateless
@Path("es-inflight")
public class EsInflightBean {
    private static final Logger LOGGER = LoggerFactory.getLogger(EsInflightBean.class);

    private static final String ES_RESOURCE = "jdbc/dataio/es";
    private static final String ES_INFLIGHT_JNDI_NAME = "jdbc/dataio/sinks/esInFlight";

    @GET
    @Produces({ MediaType.TEXT_PLAIN })
    public Response getNumberOfRecordsInfligt() throws SQLException, NamingException {
        LOGGER.info("Getting number of inflight records for database {}", ES_RESOURCE);
        int numberOfRecordsInflight = 0;
        try (final Connection connection = getConnection()) {
            final String getNumberOfRecordsInflightStmt = "SELECT SUM(recordslots) FROM esinflight WHERE resourcename = ?";
            final PreparedStatement pstmt = JDBCUtil.query(connection, getNumberOfRecordsInflightStmt, ES_RESOURCE);
            final ResultSet rs = pstmt.getResultSet();
            while (rs.next()) {
                numberOfRecordsInflight = rs.getInt(1);
            }
            JDBCUtil.closeResultSet(rs);
            JDBCUtil.closeStatement(pstmt);
        }
        return Response.ok().entity(numberOfRecordsInflight).build();
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
            LOGGER.debug("Looking up ES resource {}", ES_INFLIGHT_JNDI_NAME);
            final Object lookup = initialContext.lookup(ES_INFLIGHT_JNDI_NAME);
            if (!(lookup instanceof DataSource)) {
                throw new NamingException("Unexpected type of resource returned from lookup");
            }
            return (DataSource) lookup;
        } finally {
            closeInitialContext(initialContext);
        }
    }

}
