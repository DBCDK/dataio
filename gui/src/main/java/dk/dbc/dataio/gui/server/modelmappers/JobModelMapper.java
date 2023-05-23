package dk.dbc.dataio.gui.server.modelmappers;

import dk.dbc.dataio.commons.types.Diagnostic;
import dk.dbc.dataio.commons.types.JobSpecification;
import dk.dbc.dataio.gui.client.model.DiagnosticModel;
import dk.dbc.dataio.gui.client.model.JobModel;
import dk.dbc.dataio.gui.client.model.StateModel;
import dk.dbc.dataio.gui.client.util.Format;
import dk.dbc.dataio.jobstore.types.FlowStoreReference;
import dk.dbc.dataio.jobstore.types.FlowStoreReferences;
import dk.dbc.dataio.jobstore.types.JobInfoSnapshot;
import dk.dbc.dataio.jobstore.types.JobInputStream;
import dk.dbc.dataio.jobstore.types.State;
import dk.dbc.dataio.jobstore.types.StateChange;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;

/**
 * The Job Model Mapper class maps a jobs from JobInfoSnapshot objects to JobModel objects
 */
public class JobModelMapper {

    /**
     * Maps a single JobInfoSnapshot object to a JobModel object
     *
     * @param jobInfoSnapshot The input JobInfoSnapshot object
     * @return The mapped JobModel object
     */
    public static JobModel toModel(JobInfoSnapshot jobInfoSnapshot) {
        final JobSpecification.Ancestry ancestry = jobInfoSnapshot.getSpecification().getAncestry();
        final SimpleDateFormat simpleDateFormat = new SimpleDateFormat(Format.LONG_DATE_TIME_FORMAT);
        return new JobModel()
                .withJobCreationTime(simpleDateFormat.format(jobInfoSnapshot.getTimeOfCreation()))
                .withJobCompletionTime(jobInfoSnapshot.getTimeOfCompletion() == null ? "" : simpleDateFormat.format(jobInfoSnapshot.getTimeOfCompletion()))
                .withJobId(String.valueOf(jobInfoSnapshot.getJobId()))
                .withSubmitterNumber(Long.toString(jobInfoSnapshot.getSpecification().getSubmitterId()))
                .withSubmitterName(getSubmitterName(jobInfoSnapshot.getFlowStoreReferences()))
                .withFlowBinderName(getFlowBinderName(jobInfoSnapshot.getFlowStoreReferences()))
                .withSinkId(getSinkId(jobInfoSnapshot.getFlowStoreReferences()))
                .withSinkName(getSinkName(jobInfoSnapshot.getFlowStoreReferences()))
                .withDiagnosticModels(getDiagnostics(jobInfoSnapshot.getState().getDiagnostics()))
                .withPackaging(jobInfoSnapshot.getSpecification().getPackaging())
                .withFormat(jobInfoSnapshot.getSpecification().getFormat())
                .withCharset(jobInfoSnapshot.getSpecification().getCharset())
                .withDestination(jobInfoSnapshot.getSpecification().getDestination())
                .withMailForNotificationAboutVerification(jobInfoSnapshot.getSpecification().getMailForNotificationAboutVerification())
                .withMailForNotificationAboutProcessing(jobInfoSnapshot.getSpecification().getMailForNotificationAboutProcessing())
                .withResultMailInitials(jobInfoSnapshot.getSpecification().getResultmailInitials())
                .withType(jobInfoSnapshot.getSpecification().getType())
                .withDataFile(jobInfoSnapshot.getSpecification().getDataFile())
                .withPartNumber(jobInfoSnapshot.getPartNumber())
                .withWorkflowNoteModel(WorkflowNoteModelMapper.toWorkflowNoteModel(jobInfoSnapshot.getWorkflowNote()))
                .withAncestry(ancestry)
                .withNumberOfItems(jobInfoSnapshot.getNumberOfItems())
                .withNumberOfChunks(jobInfoSnapshot.getNumberOfChunks())
                .withStateModel(toStateModel(jobInfoSnapshot.getState()))
                .withDiagnosticFatal(jobInfoSnapshot.hasFatalError());
    }

