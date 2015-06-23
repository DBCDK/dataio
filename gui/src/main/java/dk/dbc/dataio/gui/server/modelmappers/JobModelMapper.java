package dk.dbc.dataio.gui.server.modelmappers;

import dk.dbc.dataio.gui.client.model.DiagnosticModel;
import dk.dbc.dataio.gui.client.model.JobModel;
import dk.dbc.dataio.gui.client.util.Format;
import dk.dbc.dataio.jobstore.types.Diagnostic;
import dk.dbc.dataio.jobstore.types.FlowStoreReference;
import dk.dbc.dataio.jobstore.types.FlowStoreReferences;
import dk.dbc.dataio.jobstore.types.JobInfoSnapshot;
import dk.dbc.dataio.jobstore.types.State;
import dk.dbc.dataio.jobstore.types.StateElement;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;

/**
 * The Job Model Mapper class maps a jobs from JobInfoSnapshot objects to JobModel objects
 */
public class JobModelMapper {

    /*
     * Public Methods
     */

    /**
     * Maps a single JobInfoSnapshot object to a JobModel object
     * @param jobInfoSnapshot The input JobInfoSnapshot object
     * @return The mapped JobModel object
     */
    public static JobModel toModel(JobInfoSnapshot jobInfoSnapshot) {

        SimpleDateFormat simpleDateFormat = new SimpleDateFormat(Format.LONG_DATE_TIME_FORMAT);

        return new JobModel(
                simpleDateFormat.format(jobInfoSnapshot.getTimeOfCreation()),
                String.valueOf(jobInfoSnapshot.getJobId()),
                jobInfoSnapshot.getSpecification().getDataFile().replaceFirst("^/tmp/", ""),
                Long.toString(jobInfoSnapshot.getSpecification().getSubmitterId()),
                getSubmitterName(jobInfoSnapshot.getFlowStoreReferences()) ,
                getFlowBinderName(jobInfoSnapshot.getFlowStoreReferences()),
                getSinkId(jobInfoSnapshot.getFlowStoreReferences()),
                getSinkName(jobInfoSnapshot.getFlowStoreReferences()),
                jobInfoSnapshot.getState().allPhasesAreDone(),
                getTotal(jobInfoSnapshot.getState()),
                getSucceeded(jobInfoSnapshot.getState()),
                getFailed(jobInfoSnapshot.getState()),
                getIgnored(jobInfoSnapshot.getState()),
                getStateCount(jobInfoSnapshot.getState().getPhase(State.Phase.PARTITIONING)),
                getStateCount(jobInfoSnapshot.getState().getPhase(State.Phase.PROCESSING)),
                getStateCount(jobInfoSnapshot.getState().getPhase(State.Phase.DELIVERING)),
                getDiagnostics(jobInfoSnapshot.getState().getDiagnostics()),
                jobInfoSnapshot.getSpecification().getPackaging(),
                jobInfoSnapshot.getSpecification().getFormat(),
                jobInfoSnapshot.getSpecification().getCharset(),
                jobInfoSnapshot.getSpecification().getDestination(),
                jobInfoSnapshot.getSpecification().getMailForNotificationAboutVerification(),
                jobInfoSnapshot.getSpecification().getMailForNotificationAboutProcessing(),
                jobInfoSnapshot.getSpecification().getResultmailInitials());
    }

    /**
     * Maps a list of JobInfoSnapshot objects to a list of JobModel objects
     * @param jobInfoSnapshots A list of input JobInfoSnapshot objects
     * @return The list of resulting JobModel objects
     */
    public static List<JobModel> toModel(List<JobInfoSnapshot> jobInfoSnapshots) {
        List<JobModel> jobInfoSnapshotModels = new ArrayList<JobModel>(jobInfoSnapshots.size());

        for(JobInfoSnapshot jobInfoSnapshot : jobInfoSnapshots) {
            jobInfoSnapshotModels.add(toModel(jobInfoSnapshot));
        }
        return jobInfoSnapshotModels;
    }


    /*
     * Private Methods
     */

    /**
     * This method retrieves the name of the submitter.
     * Due to a change in the database scheme it is necessary to check if the referenced submitter exists.
     * For jobs created before 02.03.2015 the reference will be null.
     *
     * @param references the referenced flow store elements
     * @return name of the submitter
     */
    private static String getSubmitterName(FlowStoreReferences references) {
        FlowStoreReference submitterReference = references.getReference(FlowStoreReferences.Elements.SUBMITTER);
        return submitterReference == null ? "" : submitterReference.getName();
    }

