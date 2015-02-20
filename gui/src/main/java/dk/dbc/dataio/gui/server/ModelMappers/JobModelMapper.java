package dk.dbc.dataio.gui.server.ModelMappers;

import dk.dbc.dataio.gui.client.model.JobModel;
import dk.dbc.dataio.gui.client.util.Format;
import dk.dbc.dataio.jobstore.types.JobInfoSnapshot;
import dk.dbc.dataio.jobstore.types.State;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;

public class JobModelMapper {

    public static JobModel toModel(JobInfoSnapshot jobInfoSnapshot) {
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat(Format.LONG_DATE_TIME_FORMAT);
        return new JobModel(
                simpleDateFormat.format(jobInfoSnapshot.getTimeOfCreation()),
                String.valueOf(jobInfoSnapshot.getJobId()),
                jobInfoSnapshot.getSpecification().getDataFile().replaceFirst("^/tmp/", ""),
                Long.toString(jobInfoSnapshot.getSpecification().getSubmitterId()),
                jobInfoSnapshot.getState().allPhasesAreDone(),
                getSucceeded(jobInfoSnapshot.getState(), State.Phase.DELIVERING),
                getFailed(jobInfoSnapshot.getState()),
                getIgnored(jobInfoSnapshot.getState(), State.Phase.DELIVERING));
    }

    public static List<JobModel> toModel(List<JobInfoSnapshot> jobInfoSnapshots) {
        List<JobModel> jobInfoSnapshotModels = new ArrayList<JobModel>(jobInfoSnapshots.size());

        for(JobInfoSnapshot jobInfoSnapshot : jobInfoSnapshots) {
            jobInfoSnapshotModels.add(toModel(jobInfoSnapshot));
        }
        return jobInfoSnapshotModels;
    }

    private static int getSucceeded(State state, State.Phase phase) {
        return state.getPhase(phase).getSucceeded();
    }

    private static int getFailed(State state) {
        return state.getPhase(State.Phase.PARTITIONING).getFailed() +
                state.getPhase(State.Phase.PROCESSING).getFailed() +
                state.getPhase(State.Phase.DELIVERING).getFailed();
    }

    private static int getIgnored(State state, State.Phase phase) {
        return state.getPhase(phase).getIgnored();
    }
}
