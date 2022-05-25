package dk.dbc.dataio.jobstore.service.entity;

import dk.dbc.invariant.InvariantUtil;
import dk.dbc.dataio.jobstore.types.criteria.ChunkListCriteria;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceException;
import javax.persistence.Query;
import java.util.List;

/**
 * Chunk listing ListQuery implementation
 */
public class ChunkListQuery extends ListQuery<ChunkListCriteria, ChunkListCriteria.Field, ChunkEntity> {

    static final String QUERY_BASE = "SELECT * FROM chunk";

    private static final Logger LOGGER = LoggerFactory.getLogger(ChunkListQuery.class);

    private final EntityManager entityManager;

    /**
     * Constructor
     * @param entityManager EntityManager used for native query creation and execution
     * @throws NullPointerException if given null-valued entityManager argument
     */
    public ChunkListQuery(EntityManager entityManager) throws NullPointerException {
        this.entityManager = InvariantUtil.checkNotNullOrThrow(entityManager, "entityManager");
        // Build list of available fields with associated field mappings
        fieldMap.put(ChunkListCriteria.Field.TIME_OF_CREATION, new BooleanOpField("timeOfCreation", new TimestampValue()));
        fieldMap.put(ChunkListCriteria.Field.TIME_OF_COMPLETION, new BooleanOpField("timeOfCompletion", new TimestampValue()));
    }

    /**
     * Creates and executes chunk listing query with given criteria
     * @param criteria query criteria
     * @return list of entities for selected chunks
     * @throws NullPointerException if given null-valued criteria argument
     * @throws javax.persistence.PersistenceException if unable to flushNotifications query
     */
    @Override
    public List<ChunkEntity> execute(ChunkListCriteria criteria) throws NullPointerException, PersistenceException {
        final String query = buildQueryString(QUERY_BASE, criteria);
        LOGGER.debug("query = {}", query);
        final Query listChunkQuery = entityManager.createNativeQuery(query, ChunkEntity.class);
        setParameters(listChunkQuery, criteria);
        return listChunkQuery.getResultList();
    }
}
