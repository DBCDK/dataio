package dk.dbc.dataio.jobstore.types.criteria;

/**
 * Chunk listing ListCriteria implementation
 */
public class ChunkListCriteria extends ListCriteria<ChunkListCriteria.Field, ChunkListCriteria> {
    /**
     * Available criteria fields
     */
    public enum Field implements ListFilterField {
        /**
         * chunk creation time
         */
        TIME_OF_CREATION,
        /**
         * chunk completion time
         */
        TIME_OF_COMPLETION
    }
}
