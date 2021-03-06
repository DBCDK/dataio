/*
 * DataIO - Data IO
 * Copyright (C) 2015 Dansk Bibliotekscenter a/s, Tempovej 7-11, DK-2750 Ballerup,
 * Denmark. CVR: 15149043
 *
 * This file is part of DataIO.
 *
 * DataIO is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * DataIO is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with DataIO.  If not, see <http://www.gnu.org/licenses/>.
 */

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

import java.util.HashSet;
import java.util.Set;

/**
 * Rerun rules: (top 3 supersedes bottom 3):
 * Type = [any] means that the Action takes precedence.
 *
 * Action = COPY, the job can only by rerun by re-submitting a copy of the existing job.
 * Action = RERUN_ALL, the job can be rerun by re-creating the job.
 * Action = RERUN_FAILED, the job can be rerun by re-creating a job containing only failed items.
 *
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
 *                           +-----------------------+  +------------------+ *
 * +------------------+  +-----------------------+  +----------------------+ *
 * | from tickle      |= | Action = RERUN_ALL    |= | Type = TICKLE        | *
 * +------------------+  | Action = RERUN_FAILED |  |                      | *
 *                       +-----------------------+  +----------------------+ *
 * +------------------+  +-----------------------+  +----------------------+ *
 * | from raw repo    |= | Action = RERUN_ALL    |= | Type = RR            | *
 * +------------------+  | Action = RERUN_FAILED |  |                      | *
 *                       +-----------------------+  +----------------------+ *
 * +------------------+  +-----------------------+  +----------------------+ *
 * | "normal" job     |= | Action = RERUN_ALL    |= | Type = ORIGINAL_FILE | *
 * +------------------+  | Action = RERUN_FAILED |  |                      | *
 *                       +-----------------------+  +----------------------+ *
 * ************************************************************************* *
 */
public class JobRerunSchemeParser {
    private final FlowStoreServiceConnector flowStoreServiceConnector;

    public JobRerunSchemeParser(FlowStoreServiceConnector flowStoreServiceConnector) {
        this.flowStoreServiceConnector = flowStoreServiceConnector;
    }

    /**
     * Deciphers the rule set for rerunning different types of jobs.
     * @param jobInfoSnapshot the job to decipher the rerun rules for.
     *
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

    private Type getRerunType(JobInfoSnapshot jobInfoSnapshot, Sink sink) throws FlowStoreServiceConnectorException {
        if(isFromRawRepo(jobInfoSnapshot)) {
            return Type.RR;
        } else if(isTickle(jobInfoSnapshot, sink)) {
            return Type.TICKLE;
        } else {
            // Any job that is not of type (TICKLE, RR)
            return Type.ORIGINAL_FILE;
        }
    }

    private boolean isTickle(JobInfoSnapshot jobInfoSnapshot, Sink sink) throws FlowStoreServiceConnectorException {
        return isFromTickle(jobInfoSnapshot) == true ? true : isToTickle(sink);
    }

    /*
    * Determines if the job is to be rerun towards tickle repo
    */
    private boolean isToTickle(Sink sink) throws FlowStoreServiceConnectorException {
        if(sink == null) {
            return false;
        }
        return sink.getContent().getSinkType() == SinkContent.SinkType.TICKLE;
    }

    private Sink lookupSink(JobInfoSnapshot jobInfoSnapshot) throws FlowStoreServiceConnectorException {
        if(jobInfoSnapshot.getSpecification().getType() == JobSpecification.Type.ACCTEST
                || jobInfoSnapshot.getFlowStoreReferences() == null || jobInfoSnapshot.getFlowStoreReferences().getReference(FlowStoreReferences.Elements.SINK) == null) {
            return null;
        }
        return flowStoreServiceConnector.getSink(jobInfoSnapshot.getFlowStoreReferences().getReference(FlowStoreReferences.Elements.SINK).getId());
    }

    /*
     * Determines if the job is to be rerun from tickle repo
     */
    private boolean isFromTickle(JobInfoSnapshot jobInfoSnapshot) {
        if(jobInfoSnapshot.getSpecification().getAncestry() == null) {
            return false;
        }
        final HarvesterToken harvesterToken = HarvesterToken.of(jobInfoSnapshot.getSpecification().getAncestry().getHarvesterToken());
        return harvesterToken.getHarvesterVariant() == HarvesterToken.HarvesterVariant.TICKLE_REPO;
    }

    /*
     * Determines if the job is to be rerun from raw repo
     */
    private boolean isFromRawRepo(JobInfoSnapshot jobInfoSnapshot) {
        if(jobInfoSnapshot.getSpecification().getAncestry() == null) {
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
        if(isCopyJob(jobInfoSnapshot)) {
            legalActions.add(Action.COPY);
        } else {
            legalActions.add(Action.RERUN_ALL);
            if(canRerunFailedOnly(jobInfoSnapshot, sink)) {
                legalActions.add(Action.RERUN_FAILED);
            }
        }
        return legalActions;
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
        } else if (sink != null && sink.getContent().getResource().equals(JobRerunScheme.TICKLE_TOTAL)) {
            return false;
        }
        return true;
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