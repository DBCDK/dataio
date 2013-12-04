package dk.dbc.dataio.sink.es;

import dk.dbc.dataio.sink.SinkException;
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

    public int insertEsTaskPackage(EsWorkload esWorkload) throws SinkException {
        try (final Connection connection = getConnection()) {
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
