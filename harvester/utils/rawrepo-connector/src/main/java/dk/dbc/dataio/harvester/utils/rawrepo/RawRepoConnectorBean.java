package dk.dbc.dataio.harvester.utils.rawrepo;

import dk.dbc.dataio.commons.utils.invariant.InvariantUtil;
import dk.dbc.rawrepo.QueueJob;
import dk.dbc.rawrepo.RawRepoDAO;
import dk.dbc.rawrepo.Record;
import dk.dbc.rawrepo.RecordId;

import javax.annotation.Resource;
import javax.ejb.Stateless;
import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;

/**
 * This stateless Enterprise Java Bean (EJB) facilitates access to the RawRepo through datasource
 * resolved via JNDI lookup of {@value #JNDI_JDBC_RAW_REPO_NAME}
 */
@Stateless
public class RawRepoConnectorBean {
    public static final String JNDI_JDBC_RAW_REPO_NAME = "jdbc/dataio/rawrepo";

    @Resource(lookup = JNDI_JDBC_RAW_REPO_NAME)
    private DataSource dataSource;

    public Record fetchRecord(RecordId id) throws NullPointerException, SQLException {
        InvariantUtil.checkNotNullOrThrow(id, "id");
        try (final Connection connection = dataSource.getConnection()) {
            return RawRepoDAO.newInstance(connection).fetchRecord(id.getId(), id.getLibrary());
        } catch (ClassNotFoundException e) {
            throw new SQLException(e);
        }
    }

    public QueueJob dequeue(String consumerId)
            throws NullPointerException, IllegalArgumentException, SQLException {
        InvariantUtil.checkNotNullNotEmptyOrThrow(consumerId, "consumerId");
        try (final Connection connection = dataSource.getConnection()) {
            return RawRepoDAO.newInstance(connection).dequeue(consumerId);
        } catch (ClassNotFoundException e) {
            throw new SQLException(e);
        }
    }

    public void queueSuccess(QueueJob queueJob) throws NullPointerException, SQLException {
        InvariantUtil.checkNotNullOrThrow(queueJob, "queueJob");
        try (final Connection connection = dataSource.getConnection()) {
            RawRepoDAO.newInstance(connection).queueSuccess(queueJob);
        } catch (ClassNotFoundException e) {
            throw new SQLException(e);
        }
    }

    public void queueFail(QueueJob queueJob, String errorMessage)
            throws NullPointerException, IllegalArgumentException, SQLException {
        InvariantUtil.checkNotNullOrThrow(queueJob, "queueJob");
        InvariantUtil.checkNotNullNotEmptyOrThrow(errorMessage, "errorMessage");
        try (final Connection connection = dataSource.getConnection()) {
            RawRepoDAO.newInstance(connection).queueFail(queueJob, errorMessage);
        } catch (ClassNotFoundException e) {
            throw new SQLException(e);
        }
    }
}
