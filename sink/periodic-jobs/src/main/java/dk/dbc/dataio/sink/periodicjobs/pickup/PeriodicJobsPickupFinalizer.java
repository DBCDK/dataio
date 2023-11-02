package dk.dbc.dataio.sink.periodicjobs.pickup;

import dk.dbc.dataio.commons.macroexpansion.MacroSubstitutor;
import dk.dbc.dataio.commons.types.Chunk;
import dk.dbc.dataio.commons.types.exceptions.InvalidMessageException;
import dk.dbc.dataio.commons.utils.jobstore.JobStoreServiceConnector;
import dk.dbc.dataio.commons.utils.jobstore.JobStoreServiceConnectorException;
import dk.dbc.dataio.harvester.types.HttpPickup;
import dk.dbc.dataio.harvester.types.Pickup;
import dk.dbc.dataio.jobstore.types.JobInfoSnapshot;
import dk.dbc.dataio.jobstore.types.State;
import dk.dbc.dataio.jobstore.types.StateElement;
import dk.dbc.dataio.sink.periodicjobs.PeriodicJobsDelivery;
import dk.dbc.weekresolver.connector.WeekResolverConnector;
import dk.dbc.weekresolver.connector.WeekResolverConnectorException;
import dk.dbc.weekresolver.model.WeekResolverResult;
import jakarta.persistence.EntityManager;
import jakarta.ws.rs.ProcessingException;

import java.time.LocalDate;
import java.util.List;

public abstract class PeriodicJobsPickupFinalizer {
    WeekResolverConnector weekResolverConnector;

    JobStoreServiceConnector jobStoreServiceConnector;


    public boolean isEmptyJob(Chunk endChunk) throws InvalidMessageException  {
        if (endChunk.getChunkId() == 0) {
            // End chunk having ID 0 means job is empty
            return true;
        }
        return isIgnoredJob(endChunk.getJobId());
    }

    public MacroSubstitutor getMacroSubstitutor(PeriodicJobsDelivery delivery) {
        // There is a risk here, that if the sink for some
        // reason has been unable to process any messages
        // for an extended period of time, then the
        // time-of-last-harvest may not match the
        // delivery in question because other harvests may
        // have run in the meantime. We estimate this is a
        // negligible risk compared to the amount of work
        // required to implement a solution where the exact
        // timestamp is carried through the entire dataIO
        // infrastructure for each job.
        return new MacroSubstitutor(delivery.getConfig().getContent().getTimeOfLastHarvest().toInstant(),
                this::getWeekCode)
                .add("__JOBID__", delivery.getJobId().toString())
                .add("__TODAY__", LocalDate.now().toString())
                .add("__JOBNAME__", delivery.getConfig().getContent().getName());
    }

    private boolean isIgnoredJob(long jobId) {
        final JobInfoSnapshot jobInfoSnapshot = getJobInfoSnapshot(jobId);
        if (jobInfoSnapshot != null) {
            final State state = jobInfoSnapshot.getState();
            final StateElement processingPhase = state.getPhase(State.Phase.PROCESSING);
            // If all job items were ignored by the job processor, handle as empty job
            return processingPhase.getIgnored() == processingPhase.getNumberOfItems();
        }
        return false;
    }

    String getRemoteFilename(PeriodicJobsDelivery delivery) {
        final MacroSubstitutor macroSubstitutor = getMacroSubstitutor(delivery);
        String overideFilename = delivery.getConfig().getContent().getPickup().getOverrideFilename();

        if (overideFilename != null && !overideFilename.isEmpty()) {
            final Pickup pickup = delivery.getConfig().getContent().getPickup();
            if (pickup instanceof HttpPickup && overideFilename.equals("autoprint")) {
                // automatically postfix autoprint job filenames with the default
                // remote filename value.
                return overideFilename + "." + getDefaultRemoteFilename(delivery);
            }
            return macroSubstitutor.replace(overideFilename);
        }
        return getDefaultRemoteFilename(delivery);
    }

    private String getDefaultRemoteFilename(PeriodicJobsDelivery delivery) {
        return delivery.getConfig().getContent()
                .getName()
                .toLowerCase()
                .replaceAll("[^\\p{ASCII}]", "")
                .replaceAll("\\s+", "_") + "." + delivery.getJobId();
    }

    private JobInfoSnapshot getJobInfoSnapshot(long jobId) {
        try {
            final List<JobInfoSnapshot> jobInfoSnapshots = jobStoreServiceConnector.listJobs("job:id = " + jobId);
            if (!jobInfoSnapshots.isEmpty()) {
                return jobInfoSnapshots.get(0);
            }
        } catch (RuntimeException | JobStoreServiceConnectorException e) {
            throw new RuntimeException("Unable to get jobinfo.", e);
        }
        return null;
    }

    private String getWeekCode(String catalogueCode, LocalDate localDate) {
        try {
            final WeekResolverResult weekResolverResult = weekResolverConnector.getCurrentWeekCodeForDate(catalogueCode, localDate);
            return String.format("%d%02d", weekResolverResult.getYear(), weekResolverResult.getWeekNumber());
        } catch (WeekResolverConnectorException e) {
            throw new ProcessingException(e);
        }
    }

    public abstract Chunk deliver(Chunk chunk, PeriodicJobsDelivery delivery, EntityManager entityManager) throws InvalidMessageException;

    public PeriodicJobsPickupFinalizer withJobStoreServiceConnector(JobStoreServiceConnector jobStoreServiceConnector) {
        this.jobStoreServiceConnector = jobStoreServiceConnector;
        return this;
    }

    public PeriodicJobsPickupFinalizer withWeekResolverConnector(WeekResolverConnector weekResolverConnector) {
        this. weekResolverConnector = weekResolverConnector;
        return this;
    }

}
