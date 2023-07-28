package dk.dbc.dataio.gui.server;

import dk.dbc.dataio.common.utils.flowstore.FlowStoreServiceConnector;
import dk.dbc.dataio.common.utils.flowstore.FlowStoreServiceConnectorException;
import dk.dbc.dataio.commons.types.HarvesterToken;
import dk.dbc.dataio.commons.types.JobSpecification;
import dk.dbc.dataio.commons.types.Sink;
import dk.dbc.dataio.commons.types.SinkContent;
import dk.dbc.dataio.gui.server.jobrerun.JobRerunScheme;
import dk.dbc.dataio.gui.server.jobrerun.JobRerunScheme.Action;
import dk.dbc.dataio.gui.server.jobrerun.JobRerunScheme.Type;
import dk.dbc.dataio.jobstore.types.FlowStoreReferences;
import dk.dbc.dataio.jobstore.types.JobInfoSnapshot;
import dk.dbc.dataio.jobstore.types.State;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;

import static dk.dbc.dataio.commons.types.JobSpecification.JOB_EXPIRATION_AGE_IN_DAYS;

/**
 * Rerun rules: (top 3 supersedes bottom 3):
 * Type = [any] means that the Action takes precedence.
 * <p>
 * Action = COPY, the job can only by rerun by re-submitting a copy of the existing job.
 * Action = RERUN_ALL, the job can be rerun by re-creating the job.
 * Action = RERUN_FAILED, the job can be rerun by re-creating a job containing only failed items.
 * <p>
 * ************************************************************************* *
 * +------------------+  +-----------------------+  +----------------------+ *
 * | fatal diagnostic |= | Action = COPY         |= | Type = [any]         | *
 * +------------------+  +-----------------------+  +----------------------+ *
 * +------------------+  +-----------------------+  +----------------------+ *
 * | preview jobs     |= | Action = COPY         |= | Type = [any]         | *
 * +------------------+  +-----------------------+  +----------------------+ *
 * +------------------+  +-----------------------+  +----------------------+ *
 * | no failed items  |= | Action = RERUN_ALL    |= | Type = [any]         | *
 * +------------------+  +-----------------------+  +----------------------+ *
 * ************************************************************************* *
 * +------------------+  +-----------------------+  +----------------------+ *
 * | to tickle total  |= | Action = RERUN_ALL    |= | Type = TICKLE        | *
 * +------------------+  +-----------------------+  +----------------------+ *
 * +----------------------+  +-----------------------+  +------------------+ *
 * | to tickle incremental|= | Action = RERUN_ALL    |= | Type = TICKLE    | *
 * +----------------------+  | Action = RERUN_FAILED |  |                  | *
 * +-----------------------+  +------------------+ *
 * +------------------+  +-----------------------+  +----------------------+ *
 * | from tickle      |= | Action = RERUN_ALL    |= | Type = TICKLE        | *
 * +------------------+  | Action = RERUN_FAILED |  |                      | *
 * +-----------------------+  +----------------------+ *
 * +------------------+  +-----------------------+  +----------------------+ *
 * | from raw repo    |= | Action = RERUN_ALL    |= | Type = RR            | *
 * +------------------+  | Action = RERUN_FAILED |  |                      | *
 * +-----------------------+  +----------------------+ *
 * +------------------+  +-----------------------+  +----------------------+ *
 * | "normal" job     |= | Action = RERUN_ALL    |= | Type = ORIGINAL_FILE | *
 * +------------------+  | Action = RERUN_FAILED |  |                      | *
 * +-----------------------+  +----------------------+ *
 * ************************************************************************* *
 */
public class JobRerunSchemeParser {
    private final FlowStoreServiceConnector flowStoreServiceConnector;

    public JobRerunSchemeParser(FlowStoreServiceConnector flowStoreServiceConnector) {
        this.flowStoreServiceConnector = flowStoreServiceConnector;
    }

    /**
     * Deciphers the rule set for rerunning different types of jobs.
     *
     * @param jobInfoSnapshot the job to decipher the rerun rules for.
     * @return jobRerunScheme containing legal actions for given job as well as type of rerun.
     * @throws FlowStoreServiceConnectorException on failure to look up sink
     */
    public JobRerunScheme parse(JobInfoSnapshot jobInfoSnapshot) throws FlowStoreServiceConnectorException {
        final Sink sink = lookupSink(jobInfoSnapshot);
        final Type type = getRerunType(jobInfoSnapshot, sink);
        final Set<Action> actions = populateActions(jobInfoSnapshot, sink);
        return new JobRerunScheme().withType(type).withActions(actions);
    }

    /* private methods */

    private Type getRerunType(JobInfoSnapshot jobInfoSnapshot, Sink sink) {
        if (isFromRawRepo(jobInfoSnapshot)) {
            return Type.RR;
        } else if (isTickle(jobInfoSnapshot, sink)) {
            return Type.TICKLE;
        } else {
            // Any job that is not of type (TICKLE, RR)
            return Type.ORIGINAL_FILE;
        }
    }

