package dk.dbc.dataio.sink.es;

import dk.dbc.dataio.commons.types.ChunkItem;
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

@Stateless
public class EsConnectorBean {

    private static final Logger LOGGER = LoggerFactory.getLogger(EsConnectorBean.class);

    @EJB
    EsSinkConfigurationBean configuration;

    public Connection getConnection() throws SQLException, NamingException {
        LOGGER.info("TEST: getConnection");
        final DataSource dataSource = doDataSourceLookup();
        LOGGER.info("TEST: got datasource");
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
        LOGGER.info("TEST: trying");
        try (final Connection connection = getConnection()) {
            LOGGER.info("TEST: inside try");
            return ESTaskPackageUtil.insertTaskPackage(
                    connection, configuration.getEsDatabaseName(), esWorkload);
        } catch (SQLException | NamingException e) {
            throw new SinkException("Failed to insert ES task package", e);
        }
    }

    public List<ESTaskPackageUtil.TaskStatus> getCompletionStatusForESTaskpackages(List<Integer> targetReferences) throws SinkException {
        try (final Connection connection = getConnection()) {
            return ESTaskPackageUtil.findCompletionStatusForTaskpackages(connection, targetReferences);
        } catch (SQLException | NamingException e) {
            throw new SinkException("Failed to get targetreferences for task packages", e);
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

    public List<ChunkItem> getSinkResultItemsForTaskPackage(int targetReference) throws SinkException {
        try (final Connection connection = getConnection()) {
            return ESTaskPackageUtil.getSinkResultItemsForTaskPackage(connection, targetReference);
        } catch (SQLException | NamingException e) {
            LOGGER.warn("Exception caught while retrieving ChunkItems from ES-taskpackage.", e);
            throw new SinkException("Failed to retrieve ChunkItems from taskpackage", e);
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
        LOGGER.info("TEST: doDataSourceLookup Begin");
        final InitialContext initialContext = getInitialContext();
        try {
            final String esResourceName = configuration.getEsResourceName();
            LOGGER.info("TEST: Looking up ES resource {}", esResourceName);
            LOGGER.debug("Looking up ES resource {}", esResourceName);
            final Object lookup = initialContext.lookup(esResourceName);
            LOGGER.info("TEST: Done! Looking up ES resource {}", esResourceName);
            if (!(lookup instanceof DataSource)) {
                throw new NamingException("Unexpected type of resource returned from lookup");
            }
            return (DataSource) lookup;
        } finally {
            LOGGER.info("TEST: doDataSourceLookup END 1");
            closeInitialContext(initialContext);
            LOGGER.info("TEST: doDataSourceLookup END 2");
        }
    }
}
