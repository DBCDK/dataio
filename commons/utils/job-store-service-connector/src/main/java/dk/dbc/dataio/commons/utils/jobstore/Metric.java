package dk.dbc.dataio.commons.utils.jobstore;

import org.eclipse.microprofile.metrics.MetricRegistry;
import org.eclipse.microprofile.metrics.SimpleTimer;
import org.eclipse.microprofile.metrics.Tag;

public enum Metric {
    ADD_ACC_TEST_JOB,
    ADD_CHUNK,
    ADD_EMPTY_JOB,
    ABORT_JOB,
    RESEND_JOB,
    ADD_JOB,
    ADD_NOTIFICATION,
    COUNT_ITEMS,
    COUNT_JOBS,
    GET_CACHED_FLOW,
    GET_CHUNK_ITEM,
    GET_PROCESSED_NEXT_RESULT,
    GET_SINK_STATUS_LIST,
    LIST_INVALID_TRANSFILE_NOTIFICATIONS,
    LIST_ITEMS,
    LIST_JOB_NOTIFICATIONS_FOR_JOB,
    LIST_JOBS,
    LIST_JOBS_CRIT,
    SET_WORKFLOW_NOTE,
    SET_WORKFLOW_NOTE2,
    SINK_STATUS;

    public SimpleTimer simpleTimer(MetricRegistry metricRegistry, Tag... tags) {
        return metricRegistry.simpleTimer(prefix() + "_" + name().toLowerCase(), tags);
    }

    private String prefix() {
        return "dataio_jobstore";
    }

    public enum StatusTag {
        FAILED, SUCCESS;

        public final Tag tag;

        StatusTag() {
            this.tag = new Tag("status", name().toLowerCase());
        }
    }
}
