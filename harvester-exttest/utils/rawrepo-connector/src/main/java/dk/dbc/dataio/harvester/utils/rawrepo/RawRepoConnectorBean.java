package dk.dbc.dataio.harvester.utils.rawrepo;

import dk.dbc.dataio.commons.utils.invariant.InvariantUtil;
import dk.dbc.marcxmerge.MarcXMerger;
import dk.dbc.marcxmerge.MarcXMergerException;
import dk.dbc.rawrepo.QueueJob;
import dk.dbc.rawrepo.RawRepoDAO;
import dk.dbc.rawrepo.RawRepoException;
import dk.dbc.rawrepo.Record;
import dk.dbc.rawrepo.RecordId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Resource;
import javax.ejb.Stateless;
import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Map;

/**
 * This stateless Enterprise Java Bean (EJB) facilitates access to the RawRepo through datasource
 * resolved via JNDI lookup of {@value #JNDI_JDBC_RAW_REPO_NAME}
 */
@Stateless
public class RawRepoConnectorBean {
    private static final Logger LOGGER = LoggerFactory.getLogger(RawRepoConnectorBean.class);

    public static final String JNDI_JDBC_RAW_REPO_NAME = "jdbc/dataio/rawrepo-exttest";

    @Resource(lookup = JNDI_JDBC_RAW_REPO_NAME)
    private DataSource dataSource;

    public Record fetchRecord(RecordId id) throws NullPointerException, SQLException, RawRepoException {
        InvariantUtil.checkNotNullOrThrow(id, "id");
        try (final Connection connection = dataSource.getConnection()) {
            final StopWatch stopWatch = new StopWatch();
            final Record record = RawRepoDAO.newInstance(connection).fetchRecord(id.getBibliographicRecordId(), id.getAgencyId());
            LOGGER.debug("RawRepo operation took {} milliseconds", stopWatch.getElapsedTime());
            return record;
        }
    }

    public Map<String, Record> fetchRecordCollection(RecordId id)
            throws NullPointerException, SQLException, RawRepoException, MarcXMergerException {
        InvariantUtil.checkNotNullOrThrow(id, "id");
        try (final Connection connection = dataSource.getConnection()) {
            final StopWatch stopWatch = new StopWatch();
            // This new'ing is expensive but I don't trust reuse due to
            // internal Transformer and DocumentBuilder
            final MarcXMerger marcXMerger = new MarcXMerger();
            final Map<String, Record> recordMap = RawRepoDAO.newInstance(connection)
                    .fetchRecordCollection(id.getBibliographicRecordId(), id.getAgencyId(), marcXMerger);
            LOGGER.debug("RawRepo operation took {} milliseconds", stopWatch.getElapsedTime());
            return recordMap;
        }
    }

    public QueueJob dequeue(String consumerId)
            throws NullPointerException, SQLException, RawRepoException {
        InvariantUtil.checkNotNullNotEmptyOrThrow(consumerId, "consumerId");
        try (final Connection connection = dataSource.getConnection()) {
            final StopWatch stopWatch = new StopWatch();
            final QueueJob queueJob = RawRepoDAO.newInstance(connection).dequeue(consumerId);
            LOGGER.debug("RawRepo operation took {} milliseconds", stopWatch.getElapsedTime());
            return queueJob;
        }
    }

    public void queueSuccess(QueueJob queueJob) throws NullPointerException, SQLException, RawRepoException {
        InvariantUtil.checkNotNullOrThrow(queueJob, "queueJob");
        try (final Connection connection = dataSource.getConnection()) {
            final StopWatch stopWatch = new StopWatch();
            RawRepoDAO.newInstance(connection).queueSuccess(queueJob);
            LOGGER.debug("RawRepo operation took {} milliseconds", stopWatch.getElapsedTime());
        }
    }

    public void queueFail(QueueJob queueJob, String errorMessage)
            throws NullPointerException, SQLException, RawRepoException {
        InvariantUtil.checkNotNullOrThrow(queueJob, "queueJob");
        InvariantUtil.checkNotNullNotEmptyOrThrow(errorMessage, "errorMessage");
        try (final Connection connection = dataSource.getConnection()) {
            final StopWatch stopWatch = new StopWatch();
            RawRepoDAO.newInstance(connection).queueFail(queueJob, errorMessage);
            LOGGER.debug("RawRepo operation took {} milliseconds", stopWatch.getElapsedTime());
        }
    }
}
