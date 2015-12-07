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

package dk.dbc.dataio.gui.server.modelmappers;

import dk.dbc.dataio.commons.types.Diagnostic;
import dk.dbc.dataio.commons.types.JobSpecification;
import dk.dbc.dataio.gui.client.model.DiagnosticModel;
import dk.dbc.dataio.gui.client.model.JobModel;
import dk.dbc.dataio.gui.client.util.Format;
import dk.dbc.dataio.jobstore.types.FlowStoreReference;
import dk.dbc.dataio.jobstore.types.FlowStoreReferences;
import dk.dbc.dataio.jobstore.types.JobInfoSnapshot;
import dk.dbc.dataio.jobstore.types.JobInputStream;
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
                jobInfoSnapshot.getTimeOfCompletion() == null ? "" : simpleDateFormat.format(jobInfoSnapshot.getTimeOfCompletion()),
                String.valueOf(jobInfoSnapshot.getJobId()),
                Long.toString(jobInfoSnapshot.getSpecification().getSubmitterId()),
                getSubmitterName(jobInfoSnapshot.getFlowStoreReferences()) ,
                getFlowBinderName(jobInfoSnapshot.getFlowStoreReferences()),
                getSinkId(jobInfoSnapshot.getFlowStoreReferences()),
                getSinkName(jobInfoSnapshot.getFlowStoreReferences()),
                jobInfoSnapshot.getState().allPhasesAreDone(),
                getTotal(jobInfoSnapshot.getState()),
                getFailed(jobInfoSnapshot.getState()),
                getIgnored(jobInfoSnapshot.getState()),
                jobInfoSnapshot.getState().getPhase(State.Phase.PROCESSING).getIgnored(),
                getStateCount(jobInfoSnapshot.getState().getPhase(State.Phase.PARTITIONING)),
                getStateCount(jobInfoSnapshot.getState().getPhase(State.Phase.PROCESSING)),
                getStateCount(jobInfoSnapshot.getState().getPhase(State.Phase.DELIVERING)),
                getDiagnostics(jobInfoSnapshot.getState().getDiagnostics()),
                hasFatalDiagnostic(jobInfoSnapshot.getState().getDiagnostics()),
                jobInfoSnapshot.getSpecification().getPackaging(),
                jobInfoSnapshot.getSpecification().getFormat(),
                jobInfoSnapshot.getSpecification().getCharset(),
                jobInfoSnapshot.getSpecification().getDestination(),
                jobInfoSnapshot.getSpecification().getMailForNotificationAboutVerification(),
                jobInfoSnapshot.getSpecification().getMailForNotificationAboutProcessing(),
                jobInfoSnapshot.getSpecification().getResultmailInitials(),
                getType(jobInfoSnapshot.getSpecification().getType()),
                jobInfoSnapshot.getSpecification().getDataFile(),
                jobInfoSnapshot.getPartNumber(),
                WorkflowNoteModelMapper.toWorkflowNoteModel(jobInfoSnapshot.getWorkflowNote())
        );
    }

    public static JobInputStream toJobInputStream(JobModel jobModel) {

        final JobSpecification  jobSpecification = new JobSpecification(
                jobModel.getPackaging(),
                jobModel.getFormat(),
                jobModel.getCharset(),
                jobModel.getDestination(),
                Integer.parseInt(jobModel.getSubmitterNumber()),
                jobModel.getMailForNotificationAboutVerification(),
                jobModel.getMailForNotificationAboutProcessing(),
                jobModel.getResultmailInitials(),
                jobModel.getDataFile(),
                getType(jobModel.getType())
        );
        return new JobInputStream(jobSpecification, jobModel.isJobDone(), jobModel.getPartNumber());
    }
    /**
     * Maps a list of JobInfoSnapshot objects to a list of JobModel objects
     * @param jobInfoSnapshots A list of input JobInfoSnapshot objects
     * @return The list of resulting JobModel objects
     */
    public static List<JobModel> toModel(List<JobInfoSnapshot> jobInfoSnapshots) {
        List<JobModel> jobInfoSnapshotModels = new ArrayList<>(jobInfoSnapshots.size());

        for(JobInfoSnapshot jobInfoSnapshot : jobInfoSnapshots) {
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
        List<DiagnosticModel> diagnosticModels = new ArrayList<>(diagnostics.size());
        for(Diagnostic diagnostic : diagnostics) {
            diagnosticModels.add(new DiagnosticModel(diagnostic.getLevel().name(), diagnostic.getMessage(), diagnostic.getStacktrace()));
        }
        return diagnosticModels;
    }

    private static boolean hasFatalDiagnostic(List<Diagnostic> diagnostics) {
        for(Diagnostic diagnostic : diagnostics) {
            if(diagnostic.getLevel() == Diagnostic.Level.FATAL) {
                return true;
            }
        }
        return false;
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

    private static JobModel.Type getType(JobSpecification.Type type) {
        switch (type) {
            case TRANSIENT: return JobModel.Type.TRANSIENT;
            case TEST: return JobModel.Type.TEST;
            case ACCTEST: return JobModel.Type.ACCTEST;
            default: return JobModel.Type.TRANSIENT;
        }
    }

    private static JobSpecification.Type getType(JobModel.Type type) {
        switch (type) {
            case TRANSIENT: return JobSpecification.Type.TRANSIENT;
            case TEST: return JobSpecification.Type.TEST;
            case ACCTEST: return JobSpecification.Type.ACCTEST;
            default: return JobSpecification.Type.TRANSIENT;
        }
    }

}