    /**
     * Maps a JobModel object to a JobInputStream
     *
     * @param jobModel The Job Model
     * @return a JobInputStream
     */
    public static JobInputStream toJobInputStream(JobModel jobModel) {

        final JobSpecification jobSpecification = new JobSpecification()
                .withPackaging(jobModel.getPackaging())
                .withFormat(jobModel.getFormat())
                .withCharset(jobModel.getCharset())
                .withDestination(jobModel.getDestination())
                .withSubmitterId(Integer.parseInt(jobModel.getSubmitterNumber()))
                .withMailForNotificationAboutVerification(jobModel.getMailForNotificationAboutVerification())
                .withMailForNotificationAboutProcessing(jobModel.getMailForNotificationAboutProcessing())
                .withResultmailInitials(jobModel.getResultMailInitials())
                .withDataFile(jobModel.getDataFile())
                .withType(jobModel.getType())
                .withAncestry(new JobSpecification.Ancestry()
                        .withTransfile(jobModel.getTransFileAncestry())
                        .withDatafile(jobModel.getDataFileAncestry())
                        .withBatchId(jobModel.getBatchIdAncestry())
                        .withDetails(jobModel.getDetailsAncestry().getBytes())
                        .withPreviousJobId(jobModel.getPreviousJobIdAncestry()));
        return new JobInputStream(jobSpecification);
    }

    /**
     * Maps a JobModel object to a JobInfoSnapshot with the information required
     * create a jobRerunScheme
     *
     * @param jobModel The Job Model
     * @return a jobInfoSnapshot populated with the required information
     */
    public static JobInfoSnapshot toJobInfoSnapshotForRerunScheme(JobModel jobModel) {
        JobInfoSnapshot jobInfoSnapshot = new JobInfoSnapshot()
                .withNumberOfChunks(jobModel.getNumberOfChunks())
                .withNumberOfItems(jobModel.getNumberOfItems())
                .withFatalError(jobModel.isDiagnosticFatal())
                .withState(toStateWithPhaseFailedInformation(jobModel.getStateModel()))
                .withSpecification(new JobSpecification().withType(jobModel.getType()))
                .withFlowStoreReferences(new FlowStoreReferences());
        if (!jobModel.getSinkName().isEmpty()) {
            jobInfoSnapshot.getFlowStoreReferences().withReference(FlowStoreReferences.Elements.SINK, new FlowStoreReference(jobModel.getSinkId(), 1, jobModel.getSinkName()));
        }
        if (jobModel.getHarvesterTokenAncestry() != null) {
            jobInfoSnapshot.getSpecification()
                    .withAncestry(new JobSpecification.Ancestry()
                            .withHarvesterToken(jobModel.getHarvesterTokenAncestry()));
        }
        return jobInfoSnapshot;
    }

    /**
     * Maps a list of JobInfoSnapshot objects to a list of JobModel objects
     *
     * @param jobInfoSnapshots A list of input JobInfoSnapshot objects
     * @return The list of resulting JobModel objects
     */
    public static List<JobModel> toModel(List<JobInfoSnapshot> jobInfoSnapshots) {
        List<JobModel> jobInfoSnapshotModels = new ArrayList<>(jobInfoSnapshots.size());

        for (JobInfoSnapshot jobInfoSnapshot : jobInfoSnapshots) {
            jobInfoSnapshotModels.add(toModel(jobInfoSnapshot));
        }
        return jobInfoSnapshotModels;
    }

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
     * This method retrieves all diagnostics.
     *
     * @param diagnostics containing Warning or Error information
     * @return list of diagnostic models. Empty list if no diagnostics were found.
     */
    private static List<DiagnosticModel> getDiagnostics(List<Diagnostic> diagnostics) {
        List<DiagnosticModel> diagnosticModels = new ArrayList<>(diagnostics.size());
        for (Diagnostic diagnostic : diagnostics) {
            diagnosticModels.add(new DiagnosticModel(diagnostic.getLevel().name(), diagnostic.getMessage(), diagnostic.getStacktrace()));
        }
        return diagnosticModels;
    }

    private static State toStateWithPhaseFailedInformation(StateModel stateModel) {
        final StateChange partitioning = new StateChange();
        partitioning.setPhase(State.Phase.PARTITIONING).setFailed(stateModel.getPartitioning().getFailed());
        final StateChange processing = new StateChange();
        processing.setPhase(State.Phase.PROCESSING).setFailed(stateModel.getProcessing().getFailed());
        final StateChange delivering = new StateChange();
        delivering.setPhase(State.Phase.DELIVERING).setFailed(stateModel.getDelivering().getFailed());

        final State state = new State();
        state.updateState(partitioning);
        state.updateState(processing);
        state.updateState(delivering);
        return state;
    }

    private static StateModel toStateModel(State state) {
        return new StateModel()
                .withPartitioning(state.getPhase(State.Phase.PARTITIONING))
                .withProcessing(state.getPhase(State.Phase.PROCESSING))
                .withDelivering(state.getPhase(State.Phase.DELIVERING))
                .withAborted(state.isAborted());
    }
}
