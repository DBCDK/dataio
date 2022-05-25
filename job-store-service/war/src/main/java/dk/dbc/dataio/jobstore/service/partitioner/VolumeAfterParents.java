package dk.dbc.dataio.jobstore.service.partitioner;

import dk.dbc.dataio.jobstore.types.MarcRecordInfo;

import javax.persistence.EntityManager;

/**
 * The responsibility of this class is to ensure that volume records are
 * returned after their parents and possibly grandparents and in the reverse
 * order for deletions.
 *
 * This class assumes it is called in a transactional context with regards to
 * the given entity manager.
 *
 * This class is not thread safe.
 */
public class VolumeAfterParents extends JobItemReorderer {
    public VolumeAfterParents(int jobId, EntityManager entityManager) {
        super(jobId, entityManager);
    }

    @Override
    SortOrder getReorderedItemSortOrder(MarcRecordInfo recordInfo) {
        switch (recordInfo.getType()) {
            case VOLUME:
                if (recordInfo.isDelete()) {
                    return SortOrder.VOLUME_DELETE;
                }
                return SortOrder.VOLUME;
            case SECTION:
                if (recordInfo.isDelete()) {
                    return SortOrder.SECTION_DELETE;
                }
                return SortOrder.SECTION;
            default:
                if (recordInfo.isDelete()) {
                    return SortOrder.HEAD_DELETE;
                }
                return SortOrder.HEAD;
        }
    }
}
