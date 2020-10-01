/*
 * Copyright Dansk Bibliotekscenter a/s. Licensed under GNU GPLv3
 * See license text in LICENSE.txt
 */

package dk.dbc.dataio.sink.periodicjobs;

import dk.dbc.dataio.commons.types.Chunk;
import dk.dbc.dataio.commons.utils.jobstore.JobStoreServiceConnector;
import dk.dbc.dataio.commons.utils.jobstore.JobStoreServiceConnectorException;
import dk.dbc.dataio.jobstore.types.JobInfoSnapshot;
import dk.dbc.dataio.jobstore.types.State;
import dk.dbc.dataio.jobstore.types.StateElement;
import dk.dbc.dataio.sink.types.SinkException;

import java.util.List;

public abstract class PeriodicJobsPickupFinalizer {
    public boolean isEmptyJob(Chunk endChunk, JobStoreServiceConnector jobStoreServiceConnector) throws SinkException {
        if (endChunk.getChunkId() == 0) {
            // End chunk having ID 0 means job is empty
            return true;
        }
        final List<JobInfoSnapshot> jobInfoSnapshots;
        try {
            jobInfoSnapshots = jobStoreServiceConnector.listJobs("job:id = " + endChunk.getJobId());
        } catch (RuntimeException | JobStoreServiceConnectorException e) {
            throw new SinkException(e);
        }
        if (!jobInfoSnapshots.isEmpty()) {
            final JobInfoSnapshot jobInfoSnapshot = jobInfoSnapshots.get(0);
            final State state = jobInfoSnapshot.getState();
            final StateElement processingPhase = state.getPhase(State.Phase.PROCESSING);
            // If all job items were ignored by the job processor, handle as empty job
            return processingPhase.getIgnored() == processingPhase.getNumberOfItems();
        }
        return false;
    }

     String getRemoteFilename(PeriodicJobsDelivery delivery) {
        String overideFilename = delivery.getConfig().getContent().getPickup().getOverrideFilename();

        if (overideFilename != null && !overideFilename.isEmpty()) {
            return overideFilename;
        }
        else {
            return delivery.getConfig().getContent()
                    .getName()
                    .toLowerCase()
                    .replaceAll("[^\\p{ASCII}]", "")
                    .replaceAll("\\s+", "_") + "." + delivery.getJobId();
        }
    }

    public abstract Chunk deliver(Chunk chunk, PeriodicJobsDelivery delivery) throws SinkException;
}
