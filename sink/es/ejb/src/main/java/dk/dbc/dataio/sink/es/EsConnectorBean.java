package dk.dbc.dataio.sink.es;

import dk.dbc.dataio.commons.types.ChunkResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ejb.EJB;
import javax.ejb.EJBException;
import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.sql.DataSource;
import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;

@Stateless
public class EsConnectorBean {
    private static final Logger LOGGER = LoggerFactory.getLogger(EsConnectorBean.class);

    @EJB
    EsSinkConfigurationBean configuration;

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

    public void closeConnection(Connection connection) {
        if (connection != null) {
            try {
                connection.close();
            } catch (SQLException e) {
                LOGGER.error("Unable to close connection to resource: {}", configuration.getEsResourceName());
            }
        }
    }

    @TransactionAttribute(TransactionAttributeType.MANDATORY)
    public int insertEsTaskPackage(ChunkResult chunkResult) throws SQLException, NamingException, IOException {
        try (final Connection connection = getConnection()) {
            //connection.setAutoCommit(false);
            final ESTaskPackageInserter esTaskPackageInserter = new ESTaskPackageInserter(
                    connection, configuration.getEsDatabaseName(), chunkResult);
            return esTaskPackageInserter.getTargetReference();
        }
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
            final String esResourceName = configuration.getEsResourceName();
            LOGGER.debug("Looking up ES resource {}", esResourceName);
            final Object lookup = initialContext.lookup(esResourceName);
            if (!(lookup instanceof DataSource)) {
                throw new NamingException("Unexpected type of resource returned from lookup");
            }
            return (DataSource) lookup;
        } finally {
            closeInitialContext(initialContext);
        }
    }
}
