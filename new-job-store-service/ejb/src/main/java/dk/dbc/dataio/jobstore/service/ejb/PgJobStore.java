package dk.dbc.dataio.jobstore.service.ejb;

import dk.dbc.dataio.commons.time.StopWatch;
import dk.dbc.dataio.commons.types.jndi.JndiConstants;
import dk.dbc.dataio.commons.utils.invariant.InvariantUtil;
import dk.dbc.dataio.jobstore.service.dbhelper.AddEntityStatement;
import dk.dbc.dataio.jobstore.types.JobStoreException;
import dk.dbc.dataio.jsonb.JSONBException;
import dk.dbc.dataio.jsonb.ejb.JSONBBean;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Resource;
import javax.ejb.EJB;
import javax.ejb.Singleton;
import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;

/**
 * This singleton Enterprise Java Bean (EJB) facilitates access to the job-store database layer through datasource
 * resolved via JNDI lookup of {@value dk.dbc.dataio.commons.types.jndi.JndiConstants#JDBC_RESOURCE_JOBSTORE}
 */
@Singleton
public class PgJobStore {
    private static final Logger LOGGER = LoggerFactory.getLogger(PgJobStore.class);

    @Resource(lookup = JndiConstants.JDBC_RESOURCE_JOBSTORE)
    DataSource dataSource;

    @EJB
    JSONBBean jsonbBean;

    /**
     * Adds entity to job-store cache if not already cached
     * @param entity entity object to cache
     * @return id of cached entity
     * @throws NullPointerException if given null-valued entity
     * @throws IllegalStateException if unable to create checksum digest
     * @throws JobStoreException on database error or on failure to marshall
     * entity object to JSON
     */
    public int addEntity(Object entity) throws NullPointerException, IllegalStateException, JobStoreException {
        final StopWatch stopWatch = new StopWatch();
        try {
            InvariantUtil.checkNotNullOrThrow(entity, "entity");
            try (final Connection connection = dataSource.getConnection();
                 final AddEntityStatement addEntityStatement = new AddEntityStatement(connection, jsonbBean.getContext())) {
                return addEntityStatement
                        .setEntity(entity)
                        .execute()
                        .getId();
            }
        } catch (JSONBException | SQLException e) {
            throw new JobStoreException("Exception caught during job-store operation", e);
        } finally {
            LOGGER.debug("Operation took {} milliseconds", stopWatch.getElapsedTime());
        }
    }
}