    /**
     * This method retrieves the name of the flow binder.
     * Due to a change in the database scheme it is necessary to check if the referenced flow binder exists.
     * For jobs created before 02.03.2015 the reference will be null.
     *
     * @param references the referenced flow store elements
     * @return name of the flow binder
     */
    private static String getFlowBinderName(FlowStoreReferences references) {
        FlowStoreReference flowBinderReference = references.getReference(FlowStoreReferences.Elements.FLOW_BINDER);
        return flowBinderReference == null ? "" : flowBinderReference.getName();
    }

    /**
     * This method retrieves the id of the sink.
     * Due to a change in the database scheme it is necessary to check if the referenced sink exists.
     * For jobs created before 02.03.2015 the reference will be null.
     *
     * @param references the referenced flow store elements
     * @return id of the sink - zero if the referenced sink doesn't exist
     */
    private static long getSinkId(FlowStoreReferences references) {
        FlowStoreReference sinkReference = references.getReference(FlowStoreReferences.Elements.SINK);
        return sinkReference == null ? 0 : sinkReference.getId();
    }

    /**
     * This method retrieves the name of the sink.
     * Due to a change in the database scheme it is necessary to check if the referenced sink exists.
     * For jobs created before 02.03.2015 the reference will be null.
     *
     * @param references the referenced flow store elements
     * @return name of the sink
     */
    private static String getSinkName(FlowStoreReferences references) {
        FlowStoreReference sinkReference = references.getReference(FlowStoreReferences.Elements.SINK);
        return sinkReference == null ? "" : sinkReference.getName();
    }

    /**
     * This method determine how many posts that successfully have completed one or more phases.
     * This is done for each phase, in order to display information to the user for a job that has not yet finished.
     *
     * @param state containing information regarding the job
     * @return number of succeeded items.
     */
    private static long getSucceeded(State state) {
        long succeeded;
        if(state.getPhase(State.Phase.DELIVERING).getSucceeded() != 0) {
            succeeded = state.getPhase(State.Phase.DELIVERING).getSucceeded();
        } else if(state.getPhase(State.Phase.PROCESSING).getSucceeded() != 0) {
            succeeded = state.getPhase(State.Phase.PROCESSING).getSucceeded();
        }
        else {
            succeeded = state.getPhase(State.Phase.PARTITIONING).getSucceeded();
        }
        return succeeded;
    }

    private static int getFailed(State state) {
        return state.getPhase(State.Phase.PARTITIONING).getFailed() +
                state.getPhase(State.Phase.PROCESSING).getFailed() +
                state.getPhase(State.Phase.DELIVERING).getFailed();
    }

    /**
     * This method determine how many posts that have been ignored. This is done for each phase,
     * in order to display information to the user, for a job that has not yet finished.
     *
     * @param state containing information regarding the job
     * @return number of ignored items.
     */
    private static int getIgnored(State state) {
        int ignored;
        if(state.getPhase(State.Phase.DELIVERING).getIgnored() != 0) {
            ignored = state.getPhase(State.Phase.DELIVERING).getIgnored();
        } else if(state.getPhase(State.Phase.PROCESSING).getIgnored() != 0) {
            ignored = state.getPhase(State.Phase.PROCESSING).getIgnored();
        } else {
            ignored = state.getPhase(State.Phase.PARTITIONING).getIgnored();
        }
        return ignored;
    }

    /**
     * This method retrieves all diagnostics.
     * @param diagnostics containing Warning or Error information
     * @return list of diagnostic models. Empty list if no diagnostics were found.
     */
    private static List<DiagnosticModel> getDiagnostics(List<Diagnostic> diagnostics) {
        List<DiagnosticModel> diagnosticModels = new ArrayList<DiagnosticModel>(diagnostics.size());
        for(Diagnostic diagnostic : diagnostics) {
            diagnosticModels.add(new DiagnosticModel(diagnostic.getLevel().toString(), diagnostic.getMessage(), diagnostic.getStacktrace()));
        }
        return diagnosticModels;
    }

    /**
     * This method finds the total amount of posts. It is using the number from the PARTITIONING phase
     * in order to display information even if the job does not complete one of the following phases:
     * PROCESSING/DELIVERING
     *
     * @param state containing information regarding the job
     * @return total number of items.
     */
    private static int getTotal(State state) {
        StateElement stateElement = state.getPhase(State.Phase.PARTITIONING);
        return stateElement.getSucceeded() + stateElement.getFailed() + stateElement.getIgnored();
    }

    /**
     * This method calculates the total number of items in the given state element
     *
     * @param element The state element for the state in question
     * @return The total number of items in the give state
     */
    private static long getStateCount(StateElement element) {
        return element.getSucceeded() + element.getFailed() + element.getIgnored();
    }

}
