package dk.dbc.dataio.commons.partioner;

import dk.dbc.dataio.commons.partioner.entity.ReorderedItemEntity;
import dk.dbc.dataio.jobstore.types.MarcRecordInfo;
import jakarta.persistence.EntityManager;

import java.util.Optional;

/**
 * The responsibility of this class is to ensure the correct ordering of records taking part
 * in multi-level record structures.
 */
public abstract class JobItemReorderer {
    public enum SortOrder {
        HEAD(1),
        SECTION(2),
        VOLUME(3),
        VOLUME_DELETE(4),
        SECTION_DELETE(5),
        HEAD_DELETE(6);

        private final int sortOrder;

        SortOrder(int sortOrder) {
            this.sortOrder = sortOrder;
        }

        public int getIntValue() {
            return sortOrder;
        }
    }

    final int jobId;
    final EntityManager entityManager;
    int numberOfItems;

    public JobItemReorderer(int jobId, EntityManager entityManager) {
        this.jobId = jobId;
        this.entityManager = entityManager;
        numberOfItems = getNumberOfItemsInDatabase();
    }

    /**
     * @return true if this Reorderer instance still contains items to be re-ordered, otherwise false
     */
    public boolean hasNext() {
        return numberOfItems != 0;
    }

    /**
     * Returns the next DataPartitionerResult to be output
     * <p>
     * If given result is empty (indicating that the caller has run out of results to be examined)
     * then
     * if this Reorderer instance still contains results to be re-ordered
     * then
     * the next result is retrieved from internal list and returned
     * else
     * an empty Optional indicating no more records is returned.
     * <p>
     * If given result is not empty
     * then
     * if the result is part of multi-level record structure
     * then
     * the result is stored in internal list and an empty placeholder result is returned
     * else
     * the result is returned as-is (passthrough)
     * 6*
     *
     * @param partitionerResult current DataPartitionerResult to be examined
     * @return the next DataPartitionerResult
     */
    public Optional<DataPartitionerResult> next(DataPartitionerResult partitionerResult) {
        Optional<DataPartitionerResult> next;
        if (partitionerResult.isEmpty()) {
            if (hasNext()) {
                next = Optional.of(getReorderedItem());
            } else {
                next = Optional.empty();
            }
        } else {
            if (mustBeReordered(partitionerResult)) {
                next = Optional.of(setReorderedItem(partitionerResult));
            } else {
                next = Optional.of(partitionerResult);
            }
        }
        return next;
    }

    public int getJobId() {
        return jobId;
    }

    /**
     * @return number of items remaining to be reordered
     */
    public int getNumberOfItems() {
        return numberOfItems;
    }

    public Boolean addCollectionWrapper() {
        return Boolean.FALSE;
    }

    private boolean mustBeReordered(DataPartitionerResult partitionerResult) {
        final MarcRecordInfo recordInfo = (MarcRecordInfo) partitionerResult.getRecordInfo();
        return recordInfo.isHead() || recordInfo.isSection() || recordInfo.isVolume();
    }

    /* Persists given DataPartitionerResult in internal list with the correct sort-order
       to be used for later retrieval. Returns empty DataPartitionerResult placeholder. */
    private DataPartitionerResult setReorderedItem(DataPartitionerResult partitionerResult) {
        final MarcRecordInfo recordInfo = (MarcRecordInfo) partitionerResult.getRecordInfo();
        final SortOrder sortOrder = getReorderedItemSortOrder(recordInfo);
        final ReorderedItemEntity reorderedItemEntity = new ReorderedItemEntity()
                .withJobId(jobId)
                .withSortkey(sortOrder.getIntValue())
                .withChunkItem(partitionerResult.getChunkItem())
                .withRecordInfo(recordInfo)
                .withPositionInDatafile(partitionerResult.getPositionInDatafile());
        entityManager.persist(reorderedItemEntity);
        numberOfItems++;
        return DataPartitionerResult.EMPTY;
    }

    /* Retrieves next DataPartitionerResult in line from internal list */
    DataPartitionerResult getReorderedItem() {
        final DataPartitionerResult partitionerResult;

        final ReorderedItemEntity reorderedItemEntity = getNextItemFromDatabase().orElse(null);
        if (reorderedItemEntity != null) {
            partitionerResult = new DataPartitionerResult(reorderedItemEntity.getChunkItem(),
                    reorderedItemEntity.getRecordInfo(), reorderedItemEntity.getPositionInDatafile());
            entityManager.remove(reorderedItemEntity);
            numberOfItems--;
        } else {
            partitionerResult = DataPartitionerResult.EMPTY;
        }
        return partitionerResult;
    }

    abstract SortOrder getReorderedItemSortOrder(MarcRecordInfo recordInfo);

    private int getNumberOfItemsInDatabase() {
        return Math.toIntExact(entityManager.createNamedQuery(ReorderedItemEntity.GET_ITEMS_COUNT_BY_JOBID_QUERY_NAME, Long.class)
                .setParameter("jobId", jobId)
                .getSingleResult());
    }

    Optional<ReorderedItemEntity> getNextItemFromDatabase() {
        return entityManager.createNamedQuery(ReorderedItemEntity.GET_NEXT_ITEM_BY_JOBID_QUERY_NAME, ReorderedItemEntity.class)
                .setParameter("jobId", jobId)
                .setMaxResults(1)
                .getResultList()
                .stream()
                .findFirst();
    }
}
