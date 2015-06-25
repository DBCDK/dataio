package dk.dbc.dataio.jobstore.types.criteria;

public class ItemListCriteria extends ListCriteria<ItemListCriteria.Field, ItemListCriteria> {
    /**
     * Available criteria fields
     */
    public enum Field implements ListFilterField {
        /**
         * job id
         */
        JOB_ID,
        /**
         * chunk id
         */
        CHUNK_ID,
        /**
         * item id
         */
        ITEM_ID,
        /**
         * item creation time
         */
        TIME_OF_CREATION,
        /*
         * failed items
         */
        STATE_FAILED,
        /*
         * ignored items
         */
        STATE_IGNORED
    }
}
