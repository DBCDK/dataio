package dk.dbc.dataio.gui.server.modelmappers;

import dk.dbc.dataio.gui.client.pages.sink.status.SinkStatusTable;
import dk.dbc.dataio.jobstore.types.SinkStatusSnapshot;

import java.util.ArrayList;
import java.util.List;

public class SinkStatusModelMapper {

    /**
     * Private Constructor prevents instantiation of this static class
     */
    private SinkStatusModelMapper() {
    }

    /**
     * Maps a list of SinkStatusSnapshot to a list of SinkStatusModel
     *
     * @param sinkStatusSnapshots, the list of sinkStatusSnapshot
     * @return list of SinkStatusModels
     */
    public static List<SinkStatusTable.SinkStatusModel> toModel(List<SinkStatusSnapshot> sinkStatusSnapshots) {
        List<SinkStatusTable.SinkStatusModel> sinkStatusModels = new ArrayList<>(sinkStatusSnapshots.size());
        for (SinkStatusSnapshot sinkStatusSnapshot : sinkStatusSnapshots) {
            sinkStatusModels.add(toModel(sinkStatusSnapshot));
        }
        return sinkStatusModels;
    }

    /**
     * Maps a SinkStatusSnapshot to a SinkStatusModel
     *
     * @param sinkStatusSnapshot, the sinkStatusSnapshot
     * @return SinkStatusModel
     */
    public static SinkStatusTable.SinkStatusModel toModel(SinkStatusSnapshot sinkStatusSnapshot) {
        return new SinkStatusTable.SinkStatusModel()
                .withSinkId(sinkStatusSnapshot.getSinkId())
                .withSinkType(sinkStatusSnapshot.getType().name())
                .withName(sinkStatusSnapshot.getName())
                .withOutstandingJobs(sinkStatusSnapshot.getNumberOfJobs())
                .withOutstandingChunks(sinkStatusSnapshot.getNumberOfChunks());
    }
}
