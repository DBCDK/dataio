package dk.dbc.dataio.jobstore.types.criteria;

import java.io.Serializable;

/**
 * Job listing ListCriteria implementation
 */
public class JobListCriteria extends ListCriteria<JobListCriteria.Field, JobListCriteria> implements Serializable {
    /**
     * Available criteria fields
     */
    public enum Field implements ListFilterField {
        /**
         * job id
         */
        JOB_ID,
        /**
         * job creation time
         */
        TIME_OF_CREATION,
        /**
         * job last modification time
         */
        TIME_OF_LAST_MODIFICATION,
        /**
         * job completion time
         */
        TIME_OF_COMPLETION,
        /*
         * jobs failed while processing
         */
        STATE_PROCESSING_FAILED,
        /*
         * jobs failed while delivering
         */
        STATE_DELIVERING_FAILED,
        /**
         * sink id for sink referenced by job
         */
        SINK_ID,
        /**
         * job specification
         */
        SPECIFICATION,
        /**
         * jobs failed with a fatal error
         */
        JOB_CREATION_FAILED,
        /**
         * record id
         */
        RECORD_ID,
        /**
         * jobs of type preview only
         */
        PREVIEW_ONLY
    }
}