    private boolean isTickle(JobInfoSnapshot jobInfoSnapshot, Sink sink) {
        return isFromTickle(jobInfoSnapshot) || isToTickle(sink);
    }

    /*
     * Determines if the job is to be rerun towards tickle repo
     */
    private boolean isToTickle(Sink sink) {
        if (sink == null) {
            return false;
        }
        return sink.getContent().getSinkType() == SinkContent.SinkType.TICKLE;
    }

    private Sink lookupSink(JobInfoSnapshot jobInfoSnapshot) throws FlowStoreServiceConnectorException {
        if (jobInfoSnapshot.getSpecification().getType() == JobSpecification.Type.ACCTEST
                || jobInfoSnapshot.getFlowStoreReferences() == null || jobInfoSnapshot.getFlowStoreReferences().getReference(FlowStoreReferences.Elements.SINK) == null) {
            return null;
        }
        return flowStoreServiceConnector.getSink(jobInfoSnapshot.getFlowStoreReferences().getReference(FlowStoreReferences.Elements.SINK).getId());
    }

    /*
     * Determines if the job is to be rerun from tickle repo
     */
    private boolean isFromTickle(JobInfoSnapshot jobInfoSnapshot) {
        if (jobInfoSnapshot.getSpecification().getAncestry() == null) {
            return false;
        }
        final HarvesterToken harvesterToken = HarvesterToken.of(jobInfoSnapshot.getSpecification().getAncestry().getHarvesterToken());
        return harvesterToken.getHarvesterVariant() == HarvesterToken.HarvesterVariant.TICKLE_REPO;
    }

    /*
     * Determines if the job is to be rerun from raw repo
     */
    private boolean isFromRawRepo(JobInfoSnapshot jobInfoSnapshot) {
        if (jobInfoSnapshot.getSpecification().getAncestry() == null) {
            return false;
        }
        final HarvesterToken harvesterToken = HarvesterToken.of(jobInfoSnapshot.getSpecification().getAncestry().getHarvesterToken());
        return harvesterToken.getHarvesterVariant() == HarvesterToken.HarvesterVariant.RAW_REPO;
    }

    /*
     * Adds the actions that are legal to perform when rerunning the given job
     */
    private Set<Action> populateActions(JobInfoSnapshot jobInfoSnapshot, Sink sink) {
        final Set<Action> legalActions = new HashSet<>();
        if (isOutDated(jobInfoSnapshot)) {
            return legalActions;
        }

        if (isCopyJob(jobInfoSnapshot)) {
            legalActions.add(Action.COPY);
        } else {
            legalActions.add(Action.RERUN_ALL);
            if (canRerunFailedOnly(jobInfoSnapshot, sink)) {
                legalActions.add(Action.RERUN_FAILED);
            }
        }
        return legalActions;
    }

    /*
     * Check if job is so old, that it cannot be re-run.
     */
    private boolean isOutDated(JobInfoSnapshot jobInfoSnapshot) {
        if (jobInfoSnapshot.getTimeOfCompletion() == null) {
            return false;
        }
        final Date marker = Date.from(Instant.now().minus(JOB_EXPIRATION_AGE_IN_DAYS, ChronoUnit.DAYS));
        return jobInfoSnapshot.getTimeOfCompletion().before(marker);
    }

    /*
     *   The job is to be recreated as a copy from file store if:
     *   The job has encountered a fatal error
     *   The job is of type preview (items present without chunk)
     */
    private boolean isCopyJob(JobInfoSnapshot jobInfoSnapshot) {
        return jobInfoSnapshot.getNumberOfChunks() == 0 && jobInfoSnapshot.getNumberOfItems() > 0 || jobInfoSnapshot.hasFatalError();
    }

    /*
     * Job can only rerun with failed only option if:
     * The job has not encountered a fatal error
     * The job contains failed items
     * The job is not to be rerun towards tickle repo total sink
     */
    private boolean canRerunFailedOnly(JobInfoSnapshot jobInfoSnapshot, Sink sink) {
        if (getNumberOfFailedItems(jobInfoSnapshot) == 0) {
            return false;
        } else return sink == null || !sink.getContent().getQueue().equals(JobRerunScheme.TICKLE_TOTAL);
    }

    /*
     * Determines if the given job has failed items in any phase
     */
    private int getNumberOfFailedItems(JobInfoSnapshot jobInfoSnapshot) {
        final State state = jobInfoSnapshot.getState();
        return state.getPhase(State.Phase.PARTITIONING).getFailed()
                + state.getPhase(State.Phase.PROCESSING).getFailed()
                + state.getPhase(State.Phase.DELIVERING).getFailed();
    }
}
