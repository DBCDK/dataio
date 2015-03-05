package dk.dbc.dataio.jobstore.service.entity;

import dk.dbc.dataio.commons.utils.invariant.InvariantUtil;
import dk.dbc.dataio.jobstore.service.util.ItemInfoSnapshotConverter;
import dk.dbc.dataio.jobstore.types.ItemInfoSnapshot;
import dk.dbc.dataio.jobstore.types.criteria.ItemListCriteria;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceException;
import javax.persistence.Query;
import java.util.ArrayList;
import java.util.List;

/**
 * Item listing ListQuery implementation
 */
public class ItemListQuery extends ListQuery<ItemListCriteria, ItemListCriteria.Field> {

    static final String QUERY_BASE = "SELECT * FROM item";

    private static final Logger LOGGER = LoggerFactory.getLogger(JobListQuery.class);

    private final EntityManager entityManager;

    /**
     * Constructor
     * @param entityManager EntityManager used for native query creation and execution
     * @throws NullPointerException if given null-valued entityManager argument
     */
    public ItemListQuery(EntityManager entityManager) throws NullPointerException {
        this.entityManager = InvariantUtil.checkNotNullOrThrow(entityManager, "entityManager");
        // Build list of available fields with associated field mappings
        fieldMap.put(ItemListCriteria.Field.JOB_ID, new ListQuery.FieldMapping("jobId", new ListQuery.ObjectValue()));
        fieldMap.put(ItemListCriteria.Field.CHUNK_ID, new ListQuery.FieldMapping("chunkId", new ListQuery.ObjectValue()));
        fieldMap.put(ItemListCriteria.Field.ITEM_ID, new ListQuery.FieldMapping("id", new ListQuery.ObjectValue()));
        fieldMap.put(ItemListCriteria.Field.TIME_OF_CREATION, new ListQuery.FieldMapping("timeOfCreation", new ListQuery.ObjectValue()));
    }

    /**
     * Creates and executes item listing query with given criteria
     * @param criteria query criteria
     * @return list of information snapshots for selected items
     * @throws NullPointerException if given null-valued criteria argument
     * @throws javax.persistence.PersistenceException if unable to execute query
     */
    @Override
    public List<ItemInfoSnapshot> execute(ItemListCriteria criteria) throws NullPointerException, PersistenceException {
        final String query = buildQueryString(QUERY_BASE, criteria);
        LOGGER.debug("query = {}", query);
        final Query listItemQuery = entityManager.createNativeQuery(query, ItemEntity.class);
        setParameters(listItemQuery, criteria);

        /* We can not utilise @SqlResultSetMapping to map directly to ItemInfoSnapshot
           since we have no way to convert our complex JSON types into their corresponding POJOs */

        final List<ItemEntity> items = listItemQuery.getResultList();
        final List<ItemInfoSnapshot> itemInfoSnapshots = new ArrayList<>(items.size());
        for (ItemEntity itemEntity : items) {
            itemInfoSnapshots.add(ItemInfoSnapshotConverter.toItemInfoSnapshot(itemEntity));
        }
        return itemInfoSnapshots;
    }
}
