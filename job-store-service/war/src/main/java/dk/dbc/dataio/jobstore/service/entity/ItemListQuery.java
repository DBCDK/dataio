package dk.dbc.dataio.jobstore.service.entity;

import dk.dbc.invariant.InvariantUtil;
import dk.dbc.dataio.jobstore.types.criteria.ItemListCriteria;
import dk.dbc.dataio.querylanguage.DataIOQLParser;
import dk.dbc.dataio.querylanguage.ParseException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceException;
import javax.persistence.Query;
import java.util.List;

/**
 * Item listing ListQuery implementation
 */
public class ItemListQuery extends ListQuery<ItemListCriteria, ItemListCriteria.Field, ItemEntity> {
    public List<ItemEntity> execute(String query) throws IllegalArgumentException {
        return getQuery(query).getResultList();
    }

    public ResultSet stream(String query) throws IllegalArgumentException {
        return new ResultSet(getQuery(query));
    }

    public long count(String query) throws IllegalArgumentException {
        final DataIOQLParser dataIOQLParser = new DataIOQLParser();
        try {
            final String sql = dataIOQLParser.parse("COUNT " + query);
            final Query q = entityManager.createNativeQuery(sql);
            return (long) q.getSingleResult();
        } catch (ParseException e) {
            throw new IllegalArgumentException("Unable to parse '" + query + "'", e);
        }
    }

    private Query getQuery(String query) {
        final DataIOQLParser dataIOQLParser = new DataIOQLParser();
        try {
            final String sql = dataIOQLParser.parse(query);
            return entityManager.createNativeQuery(sql, ItemEntity.class);
        } catch (ParseException e) {
            throw new IllegalArgumentException("Unable to parse '" + query + "'", e);
        }
    }

    /* !!! DEPRECATION WARNING !!!

        Future enhancements should NOT use the Criteria based API
        but work towards using the IO query language instead.

        Below code is therefore considered deprecated.
     */

    static final String QUERY_BASE = "SELECT * FROM item";
    static final String QUERY_COUNT_BASE = "SELECT count(*) FROM item";

    private static final Logger LOGGER = LoggerFactory.getLogger(ItemListQuery.class);

    private final EntityManager entityManager;

    /*
     * Constructor
     * @param entityManager EntityManager used for native query creation and execution
     * @throws NullPointerException if given null-valued entityManager argument
     */
    public ItemListQuery(EntityManager entityManager) throws NullPointerException {
        this.entityManager = InvariantUtil.checkNotNullOrThrow(entityManager, "entityManager");
        // Build list of available fields with associated field mappings
        fieldMap.put(ItemListCriteria.Field.JOB_ID, new BooleanOpField("jobId", new NumericValue()));
        fieldMap.put(ItemListCriteria.Field.CHUNK_ID, new BooleanOpField("chunkId", new NumericValue()));
        fieldMap.put(ItemListCriteria.Field.ITEM_ID, new BooleanOpField("id", new NumericValue()));
        fieldMap.put(ItemListCriteria.Field.TIME_OF_CREATION, new BooleanOpField("timeOfCreation", new ListQuery.TimestampValue()));
        fieldMap.put(ItemListCriteria.Field.STATE_FAILED, new VerbatimField("(state->'states'->'PARTITIONING'->>'failed' != '0' OR state->'states'->'PROCESSING'->>'failed' != '0' OR state->'states'->'DELIVERING'->>'failed' != '0')"));
        fieldMap.put(ItemListCriteria.Field.PARTITIONING_FAILED, new VerbatimField("state->'states'->'PARTITIONING'->>'failed' != '0'"));
        fieldMap.put(ItemListCriteria.Field.PROCESSING_FAILED, new VerbatimField("state->'states'->'PROCESSING'->>'failed' != '0'"));
        fieldMap.put(ItemListCriteria.Field.DELIVERY_FAILED, new VerbatimField("state->'states'->'DELIVERING'->>'failed' != '0'"));
        fieldMap.put(ItemListCriteria.Field.STATE_IGNORED, new VerbatimField("(state->'states'->'PARTITIONING'->>'ignored' != '0' OR state->'states'->'PROCESSING'->>'ignored' != '0' OR state->'states'->'DELIVERING'->>'ignored' != '0')"));
        fieldMap.put(ItemListCriteria.Field.RECORD_ID, new BooleanOpField("(recordinfo->>'id')", new ListQuery.StringValue()));
    }

    /**
     * Executes item listing query based on given criteria
     * @param criteria query criteria
     * @return list of entities for selected items
     * @throws NullPointerException if given null-valued criteria argument
     * @throws PersistenceException if unable to execute query
     */
    @Override
    public List<ItemEntity> execute(ItemListCriteria criteria) throws NullPointerException, PersistenceException {
        return getListQuery(criteria).getResultList();
    }

    /**
     * Streams result of item listing query based on given criteria
     * @param criteria query criteria
     * @return ResultSet stream
     * @throws NullPointerException if given null-valued criteria argument
     * @throws PersistenceException if unable to execute query
     */
    public ResultSet stream(ItemListCriteria criteria) {
        return new ResultSet(getListQuery(criteria));
    }

    /**
     * Creates and executes item count query with given criteria
     *
     * @param criteria query criteria
     * @return list of information snapshots for selected items
     * @throws NullPointerException if given null-valued criteria argument
     * @throws PersistenceException if unable to flushNotifications query
     */
    public long execute_count(ItemListCriteria criteria) throws NullPointerException, PersistenceException {
        final String query = buildCountQueryString(QUERY_COUNT_BASE, criteria);
        LOGGER.debug("query = {}", query);
        final Query listItemQuery = entityManager.createNativeQuery(query);
        setParameters(listItemQuery, criteria);
        final Long items = (Long)listItemQuery.getSingleResult();
        return items;
    }

    private Query getListQuery(ItemListCriteria criteria) {
        final String queryString = buildQueryString(QUERY_BASE, criteria);
        LOGGER.debug("query = {}", queryString);
        final Query query = entityManager.createNativeQuery(queryString, ItemEntity.class);
        setParameters(query, criteria);
        return query;
    }
}
