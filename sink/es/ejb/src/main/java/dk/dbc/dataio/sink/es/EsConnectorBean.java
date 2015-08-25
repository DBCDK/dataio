package dk.dbc.dataio.sink.es;

import dk.dbc.dataio.commons.types.ExternalChunk;
import dk.dbc.dataio.sink.types.SinkException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ejb.EJB;
import javax.ejb.EJBException;
import javax.ejb.Stateless;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;

@Stateless
public class EsConnectorBean {

    private static final Logger LOGGER = LoggerFactory.getLogger(EsConnectorBean.class);

    @EJB
    EsSinkConfigurationBean configuration;

    public Connection getConnection() throws SQLException, NamingException {
        LOGGER.debug("Looking up datasource");
        final DataSource dataSource = doDataSourceLookup();
        LOGGER.debug("Looked up datasource");
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

    public int insertEsTaskPackage(EsWorkload esWorkload) throws SinkException {
        LOGGER.debug("Getting connection");
        try (final Connection connection = getConnection()) {
            LOGGER.debug("Inserting task package");
            return ESTaskPackageUtil.insertTaskPackage(
                    connection, configuration.getEsDatabaseName(), esWorkload);
        } catch (SQLException | NamingException e) {
            throw new SinkException("Failed to insert ES task package", e);
        }
    }

    public Map<Integer, ESTaskPackageUtil.TaskStatus> getCompletionStatusForESTaskpackages(List<Integer> targetReferences) throws SinkException {
        try (final Connection connection = getConnection()) {
            return ESTaskPackageUtil.findCompletionStatusForTaskpackages(connection, targetReferences);
        } catch (SQLException | NamingException e) {
            throw new SinkException("Failed to get completion status for task packages", e);
        }
    }

    public void deleteESTaskpackages(List<Integer> targetReferences) throws SinkException {
        try (final Connection connection = getConnection()) {
            ESTaskPackageUtil.deleteTaskpackages(connection, targetReferences);
        } catch (SQLException | NamingException e) {
            LOGGER.warn("Exception caught while deleting ES-taskpackages.", e);
            throw new SinkException("Failed to delete task packages", e);
        }
    }

    public ExternalChunk getChunkForTaskPackage(int targetReference, ExternalChunk placeholderChunk) throws SinkException {
        try (final Connection connection = getConnection()) {
            return ESTaskPackageUtil.getChunkForTaskPackage(connection, targetReference, placeholderChunk);
        } catch (SQLException | NamingException e) {
            LOGGER.warn("Exception caught while creating chunk for ES task package", e);
            throw new SinkException("Failed to create chunk for task package", e);
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
        LOGGER.debug("Getting initial context");
        final InitialContext initialContext = getInitialContext();
        try {
            final String esResourceName = configuration.getEsResourceName();
            LOGGER.debug("Looking up ES resource {}", esResourceName);
            final Object lookup = initialContext.lookup(esResourceName);
            LOGGER.debug("Looked up ES resource {}", esResourceName);
            if (!(lookup instanceof DataSource)) {
                throw new NamingException("Unexpected type of resource returned from lookup");
            }
            return (DataSource) lookup;
        } finally {
            LOGGER.debug("Closing initial context");
            closeInitialContext(initialContext);
            LOGGER.debug("Closed initital context");
        }
    }
}
